// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.message;

/**
 * The {@code Behavior} class represents a single behavior with an optional argument.
 * Arguments may be numeric or textual depending on the behavior (i.e. FRIENDSHIP uses
 * integers while BUILD uses a string such as "house").
 */
public class Behavior {
    private final String name;
    private final String argument;

    public Behavior(String name, String argument) {
        this.name = name;
        this.argument = argument;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getArgument() {
        return argument;
    }

    /**
     * Helper to parse the argument as an {@link Integer}. Returns {@code null} if the
     * argument is absent or cannot be parsed as an integer.
     */
    public Integer getArgumentAsInt() {
        if (argument == null) {
            return null;
        }
        try {
            return Integer.valueOf(argument.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return argument != null ? name + ": " + argument : name;
    }
}
