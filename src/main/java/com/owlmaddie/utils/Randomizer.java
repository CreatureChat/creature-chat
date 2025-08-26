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
    public enum RandomType { NO_RESPONSE, ADJECTIVE, SPEAKING_STYLE, CLASS, ALIGNMENT }
    public enum ErrorType { GENERAL, CONNECTION, CODE401, CODE403, CODE429, CODE500, CODE503 }

    public static final String DISCORD_LINK = "discord.creaturechat.com";
    private static List<String> noResponseMessages = Arrays.asList(
            "<no response>",
            "<silence>",
            "<stares>",
            "<blinks>",
            "<looks away>",
            "<sighs>",
            "<shrugs>",
            "<taps foot>",
            "<yawns>",
            "<examines nails>",
            "<whistles softly>",
            "<shifts uncomfortably>",
            "<glances around>",
            "<pretends not to hear>",
            "<hums quietly>",
            "<fiddles with something>",
            "<gazes into the distance>",
            "<smirks>",
            "<raises an eyebrow>",
            "<clears throat>",
            "<peers over your shoulder>",
            "<fakes a smile>",
            "<mutters under breath>",
            "<counts imaginary stars>",
            "<shakes head>",
            "<squints>",
            "<tilts head>",
            "<checks compass>",
            "<studies map>",
            "<kicks dirt>",
            "<rolls eyes>",
            "<waves awkwardly>",
            "<looks at the sky>",
            "<counts to three>",
            "<checks inventory>",
            "<sips water>",
            "<stares at the ground>"
    );
    public static final List<TR> ERROR_GENERAL = List.of(
            new TR("error.general.0", "Seems like my words got lost in the End. Check out %s for clues!"),
            new TR("error.general.1", "Oops! My speech bubble popped. Need help? Visit %s"),
            new TR("error.general.2", "I might've eaten a bad Command Block. Help me out at %s!"),
            new TR("error.general.3", "I think a Creeper blew up my script. Instructions? %s"),
            new TR("error.general.4", "BRB, asking a villager for directions to %s"),
            new TR("error.general.5", "I tried to speak, but it was a critical miss. Help at %s"),
            new TR("error.general.6", "Words are hard. Come chat at %s"),
            new TR("error.general.7", "I must've left my responses at my other base. See %s"),
            new TR("error.general.8", "I’d tell you, but then I’d have to respawn. Meet me at %s"),
            new TR("error.general.9", "Response not found. Maybe it’s hiding at %s?"),
            new TR("error.general.10", "I'm speechless, literally. Let's troubleshoot at %s"),
            new TR("error.general.11", "Looks like my connection got lost in the Nether. Can you help? %s"),
            new TR("error.general.12", "I forgot what I was saying, but %s remembers."),
            new TR("error.general.13", "My message is unfortunately out of order. Find help at %s")
    );
    public static final List<TR> ERROR_CONNECTION = List.of(
            new TR("error.connection.0", "No signal! Maybe connect to the internet? %s"),
            new TR("error.connection.1", "Looks like your request was blocked by a firewall. Info at %s"),
            new TR("error.connection.2", "Can't reach the server. Are you offline? %s"),
            new TR("error.connection.3", "Connection errors everywhere! My bad. Ask %s for help"),
            new TR("error.connection.4", "This network portal is closed. Check your connection. %s"),
            new TR("error.connection.5", "Are we underground? I can't find the web. Connection not found. %s"),
            new TR("error.connection.6", "Your network doesn't like me. More help at %s"),
            new TR("error.connection.7", "I bumped into a blocked port. Connection error, find help at %s"),
            new TR("error.connection.8", "Maybe your wifi is grumpy. Try fixing it. %s"),
            new TR("error.connection.9", "Your wifi might have vanished. %s"),
            new TR("error.connection.10", "Firewall says 'nope'. %s"),
            new TR("error.connection.11", "We might have no internet at all. Whoops. %s"),
            new TR("error.connection.12", "Connection meltdown! %s")
    );
    public static final List<TR> ERROR_401 = List.of(
            new TR("error.401.0", "I lost my keys! Maybe update the API key? %s"),
            new TR("error.401.1", "No entry without a key! Visit %s to learn more."),
            new TR("error.401.2", "API door slammed shut. Did you set a valid key? %s"),
            new TR("error.401.3", "I'm keyless and confused. Learn more at %s"),
            new TR("error.401.4", "Access denied! That's usually an invalid API key. See %s"),
            new TR("error.401.5", "Your key is invalid or confused. Get some help maybe, at %s"),
            new TR("error.401.6", "I forgot the secret handshake. Check your API key or go to %s"),
            new TR("error.401.7", "Bad token, bad! Fix your API key, support at %s"),
            new TR("error.401.8", "This door won't let me in. Did you misplace the API key? %s"),
            new TR("error.401.9", "Invalid key. Get help or something at %s"),
            new TR("error.401.10", "No key, no chat! Configure it via %s"),
            new TR("error.401.11", "No ominous trial keys allowed, API keys only. See %s for help"),
            new TR("error.401.12", "Locked out! Did you use a trial key instead of an API key? Get help at %s"),
            new TR("error.401.13", "Help! Double-check your API key, or see %s"),
            new TR("error.401.14", "You need the golden API key to open this door. Invalid API key, see %s"),
            new TR("error.401.15", "Please insert a valid API key, it no work. %s")
    );
    public static final List<TR> ERROR_403 = List.of(
            new TR("error.403.0", "The server says your region is off-limits. Sorry. :( Help available at %s"),
            new TR("error.403.1", "Region not available. I guess your region isn't supported. :( Visit %s"),
            new TR("error.403.2", "I'm blocked by a magical region barrier. :( Learn more at %s"),
            new TR("error.403.3", "Country or region unavailable. :( See %s for help."),
            new TR("error.403.4", "Location denied by the elders. Region not supported :( See %s"),
            new TR("error.403.5", "Messages evaporated. :( Are you from a supported region? %s"),
            new TR("error.403.6", "Your region is having issues, I guess. Check region or get help at %s"),
            new TR("error.403.7", "Check your region or get help at %s"),
            new TR("error.403.8", "Region might not be supported. Try again after checking your region or see %s"),
            new TR("error.403.9", "An angry golem refused me. Confirm your region on %s"),
            new TR("error.403.10", "This gate is region-locked. :( More info at %s"),
            new TR("error.403.11", "I'm trapped outside your region. Learn more at %s"),
            new TR("error.403.12", "Country not supported. :( See %s for advice."),
            new TR("error.403.13", "A barrier ahead blocks us. Check your region or see %s for help.")
    );
    public static final List<TR> ERROR_429 = List.of(
            new TR("error.429.0", "Slow down! We hit the quota. See %s"),
            new TR("error.429.1", "Overload! The token bucket is dry. Refill at %s"),
            new TR("error.429.2", "Too many requests. Take a breather and check %s"),
            new TR("error.429.3", "My brain is out of tokens or you're typing too fast. %s"),
            new TR("error.429.4", "You might have exceeded the daily chatter limit. Details on %s"),
            new TR("error.429.5", "Rate limit reached. Pause for a moment and visit %s"),
            new TR("error.429.6", "Oops, I babbled too much. Get help at %s"),
            new TR("error.429.7", "The LLM says I'm too chatty. Get help at %s"),
            new TR("error.429.8", "Quota exhausted, or you talk too much. %s"),
            new TR("error.429.9", "Your token bank is empty. Replenish or slow down the messaging! %s"),
            new TR("error.429.10", "Hold on, I'm hitting the brake. Quota problems? %s"),
            new TR("error.429.11", "Our conversation is temporarily paused. Learn more at %s"),
            new TR("error.429.12", "Token supply run out. Grab extras on %s"),
            new TR("error.429.13", "I can't talk until we slow down. Try %s"),
            new TR("error.429.14", "Rate limited by the server. Info at %s"),
            new TR("error.429.15", "Token meter flashing red. Head over to %s"),
            new TR("error.429.16", "The API is tired of us. Let it rest (or buy more tokens) %s")
    );
    public static final List<TR> ERROR_500 = List.of(
            new TR("error.500.0", "The server's brain melted. Let's retry later. See %s"),
            new TR("error.500.1", "Something broke on their end. Keep calm and visit %s"),
            new TR("error.500.2", "I triggered an internal hiccup. Try again or check %s"),
            new TR("error.500.3", "Server is tangled in redstone dust. Hang tight! %s"),
            new TR("error.500.4", "Oops, big crash over there. We'll need to wait. %s"),
            new TR("error.500.5", "The API tripped on itself. Maybe check their status or %s"),
            new TR("error.500.6", "Redstone bug. Give it a sec! %s"),
            new TR("error.500.7", "Internal server meltdown! More details at %s"),
            new TR("error.500.8", "Something on the other side is broken. %s"),
            new TR("error.500.9", "The LLM spilled its coffee on the redstone wires, whoops. %s"),
            new TR("error.500.10", "Back-end exploded. Redstone everywhere. Help at %s"),
            new TR("error.500.11", "It's not you, it's them. Hang on and see %s"),
            new TR("error.500.12", "The server monster ate my redstone. Look up %s"),
            new TR("error.500.13", "I received gibberish back. Another hiccup. %s"),
            new TR("error.500.14", "They're busy fixing stuff. In the meantime, %s")
    );
    public static final List<TR> ERROR_503 = List.of(
            new TR("error.503.0", "Service is overloaded. We'll try later. %s"),
            new TR("error.503.1", "Too many folks chatting. Hold tight! %s"),
            new TR("error.503.2", "The engine is overworked right now. See %s"),
            new TR("error.503.3", "Server says come back later. Info at %s"),
            new TR("error.503.4", "They're taking a quick nap. Check %s"),
            new TR("error.503.5", "Busy signal! Let's retry soon. %s"),
            new TR("error.503.6", "Server at capacity. Patience! %s"),
            new TR("error.503.7", "I'm stuck in a queue. Learn more at %s"),
            new TR("error.503.8", "Overload! Wait a tick then check %s"),
            new TR("error.503.9", "The API is swamped. Guidance at %s"),
            new TR("error.503.10", "Server busy, please hold... In the meantime %s"),
            new TR("error.503.11", "They put up a 'Back Soon' sign. %s"),
            new TR("error.503.12", "Service unavailable. The fix? See %s"),
            new TR("error.503.13", "The server is catching its breath. %s"),
            new TR("error.503.14", "Our internal redstone is literally on fire. %s"),
            new TR("error.503.15", "They're at capacity and can't talk. %s"),
            new TR("error.503.16", "Try again once they fix everything. %s")
    );

    public static Stream<TR> allErrorText() {
        return Stream.of(ERROR_GENERAL, ERROR_CONNECTION, ERROR_401, ERROR_403, ERROR_429, ERROR_500, ERROR_503)
                .flatMap(List::stream);
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
        if (messageType.equals(RandomType.NO_RESPONSE)) {
            messages = noResponseMessages;
        } else if (messageType.equals(RandomType.ADJECTIVE)) {
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

    // Get random error text by type
    public static TR getRandomError(ErrorType errorType) {
        Random random = new Random();
        List<TR> messages = ERROR_GENERAL;
        switch (errorType) {
            case CONNECTION:
                messages = ERROR_CONNECTION;
                break;
            case CODE401:
                messages = ERROR_401;
                break;
            case CODE403:
                messages = ERROR_403;
                break;
            case CODE429:
                messages = ERROR_429;
                break;
            case CODE500:
                messages = ERROR_500;
                break;
            case CODE503:
                messages = ERROR_503;
                break;
            default:
                messages = ERROR_GENERAL;
        }
        int index = random.nextInt(messages.size());
        return messages.get(index);
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
