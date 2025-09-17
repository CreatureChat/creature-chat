// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.chat;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.owlmaddie.commands.ConfigurationHandler;
import com.owlmaddie.json.ChatGPTResponse;
import com.owlmaddie.network.S2C.AuthRequestPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * The {@code ChatGPTRequest} class is used to send HTTP requests to our LLM to generate
 * messages.
 */
public class ChatGPTRequest {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturepals");
    public static String lastErrorMessage;


    public static Map<UUID, CompletableFuture<String>> apiKeyAwaiter = new ConcurrentHashMap<>();


    static class ChatGPTRequestMessage {
        String role;
        String content;

        public ChatGPTRequestMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    static class ChatGPTRequestPayload {
        String model;
        List<ChatGPTRequestMessage> messages;
        ResponseFormat response_format;
        float temperature;
        int max_tokens;
        boolean stream;

        public ChatGPTRequestPayload(String model, List<ChatGPTRequestMessage> messages, Boolean jsonMode, float temperature, int maxTokens) {
            this.model = model;
            this.messages = messages;
            this.temperature = temperature;
            this.max_tokens = maxTokens;
            this.stream = false;
            if (jsonMode) {
                this.response_format = new ResponseFormat("json_object");
            } else {
                this.response_format = new ResponseFormat("text");
            }
        }
    }

    static class ResponseFormat {
        String type;

        public ResponseFormat(String type) {
            this.type = type;
        }
    }

    public static String removeQuotes(String str) {
        if (str != null && str.length() > 1 && str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    // Class to represent the error response structure
    public static class ErrorResponse {
        Error error;

        static class Error {
            String message;
            String type;
            String code;
        }
    }

    public static String parseAndLogErrorResponse(String errorResponse) {
        try {
            Gson gson = new Gson();
            ErrorResponse response = gson.fromJson(errorResponse, ErrorResponse.class);

            if (response.error != null) {
                LOGGER.error("Error Message: " + response.error.message);
                LOGGER.error("Error Type: " + response.error.type);
                LOGGER.error("Error Code: " + response.error.code);
                return response.error.message;
            } else {
                LOGGER.error("Unknown error response: " + errorResponse);
                return "Unknown: " + errorResponse;
            }
        } catch (JsonSyntaxException e) {
            LOGGER.warn("Failed to parse error response as JSON, falling back to plain text");
            LOGGER.error("Error response: " + errorResponse);
        } catch (Exception e) {
            LOGGER.error("Failed to parse error response", e);
        }
        return removeQuotes(errorResponse);
    }

    // Function to replace placeholders in the template
    public static String replacePlaceholders(String template, Map<String, String> replacements) {
        String result = template;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replaceAll(Pattern.quote("{{" + entry.getKey() + "}}"), entry.getValue());
        }
        return result.replace("\"", "") ;
    }

    // Function to roughly estimate # of OpenAI tokens in String
    private static int estimateTokenSize(String text) {
        return (int) Math.round(text.length() / 3.5);
    }



    public static String startChat(ConfigurationHandler.Config config, String systemPrompt, Map<String, String> contextData, List<ChatMessage> messageHistory, Boolean jsonMode, ServerPlayer player) {
        CompletableFuture<String> apiKeyFuture = new CompletableFuture<String>().orTimeout(30, java.util.concurrent.TimeUnit.SECONDS);

        UUID authRequestId = UUID.randomUUID();
        apiKeyAwaiter.put(authRequestId, apiKeyFuture);
        CompletableFuture<String> messageFuture = apiKeyAwaiter.get(authRequestId).thenComposeAsync((apiKey) -> {
            LOGGER.info("Received API key from player {}", player.getName().getString());
            return fetchMessageFromChatGPT(config, systemPrompt, contextData, messageHistory, jsonMode, apiKey);
        });


        ServerPlayNetworking.send(player, new AuthRequestPayload(authRequestId));

        return messageFuture.join();
    }


    public static CompletableFuture<String> fetchMessageFromChatGPT(ConfigurationHandler.Config config, String systemPrompt, Map<String, String> contextData, List<ChatMessage> messageHistory, Boolean jsonMode, String apiKey) {


        // Init API & LLM details
        String apiUrl = "https://api.player2.game/v1/chat/completions";
        String modelName = config.getModel();
        int timeout = config.getTimeout() * 1000;
        int maxContextTokens = config.getMaxContextTokens();
        int maxOutputTokens = config.getMaxOutputTokens();
        double percentOfContext = config.getPercentOfContext();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Replace placeholders
                String systemMessage = replacePlaceholders(systemPrompt, contextData);

                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setDoOutput(true);
                connection.setConnectTimeout(timeout); // 10 seconds connection timeout
                connection.setReadTimeout(timeout); // 10 seconds read timeout

                // Create messages list (for chat history)
                List<ChatGPTRequestMessage> messages = new ArrayList<>();

                // Don't exceed a specific % of total context window (to limit message history in request)
                int remainingContextTokens = (int) ((maxContextTokens - maxOutputTokens) * percentOfContext);
                int usedTokens = estimateTokenSize("system: " + systemMessage);

                // Iterate backwards through the message history
                for (int i = messageHistory.size() - 1; i >= 0; i--) {
                    ChatMessage chatMessage = messageHistory.get(i);
                    String senderName = chatMessage.sender.toString().toLowerCase(Locale.ENGLISH);
                    String messageText = replacePlaceholders(chatMessage.message, contextData);
                    int messageTokens = estimateTokenSize(senderName + ": " + messageText);

                    if (usedTokens + messageTokens > remainingContextTokens) {
                        break;  // If adding this message would exceed the token limit, stop adding more messages
                    }

                    // Add the message to the temporary list
                    messages.add(new ChatGPTRequestMessage(senderName, messageText));
                    usedTokens += messageTokens;
                }

                // Add system message
                messages.add(new ChatGPTRequestMessage("system", systemMessage));

                // Reverse the list to restore chronological order
                // This is needed since we build the list in reverse order for token restricting above
                Collections.reverse(messages);

                // Convert JSON to String
                ChatGPTRequestPayload payload = new ChatGPTRequestPayload(modelName, messages, jsonMode, 1.0f, maxOutputTokens);
                Gson gsonInput = new Gson();
                String jsonInputString = gsonInput.toJson(payload);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Check for error message in response
                if (connection.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                        String errorLine;
                        StringBuilder errorResponse = new StringBuilder();
                        while ((errorLine = errorReader.readLine()) != null) {
                            errorResponse.append(errorLine.trim());
                        }

                        // Parse and log the error response using Gson
                        String cleanError = parseAndLogErrorResponse(errorResponse.toString());
                        lastErrorMessage = cleanError;
                    } catch (Exception e) {
                        LOGGER.error("Failed to read error response", e);
                        lastErrorMessage = "Failed to read error response: " + e.getMessage();
                    }
                    return null;
                } else {
                    lastErrorMessage = null;
                }

                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    Gson gsonOutput = new Gson();
                    ChatGPTResponse chatGPTResponse = gsonOutput.fromJson(response.toString(), ChatGPTResponse.class);
                    if (chatGPTResponse != null && chatGPTResponse.choices != null && !chatGPTResponse.choices.isEmpty()) {
                        String content = chatGPTResponse.choices.get(0).message.content;
                        return content;
                    } else {
                        lastErrorMessage = "Failed to parse response from LLM";
                        return null;
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to request message from LLM", e);
                lastErrorMessage = "Failed to request message from LLM: " + e.getMessage();
                return null;
            }
        });
    }
}

