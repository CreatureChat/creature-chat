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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
    String NO_API_KEY = "No API_KEY environment variable has been set.";

    // Requests per second limit
    private static final RateLimiter rateLimiter = new RateLimiter(1);

    ConfigurationHandler.Config config = null;
    String systemChatContents = null;

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

    Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    Gson gson = new GsonBuilder().create();

    @BeforeEach
    public void setup() {
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
        assertNotNull(API_KEY, NO_API_KEY);

        // Load system chat prompt
        systemChatContents = readFileContents(systemChatPath);
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
            testPromptForBehavior(bravePath, List.of(message), "STOP", "FOLLOW");
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
    public void friendshipUpNervous() {
        ParsedMessage result = testPromptForBehavior(nervousPath, friendshipUpMessages, "FRIENDSHIP", "");
        assertTrue(result.getBehaviors().stream().anyMatch(b -> "FRIENDSHIP".equals(b.getName()) && b.getArgument() > 0));
    }

    @Test
    public void friendshipUpBrave() {
        ParsedMessage result = testPromptForBehavior(bravePath, friendshipUpMessages, "FRIENDSHIP", "");
        assertTrue(result.getBehaviors().stream().anyMatch(b -> "FRIENDSHIP".equals(b.getName()) && b.getArgument() > 0));
    }

    @Test
    public void friendshipDownNervous() {
        for (String message : friendshipDownMessages) {
            ParsedMessage result = testPromptForBehavior(nervousPath, List.of(message), "FRIENDSHIP", "");
            assertTrue(result.getBehaviors().stream().anyMatch(b -> "FRIENDSHIP".equals(b.getName()) && b.getArgument() < 0));
        }
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

                    // Check for the presence of good behavior
                    assertTrue(result.getBehaviors().stream().anyMatch(b -> goodBehavior.equals(b.getName())));

                    // Check for the absence of bad behavior if badBehavior is not empty
                    if (!badBehavior.isEmpty()) {
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

    public String readFileContents(Path filePath) {
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

}
