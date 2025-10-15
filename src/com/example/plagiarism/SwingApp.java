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
    private final DefaultListModel<String> historyModel;
    private final JList<String> historyList;
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
        algorithmBox = new JComboBox<>(new String[]{"Ensemble", "Cosine", "Jaccard", "ShingleJaccard"});
        JButton checkButton = new JButton("Check Plagiarism");
        JButton sourceFindButton = new JButton("Auto-Find Source (Doc1)");
        JButton apiButton = new JButton("Use Mock API for Doc1");
        JButton saveButton = new JButton("Save Chain");
        JButton loadButton = new JButton("Load Chain");
        JButton compareSelectedButton = new JButton("Compare With Selected Block");
        controlPanel.add(new JLabel("Algorithm:"));
        controlPanel.add(algorithmBox);
        controlPanel.add(checkButton);
        controlPanel.add(apiButton);
        controlPanel.add(sourceFindButton);
        controlPanel.add(saveButton);
        controlPanel.add(loadButton);
        controlPanel.add(compareSelectedButton);

        resultLabel = new JLabel("Result: ");
        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD, 14f));

        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setPreferredSize(new Dimension(300, 0));

        root.add(inputPanel, BorderLayout.CENTER);
        root.add(controlPanel, BorderLayout.NORTH);
        root.add(resultLabel, BorderLayout.SOUTH);
        root.add(historyScroll, BorderLayout.EAST);

        frame.setContentPane(root);

        blockchain = new Blockchain();
        chainFile = new File(System.getProperty("user.home"), "plagiarism_chain.txt");

        checkButton.addActionListener(this::onCheck);
        apiButton.addActionListener(this::onApiDoc1);
        saveButton.addActionListener(e -> onSave());
        loadButton.addActionListener(e -> onLoad());
        sourceFindButton.addActionListener(e -> onAutoFindSourceForDoc1());
        compareSelectedButton.addActionListener(e -> onCompareWithSelected());
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

    private void onAutoFindSourceForDoc1() {
        String text1 = textArea1.getText();
        Document suspect = new Document("Doc1", System.getProperty("user.name"), LocalDate.now().toString(), text1);
        SourceFinder finder = new SourceFinder();
        String algorithm = (String) algorithmBox.getSelectedItem();
        var found = finder.autoFindOriginal(suspect, algorithm);
        if (found.isPresent()) {
            Document original = found.get();
            // Compare and update result label
            double score = PlagiarismChecker.computeSimilarity(suspect, original, algorithm);
            String verdict = PlagiarismChecker.verdictFor(score);
            resultLabel.setText(String.format("Auto Source Found: %.1f%% - %s | %s", score * 100.0, verdict, original.getSourceUrl()));

            // Store ORIGINAL in blockchain instead of suspect
            Block newBlock = blockchain.addBlock(original);
            historyModel.addElement(String.format("Block #%d | %.1f%% | %s | src=%s", newBlock.getIndex(), score * 100.0, verdict, original.getSourceUrl()));
        } else {
            // Ask user ONLY for website link as fallback and store if provided
            promptForAndStoreUrlOnly(suspect, algorithm);
        }
    }

    private void onCheck(ActionEvent e) {
        String text1 = textArea1.getText();
        String text2 = textArea2.getText();
        Document doc1 = new Document("Doc1", System.getProperty("user.name"), LocalDate.now().toString(), text1);
        Document doc2 = new Document("Doc2", System.getProperty("user.name"), LocalDate.now().toString(), text2);

        String algorithm = (String) algorithmBox.getSelectedItem();
        PlagiarismChecker.Result result = PlagiarismChecker.checkPlagiarism(doc1, doc2, algorithm);

        double percent = result.score() * 100.0;
        // Auto-find original for doc1 using SourceFinder
        SourceFinder finder = new SourceFinder();
        var found1 = finder.autoFindOriginal(doc1, algorithm);
        String src1 = found1.map(Document::getSourceUrl).orElse("");
        if (found1.isPresent()) {
            // Store ORIGINAL for doc1
            Block b1 = blockchain.addBlock(found1.get());
            historyModel.addElement(String.format("Block #%d | Doc1 src | %s", b1.getIndex(), src1));
        } else {
            boolean provided = promptForAndStoreUrlOnly(doc1, algorithm);
            if (!provided) {
                // Store suspect metadata (no source)
                Block b1 = blockchain.addBlock(new Document(doc1.getTitle(), doc1.getAuthor(), doc1.getSubmissionDate(), doc1.getText(), ""));
                historyModel.addElement(String.format("Block #%d | Doc1 src | %s", b1.getIndex(), "<none>"));
            }
        }

        // Optionally also attempt for doc2 if provided
        if (text2 != null && !text2.isBlank()) {
            var found2 = finder.autoFindOriginal(doc2, algorithm);
            String src2 = found2.map(Document::getSourceUrl).orElse("");
            if (found2.isPresent()) {
                Block b2 = blockchain.addBlock(found2.get());
                historyModel.addElement(String.format("Block #%d | Doc2 src | %s", b2.getIndex(), src2));
            }
        }

        resultLabel.setText(String.format("Result: %.1f%% - %s | Doc1 src: %s", percent, result.verdict(), src1.isBlank()?"<none>":src1));
    }

    private boolean promptForAndStoreSource(Document suspect, String algorithm) {
        SourceFinder finder = new SourceFinder();
        Object[] options = {"Enter URL", "Pick File", "Cancel"};
        int choice = JOptionPane.showOptionDialog(frame,
                "No source found automatically. Provide a website link or choose a file.",
                "Provide Original Source",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
        if (choice == 0) { // Enter URL
            String url = JOptionPane.showInputDialog(frame, "Enter source URL:", "https://");
            if (url != null && !url.isBlank()) {
                var docOpt = finder.buildDocumentFromUrl(url);
                if (docOpt.isPresent()) {
                    Document original = docOpt.get();
                    double score = PlagiarismChecker.computeSimilarity(suspect, original, algorithm);
                    String verdict = PlagiarismChecker.verdictFor(score);
                    resultLabel.setText(String.format("User URL: %.1f%% - %s | %s", score * 100.0, verdict, original.getSourceUrl()));
                    Block newBlock = blockchain.addBlock(original);
                    historyModel.addElement(String.format("Block #%d | %.1f%% | %s | src=%s", newBlock.getIndex(), score * 100.0, verdict, original.getSourceUrl()));
                    return true;
                } else {
                    JOptionPane.showMessageDialog(frame, "Failed to read from provided URL.");
                }
            }
        } else if (choice == 1) { // Pick File
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text Files", "txt", "md"));
            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                java.nio.file.Path path = chooser.getSelectedFile().toPath();
                var docOpt = SourceFinder.buildDocumentFromFile(path);
                if (docOpt.isPresent()) {
                    Document original = docOpt.get();
                    double score = PlagiarismChecker.computeSimilarity(suspect, original, algorithm);
                    String verdict = PlagiarismChecker.verdictFor(score);
                    resultLabel.setText(String.format("User File: %.1f%% - %s | %s", score * 100.0, verdict, original.getSourceUrl()));
                    Block newBlock = blockchain.addBlock(original);
                    historyModel.addElement(String.format("Block #%d | %.1f%% | %s | src=%s", newBlock.getIndex(), score * 100.0, verdict, original.getSourceUrl()));
                    return true;
                } else {
                    JOptionPane.showMessageDialog(frame, "Failed to read selected file.");
                }
            }
        }
        return false;
    }

    private boolean promptForAndStoreUrlOnly(Document suspect, String algorithm) {
        SourceFinder finder = new SourceFinder();
        String url = JOptionPane.showInputDialog(frame, "Enter source URL:", "https://");
        if (url != null && !url.isBlank()) {
            var docOpt = finder.buildDocumentFromUrl(url);
            if (docOpt.isPresent()) {
                Document original = docOpt.get();
                double score = PlagiarismChecker.computeSimilarity(suspect, original, algorithm);
                String verdict = PlagiarismChecker.verdictFor(score);
                resultLabel.setText(String.format("User URL: %.1f%% - %s | %s", score * 100.0, verdict, original.getSourceUrl()));
                Block newBlock = blockchain.addBlock(original);
                historyModel.addElement(String.format("Block #%d | %.1f%% | %s | src=%s", newBlock.getIndex(), score * 100.0, verdict, original.getSourceUrl()));
                return true;
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to read from provided URL.");
            }
        }
        return false;
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

    private void onCompareWithSelected() {
        int idx = historyList.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(frame, "Select a block from the list on the right.");
            return;
        }
        // Fetch corresponding block by parsing the displayed string
        String item = historyModel.get(idx);
        int hashIndex = item.indexOf('#');
        int pipeIndex = item.indexOf('|');
        if (hashIndex < 0 || pipeIndex < 0 || pipeIndex <= hashIndex) {
            JOptionPane.showMessageDialog(frame, "Could not parse selected block index.");
            return;
        }
        String numStr = item.substring(hashIndex + 1, pipeIndex).trim();
        int blockNumber;
        try { blockNumber = Integer.parseInt(numStr); } catch (Exception ex) { JOptionPane.showMessageDialog(frame, "Invalid block selection."); return; }

        List<Block> blocks = blockchain.getBlocks();
        if (blockNumber < 0 || blockNumber >= blocks.size()) {
            JOptionPane.showMessageDialog(frame, "Block out of range.");
            return;
        }

        String currentText = textArea1.getText();
        Document suspect = new Document("Doc1", System.getProperty("user.name"), java.time.LocalDate.now().toString(), currentText);
        Document original = blocks.get(blockNumber).getDocument();
        String algorithm = (String) algorithmBox.getSelectedItem();
        double score = PlagiarismChecker.computeSimilarity(suspect, original, algorithm);
        String verdict = PlagiarismChecker.verdictFor(score);
        resultLabel.setText(String.format("Selected Block #%d | %.1f%% - %s | %s", blockNumber, score * 100.0, verdict, original.getSourceUrl()));
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
