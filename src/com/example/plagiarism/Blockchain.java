package com.example.plagiarism;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Blockchain {
    private final List<Block> chain = new ArrayList<>();

    public Blockchain() {
        // genesis block with empty document
        addBlock(new Document("GENESIS","system","", ""));
    }

    public synchronized Block addBlock(Document document) {
        String previousHash = chain.isEmpty() ? "" : chain.get(chain.size()-1).getHash();
        Block block = new Block(chain.size(), document, previousHash);
        chain.add(block);
        return block;
    }

    public synchronized boolean isChainValid() {
        if (chain.isEmpty()) return true;
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);
            if (!current.getPreviousHash().equals(previous.getHash())) {
                return false;
            }
            if (!current.computeHash().equals(current.getHash())) {
                return false;
            }
        }
        return true;
    }

    public synchronized List<Block> getBlocks() {
        return Collections.unmodifiableList(chain);
    }

    public synchronized void clearAndLoad(List<Block> blocks) {
        chain.clear();
        chain.addAll(blocks);
    }
}
