package com.example.plagiarism.ui;

import com.example.plagiarism.*;
import com.example.plagiarism.config.AppConfig;
import com.example.plagiarism.export.ResultExporter;
import com.example.plagiarism.supabase.SupabaseClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EnhancedSwingApp {
    private final JFrame frame;
    private JTextArea textArea1;
    private JTextArea textArea2;
    private JComboBox<String> algorithmBox;
    private JLabel resultLabel;
    private JLabel sourceUrlLabel;
    private JProgressBar progressBar;
    private DefaultTableModel historyTableModel;
    private final Blockchain blockchain;
    private final File chainFile;
    private final SupabaseClient supabaseClient;
    private final AppConfig config;

    public EnhancedSwingApp() {
        config = AppConfig.getInstance();
        frame = new JFrame("Advanced Plagiarism Detection System v2.0");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);

        JMenuBar menuBar = createMenuBar();
        frame.setJMenuBar(menuBar);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(15, 15, 15, 15));
        root.setBackground(new Color(245, 245, 247));

        JPanel inputPanel = createInputPanel();
        JPanel controlPanel = createControlPanel();
        JPanel resultPanel = createResultPanel();
        JPanel historyPanel = createHistoryPanel();

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, historyPanel);
        mainSplit.setResizeWeight(0.7);
        mainSplit.setDividerLocation(800);

        root.add(controlPanel, BorderLayout.NORTH);
        root.add(mainSplit, BorderLayout.CENTER);
        root.add(resultPanel, BorderLayout.SOUTH);

        frame.setContentPane(root);

        blockchain = new Blockchain();
        chainFile = new File(System.getProperty("user.home"), "plagiarism_chain.txt");
        supabaseClient = new SupabaseClient();

        if (supabaseClient.isConfigured()) {
            System.out.println("Supabase integration enabled");
        }
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem saveItem = new JMenuItem("Save Blockchain");
        JMenuItem loadItem = new JMenuItem("Load Blockchain");
        JMenuItem exportJSONItem = new JMenuItem("Export to JSON");
        JMenuItem exportCSVItem = new JMenuItem("Export to CSV");
        JMenuItem exportReportItem = new JMenuItem("Export Detailed Report");
        JMenuItem exitItem = new JMenuItem("Exit");

        saveItem.addActionListener(e -> onSave());
        loadItem.addActionListener(e -> onLoad());
        exportJSONItem.addActionListener(e -> exportResults("json"));
        exportCSVItem.addActionListener(e -> exportResults("csv"));
        exportReportItem.addActionListener(e -> exportResults("report"));
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(saveItem);
        fileMenu.add(loadItem);
        fileMenu.addSeparator();
        fileMenu.add(exportJSONItem);
        fileMenu.add(exportCSVItem);
        fileMenu.add(exportReportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem settingsItem = new JMenuItem("Settings");
        JMenuItem validateChainItem = new JMenuItem("Validate Blockchain");
        JMenuItem clearHistoryItem = new JMenuItem("Clear History");

        settingsItem.addActionListener(e -> showSettings());
        validateChainItem.addActionListener(e -> validateBlockchain());
        clearHistoryItem.addActionListener(e -> clearHistory());

        toolsMenu.add(settingsItem);
        toolsMenu.add(validateChainItem);
        toolsMenu.add(clearHistoryItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        JMenuItem helpItem = new JMenuItem("User Guide");

        aboutItem.addActionListener(e -> showAbout());
        helpItem.addActionListener(e -> showHelp());

        helpMenu.add(helpItem);
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setOpaque(false);

        textArea1 = new JTextArea();
        textArea1.setLineWrap(true);
        textArea1.setWrapStyleWord(true);
        textArea1.setFont(new Font("Monospaced", Font.PLAIN, 12));

        textArea2 = new JTextArea();
        textArea2.setLineWrap(true);
        textArea2.setWrapStyleWord(true);
        textArea2.setFont(new Font("Monospaced", Font.PLAIN, 12));

        panel.add(createDocumentPanel(textArea1, "Submission Document", true));
        panel.add(createDocumentPanel(textArea2, "Source Document", false));

        return panel;
    }

    private JPanel createDocumentPanel(JTextArea area, String title, boolean isSubmission) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder(title));
        panel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton uploadButton = new JButton("Upload File");
        JButton clearButton = new JButton("Clear");
        JButton pasteButton = new JButton("Paste");

        uploadButton.addActionListener(e -> onUpload(area));
        clearButton.addActionListener(e -> area.setText(""));
        pasteButton.addActionListener(e -> {
            try {
                String clipboardText = (String) Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                area.setText(clipboardText);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Failed to paste from clipboard");
            }
        });

        buttonPanel.add(uploadButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(pasteButton);

        JLabel charCountLabel = new JLabel("Characters: 0");
        area.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void update() {
                charCountLabel.setText("Characters: " + area.getText().length());
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        buttonPanel.add(charCountLabel);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBackground(new Color(230, 230, 235));
        panel.setBorder(BorderFactory.createEtchedBorder());

        JLabel algoLabel = new JLabel("Algorithm:");
        algoLabel.setFont(new Font("Arial", Font.BOLD, 12));

        algorithmBox = new JComboBox<>(new String[]{"Cosine", "Jaccard", "Levenshtein", "N-Gram"});
        algorithmBox.setPreferredSize(new Dimension(120, 25));

        JButton checkButton = new JButton("Check Plagiarism");
        checkButton.setBackground(new Color(52, 152, 219));
        checkButton.setForeground(Color.WHITE);
        checkButton.setFont(new Font("Arial", Font.BOLD, 12));

        JButton autoButton = new JButton("Auto Find Source + Check");
        autoButton.setBackground(new Color(46, 204, 113));
        autoButton.setForeground(Color.WHITE);
        autoButton.setFont(new Font("Arial", Font.BOLD, 12));

        JButton useLastSourceButton = new JButton("Use Last Source");
        JButton saveButton = new JButton("Save Chain");
        JButton loadButton = new JButton("Load Chain");

        checkButton.addActionListener(this::onCheck);
        autoButton.addActionListener(this::onAutoFindSource);
        useLastSourceButton.addActionListener(e -> onUseLastSource());
        saveButton.addActionListener(e -> onSave());
        loadButton.addActionListener(e -> onLoad());

        panel.add(algoLabel);
        panel.add(algorithmBox);
        panel.add(checkButton);
        panel.add(autoButton);
        panel.add(useLastSourceButton);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(saveButton);
        panel.add(loadButton);

        return panel;
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder("Analysis Results"),
                new EmptyBorder(10, 10, 10, 10)
        ));
        panel.setBackground(Color.WHITE);

        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        infoPanel.setOpaque(false);

        resultLabel = new JLabel("Result: Ready");
        resultLabel.setFont(new Font("Arial", Font.BOLD, 16));
        resultLabel.setForeground(new Color(52, 73, 94));

        sourceUrlLabel = new JLabel("Source: -");
        sourceUrlLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Idle");
        progressBar.setPreferredSize(new Dimension(400, 25));

        infoPanel.add(resultLabel);
        infoPanel.add(sourceUrlLabel);
        infoPanel.add(progressBar);

        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder("Blockchain History"));
        panel.setBackground(Color.WHITE);

        String[] columns = {"Block", "Score", "Verdict", "Timestamp"};
        historyTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable historyTable = new JTable(historyTableModel);
        historyTable.setFillsViewportHeight(true);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(150);

        JScrollPane scrollPane = new JScrollPane(historyTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton viewDetailsButton = new JButton("View Block Details");
        viewDetailsButton.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row >= 0) {
                showBlockDetails(row);
            } else {
                JOptionPane.showMessageDialog(frame, "Please select a block first");
            }
        });

        panel.add(viewDetailsButton, BorderLayout.SOUTH);

        return panel;
    }

    private void onUpload(JTextArea target) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt", "md", "doc"));
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

    private void onCheck(ActionEvent e) {
        String text1 = textArea1.getText();
        String text2 = textArea2.getText();

        if (text1.isBlank() || text2.isBlank()) {
            JOptionPane.showMessageDialog(frame, "Both documents must contain text");
            return;
        }

        progressBar.setIndeterminate(true);
        progressBar.setString("Analyzing...");

        SwingWorker<PlagiarismChecker.Result, Void> worker = new SwingWorker<>() {
            @Override
            protected PlagiarismChecker.Result doInBackground() {
                Document doc1 = new Document("Submission", System.getProperty("user.name"),
                        LocalDate.now().toString(), text1);
                Document doc2 = new Document("Source", System.getProperty("user.name"),
                        LocalDate.now().toString(), text2);

                String algorithm = (String) algorithmBox.getSelectedItem();
                return PlagiarismChecker.checkPlagiarism(doc1, doc2, algorithm);
            }

            @Override
            protected void done() {
                try {
                    PlagiarismChecker.Result result = get();
                    double percent = result.score() * 100.0;

                    Color verdictColor = switch (result.verdict()) {
                        case "Safe" -> new Color(39, 174, 96);
                        case "Moderate" -> new Color(243, 156, 18);
                        default -> new Color(231, 76, 60);
                    };

                    resultLabel.setText(String.format("Result: %.2f%% - %s", percent, result.verdict()));
                    resultLabel.setForeground(verdictColor);

                    Document doc2 = new Document("Source", "web", LocalDate.now().toString(), text2);
                    Block newBlock = blockchain.addBlock(doc2);

                    historyTableModel.addRow(new Object[]{
                            newBlock.getIndex(),
                            String.format("%.2f%%", percent),
                            result.verdict(),
                            newBlock.getTimestamp()
                    });

                    if (config.getBoolean("blockchain.auto_save", true)) {
                        try {
                            StorageManager.saveChainToFile(blockchain, chainFile);
                        } catch (IOException ex) {
                            System.err.println("Auto-save failed: " + ex.getMessage());
                        }
                    }

                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    progressBar.setString("Complete");

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Analysis failed: " + ex.getMessage());
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    progressBar.setString("Failed");
                }
            }
        };

        worker.execute();
    }

    private void onAutoFindSource(ActionEvent e) {
        String submission = textArea1.getText();
        if (submission.isBlank()) {
            JOptionPane.showMessageDialog(frame, "Please provide the submission text first");
            return;
        }

        progressBar.setIndeterminate(true);
        progressBar.setString("Discovering source...");
        resultLabel.setText("Result: Searching web for original source...");
        sourceUrlLabel.setText("Source: -");

        SwingWorker<SourceDiscoveryService.DiscoveredSource, Void> worker = new SwingWorker<>() {
            @Override
            protected SourceDiscoveryService.DiscoveredSource doInBackground() {
                SourceDiscoveryService discovery = new SourceDiscoveryService();
                return discovery.discoverOriginalSource(submission).orElse(null);
            }

            @Override
            protected void done() {
                try {
                    SourceDiscoveryService.DiscoveredSource found = get();

                    String originalText;
                    String sourceUrl = "";

                    if (found != null) {
                        originalText = found.text();
                        sourceUrl = found.url();
                    } else {
                        int choice = JOptionPane.showConfirmDialog(frame,
                                "Could not auto-find the original source. Provide a source file?",
                                "Provide Source",
                                JOptionPane.YES_NO_OPTION);

                        if (choice != JOptionPane.YES_OPTION) {
                            resultLabel.setText("Result: No source provided");
                            progressBar.setIndeterminate(false);
                            progressBar.setValue(0);
                            progressBar.setString("Cancelled");
                            return;
                        }

                        JFileChooser chooser = new JFileChooser();
                        chooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt", "md"));
                        int res = chooser.showOpenDialog(frame);

                        if (res != JFileChooser.APPROVE_OPTION) {
                            resultLabel.setText("Result: No source provided");
                            progressBar.setIndeterminate(false);
                            progressBar.setValue(0);
                            progressBar.setString("Cancelled");
                            return;
                        }

                        File file = chooser.getSelectedFile();
                        try {
                            originalText = Files.readString(file.toPath());
                            sourceUrl = file.getAbsolutePath();
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(frame, "Failed to read file: " + ex.getMessage());
                            progressBar.setIndeterminate(false);
                            progressBar.setValue(0);
                            progressBar.setString("Error");
                            return;
                        }
                    }

                    textArea2.setText(originalText);
                    sourceUrlLabel.setText("Source: " + (sourceUrl.isBlank() ? "(local file)" : sourceUrl));

                    Document docSubmission = new Document("Submission", System.getProperty("user.name"),
                            LocalDate.now().toString(), submission);
                    Document docOriginal = new Document("OriginalSource", "web",
                            LocalDate.now().toString(), originalText, sourceUrl);

                    String algorithm = (String) algorithmBox.getSelectedItem();
                    PlagiarismChecker.Result result = PlagiarismChecker.checkPlagiarism(docSubmission, docOriginal, algorithm);

                    double percent = result.score() * 100.0;

                    Color verdictColor = switch (result.verdict()) {
                        case "Safe" -> new Color(39, 174, 96);
                        case "Moderate" -> new Color(243, 156, 18);
                        default -> new Color(231, 76, 60);
                    };

                    resultLabel.setText(String.format("Result: %.2f%% - %s", percent, result.verdict()));
                    resultLabel.setForeground(verdictColor);

                    Block newBlock = blockchain.addBlock(docOriginal);
                    historyTableModel.addRow(new Object[]{
                            newBlock.getIndex(),
                            String.format("%.2f%%", percent),
                            result.verdict(),
                            newBlock.getTimestamp()
                    });

                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    progressBar.setString("Complete");

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Source discovery failed: " + ex.getMessage());
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    progressBar.setString("Failed");
                }
            }
        };

        worker.execute();
    }

    private void onUseLastSource() {
        List<Block> blocks = blockchain.getBlocks();
        if (blocks.size() <= 1) {
            JOptionPane.showMessageDialog(frame, "No saved source in blockchain yet");
            return;
        }

        Block last = blocks.get(blocks.size() - 1);
        Document d = last.getDocument();

        if (d.getText() == null || d.getText().isBlank()) {
            JOptionPane.showMessageDialog(frame, "Last block has no source text");
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
                JOptionPane.showMessageDialog(frame, "Loaded chain is invalid!");
                return;
            }

            historyTableModel.setRowCount(0);
            for (Block b : loaded.getBlocks()) {
                historyTableModel.addRow(new Object[]{
                        b.getIndex(),
                        String.format("%.2f%%", b.getDocument().getPlagiarismScore() * 100),
                        "-",
                        b.getTimestamp()
                });
            }

            JOptionPane.showMessageDialog(frame, "Blockchain loaded successfully");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Load failed: " + ex.getMessage());
        }
    }

    private void exportResults(String format) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Results");

        switch (format) {
            case "json" -> chooser.setSelectedFile(new File("blockchain_export.json"));
            case "csv" -> chooser.setSelectedFile(new File("results_export.csv"));
            case "report" -> chooser.setSelectedFile(new File("plagiarism_report.txt"));
        }

        int result = chooser.showSaveDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                if (format.equals("json")) {
                    ResultExporter.exportBlockchainToJSON(blockchain.getBlocks(), file);
                } else if (format.equals("report")) {
                    JOptionPane.showMessageDialog(frame, "Report export requires completed check results");
                }
                JOptionPane.showMessageDialog(frame, "Export successful: " + file.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Export failed: " + ex.getMessage());
            }
        }
    }

    private void showSettings() {
        SettingsDialog dialog = new SettingsDialog(frame, config);
        dialog.setVisible(true);
    }

    private void validateBlockchain() {
        boolean isValid = blockchain.isChainValid();
        String message = isValid ? "Blockchain is valid!" : "Blockchain integrity compromised!";
        int messageType = isValid ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE;
        JOptionPane.showMessageDialog(frame, message, "Blockchain Validation", messageType);
    }

    private void clearHistory() {
        int choice = JOptionPane.showConfirmDialog(frame,
                "Are you sure you want to clear the blockchain history?",
                "Confirm Clear",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            historyTableModel.setRowCount(0);
            JOptionPane.showMessageDialog(frame, "History cleared (blockchain file not deleted)");
        }
    }

    private void showBlockDetails(int blockIndex) {
        List<Block> blocks = blockchain.getBlocks();
        if (blockIndex < 0 || blockIndex >= blocks.size()) {
            return;
        }

        Block block = blocks.get(blockIndex);
        Document doc = block.getDocument();

        String details = String.format(
                "Block Details\n\n" +
                "Index: %d\n" +
                "Timestamp: %s\n" +
                "Hash: %s\n" +
                "Previous Hash: %s\n\n" +
                "Document:\n" +
                "Title: %s\n" +
                "Author: %s\n" +
                "Score: %.2f%%\n" +
                "Source URL: %s\n",
                block.getIndex(),
                block.getTimestamp(),
                block.getHash(),
                block.getPreviousHash(),
                doc.getTitle(),
                doc.getAuthor(),
                doc.getPlagiarismScore() * 100,
                doc.getSourceUrl().isBlank() ? "N/A" : doc.getSourceUrl()
        );

        JTextArea textArea = new JTextArea(details);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(frame, scrollPane, "Block Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAbout() {
        String about = """
                Advanced Plagiarism Detection System v2.0

                Features:
                - Multiple similarity algorithms (Cosine, Jaccard, Levenshtein, N-Gram)
                - AI-powered source discovery
                - Blockchain-based tamper-proof storage
                - Cloud synchronization with Supabase
                - Export results to JSON, CSV, and detailed reports

                Developed for academic integrity and content verification.
                """;

        JOptionPane.showMessageDialog(frame, about, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showHelp() {
        String help = """
                User Guide

                1. Load Documents:
                   - Enter text directly or upload files
                   - Use "Paste" to paste from clipboard

                2. Run Analysis:
                   - Select an algorithm
                   - Click "Check Plagiarism" for manual comparison
                   - Click "Auto Find Source" to discover sources online

                3. View Results:
                   - Similarity score and verdict displayed
                   - History tracked in blockchain

                4. Export:
                   - Save blockchain data
                   - Export reports in various formats

                5. Settings:
                   - Configure thresholds
                   - Enable/disable AI features
                """;

        JTextArea textArea = new JTextArea(help);
        textArea.setEditable(false);
        textArea.setFont(new Font("Arial", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));

        JOptionPane.showMessageDialog(frame, scrollPane, "Help", JOptionPane.INFORMATION_MESSAGE);
    }

    public void displayForm() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Headless environment detected. Running console demo...");
            ConsoleMain.main(args);
            return;
        }

        new EnhancedSwingApp().displayForm();
    }
}
