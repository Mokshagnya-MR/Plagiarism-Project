package com.example.plagiarism.export;

import com.example.plagiarism.Block;
import com.example.plagiarism.PlagiarismChecker;
import com.example.plagiarism.Document;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ResultExporter {

    public static void exportToJSON(PlagiarismChecker.Result result,
                                    Document submission,
                                    Document source,
                                    File outputFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("{\n");
            writer.write("  \"timestamp\": \"" + LocalDateTime.now() + "\",\n");
            writer.write("  \"submission\": {\n");
            writer.write("    \"title\": \"" + escapeJson(submission.getTitle()) + "\",\n");
            writer.write("    \"author\": \"" + escapeJson(submission.getAuthor()) + "\",\n");
            writer.write("    \"content_length\": " + submission.getText().length() + "\n");
            writer.write("  },\n");
            writer.write("  \"source\": {\n");
            writer.write("    \"title\": \"" + escapeJson(source.getTitle()) + "\",\n");
            writer.write("    \"url\": \"" + escapeJson(source.getSourceUrl()) + "\",\n");
            writer.write("    \"content_length\": " + source.getText().length() + "\n");
            writer.write("  },\n");
            writer.write("  \"result\": {\n");
            writer.write("    \"similarity_score\": " + result.score() + ",\n");
            writer.write("    \"similarity_percentage\": " + (result.score() * 100) + ",\n");
            writer.write("    \"verdict\": \"" + result.verdict() + "\"\n");
            writer.write("  }\n");
            writer.write("}\n");
        }
    }

    public static void exportToCSV(List<PlagiarismChecker.PairwiseResult> results, File outputFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("Document A,Document B,Similarity Score,Similarity %,Verdict\n");

            for (PlagiarismChecker.PairwiseResult result : results) {
                writer.write(String.format("\"%s\",\"%s\",%.4f,%.2f,\"%s\"\n",
                        escapeCSV(result.a().getTitle()),
                        escapeCSV(result.b().getTitle()),
                        result.score(),
                        result.score() * 100,
                        result.verdict()));
            }
        }
    }

    public static void exportBlockchainToJSON(List<Block> blocks, File outputFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("{\n");
            writer.write("  \"blockchain\": [\n");

            for (int i = 0; i < blocks.size(); i++) {
                Block block = blocks.get(i);
                writer.write("    {\n");
                writer.write("      \"index\": " + block.getIndex() + ",\n");
                writer.write("      \"timestamp\": \"" + block.getTimestamp() + "\",\n");
                writer.write("      \"hash\": \"" + block.getHash() + "\",\n");
                writer.write("      \"previous_hash\": \"" + block.getPreviousHash() + "\",\n");
                writer.write("      \"document\": {\n");
                writer.write("        \"title\": \"" + escapeJson(block.getDocument().getTitle()) + "\",\n");
                writer.write("        \"author\": \"" + escapeJson(block.getDocument().getAuthor()) + "\",\n");
                writer.write("        \"score\": " + block.getDocument().getPlagiarismScore() + "\n");
                writer.write("      }\n");
                writer.write("    }");
                if (i < blocks.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }

            writer.write("  ],\n");
            writer.write("  \"is_valid\": true,\n");
            writer.write("  \"total_blocks\": " + blocks.size() + "\n");
            writer.write("}\n");
        }
    }

    public static void exportDetailedReport(PlagiarismChecker.Result result,
                                           Document submission,
                                           Document source,
                                           String algorithm,
                                           File outputFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("PLAGIARISM DETECTION REPORT\n");
            writer.write("=" .repeat(80) + "\n\n");

            writer.write("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n\n");

            writer.write("SUBMISSION DETAILS\n");
            writer.write("-" .repeat(80) + "\n");
            writer.write("Title: " + submission.getTitle() + "\n");
            writer.write("Author: " + submission.getAuthor() + "\n");
            writer.write("Submission Date: " + submission.getSubmissionDate() + "\n");
            writer.write("Content Length: " + submission.getText().length() + " characters\n\n");

            writer.write("SOURCE DETAILS\n");
            writer.write("-" .repeat(80) + "\n");
            writer.write("Title: " + source.getTitle() + "\n");
            if (!source.getSourceUrl().isBlank()) {
                writer.write("URL: " + source.getSourceUrl() + "\n");
            }
            writer.write("Content Length: " + source.getText().length() + " characters\n\n");

            writer.write("ANALYSIS RESULTS\n");
            writer.write("-" .repeat(80) + "\n");
            writer.write("Algorithm Used: " + algorithm + "\n");
            writer.write("Similarity Score: " + String.format("%.4f", result.score()) + "\n");
            writer.write("Similarity Percentage: " + String.format("%.2f%%", result.score() * 100) + "\n");
            writer.write("Verdict: " + result.verdict() + "\n\n");

            writer.write("INTERPRETATION\n");
            writer.write("-" .repeat(80) + "\n");
            if (result.verdict().equals("Safe")) {
                writer.write("The similarity is below 30%, indicating low overlap. This is generally acceptable.\n");
            } else if (result.verdict().equals("Moderate")) {
                writer.write("The similarity is between 30-70%, indicating moderate overlap. Further review recommended.\n");
            } else {
                writer.write("The similarity exceeds 70%, indicating high overlap. This requires immediate attention.\n");
            }
        }
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private static String escapeCSV(String text) {
        if (text == null) return "";
        return text.replace("\"", "\"\"");
    }
}
