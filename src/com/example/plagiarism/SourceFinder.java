package com.example.plagiarism;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic source finder:
 * - Extract URLs embedded in the suspect text and try them
 * - Perform a lightweight DuckDuckGo HTML search with top keywords
 * - Fetch candidate pages and compute similarity, choose the best above a threshold
 * - Returns Document of the original source when found
 */
public class SourceFinder {
    private static final double MIN_ACCEPTABLE_SIMILARITY = 0.15; // Lowered for paraphrased matches
    private static final int MAX_SEARCH_TOKENS = 8;
    private static final int MAX_CANDIDATE_LINKS = 20;
    private static final int PHRASE_WINDOW_SIZE = 12;
    private static final int MAX_PHRASE_WINDOWS = 6;

    private final HttpClient httpClient;

    public SourceFinder() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Optional<Document> autoFindOriginal(Document suspect, String algorithmName) {
        if (suspect == null) return Optional.empty();
        String suspectText = suspect.extractText();
        if (suspectText == null || suspectText.isBlank()) return Optional.empty();

        // 1) Embedded URLs in the suspect text
        List<String> embeddedUrls = extractUrls(suspectText);
        Optional<Document> byEmbedded = pickBestCandidate(embeddedUrls, suspect, algorithmName);
        if (byEmbedded.isPresent()) return byEmbedded;

        // 2) Multi-provider search using exact phrases and keywords; pick most frequent URLs first
        List<String> queries = new ArrayList<>();
        queries.add('"' + pickRepresentativePhrase(suspectText) + '"');
        queries.addAll(buildPhraseQueries(suspectText));
        queries.add(buildKeywordQuery(suspectText));

        List<String> allLinks = new ArrayList<>();
        for (String q : queries) {
            if (q == null || q.isBlank()) continue;
            allLinks.addAll(searchAllProviders(q));
        }
        // Count frequency and sort URLs by descending frequency
        List<String> prioritized = prioritizeByFrequency(allLinks);
        Optional<Document> bySearch = pickBestCandidate(prioritized, suspect, algorithmName);
        if (bySearch.isPresent()) return bySearch;

        return Optional.empty();
    }

    private List<String> searchAllProviders(String query) {
        List<String> links = new ArrayList<>();
        // Optional providers via env vars
        links.addAll(searchBingLinks(query));
        links.addAll(searchSerpApiLinks(query));
        links.addAll(searchSearxngLinks(query));
        // Always include DuckDuckGo HTML fallback
        links.addAll(searchDuckDuckGoLinks(query));
        return links;
    }

    private List<String> searchBingLinks(String query) {
        String apiKey = System.getenv("BING_API_KEY");
        if (apiKey == null || apiKey.isBlank()) return List.of();
        try {
            URI uri = new URI("https", "api.bing.microsoft.com", "/v7.0/search", "q=" + urlEncode(query), null);
            HttpRequest req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Ocp-Apim-Subscription-Key", apiKey)
                    .GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return extractJsonUrls(resp.body());
            }
        } catch (Exception ignored) {}
        return List.of();
    }

    private List<String> searchSerpApiLinks(String query) {
        String apiKey = System.getenv("SERPAPI_KEY");
        if (apiKey == null || apiKey.isBlank()) return List.of();
        try {
            URI uri = new URI("https", "serpapi.com", "/search.json", "engine=google&q=" + urlEncode(query) + "&api_key=" + urlEncode(apiKey), null);
            String json = fetch(uri);
            if (json == null) return List.of();
            return extractJsonUrls(json);
        } catch (Exception ignored) {}
        return List.of();
    }

    private List<String> searchSearxngLinks(String query) {
        String base = System.getenv("SEARXNG_BASE_URL");
        if (base == null || base.isBlank()) return List.of();
        try {
            if (base.endsWith("/")) base = base.substring(0, base.length()-1);
            URI uri = new URI(base + "/search?q=" + urlEncode(query) + "&format=json");
            String json = fetch(uri);
            if (json == null) return List.of();
            return extractJsonUrls(json);
        } catch (Exception ignored) {}
        return List.of();
    }

    private static List<String> extractJsonUrls(String json) {
        if (json == null || json.isBlank()) return List.of();
        List<String> urls = new ArrayList<>();
        Matcher m = Pattern.compile("\"url\"\s*:\s*\"(https?://[^\\\"]+)\"").matcher(json);
        while (m.find()) {
            String u = m.group(1);
            if (u.contains("duckduckgo.com/l/?uddg=")) u = decodeDdgRedirect(u);
            urls.add(u);
            if (urls.size() >= MAX_CANDIDATE_LINKS) break;
        }
        return urls;
    }

    private static List<String> buildPhraseQueries(String text) {
        List<String> words = TextPreprocessor.preprocessToWordsPreservingOrder(text);
        List<String> phrases = new ArrayList<>();
        if (words.isEmpty()) return phrases;
        int step = Math.max(1, words.size() / Math.max(1, MAX_PHRASE_WINDOWS));
        for (int start = 0; start < words.size() && phrases.size() < MAX_PHRASE_WINDOWS; start += step) {
            int end = Math.min(words.size(), start + PHRASE_WINDOW_SIZE);
            if (end - start < 4) break;
            String phrase = String.join(" ", words.subList(start, end));
            phrases.add('"' + phrase + '"');
        }
        return phrases;
    }

    private static List<String> prioritizeByFrequency(List<String> urls) {
        Map<String, Integer> counts = new HashMap<>();
        for (String u : urls) {
            if (u == null || u.isBlank()) continue;
            counts.merge(u, 1, Integer::sum);
        }
        List<Map.Entry<String,Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a,b) -> Integer.compare(b.getValue(), a.getValue()));
        List<String> sorted = new ArrayList<>();
        for (Map.Entry<String,Integer> e : entries) sorted.add(e.getKey());
        return sorted;
    }

    private Optional<Document> pickBestCandidate(List<String> urls, Document suspect, String algorithmName) {
        if (urls == null || urls.isEmpty()) return Optional.empty();
        double bestScore = -1.0;
        Document bestDoc = null;
        Set<String> seen = new HashSet<>();
        for (String url : urls) {
            if (url == null || url.isBlank()) continue;
            String normalized = normalizeUrl(url);
            if (!seen.add(normalized)) continue;
            String content = fetchText(normalized);
            if (content == null || content.isBlank()) continue;
            Document candidate = new Document("Original Source", "", LocalDate.now().toString(), content, normalized);
            double score = PlagiarismChecker.computeSimilarity(suspect, candidate, algorithmName);
            if (score > bestScore) {
                bestScore = score;
                bestDoc = candidate;
            }
        }
        if (bestDoc != null && bestScore >= MIN_ACCEPTABLE_SIMILARITY) {
            bestDoc.setPlagiarismScore(bestScore);
            return Optional.of(bestDoc);
        }
        return Optional.empty();
    }

    private static String buildKeywordQuery(String text) {
        List<String> tokens = TextPreprocessor.preprocessToTokens(text);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(MAX_SEARCH_TOKENS, tokens.size()); i++) {
            if (i > 0) sb.append(' ');
            sb.append(tokens.get(i));
        }
        return sb.toString();
    }

    private List<String> searchDuckDuckGoLinks(String query) {
        if (query.isBlank()) return List.of();
        try {
            URI uri = new URI("https", "duckduckgo.com", "/html/", "q=" + urlEncode(query), null);
            String html = fetch(uri);
            if (html == null) return List.of();
            return extractLinksFromDuckDuckGo(html);
        } catch (URISyntaxException e) {
            return List.of();
        }
    }

    private List<String> extractLinksFromDuckDuckGo(String html) {
        // Very naive extraction: look for result links in anchor tags
        // DuckDuckGo HTML results often contain links with href starting with https://
        List<String> links = new ArrayList<>();
        Matcher m = Pattern.compile("href=\\\"(https?://[^\\\"]+)\\\"").matcher(html);
        while (m.find()) {
            String href = m.group(1);
            // Decode DDG redirector links
            if (href.contains("duckduckgo.com/l/?uddg=")) {
                href = decodeDdgRedirect(href);
            }
            links.add(href);
            if (links.size() >= MAX_CANDIDATE_LINKS) break;
        }
        return links;
    }

    private static String pickRepresentativePhrase(String text) {
        List<String> words = TextPreprocessor.preprocessToWordsPreservingOrder(text);
        if (words.isEmpty()) return "";
        if (words.size() <= 8) return String.join(" ", words);
        int start = Math.max(0, words.size() / 3 - 3);
        int end = Math.min(words.size(), start + 8);
        return String.join(" ", words.subList(start, end));
    }

    private static String decodeDdgRedirect(String href) {
        try {
            int i = href.indexOf("uddg=");
            if (i >= 0) {
                String enc = href.substring(i + 5);
                return java.net.URLDecoder.decode(enc, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}
        return href;
    }

    private static List<String> extractUrls(String text) {
        List<String> urls = new ArrayList<>();
        Matcher m = Pattern.compile("https?://\\S+").matcher(text);
        while (m.find()) {
            String url = m.group();
            // Trim trailing punctuation
            url = url.replaceAll("[)\"'.,;!?]+$", "");
            urls.add(url);
        }
        return urls;
    }

    private String normalizeUrl(String url) {
        // Accept http(s) or file:// URIs
        if (url.startsWith("file://")) return url;
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        return "https://" + url;
    }

    private String fetchText(String url) {
        if (url.startsWith("file://")) {
            try {
                Path path = Path.of(URI.create(url));
                return Files.readString(path);
            } catch (Exception e) {
                return null;
            }
        }
        try {
            String html = fetch(URI.create(url));
            if (html == null) return null;
            String text = htmlToPlainText(html);
            if (text != null && text.length() >= 200) return text;
            String reader = fetch(URI.create("https://r.jina.ai/http://" + url.replaceFirst("^https?://", "")));
            if (reader != null && !reader.isBlank()) return reader.trim();
            return text;
        } catch (Exception e) {
            return null;
        }
    }

    private String fetch(URI uri) {
        try {
            HttpRequest req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "Mozilla/5.0 (compatible; SourceFinder/1.0; +https://example.org)")
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                Charset charset = sniffCharset(resp);
                return new String(resp.body(), charset);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Charset sniffCharset(HttpResponse<?> resp) {
        try {
            String ctype = resp.headers().firstValue("content-type").orElse("").toLowerCase();
            if (ctype.contains("charset=")) {
                String cs = ctype.substring(ctype.indexOf("charset=") + 8).trim();
                // strip ending ; if any
                int semi = cs.indexOf(';');
                if (semi > 0) cs = cs.substring(0, semi);
                return Charset.forName(cs);
            }
        } catch (Exception ignored) {}
        return StandardCharsets.UTF_8;
    }

    private static String htmlToPlainText(String html) {
        String noScripts = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ");
        String withBreaks = noScripts.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n\n");
        String noTags = withBreaks.replaceAll("(?s)<[^>]+>", " ");
        String unescaped = noTags
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        return unescaped.replaceAll("\s+", " ").trim();
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    // Convenience to build a Document from a local file path
    public static Optional<Document> buildDocumentFromFile(Path path) {
        try {
            if (path == null || !Files.exists(path)) return Optional.empty();
            String text = Files.readString(path);
            String url = path.toUri().toString();
            Document doc = new Document(path.getFileName().toString(), System.getProperty("user.name"), LocalDate.now().toString(), text, url);
            return Optional.of(doc);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    // Convenience to build a Document from a URL (http/https/file)
    public Optional<Document> buildDocumentFromUrl(String url) {
        try {
            if (url == null || url.isBlank()) return Optional.empty();
            String normalized = normalizeUrl(url);
            String text = fetchText(normalized);
            if (text == null || text.isBlank()) return Optional.empty();
            String title = "Original Source";
            Document doc = new Document(title, "", LocalDate.now().toString(), text, normalized);
            return Optional.of(doc);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
