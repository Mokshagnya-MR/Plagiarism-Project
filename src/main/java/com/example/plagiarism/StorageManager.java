package com.example.plagiarism;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class StorageManager {

    public static final String DEFAULT_DIR = "data";
    public static final String DEFAULT_CHAIN_FILE = DEFAULT_DIR + "/chain.ndjson";

    public static void saveChainToFile(Blockchain blockchain) throws IOException {
        saveChainToFile(blockchain, DEFAULT_CHAIN_FILE);
    }

    public static void saveChainToFile(Blockchain blockchain, String filepath) throws IOException {
        Path path = Paths.get(filepath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            // Write header line
            writer.write("{\"meta\":{\"savedAt\":\"" + escape(Instant.now().toString()) + "\"}}\n");
            for (Block block : blockchain.getChain()) {
                writer.write(blockToJson(block));
                writer.write("\n");
            }
        }
    }

    public static Blockchain loadChainFromFile() throws IOException {
        return loadChainFromFile(DEFAULT_CHAIN_FILE);
    }

    public static Blockchain loadChainFromFile(String filepath) throws IOException {
        Path path = Paths.get(filepath);
        if (!Files.exists(path)) {
            return new Blockchain();
        }
        List<Block> blocks = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (line.contains("\"meta\"")) continue; // skip header
                Block block = jsonToBlock(line.trim());
                if (block != null) {
                    blocks.add(block);
                }
            }
        }
        if (blocks.isEmpty()) {
            return new Blockchain();
        }
        return new Blockchain(blocks);
    }

    private static String blockToJson(Block block) {
        Document d = block.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"index\":").append(block.getIndex()).append(',');
        sb.append("\"timestamp\":\"").append(escape(block.getTimestamp())).append("\",");
        sb.append("\"previousHash\":\"").append(escape(block.getPreviousHash())).append("\",");
        sb.append("\"hash\":\"").append(escape(block.getHash())).append("\",");
        sb.append("\"document\":{");
        if (d != null) {
            sb.append("\"title\":\"").append(escape(d.getTitle())).append("\",");
            sb.append("\"author\":\"").append(escape(d.getAuthor())).append("\",");
            sb.append("\"submissionDate\":\"").append(escape(d.getSubmissionDate())).append("\",");
            sb.append("\"text\":\"").append(escape(shortText(d.getText()))).append("\",");
            sb.append("\"plagiarismScore\":").append(d.getPlagiarismScore());
        }
        sb.append("}");
        sb.append("}");
        return sb.toString();
    }

    private static Block jsonToBlock(String json) {
        try {
            int index = parseInt(json, "\"index\":");
            String timestamp = parseString(json, "\"timestamp\":\"");
            String previousHash = parseString(json, "\"previousHash\":\"");
            String hash = parseString(json, "\"hash\":\"");

            String docTitle = parseString(json, "\"title\":\"");
            String docAuthor = parseString(json, "\"author\":\"");
            String docSubmissionDate = parseString(json, "\"submissionDate\":\"");
            String docText = parseString(json, "\"text\":\"");
            Double docScore = parseDouble(json, "\"plagiarismScore\":");

            Document d = new Document(docTitle, docAuthor, docSubmissionDate, docText);
            if (docScore != null) d.setPlagiarismScore(docScore);

            // Recreate block. Its computeHash() may produce same value if fields match
            return new Block(index, timestamp, d, previousHash);
        } catch (Exception e) {
            return null;
        }
    }

    private static int parseInt(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return 0;
        int start = i + key.length();
        int end = findNumberEnd(json, start);
        return Integer.parseInt(json.substring(start, end));
    }

    private static Double parseDouble(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return null;
        int start = i + key.length();
        int end = findNumberEnd(json, start);
        return Double.parseDouble(json.substring(start, end));
    }

    private static int findNumberEnd(String s, int start) {
        int end = start;
        while (end < s.length()) {
            char c = s.charAt(end);
            if ((c >= '0' && c <= '9') || c == '.' || c == '-' ) {
                end++;
            } else {
                break;
            }
        }
        return end;
    }

    private static String parseString(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return "";
        int start = i + key.length();
        int end = json.indexOf('"', start);
        while (end > 0 && json.charAt(end - 1) == '\\') { // handle escaped quotes
            end = json.indexOf('"', end + 1);
        }
        if (end < 0) end = json.length();
        return unescape(json.substring(start, end));
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "").replace("\t", " ");
    }

    private static String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String shortText(String s) {
        if (s == null) return "";
        int max = 512;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
