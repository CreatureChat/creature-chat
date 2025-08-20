// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.chat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Central registry for all CreatureChat advancements.
 * Provides a single source of truth for IDs, titles, descriptions and types
 * so version-specific code only needs to map these values to the respective
 * Minecraft APIs.
 */
public enum Advancements {
    ROOT("root", "CreatureChat", "Talk to mobs. Make friends. Start drama.", Type.TASK,
            Items.BOOK, null, 0, false, new ResourceLocation("minecraft", "block/diamond_block")),

    ICE_BREAKER("ice_breaker", "Ice Breaker", "Send at least 2 player messages to the same mob.", Type.TASK,
            Items.WRITABLE_BOOK, ROOT, 0, false),

    FIRST_IMPRESSIONS("first_impressions", "First Impressions", "After Ice Breaker, friendship with that mob first rises from 0 to ≥1.", Type.TASK,
            Items.EMERALD, ICE_BREAKER, 0, false),

    NO_HARD_FEELINGS("no_hard_feelings", "No Hard Feelings", "After you damage a mob, raise its friendship back to the exact pre-hit value without further damage.", Type.TASK,
            Items.HONEY_BOTTLE, ICE_BREAKER, 0, false),

    OPEN_SESAME("open_sesame", "Open Sesame", "First time you open a mob’s inventory by reaching friendship > 0.", Type.TASK,
            Items.TRIPWIRE_HOOK, FIRST_IMPRESSIONS, 0, false),

    TAG_ALONG("tag_along", "Tag Along", "Cause a mob to enter FOLLOW using chat.", Type.TASK,
            Items.LEAD, FIRST_IMPRESSIONS, 0, false),

    LEAD_THE_WAY("lead_the_way", "Lead The Way", "Cause a mob to enter LEAD using chat.", Type.TASK,
            Items.COMPASS, TAG_ALONG, 0, false),

    CALM_THE_STORM("calm_the_storm", "Calm The Storm", "While a mob is ATTACKing, use chat to clear ATTACK behavior.", Type.GOAL,
            Items.WHITE_BANNER, FIRST_IMPRESSIONS, 0, false),

    DO_NOT_RUN("do_not_run", "Do Not Run", "While a mob is FLEEing, use chat to stop FLEE.", Type.GOAL,
            Items.CARROT_ON_A_STICK, FIRST_IMPRESSIONS, 0, false),

    BODYGUARD_DETAIL("bodyguard_detail", "Bodyguard Detail", "Cause a mob to enter PROTECT using chat.", Type.GOAL,
            Items.SHIELD, FIRST_IMPRESSIONS, 0, false),

    WORDSMITH("wordsmith", "Wordsmith", "With one mob, go from <0 to 3 using chat only (no items, no inventory, no damage).", Type.CHALLENGE,
            Items.AMETHYST_SHARD, NO_HARD_FEELINGS, 350, false),

    TRUE_COMPANION("true_companion", "True Companion", "Friendship with a single mob reaches 3.", Type.GOAL,
            Items.NAME_TAG, FIRST_IMPRESSIONS, 0, false),

    DRESSED_TO_KILL("dressed_to_kill", "Dressed to Kill", "At friendship 3, change that mob’s mainhand or offhand item.", Type.GOAL,
            Items.ARMOR_STAND, TRUE_COMPANION, 0, false),

    SHARED_STASH("shared_stash", "Shared Stash", "Place at least one item into the inventories of 5 different friendly mobs.", Type.GOAL,
            Items.CHEST, OPEN_SESAME, 0, false),

    SOCIAL_BUTTERFLY("social_butterfly", "Social Butterfly", "Reach >0 friendship with 10 different mob types (≥2 player messages each).", Type.GOAL,
            Items.BOOK, FIRST_IMPRESSIONS, 0, false),

    INNER_CIRCLE("inner_circle", "Inner Circle", "While engaged with a mob at ≥2, have 5 other mobs within 8 blocks also at ≥2.", Type.GOAL,
            Items.GOAT_HORN, TRUE_COMPANION, 0, false),

    POPULAR_OPINION("popular_opinion", "Popular Opinion", "Have 10 mobs within 12 blocks each at ≥2 friendship at once.", Type.CHALLENGE,
            Items.BELL, INNER_CIRCLE, 300, false),

    DRAMA_LLAMA("drama_llama", "Drama Llama", "With the same mob, have friendship move from ≥2 to ≤ -2.", Type.GOAL,
            Items.LIGHTNING_ROD, FIRST_IMPRESSIONS, 0, false),

    LOVE_HATE_RELATIONSHIP("love_hate_relationship", "Love Hate Relationship", "With the same mob, change sign 4 times total (positive ↔ negative).", Type.CHALLENGE,
            Items.COMPARATOR, DRAMA_LLAMA, 350, false),

    ARCH_NEMESIS("arch_nemesis", "Arch Nemesis", "Friendship with a single mob reaches -3.", Type.CHALLENGE,
            Items.CROSSBOW, NO_HARD_FEELINGS, 150, false),

    FRIEND_OR_FOE("friend_or_foe", "Friend Or Foe", "With the same mob, achieve both extremes: +3 and -3.", Type.CHALLENGE,
            Items.TARGET, ARCH_NEMESIS, 400, false),

    FINDERS_KEEPERS("finders_keepers", "Finder’s Keepers", "Take an item from a mob when friendship with that mob is exactly 3.", Type.CHALLENGE,
            Items.ITEM_FRAME, TRUE_COMPANION, 250, false),

    GUIDED_TOUR("guided_tour", "Guided Tour", "Complete a LEAD journey that ends ≥64 blocks from where it began.", Type.GOAL,
            Items.COMPASS, LEAD_THE_WAY, 0, false),

    LONG_CONVERSATION("long_conversation", "Long Conversation", "Exchange 50 total messages with the same mob without friendship <0.", Type.GOAL,
            Items.CAMPFIRE, ICE_BREAKER, 0, false),

    GRAND_GESTURE("grand_gesture", "Grand Gesture", "Perform a single action that increases a mob’s friendship by ≥ +2 at once.", Type.GOAL,
            Items.DIAMOND, FIRST_IMPRESSIONS, 0, false),

    BACKSEAT_DRIVER("backseat_driver", "Backseat Driver", "With the same mob, make it FOLLOW you once and LEAD you another time.", Type.GOAL,
            Items.OAK_SIGN, LEAD_THE_WAY, 0, false),

    HAIL_TO_THE_KING("hail_to_the_king", "Hail to the King", "On a pig at friendship 3, set PROTECT via chat, then place a golden helmet in its offhand.", Type.CHALLENGE,
            Items.GOLDEN_HELMET, TRUE_COMPANION, 500, true),

    POTATO_PACT("potato_pact", "Potato Pact", "On a pig at friendship 3, put potato, baked potato, and poisonous potato in its inventory, then set FOLLOW.", Type.CHALLENGE,
            Items.POTATO, TRUE_COMPANION, 400, true),

    LOVE_CONQUERS_ALL("love_conquers_all", "Love Conquers All", "With the Ender Dragon, reach friendship 3 (CreatureChat win condition).", Type.CHALLENGE,
            Items.DRAGON_EGG, TRUE_COMPANION, 1000, false);

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

