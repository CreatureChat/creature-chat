// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.chat;

import net.minecraft.resources.ResourceLocation;

/**
 * Central registry for all CreatureChat advancements.
 * Provides a single source of truth for IDs, titles, descriptions and types
 * so version-specific code only needs to map these values to the respective
 * Minecraft APIs.
 */
public enum Advancements {
    ROOT("root", "CreatureChat", "Talk to mobs. Make friends. Start drama.", Type.TASK, new ResourceLocation("minecraft", "icon.png")),
    ICE_BREAKER("ice_breaker", "Ice Breaker", "So... weather, huh?", Type.TASK),
    FRIENDLY_CREATURE("friendly_creature", "Friendly Creature", "Made a new pal!", Type.TASK),
    BEASTIE_BESTIE("beastie_bestie", "Beastie Bestie", "They would share their last potato with you.", Type.GOAL),
    ARCH_NEMESIS("arch_nemesis", "Arch Nemesis", "They hiss in your general direction.", Type.CHALLENGE),
    LOVE_HATE("love_hate", "Love Hate Relationship", "Best friend, worst enemy... same creature.", Type.CHALLENGE),
    DRAMA_LLAMA("drama_llama", "Drama Llama", "It’s not complicated, it’s chaotic.", Type.GOAL),
    SMOOTH_TALKER("smooth_talker", "Smooth Talker", "All persuasion, zero inventory.", Type.GOAL),
    NO_HARD_FEELINGS("no_hard_feelings", "No Hard Feelings", "Ouch. Sorry. Friends?", Type.TASK),
    SQUAD_GOALS("squad_goals", "Squad Goals", "Group selfie energy.", Type.GOAL),
    BORROWED_FOREVER("borrowed_forever", "Borrowed Forever", "Technically not stealing.", Type.CHALLENGE);

    public final ResourceLocation id;
    public final String title;
    public final String description;
    public final Type type;
    public final ResourceLocation background;

    Advancements(String path, String title, String description, Type type) {
        this(path, title, description, type, null);
    }

    Advancements(String path, String title, String description, Type type, ResourceLocation background) {
        this.id = new ResourceLocation("creaturechat", path);
        this.title = title;
        this.description = description;
        this.type = type;
        this.background = background;
    }

    public enum Type {
        TASK,
        GOAL,
        CHALLENGE
    }
}
