package com.example.plagiarism;

import java.util.List;
import java.util.Map;

public interface SimilarityAlgorithm {
    double compute(List<String> tokensA, List<String> tokensB);

    static Map<String, Integer> buildFrequencyMap(List<String> tokens) {
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        for (String token : tokens) {
            map.merge(token, 1, Integer::sum);
        }
        return map;
    }
}
