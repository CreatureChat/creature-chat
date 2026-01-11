// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.goals;

import java.util.Locale;
import java.util.Optional;

/**
 * Parses target strings for the LEAD goal. Accepts syntax like:
 * <pre>
 *     village
 *     structure:village
 *     biome:jungle
 *     stronghold:portal_room
 * </pre>
 */
public class LeadTargetParser {
    private LeadTargetParser() {}

    public static Optional<LeadTarget> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String input = raw.trim().toLowerCase(Locale.ROOT);

        if (input.startsWith("#biome:") || input.startsWith("#structure:") || input.startsWith("#resource:")
                || input.startsWith("#poi:") || input.startsWith("#tag:")) {
            input = input.substring(1);
        }

        if ("cave".equals(input)) {
            return Optional.of(new LeadTarget(LeadTarget.Type.BIOME, "cave", null));
        }

        if (input.startsWith("#")) {
            String tag = input.substring(1);
            if (!tag.contains(":") && !tag.startsWith("is_") && !tag.startsWith("has_")) {
                tag = "is_" + tag;
            }
            return Optional.of(new LeadTarget(LeadTarget.Type.TAG, tag, null));
        }

        String[] parts = input.split(":");
        if (parts.length == 1) {
            return Optional.of(new LeadTarget(LeadTarget.Type.STRUCTURE, parts[0], null));
        } else if (parts.length == 2) {
            switch (parts[0]) {
                case "structure":
                    if (parts[1].startsWith("#")) {
                        return Optional.of(new LeadTarget(LeadTarget.Type.TAG, parts[1].substring(1), null));
                    }
                    return Optional.of(new LeadTarget(LeadTarget.Type.STRUCTURE, parts[1], null));
                case "biome":
                    return Optional.of(new LeadTarget(LeadTarget.Type.BIOME, parts[1], null));
                case "resource":
                    return Optional.of(new LeadTarget(LeadTarget.Type.RESOURCE, parts[1], null));
                case "tag":
                    String tagName = parts[1];
                    if (!tagName.contains(":") && !tagName.startsWith("is_") && !tagName.startsWith("has_")) {
                        tagName = "is_" + tagName;
                    }
                    return Optional.of(new LeadTarget(LeadTarget.Type.TAG, tagName, null));
                case "poi":
                    return Optional.of(new LeadTarget(LeadTarget.Type.POI, parts[1], null));
                default:
                    return Optional.of(new LeadTarget(LeadTarget.Type.STRUCTURE, parts[0], parts[1]));
            }
        } else if (parts.length >= 2) {
            return Optional.of(new LeadTarget(LeadTarget.Type.STRUCTURE, parts[0], parts[1]));
        }
        return Optional.empty();
    }
}

