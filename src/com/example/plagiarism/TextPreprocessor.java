package com.example.plagiarism;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class TextPreprocessor {
    private static final Pattern NON_LETTER = Pattern.compile("[^a-zA-Z\\s]");
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            // Common English stopwords
            "a","an","the","and","or","but","if","while","of","at","by","for","with","about","against","between","into","through","during","before","after","above","below","to","from","up","down","in","out","on","off","over","under","again","further","then","once","here","there","when","where","why","how","all","any","both","each","few","more","most","other","some","such","no","nor","not","only","own","same","so","than","too","very","can","will","just","don","should","now","is","am","are","was","were","be","been","being","have","has","had","do","does","did"
    ));

    public static List<String> preprocessToTokens(String raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        String lowered = raw.toLowerCase(Locale.ROOT);
        String lettersOnly = NON_LETTER.matcher(lowered).replaceAll(" ");
        List<String> tokens = splitWords(lettersOnly);
        List<String> filtered = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String token : tokens) {
            if (token.isBlank()) continue;
            if (STOPWORDS.contains(token)) continue;
            if (seen.add(token)) { // remove duplicates
                filtered.add(token);
            }
        }
        return filtered;
    }

    private static List<String> splitWords(String text) {
        // Beginner-friendly split on whitespace
        String[] parts = text.trim().split("\\s+");
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            if (!p.isBlank()) result.add(p);
        }
        return result;
    }
}
