package com.owlmaddie.player2;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Player2APIService {
    static Logger LOGGER = LoggerFactory.getLogger("creaturepals");

    private static final String BASE_URL = "https://api.player2.game";
    
    /**
     * Get the Player2 API key from Player2StartupHandler
     * @return The API key, or null if not set
     */
    private static String getApiKey() {
        return Player2StartupHandler.getApiKey();
    }

    /**
     * Handles boilerplate logic for interacting with the Player2 API endpoint
     *
     * @param endpoint    The API endpoint (e.g., "/v1/chat/completions").
     * @param postRequest True -> POST request, False -> GET request
     * @param requestBody JSON payload to send.
     * @return A map containing JSON keys and values from the response.
     * @throws Exception If there is an error.
     */
    private static Map<String, JsonElement> sendRequest(String endpoint, boolean postRequest, JsonObject requestBody)
            throws Exception {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("PLAYER2_API_KEY environment variable is not set");
        }

        URL url = new URI(BASE_URL + endpoint).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(postRequest ? "POST" : "GET");

        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json; charset=utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);

        LOGGER.debug("Sending %s request to %s\n", postRequest ? "POST" : "GET", endpoint);

        if (postRequest && requestBody != null) {
            LOGGER.debug("Request Body: %s\n", requestBody);
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        int responseCode = connection.getResponseCode();

        if (responseCode != 200) {
            // read error info:
            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(errorStream, StandardCharsets.UTF_8));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                errorReader.close();
                LOGGER.error("Error response: " + errorResponse);
            }
            throw new IOException("HTTP " + responseCode + ": " + connection.getResponseMessage());
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
        Map<String, JsonElement> responseMap = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : jsonResponse.entrySet()) {
            responseMap.put(entry.getKey(), entry.getValue());
        }
        LOGGER.debug("DONE %s request to %s \n", postRequest ? "POST" : "GET", endpoint);

        return responseMap;
    }

    /**
     * Send a heartbeat to check API status and get client version
     */
    public static void sendHeartbeat() {
        try {
            LOGGER.debug("Sending Heartbeat to Player2 API");
            Map<String, JsonElement> responseMap = sendRequest("/v1/health", false, null);
            if (responseMap.containsKey("status")) {
                String status = responseMap.get("status").getAsString();
                LOGGER.debug("Heartbeat Successful - Status: " + status);
            }
        } catch (Exception e) {
            LOGGER.error("Heartbeat Fail: %s", e.getMessage());
        }
    }

    /**
     * Convert text to speech using Player2 TTS API
     * 
     * @param message The text message to convert to speech
     * @param voiceId The voice ID to use for TTS
     * @param entityId The entity ID this TTS is for (for audio tracking)
     */
    public static void textToSpeech(String message, String voiceId, UUID entityId) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("text", message);

            // voice_ids should be an array of strings according to the API docs
            JsonArray voiceIdsArray = new JsonArray();
            voiceIdsArray.add(voiceId);
            requestBody.add("voice_ids", voiceIdsArray);

            requestBody.addProperty("speed", 1.0);
            requestBody.addProperty("audio_format", "wav");

            LOGGER.debug("Sending TTS request: " + message);
            Map<String, JsonElement> response = null;
            try {
                response = sendRequest("/v1/tts/speak", true, requestBody);
                // Handle the audio response - Player2 returns audio data
                if (response.containsKey("data")) {
                    String audioData = response.get("data").getAsString();
                    LOGGER.debug("TTS audio generated: " + audioData);

                    try {
                        File tempFile = File.createTempFile("player2_tts_", ".wav");

                        // Check if it's a data URL (base64 encoded) or regular URL
                        if (audioData.startsWith("data:")) {
                            // Handle base64 data URL
                            String base64Data = audioData.substring(audioData.indexOf(",") + 1);
                            byte[] audioBytes = java.util.Base64.getDecoder().decode(base64Data);
                            java.nio.file.Files.write(tempFile.toPath(), audioBytes);
                            LOGGER.debug("Decoded base64 audio data to: " + tempFile.getAbsolutePath());
                        } else {
                            // Handle regular URL
                            URL url = new URL(audioData);
                            InputStream in = url.openStream();
                            java.nio.file.Files.copy(in, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            in.close();
                            LOGGER.debug("Downloaded TTS audio to: " + tempFile.getAbsolutePath());
                        }

                        // Now play the audio file using our custom audio handler
                        // Pass the actual entity ID for proper tracking
                        boolean playbackStarted = Player2AudioHandler.playAudioFile(tempFile, entityId);

                        if (playbackStarted) {
                            LOGGER.debug("TTS audio playback started successfully");
                        } else {
                            LOGGER.error("Failed to start TTS audio playback");
                        }

                        // Clean up the temporary file after a delay
                        tempFile.deleteOnExit();
                    } catch (Exception ex) {
                        LOGGER.error("Failed to play TTS audio: " + ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Get available voices from Player2 API
     * 
     * @return List of available voice objects
     */
    public static List<JsonObject> getVoices() {
        try {
            Map<String, JsonElement> responseMap = sendRequest("/v1/tts/voices", false, null);
            JsonElement voicesJsonElement = responseMap.get("voices");
            if (voicesJsonElement != null && voicesJsonElement.isJsonArray()) {
                JsonArray voicesJsonArray = voicesJsonElement.getAsJsonArray();

                List<JsonObject> voiceIds = new ArrayList<>();
                for (JsonElement voiceElement : voicesJsonArray) {
                    JsonObject voiceObject = voiceElement.getAsJsonObject();
                    voiceIds.add(voiceObject);
                }
                return voiceIds;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get voices: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Create a chat completion using Player2 Chat API
     * 
     * @param messages List of chat messages
     * @param model The model to use (e.g., "gpt-4", "gpt-3.5-turbo")
     * @return The chat completion response
     */
    public static Map<String, JsonElement> createChatCompletion(List<JsonObject> messages, String model) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            requestBody.add("messages", new Gson().toJsonTree(messages));
            requestBody.addProperty("max_tokens", 1000);
            requestBody.addProperty("temperature", 0.7);
            
            LOGGER.debug("Sending chat completion request");
            return sendRequest("/v1/chat/completions", true, requestBody);
        } catch (Exception e) {
            LOGGER.error("Chat completion request failed: " + e.getMessage());
            return Collections.emptyMap();
        }
    }
}
