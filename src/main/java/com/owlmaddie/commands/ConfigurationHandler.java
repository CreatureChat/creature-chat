// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.owlmaddie.network.ServerPackets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code ConfigurationHandler} class loads and saves configuration settings for this mod. It first
 * checks for a config file in the world save folder, and if not found, falls back to the root folder.
 * This allows for global/default settings, or optional server-specific settings.
 */

public class ConfigurationHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path serverConfigPath;
    private final Path defaultConfigPath;

    public ConfigurationHandler(MinecraftServer server) {
        this.serverConfigPath = server.getWorldPath(LevelResource.ROOT).resolve("creaturechat.json");
        this.defaultConfigPath = Paths.get(".", "creaturechat.json"); // Assumes the default location is the server root or a similar logical default
    }

    public Config loadConfig() {
        Config config = loadConfigFromFile(serverConfigPath);
        if (config == null) {
            config = loadConfigFromFile(defaultConfigPath);
        }
        return config != null ? config : new Config(); // Return new config if both are null
    }

    public boolean saveConfig(Config config, boolean useServerConfig) {
        Path path = useServerConfig ? serverConfigPath : defaultConfigPath;
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(config, writer);
            return true;
        } catch (IOException e) {
            String errorMessage = "Error saving `creaturechat.json`. CreatureChat config was not saved. " + e.getMessage();
            LOGGER.error(errorMessage, e);
            ServerPackets.sendErrorToAllOps(ServerPackets.serverInstance, errorMessage);
            return false;
        }
    }

    private Config loadConfigFromFile(Path filePath) {
        try (Reader reader = Files.newBufferedReader(filePath)) {
            return gson.fromJson(reader, Config.class);
        } catch (IOException e) {
            return null; // File does not exist or other IO errors
        }
    }

    public static class Config {
        private String apiKey = "";
        private String url = "https://api.openai.com/v1/chat/completions";
        private String model = "gpt-3.5-turbo";
        private int maxContextTokens = 16385;
        private int maxOutputTokens = 200;
        private double percentOfContext = 0.75;
        private int timeout = 10;
        private boolean chatBubbles = true;
        private List<String> whitelist = new ArrayList<>();
        private List<String> blacklist = new ArrayList<>();
        private String story = "";
        private int maxPlayerAutoResponses = 10;
        private int playerAutoCooldownSeconds = 3;
        private int maxEntityAutoResponses = 3;
        private int entityAutoCooldownSeconds = 3;

        // Getters and setters for existing fields
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) {
            if (apiKey.startsWith("cc_") && apiKey.length() == 15) {
                // Update URL if a CreatureChat API key is detected
                setUrl("https://api.creaturechat.com/v1/chat/completions");
            } else if (apiKey.startsWith("sk-")) {
                // Update URL if an OpenAI API key is detected
                setUrl("https://api.openai.com/v1/chat/completions");
            }
            this.apiKey = apiKey;
        }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }

        public int getMaxContextTokens() { return maxContextTokens; }
        public void setMaxContextTokens(int maxContextTokens) { this.maxContextTokens = maxContextTokens; }

        public int getMaxOutputTokens() { return maxOutputTokens; }
        public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }

        public double getPercentOfContext() { return percentOfContext; }
        public void setPercentOfContext(double percentOfContext) { this.percentOfContext = percentOfContext; }

        public List<String> getWhitelist() { return whitelist; }
        public void setWhitelist(List<String> whitelist) { this.whitelist = whitelist; }

        public List<String> getBlacklist() { return blacklist; }
        public void setBlacklist(List<String> blacklist) { this.blacklist = blacklist; }

        public String getStory() { return story; }
        public void setStory(String story) { this.story = story; }

        // Add getter and setter
        public boolean getChatBubbles() { return chatBubbles; }
        public void setChatBubbles(boolean chatBubblesEnabled) { this.chatBubbles = chatBubblesEnabled; }

        public int getMaxPlayerAutoResponses() { return maxPlayerAutoResponses; }
        public void setMaxPlayerAutoResponses(int maxPlayerAutoResponses) { this.maxPlayerAutoResponses = maxPlayerAutoResponses; }

        public int getPlayerAutoCooldownSeconds() { return playerAutoCooldownSeconds; }
        public void setPlayerAutoCooldownSeconds(int playerAutoCooldownSeconds) { this.playerAutoCooldownSeconds = playerAutoCooldownSeconds; }

        public int getMaxEntityAutoResponses() { return maxEntityAutoResponses; }
        public void setMaxEntityAutoResponses(int maxEntityAutoResponses) { this.maxEntityAutoResponses = maxEntityAutoResponses; }

        public int getEntityAutoCooldownSeconds() { return entityAutoCooldownSeconds; }
        public void setEntityAutoCooldownSeconds(int entityAutoCooldownSeconds) { this.entityAutoCooldownSeconds = entityAutoCooldownSeconds; }
    }
}
