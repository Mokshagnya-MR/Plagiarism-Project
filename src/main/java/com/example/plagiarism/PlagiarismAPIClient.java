package com.example.plagiarism;

import java.util.Random;

public class PlagiarismAPIClient {

    private final Random random = new Random();

    // Mock implementation; in real usage, perform HTTP request
    public double checkPlagiarismAPI(String textA, String textB) {
        if (textA == null || textB == null || textA.isBlank() || textB.isBlank()) {
            return 0.0;
        }
        // Deterministic-ish mock based on hashes for repeatability
        int seed = Math.abs((textA + "|" + textB).hashCode());
        random.setSeed(seed);
        // Bias towards lower scores to avoid constant HIGH verdicts
        double value = random.nextDouble() * 0.6; // 0.0..0.6
        return Math.round(value * 1000.0) / 1000.0;
    }
}
