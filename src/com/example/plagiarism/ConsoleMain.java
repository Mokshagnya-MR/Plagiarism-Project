package com.example.plagiarism;

import java.io.File;
import java.time.LocalDate;

public class ConsoleMain {
    public static void main(String[] args) {
        String text1 = "AI is transforming education";
        String text2 = "AI helps improve modern learning";

        Document doc1 = new Document("Doc1", System.getProperty("user.name"), LocalDate.now().toString(), text1);
        Document doc2 = new Document("Doc2", System.getProperty("user.name"), LocalDate.now().toString(), text2);

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

        // Try auto-finding original for doc1 and store ORIGINAL in blockchain if found
        SourceFinder finder = new SourceFinder();
        String algorithm = "Ensemble";
        var originalOpt = finder.autoFindOriginal(doc1, algorithm);
        if (originalOpt.isPresent()) {
            Document original = originalOpt.get();
            Block block = blockchain.addBlock(original);
            System.out.println("→ Original source stored to blockchain [Block #" + block.getIndex() + "]");
            System.out.println("   Source URL: " + original.getSourceUrl());
        } else {
            // Console fallback: read from env SOURCE_URL (file fallback removed)
            String url = System.getenv("SOURCE_URL");
            boolean stored = false;
            if (url != null && !url.isBlank()) {
                var fromUrl = finder.buildDocumentFromUrl(url);
                if (fromUrl.isPresent()) {
                    Document original = fromUrl.get();
                    Block block = blockchain.addBlock(original);
                    System.out.println("→ User-provided URL stored as original [Block #" + block.getIndex() + "]");
                    System.out.println("   Source URL: " + original.getSourceUrl());
                    stored = true;
                }
            }
            if (!stored) {
                // Fall back: store suspect metadata only (sourceUrl empty)
                Block block = blockchain.addBlock(new Document(doc1.getTitle(), doc1.getAuthor(), doc1.getSubmissionDate(), doc1.getText(), ""));
                System.out.println("→ No source found. Stored suspect document metadata [Block #" + block.getIndex() + "]");
            }
        }

        try {
            File chainFile = new File(System.getProperty("user.home"), "plagiarism_chain.txt");
            StorageManager.saveChainToFile(blockchain, chainFile);
            System.out.println("Chain saved to: " + chainFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Failed to save chain: " + e.getMessage());
        }
    }
}
