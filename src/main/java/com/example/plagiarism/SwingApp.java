package com.example.plagiarism;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SwingApp {

    private final JFrame frame;
    private final JTextArea textAreaA;
    private final JTextArea textAreaB;
    private final JLabel resultLabel;
    private final JTextArea historyArea;
    private final Blockchain blockchain;

    public SwingApp() {
        this.blockchain = initBlockchain();
        this.frame = new JFrame("Plagiarism Detection System");
        this.textAreaA = new JTextArea(10, 40);
        this.textAreaB = new JTextArea(10, 40);
        this.resultLabel = new JLabel("\u00A0");
        this.historyArea = new JTextArea(10, 85);
        historyArea.setEditable(false);
        buildUI();
        refreshHistory();
    }

    private Blockchain initBlockchain() {
        try {
            Blockchain chain = StorageManager.loadChainFromFile();
            if (!chain.isChainValid()) {
                return new Blockchain();
            }
            return chain;
        } catch (IOException e) {
            return new Blockchain();
        }
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10,10,10,10));

        JPanel inputs = new JPanel(new GridLayout(1,2,8,8));
        inputs.add(wrap("Document A", new JScrollPane(textAreaA)));
        inputs.add(wrap("Document B", new JScrollPane(textAreaB)));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton loadABtn = new JButton("Load File A");
        loadABtn.addActionListener(e -> loadFileInto(textAreaA));
        JButton loadBBtn = new JButton("Load File B");
        loadBBtn.addActionListener(e -> loadFileInto(textAreaB));
        JButton checkBtn = new JButton("Check Plagiarism");
        checkBtn.addActionListener(this::onCheck);
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> { textAreaA.setText(""); textAreaB.setText(""); resultLabel.setText("\u00A0"); });
        JButton refreshBtn = new JButton("Refresh History");
        refreshBtn.addActionListener(e -> refreshHistory());
        controls.add(loadABtn);
        controls.add(loadBBtn);
        controls.add(checkBtn);
        controls.add(clearBtn);
        controls.add(refreshBtn);

        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.add(resultLabel, BorderLayout.CENTER);

        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.add(new JLabel("History (Blockchain)"), BorderLayout.NORTH);
        historyPanel.add(new JScrollPane(historyArea), BorderLayout.CENTER);

        root.add(inputs, BorderLayout.NORTH);
        root.add(controls, BorderLayout.CENTER);
        root.add(resultPanel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, root, historyPanel);
        split.setResizeWeight(0.6);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(split);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    private JPanel wrap(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(title), BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private void loadFileInto(JTextArea targetArea) {
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(frame);
        if (res == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            try {
                Path path = chooser.getSelectedFile().toPath();
                String content = Files.readString(path, StandardCharsets.UTF_8);
                targetArea.setText(content);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Failed to read file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onCheck(ActionEvent event) {
        String a = textAreaA.getText();
        String b = textAreaB.getText();
        Document d1 = Document.ofToday("Doc A", System.getProperty("user.name", "user"), a);
        Document d2 = Document.ofToday("Doc B", System.getProperty("user.name", "user"), b);

        PlagiarismChecker checker = new PlagiarismChecker();
        PlagiarismChecker.PlagiarismResult result = checker.checkPlagiarism(d1, d2);

        double cosinePercent = Math.round(result.cosine * 10000.0) / 100.0;
        double jaccardPercent = Math.round(result.jaccard * 10000.0) / 100.0;

        String verdictText = switch (result.verdict) {
            case SAFE -> "Safe";
            case MODERATE -> "Moderate";
            case HIGH -> "High";
        };

        resultLabel.setText(String.format("Similarity (Cosine): %.2f%%  |  (Jaccard): %.2f%%  -> Verdict: %s", cosinePercent, jaccardPercent, verdictText));

        // Add block to blockchain (store combined score)
        Document record = Document.ofToday("PlagiarismCheck", System.getProperty("user.name", "user"), a + "\n---\n" + b);
        record.setPlagiarismScore(Math.max(result.cosine, result.jaccard));
        Block added = blockchain.addBlock(record);
        try {
            StorageManager.saveChainToFile(blockchain);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Failed to save blockchain: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        refreshHistory();
        JOptionPane.showMessageDialog(frame, "Blockchain Entry Created [Block #" + added.getIndex() + "]", "Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshHistory() {
        StringBuilder sb = new StringBuilder();
        for (Block b : blockchain.getChain()) {
            sb.append("#").append(b.getIndex()).append("  ")
              .append(b.getTimestamp()).append("\n")
              .append("Prev: ").append(b.getPreviousHash()).append("\n")
              .append("Hash: ").append(b.getHash()).append("\n");
            if (b.getDocument() != null) {
                sb.append("Title: ").append(b.getDocument().getTitle()).append("  ")
                  .append("Score: ")
                  .append(String.format("%.3f", b.getDocument().getPlagiarismScore()))
                  .append("\n");
            }
            sb.append("\n");
        }
        historyArea.setText(sb.toString());
    }

    public void displayForm() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public static void main(String[] args) {
        new SwingApp().displayForm();
    }
}
