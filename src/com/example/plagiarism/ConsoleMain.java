package com.example.plagiarism;

import java.io.File;
import java.time.LocalDate;

public class ConsoleMain {
    public static void main(String[] args) {
        String text1 = "AI is transforming education by enabling personalized learning at scale.";
        String text2 = "";

        Document doc1 = new Document("Doc1", System.getProperty("user.name"), LocalDate.now().toString(), text1);
        // Try to auto-discover original source for doc1
        SourceDiscoveryService discovery = new SourceDiscoveryService();
        SourceDiscoveryService.DiscoveredSource found = discovery.discoverOriginalSource(text1).orElse(null);
        String sourceUrl = "";
        if (found != null) {
            text2 = found.text();
            sourceUrl = found.url();
            System.out.println("Auto-discovered source: " + sourceUrl);
        } else {
            System.out.println("Could not auto-discover a source. Using a sample comparator text.");
            text2 = "AI enables personalized learning by adapting educational content.";
        }
        Document doc2 = new Document("OriginalSource", "web", LocalDate.now().toString(), text2, sourceUrl);

        double cosScore = PlagiarismChecker.computeSimilarity(doc1, doc2, "Cosine");
        double jacScore = PlagiarismChecker.computeSimilarity(doc1, doc2, "Jaccard");

        String cosVerdict = PlagiarismChecker.verdictFor(cosScore);
        String jacVerdict = PlagiarismChecker.verdictFor(jacScore);

        System.out.println("Document 1: \"" + text1 + "\"");
        System.out.println("Document 2: \"" + text2 + "\"");
        System.out.printf("→ Similarity (Cosine): %.1f%%%n", cosScore * 100.0);
        System.out.printf("→ Verdict: %s%n", cosVerdict);
        System.out.printf("→ Similarity (Jaccard): %.1f%%%n", jacScore * 100.0);
        System.out.printf("→ Verdict: %s%n", jacVerdict);

        Blockchain blockchain = new Blockchain();
        Block block = blockchain.addBlock(doc2);
        System.out.println("→ Blockchain Entry Created [Block #" + block.getIndex() + "]");

        try {
            File chainFile = new File(System.getProperty("user.home"), "plagiarism_chain.txt");
            StorageManager.saveChainToFile(blockchain, chainFile);
            System.out.println("Chain saved to: " + chainFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Failed to save chain: " + e.getMessage());
        }
    }
}
