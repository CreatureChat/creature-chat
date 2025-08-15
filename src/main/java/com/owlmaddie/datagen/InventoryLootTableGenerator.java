// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.datagen;

import java.util.function.BiConsumer;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * Builds the per-biome inventory loot tables used by the datagen providers.
 * <p>
 * The generated tables mirror the former static JSON files but are defined in
 * code so they can compile across multiple Minecraft versions.
 */
public final class InventoryLootTableGenerator {
    private InventoryLootTableGenerator() {}

    /**
     * Emits loot tables for every supported biome.
     *
     * @param out callback receiving the biome name and built table
     */
    public static void generate(BiConsumer<String, LootTable.Builder> out) {
        addBiome(out, "aquatic",
                w(Items.KELP, 12, 2.0F, 7.0F), w(Items.SEAGRASS, 10, 2.0F, 5.0F), w(Items.SAND, 8, 3.0F, 6.0F),
                w(Items.CLAY_BALL, 7, 3.0F, 6.0F), w(Items.COD, 5, 1.0F, 3.0F), w(Items.SALMON, 5, 1.0F, 3.0F),
                w(Items.BONE, 3, 1.0F, 3.0F), w(Items.PRISMARINE_SHARD, 2, 1.0F, 2.0F), w(Items.SEA_PICKLE, 2, 1.0F, 4.0F),
                w(Items.NAUTILUS_SHELL, 1, 1.0F, 1.0F)
        );
        addBiome(out, "beach",
                w(Items.SAND, 12, 1.0F, 7.0F), w(Items.STICK, 10, 1.0F, 7.0F), w(Items.SUGAR_CANE, 8, 1.0F, 4.0F),
                w(Items.KELP, 8, 1.0F, 5.0F), w(Items.SEAGRASS, 8, 1.0F, 4.0F), w(Items.SANDSTONE, 6, 1.0F, 3.0F),
                w(Items.STRING, 5, 1.0F, 3.0F), w(Items.GLASS_BOTTLE, 3, 1.0F, 1.0F), w(Items.SEA_PICKLE, 2, 1.0F, 4.0F),
                w(Items.NAUTILUS_SHELL, 1, 1.0F, 1.0F)
        );
        addBiome(out, "catch_all",
                w(Items.STICK, 12, 1.0F, 7.0F), w(Items.DIRT, 10, 1.0F, 7.0F), w(Items.COBBLESTONE, 10, 1.0F, 7.0F),
                w(Items.FLINT, 8, 1.0F, 5.0F), w(Items.STRING, 6, 1.0F, 4.0F), w(Items.FEATHER, 6, 1.0F, 4.0F),
                w(Items.BONE, 6, 1.0F, 4.0F), w(Items.LEATHER, 4, 1.0F, 3.0F), w(Items.APPLE, 3, 1.0F, 1.0F),
                w(Items.COAL, 3, 1.0F, 4.0F)
        );
        addBiome(out, "cave",
                w(Items.COBBLESTONE, 12, 1.0F, 7.0F), w(Items.COAL, 10, 1.0F, 5.0F), w(Items.FLINT, 8, 1.0F, 4.0F),
                w(Items.TORCH, 8, 2.0F, 6.0F), w(Items.STRING, 6, 1.0F, 3.0F), w(Items.BONE, 6, 1.0F, 3.0F),
                w(Items.SPIDER_EYE, 3, 1.0F, 1.0F), w(Items.IRON_NUGGET, 2, 1.0F, 3.0F), w(Items.DRIPSTONE_BLOCK, 4, 1.0F, 3.0F),
                w(Items.GLOW_BERRIES, 3, 1.0F, 3.0F)
        );
        addBiome(out, "dry_overworld",
                w(Items.SAND, 10, 2.0F, 7.0F), w(Items.RED_SAND, 8, 2.0F, 5.0F), w(Items.SANDSTONE, 6, 1.0F, 3.0F),
                w(Items.CACTUS, 8, 1.0F, 3.0F), w(Items.DEAD_BUSH, 6, 1.0F, 1.0F), w(Items.TERRACOTTA, 4, 1.0F, 3.0F),
                w(Items.ORANGE_TERRACOTTA, 4, 1.0F, 3.0F), w(Items.STICK, 6, 2.0F, 5.0F), w(Items.RABBIT_HIDE, 2, 1.0F, 1.0F),
                w(Items.GOLD_NUGGET, 1, 1.0F, 1.0F)
        );
        addBiome(out, "end",
                w(Items.END_STONE, 12, 2.0F, 7.0F), w(Items.CHORUS_FRUIT, 8, 1.0F, 4.0F), w(Items.POPPED_CHORUS_FRUIT, 4, 1.0F, 3.0F),
                w(Items.PURPUR_BLOCK, 6, 2.0F, 4.0F), w(Items.PURPUR_PILLAR, 4, 1.0F, 3.0F), w(Items.OBSIDIAN, 2, 1.0F, 1.0F),
                w(Items.PURPLE_DYE, 2, 1.0F, 3.0F), w(Items.CHORUS_FLOWER, 2, 1.0F, 1.0F), w(Items.END_ROD, 1, 1.0F, 2.0F),
                w(Items.GLASS_BOTTLE, 3, 1.0F, 1.0F)
        );
        addBiome(out, "forest",
                w(Items.STICK, 12, 2.0F, 7.0F), w(Items.OAK_SAPLING, 10, 1.0F, 1.0F), w(Items.BIRCH_SAPLING, 8, 1.0F, 1.0F),
                w(Items.APPLE, 6, 1.0F, 1.0F), w(Items.BROWN_MUSHROOM, 6, 1.0F, 3.0F), w(Items.RED_MUSHROOM, 6, 1.0F, 3.0F),
                w(Items.WHEAT_SEEDS, 8, 2.0F, 7.0F), w(Items.DANDELION, 5, 2.0F, 4.0F), w(Items.POPPY, 5, 2.0F, 4.0F),
                w(Items.OAK_LOG, 3, 1.0F, 1.0F)
        );
        addBiome(out, "jungle",
                w(Items.BAMBOO, 12, 2.0F, 7.0F), w(Items.STICK, 10, 2.0F, 6.0F), w(Items.VINE, 8, 2.0F, 5.0F),
                w(Items.COCOA_BEANS, 8, 2.0F, 5.0F), w(Items.SUGAR_CANE, 6, 2.0F, 5.0F), w(Items.MELON_SLICE, 6, 2.0F, 4.0F),
                w(Items.JUNGLE_SAPLING, 8, 1.0F, 1.0F), w(Items.MELON, 4, 1.0F, 1.0F), w(Items.TROPICAL_FISH, 3, 1.0F, 3.0F),
                w(Items.FEATHER, 3, 1.0F, 3.0F)
        );
        addBiome(out, "mushroom",
                w(Items.BROWN_MUSHROOM, 12, 2.0F, 7.0F), w(Items.RED_MUSHROOM, 10, 2.0F, 7.0F), w(Items.MUSHROOM_STEW, 2, 1.0F, 1.0F),
                w(Items.BOWL, 3, 2.0F, 3.0F), w(Items.BONE_MEAL, 6, 2.0F, 5.0F), w(Items.DIRT, 6, 3.0F, 5.0F),
                w(Items.COMPOSTER, 2, 1.0F, 1.0F), w(Items.SUGAR, 3, 1.0F, 3.0F), w(Items.OAK_SAPLING, 3, 1.0F, 1.0F),
                w(Items.MYCELIUM, 1, 1.0F, 1.0F)
        );
        addBiome(out, "nether",
                w(Items.NETHERRACK, 12, 2.0F, 7.0F), w(Items.SOUL_SAND, 8, 1.0F, 4.0F), w(Items.BASALT, 8, 2.0F, 5.0F),
                w(Items.BLACKSTONE, 6, 2.0F, 4.0F), w(Items.NETHER_WART, 5, 1.0F, 3.0F), w(Items.WARPED_FUNGUS, 4, 1.0F, 3.0F),
                w(Items.CRIMSON_FUNGUS, 4, 1.0F, 3.0F), w(Items.GLOWSTONE_DUST, 3, 2.0F, 4.0F), w(Items.QUARTZ, 3, 1.0F, 3.0F),
                w(Items.MAGMA_CREAM, 1, 1.0F, 1.0F)
        );
        addBiome(out, "plains",
                w(Items.WHEAT_SEEDS, 12, 2.0F, 7.0F), w(Items.STICK, 10, 2.0F, 6.0F), w(Items.DANDELION, 8, 2.0F, 5.0F),
                w(Items.POPPY, 8, 2.0F, 5.0F), w(Items.OAK_SAPLING, 6, 1.0F, 1.0F), w(Items.BEETROOT_SEEDS, 6, 2.0F, 7.0F),
                w(Items.PUMPKIN_SEEDS, 5, 2.0F, 7.0F), w(Items.MELON_SEEDS, 5, 2.0F, 7.0F), w(Items.BONE_MEAL, 3, 2.0F, 4.0F),
                w(Items.BREAD, 2, 1.0F, 1.0F)
        );
        addBiome(out, "snowy",
                w(Items.SNOWBALL, 12, 2.0F, 7.0F), w(Items.SNOW_BLOCK, 10, 1.0F, 3.0F), w(Items.SPRUCE_SAPLING, 8, 1.0F, 1.0F),
                w(Items.SWEET_BERRIES, 6, 2.0F, 4.0F), w(Items.STICK, 6, 2.0F, 5.0F), w(Items.ICE, 4, 1.0F, 3.0F),
                w(Items.PACKED_ICE, 3, 1.0F, 1.0F), w(Items.WHITE_DYE, 3, 1.0F, 3.0F), w(Items.RABBIT_HIDE, 2, 1.0F, 1.0F),
                w(Items.BLUE_ICE, 1, 1.0F, 1.0F)
        );
        addBiome(out, "swamp",
                w(Items.CLAY_BALL, 10, 3.0F, 7.0F), w(Items.STICK, 10, 2.0F, 6.0F), w(Items.LILY_PAD, 8, 1.0F, 4.0F),
                w(Items.VINE, 8, 2.0F, 5.0F), w(Items.SUGAR_CANE, 7, 2.0F, 5.0F), w(Items.MUD, 6, 1.0F, 4.0F),
                w(Items.MANGROVE_PROPAGULE, 3, 1.0F, 1.0F), w(Items.MANGROVE_ROOTS, 4, 1.0F, 3.0F), w(Items.SLIME_BALL, 2, 1.0F, 1.0F),
                w(Items.BOWL, 3, 2.0F, 3.0F)
        );
        addBiome(out, "taiga",
                w(Items.SPRUCE_SAPLING, 10, 1.0F, 1.0F), w(Items.STICK, 10, 2.0F, 6.0F), w(Items.SWEET_BERRIES, 8, 2.0F, 5.0F),
                w(Items.SNOWBALL, 6, 1.0F, 7.0F), w(Items.FERN, 4, 1.0F, 3.0F), w(Items.SPRUCE_LOG, 6, 1.0F, 1.0F),
                w(Items.BONE, 4, 1.0F, 3.0F), w(Items.COBBLESTONE, 5, 1.0F, 6.0F), w(Items.WHITE_DYE, 3, 1.0F, 3.0F),
                w(Items.RABBIT_HIDE, 3, 1.0F, 1.0F)
        );
    }

    private static void addBiome(BiConsumer<String, LootTable.Builder> out, String name, WeightedItem... items) {
        LootPool.Builder pool = LootPool.lootPool().setRolls(UniformGenerator.between(1.0F, 5.0F));
        for (WeightedItem item : items) {
            NumberProvider number = item.min == item.max
                    ? ConstantValue.exactly(item.min)
                    : UniformGenerator.between(item.min, item.max);
            pool.add(LootItem.lootTableItem(item.item).setWeight(item.weight)
                    .apply(SetItemCountFunction.setCount(number)));
        }
        out.accept(name, LootTable.lootTable().withPool(pool));
    }

    private static WeightedItem w(Item item, int weight, float min, float max) {
        return new WeightedItem(item, weight, min, max);
    }

    private record WeightedItem(Item item, int weight, float min, float max) {}
}

