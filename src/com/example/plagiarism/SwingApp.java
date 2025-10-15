package com.example.plagiarism;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SwingApp {
    private final JFrame frame;
    private final JTextArea textArea1;
    private final JTextArea textArea2;
    private final JComboBox<String> algorithmBox;
    private final JLabel resultLabel;
    private final JLabel sourceUrlLabel;
    private final DefaultListModel<String> historyModel;
    private final Blockchain blockchain;
    private final File chainFile;

    public SwingApp() {
        frame = new JFrame("Plagiarism Detection System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel inputPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        textArea1 = new JTextArea();
        textArea2 = new JTextArea();
        inputPanel.add(wrapWithToolbar(textArea1, "Document 1"));
        inputPanel.add(wrapWithToolbar(textArea2, "Document 2"));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        algorithmBox = new JComboBox<>(new String[]{"Cosine", "Jaccard"});
        JButton checkButton = new JButton("Check Plagiarism");
        JButton apiButton = new JButton("Use Mock API for Doc1");
        JButton autoButton = new JButton("Auto Find Source + Check");
        JButton useLastSourceButton = new JButton("Use Last Saved Source");
        JButton saveButton = new JButton("Save Chain");
        JButton loadButton = new JButton("Load Chain");
        controlPanel.add(new JLabel("Algorithm:"));
        controlPanel.add(algorithmBox);
        controlPanel.add(checkButton);
        controlPanel.add(apiButton);
        controlPanel.add(autoButton);
        controlPanel.add(useLastSourceButton);
        controlPanel.add(saveButton);
        controlPanel.add(loadButton);

        resultLabel = new JLabel("Result: ");
        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD, 14f));
        sourceUrlLabel = new JLabel("Source: -");

        historyModel = new DefaultListModel<>();
        JList<String> historyList = new JList<>(historyModel);
        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setPreferredSize(new Dimension(300, 0));

        root.add(inputPanel, BorderLayout.CENTER);
        root.add(controlPanel, BorderLayout.NORTH);
        JPanel southPanel = new JPanel(new GridLayout(2, 1));
        southPanel.add(resultLabel);
        southPanel.add(sourceUrlLabel);
        root.add(southPanel, BorderLayout.SOUTH);
        root.add(historyScroll, BorderLayout.EAST);

        frame.setContentPane(root);

        blockchain = new Blockchain();
        chainFile = new File(System.getProperty("user.home"), "plagiarism_chain.txt");

        checkButton.addActionListener(this::onCheck);
        apiButton.addActionListener(this::onApiDoc1);
        autoButton.addActionListener(this::onAutoFindSource);
        useLastSourceButton.addActionListener(e -> onUseLastSource());
        saveButton.addActionListener(e -> onSave());
        loadButton.addActionListener(e -> onLoad());
    }

    private JPanel wrapWithToolbar(JTextArea area, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(title), BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(area);
        panel.add(scrollPane, BorderLayout.CENTER);
        JButton upload = new JButton("Upload...");
        upload.addActionListener(e -> onUpload(area));
        panel.add(upload, BorderLayout.SOUTH);
        return panel;
    }

    private void onUpload(JTextArea target) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt", "md"));
        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                String content = Files.readString(file.toPath());
                target.setText(content);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Failed to read file: " + ex.getMessage());
            }
        }
    }

    private void onApiDoc1(ActionEvent e) {
        String text1 = textArea1.getText();
        PlagiarismAPIClient api = new PlagiarismAPIClient();
        double score = api.checkPlagiarismAPI(text1);
        String verdict = PlagiarismChecker.verdictFor(score);
        resultLabel.setText(String.format("Result (API Doc1): %.1f%% - %s", score * 100.0, verdict));
    }

    private void onCheck(ActionEvent e) {
        String text1 = textArea1.getText();
        String text2 = textArea2.getText();
        Document doc1 = new Document("Doc1", System.getProperty("user.name"), LocalDate.now().toString(), text1);
        Document doc2 = new Document("Doc2", System.getProperty("user.name"), LocalDate.now().toString(), text2);

        String algorithm = (String) algorithmBox.getSelectedItem();
        PlagiarismChecker.Result result = PlagiarismChecker.checkPlagiarism(doc1, doc2, algorithm);

        double percent = result.score() * 100.0;
        resultLabel.setText(String.format("Result: %.1f%% - %s", percent, result.verdict()));

        // Record original source instead when provided in Doc2
        Block newBlock = blockchain.addBlock(doc2);
        historyModel.addElement(String.format("Block #%d | %.1f%% | %s", newBlock.getIndex(), percent, result.verdict()));
    }

    private void onAutoFindSource(ActionEvent e) {
        String submission = textArea1.getText();
        if (submission == null || submission.isBlank()) {
            JOptionPane.showMessageDialog(frame, "Please provide the submission text in Document 1.");
            return;
        }
        resultLabel.setText("Result: Searching web for original source...");
        sourceUrlLabel.setText("Source: -");
        SourceDiscoveryService discovery = new SourceDiscoveryService();
        SourceDiscoveryService.DiscoveredSource found = discovery.discoverOriginalSource(submission).orElse(null);

        String originalText;
        String sourceUrl = "";
        if (found != null) {
            originalText = found.text();
            sourceUrl = found.url();
        } else {
            int choice = JOptionPane.showConfirmDialog(frame,
                    "Could not auto-find the original source online. Do you want to provide a source file?",
                    "Provide Source",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                resultLabel.setText("Result: No source provided.");
                return;
            }
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text Files", "txt", "md"));
            int res = chooser.showOpenDialog(frame);
            if (res != JFileChooser.APPROVE_OPTION) {
                resultLabel.setText("Result: No source provided.");
                return;
            }
            File file = chooser.getSelectedFile();
            try {
                originalText = java.nio.file.Files.readString(file.toPath());
                sourceUrl = file.getAbsolutePath();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Failed to read file: " + ex.getMessage());
                return;
            }
        }

        textArea2.setText(originalText);
        Document docSubmission = new Document("Submission", System.getProperty("user.name"), LocalDate.now().toString(), submission);
        Document docOriginal = new Document("OriginalSource", "web", LocalDate.now().toString(), originalText, sourceUrl);
        String algorithm = (String) algorithmBox.getSelectedItem();
        PlagiarismChecker.Result result = PlagiarismChecker.checkPlagiarism(docSubmission, docOriginal, algorithm);
        double percent = result.score() * 100.0;
        resultLabel.setText(String.format("Result: %.1f%% - %s", percent, result.verdict()));
        sourceUrlLabel.setText("Source: " + (sourceUrl == null || sourceUrl.isBlank() ? "(local file)" : sourceUrl));

        // Save the original source in the blockchain
        Block newBlock = blockchain.addBlock(docOriginal);
        historyModel.addElement(String.format("Block #%d | %.1f%% | %s | Saved original", newBlock.getIndex(), percent, result.verdict()));
    }

    private void onUseLastSource() {
        List<Block> blocks = blockchain.getBlocks();
        if (blocks.size() <= 1) { // only genesis
            JOptionPane.showMessageDialog(frame, "No saved source in blockchain yet.");
            return;
        }
        Block last = blocks.get(blocks.size() - 1);
        Document d = last.getDocument();
        if (d.getText() == null || d.getText().isBlank()) {
            JOptionPane.showMessageDialog(frame, "Last block has no source text.");
            return;
        }
        textArea2.setText(d.getText());
        if (d.getSourceUrl() != null && !d.getSourceUrl().isBlank()) {
            sourceUrlLabel.setText("Source: " + d.getSourceUrl());
        } else {
            sourceUrlLabel.setText("Source: (from blockchain)");
        }
    }

    private void onSave() {
        try {
            StorageManager.saveChainToFile(blockchain, chainFile);
            JOptionPane.showMessageDialog(frame, "Blockchain saved to: " + chainFile.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Save failed: " + ex.getMessage());
        }
    }

    private void onLoad() {
        try {
            Blockchain loaded = StorageManager.loadChainFromFile(chainFile);
            if (!loaded.isChainValid()) {
                JOptionPane.showMessageDialog(frame, "Loaded chain invalid!");
                return;
            }
            historyModel.clear();
            for (Block b : loaded.getBlocks()) {
                historyModel.addElement(String.format("Block #%d | %s | %s", b.getIndex(), b.getTimestamp(), b.getDocument().toString()));
            }
            JOptionPane.showMessageDialog(frame, "Blockchain loaded.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Load failed: " + ex.getMessage());
        }
    }

    public void displayForm() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Headless environment detected. Running console demo...");
            ConsoleMain.main(args);
            return;
        }
        new SwingApp().displayForm();
    }
}
