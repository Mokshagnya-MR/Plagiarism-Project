package com.example.plagiarism.ai;

import com.example.plagiarism.config.AppConfig;
import com.example.plagiarism.SourceDiscoveryService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AISourceDiscoveryService {

    private final HttpClient httpClient;
    private final AppConfig config;

    public AISourceDiscoveryService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.config = AppConfig.getInstance();
    }

    public Optional<SourceDiscoveryService.DiscoveredSource> discoverWithAI(String submissionText) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("AI API key not configured. Falling back to basic search.");
            return Optional.empty();
        }

        String model = config.get("ai.model", "anthropic");

        if ("anthropic".equalsIgnoreCase(model)) {
            return discoverWithAnthropic(submissionText, apiKey);
        } else if ("openai".equalsIgnoreCase(model)) {
            return discoverWithOpenAI(submissionText, apiKey);
        }

        return Optional.empty();
    }

    private String getApiKey() {
        String model = config.get("ai.model", "anthropic");
        if ("anthropic".equalsIgnoreCase(model)) {
            return config.getAnthropicKey();
        } else {
            return config.getOpenAIKey();
        }
    }

    private Optional<SourceDiscoveryService.DiscoveredSource> discoverWithAnthropic(String text, String apiKey) {
        try {
            String prompt = buildSourceDiscoveryPrompt(text);
            String jsonBody = String.format(
                "{\"model\":\"claude-3-5-sonnet-20241022\",\"max_tokens\":1024,\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
                escapeJson(prompt)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.anthropic.com/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                return parseAIResponse(responseBody);
            } else {
                System.err.println("AI API error: " + response.statusCode());
                return Optional.empty();
            }
        } catch (Exception e) {
            System.err.println("AI discovery failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<SourceDiscoveryService.DiscoveredSource> discoverWithOpenAI(String text, String apiKey) {
        try {
            String prompt = buildSourceDiscoveryPrompt(text);
            String jsonBody = String.format(
                "{\"model\":\"gpt-4\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"temperature\":0.3}",
                escapeJson(prompt)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                return parseAIResponse(responseBody);
            } else {
                System.err.println("AI API error: " + response.statusCode());
                return Optional.empty();
            }
        } catch (Exception e) {
            System.err.println("AI discovery failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    private String buildSourceDiscoveryPrompt(String text) {
        return "Analyze the following text and suggest 3-5 specific search queries that would help find the original source online. " +
               "Focus on distinctive phrases, quotes, or unique terminology. " +
               "Format your response as: QUERY1: <query>\\nQUERY2: <query>\\n...\\n\\n" +
               "Text to analyze:\\n" + text.substring(0, Math.min(text.length(), 1000));
    }

    private Optional<SourceDiscoveryService.DiscoveredSource> parseAIResponse(String response) {
        Pattern queryPattern = Pattern.compile("QUERY\\d+:\\s*(.+)");
        Matcher matcher = queryPattern.matcher(response);

        if (matcher.find()) {
            String suggestedQuery = matcher.group(1).trim();
            return Optional.of(new SourceDiscoveryService.DiscoveredSource(
                "AI-suggested query: " + suggestedQuery,
                suggestedQuery
            ));
        }

        return Optional.empty();
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
