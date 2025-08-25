// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.utils;

import net.minecraft.resources.ResourceLocation;

/**
 * Utility for resolving advancement background textures across Minecraft versions.
 * The base location omits the "textures/" prefix and file extension so newer
 * versions (1.21+) can reference assets directly while older versions still
 * receive the expected legacy path when prefixed and suffixed.
 */
public final class AdvancementBackgroundHelper {
    private AdvancementBackgroundHelper() {}

    /**
     * Returns the base UI texture location for the given name.
     * The returned path does not include the "textures/" prefix.
     */
    public static ResourceLocation ui(String name) {
        return new ResourceLocation("creaturechat", "ui/" + name);
    }

    /**
     * Prefixes the provided base location with "textures/" and appends
     * ".png" for versions that expect backgrounds as vanilla resource paths.
     */
    public static ResourceLocation prependTextures(ResourceLocation base) {
        if (base == null) return null;
        return new ResourceLocation(base.getNamespace(), "textures/" + base.getPath() + ".png");
    }
}
