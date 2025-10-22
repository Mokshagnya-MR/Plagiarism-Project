package com.example.plagiarism.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfig {
    private static final String CONFIG_FILE = System.getProperty("user.home") + File.separator + ".plagiarism_checker_config.properties";
    private static AppConfig instance;
    private Properties properties;

    private AppConfig() {
        properties = new Properties();
        loadConfig();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    private void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Failed to load config: " + e.getMessage());
            }
        }
        setDefaults();
    }

    private void setDefaults() {
        properties.putIfAbsent("similarity.threshold.safe", "30");
        properties.putIfAbsent("similarity.threshold.high", "70");
        properties.putIfAbsent("ai.model", "anthropic");
        properties.putIfAbsent("ai.enabled", "true");
        properties.putIfAbsent("supabase.enabled", "true");
        properties.putIfAbsent("blockchain.auto_save", "true");
        properties.putIfAbsent("source_discovery.max_results", "5");
        properties.putIfAbsent("source_discovery.min_confidence", "0.55");
    }

    public void saveConfig() {
        File configFile = new File(CONFIG_FILE);
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "Plagiarism Checker Configuration");
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(defaultValue)));
    }

    public void set(String key, String value) {
        properties.setProperty(key, value);
    }

    public String getSupabaseUrl() {
        return System.getenv("VITE_SUPABASE_URL");
    }

    public String getSupabaseKey() {
        return System.getenv("VITE_SUPABASE_ANON_KEY");
    }

    public String getOpenAIKey() {
        return System.getenv("OPENAI_API_KEY");
    }

    public String getAnthropicKey() {
        return System.getenv("ANTHROPIC_API_KEY");
    }
}
