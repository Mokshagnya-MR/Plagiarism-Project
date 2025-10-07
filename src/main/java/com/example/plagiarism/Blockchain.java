package com.example.plagiarism;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Blockchain {
    private final List<Block> chain;

    public Blockchain() {
        this.chain = new ArrayList<>();
        this.chain.add(createGenesisBlock());
    }

    public Blockchain(List<Block> existingChain) {
        if (existingChain == null || existingChain.isEmpty()) {
            this.chain = new ArrayList<>();
            this.chain.add(createGenesisBlock());
        } else {
            this.chain = new ArrayList<>(existingChain);
        }
    }

    private Block createGenesisBlock() {
        Document genesisDoc = new Document("Genesis", "System", Instant.now().toString(), "Genesis block");
        genesisDoc.setPlagiarismScore(0.0);
        return new Block(0, Instant.now().toString(), genesisDoc, "0");
    }

    public synchronized Block addBlock(Document document) {
        int newIndex = chain.size();
        String previousHash = chain.get(chain.size() - 1).getHash();
        Block newBlock = Block.create(newIndex, document, previousHash);
        chain.add(newBlock);
        return newBlock;
    }

    public synchronized boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block prev = chain.get(i - 1);
            if (!current.getPreviousHash().equals(prev.getHash())) {
                return false;
            }
            if (!current.computeHash().equals(current.getHash())) {
                return false;
            }
        }
        return true;
    }

    public List<Block> getChain() {
        return Collections.unmodifiableList(chain);
    }
}
