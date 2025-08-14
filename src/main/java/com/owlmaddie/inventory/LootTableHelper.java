// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.inventory;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Utility for retrieving loot tables in a version-agnostic way.
 */
public class LootTableHelper {
    public static LootTable get(ServerLevel level, ResourceLocation id) {
        return level.getServer().getLootData().getLootTable(id);
    }
}
