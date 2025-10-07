package com.example.plagiarism;

import java.util.ArrayList;
import java.util.List;

public class PlagiarismChecker {

    public static double computeSimilarity(Document a, Document b, SimilarityAlgorithm algorithm) {
        List<String> tokensA = TextPreprocessor.preprocessToTokens(a.extractText());
        List<String> tokensB = TextPreprocessor.preprocessToTokens(b.extractText());
        return algorithm.compute(tokensA, tokensB);
    }

    public static Result checkPlagiarism(Document a, Document b, SimilarityAlgorithm algorithm) {
        double score = computeSimilarity(a, b, algorithm);
        a.setPlagiarismScore(score);
        b.setPlagiarismScore(score);
        String verdict = verdictFor(score);
        return new Result(score, verdict);
    }

    public static List<PairwiseResult> checkPairwise(List<Document> documents, SimilarityAlgorithm algorithm) {
        List<PairwiseResult> results = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            for (int j = i + 1; j < documents.size(); j++) {
                Document a = documents.get(i);
                Document b = documents.get(j);
                double score = computeSimilarity(a, b, algorithm);
                results.add(new PairwiseResult(a, b, score, verdictFor(score)));
            }
        }
        return results;
    }

    public static String verdictFor(double score) {
        double percent = score * 100.0;
        if (percent < 30.0) return "Safe";
        if (percent <= 70.0) return "Moderate";
        return "High";
    }

    public record Result(double score, String verdict) {}

    public record PairwiseResult(Document a, Document b, double score, String verdict) {}
}
