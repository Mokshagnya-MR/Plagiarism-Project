package com.example.plagiarism;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort web source discovery without external libraries.
 *
 * Strategy:
 * 1) Build a few distinctive queries from the submission text (long sentences and token-based query)
 * 2) Query DuckDuckGo HTML endpoint
 * 3) Scrape result links and fetch pages
 * 4) Convert HTML to plain text and compute similarity
 * 5) Return the best match above a threshold
 */
public class SourceDiscoveryService {

    public static class DiscoveredSource {
        private final String url;
        private final String text;

        public DiscoveredSource(String url, String text) {
            this.url = url == null ? "" : url;
            this.text = text == null ? "" : text;
        }

        public String url() { return url; }
        public String text() { return text; }
    }

    private final HttpClient httpClient;

    public SourceDiscoveryService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Optional<DiscoveredSource> discoverOriginalSource(String submissionText) {
        if (submissionText == null || submissionText.isBlank()) {
            return Optional.empty();
        }
        List<String> queries = buildQueries(submissionText);
        double bestScore = -1.0;
        DiscoveredSource best = null;

        for (String q : queries) {
            List<String> links = searchDuckDuckGo(q);
            for (String link : links) {
                String pageText = fetchPageText(link);
                if (pageText.isBlank()) continue;
                Document submission = new Document("Submission", "system", "", submissionText);
                Document candidate = new Document("Candidate", "web", "", pageText);
                double score = PlagiarismChecker.computeSimilarity(submission, candidate, "Cosine");
                if (score > bestScore) {
                    bestScore = score;
                    best = new DiscoveredSource(link, pageText);
                }
                // Short-circuit on very high similarity
                if (bestScore >= 0.9) {
                    return Optional.of(best);
                }
            }
        }
        if (bestScore >= 0.55 && best != null) {
            return Optional.of(best);
        }
        return Optional.empty();
    }

    private List<String> buildQueries(String text) {
        List<String> queries = new ArrayList<>();
        // Sentence-based exact-match queries
        List<String> sentences = extractSentences(text);
        sentences.sort(Comparator.comparingInt(String::length).reversed());
        int added = 0;
        for (String s : sentences) {
            String trimmed = s.trim();
            if (trimmed.length() >= 60 && trimmed.length() <= 220) {
                queries.add("\"" + trimmed + "\"");
                if (++added >= 3) break;
            }
        }
        // Token-based broad query
        List<String> tokens = TextPreprocessor.preprocessToTokens(text);
        Set<String> unique = new HashSet<>();
        List<String> selected = new ArrayList<>();
        for (String t : tokens) {
            if (unique.add(t)) {
                selected.add(t);
            }
            if (selected.size() >= 10) break;
        }
        if (!selected.isEmpty()) {
            queries.add(String.join(" ", selected));
        }
        if (queries.isEmpty()) {
            queries.add(text.length() > 120 ? text.substring(0, 120) : text);
        }
        return queries;
    }

    private static List<String> extractSentences(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        BreakIterator it = BreakIterator.getSentenceInstance(Locale.ROOT);
        it.setText(text);
        int start = it.first();
        for (int end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
            String sentence = text.substring(start, end).trim();
            if (!sentence.isEmpty()) result.add(sentence);
        }
        return result;
    }

    private List<String> searchDuckDuckGo(String query) {
        try {
            String q = java.net.URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://duckduckgo.com/html/?kl=us-en&q=" + q;
            HttpRequest req = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/118 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .timeout(Duration.ofSeconds(15))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return List.of();
            String html = resp.body();
            return extractResultLinks(html);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static final Pattern RESULT_LINK = Pattern.compile("<a[^>]*class=\"[^\"]*result__a[^\"]*\"[^>]*href=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    private List<String> extractResultLinks(String html) {
        List<String> links = new ArrayList<>();
        Matcher m = RESULT_LINK.matcher(html);
        while (m.find()) {
            String href = m.group(1);
            if (href == null || href.isBlank()) continue;
            if (href.startsWith("/y.js") || href.startsWith("javascript:")) continue;
            if (href.contains("duckduckgo.com/y.js")) continue;
            // Some links are DDG redirect links starting with /l/?kh=...&uddg=URL
            if (href.startsWith("/l/?")) {
                int idx = href.indexOf("uddg=");
                if (idx >= 0) {
                    String enc = href.substring(idx + 5);
                    href = java.net.URLDecoder.decode(enc, StandardCharsets.UTF_8);
                }
            }
            if (href.startsWith("http://") || href.startsWith("https://")) {
                links.add(href);
            }
            if (links.size() >= 5) break; // limit fetches per query
        }
        return links;
    }

    private String fetchPageText(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/118 Safari/537.36")
                    .timeout(Duration.ofSeconds(15))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return "";
            String html = resp.body();
            return htmlToText(html);
        } catch (Exception e) {
            return "";
        }
    }

    private static String htmlToText(String html) {
        if (html == null || html.isBlank()) return "";
        String noScript = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ");
        String withBreaks = noScript.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n\n")
                .replaceAll("(?i)</div>", "\n");
        String text = withBreaks.replaceAll("(?is)<[^>]+>", " ");
        text = decodeBasicEntities(text);
        text = text.replaceAll("\n{3,}", "\n\n");
        text = text.replaceAll("[\r\t]", " ");
        return text.trim();
    }

    private static String decodeBasicEntities(String s) {
        return s.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }
}
