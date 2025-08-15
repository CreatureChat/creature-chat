// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.inventory;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for retrieving loot tables in a version-agnostic way.
 */
public class LootTableHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");

    public static LootTable get(ServerLevel level, ResourceLocation id) {
        LootTable table = level.getServer().getLootData().getLootTable(id);
        if (table == LootTable.EMPTY) {
            LOGGER.info("Loot table {} not found or empty", id);
        } else {
            LOGGER.info("Loaded loot table {}", id);
        }
        return table;
    }
}
