// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@code MessageParser} class parses out behaviors that are included in messages, and outputs
 * a {@code ParsedMessage} result, which separates the cleaned message and the included behaviors.
 */
public class MessageParser {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturepals");

    public static ParsedMessage parseMessage(String input) {
        LOGGER.debug("Parsing message: {}", input);
        StringBuilder cleanedMessage = new StringBuilder();
        List<Behavior> behaviors = new ArrayList<>();
        Pattern pattern = Pattern.compile("[<*](FOLLOW|LEAD|FLEE|ATTACK|PROTECT|FRIENDSHIP|UNFOLLOW|UNLEAD|UNPROTECT|UNFLEE)[:\\s]*(\\s*[+-]?\\d+)?[>*]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String behaviorName = matcher.group(1);
            Integer argument = null;
            if (matcher.group(2) != null) {
                argument = Integer.valueOf(matcher.group(2));
            }
            behaviors.add(new Behavior(behaviorName, argument));
            LOGGER.debug("Found behavior: {} with argument: {}", behaviorName, argument);

            matcher.appendReplacement(cleanedMessage, "");
        }
        matcher.appendTail(cleanedMessage);

        // Get final cleaned string
        String displayMessage = cleanedMessage.toString().trim();

        // Remove all occurrences of "<>" and "**" (if any)
        displayMessage = displayMessage.replaceAll("<>", "").replaceAll("\\*\\*", "").trim();
        LOGGER.debug("Cleaned message: {}", displayMessage);

        return new ParsedMessage(displayMessage, input.trim(), behaviors);
    }
}
