// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.chat;

/**
 * The {@code PlayerData} class represents data associated with a player,
 * specifically tracking their friendship level.
 */
public class PlayerData {
    public int friendship;
    public long lastDamageTime;
    public long swingStartTime;

    public PlayerData() {
        this.friendship = 0;
        this.lastDamageTime = 0;
        this.swingStartTime = 0;
    }
}