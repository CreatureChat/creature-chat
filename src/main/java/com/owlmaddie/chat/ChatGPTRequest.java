// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.chat;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.owlmaddie.commands.ConfigurationHandler;
import com.owlmaddie.json.ChatGPTResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * The {@code ChatGPTRequest} class is used to send HTTP requests to our LLM to generate
 * messages.
 */
public class ChatGPTRequest {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static final Gson GSON = new Gson();
    public static String lastErrorMessage;
    public static int lastErrorCode = 0;

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
            ErrorResponse response = GSON.fromJson(errorResponse, ErrorResponse.class);

            if (response != null && response.error != null) {
                LOGGER.error("Error Message: " + response.error.message);
                LOGGER.error("Error Type: " + response.error.type);
                LOGGER.error("Error Code: " + response.error.code);
                return response.error.message != null ? response.error.message : "Unknown error";
            } else {
                // Some gateways return {"message":"Internal server error"} or similar
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = GSON.fromJson(errorResponse, Map.class);
                    Object msg = (m != null) ? m.get("message") : null;
                    if (msg instanceof String && !((String) msg).isEmpty()) {
                        LOGGER.error("Gateway error message: " + msg);
                        return (String) msg;
                    }
                } catch (Exception ignore) {
                    // fall through to generic handling below
                }
                LOGGER.error("Unknown error response: " + errorResponse);
                return "Unknown error";
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

    private static String sanitizeApiKey(String message, String apiKey) {
        if (message == null || apiKey == null || apiKey.isEmpty()) {
            return message;
        }
        return message.replace(apiKey, "**********");
    }

    public static CompletableFuture<String> fetchMessageFromChatGPT(ConfigurationHandler.Config config, String systemPrompt, Map<String, String> contextData, List<ChatMessage> messageHistory, Boolean jsonMode) {
        // Init API & LLM details
        String apiUrl = config.getUrl();
        String apiKey = config.getApiKey();
        String modelName = config.getModel();
        Integer timeout = config.getTimeout() * 1000;
        int maxContextTokens = config.getMaxContextTokens();
        int maxOutputTokens = config.getMaxOutputTokens();
        double percentOfContext = config.getPercentOfContext();

        return CompletableFuture.supplyAsync(() -> {
            lastErrorCode = 0;
            HttpURLConnection connection = null;
            try {
                // Replace placeholders
                String systemMessage = replacePlaceholders(systemPrompt, contextData);

                URL url = new URL(apiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Accept-Encoding", "gzip");
                connection.setDoOutput(true);
                connection.setConnectTimeout(timeout);
                connection.setReadTimeout(timeout);

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
                ChatGPTRequestPayload payload = new ChatGPTRequestPayload(
                        modelName, messages, jsonMode, 1.0f, maxOutputTokens);

                Gson gsonInput = new Gson();
                String jsonInputString = gsonInput.toJson(payload);

                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(input.length);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(input);
                }

                // Check for error message in response
                int statusCode = connection.getResponseCode();
                if (statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                    lastErrorCode = statusCode;
                    final String reason = connection.getResponseMessage() != null ? connection.getResponseMessage() : "";

                    // Try to capture helpful IDs for tracing through AWS and OpenAI
                    final String awsRequestId    = connection.getHeaderField("x-amzn-RequestId");
                    final String awsErrorType    = connection.getHeaderField("x-amzn-ErrorType");
                    final String openaiRequestId = connection.getHeaderField("x-request-id");

                    // Log AWS headers only for debugging so they don't bloat user-facing messages
                    if (awsRequestId != null) LOGGER.debug("AWS Request ID: {}", awsRequestId);
                    if (awsErrorType != null) LOGGER.debug("AWS Error Type: {}", awsErrorType);
                    if (openaiRequestId != null) LOGGER.debug("OpenAI Request ID: {}", openaiRequestId);

                    InputStream errStream = connection.getErrorStream();
                    if (errStream == null) {
                        try {
                            errStream = connection.getInputStream();
                        } catch (Exception ex) {
                            LOGGER.error("Failed to obtain error stream", ex);
                            String msg = reason != null ? reason : ("HTTP error " + statusCode);
                            StringBuilder base = new StringBuilder();
                            base.append("HTTP ").append(statusCode);
                            if (msg != null && !msg.isEmpty()) base.append(" ").append(msg);

                            lastErrorMessage = sanitizeApiKey(base + ": " + ex.getMessage(), apiKey);
                            return null;
                        }
                    }
                    if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
                        errStream = new GZIPInputStream(errStream);
                    }
                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(errStream, StandardCharsets.UTF_8))) {
                        String line;
                        StringBuilder errorResponse = new StringBuilder();
                        while ((line = errorReader.readLine()) != null) {
                            errorResponse.append(line.trim());
                        }

                        // Try known shapes first
                        String cleanError = parseAndLogErrorResponse(errorResponse.toString());

                        // Build a richer message (status + reason + IDs + short body preview)
                        StringBuilder sb = new StringBuilder();
                        sb.append("HTTP ").append(statusCode);
                        if (!reason.isEmpty()) sb.append(" ").append(reason);

                        if (cleanError != null && !cleanError.isEmpty() && !"Unknown error".equals(cleanError)) {
                            sb.append(": ").append(cleanError);
                        } else if (errorResponse.length() > 0) {
                            String bodyPreview = errorResponse.length() > 300
                                    ? errorResponse.substring(0, 300) + "..."
                                    : errorResponse.toString();
                            sb.append(": ").append(bodyPreview);
                        }

                        String finalMsg = sb.toString();
                        LOGGER.error(finalMsg);
                        lastErrorMessage = sanitizeApiKey(finalMsg, apiKey);
                    } catch (Exception e) {
                        LOGGER.error("Failed to read error response", e);
                        lastErrorMessage = sanitizeApiKey("Failed to read error response: " + e.getMessage(), apiKey);
                    }
                    return null;
                } else {
                    lastErrorMessage = null;
                    lastErrorCode = 0;
                }

                InputStream inStream = connection.getInputStream();
                if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
                    inStream = new GZIPInputStream(inStream);
                }
                try (BufferedReader br = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    ChatGPTResponse chatGPTResponse = GSON.fromJson(response.toString(), ChatGPTResponse.class);
                    if (chatGPTResponse != null && chatGPTResponse.choices != null && !chatGPTResponse.choices.isEmpty()) {
                        return chatGPTResponse.choices.get(0).message.content;
                    }
                    lastErrorMessage = "Failed to parse response";
                    return null;
                }
            } catch (SocketException | SocketTimeoutException ce) {
                LOGGER.warn("Connection failed", ce);
                lastErrorMessage = "No Internet or Blocked Request: " + ce.getMessage();
                lastErrorCode = -1;
                return null;
            } catch (Exception e) {
                LOGGER.error("Failed to request message", e);
                lastErrorMessage = sanitizeApiKey("Failed to request message: " + e.getMessage(), apiKey);
                lastErrorCode = 0;
                return null;
            }
        });
    }
}

