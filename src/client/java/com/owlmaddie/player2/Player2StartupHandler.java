package com.owlmaddie.player2;

import com.owlmaddie.commands.ConfigurationHandler;
import com.owlmaddie.player2.Player2OAuthHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.dialog.ButtonListDialogScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.swing.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles Player2 API key validation and setup on Minecraft startup
 */
public class Player2StartupHandler {
    static Logger LOGGER = LoggerFactory.getLogger("creaturepals");

    private static boolean hasCheckedApiKey = false;
    private static boolean isApiKeyValid = false;
    
    /**
     * Check if the Player2 API key is set and valid
     * This should be called on Minecraft startup
     */
    public static void checkApiKeyOnStartup() {
        if (hasCheckedApiKey) {
            return; // Already checked
        }
        
        hasCheckedApiKey = true;
        LOGGER.info("Player2StartupHandler: Checking API key on startup...");
        
        // Resolve API key from system property, env var, or persisted file
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            // No API key set, show setup screen
            LOGGER.info("Player2StartupHandler: No API key found, showing setup screen");
            showApiKeySetupScreen();
            return;
        }
        
        LOGGER.info("Player2StartupHandler: API key found, validating...");
        
        // API key is set, validate it
        validateApiKey(apiKey);
    }
    
    /**
     * Validate the provided API key by sending a test request
     */
    private static void validateApiKey(String apiKey) {
        CompletableFuture.runAsync(() -> {
            try {
                // Test the API key with a simple heartbeat
                Player2APIService.sendHeartbeat();
                isApiKeyValid = true;
                LOGGER.info("Player2 API key validated successfully: " + apiKey);
            } catch (Exception e) {
                isApiKeyValid = false;
                LOGGER.warn("Player2 API key validation failed: " + e.getMessage());
                
                // Show error screen on main thread
                Minecraft.getInstance().execute(() -> {
                    showApiKeyErrorScreen(e.getMessage());
                });
            }
        });
    }
    
    /**
     * Show the API key setup screen
     */
    private static void showApiKeySetupScreen() {
        LOGGER.info("Player2StartupHandler: showApiKeySetupScreen called");
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            // If we're on the title screen, show the OAuth screen directly
            // Otherwise, show it on the current screen
            Screen currentScreen = client.screen;
            LOGGER.info("Player2StartupHandler: Current screen: " + (currentScreen != null ? currentScreen.getClass().getSimpleName() : "null"));
            if (currentScreen instanceof TitleScreen) {
                LOGGER.info("Player2StartupHandler: On title screen, starting OAuth flow");
                Player2OAuthHandler.startOAuthFlow(null);
            } else if (currentScreen != null) {
                LOGGER.info("Player2StartupHandler: On other screen, starting OAuth flow");
                Player2OAuthHandler.startOAuthFlow(currentScreen);
            }
        } else {
            LOGGER.warn("Player2StartupHandler: Minecraft client is null!");
        }
    }
    
    /**
     * Show the API key error screen
     */
    private static void showApiKeyErrorScreen(String errorMessage) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.screen != null) {
            client.setScreen(new Player2ApiKeyErrorScreen(client.screen, errorMessage));
        }
    }
    
    /**
     * Check if the API key is currently valid
     */
    public static boolean isApiKeyValid() {
        return isApiKeyValid;
    }
    
    /**
     * Check if the API key has been checked
     */
    public static boolean hasCheckedApiKey() {
        return hasCheckedApiKey;
    }
    
    /**
     * Force revalidation of the API key
     */
    public static void revalidateApiKey() {
        hasCheckedApiKey = false;
        isApiKeyValid = false;
        checkApiKeyOnStartup();
    }
    
    /**
     * Clear the API key from system properties
     */
    public static void clearApiKey() {
        System.clearProperty("PLAYER2_API_KEY");
        hasCheckedApiKey = false;
        isApiKeyValid = false;
    }
    
    /**
     * Set the API key in system properties and file
     */
    public static void setApiKey(String apiKey) {
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            // Store in system properties for current session
            System.setProperty("PLAYER2_API_KEY", apiKey.trim());
            
            // Store in file for persistence
            try {
                String minecraftDir = null;
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    minecraftDir = System.getenv("APPDATA") + "\\.minecraft";
                } else if (os.contains("mac")) {
                    minecraftDir = System.getProperty("user.home") + "/Library/Application Support/minecraft";
                } else {
                    // Assume Linux/Unix
                    minecraftDir = System.getProperty("user.home") + "/.minecraft";
                }
                java.io.File file = new java.io.File(minecraftDir, "p2key.txt");
                java.nio.file.Files.write(file.toPath(), apiKey.trim().getBytes());
                LOGGER.debug("Player2 API key saved to: " + file.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.warn("Failed to save API key to file: " + e.getMessage());
            }
            
            hasCheckedApiKey = false;
            isApiKeyValid = false;
        }
    }
    /**
     * Get the API key from system properties, environment variable, or file
     */
    @Nullable
    public static String getApiKey() {
        // First try system properties (current session)
        String apiKey = System.getProperty("PLAYER2_API_KEY");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return apiKey.trim();
        }
        
        // Then try environment variable
        apiKey = System.getenv("PLAYER2_API_KEY");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return apiKey.trim();
        }
        
        // Finally try reading from file
        try {
            String minecraftDir = null;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                minecraftDir = System.getenv("APPDATA") + "\\.minecraft";
            } else if (os.contains("mac")) {
                minecraftDir = System.getProperty("user.home") + "/Library/Application Support/minecraft";
            } else {
                // Assume Linux/Unix
                minecraftDir = System.getProperty("user.home") + "/.minecraft";
            }
            java.io.File file = new java.io.File(minecraftDir, "p2key.txt");
            if (file.exists()) {
                apiKey = new String(java.nio.file.Files.readAllBytes(file.toPath())).trim();
                if (!apiKey.isEmpty()) {
                    // Store in system properties for this session
                    System.setProperty("PLAYER2_API_KEY", apiKey);
                    return apiKey;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read API key from file: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Screen for setting up the Player2 API key
     */
    public static class Player2ApiKeySetupScreen extends Screen {
        private final Screen parent;
        private EditBox apiKeyField;
        private Button saveButton;
        private Button skipButton;
        private Button helpButton;
        
        public Player2ApiKeySetupScreen(Screen parent) {
            super(Component.literal("Player2 API Key Setup"));
            this.parent = parent;
            LOGGER.debug("Player2ApiKeySetupScreen: Constructor called with parent: " + (parent != null ? parent.getClass().getSimpleName() : "null"));
        }
        
        @Override
        protected void init() {
            super.init();
            LOGGER.debug("Player2ApiKeySetupScreen: init() called");
            
            int centerX = width / 2;
            int centerY = height / 2;
            
            // Title
            addRenderableWidget(Button.builder(
                Component.literal("Player2 API Key Required"),
                button -> {}
            ).bounds(centerX - 100, centerY - 80, 200, 20).build());
            
            // Description
            addRenderableWidget(Button.builder(
                Component.literal("This mod requires a Player2 API key to function."),
                button -> {}
            ).bounds(centerX - 150, centerY - 50, 300, 20).build());

            addRenderableWidget(Button.builder(
                Component.literal("Get your key at: player2.game"),
                button -> {}
            ).bounds(centerX - 150, centerY - 30, 300, 20).build());
            
            // API Key input field
            apiKeyField = new net.minecraft.client.gui.components.EditBox(
                minecraft.font,
                centerX - 100,
                centerY,
                200,
                20,
                Component.literal("Enter your Player2 API key")
            );

            apiKeyField.setMaxLength(100);
            apiKeyField.setResponder(text -> {
                saveButton.active = !text.trim().isEmpty();
            });
            
            // Save button
            saveButton = Button.builder(
                Component.literal("Save & Validate"),
                button -> saveApiKey()
            ).bounds(centerX - 100, centerY + 30, 200, 20).build();
            saveButton.active = false;
            
            // Skip button
            skipButton = Button.builder(
                Component.literal("Skip for now"),
                button -> onClose()
            ).bounds(centerX - 100, centerY + 60, 200, 20).build();
            
            // Help button
            helpButton = Button.builder(
                Component.literal("Help"),
                button -> showHelp()
            ).bounds(centerX - 100, centerY + 90, 200, 20).build();
            
            addWidget(apiKeyField);
            addRenderableWidget(apiKeyField);
            addRenderableWidget(saveButton);
            addRenderableWidget(skipButton);
            addRenderableWidget(helpButton);
            
            setInitialFocus(apiKeyField);
        }
        
        private void saveApiKey() {
            String apiKey = apiKeyField.getMessage().tryCollapseToString();
            if (apiKey.isEmpty()) {
                return;
            }
            
            // Set the API key in system property for this session
            try {
                System.setProperty("PLAYER2_API_KEY", apiKey);
                
                // Validate the key
                validateApiKey(apiKey);
                
                // Show success message
                minecraft.setScreen(new ConfirmScreen(
                    this::onValidationComplete,
                    Component.literal("API Key Saved"),
                    Component.literal("Your Player2 API key has been saved for this session.\n\nNote: You'll need to set this as an environment variable for permanent storage."),
                    Component.literal("Continue"),
                    ButtonListDialogScreen.DISCONNECT
                ));
                
            } catch (Exception e) {
                minecraft.setScreen(new ConfirmScreen(
                    button -> minecraft.setScreen(this),
                    Component.literal("Error"),
                    Component.literal("Failed to save API key: " + e.getMessage()),
                    Component.literal("OK"),
                        ButtonListDialogScreen.DISCONNECT

                ));
            }
        }
        
        private void onValidationComplete(boolean confirmed) {
            if (confirmed) {
                onClose();
            }
        }
        
        private void showHelp() {
            minecraft.setScreen(new ConfirmScreen(
                button -> minecraft.setScreen(this),
                Component.literal("How to Set Player2 API Key"),
                Component.literal("1. Visit player2.game and sign up\n2. Get your API key from your account\n3. Set it as an environment variable:\n\nWindows (PowerShell):\n$env:PLAYER2_API_KEY=\"your_key\"\n\nWindows (CMD):\nset PLAYER2_API_KEY=your_key\n\nLinux/macOS:\nexport PLAYER2_API_KEY=\"your_key\"\n\n4. Restart Minecraft"),
                Component.literal("OK"),
                    ButtonListDialogScreen.DISCONNECT

            ));
        }
        
        @Override
        public void onClose() {
            if (parent != null) {
                minecraft.setScreen(parent);
            } else {
                // If no parent screen, go back to title screen
                minecraft.setScreen(new TitleScreen());
            }
        }

    }
    
    /**
     * Screen for showing API key validation errors
     */
    public static class Player2ApiKeyErrorScreen extends Screen {
        private final Screen parent;
        private final String errorMessage;
        
        public Player2ApiKeyErrorScreen(Screen parent, String errorMessage) {
            super(Component.literal("Player2 API Key Error"));
            this.parent = parent;
            this.errorMessage = errorMessage;
        }
        
        @Override
        protected void init() {
            super.init();
            
            int centerX = width / 2;
            int centerY = height / 2;
            
            // Error title
            addRenderableWidget(Button.builder(
                Component.literal("API Key Validation Failed"),
                button -> {}
            ).bounds(centerX - 100, centerY - 80, 200, 20).build());
            
            // Error message
            addRenderableWidget(Button.builder(
                Component.literal("Error: " + errorMessage),
                button -> {}
            ).bounds(centerX - 100, centerY - 50, 300, 20).build());
            
            // Retry button
            addRenderableWidget(Button.builder(
                Component.literal("Retry"),
                button -> retryValidation()
            ).bounds(centerX - 100, centerY, 200, 20).build());
            
            // Setup button
            addRenderableWidget(Button.builder(
                Component.literal("Setup New Key"),
                button -> setupNewKey()
            ).bounds(centerX - 100, centerY + 30, 200, 20).build());
            
            // Continue anyway button
            addRenderableWidget(Button.builder(
                Component.literal("Continue Anyway"),
                button -> onClose()
            ).bounds(centerX - 100, centerY + 60, 200, 20).build());
        }
        
        private void retryValidation() {
            Player2StartupHandler.revalidateApiKey();
            onClose();
        }
        
        private void setupNewKey() {
            minecraft.setScreen(new Player2ApiKeySetupScreen(this));
        }
        
        @Override
        public void onClose() {
            if (parent != null) {
                minecraft.setScreen(parent);
            } else {
                // If no parent screen, go back to title screen
                minecraft.setScreen(new TitleScreen());
            }
        }
        

    }
}
