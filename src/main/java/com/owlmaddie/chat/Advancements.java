// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.chat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import com.owlmaddie.utils.AdvancementBackgroundHelper;

/**
 * Central registry for all CreatureChat advancements.
 * Provides a single source of truth for IDs, titles, descriptions and types
 * so version-specific code only needs to map these values to the respective
 * Minecraft APIs.
 */
public enum Advancements {
    ROOT("root", "CreatureChat", "Talk to mobs. Make friends. Start drama.", Type.TASK,
            Items.BOOK, null, 0, false,
            AdvancementBackgroundHelper.ui("text-top-friend")),

    ICE_BREAKER("ice_breaker", "Ice Breaker", "How's the weather...I guess?", Type.TASK,
            Items.SNOWBALL, ROOT, 0, false),

    FIRST_IMPRESSIONS("first_impressions", "First Impressions", "Make a friend.", Type.TASK,
            Items.POPPY, ICE_BREAKER, 0, false),

    NO_HARD_FEELINGS("no_hard_feelings", "No Hard Feelings", "Regain a friend after exchanging blows.", Type.TASK,
            Items.HONEY_BOTTLE, ICE_BREAKER, 0, false),

    OPEN_SESAME("open_sesame", "Open Sesame", "Check out a friends inventory.", Type.TASK,
            Items.TRIPWIRE_HOOK, FIRST_IMPRESSIONS, 0, false),

    TAG_ALONG("tag_along", "Tag Along", "Follow me bro.", Type.TASK,
            Items.LEAD, FIRST_IMPRESSIONS, 0, false),

    LEAD_THE_WAY("lead_the_way", "Lead The Way", "Where are we going anyway?", Type.TASK,
            Items.COMPASS, TAG_ALONG, 0, false),

    CALM_THE_STORM("calm_the_storm", "Calm The Storm", "Stop attacking me bro.", Type.GOAL,
            Items.WHITE_BANNER, FIRST_IMPRESSIONS, 0, false),

    STAND_YOUR_GROUND("stand_your_ground", "Stand Your Ground", "Stop. It's okay.", Type.GOAL,
            Items.CARROT_ON_A_STICK, FIRST_IMPRESSIONS, 0, false),

    SWORN_OATH("sworn_oath", "Sworn Oath", "I've got backup.", Type.GOAL,
            Items.SHIELD, FIRST_IMPRESSIONS, 0, false),

    WORDSMITH("wordsmith", "Wordsmith", "From rocky start to best friends.", Type.CHALLENGE,
            Items.WRITABLE_BOOK, NO_HARD_FEELINGS, 350, false),

    TRUE_COMPANION("true_companion", "True Companion", "Best friends forever.", Type.GOAL,
            Items.NAME_TAG, FIRST_IMPRESSIONS, 0, false),

    SLEIGHT_OF_HAND("sleight_of_hand", "Sleight of Hand", "Try my sword.", Type.GOAL,
            Items.TOTEM_OF_UNDYING, TRUE_COMPANION, 0, false),

    SHARED_STASH("shared_stash", "Shared Stash", "Share the loot around.", Type.GOAL,
            Items.CHEST, OPEN_SESAME, 0, false),

    SOCIAL_BUTTERFLY("social_butterfly", "Social Butterfly", "A little trust in many places.", Type.GOAL,
            Items.HONEYCOMB, FIRST_IMPRESSIONS, 0, false),

    INNER_CIRCLE("inner_circle", "Inner Circle", "A cozy crowd of close friends.", Type.GOAL,
            Items.GOAT_HORN, TRUE_COMPANION, 0, false),

    POPULAR_OPINION("popular_opinion", "Popular Opinion", "The whole room leans your way.", Type.CHALLENGE,
            Items.BELL, INNER_CIRCLE, 300, false),

    DRAMA_LLAMA("drama_llama", "Drama Llama", "Best friends for never.", Type.GOAL,
            Items.LIGHTNING_ROD, FIRST_IMPRESSIONS, 0, false),

    LOVE_HATE_RELATIONSHIP("love_hate_relationship", "Love Hate Relationship", "Flip the vibe. Again and again.", Type.CHALLENGE,
            Items.COMPARATOR, DRAMA_LLAMA, 350, false),

    ARCH_NEMESIS("arch_nemesis", "Arch Nemesis", "Meet your worst enemy.", Type.CHALLENGE,
            Items.CROSSBOW, NO_HARD_FEELINGS, 150, false),

    FRIEND_OR_FOE("friend_or_foe", "Friend Or Foe", "Remember the good times.", Type.CHALLENGE,
            Items.TARGET, ARCH_NEMESIS, 400, false),

    FINDERS_KEEPERS("finders_keepers", "Finder’s Keepers", "\"Borrow\" an item from your best friend.", Type.CHALLENGE,
            Items.ITEM_FRAME, TRUE_COMPANION, 250, false),

    GUIDED_TOUR("guided_tour", "Guided Tour", "A long walk.", Type.GOAL,
            Items.MAP, LEAD_THE_WAY, 0, false),

    THE_NEVERENDING_STORY("the_neverending_story", "The Neverending Story", "50 player messages. Still talking.", Type.GOAL,
            Items.CAMPFIRE, ICE_BREAKER, 0, false),

    GRAND_GESTURE("grand_gesture", "Grand Gesture", "One move. Big friendship.", Type.GOAL,
            Items.DIAMOND, FIRST_IMPRESSIONS, 0, false),

    A_LEGEND("a_legend", "A Legend", "Crown a loyal hog.", Type.CHALLENGE,
            Items.GOLDEN_HELMET, TRUE_COMPANION, 500, true),

    POTATO_WAR("potato_war", "Potato War", "Every slot filled with potatoes.", Type.CHALLENGE,
            Items.POTATO, TRUE_COMPANION, 400, true),

    TRUE_PACIFIST("true_pacifist", "True Pacifist", "Love conquers all.", Type.CHALLENGE,
            Items.DRAGON_EGG, TRUE_COMPANION, 1000, false),

    THE_HEIST("the_heist", "The Heist", "Steal a diamond from a mob.", Type.CHALLENGE,
            Items.DIAMOND, FINDERS_KEEPERS, 500, true),

    ENDER_ESCORT("ender_escort", "Ender Escort", "Take a friend to the End.", Type.CHALLENGE,
            Items.ENDER_PEARL, TAG_ALONG, 400, true);

    public final ResourceLocation id;
    public final String title;
    public final String description;
    public final Type type;
    public final Item icon;
    public final Advancements parent;
    public final int rewardXp;
    public final boolean hidden;
    public final ResourceLocation background;

    Advancements(String path, String title, String description, Type type, Item icon, Advancements parent, int rewardXp, boolean hidden) {
        this(path, title, description, type, icon, parent, rewardXp, hidden, null);
    }

    Advancements(String path, String title, String description, Type type, Item icon, Advancements parent, int rewardXp, boolean hidden, ResourceLocation background) {
        this.id = new ResourceLocation("creaturechat", path);
        this.title = title;
        this.description = description;
        this.type = type;
        this.icon = icon;
        this.parent = parent;
        this.rewardXp = rewardXp;
        this.hidden = hidden;
        this.background = background;
    }

    public enum Type {
        TASK,
        GOAL,
        CHALLENGE
    }
}

