package com.example.plagiarism;

import java.util.*;
import java.util.regex.Pattern;

public class PlagiarismChecker {

    public enum Metric { COSINE, JACCARD }
    public enum Verdict { SAFE, MODERATE, HIGH }

    public static class PlagiarismResult {
        public final double cosine; // 0..1
        public final double jaccard; // 0..1
        public final Verdict verdict;

        public PlagiarismResult(double cosine, double jaccard, Verdict verdict) {
            this.cosine = cosine;
            this.jaccard = jaccard;
            this.verdict = verdict;
        }
    }

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "a","an","the","and","or","but","if","then","else","when","at","by","for","with","about","against","between","into","through","during","before","after","above","below","to","from","up","down","in","out","on","off","over","under","again","further","then","once","here","there","all","any","both","each","few","more","most","other","some","such","no","nor","not","only","own","same","so","than","too","very","can","will","just","don","should","now","is","am","are","was","were","be","been","being","do","does","did","having","have","has","had","of"
    ));

    private static final Pattern NON_LETTERS = Pattern.compile("[^a-z]+");

    public PlagiarismResult checkPlagiarism(Document d1, Document d2) {
        String t1 = d1.extractText();
        String t2 = d2.extractText();
        double cosine = cosineSimilarity(t1, t2);
        double jaccard = jaccardIndex(t1, t2);
        double percent = cosine * 100.0; // Primary indicator
        Verdict verdict = verdictFromPercent(percent);
        double combinedScore = Math.max(cosine, jaccard);
        d1.setPlagiarismScore(combinedScore);
        d2.setPlagiarismScore(combinedScore);
        return new PlagiarismResult(cosine, jaccard, verdict);
    }

    public double calculateSimilarity(Document d1, Document d2, Metric metric) {
        String t1 = d1.extractText();
        String t2 = d2.extractText();
        return metric == Metric.COSINE ? cosineSimilarity(t1, t2) : jaccardIndex(t1, t2);
    }

    public Verdict verdictFromPercent(double percent) {
        if (percent > 70.0) return Verdict.HIGH;
        if (percent >= 30.0) return Verdict.MODERATE;
        return Verdict.SAFE;
    }

    // --- Cosine Similarity ---
    public double cosineSimilarity(String textA, String textB) {
        List<String> tokensA = tokenize(textA);
        List<String> tokensB = tokenize(textB);
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0;

        Map<String, Integer> tfA = termFrequencies(tokensA);
        Map<String, Integer> tfB = termFrequencies(tokensB);

        Set<String> vocabulary = new HashSet<>();
        vocabulary.addAll(tfA.keySet());
        vocabulary.addAll(tfB.keySet());

        long dot = 0L;
        long normA2 = 0L;
        long normB2 = 0L;
        for (String term : vocabulary) {
            int a = tfA.getOrDefault(term, 0);
            int b = tfB.getOrDefault(term, 0);
            dot += (long) a * b;
        }
        for (int a : tfA.values()) normA2 += (long) a * a;
        for (int b : tfB.values()) normB2 += (long) b * b;

        if (normA2 == 0 || normB2 == 0) return 0.0;
        return dot / (Math.sqrt(normA2) * Math.sqrt(normB2));
    }

    // --- Jaccard Index (set-based) ---
    public double jaccardIndex(String textA, String textB) {
        Set<String> setA = new HashSet<>(tokenizeUnique(textA));
        Set<String> setB = new HashSet<>(tokenizeUnique(textB));
        if (setA.isEmpty() && setB.isEmpty()) return 1.0;
        if (setA.isEmpty() || setB.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);
        return (double) intersection.size() / (double) union.size();
    }

    private List<String> tokenize(String text) {
        if (text == null) return Collections.emptyList();
        String normalized = NON_LETTERS.matcher(text.toLowerCase()).replaceAll(" ");
        String[] parts = normalized.trim().split("\\s+");
        List<String> tokens = new ArrayList<>(parts.length);
        for (String token : parts) {
            if (token.isEmpty()) continue;
            if (STOPWORDS.contains(token)) continue;
            tokens.add(token);
        }
        return tokens;
    }

    private List<String> tokenizeUnique(String text) {
        List<String> tokens = tokenize(text);
        LinkedHashSet<String> unique = new LinkedHashSet<>(tokens);
        return new ArrayList<>(unique);
    }

    private Map<String, Integer> termFrequencies(List<String> tokens) {
        Map<String, Integer> tf = new HashMap<>();
        for (String token : tokens) {
            tf.merge(token, 1, Integer::sum);
        }
        return tf;
    }
}
