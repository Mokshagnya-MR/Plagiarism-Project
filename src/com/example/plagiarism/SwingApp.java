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
        JButton sourceFindButton = new JButton("Auto-Find Source (Doc1)");
        JButton apiButton = new JButton("Use Mock API for Doc1");
        JButton saveButton = new JButton("Save Chain");
        JButton loadButton = new JButton("Load Chain");
        controlPanel.add(new JLabel("Algorithm:"));
        controlPanel.add(algorithmBox);
        controlPanel.add(checkButton);
        controlPanel.add(apiButton);
        controlPanel.add(sourceFindButton);
        controlPanel.add(saveButton);
        controlPanel.add(loadButton);

        resultLabel = new JLabel("Result: ");
        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD, 14f));

        historyModel = new DefaultListModel<>();
        JList<String> historyList = new JList<>(historyModel);
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
            // Ask user to provide a source file as fallback
            int choice = JOptionPane.showConfirmDialog(frame, "No source found automatically. Provide a source file?", "Fallback", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
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
                        resultLabel.setText(String.format("User Source: %.1f%% - %s | %s", score * 100.0, verdict, original.getSourceUrl()));
                        Block newBlock = blockchain.addBlock(original);
                        historyModel.addElement(String.format("Block #%d | %.1f%% | %s | src=%s", newBlock.getIndex(), score * 100.0, verdict, original.getSourceUrl()));
                    } else {
                        JOptionPane.showMessageDialog(frame, "Failed to read selected file.");
                    }
                }
            }
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
        resultLabel.setText(String.format("Result: %.1f%% - %s", percent, result.verdict()));

        // Record suspect doc result (metadata only) but do NOT store as original. Users can use Auto-Find Source to store original.
        Block newBlock = blockchain.addBlock(new Document(doc1.getTitle(), doc1.getAuthor(), doc1.getSubmissionDate(), doc1.getText(), ""));
        historyModel.addElement(String.format("Block #%d | %.1f%% | %s | src=%s", newBlock.getIndex(), percent, result.verdict(), ""));
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
