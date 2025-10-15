package com.example.plagiarism;

import java.util.Random;

public class PlagiarismAPIClient {
    private final Random random = new Random();

    // Mock external API call: returns a stable pseudo-random score based on text hash
    public double checkPlagiarismAPI(String text) {
        if (text == null || text.isBlank()) return 0.0;
        int seed = text.hashCode();
        Random seeded = new Random(seed);
        double base = 0.2 + (seeded.nextDouble() * 0.6); // 0.2 - 0.8
        return Math.min(1.0, Math.max(0.0, base));
    }
}
