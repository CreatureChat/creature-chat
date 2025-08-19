// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.inventory;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Version-specific helper for retrieving loot tables on 1.20.5+.
 */
public class LootTableHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");

    public static LootTable get(ServerLevel level, ResourceLocation id) {
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, id);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        if (table == LootTable.EMPTY) {
            LOGGER.info("Loot table {} not found or empty", id);
        } else {
            LOGGER.info("Loaded loot table {}", id);
        }
        return table;
    }
}
