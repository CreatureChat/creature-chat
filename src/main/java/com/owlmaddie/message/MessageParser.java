package com.owlmaddie.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@code MessageParser} class parses out behaviors that are included in messages, and outputs
 * a {@code ParsedMessage} result, which separates the cleaned message and the included behaviors.
 */
public class MessageParser {
    private static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");

    // We only match our "recognized" emojis anywhere in the text:
    // (Short versions shown here, but adjust to whatever codepoints you actually use.)
    private static final Pattern RECOGNIZED_EMOJI = Pattern.compile("🚫|👣|🏃(?:‍[♂♀️])?|🛡(?:️)?|⚔(?:️)?|🐕|❤(?:️)?|💔");
    private static final Pattern ZWJ_OR_VARIATION = Pattern.compile("[\u200D\uFE0E\uFE0F]");

    private static String normalizeEmoji(String raw) {
        // remove zero-width joiners, variation selectors, etc.
        return ZWJ_OR_VARIATION.matcher(raw).replaceAll("");
    }

    public static ParsedMessage parseMessage(String input) {
        LOGGER.debug("Parsing message: {}", input);

        String updated = parseLegacyBehaviors(input);

        // List of extracted behaviors
        List<Behavior> behaviors = new ArrayList<>();
        AtomicInteger friendshipScore = new AtomicInteger(0);
        AtomicBoolean hasFriendship = new AtomicBoolean(false);

        // We'll remove recognized emojis from the entire message,
        // building a 'cleaned' version as we go
        Matcher emojiMatcher = RECOGNIZED_EMOJI.matcher(updated);
        StringBuffer cleanedBuffer = new StringBuffer();

        while (emojiMatcher.find()) {
            String rawEmoji = emojiMatcher.group();
            String normalized = normalizeEmoji(rawEmoji);  // remove extra codepoints
            LOGGER.debug("Processing raw emoji: {}, normalized: {}", rawEmoji, normalized);

            // Handle only the base "normalized" emoji
            switch (normalized) {
                case "🏃" -> behaviors.add(new Behavior("FLEE", null));
                case "👣" -> behaviors.add(new Behavior("FOLLOW", null));
                case "🛡" -> behaviors.add(new Behavior("PROTECT", null));
                case "⚔" -> behaviors.add(new Behavior("ATTACK", null));
                case "🐕" -> behaviors.add(new Behavior("LEAD", null));
                case "🚫" -> behaviors.add(new Behavior("STOP", null));
                case "❤" -> {
                    friendshipScore.incrementAndGet();
                    hasFriendship.set(true);
                }
                case "💔" -> {
                    friendshipScore.decrementAndGet();
                    hasFriendship.set(true);
                }
            }

            // Replace recognized emoji with nothing in the cleaned text
            emojiMatcher.appendReplacement(cleanedBuffer, "");
        }
        // Append any remaining text after the last match
        emojiMatcher.appendTail(cleanedBuffer);

        // If friendship changed, add that as a separate behavior
        if (hasFriendship.get()) {
            behaviors.add(new Behavior("FRIENDSHIP", friendshipScore.get()));
        }

        // Now 'cleanedBuffer' has the text with recognized emojis removed
        String cleanedMessage = cleanedBuffer.toString().stripTrailing();

        LOGGER.debug("Cleaned message: {}", cleanedMessage);
        LOGGER.debug("Extracted behaviors: {}", behaviors);

        // Return the result
        return new ParsedMessage(cleanedMessage, updated.trim(), behaviors);
    }

    private static String parseLegacyBehaviors(String input) {
        LOGGER.debug("Parsing legacy behaviors in message: {}", input);

        Matcher matcher = Pattern.compile(
                "[<*](FOLLOW|LEAD|FLEE|ATTACK|PROTECT|FRIENDSHIP|UNFOLLOW|UNLEAD|UNPROTECT|UNFLEE|UNATTACK)" +
                        "[:\\s]*(\\s*[+-]?\\d+)?[>*]").matcher(input);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String behavior = matcher.group(1).toUpperCase();
            String value = matcher.group(2);
            String replacement = switch (behavior) {
                case "FOLLOW" -> "👣";
                case "LEAD" -> "🐕";
                case "FLEE" -> "🏃‍♂️";
                case "PROTECT" -> "🛡️";
                case "ATTACK" -> "⚔️";
                case "UNFOLLOW", "UNLEAD", "UNPROTECT", "UNFLEE", "UNATTACK" -> "🚫";
                case "FRIENDSHIP" -> {
                    int score = value != null ? Integer.parseInt(value.trim()) : 0;
                    if (score > 0) yield "❤️".repeat(score);
                    else if (score < 0) yield "💔".repeat(-score);
                    else yield "";
                }
                default -> "";
            };
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        LOGGER.debug("Updated message after legacy behavior parsing: {}", sb);
        return sb.toString();
    }
}
