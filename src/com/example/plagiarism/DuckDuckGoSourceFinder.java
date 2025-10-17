package com.example.plagiarism;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DuckDuckGoSourceFinder implements SourceFinder {
    private static final String[] SEARCH_ENDPOINTS = new String[] {
            "https://html.duckduckgo.com/html/?q=",
            "https://duckduckgo.com/html/?q="
    };

    @Override
    public List<WebSource> findExactWordMatches(String text, int maxResults) {
        String phrase = chooseExactPhrase(text);
        if (phrase.isEmpty()) {
            return Collections.emptyList();
        }
        String query = '"' + phrase + '"';
        String html = null;
        for (String base : SEARCH_ENDPOINTS) {
            html = fetchHtml(base + urlEncode(query));
            if (html != null && !html.isBlank()) break;
        }
        if (html == null || html.isBlank()) {
            return Collections.emptyList();
        }
        return parseResults(html, phrase, Math.max(1, maxResults));
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String chooseExactPhrase(String text) {
        if (text == null) return "";
        String norm = normalizeWhitespace(text);
        if (norm.isEmpty()) return "";
        String[] sentences = norm.split("(?<=[.!?])\\s+");
        String best = "";
        for (String sent : sentences) {
            if (sent.length() <= 200 && sent.length() > best.length()) {
                best = sent;
            }
        }
        if (!best.isEmpty()) return best;
        String[] words = norm.split("\\s+");
        int take = Math.min(words.length, 12);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < take; i++) {
            if (i > 0) sb.append(' ');
            sb.append(words[i]);
        }
        return sb.toString();
    }

    private static String normalizeWhitespace(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    private String fetchHtml(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (SourceFinder)");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(8000);
            try (InputStream in = conn.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private List<WebSource> parseResults(String html, String phrase, int maxResults) {
        List<WebSource> results = new ArrayList<>();
        Pattern p = Pattern.compile("(?s)<a[^>]*class=\\\"result__a\\\"[^>]*href=\\\"([^\\\"]+)\\\"[^>]*>(.*?)</a>(?:.*?(?:<a[^>]*class=\\\"result__snippet\\\"[^>]*>(.*?)</a>|<div[^>]*class=\\\"result__snippet\\\"[^>]*>(.*?)</div>))?");
        Matcher m = p.matcher(html);
        while (m.find() && results.size() < maxResults) {
            String href = htmlDecode(m.group(1));
            String title = stripTags(htmlDecode(m.group(2)));
            String snippetRaw = m.group(3) != null ? m.group(3) : m.group(4);
            String snippet = snippetRaw == null ? "" : stripTags(htmlDecode(snippetRaw));
            boolean verified = containsPhrase(snippet, phrase);
            results.add(new WebSource(href, title, snippet, verified));
        }
        return results;
    }

    private static boolean containsPhrase(String haystack, String phrase) {
        if (haystack == null) return false;
        String h = normalizeWhitespace(haystack).toLowerCase(Locale.ROOT);
        String p = normalizeWhitespace(phrase).toLowerCase(Locale.ROOT);
        return h.contains(p);
    }

    private static String stripTags(String s) {
        return s == null ? null : s.replaceAll("<[^>]+>", "");
    }

    private static String htmlDecode(String s) {
        if (s == null) return null;
        return s
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }
}
