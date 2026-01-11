// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.buildrec;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Utility for registry lookups that vary across Minecraft versions. */
public final class RegistryUtil {
    private RegistryUtil() {}

    public static Item getItem(ResourceLocation id) {
        if (id == null) return null;
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Holder.Reference<Item> ref = BuiltInRegistries.ITEM.getHolder(key).orElse(null);
        return ref != null ? ref.value() : null;
    }
}
