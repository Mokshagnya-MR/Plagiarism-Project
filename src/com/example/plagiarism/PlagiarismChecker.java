package com.example.plagiarism;

import com.example.plagiarism.similarity.LevenshteinSimilarity;
import com.example.plagiarism.similarity.NGramSimilarity;
import com.example.plagiarism.config.AppConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlagiarismChecker {

    public static double computeSimilarity(Document a, Document b, String algorithmName) {
        List<String> tokensA = TextPreprocessor.preprocessToTokens(a.extractText());
        List<String> tokensB = TextPreprocessor.preprocessToTokens(b.extractText());

        switch (algorithmName.toLowerCase()) {
            case "jaccard":
                return computeJaccard(tokensA, tokensB);
            case "levenshtein":
                return LevenshteinSimilarity.compute(tokensA, tokensB);
            case "ngram":
                return NGramSimilarity.compute(tokensA, tokensB, 3);
            case "cosine":
            default:
                return computeCosine(tokensA, tokensB);
        }
    }

    public static Result checkPlagiarism(Document a, Document b, String algorithmName) {
        double score = computeSimilarity(a, b, algorithmName);
        a.setPlagiarismScore(score);
        b.setPlagiarismScore(score);
        String verdict = verdictFor(score);
        return new Result(score, verdict);
    }

    public static List<PairwiseResult> checkPairwise(List<Document> documents, String algorithmName) {
        List<PairwiseResult> results = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            for (int j = i + 1; j < documents.size(); j++) {
                Document a = documents.get(i);
                Document b = documents.get(j);
                double score = computeSimilarity(a, b, algorithmName);
                results.add(new PairwiseResult(a, b, score, verdictFor(score)));
            }
        }
        return results;
    }

    public static String verdictFor(double score) {
        AppConfig config = AppConfig.getInstance();
        double safeThreshold = config.getDouble("similarity.threshold.safe", 30.0);
        double highThreshold = config.getDouble("similarity.threshold.high", 70.0);

        double percent = score * 100.0;
        if (percent < safeThreshold) return "Safe";
        if (percent <= highThreshold) return "Moderate";
        return "High";
    }

    public static class Result {
        private final double score;
        private final String verdict;
        public Result(double score, String verdict) { this.score = score; this.verdict = verdict; }
        public double score() { return score; }
        public String verdict() { return verdict; }
    }

    public static class PairwiseResult {
        private final Document a;
        private final Document b;
        private final double score;
        private final String verdict;
        public PairwiseResult(Document a, Document b, double score, String verdict) {
            this.a = a; this.b = b; this.score = score; this.verdict = verdict;
        }
        public Document a() { return a; }
        public Document b() { return b; }
        public double score() { return score; }
        public String verdict() { return verdict; }
    }

    // Beginner-friendly cosine and jaccard implementations in one place
    private static double computeCosine(List<String> tokensA, List<String> tokensB) {
        Map<String, Integer> freqA = buildFrequencyMap(tokensA);
        Map<String, Integer> freqB = buildFrequencyMap(tokensB);
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
        for (int a : freqA.values()) normA += a * a;
        for (int b : freqB.values()) normB += b * b;
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static double computeJaccard(List<String> tokensA, List<String> tokensB) {
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

    private static Map<String, Integer> buildFrequencyMap(List<String> tokens) {
        Map<String, Integer> map = new HashMap<>();
        for (String token : tokens) {
            map.merge(token, 1, Integer::sum);
        }
        return map;
    }
}
