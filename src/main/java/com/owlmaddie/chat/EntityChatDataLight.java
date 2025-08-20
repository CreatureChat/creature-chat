// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.chat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The {@code EntityChatDataLight} class represents the current displayed message, and no
 * previous messages or player message history. This is primarily used to broadcast the
 * currently displayed messages to players as they connect to the server.
 */
public class EntityChatDataLight {
    public String entityId;
    public String currentMessage;
    public int currentLineNumber;
    public ChatDataManager.ChatStatus status;
    public ChatDataManager.ChatSender sender;
    public Map<String, PlayerData> players;
    public Long death;
    public String entity_type;

    // Constructor to initialize the light version from the full version
    public EntityChatDataLight(EntityChatData fullData, String playerName) {
        this.entityId = fullData.entityId;
        this.currentMessage = fullData.currentMessage;
        this.currentLineNumber = fullData.currentLineNumber;
        this.status = fullData.status;
        this.sender = fullData.sender;
        this.death = fullData.death;
        this.entity_type = fullData.entity_type;

        // Initialize the players map and add only the current player's data
        this.players = new HashMap<>();
        PlayerData playerData = fullData.getPlayerData(playerName);
        this.players.put(playerName, playerData);
    }
}