package com.example.plagiarism;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

public class Block {
    private final int index;
    private final String timestamp; // ISO-8601
    private final Document document;
    private final String previousHash;
    private final String hash;

    public Block(int index, String timestamp, Document document, String previousHash) {
        this.index = index;
        this.timestamp = timestamp;
        this.document = document;
        this.previousHash = previousHash;
        this.hash = computeHash();
    }

    public static Block create(int index, Document document, String previousHash) {
        String ts = Instant.now().toString();
        return new Block(index, ts, document, previousHash);
    }

    public String computeHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = index + "|" + timestamp + "|" +
                    (document != null ? (safe(document.getTitle()) + "|" + safe(document.getAuthor()) + "|" +
                            safe(document.getSubmissionDate()) + "|" + String.format("%.6f", document.getPlagiarismScore()) + "|" +
                            shortText(document.getText())) : "null") + "|" +
                    previousHash;
            byte[] hashBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String shortText(String s) {
        if (s == null) return "";
        int max = 256;
        return s.length() <= max ? s : s.substring(0, max);
    }

    public int getIndex() {
        return index;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Document getDocument() {
        return document;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getHash() {
        return hash;
    }
}
