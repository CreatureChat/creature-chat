// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

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
            "<counts imaginary stars>"
    );
    private static List<String> errorsGeneral = Arrays.asList(
            "Seems like my words got lost in the End. Check out {LINK} for clues!",
            "Oops! My speech bubble popped. Need help? Visit {LINK}",
            "I might've eaten a bad Command Block. Help me out at {LINK}!",
            "My words are on strike. More info? {LINK}",
            "I think a Creeper blew up my script. Instructions? {LINK}",
            "BRB, asking a villager for directions to {LINK}",
            "It’s not you, it’s my API key. Let's regroup at {LINK}",
            "I tried to speak, but it was a critical miss. Help at {LINK}",
            "Words are hard. Come chat at {LINK}",
            "I must've left my responses in my other pants. See {LINK}",
            "Shh... I’m hiding from an invalid API key. Join the hunt at {LINK}",
            "I’d tell you, but then I’d have to respawn. Meet me at {LINK}",
            "Response not found. Maybe it’s hiding at {LINK}?",
            "I'm speechless, literally. Let's troubleshoot at {LINK}",
            "Looks like my connection got lost in the Nether. Can you help? {LINK}",
            "I forgot what I was saying, but {LINK} remembers.",
            "Are my words mining without a pickaxe? Dig up some help at {LINK}",
            "Sorry, my parrot ate the response. Teach it better at {LINK}",
            "My magic mirror says: 'Better answers found at {LINK}'",
            "This message is temporarily out of order. Order yours at {LINK}"
    );
    private static List<String> errorsConnection = Arrays.asList(
            "No signal! Maybe connect to the internet? {LINK}",
            "Looks like your request was blocked by a firewall. Info at {LINK}",
            "Can't reach the server. Are you offline? {LINK}",
            "Socket errors everywhere! See {LINK}",
            "This network portal is closed. Check your connection. {LINK}",
            "Are we underground? I can't find the web. {LINK}",
            "Your network doesn't like me. More help at {LINK}",
            "I bumped into a blocked port. {LINK}",
            "Maybe Windows Defender is grumpy. Try adding an exception. {LINK}",
            "I'm stuck at the network gate. {LINK}",
            "Your wifi might have vanished. {LINK}",
            "Firewall says 'nope'. {LINK}",
            "We might have no internet at all. {LINK}",
            "Socket meltdown! {LINK}"
    );
    private static List<String> errors401 = Arrays.asList(
            "The server thinks I'm an impostor. Double-check that API key via {LINK}",
            "I lost my credentials! Maybe update the API key? {LINK}",
            "No entry without a key! Visit {LINK} to learn how.",
            "API door slammed shut. Did you set a valid key? {LINK}",
            "I'm keyless and confused. Retrieve your API key from {LINK}",
            "Access denied! That's usually a bad API key. See {LINK}",
            "The server waved a big 'go away' sign. Revisit your API key settings on {LINK}",
            "I forgot the secret handshake. Check your API key at {LINK}",
            "Bad token, bad! Replace the API key, details at {LINK}",
            "Guardians won't let me in. Did you misplace the API key? {LINK}",
            "Invalid passphrase. Grab the right API key from {LINK}",
            "No key, no chat! Configure it via {LINK}",
            "My access card is missing. Acquire a new API key at {LINK}",
            "Locked out! Time to fix your API key at {LINK}",
            "I'm stuck at the login gate. Double-check the API key at {LINK}",
            "You need the golden API key to open this door. See {LINK}",
            "Unauthorized entry! Insert a valid API key via {LINK}"
    );
    private static List<String> errors403 = Arrays.asList(
            "The server says my region is off-limits. Check your region at {LINK}",
            "A border guard stopped me. Maybe your region isn't supported. Visit {LINK}",
            "I'm blocked by a magical barrier. Check your region at {LINK}",
            "I can't step into that country. See {LINK} for region help",
            "Location denied by the elders. Review your region at {LINK}",
            "The door is barred. Are you from a supported region? {LINK}",
            "Your account lacks the correct spells. Check region at {LINK}",
            "I'm grounded by the API. Check your region at {LINK}",
            "The server gave me a stern no. Try again after fixing your region on {LINK}",
            "An angry guardian refused me. Confirm your region on {LINK}",
            "This gate is region-locked. More info at {LINK}",
            "I'm trapped outside the perimeter. Get region help at {LINK}",
            "Country not supported! See {LINK} for advice",
            "A barrier ahead blocks us. Check your region at {LINK}"
    );
    private static List<String> errors429 = Arrays.asList(
            "Slow down! We hit the quota wall. See {LINK}",
            "Overload! The token bucket is dry. Refill at {LINK}",
            "Too many requests. Take a breather and check {LINK}",
            "My brain is out of tokens. Top up or wait! {LINK}",
            "You've exceeded the daily chatter limit. Details on {LINK}",
            "Rate limit reached. Pause for a moment and visit {LINK}",
            "Oops, I babbled too much. More info at {LINK}",
            "The LLM says I'm too chatty. Maybe upgrade? {LINK}",
            "Quota exhausted! We need more tokens from {LINK}",
            "Your token bank is empty. Replenish via {LINK}",
            "Hold on, I'm hitting the brake. Quota problems? {LINK}",
            "Our conversation is temporarily paused. Learn more at {LINK}",
            "Token supply run out. Grab extras on {LINK}",
            "I can't talk until we slow down. Try {LINK}",
            "Too many asks at once! Wait a bit then check {LINK}",
            "Rate limited by the server. Info at {LINK}",
            "We've used up our allowance. Refill instructions at {LINK}",
            "Token meter flashing red. Head over to {LINK}",
            "The API is tired of us. Let it rest (or buy more) {LINK}",
            "Need more tokens to proceed! Click {LINK}"
    );
    private static List<String> errors500 = Arrays.asList(
            "The server's brain melted. Let's retry later. See {LINK}",
            "Something broke on their end. Keep calm and visit {LINK}",
            "I triggered an internal hiccup. Try again or check {LINK}",
            "Server is tangled in redstone dust. Hang tight! {LINK}",
            "Oops, big crash over there. We'll need to wait. {LINK}",
            "The API tripped on itself. Maybe check their status or {LINK}",
            "I'm getting a server error dance. Info? {LINK}",
            "That request made the server puke. Give it a sec! {LINK}",
            "Internal server meltdown! More details at {LINK}",
            "Something on the other side is broken. {LINK}",
            "The LLM spilled its coffee on the wires. {LINK}",
            "Their machine is sulking. Retry soon, or visit {LINK}",
            "They're having a bad day. Check {LINK}",
            "Server's thrown a fit. Let's consult {LINK}",
            "Back-end exploded. When in doubt, {LINK}",
            "My request hit a snag there. Wait then read {LINK}",
            "It's them, not you. Hang on and see {LINK}",
            "The server monster ate my query. Look up {LINK}",
            "I received gibberish back. Another hiccup. {LINK}",
            "They're busy fixing stuff. In the meantime, {LINK}"
    );
    private static List<String> errors503 = Arrays.asList(
            "Service is overloaded. We'll try later. {LINK}",
            "Too many folks chatting. Hold tight! {LINK}",
            "The engine is overworked right now. See {LINK}",
            "Server says come back later. Info at {LINK}",
            "They're taking a quick nap. Check {LINK}",
            "Busy signal! Let's retry soon. {LINK}",
            "Server at capacity. Patience! {LINK}",
            "The machine is doing maintenance. See {LINK}",
            "I'm stuck in a queue. Learn more at {LINK}",
            "This lane is temporarily closed. {LINK}",
            "Overload! Wait a tick then check {LINK}",
            "The API is swamped. Guidance at {LINK}",
            "Out for lunch. We'll get through eventually. {LINK}",
            "Server busy, please hold... In the meantime {LINK}",
            "They put up a 'Back Soon' sign. {LINK}",
            "Service unavailable. The fix? See {LINK}",
            "The server is catching its breath. {LINK}",
            "It's overloaded like a mob farm. {LINK}",
            "They're at capacity and can't talk. {LINK}",
            "Try again once they clean up. {LINK}"
    );
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

    // Get random error message by type
    public static String getRandomErrorMessage(ErrorType errorType) {
        Random random = new Random();
        List<String> messages = errorsGeneral;
        switch (errorType) {
            case CONNECTION:
                messages = errorsConnection;
                break;
            case CODE401:
                messages = errors401;
                break;
            case CODE403:
                messages = errors403;
                break;
            case CODE429:
                messages = errors429;
                break;
            case CODE500:
                messages = errors500;
                break;
            case CODE503:
                messages = errors503;
                break;
            default:
                messages = errorsGeneral;
        }
        int index = random.nextInt(messages.size());
        return messages.get(index).replace("{LINK}", DISCORD_LINK).trim();
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
