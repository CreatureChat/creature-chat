// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.datagen;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

/**
 * 1.20.5+ variant of the loot-table provider.
 *
 * <p>The Fabric datagen API added a registry lookup parameter and changed the
 * {@code generate} signature beginning with 1.20.5.</p>
 */
public class CreatureChatLootTableProvider extends SimpleFabricLootTableProvider {
    public CreatureChatLootTableProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, LootContextParamSets.ALL_PARAMS);
    }

    @Override
    public void generate(HolderLookup.Provider registries, BiConsumer<ResourceKey<LootTable>, LootTable.Builder> consumer) {
        InventoryLootTableGenerator.generate((name, table) ->
                consumer.accept(ResourceKey.create(Registries.LOOT_TABLE, id("biomes/" + name)), table));
    }

    private ResourceLocation id(String path) {
        return new ResourceLocation("creaturechat", path);
    }
}

