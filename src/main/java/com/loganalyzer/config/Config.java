package com.loganalyzer.config;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Config {

    public static final String DEFAULT_API_BASE_URL = "http://localhost:11434/v1";
    public static final String DEFAULT_API_KEY = "";
    public static final String DEFAULT_MODEL = "qwen3.5:9b";
    public static final String DEFAULT_SYSTEM_PROMPT =
            "You are a helpful assistant that analyzes log files. "
                    + "Answer questions about the provided log clearly and concisely, "
                    + "citing relevant lines when useful.";

    private static final Logger LOGGER = Logger.getLogger(Config.class.getName());
    private static final Object LOCK = new Object();

    private static volatile Config instance;
    private static Path configPath = defaultConfigPath();

    private volatile String apiBaseUrl = DEFAULT_API_BASE_URL;
    private volatile String apiKey = DEFAULT_API_KEY;
    private volatile String model = DEFAULT_MODEL;
    private volatile String systemPrompt = DEFAULT_SYSTEM_PROMPT;

    private Config() {}

    public static Config getInstance() {
        Config c = instance;
        if (c == null) {
            synchronized (LOCK) {
                c = instance;
                if (c == null) {
                    c = load();
                }
            }
        }
        return c;
    }

    public static Config load() {
        synchronized (LOCK) {
            Config c = new Config();
            Path path = configPath;
            if (Files.isRegularFile(path)) {
                try (XMLDecoder dec = new XMLDecoder(
                        new BufferedInputStream(Files.newInputStream(path)))) {
                    Object obj = dec.readObject();
                    if (obj instanceof ConfigBean b) {
                        if (b.getApiBaseUrl() != null) c.apiBaseUrl = b.getApiBaseUrl();
                        if (b.getApiKey() != null) c.apiKey = b.getApiKey();
                        if (b.getModel() != null) c.model = b.getModel();
                        if (b.getSystemPrompt() != null) c.systemPrompt = b.getSystemPrompt();
                    } else {
                        LOGGER.warning("Config file has unexpected contents; using defaults");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING,
                            "Failed to read config at " + path + "; using defaults", e);
                }
            }
            instance = c;
            return c;
        }
    }

    public void save() {
        synchronized (LOCK) {
            Path path = configPath;
            try {
                Path parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                ConfigBean bean = new ConfigBean();
                bean.setApiBaseUrl(apiBaseUrl);
                bean.setApiKey(apiKey);
                bean.setModel(model);
                bean.setSystemPrompt(systemPrompt);
                try (XMLEncoder enc = new XMLEncoder(
                        new BufferedOutputStream(Files.newOutputStream(path)))) {
                    enc.writeObject(bean);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to write config at " + path, e);
            }
        }
    }

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String v) { this.apiBaseUrl = v; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String v) { this.apiKey = v; }

    public String getModel() { return model; }
    public void setModel(String v) { this.model = v; }

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String v) { this.systemPrompt = v; }

    static Path defaultConfigPath() {
        return Path.of(System.getProperty("user.home"), ".loganalyzer", "config.xml");
    }

    static void setConfigPathForTesting(Path path) {
        synchronized (LOCK) {
            configPath = path;
            instance = null;
        }
    }

    static void resetConfigPathForTesting() {
        synchronized (LOCK) {
            configPath = defaultConfigPath();
            instance = null;
        }
    }
}
