package com.loganalyzer.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @AfterEach
    void tearDown() {
        Config.resetConfigPathForTesting();
    }

    @Test
    void returnsDefaultsWhenXmlAbsent(@TempDir Path tmp) {
        Config.setConfigPathForTesting(tmp.resolve("config.xml"));

        Config c = Config.load();

        assertEquals(Config.DEFAULT_API_BASE_URL, c.getApiBaseUrl());
        assertEquals(Config.DEFAULT_API_KEY, c.getApiKey());
        assertEquals(Config.DEFAULT_MODEL, c.getModel());
        assertEquals(Config.DEFAULT_SYSTEM_PROMPT, c.getSystemPrompt());
        assertSame(c, Config.getInstance());
    }

    @Test
    void savePersistsFieldsAndCreatesParentDir(@TempDir Path tmp) {
        Path configFile = tmp.resolve("nested").resolve("dir").resolve("config.xml");
        Config.setConfigPathForTesting(configFile);

        Config c = Config.getInstance();
        c.setApiBaseUrl("https://example.test/v1");
        c.setApiKey("sk-secret");
        c.setModel("test-model");
        c.setSystemPrompt("be terse");
        c.save();

        assertTrue(Files.isRegularFile(configFile),
                "save() must create the XML file and any missing parent directories");
    }

    @Test
    void roundTripsMutationsThroughFreshInstance(@TempDir Path tmp) {
        Path configFile = tmp.resolve("config.xml");
        Config.setConfigPathForTesting(configFile);

        Config first = Config.getInstance();
        first.setApiBaseUrl("https://example.test/v1");
        first.setApiKey("sk-secret");
        first.setModel("test-model");
        first.setSystemPrompt("be terse");
        first.save();

        // Force a fresh singleton reading from the same path.
        Config.setConfigPathForTesting(configFile);
        Config reloaded = Config.getInstance();

        assertNotSame(first, reloaded);
        assertEquals("https://example.test/v1", reloaded.getApiBaseUrl());
        assertEquals("sk-secret", reloaded.getApiKey());
        assertEquals("test-model", reloaded.getModel());
        assertEquals("be terse", reloaded.getSystemPrompt());
    }
}
