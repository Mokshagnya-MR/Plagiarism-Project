package com.example.plagiarism;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JaccardIndex implements SimilarityAlgorithm {
    @Override
    public double compute(List<String> tokensA, List<String> tokensB) {
        Set<String> setA = new HashSet<>(tokensA);
        Set<String> setB = new HashSet<>(tokensB);
        if (setA.isEmpty() && setB.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);
        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / (double) union.size();
    }
}
