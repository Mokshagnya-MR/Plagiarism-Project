package com.example.plagiarism;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StorageManager {

    public static void saveChainToFile(Blockchain blockchain, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Block block : blockchain.getBlocks()) {
                Document d = block.getDocument();
                String line = String.join("|",
                        Integer.toString(block.getIndex()),
                        escape(block.getTimestamp()),
                        escape(block.getPreviousHash()),
                        escape(block.getHash()),
                        escape(d.getTitle()),
                        escape(d.getAuthor()),
                        escape(d.getSubmissionDate()),
                        escape(Double.toString(d.getPlagiarismScore())),
                        escape(d.getSourceUrl()),
                        escape(d.getText())
                );
                writer.write(line);
                writer.newLine();
            }
        }
    }

    public static Blockchain loadChainFromFile(File file) throws IOException {
        List<Block> loaded = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = split(line);
                if (parts.length < 10) continue;
                int index = Integer.parseInt(parts[0]);
                String timestamp = unescape(parts[1]);
                String previousHash = unescape(parts[2]);
                String hash = unescape(parts[3]);
                String title = unescape(parts[4]);
                String author = unescape(parts[5]);
                String date = unescape(parts[6]);
                double score = Double.parseDouble(unescape(parts[7]));
                String sourceUrl = unescape(parts[8]);
                String text = unescape(parts[9]);
                Document doc = new Document(title, author, date, text, sourceUrl);
                doc.setPlagiarismScore(score);
                Block block = new Block(index, timestamp, doc, previousHash, hash);
                loaded.add(block);
            }
        }
        Blockchain blockchain = new Blockchain();
        if (!loaded.isEmpty()) {
            // Replace with loaded blocks exactly
            blockchain.clearAndLoad(loaded);
        }
        return blockchain;
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("|", "\\|");
    }

    private static String unescape(String value) {
        if (value == null) return "";
        return value.replace("\\|", "|").replace("\\\\", "\\");
    }

    private static String[] split(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escape = false;
        for (char c : line.toCharArray()) {
            if (escape) {
                current.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '|') {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }
}
