// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.ui.BubbleRenderer;
import com.owlmaddie.ui.PlayerMessageManager;
import com.owlmaddie.utils.ClientEntityFinder;
import com.owlmaddie.utils.Decompression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * The {@code ClientPackets} class provides methods to send packets to/from the server for generating greetings,
 * updating message details, and sending user messages.
 */
public class ClientPackets {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturepals");
    static HashMap<Integer, byte[]> receivedChunks = new HashMap<>();

    public static void sendGenerateGreeting(Entity entity) {
        // Get user language
        String userLanguageCode = Minecraft.getInstance().getLanguageManager().getSelected();
        String userLanguageName = Minecraft.getInstance().getLanguageManager().getLanguage(userLanguageCode).toComponent().getString();

        FriendlyByteBuf buf = ClientBufferHelper.create();
        buf.writeUtf(entity.getStringUUID());
        buf.writeUtf(userLanguageName);

        // Send C2S packet
        ClientPacketHelper.send(ServerPackets.PACKET_C2S_GREETING, buf);
    }

    public static void sendUpdateLineNumber(Entity entity, Integer lineNumber) {
        FriendlyByteBuf buf = ClientBufferHelper.create();
        buf.writeUtf(entity.getStringUUID());
        buf.writeInt(lineNumber);

        // Send C2S packet
        ClientPacketHelper.send(ServerPackets.PACKET_C2S_READ_NEXT, buf);
    }

    public static void sendOpenChat(Entity entity) {
        FriendlyByteBuf buf = ClientBufferHelper.create();
        buf.writeUtf(entity.getStringUUID());

        // Send C2S packet
        ClientPacketHelper.send(ServerPackets.PACKET_C2S_OPEN_CHAT, buf);
    }

    public static void sendCloseChat() {
        FriendlyByteBuf buf = ClientBufferHelper.create();

        // Send C2S packet
        ClientPacketHelper.send(ServerPackets.PACKET_C2S_CLOSE_CHAT, buf);
    }

    public static void setChatStatus(Entity entity, ChatDataManager.ChatStatus new_status) {
        FriendlyByteBuf buf = ClientBufferHelper.create();
        buf.writeUtf(entity.getStringUUID());
        buf.writeUtf(new_status.toString());

        // Send C2S packet
        ClientPacketHelper.send(ServerPackets.PACKET_C2S_SET_STATUS, buf);
    }

    public static void sendChat(Entity entity, String message) {
        // Get user language
        String userLanguageCode = Minecraft.getInstance().getLanguageManager().getSelected();
        String userLanguageName = Minecraft.getInstance().getLanguageManager().getLanguage(userLanguageCode).toComponent().getString();

        FriendlyByteBuf buf = ClientBufferHelper.create();
        buf.writeUtf(entity.getStringUUID());
        buf.writeUtf(message);
        buf.writeUtf(userLanguageName);

        // Send C2S packet
        ClientPacketHelper.send(ServerPackets.PACKET_C2S_SEND_CHAT, buf);
    }

    // Reading a Map<String, PlayerData> from the buffer
    public static Map<String, PlayerData> readPlayerDataMap(FriendlyByteBuf buffer) {
        int size = buffer.readInt(); // Read the size of the map
        Map<String, PlayerData> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buffer.readUtf(); // Read the key (playerName)
            PlayerData data = new PlayerData();
            data.friendship = buffer.readInt(); // Read PlayerData field(s)
            map.put(key, data); // Add to the map
        }
        return map;
    }

    public static void register() {
        // Client-side packet handler, message sync
        ClientPacketHelper.registerReceiver(ServerPackets.PACKET_S2C_ENTITY_MESSAGE, (client, handler, buffer, responseSender) -> {
            // Read the data from the server packet
            UUID entityId = UUID.fromString(buffer.readUtf());
            String message = buffer.readUtf(32767);
            int line = buffer.readInt();
            String status_name = buffer.readUtf(32767);
            ChatDataManager.ChatStatus status = ChatDataManager.ChatStatus.valueOf(status_name);
            String sender_name = buffer.readUtf(32767);
            ChatDataManager.ChatSender sender = ChatDataManager.ChatSender.valueOf(sender_name);
            Map<String, PlayerData> players = readPlayerDataMap(buffer);

            // Update the chat data manager on the client-side
            client.execute(() -> { // Make sure to run on the client thread
                // Ensure client.player is initialized
                if (client.player == null || client.level == null) {
                    LOGGER.warn("Client not fully initialized. Dropping message for entity '{}'.", entityId);
                    return;
                }

                // Get entity chat data for current entity & player
                ChatDataManager chatDataManager = ChatDataManager.getClientInstance();
                EntityChatData chatData = chatDataManager.getOrCreateChatData(entityId.toString());

                // Add entity message
                if (!message.isEmpty()) {
                    chatData.currentMessage = message;
                }
                chatData.currentLineNumber = line;
                chatData.status = status;
                chatData.sender = sender;
                chatData.players = players;

                // Play sound with volume based on distance (from player or entity)
                Mob entity = ClientEntityFinder.getEntityByUUID(client.level, entityId);
                if (entity != null) {
                    playNearbyUISound(client, entity, 0.2f);
                }
            });
        });

        // Client-side packet handler, message sync
        ClientPacketHelper.registerReceiver(ServerPackets.PACKET_S2C_PLAYER_MESSAGE, (client, handler, buffer, responseSender) -> {
            // Read the data from the server packet
            UUID senderPlayerId = UUID.fromString(buffer.readUtf());
            String senderPlayerName = buffer.readUtf(32767);
            String message = buffer.readUtf(32767);

            // Update the chat data manager on the client-side
            client.execute(() -> { // Make sure to run on the client thread
                // Ensure client.player is initialized
                if (client.player == null || client.level == null) {
                    LOGGER.warn("Client not fully initialized. Dropping message for sender '{}'.", senderPlayerId);
                    return;
                }

                // Add player message to queue for rendering
                PlayerMessageManager.addMessage(senderPlayerId, message, senderPlayerName, ChatDataManager.TICKS_TO_DISPLAY_USER_MESSAGE);
            });
        });

        // Client-side player login: get all chat data
        ClientPacketHelper.registerReceiver(ServerPackets.PACKET_S2C_LOGIN, (client, handler, buffer, responseSender) -> {
            int sequenceNumber = buffer.readInt(); // Sequence number of the current packet
            int totalPackets = buffer.readInt(); // Total number of packets for this data
            byte[] chunk = buffer.readByteArray(); // Read the byte array chunk from the current packet

            client.execute(() -> { // Make sure to run on the client thread
                // Store the received chunk
                receivedChunks.put(sequenceNumber, chunk);

                // Check if all chunks have been received
                if (receivedChunks.size() == totalPackets) {
                    LOGGER.info("Reassemble chunks on client and decompress lite JSON data string");

                    // Combine all byte array chunks
                    ByteArrayOutputStream combined = new ByteArrayOutputStream();
                    for (int i = 0; i < totalPackets; i++) {
                        combined.write(receivedChunks.get(i), 0, receivedChunks.get(i).length);
                    }

                    // Decompress the combined byte array to get the original JSON string
                    String chatDataJSON = Decompression.decompressString(combined.toByteArray());
                    if (chatDataJSON == null || chatDataJSON.isEmpty()) {
                        LOGGER.warn("Received invalid or empty chat data JSON. Skipping processing.");
                        return;
                    }

                    // Parse JSON and update client chat data
                    Gson GSON = new Gson();
                    Type type = new TypeToken<ConcurrentHashMap<String, EntityChatData>>(){}.getType();
                    ChatDataManager.getClientInstance().entityChatDataMap = GSON.fromJson(chatDataJSON, type);

                    // Clear receivedChunks for future use
                    receivedChunks.clear();
                }
            });
        });

        // Client-side packet handler, receive entire whitelist / blacklist, and update BubbleRenderer
        ClientPacketHelper.registerReceiver(ServerPackets.PACKET_S2C_WHITELIST, (client, handler, buffer, responseSender) -> {
            // Read the whitelist data from the buffer
            int whitelistSize = buffer.readInt();
            List<String> whitelist = new ArrayList<>(whitelistSize);
            for (int i = 0; i < whitelistSize; i++) {
                whitelist.add(buffer.readUtf(32767));
            }

            // Read the blacklist data from the buffer
            int blacklistSize = buffer.readInt();
            List<String> blacklist = new ArrayList<>(blacklistSize);
            for (int i = 0; i < blacklistSize; i++) {
                blacklist.add(buffer.readUtf(32767));
            }

            client.execute(() -> {
                BubbleRenderer.whitelist = whitelist;
                BubbleRenderer.blacklist = blacklist;
            });
        });

        // Client-side packet handler, player status sync
        ClientPacketHelper.registerReceiver(ServerPackets.PACKET_S2C_PLAYER_STATUS, (client, handler, buffer, responseSender) -> {
            // Read the data from the server packet
            UUID playerId = UUID.fromString(buffer.readUtf());
            boolean isChatOpen = buffer.readBoolean();

            // Get player instance
            Player player = ClientEntityFinder.getPlayerEntityFromUUID(playerId);

            // Update the player status data manager on the client-side
            client.execute(() -> {
                if (player == null) {
                    LOGGER.warn("Player entity is null. Skipping status update.");
                    return;
                }

                if (isChatOpen) {
                    PlayerMessageManager.openChatUI(playerId);
                    playNearbyUISound(client, player, 0.2f);
                } else {
                    PlayerMessageManager.closeChatUI(playerId);
                }
            });
        });
    }

    private static void playNearbyUISound(Minecraft client, Entity player, float maxVolume) {
        // Play sound with volume based on distance
        int distance_squared = 144;
        if (client.player != null) {
            double distance = client.player.distanceToSqr(player.getX(), player.getY(), player.getZ());
            if (distance <= distance_squared) {
                // Decrease volume based on distance
                float volume = maxVolume - (float)distance / distance_squared * maxVolume;
                client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), volume, 0.8F);
            }
        }
    }
}

