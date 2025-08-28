// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.chat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import com.owlmaddie.utils.AdvancementBackgroundHelper;
import com.owlmaddie.i18n.TR;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Central registry for all CreatureChat advancements.
 * Provides a single source of truth for IDs, titles, descriptions and types
 * so version-specific code only needs to map these values to the respective
 * Minecraft APIs.
 */
public enum Advancements {
    ROOT("root", "CreatureChat", "Your world just got way more alive.", Type.TASK,
            Items.BOOK, null, 0, false,
            AdvancementBackgroundHelper.ui("advancements-background")),

    ICE_BREAKER("ice_breaker", "Ice Breaker", "Cold open.", Type.TASK,
            Items.SNOWBALL, ROOT, 0, false),

    FIRST_IMPRESSIONS("first_impressions", "First Impressions", "Make a friend.", Type.TASK,
            Items.APPLE, ICE_BREAKER, 0, false),

    TRUE_COMPANION("true_companion", "True Companion", "Did we just become best friends?", Type.TASK,
            Items.NAME_TAG, FIRST_IMPRESSIONS, 0, false),

    THE_NEVERENDING_STORY("the_neverending_story", "The NeverEnding Story", "Every real story is a never ending story.", Type.TASK,
            Items.WRITABLE_BOOK, TRUE_COMPANION, 0, false),

    TRUE_PACIFIST("true_pacifist", "Pacifist Route", "The best ending.", Type.GOAL,
            Items.DRAGON_EGG, THE_NEVERENDING_STORY, 0, false),

    SOCIAL_BUTTERFLY("social_butterfly", "Social Butterfly", "Love conquers all.", Type.TASK,
            Items.OXEYE_DAISY, TRUE_COMPANION, 0, false),

    INNER_CIRCLE("inner_circle", "Inner Circle", "Gathered round the fire.", Type.TASK,
            Items.CAMPFIRE, SOCIAL_BUTTERFLY, 0, false),

    POPULAR_OPINION("popular_opinion", "Popular Opinion", "Sway the Crowd.", Type.GOAL,
            Items.BELL, INNER_CIRCLE, 0, false),

    OPEN_SESAME("open_sesame", "Open Sesame", "Your stuff is my stuff.", Type.TASK,
            Items.CHEST, FIRST_IMPRESSIONS, 0, false),

    SHARED_STASH("shared_stash", "Shared Stash", "Share the loot.", Type.GOAL,
            Items.BUNDLE, OPEN_SESAME, 0, false),

    FINDERS_KEEPERS("finders_keepers", "Finder’s Keepers", "Borrowed forever.", Type.TASK,
            Items.GOLD_INGOT, OPEN_SESAME, 0, false),

    SLEIGHT_OF_HAND("sleight_of_hand", "Sleight of Hand", "Try my sword.", Type.GOAL,
            Items.STICK, FINDERS_KEEPERS, 0, false),

    THE_HEIST("the_heist", "The Heist", "The perfect crime.", Type.GOAL,
            Items.DIAMOND, FINDERS_KEEPERS, 0, true),

    NO_HARD_FEELINGS("no_hard_feelings", "No Hard Feelings", "Regain a friend.", Type.TASK,
            Items.CAKE, FIRST_IMPRESSIONS, 0, false),

    WORDSMITH("wordsmith", "Wordsmith", "From rocky start to best friends.", Type.TASK,
            Items.FEATHER, NO_HARD_FEELINGS, 0, false),

    CALM_THE_STORM("calm_the_storm", "Calm The Storm", "Chill out, man.", Type.TASK,
            Items.POWDER_SNOW_BUCKET, WORDSMITH, 0, false),

    GRAND_GESTURE("grand_gesture", "Grand Gesture", "You got rizz.", Type.TASK,
            Items.EXPERIENCE_BOTTLE, CALM_THE_STORM, 0, false),

    DRAMA_LLAMA("drama_llama", "Drama Llama", "Best friends for never.", Type.GOAL,
            Items.LLAMA_SPAWN_EGG, GRAND_GESTURE, 0, false),

    TAG_ALONG("tag_along", "Tag Along", "Follow me, bro.", Type.TASK,
            Items.LEAD, ICE_BREAKER, 0, false),

    LEAD_THE_WAY("lead_the_way", "Lead The Way", "Where are we going?", Type.TASK,
            Items.COMPASS, TAG_ALONG, 0, false),

    GUIDED_TOUR("guided_tour", "Guided Tour", "Lost, together.", Type.TASK,
            Items.SPYGLASS, LEAD_THE_WAY, 0, false),

    ENDER_ESCORT("ender_escort", "Ender Escort", "Together till the End.", Type.CHALLENGE,
            Items.ENDER_EYE, GUIDED_TOUR, 50, true),

    STAND_YOUR_GROUND("stand_your_ground", "Stand Your Ground", "Why are you running?", Type.GOAL,
            Items.LEATHER_BOOTS, LEAD_THE_WAY, 0, false),

    SWORN_OATH("sworn_oath", "Sworn Oath", "I will protect you.", Type.TASK,
            Items.SHIELD, TAG_ALONG, 0, false),

    A_LEGEND("a_legend", "A Legend", "Legends never die.", Type.CHALLENGE,
            Items.DIAMOND_SWORD, SWORN_OATH, 100, true),

    POTATO_WAR("potato_war", "Potato War", "All war is deception.", Type.CHALLENGE,
            Items.POTATO, SWORN_OATH, 100, true),

    ARCH_NEMESIS("arch_nemesis", "Arch Nemesis", "Keep your enemies closer.", Type.TASK,
            Items.CROSSBOW, ICE_BREAKER, 0, false),

    FRIEND_OR_FOE("friend_or_foe", "Friend Or Foe", "Remember the good times?", Type.TASK,
            Items.TNT, ARCH_NEMESIS, 0, false),

    LOVE_HATE_RELATIONSHIP("love_hate_relationship", "Love Hate Relationship", "It’s complicated.", Type.CHALLENGE,
            Items.WITHER_ROSE, FRIEND_OR_FOE, 0, false);

    public final ResourceLocation id;
    public final TR title;
    public final TR description;
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
        this.title = new TR("advancement." + path + ".title", title);
        this.description = new TR("advancement." + path + ".desc", description);
        this.type = type;
        this.icon = icon;
        this.parent = parent;
        this.rewardXp = rewardXp;
        this.hidden = hidden;
        this.background = background;
    }

    public static Stream<TR> allText() {
        return Arrays.stream(values()).flatMap(a -> Stream.of(a.title, a.description));
    }

    public enum Type {
        TASK,
        GOAL,
        CHALLENGE
    }
}
