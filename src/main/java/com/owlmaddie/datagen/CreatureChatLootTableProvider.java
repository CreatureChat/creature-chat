// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.datagen;

import java.util.function.BiConsumer;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class CreatureChatLootTableProvider extends SimpleFabricLootTableProvider {
    public CreatureChatLootTableProvider(FabricDataOutput output) {
        super(output, LootContextParamSets.ALL_PARAMS);
    }

    @Override
    public void generate(BiConsumer<ResourceLocation, LootTable.Builder> consumer) {
        InventoryLootTableGenerator.generate((name, table) ->
                consumer.accept(id("biomes/" + name), table));
    }

    private ResourceLocation id(String path) {
        return new ResourceLocation("creaturechat", path);
    }
}
