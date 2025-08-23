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
    public int lastDamageFriendship;
    public int signFlipCount;
    public int lastSign;
    public boolean seenHigh;
    public boolean seenLow;
    public boolean reachedPos3;
    public boolean reachedNeg3;
    public boolean attacking;
    public boolean fleeing;
    public boolean gaveItem;
    public boolean pigProtect;
    public int conversationCount;
    public boolean droppedBelowZero;
    public boolean openedInventory;
    public boolean wordsmithActive;
    public boolean wordsmithOpenedInventory;
    public boolean wordsmithGaveItem;
    public boolean wordsmithDamaged;
    public int messageCount;
    public boolean wasInOverworld;

    public PlayerData() {
        this.friendship = 0;
        this.lastDamageFriendship = Integer.MIN_VALUE;
        this.signFlipCount = 0;
        this.lastSign = 0;
        this.seenHigh = false;
        this.seenLow = false;
        this.reachedPos3 = false;
        this.reachedNeg3 = false;
        this.attacking = false;
        this.fleeing = false;
        this.gaveItem = false;
        this.pigProtect = false;
        this.conversationCount = 0;
        this.droppedBelowZero = false;
        this.openedInventory = false;
        this.wordsmithActive = false;
        this.wordsmithOpenedInventory = false;
        this.wordsmithGaveItem = false;
        this.wordsmithDamaged = false;
        this.messageCount = 0;
        this.wasInOverworld = false;
    }
}