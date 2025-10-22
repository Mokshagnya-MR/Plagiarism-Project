package com.example.plagiarism.ui;

import com.example.plagiarism.config.AppConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class SettingsDialog extends JDialog {
    private final AppConfig config;
    private final JSpinner safeThresholdSpinner;
    private final JSpinner highThresholdSpinner;
    private final JComboBox<String> aiModelBox;
    private final JCheckBox aiEnabledCheck;
    private final JCheckBox supabaseEnabledCheck;
    private final JCheckBox autoSaveCheck;
    private final JSpinner maxResultsSpinner;
    private final JSpinner minConfidenceSpinner;

    public SettingsDialog(JFrame parent, AppConfig config) {
        super(parent, "Settings", true);
        this.config = config;

        setSize(600, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel thresholdPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        thresholdPanel.setBorder(new TitledBorder("Similarity Thresholds"));

        safeThresholdSpinner = new JSpinner(new SpinnerNumberModel(
                config.getInt("similarity.threshold.safe", 30), 0, 100, 1));
        highThresholdSpinner = new JSpinner(new SpinnerNumberModel(
                config.getInt("similarity.threshold.high", 70), 0, 100, 1));

        thresholdPanel.add(new JLabel("Safe Threshold (%):"));
        thresholdPanel.add(safeThresholdSpinner);
        thresholdPanel.add(new JLabel("High Threshold (%):"));
        thresholdPanel.add(highThresholdSpinner);

        JPanel aiPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        aiPanel.setBorder(new TitledBorder("AI Configuration"));

        aiEnabledCheck = new JCheckBox("Enable AI Source Discovery",
                config.getBoolean("ai.enabled", true));
        aiModelBox = new JComboBox<>(new String[]{"anthropic", "openai"});
        aiModelBox.setSelectedItem(config.get("ai.model", "anthropic"));

        aiPanel.add(aiEnabledCheck);
        aiPanel.add(new JLabel());
        aiPanel.add(new JLabel("AI Model:"));
        aiPanel.add(aiModelBox);

        JPanel storagePanel = new JPanel(new GridLayout(2, 1, 10, 10));
        storagePanel.setBorder(new TitledBorder("Storage Options"));

        supabaseEnabledCheck = new JCheckBox("Enable Supabase Sync",
                config.getBoolean("supabase.enabled", true));
        autoSaveCheck = new JCheckBox("Auto-save Blockchain",
                config.getBoolean("blockchain.auto_save", true));

        storagePanel.add(supabaseEnabledCheck);
        storagePanel.add(autoSaveCheck);

        JPanel discoveryPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        discoveryPanel.setBorder(new TitledBorder("Source Discovery"));

        maxResultsSpinner = new JSpinner(new SpinnerNumberModel(
                config.getInt("source_discovery.max_results", 5), 1, 20, 1));
        minConfidenceSpinner = new JSpinner(new SpinnerNumberModel(
                config.getDouble("source_discovery.min_confidence", 0.55), 0.0, 1.0, 0.05));

        discoveryPanel.add(new JLabel("Max Results:"));
        discoveryPanel.add(maxResultsSpinner);
        discoveryPanel.add(new JLabel("Min Confidence:"));
        discoveryPanel.add(minConfidenceSpinner);

        mainPanel.add(thresholdPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(aiPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(storagePanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(discoveryPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        JButton resetButton = new JButton("Reset to Defaults");

        saveButton.addActionListener(e -> saveSettings());
        cancelButton.addActionListener(e -> dispose());
        resetButton.addActionListener(e -> resetToDefaults());

        buttonPanel.add(resetButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        add(new JScrollPane(mainPanel), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void saveSettings() {
        config.set("similarity.threshold.safe", safeThresholdSpinner.getValue().toString());
        config.set("similarity.threshold.high", highThresholdSpinner.getValue().toString());
        config.set("ai.model", (String) aiModelBox.getSelectedItem());
        config.set("ai.enabled", String.valueOf(aiEnabledCheck.isSelected()));
        config.set("supabase.enabled", String.valueOf(supabaseEnabledCheck.isSelected()));
        config.set("blockchain.auto_save", String.valueOf(autoSaveCheck.isSelected()));
        config.set("source_discovery.max_results", maxResultsSpinner.getValue().toString());
        config.set("source_discovery.min_confidence", minConfidenceSpinner.getValue().toString());

        config.saveConfig();

        JOptionPane.showMessageDialog(this, "Settings saved successfully!");
        dispose();
    }

    private void resetToDefaults() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Reset all settings to defaults?",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            safeThresholdSpinner.setValue(30);
            highThresholdSpinner.setValue(70);
            aiModelBox.setSelectedItem("anthropic");
            aiEnabledCheck.setSelected(true);
            supabaseEnabledCheck.setSelected(true);
            autoSaveCheck.setSelected(true);
            maxResultsSpinner.setValue(5);
            minConfidenceSpinner.setValue(0.55);
        }
    }
}
