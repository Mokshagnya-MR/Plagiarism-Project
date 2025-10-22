package com.example.plagiarism.supabase;

import com.example.plagiarism.config.AppConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SupabaseClient {
    private final HttpClient httpClient;
    private final String supabaseUrl;
    private final String supabaseKey;

    public SupabaseClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        AppConfig config = AppConfig.getInstance();
        this.supabaseUrl = config.getSupabaseUrl();
        this.supabaseKey = config.getSupabaseKey();
    }

    public boolean isConfigured() {
        return supabaseUrl != null && !supabaseUrl.isBlank() &&
               supabaseKey != null && !supabaseKey.isBlank();
    }

    public String saveBlockchainEntry(int blockIndex, String userId, String checkId,
                                      String previousHash, String currentHash,
                                      String blockData, String timestamp) {
        if (!isConfigured()) {
            System.err.println("Supabase not configured");
            return null;
        }

        try {
            String jsonBody = String.format(
                "{\"block_index\":%d,\"user_id\":\"%s\",\"check_id\":\"%s\"," +
                "\"previous_hash\":\"%s\",\"current_hash\":\"%s\"," +
                "\"block_data\":%s,\"timestamp\":\"%s\"}",
                blockIndex, userId, checkId, previousHash, currentHash, blockData, timestamp
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(supabaseUrl + "/rest/v1/blockchain_entries"))
                    .header("Content-Type", "application/json")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Prefer", "return=representation")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return extractId(response.body());
            } else {
                System.err.println("Failed to save blockchain entry: " + response.statusCode() + " - " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error saving blockchain entry: " + e.getMessage());
            return null;
        }
    }

    public List<BlockchainEntry> loadBlockchainEntries(String userId) {
        if (!isConfigured()) {
            System.err.println("Supabase not configured");
            return List.of();
        }

        try {
            String url = String.format("%s/rest/v1/blockchain_entries?user_id=eq.%s&order=block_index.asc",
                    supabaseUrl, userId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseBlockchainEntries(response.body());
            } else {
                System.err.println("Failed to load blockchain entries: " + response.statusCode());
                return List.of();
            }
        } catch (Exception e) {
            System.err.println("Error loading blockchain entries: " + e.getMessage());
            return List.of();
        }
    }

    private String extractId(String jsonResponse) {
        Pattern pattern = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(jsonResponse);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private List<BlockchainEntry> parseBlockchainEntries(String jsonResponse) {
        List<BlockchainEntry> entries = new ArrayList<>();
        return entries;
    }

    public static class BlockchainEntry {
        public final String id;
        public final int blockIndex;
        public final String userId;
        public final String checkId;
        public final String previousHash;
        public final String currentHash;
        public final String blockData;
        public final String timestamp;

        public BlockchainEntry(String id, int blockIndex, String userId, String checkId,
                              String previousHash, String currentHash, String blockData, String timestamp) {
            this.id = id;
            this.blockIndex = blockIndex;
            this.userId = userId;
            this.checkId = checkId;
            this.previousHash = previousHash;
            this.currentHash = currentHash;
            this.blockData = blockData;
            this.timestamp = timestamp;
        }
    }
}
