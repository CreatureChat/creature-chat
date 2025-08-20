// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.datagen;

import com.owlmaddie.items.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.model.ModelTemplates;

/**
 * Generates item model JSON files using Minecraft's model data generators
 * instead of manually constructing JSON.
 */
public class CreatureChatModelProvider extends FabricModelProvider {
    public CreatureChatModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerators) {
        // No block models are generated for this mod.
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerators) {
        // Generates the "minecraft:item/generated" style model for the book item.
        itemModelGenerators.generateFlatItem(ModItems.BOOK, ModelTemplates.FLAT_ITEM);
    }
}

