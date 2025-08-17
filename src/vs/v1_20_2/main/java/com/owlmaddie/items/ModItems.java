// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.items;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Registers items used by the mod.
 */
public final class ModItems {
    private ModItems() {}

    /** The book that opens the creature log UI. */
    private static final ResourceLocation MEMORY_BOOK_ID = new ResourceLocation("creaturechat", "memory_book");

    public static final Item MEMORY_BOOK = Registry.register(
            BuiltInRegistries.ITEM,
            MEMORY_BOOK_ID,
            new Item(new Item.Properties()
                    .stacksTo(1)
                    .useItemDescriptionPrefix()
                    .setId(ResourceKey.create(Registries.ITEM, MEMORY_BOOK_ID))
            )
    );

    /** Placeholder for future item registrations. */
    public static void register() {}
}
