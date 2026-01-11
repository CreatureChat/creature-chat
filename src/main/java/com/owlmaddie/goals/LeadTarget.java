// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.goals;

/**
 * Represents a parsed target for the LEAD goal.
 */
public class LeadTarget {
    public enum Type {
        STRUCTURE,
        BIOME,
        RESOURCE,
        TAG,
        POI
    }

    private final Type type;
    private final String primary;
    private final String secondary;

    public LeadTarget(Type type, String primary, String secondary) {
        this.type = type;
        this.primary = primary;
        this.secondary = secondary;
    }

    public Type getType() {
        return type;
    }

    public String getPrimary() {
        return primary;
    }

    public String getSecondary() {
        return secondary;
    }
}

