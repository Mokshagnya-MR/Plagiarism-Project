package com.example.plagiarism.similarity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NGramSimilarity {

    public static double compute(List<String> tokensA, List<String> tokensB, int n) {
        Set<String> ngramsA = generateNGrams(tokensA, n);
        Set<String> ngramsB = generateNGrams(tokensB, n);

        if (ngramsA.isEmpty() && ngramsB.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(ngramsA);
        intersection.retainAll(ngramsB);

        Set<String> union = new HashSet<>(ngramsA);
        union.addAll(ngramsB);

        if (union.isEmpty()) return 0.0;

        return (double) intersection.size() / union.size();
    }

    private static Set<String> generateNGrams(List<String> tokens, int n) {
        Set<String> ngrams = new HashSet<>();

        if (tokens.size() < n) {
            ngrams.add(String.join(" ", tokens));
            return ngrams;
        }

        for (int i = 0; i <= tokens.size() - n; i++) {
            List<String> ngram = tokens.subList(i, i + n);
            ngrams.add(String.join(" ", ngram));
        }

        return ngrams;
    }
}
