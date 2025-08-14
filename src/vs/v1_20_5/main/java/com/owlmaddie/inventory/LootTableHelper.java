package com.owlmaddie.inventory;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Version-specific helper for retrieving loot tables on 1.20.5+.
 */
public class LootTableHelper {
    public static LootTable get(ServerLevel level, ResourceLocation id) {
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, id);
        return level.getServer().reloadableRegistries().getLootTable(key);
    }
}
