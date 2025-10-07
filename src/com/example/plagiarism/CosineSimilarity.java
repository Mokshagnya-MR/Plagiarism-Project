package com.example.plagiarism;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CosineSimilarity implements SimilarityAlgorithm {
    @Override
    public double compute(List<String> tokensA, List<String> tokensB) {
        Map<String, Integer> freqA = SimilarityAlgorithm.buildFrequencyMap(tokensA);
        Map<String, Integer> freqB = SimilarityAlgorithm.buildFrequencyMap(tokensB);
        Set<String> vocabulary = new HashSet<>();
        vocabulary.addAll(freqA.keySet());
        vocabulary.addAll(freqB.keySet());

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (String term : vocabulary) {
            int a = freqA.getOrDefault(term, 0);
            int b = freqB.getOrDefault(term, 0);
            dot += a * b;
        }
        for (int a : freqA.values()) {
            normA += a * a;
        }
        for (int b : freqB.values()) {
            normB += b * b;
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
