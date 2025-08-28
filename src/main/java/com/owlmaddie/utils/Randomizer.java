// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import com.owlmaddie.i18n.TR;

/**
 * The {@code Randomizer} class provides easy functions for generating a variety of different random numbers
 * and phrases used by this mod.
 */
public class Randomizer {
    public enum RandomType { ADJECTIVE, SPEAKING_STYLE, CLASS, ALIGNMENT }
    public enum ErrorType { GENERAL, CONNECTION, CODE401, CODE403, CODE429, CODE500, CODE503 }

    public static final String DISCORD_LINK = "discord.creaturechat.com";

    private static final List<TR> NO_RESPONSE = List.of(
            new TR("no_response.0", "<no response>"),
            new TR("no_response.1", "<silence>"),
            new TR("no_response.2", "<stares>"),
            new TR("no_response.3", "<blinks>"),
            new TR("no_response.4", "<looks away>"),
            new TR("no_response.5", "<sighs>"),
            new TR("no_response.6", "<shrugs>"),
            new TR("no_response.7", "<taps foot>"),
            new TR("no_response.8", "<yawns>"),
            new TR("no_response.9", "<glances around>"),
            new TR("no_response.10", "<hums quietly>"),
            new TR("no_response.11", "<shakes head>"),
            new TR("no_response.12", "<squints>"),
            new TR("no_response.13", "<tilts head>"),
            new TR("no_response.14", "<checks compass>"),
            new TR("no_response.15", "<studies map>"),
            new TR("no_response.16", "<rolls eyes>")
    );

    public static final TR ERROR_GENERAL    = new TR("error.general",    "Something unexpected has gone wrong. Find help at %s.");
    public static final TR ERROR_CONNECTION = new TR("error.connection", "Can't reach the server. Are you offline? Find help at %s.");
    public static final TR ERROR_401        = new TR("error.401",        "To get started, you need an AI key or a local LLM. Find help at %s.");
    public static final TR ERROR_403        = new TR("error.403",        "Country or region unavailable. Find help at %s.");
    public static final TR ERROR_429        = new TR("error.429",        "Your AI key is out of tokens and needs funds, or you are sending too many messages. Find help at %s.");
    public static final TR ERROR_500        = new TR("error.500",        "There was an error processing your message request. Please try again or find help at %s.");
    public static final TR ERROR_503        = new TR("error.503",        "Service is overloaded or temporarily down. Please try again later. Find help at %s.");

    public static Stream<TR> allErrorText() {
        return Stream.of(
                ERROR_GENERAL,
                ERROR_CONNECTION,
                ERROR_401,
                ERROR_403,
                ERROR_429,
                ERROR_500,
                ERROR_503
        );
    }

    public static Stream<TR> allNoResponseText() {
        return NO_RESPONSE.stream();
    }

    public static TR getRandomNoResponse() {
        Random random = new Random();
        return NO_RESPONSE.get(random.nextInt(NO_RESPONSE.size()));
    }
    private static List<String> characterAdjectives = Arrays.asList(
            "mystical", "fiery", "ancient", "cursed", "ethereal", "clumsy", "stealthy",
            "legendary", "toxic", "enigmatic", "celestial", "rambunctious", "shadowy",
            "brave", "screaming", "radiant", "savage", "whimsical", "positive", "turbulent",
            "ominous", "jubilant", "arcane", "hopeful", "rugged", "venomous", "timeworn",
            "heinous", "friendly", "humorous", "silly", "goofy", "irate", "furious",
            "wrathful", "nefarious", "sinister", "malevolent", "sly", "roguish", "deceitful",
            "untruthful", "loving", "noble", "dignified", "righteous", "defensive",
            "protective", "heroic", "amiable", "congenial", "happy", "sarcastic", "funny",
            "short", "zany", "cooky", "wild", "fearless insane", "cool", "chill",
            "cozy", "comforting", "stern", "stubborn", "scatterbrain", "scaredy", "aloof",
            "gullible", "mischievous", "prankster", "trolling", "clingy", " manipulative",
            "weird", "famous", "persuasive", "sweet", "wholesome", "innocent", "annoying",
            "trusting", "hyper", "egotistical", "slow", "obsessive", "compulsive", "impulsive",
            "unpredictable", "wildcard", "stuttering", "hypochondriac", "hypocritical",
            "optimistic", "overconfident", "jumpy", "brief", "flighty", "visionary", "adorable",
            "sparkly", "bubbly", "unstable", "sad", "angry", "bossy", "altruistic", "quirky",
            "nostalgic", "emotional", "enthusiastic", "unusual", "conspirator", "traitorous"
    );
    private static List<String> speakingStyles = Arrays.asList(
            "formal", "casual", "eloquent", "blunt", "humorous", "sarcastic", "mysterious",
            "cheerful", "melancholic", "authoritative", "nervous", "whimsical", "grumpy",
            "wise", "aggressive", "soft-spoken", "patriotic", "romantic", "pedantic", "dramatic",
            "inquisitive", "cynical", "empathetic", "boisterous", "monotone", "laconic", "poetic",
            "archaic", "childlike", "erudite", "streetwise", "flirtatious", "stoic", "rhetorical",
            "inspirational", "goofy", "overly dramatic", "deadpan", "sing-song", "pompous",
            "hyperactive", "valley girl", "robot", "baby talk", "lolcat",
            "gen-z", "gamer", "nerdy", "shakespearean", "old-timer", "dramatic anime",
            "hipster", "mobster", "angry", "heroic", "disagreeable", "minimalist",
            "scientific", "bureaucratic", "DJ", "military", "shy", "tsundere", "theater kid",
            "boomer", "goth", "surfer", "detective noir", "stupid", "auctioneer", "exaggerated British",
            "corporate jargon", "motivational speaker", "fast-talking salesperson", "slimy"
    );
    private static List<String> classes = Arrays.asList(
            "warrior", "mage", "archer", "rogue", "paladin", "necromancer", "bard", "lorekeeper",
            "sorcerer", "ranger", "cleric", "berserker", "alchemist", "summoner", "shaman",
            "illusionist", "assassin", "knight", "valkyrie", "hoarder", "organizer", "lurker",
            "elementalist", "gladiator", "templar", "reaver", "spellblade", "enchanter", "samurai",
            "runemaster", "witch", "miner", "redstone engineer", "ender knight", "decorator",
            "wither hunter", "nethermancer", "slime alchemist", "trader", "traitor", "noob", "griefer",
            "potion master", "builder", "explorer", "herbalist", "fletcher", "enchantress",
            "smith", "geomancer", "hunter", "lumberjack", "farmer", "fisherman", "cartographer",
            "librarian", "blacksmith", "architect", "trapper", "baker", "mineralogist",
            "beekeeper", "hermit", "farlander", "void searcher", "end explorer", "archeologist",
            "hero", "villain", "mercenary", "guardian", "rebel", "paragon",
            "antagonist", "avenger", "seeker", "mystic", "outlaw"
    );
    private static List<String> alignments = Arrays.asList(
            "lawful good", "neutral good", "chaotic good",
            "lawful neutral", "true neutral", "chaotic neutral",
            "lawful evil", "neutral evil", "chaotic evil"
    );

    // Get random message by type
    public static String getRandomMessage(RandomType messageType) {
        Random random = new Random();
        List<String> messages = null;
        if (messageType.equals(RandomType.ADJECTIVE)) {
            messages = characterAdjectives;
        } else if (messageType.equals(RandomType.CLASS)) {
            messages = classes;
        } else if (messageType.equals(RandomType.ALIGNMENT)) {
            messages = alignments;
        } else if (messageType.equals(RandomType.SPEAKING_STYLE)) {
            messages = speakingStyles;
        }

        int index = random.nextInt(messages.size());
        return messages.get(index).trim();
    }

    // Get error text by type
    public static TR getRandomError(ErrorType errorType) {
        return switch (errorType) {
            case CONNECTION -> ERROR_CONNECTION;
            case CODE401 -> ERROR_401;
            case CODE403 -> ERROR_403;
            case CODE429 -> ERROR_429;
            case CODE500 -> ERROR_500;
            case CODE503 -> ERROR_503;
            default -> ERROR_GENERAL;
        };
    }

    public static String RandomLetter() {
        // Return random letter between 'A' and 'Z'
        int randomNumber = RandomNumber(26);
        return String.valueOf((char) ('A' + randomNumber));
    }

    public static int RandomNumber(int max) {
        // Generate a random integer between 0 and max (inclusive)
        Random random = new Random();
        return random.nextInt(max);
    }
}
