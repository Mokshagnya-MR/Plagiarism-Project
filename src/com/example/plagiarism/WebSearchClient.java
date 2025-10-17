package com.example.plagiarism;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal web search client that queries public search engines for pages
 * containing an exact phrase from Doc1. It avoids external SDKs and only uses
 * Java 11+ HttpClient with light HTML parsing via regex for common engines.
 */
public class WebSearchClient {
    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36";

    private final HttpClient httpClient;

    public WebSearchClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public List<SearchResult> findExactMatches(String rawText, int maxResults) {
        List<String> phrases = extractSearchPhrases(rawText, 3);
        LinkedHashSet<SearchResult> aggregated = new LinkedHashSet<>();

        for (String phrase : phrases) {
            int remaining = Math.max(0, maxResults - aggregated.size());
            if (remaining == 0) break;

            List<SearchResult> ddg = searchDuckDuckGoExact(phrase, remaining);
            aggregated.addAll(ddg);

            remaining = Math.max(0, maxResults - aggregated.size());
            if (remaining == 0) break;

            if (ddg.isEmpty()) { // Fallback only if DDG yielded nothing for this phrase
                aggregated.addAll(searchBingExact(phrase, remaining));
            }
        }

        return new ArrayList<>(aggregated).subList(0, Math.min(maxResults, aggregated.size()));
    }

    // --- Phrase extraction -------------------------------------------------

    private static List<String> extractSearchPhrases(String raw, int maxPhrases) {
        List<String> phrases = new ArrayList<>();
        if (raw == null) return phrases;
        String text = raw.trim().replaceAll("\u201C|\u201D", "\""); // normalize quotes
        if (text.isEmpty()) return phrases;

        // Split by sentence ends or newlines
        String[] sentences = text.split("(?<=[.!?])\\s+|\n+");
        for (String sentence : sentences) {
            String cleaned = sentence.replaceAll("\s+", " ").trim();
            if (cleaned.isEmpty()) continue;
            String phrase = limitWords(cleaned, 12, 5);
            if (!phrase.isEmpty()) phrases.add(phrase);
            if (phrases.size() >= maxPhrases) break;
        }

        // If we still don't have enough, backfill using 8-word shingles from the full text
        if (phrases.size() < maxPhrases) {
            String[] tokens = text.split("\\s+");
            for (int i = 0; i < tokens.length && phrases.size() < maxPhrases; i += 8) {
                StringBuilder sb = new StringBuilder();
                for (int j = i; j < Math.min(tokens.length, i + 8); j++) {
                    sb.append(tokens[j]).append(' ');
                }
                String candidate = sb.toString().trim();
                candidate = candidate.replaceAll("\s+", " ");
                candidate = limitWords(candidate, 12, 5);
                if (!candidate.isEmpty()) phrases.add(candidate);
            }
        }

        // Deduplicate while preserving order
        List<String> deduped = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String p : phrases) {
            String key = p.toLowerCase(Locale.ROOT);
            if (seen.add(key)) deduped.add(p);
        }
        return deduped.subList(0, Math.min(maxPhrases, deduped.size()));
    }

    private static String limitWords(String input, int maxWords, int minWords) {
        String[] words = input.split("\\s+");
        if (words.length < minWords) return "";
        int end = Math.min(words.length, maxWords);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < end; i++) {
            if (i > 0) sb.append(' ');
            sb.append(words[i]);
        }
        return sb.toString().trim();
    }

    // --- DuckDuckGo (HTML) -------------------------------------------------

    private List<SearchResult> searchDuckDuckGoExact(String phrase, int maxResults) {
        String quoted = '"' + phrase + '"';
        String q = URLEncoder.encode(quoted, StandardCharsets.UTF_8);
        String url = "https://duckduckgo.com/html/?q=" + q + "&kl=us-en";

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseDuckDuckGoHtml(response.body(), maxResults);
            }
        } catch (IOException | InterruptedException ignored) {
        }
        return List.of();
    }

    private static List<SearchResult> parseDuckDuckGoHtml(String html, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        if (html == null || html.isEmpty()) return results;

        // Match DDG classic HTML result links: <a class="result__a" href="...">Title</a>
        Pattern pattern = Pattern.compile("<a[^>]*class=\"[^\"]*result__a[^\"]*\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = pattern.matcher(html);
        while (m.find() && results.size() < maxResults) {
            String href = sanitizeUrl(m.group(1));
            String title = stripHtml(m.group(2));
            if (isHttpUrl(href)) {
                results.add(new SearchResult(title, href));
            }
        }
        return results;
    }

    // --- Bing (HTML) -------------------------------------------------------

    private List<SearchResult> searchBingExact(String phrase, int maxResults) {
        String quoted = '"' + phrase + '"';
        String q = URLEncoder.encode(quoted, StandardCharsets.UTF_8);
        String url = "https://www.bing.com/search?q=" + q + "&setmkt=en-US&setlang=en-US";

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseBingHtml(response.body(), maxResults);
            }
        } catch (IOException | InterruptedException ignored) {
        }
        return List.of();
    }

    private static List<SearchResult> parseBingHtml(String html, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        if (html == null || html.isEmpty()) return results;

        // Match Bing result entries: <li class="b_algo"> ... <h2><a href="...">Title</a></h2>
        Pattern pattern = Pattern.compile("<li[^>]*class=\"[^\"]*b_algo[^\"]*\"[^>]*>.*?<h2>\\s*<a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>.*?</h2>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = pattern.matcher(html);
        while (m.find() && results.size() < maxResults) {
            String href = sanitizeUrl(m.group(1));
            String title = stripHtml(m.group(2));
            if (isHttpUrl(href)) {
                results.add(new SearchResult(title, href));
            }
        }
        return results;
    }

    // --- Helpers -----------------------------------------------------------

    private static boolean isHttpUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private static String stripHtml(String html) {
        if (html == null) return "";
        String noTags = html.replaceAll("<[^>]+>", " ");
        return noTags.replaceAll("\\s+", " ").trim();
    }

    private static String sanitizeUrl(String url) {
        if (url == null) return "";
        // Some engines wrap URLs or include redirect params; keep it minimal here
        return url.replaceAll("&amp;", "&").trim();
    }

    public static class SearchResult {
        private final String title;
        private final String url;

        public SearchResult(String title, String url) {
            this.title = title == null ? "" : title;
            this.url = url == null ? "" : url;
        }

        public String getTitle() { return title; }
        public String getUrl() { return url; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SearchResult)) return false;
            SearchResult that = (SearchResult) o;
            return Objects.equals(url, that.url);
        }

        @Override
        public int hashCode() { return Objects.hash(url); }
    }
}
