package com.example.plagiarism;

import java.io.File;
import java.time.LocalDate;

public class ConsoleMain {
    public static void main(String[] args) {
        String text1 = "AI is transforming education";
        String text2 = "AI helps improve modern learning";

        Document doc1 = new Document("Doc1", System.getProperty("user.name"), LocalDate.now().toString(), text1);
        Document doc2 = new Document("Doc2", System.getProperty("user.name"), LocalDate.now().toString(), text2);

        SimilarityAlgorithm cosine = new CosineSimilarity();
        SimilarityAlgorithm jaccard = new JaccardIndex();

        double cosScore = PlagiarismChecker.computeSimilarity(doc1, doc2, cosine);
        double jacScore = PlagiarismChecker.computeSimilarity(doc1, doc2, jaccard);

        String cosVerdict = PlagiarismChecker.verdictFor(cosScore);
        String jacVerdict = PlagiarismChecker.verdictFor(jacScore);

        System.out.println("Document 1: \"" + text1 + "\"");
        System.out.println("Document 2: \"" + text2 + "\"");
        System.out.printf("→ Similarity (Cosine): %.1f%%%n", cosScore * 100.0);
        System.out.printf("→ Verdict: %s%n", cosVerdict);
        System.out.printf("→ Similarity (Jaccard): %.1f%%%n", jacScore * 100.0);
        System.out.printf("→ Verdict: %s%n", jacVerdict);

        Blockchain blockchain = new Blockchain();
        Block block = blockchain.addBlock(doc1);
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
