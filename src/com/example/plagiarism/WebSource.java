package com.example.plagiarism;

public class WebSource {
    private final String url;
    private final String title;
    private final String snippet;
    private final boolean verifiedPhrasePresent;

    public WebSource(String url, String title, String snippet, boolean verifiedPhrasePresent) {
        this.url = url;
        this.title = title;
        this.snippet = snippet;
        this.verifiedPhrasePresent = verifiedPhrasePresent;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public boolean isVerifiedPhrasePresent() {
        return verifiedPhrasePresent;
    }

    @Override
    public String toString() {
        String marker = verifiedPhrasePresent ? "[Verified] " : "";
        String base = (title == null || title.isBlank() ? url : title);
        String extra = (snippet == null || snippet.isBlank() ? "" : ("\n" + snippet));
        return marker + base + "\n" + url + extra;
    }
}
