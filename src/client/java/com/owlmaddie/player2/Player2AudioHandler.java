package com.owlmaddie.player2;

import com.mojang.blaze3d.audio.SoundBuffer;
import com.owlmaddie.utils.ClientEntityFinder;
import net.fabricmc.fabric.mixin.client.sound.SoundInstanceMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles 3D audio playback for Player2 TTS in Fabric using Java's audio system
 */
public class Player2AudioHandler {
    private static final ConcurrentHashMap<UUID, String> activeAudioFiles = new ConcurrentHashMap<>();
    private static final String SOUND_NAMESPACE = "creaturepals";
    private static final String TTS_SOUND_NAME = "tts_audio";
    
    /**
     * Play an OGG audio file using Java's audio system with 3D positioning
     * 
     * @param audioFile The OGG file to play
     * @param entityId The entity ID this audio is for (for tracking)
     * @return true if playback started successfully
     */
    public static boolean playAudioFile(File audioFile, UUID entityId) {
        if (audioFile == null || !audioFile.exists()) {
            System.err.println("Audio file does not exist: " + audioFile);
            return false;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            System.err.println("Minecraft client not ready for audio playback");
            return false;
        }
        
        try {
            System.out.println("Source audio file: " + audioFile.getAbsolutePath());
            System.out.println("Source file size: " + audioFile.length() + " bytes");
            
            // Track this audio file for cleanup
            activeAudioFiles.put(entityId, audioFile.getAbsolutePath());
            
            // Get the entity for 3D positioning
            Entity entity = ClientEntityFinder.getEntityByUUID(mc.level, entityId);
            if (entity == null) {
                System.out.println("Entity not found, playing TTS at player location");
                entity = mc.player;
            }
            
            // Play the audio with 3D positioning
            play3DAudio(audioFile, entity, mc);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to prepare audio file for playback: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Play 3D audio using Java's audio system
     */
    private static void play3DAudio(File audioFile, Entity entity, Minecraft mc) {
        try {
            // Get entity position for 3D sound
            Vec3 entityPos = entity.position();
            Vec3 playerPos = mc.player.position();
            
            // Calculate distance and direction for 3D audio
            double distance = entityPos.distanceTo(playerPos);
            Vec3 direction = entityPos.subtract(playerPos).normalize();
            
            System.out.println("Playing 3D TTS audio at entity position: " + entityPos);
            System.out.println("Distance from player: " + String.format("%.2f", distance) + " blocks");
            
            // Start audio playback in a separate thread to avoid blocking
            Thread audioThread = new Thread(() -> {
                try {
                    // Load the audio file
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                    AudioFormat format = audioStream.getFormat();
                    
                    // Create a data line for playback
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    if (!AudioSystem.isLineSupported(info)) {
                        System.err.println("Audio format not supported: " + format);
                        return;
                    }
                    
                    SourceDataLine audioLine = (SourceDataLine) AudioSystem.getLine(info);
                    audioLine.open(format);
                    audioLine.start();
                    
                    // Calculate 3D audio parameters
                    float volume = calculate3DVolume(distance);
                    float pan = calculate3DPan(direction);
                    
                    System.out.println("3D Audio - Volume: " + String.format("%.2f", volume) + ", Pan: " + String.format("%.2f", pan));
                    
                    // Apply 3D audio effects
                    apply3DEffects(audioLine, volume, pan);
                    
                    // Play the audio
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    
                    while ((bytesRead = audioStream.read(buffer)) != -1) {
                        audioLine.write(buffer, 0, bytesRead);
                    }
                    
                    // Clean up
                    audioLine.drain();
                    audioLine.close();
                    audioStream.close();
                    
                    System.out.println("3D TTS audio playback completed successfully");
                    
                } catch (Exception e) {
                    System.err.println("Failed to play 3D audio: " + e.getMessage());
                    e.printStackTrace();
                    
                    // Fallback to notification sound
                    try {
                        mc.execute(() -> {
                            mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.5F));
                        });
                        System.out.println("Fallback notification sound played");
                    } catch (Exception fallbackEx) {
                        System.err.println("Failed to play fallback sound: " + fallbackEx.getMessage());
                    }
                }
            });
            
            audioThread.setDaemon(true);
            audioThread.start();
            
        } catch (Exception e) {
            System.err.println("Failed to start 3D audio playback: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Calculate 3D volume based on distance
     */
    private static float calculate3DVolume(double distance) {
        // Volume decreases with distance (inverse square law)
        // Max volume at 0 blocks, min volume at 64+ blocks
        double maxDistance = 64.0;
        double volume = Math.max(0.1, 1.0 - (distance / maxDistance));
        return (float) volume;
    }
    
    /**
     * Calculate 3D panning based on direction
     */
    private static float calculate3DPan(Vec3 direction) {
        // Convert 3D direction to stereo panning
        // Left = -1.0, Center = 0.0, Right = 1.0
        double pan = direction.x;
        return (float) Math.max(-1.0, Math.min(1.0, pan));
    }
    
    /**
     * Apply 3D audio effects to the audio line
     */
    private static void apply3DEffects(SourceDataLine audioLine, float volume, float pan) {
        try {
            // Set volume
            FloatControl volumeControl = (FloatControl) audioLine.getControl(FloatControl.Type.MASTER_GAIN);
            if (volumeControl != null) {
                float minGain = volumeControl.getMinimum();
                float maxGain = volumeControl.getMaximum();
                float gain = minGain + (maxGain - minGain) * volume;
                volumeControl.setValue(gain);
            }
            
            // Set panning
            FloatControl panControl = (FloatControl) audioLine.getControl(FloatControl.Type.BALANCE);
            if (panControl != null) {
                panControl.setValue(pan);
            }
            
        } catch (Exception e) {
            System.err.println("Failed to apply 3D audio effects: " + e.getMessage());
        }
    }
    
    /**
     * Clean up audio files for a specific entity
     */
    public static void cleanupAudio(UUID entityId) {
        String audioPath = activeAudioFiles.remove(entityId);
        if (audioPath != null) {
            try {
                File audioFile = new File(audioPath);
                if (audioFile.exists()) {
                    Files.delete(audioFile.toPath());
                    System.out.println("Cleaned up audio file: " + audioPath);
                }
            } catch (IOException e) {
                System.err.println("Failed to cleanup audio file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Clean up all audio files
     */
    public static void cleanupAllAudio() {
        for (UUID entityId : activeAudioFiles.keySet()) {
            cleanupAudio(entityId);
        }
    }
}
