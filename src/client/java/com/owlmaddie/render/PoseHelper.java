// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.render;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Cross-version wrapper for simple pose transformations.
 */
public final class PoseHelper {
    private PoseHelper() {}

    public static void push(PoseStack stack) {
        stack.pushPose();
    }

    public static void pop(PoseStack stack) {
        stack.popPose();
    }

    public static void translate(PoseStack stack, float x, float y) {
        stack.translate(x, y, 0);
    }

    public static void scale(PoseStack stack, float sx, float sy) {
        stack.scale(sx, sy, 1.0f);
    }
}
