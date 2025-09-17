package com.owlmaddie.player2;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TTS {
    static class PerEntityTTS {
        public String voiceId;
        public String lastMessage;
        public PerEntityTTS(String voiceId, String lastMessage) {
            this.voiceId = voiceId;
            this.lastMessage = lastMessage;
        }
    }
    public static final ExecutorService ttsThread = Executors.newSingleThreadExecutor();
    public static ConcurrentHashMap<UUID, PerEntityTTS> entityTTSData = new ConcurrentHashMap<>();
    public static List<String> englishVoices = null;
    public static boolean enabled = true;
    private static final Random random = new Random();

    public static void speak(String message, UUID entityId) {
        if(!enabled){
            return;
        }
        ttsThread.submit(() -> {
            entityTTSData.computeIfAbsent(entityId, id -> {
                if (englishVoices == null) {
                    List<JsonObject> voices = Player2APIService.getVoices();
                    // Filter for English voices - adjust based on actual Player2 voice structure
                    englishVoices = voices.stream().filter(jsonV -> {
                        // Player2 voices might have different structure, so we'll be more flexible
                        if (jsonV.has("language")) {
                            String language = jsonV.get("language").getAsString();
                            return language.toLowerCase().contains("english");
                        } else if (jsonV.has("name")) {
                            String name = jsonV.get("name").getAsString();
                            return name.toLowerCase().contains("en") || name.toLowerCase().contains("english");
                        }
                        // If no language info, include it as a fallback
                        return true;
                    }).map(jsonV -> {
                        // Extract voice ID - Player2 might use different field names
                        if (jsonV.has("id")) {
                            return jsonV.get("id").getAsString();
                        } else if (jsonV.has("voice_id")) {
                            return jsonV.get("voice_id").getAsString();
                        } else if (jsonV.has("name")) {
                            return jsonV.get("name").getAsString();
                        }
                        return "alloy"; // Default fallback voice
                    }).toList();
                    
                    // If no voices found, use a default
                    if (englishVoices.isEmpty()) {
                        englishVoices = List.of("alloy", "echo", "fable", "onyx", "nova");
                    }
                }
                int index = random.nextInt(englishVoices.size());
                PerEntityTTS data = new PerEntityTTS(englishVoices.get(index), "");
                return data;
            });
            PerEntityTTS data = entityTTSData.get(entityId);
            if(data.lastMessage.equals(message)){
                return;
            }
            data.lastMessage = message;
            Player2APIService.textToSpeech(message, data.voiceId, entityId);
        });
    }

    /**
     * Initialize voices on startup
     */
    public static void initializeVoices() {
        ttsThread.submit(() -> {
            List<JsonObject> voices = Player2APIService.getVoices();
            if (!voices.isEmpty()) {
                System.out.println("Loaded " + voices.size() + " voices from Player2 API");
                // Pre-populate english voices
                englishVoices = voices.stream().filter(jsonV -> {
                    if (jsonV.has("language")) {
                        String language = jsonV.get("language").getAsString();
                        return language.toLowerCase().contains("english");
                    } else if (jsonV.has("name")) {
                        String name = jsonV.get("name").getAsString();
                        return name.toLowerCase().contains("en") || name.toLowerCase().contains("english");
                    }
                    return true;
                }).map(jsonV -> {
                    if (jsonV.has("id")) {
                        return jsonV.get("id").getAsString();
                    } else if (jsonV.has("voice_id")) {
                        return jsonV.get("voice_id").getAsString();
                    } else if (jsonV.has("name")) {
                        return jsonV.get("name").getAsString();
                    }
                    return "alloy";
                }).toList();
                
                if (englishVoices.isEmpty()) {
                    englishVoices = List.of("alloy", "echo", "fable", "onyx", "nova");
                }
            }
        });
    }
}
