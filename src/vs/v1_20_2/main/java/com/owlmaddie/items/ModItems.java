// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.items;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Registers items used by the mod.
 */
public final class ModItems {
    private ModItems() {}

    /** The book that opens the creature log UI. */
    private static final ResourceLocation BOOK_ID = new ResourceLocation("creaturechat", "book");

    public static final Item BOOK = Registry.register(
            BuiltInRegistries.ITEM,
            BOOK_ID,
            new Item(new Item.Properties().stacksTo(1))
    );

    /** Placeholder for future item registrations. */
    public static void register() {}
}
