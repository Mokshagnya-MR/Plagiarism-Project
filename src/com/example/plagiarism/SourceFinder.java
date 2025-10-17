package com.example.plagiarism;

import java.util.List;

public interface SourceFinder {
    List<WebSource> findExactWordMatches(String text, int maxResults);
}
