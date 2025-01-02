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

    // Regex capturing all text in (group1), and trailing emojis in (group2).
    // Only these exact emojis are recognized, optionally with spaces before/after.
    private static final Pattern TRAILING_BEHAVIORS = Pattern.compile("^(.*?)((?:\\s*(?:🚫|👣|🏃|🛡️|⚔️|🐕|❤️|💔))+)(.*)$");

    // Regex to find each recognized emoji in the trailing chunk.
    private static final Pattern RECOGNIZED_EMOJI = Pattern.compile("🚫|👣|🏃(?:‍[♂♀️])?|🛡(?:️)?|⚔(?:️)?|🐕|❤(?:️)?|💔");

    public static ParsedMessage parseMessage(String input) {
        LOGGER.debug("Parsing message: {}", input);

        String updated = parseLegacyBehaviors(input);

        // Separate trailing emojis from main text
        Matcher m = TRAILING_BEHAVIORS.matcher(updated.stripTrailing());
        String mainText = updated;
        String trailing = "";
        if (m.matches()) {
            mainText = m.group(1).stripTrailing();
            trailing = m.group(2).strip();
        }

        LOGGER.debug("Emoji sequence found: {}", trailing);

        List<Behavior> behaviors = new ArrayList<>();
        AtomicInteger friendshipScore = new AtomicInteger(0);
        AtomicBoolean hasFriendship = new AtomicBoolean(false);

        // Find all individual emojis from the trailing chunk
        Matcher emojiMatcher = RECOGNIZED_EMOJI.matcher(trailing);
        while (emojiMatcher.find()) {
            String emoji = emojiMatcher.group();
            LOGGER.debug("Processing emoji: {}", emoji);

            switch (emoji) {
                case "🚫" -> behaviors.add(new Behavior("STOP", null));
                case "👣" -> behaviors.add(new Behavior("FOLLOW", null));
                case "🏃" -> behaviors.add(new Behavior("FLEE", null));
                case "🛡️" -> behaviors.add(new Behavior("PROTECT", null));
                case "⚔️" -> behaviors.add(new Behavior("ATTACK", null));
                case "🐕" -> behaviors.add(new Behavior("LEAD", null));
                case "❤️" -> {
                    friendshipScore.incrementAndGet();
                    hasFriendship.set(true);
                }
                case "💔" -> {
                    friendshipScore.decrementAndGet();
                    hasFriendship.set(true);
                }
            }
        }

        if (hasFriendship.get()) {
            behaviors.add(new Behavior("FRIENDSHIP", friendshipScore.get()));
        }

        LOGGER.debug("Cleaned message: {}", mainText);
        LOGGER.debug("Extracted behaviors: {}", behaviors);

        return new ParsedMessage(mainText, updated.trim(), behaviors);
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
