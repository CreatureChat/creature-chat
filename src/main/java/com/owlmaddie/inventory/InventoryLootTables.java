// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.inventory;

import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBiomeTags;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * Selects biome-specific loot tables for mob inventories.
 */
public class InventoryLootTables {
    private static ResourceLocation id(String name) {
        return new ResourceLocation("creaturechat", "biomes/" + name);
    }

    public static ResourceLocation forBiome(Holder<Biome> biome) {
        if (biome.is(ConventionalBiomeTags.CAVES)) return id("cave");
        if (biome.is(ConventionalBiomeTags.MUSHROOM)) return id("mushroom");
        if (biome.is(ConventionalBiomeTags.IN_THE_END)) return id("end");
        if (biome.is(ConventionalBiomeTags.IN_NETHER)) return id("nether");
        if (biome.is(ConventionalBiomeTags.JUNGLE)) return id("jungle");
        if (biome.is(ConventionalBiomeTags.SWAMP)) return id("swamp");
        if (biome.is(ConventionalBiomeTags.BEACH)) return id("beach");
        if (biome.is(ConventionalBiomeTags.AQUATIC)) return id("aquatic");
        if (biome.is(ConventionalBiomeTags.TAIGA)) return id("taiga");
        if (biome.is(ConventionalBiomeTags.SNOWY)) return id("snowy");
        if (biome.is(ConventionalBiomeTags.CLIMATE_HOT)) return id("dry_overworld");
        if (biome.is(ConventionalBiomeTags.FOREST)) return id("forest");
        if (biome.is(ConventionalBiomeTags.PLAINS)) return id("plains");
        return id("catch_all");
    }
}
