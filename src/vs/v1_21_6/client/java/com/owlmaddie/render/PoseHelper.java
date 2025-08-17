// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.render;

import org.joml.Matrix3x2fStack;

/**
 * Cross-version wrapper for simple pose transformations.
 * Modified for Minecraft 1.21.6+ which uses JOML matrix stacks.
 */
public final class PoseHelper {
    private PoseHelper() {}

    public static void push(Matrix3x2fStack stack) {
        stack.pushMatrix();
    }

    public static void pop(Matrix3x2fStack stack) {
        stack.popMatrix();
    }

    public static void translate(Matrix3x2fStack stack, float x, float y) {
        stack.translate(x, y);
    }

    public static void scale(Matrix3x2fStack stack, float sx, float sy) {
        stack.scale(sx, sy);
    }
}
