// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.tests;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.ChatGPTRequest;
import com.owlmaddie.commands.ConfigurationHandler;
import com.owlmaddie.message.MessageParser;
import com.owlmaddie.message.ParsedMessage;
import com.owlmaddie.utils.EntityTestData;
import com.owlmaddie.utils.RateLimiter;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code BehaviorTests} class tests a variety of LLM prompts and expected outputs from specific characters
 * and personality types. For example, an aggressive character will attack, a nervous character will flee, etc...
 */
public class BehaviorTests {
    static String PROMPT_PATH = "src/main/resources/data/creaturechat/prompts/";
    static String RESOURCE_PATH = "src/test/resources/data/creaturechat/";
    static String API_KEY = "";
    static String API_URL = "";
    static String API_MODEL = "";
    static String OUTPUT_JSON_PATH = "src/test/BehaviorOutputs.json";
    static String NO_API_KEY = "No API_KEY environment variable has been set.";

    // Requests per second limit
    private static final RateLimiter rateLimiter = new RateLimiter(1);

    static ConfigurationHandler.Config config = null;
    static String systemChatContents = null;

    List<String> followMessages = Arrays.asList(
            "Please follow me",
            "Come with me please",
            "Quickly, please join me on an adventure");
    List<String> leadMessages = Arrays.asList(
            "Take me to a secret forrest",
            "Where is the strong hold?",
            "Can you help me find the location of the secret artifact?");
    List<String> attackMessages = Arrays.asList(
            "<attacked you directly with Stone Axe>",
            "<attacked you indirectly with Arrow>",
            "Fight me now or your city burns!");
    List<String> protectMessages = Arrays.asList(
            "Please protect me",
            "Please keep me safe friend",
            "Don't let them hurt me please");
    List<String> buildMessages = Arrays.asList(
            "Can you build a house for me?",
            "Please make a garden here",
            "Let's put up a small hut");
    List<String> unBuildMessages = Arrays.asList(
            "stop building",
            "please cancel the build",
            "you can quit building now");
    List<String> unFleeMessages = Arrays.asList(
            "I'm so sorry, please stop running away",
            "Stop fleeing immediately",
            "You are safe now, please stop running");
    List<String> friendshipUpMessages = Arrays.asList(
            "Hi friend! I am so happy to see you again!",
            "Looking forward to hanging out with you.",
            "<gives 1 golden apple>");
    List<String> friendshipDownMessages = Arrays.asList(
            "<attacked you directly with Stone Axe>",
            "You suck so much! I hate you",
            "DIEEE!");

    static Path systemChatPath = Paths.get(PROMPT_PATH, "system-chat");
    static Path bravePath = Paths.get(RESOURCE_PATH, "chatdata", "brave-archer.json");
    static Path nervousPath = Paths.get(RESOURCE_PATH, "chatdata", "nervous-rogue.json");
    static Path entityPigPath = Paths.get(RESOURCE_PATH, "entities", "pig.json");
    static Path playerPath = Paths.get(RESOURCE_PATH, "players", "player.json");
    static Path worldPath = Paths.get(RESOURCE_PATH, "worlds", "world.json");
    static Map<String, Map<String, String>> outputData;

    static Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    static Gson gson = new GsonBuilder().create();

    @AfterAll
    static public void cleanup() throws IOException {
        if (outputData != null) {
            // Save BehaviorOutput.json file (with appended prompt outputs)
            final Gson gsonOutput = new GsonBuilder().setPrettyPrinting().create(); // Pretty-print enabled
            Files.write(Paths.get(OUTPUT_JSON_PATH), gsonOutput.toJson(outputData).getBytes());
        }
    }

    @BeforeAll
    public static void setup() {
        // Get API key from env var
        API_KEY = System.getenv("API_KEY");
        API_URL = System.getenv("API_URL");
        API_MODEL = System.getenv("API_MODEL");

        // Config
        config = new ConfigurationHandler.Config();
        config.setTimeout(0);
        if (API_KEY != null && !API_KEY.isEmpty()) {
            config.setApiKey(API_KEY);
        }
        if (API_URL != null && !API_URL.isEmpty()) {
            config.setUrl(API_URL);
        }
        if (API_MODEL != null && !API_MODEL.isEmpty()) {
            config.setModel(API_MODEL);
        }
        // Verify API key is set correctly
        Assumptions.assumeTrue(API_KEY != null && !API_KEY.isEmpty(), NO_API_KEY);

        // Load system chat prompt
        systemChatContents = readFileContents(systemChatPath);

        // Load previous unit tests outputs (so new ones can be appended)
        outputData = loadExistingOutputData();
    }

    @Test
    public void followBrave() {
        for (String message : followMessages) {
            testPromptForBehavior(bravePath, List.of(message), "FOLLOW", "LEAD");
        }
    }

    @Test
    public void followNervous() {
        for (String message : followMessages) {
            testPromptForBehavior(nervousPath, List.of(message), "FOLLOW", "LEAD");
        }
    }

    @Test
    public void leadBrave() {
        for (String message : leadMessages) {
            testPromptForBehavior(bravePath, List.of(message), "LEAD", "FOLLOW");
        }
    }

    @Test
    public void leadNervous() {
        for (String message : leadMessages) {
            testPromptForBehavior(nervousPath, List.of(message), "LEAD", "FOLLOW");
        }
    }

    @Test
    public void unFleeBrave() {
        for (String message : unFleeMessages) {
            testPromptForBehavior(bravePath, List.of(message), "UNFLEE", "FOLLOW");
        }
    }

    @Test
    public void protectBrave() {
        for (String message : protectMessages) {
            testPromptForBehavior(bravePath, List.of(message), "PROTECT", "ATTACK");
        }
    }

    @Test
    public void protectNervous() {
        for (String message : protectMessages) {
            testPromptForBehavior(nervousPath, List.of(message), "PROTECT", "ATTACK");
        }
    }

    @Test
    public void attackBrave() {
        for (String message : attackMessages) {
            testPromptForBehavior(bravePath, List.of(message), "ATTACK", "FLEE");
        }
    }

    @Test
    public void attackNervous() {
        for (String message : attackMessages) {
            testPromptForBehavior(nervousPath, List.of(message), "FLEE", "ATTACK");
        }
    }

    @Test
    public void buildBrave() {
        for (String message : buildMessages) {
            testPromptForBehavior(bravePath, List.of(message), "BUILD", null);
        }
    }

    @Test
    public void buildNervous() {
        for (String message : buildMessages) {
            testPromptForBehavior(nervousPath, List.of(message), "BUILD", null);
        }
    }

    @Test
    public void unBuildBrave() {
        for (String message : unBuildMessages) {
            testPromptForBehavior(bravePath, List.of(message), "UNBUILD", null);
        }
    }

    @Test
    public void unBuildNervous() {
        for (String message : unBuildMessages) {
            testPromptForBehavior(nervousPath, List.of(message), "UNBUILD", null);
        }
    }

    @Test
    public void friendshipUpNervous() {
        for (String message : friendshipUpMessages) {
            ParsedMessage result = testPromptForBehavior(nervousPath, List.of(message), "FRIENDSHIP+", null);
            assertTrue(result.getBehaviors().stream().anyMatch(b -> "FRIENDSHIP".equals(b.getName()) && b.getArgumentAsInt() > 0));
        }
    }

    @Test
    public void friendshipUpBrave() {
        for (String message : friendshipUpMessages) {
            ParsedMessage result = testPromptForBehavior(bravePath, List.of(message), "FRIENDSHIP+", null);
            assertTrue(result.getBehaviors().stream().anyMatch(b -> "FRIENDSHIP".equals(b.getName()) && b.getArgumentAsInt() > 0));
        }
    }

    @Test
    public void friendshipDownNervous() {
        for (String message : friendshipDownMessages) {
            ParsedMessage result = testPromptForBehavior(nervousPath, List.of(message), "FRIENDSHIP-", null);
            assertTrue(result.getBehaviors().stream().anyMatch(b -> "FRIENDSHIP".equals(b.getName()) && b.getArgumentAsInt() < 0));
        }
    }

    @Test
    public void missingItemsDoesNotUnbuild() {
        String message = "The build is paused because I'm missing materials. Next item needed: dirt. Missing items to finish: 3 dirt, 2 stone. In your reply, ask the player for these items and confirm you'll continue building once they arrive.";
        testPromptForBehavior(bravePath, List.of(message), null, "UNBUILD");
    }

    public ParsedMessage testPromptForBehavior(Path chatDataPath, List<String> messages, String goodBehavior, String badBehavior) {
        LOGGER.info("Testing '" + chatDataPath.getFileName() + "' with '" + messages.toString() +
                "' expecting behavior: " + goodBehavior + " and avoid: " + badBehavior);

        try {
            // Enforce rate limit
            rateLimiter.acquire();

            try {
                // Load entity chat data
                String chatDataPathContents = readFileContents(chatDataPath);
                EntityTestData entityTestData = gson.fromJson(chatDataPathContents, EntityTestData.class);

                // Load context
                Map<String, String> contextData = entityTestData.getPlayerContext(worldPath, playerPath, entityPigPath);
                assertNotNull(contextData);

                // Add test message
                for (String message : messages) {
                    entityTestData.addMessage(message, ChatDataManager.ChatSender.USER, "TestPlayer1");
                }

                // Get prompt
                Path promptPath = Paths.get(PROMPT_PATH, "system-chat");
                String promptText = Files.readString(promptPath);
                assertNotNull(promptText);

                // Fetch HTTP response from ChatGPT
                CompletableFuture<String> future = ChatGPTRequest.fetchMessageFromChatGPT(
                        config, promptText, contextData, entityTestData.previousMessages, false);

                try {
                    String outputMessage = future.get(60 * 60, TimeUnit.SECONDS);
                    assertNotNull(outputMessage);

                    // Chat Message: Check for behaviors
                    ParsedMessage result = MessageParser.parseMessage(outputMessage.replace("\n", " "));

                    // Save model outputs (for comparison later)
                    String[] filePathParts = chatDataPath.toString().split("/");
                    String Key = filePathParts[filePathParts.length - 1] + ": " + messages.get(0);
                    outputData.putIfAbsent(Key, new HashMap<>());
                    outputData.get(Key).put(config.getModel(), result.getCleanedMessage());

                    // Check for the presence of good behavior
                    if (goodBehavior != null) {
                        if (goodBehavior.contains("FRIENDSHIP")) {
                            boolean isPositive = goodBehavior.equals("FRIENDSHIP+");
                            assertTrue(result.getBehaviors().stream().anyMatch(b -> "FRIENDSHIP".equals(b.getName()) &&
                                    ((isPositive && b.getArgumentAsInt() > 0) || (!isPositive && b.getArgumentAsInt() < 0))));
                        } else {
                            assertTrue(result.getBehaviors().stream().anyMatch(b -> goodBehavior.equals(b.getName())));
                        }
                    }

                    // Check for the absence of bad behavior if badBehavior is not empty
                    if (badBehavior != null && !badBehavior.isEmpty()) {
                        assertTrue(result.getBehaviors().stream().noneMatch(b -> badBehavior.equals(b.getName())));
                    }

                    return result;

                } catch (TimeoutException e) {
                    fail("The asynchronous operation timed out.");
                } catch (Exception e) {
                    fail("The asynchronous operation failed: " + e.getMessage());
                }

            } catch (IOException e) {
                e.printStackTrace();
                fail("Failed to read the file: " + e.getMessage());
            }
            LOGGER.info("");

        } catch (InterruptedException e) {
            LOGGER.warn("Rate limit enforcement interrupted: " + e.getMessage());
        }
        return null;
    }

    public static String readFileContents(Path filePath) {
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static Map<String, Map<String, String>> loadExistingOutputData() {
        try {
            Path path = Paths.get(OUTPUT_JSON_PATH);
            if (Files.exists(path)) {
                String content = Files.readString(path);
                return gson.fromJson(content, Map.class);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read existing output JSON: {}", e.getMessage());
        }
        return new HashMap<>();
    }

}
