package com.example.plagiarism;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class Block {
    private final int index;
    private final String timestamp;
    private final Document document;
    private final String previousHash;
    private final String hash;

    public Block(int index, Document document, String previousHash) {
        this.index = index;
        this.timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        this.document = document;
        this.previousHash = previousHash == null ? "" : previousHash;
        this.hash = computeHash();
    }

    // Used when loading from storage to preserve timestamp/hash
    public Block(int index, String timestamp, Document document, String previousHash, String hash) {
        this.index = index;
        this.timestamp = timestamp;
        this.document = document;
        this.previousHash = previousHash == null ? "" : previousHash;
        this.hash = hash;
    }

    public String computeHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String data = index + timestamp + (document == null ? "" : document.toString()) + previousHash;
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
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
