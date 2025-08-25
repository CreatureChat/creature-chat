// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.utils;

import net.minecraft.resources.ResourceLocation;

/**
 * Version-specific advancement background helper for 1.21+ where the
 * background path no longer resides under the "textures" directory and
 * omits the file extension.
 */
public final class AdvancementBackgroundHelper {
    private AdvancementBackgroundHelper() {}

    /** Returns the base UI texture location for the given name. */
    public static ResourceLocation ui(String name) {
        return new ResourceLocation("creaturechat", "ui/" + name);
    }

    /** Returns the provided location unchanged. */
    public static ResourceLocation prependTextures(ResourceLocation base) {
        return base;
    }
}
