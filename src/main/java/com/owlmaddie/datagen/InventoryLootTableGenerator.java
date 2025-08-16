// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.datagen;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
 * The generated tables mirror the former static JSON files but are defined in
 * code so they can compile across multiple Minecraft versions.
 */
public final class InventoryLootTableGenerator {
    private static final NumberProvider DEFAULT_ROLLS = UniformGenerator.between(2.0F, 8.0F);

    private InventoryLootTableGenerator() {}

    /**
     * Emits loot tables for every supported biome.
     *
     * @param out callback receiving the biome name and built table
     */
    public static void generate(BiConsumer<String, LootTable.Builder> out) {
        biome(out, "aquatic", b -> {
            b.item(Items.SAND, 10, 1, 16);
            b.item(Items.GRAVEL, 10, 1, 16);
            b.item(Items.KELP, 9, 1, 8);
            b.item(Items.COD, 8, 1, 3);
            b.item(Items.SALMON, 7, 1, 3);
            b.item(Items.CLAY_BALL, 5, 4, 8);
            b.item(Items.COAL, 4, 1, 6);
            b.item(Items.DIRT, 4, 1, 16);
            b.item(Items.INK_SAC, 3, 1, 6);
            b.item(Items.SEA_PICKLE, 3, 1, 4);
            b.item(Items.PRISMARINE_SHARD, 2, 1, 4);
            b.item(Items.PRISMARINE_CRYSTALS, 2, 1, 1);
            b.item(Items.NAUTILUS_SHELL, 1, 1, 1);
        });
        biome(out, "beach", b -> {
            b.item(Items.SAND, 10, 1, 16);
            b.item(Items.SANDSTONE, 8, 1, 16);
            b.item(Items.COD, 7, 1, 3);
            b.item(Items.STICK, 6, 1, 8);
            b.item(Items.SUGAR_CANE, 5, 1, 6);
            b.item(Items.KELP, 5, 1, 5);
            b.item(Items.GLASS_BOTTLE, 4, 1, 1);
            b.item(Items.DIRT, 4, 1, 16);
            b.item(Items.SEA_PICKLE, 3, 1, 4);
            b.item(Items.STRING, 3, 1, 2);
            b.item(Items.FISHING_ROD, 1, 1, 1);
        });
        biome(out, "cave", b -> {
            b.item(Items.COBBLESTONE, 10, 1, 16);
            b.item(Items.COAL, 9, 1, 8);
            b.item(Items.TORCH, 8, 4, 8);
            b.item(Items.GLOW_BERRIES, 7, 2, 6);
            b.item(Items.DIRT, 7, 1, 16);
            b.item(Items.STRING, 6, 1, 3);
            b.item(Items.BONE, 6, 1, 5);
            b.item(Items.SPIDER_EYE, 6, 1, 1);
            b.item(Items.RAW_COPPER, 5, 1, 6);
            b.item(Items.ARROW, 5, 1, 4);
            b.item(Items.FLINT, 5, 1, 2);
            b.item(Items.REDSTONE, 4, 1, 6);
            b.item(Items.LAPIS_LAZULI, 4, 1, 5);
            b.item(Items.GUNPOWDER, 3, 1, 3);
            b.item(Items.IRON_NUGGET, 3, 1, 6);
            b.item(Items.RAW_IRON, 2, 1, 6);
            b.item(Items.IRON_INGOT, 1, 1, 1);

        });
        biome(out, "dry_overworld", b -> {
            b.item(Items.SAND, 10, 1, 16);
            b.item(Items.DIRT, 8, 1, 16);
            b.item(Items.STICK, 7, 1, 8);
            b.item(Items.SANDSTONE, 6, 1, 6);
            b.item(Items.CACTUS, 6, 1, 3);
            b.item(Items.DEAD_BUSH, 6, 1, 4);
            b.item(Items.TERRACOTTA, 4, 1, 3);
            b.item(Items.RABBIT, 4, 1, 2);
            b.item(Items.RABBIT_HIDE, 3, 1, 1);
            b.item(Items.COBBLESTONE, 3, 1, 16);
            b.item(Items.GOLD_NUGGET, 2, 1, 8);
            b.item(Items.GOLD_INGOT, 1, 1, 1);

        });
        biome(out, "end", b -> {
            b.item(Items.END_STONE, 10, 1, 16);
            b.item(Items.CHORUS_FRUIT, 9, 1, 8);
            b.item(Items.END_STONE_BRICKS, 8, 1, 16);
            b.item(Items.PURPUR_BLOCK, 7, 1, 8);
            b.item(Items.POPPED_CHORUS_FRUIT, 7, 1, 4);
            b.item(Items.END_ROD, 6, 1, 4);
            b.item(Items.CHORUS_FLOWER, 5, 1, 1);
            b.item(Items.OBSIDIAN, 4, 1, 4);
            b.item(Items.ENDER_PEARL, 4, 1, 3);
            b.item(Items.GOLD_INGOT, 4, 1, 3);
            b.item(Items.IRON_INGOT, 3, 1, 5);
            b.item(Items.DIAMOND, 1, 1, 2);
        });
        biome(out, "forest", b -> {
            b.item(Items.DIRT, 12, 1, 16);
            b.item(Items.STICK, 11, 2, 8);
            b.item(Items.WHEAT_SEEDS, 10, 1, 8);
            b.item(Items.OAK_SAPLING, 9, 1, 3);
            b.item(Items.BIRCH_SAPLING, 9, 1, 3);
            b.item(Items.OAK_PLANKS, 8, 1, 16);
            b.item(Items.DANDELION, 8, 1, 5);
            b.item(Items.POPPY, 8, 1, 5);
            b.item(Items.PINK_PETALS, 7, 1, 8);
            b.item(Items.OAK_LOG, 7, 1, 8);
            b.item(Items.OXEYE_DAISY, 6, 1, 5);
            b.item(Items.CORNFLOWER, 6, 1, 5);
            b.item(Items.ALLIUM, 6, 1, 5);
            b.item(Items.APPLE, 4, 1, 5);
            b.item(Items.COBBLESTONE, 2, 1, 16);
            b.item(Items.HONEYCOMB, 1, 1, 3);
        });
        biome(out, "jungle", b -> {
            b.item(Items.BAMBOO, 12, 1, 16);
            b.item(Items.STICK, 10, 3, 8);
            b.item(Items.JUNGLE_SAPLING, 10, 4, 8);
            b.item(Items.COCOA_BEANS, 9, 3, 8);
            b.item(Items.MELON_SEEDS, 9, 1, 3);
            b.item(Items.MELON_SLICE, 8, 1, 6);
            b.item(Items.MELON, 5, 1, 1);
            b.item(Items.JUNGLE_PLANKS, 4, 1, 16);
            b.item(Items.JUNGLE_LOG, 3, 1, 4);
            b.item(Items.BAMBOO_BLOCK, 3, 1, 3);
            b.item(Items.COBBLESTONE, 2, 1, 16);
            b.item(Items.GOLD_NUGGET, 2, 1, 8);
            b.item(Items.GOLD_INGOT, 1, 1, 4);
        });
        biome(out, "mushroom", b -> {
            b.item(Items.DIRT, 14, 1, 16);
            b.item(Items.BROWN_MUSHROOM, 12, 1, 8);
            b.item(Items.RED_MUSHROOM, 10, 1, 8);
            b.item(Items.BOWL, 9, 1, 3);
            b.item(Items.MUSHROOM_STEW, 5, 1, 1);
            b.item(Items.OAK_PLANKS, 4, 1, 16);
            b.item(Items.OAK_LOG, 3, 1, 4);
            b.item(Items.OAK_SAPLING, 3, 1, 4);
            b.item(Items.DARK_OAK_SAPLING, 2, 4, 8);
            b.item(Items.DARK_OAK_LOG, 2, 1, 4);
            b.item(Items.COBBLESTONE, 1, 1, 16);
            b.item(Items.SUSPICIOUS_STEW, 1, 1, 1);
            b.item(Items.MYCELIUM, 1, 1, 4);
        });
        biome(out, "nether", b -> {
            b.item(Items.NETHERRACK, 12, 4, 16);
            b.item(Items.GLOWSTONE_DUST, 11, 2, 8);
            b.item(Items.QUARTZ, 10, 1, 8);
            b.item(Items.MAGMA_CREAM, 10, 1, 4);
            b.item(Items.NETHER_BRICK, 9, 1, 8);
            b.item(Items.NETHER_BRICKS, 8, 1, 16);
            b.item(Items.GOLD_NUGGET, 7, 3, 8);
            b.item(Items.BLACKSTONE, 7, 1, 16);
            b.item(Items.POLISHED_BLACKSTONE, 6, 1, 8);
            b.item(Items.SOUL_SAND, 6, 1, 8);
            b.item(Items.CRIMSON_FUNGUS, 5, 1, 4);
            b.item(Items.WARPED_FUNGUS, 5, 1, 4);
            b.item(Items.GLASS_BOTTLE, 4, 1, 3);
            b.item(Items.NETHER_WART, 4, 1, 8);
            b.item(Items.GOLD_INGOT, 3, 1, 4);
            b.item(Items.GHAST_TEAR, 2, 1, 3);
            b.item(Items.OBSIDIAN, 1, 1, 4);
        });
        biome(out, "plains", b -> {
            b.item(Items.DIRT, 12, 1, 16);
            b.item(Items.WHEAT_SEEDS, 11, 4, 8);
            b.item(Items.STICK, 11, 1, 8);
            b.item(Items.DANDELION, 10, 1, 5);
            b.item(Items.POPPY, 10, 1, 5);
            b.item(Items.SUNFLOWER, 9, 1, 5);
            b.item(Items.WHEAT, 8, 1, 5);
            b.item(Items.BEETROOT_SEEDS, 7, 1, 6);
            b.item(Items.CARROT, 7, 2, 8);
            b.item(Items.POTATO, 6, 2, 8);
            b.item(Items.BONE_MEAL, 5, 1, 3);
            b.item(Items.BREAD, 4, 1, 8);
            b.item(Items.OAK_SAPLING, 3, 1, 3);
            b.item(Items.OAK_PLANKS, 2, 1, 16);
            b.item(Items.COBBLESTONE, 1, 1, 16);
        });
        biome(out, "snowy", b -> {
            b.item(Items.DIRT, 12, 1, 16);
            b.item(Items.SNOWBALL, 12, 4, 8);
            b.item(Items.SWEET_BERRIES, 10, 1, 8);
            b.item(Items.STICK, 9, 1, 8);
            b.item(Items.WHEAT_SEEDS, 9, 1, 6);
            b.item(Items.SNOW_BLOCK, 8, 1, 3);
            b.item(Items.SPRUCE_SAPLING, 7, 1, 6);
            b.item(Items.RABBIT, 6, 1, 2);
            b.item(Items.ICE, 5, 1, 8);
            b.item(Items.SPRUCE_PLANKS, 4, 1, 16);
            b.item(Items.RABBIT_HIDE, 2, 1, 3);
            b.item(Items.PACKED_ICE, 1, 1, 4);
        });
        biome(out, "swamp", b -> {
            b.item(Items.DIRT, 12, 1, 16);
            b.item(Items.STICK, 11, 2, 8);
            b.item(Items.CLAY_BALL, 10, 4, 16);
            b.item(Items.LILY_PAD, 10, 1, 4);
            b.item(Items.SUGAR_CANE, 9, 2, 5);
            b.item(Items.MUD, 8, 1, 8);
            b.item(Items.SUGAR, 8, 1, 3);
            b.item(Items.BOWL, 7, 1, 3);
            b.item(Items.GLASS_BOTTLE, 7, 1, 4);
            b.item(Items.MANGROVE_PROPAGULE, 6, 1, 4);
            b.item(Items.MANGROVE_PLANKS, 6, 1, 16);
            b.item(Items.MANGROVE_LOG, 5, 1, 4);
            b.item(Items.SUSPICIOUS_STEW, 4, 1, 1);
            b.item(Items.COBBLESTONE, 2, 1, 16);
            b.item(Items.SLIME_BALL, 1, 1, 4);
        });
        biome(out, "taiga", b -> {
            b.item(Items.DIRT, 12, 1, 16);
            b.item(Items.SPRUCE_SAPLING, 11, 1, 8);
            b.item(Items.STICK, 10, 2, 8);
            b.item(Items.SWEET_BERRIES, 9, 3, 8);
            b.item(Items.SPRUCE_PLANKS, 8, 1, 16);
            b.item(Items.FERN, 7, 1, 3);
            b.item(Items.COARSE_DIRT, 6, 1, 8);
            b.item(Items.BONE_MEAL, 5, 1, 3);
            b.item(Items.BROWN_MUSHROOM, 4, 1, 5);
            b.item(Items.RED_MUSHROOM, 4, 1, 5);
            b.item(Items.SPRUCE_LOG, 2, 1, 4);
            b.item(Items.MOSSY_COBBLESTONE, 1, 1, 8);
            b.item(Items.COBBLESTONE, 1, 1, 16);
        });
        biome(out, "catch_all", b -> {
            b.item(Items.DIRT, 10, 1, 16);
            b.item(Items.STICK, 9, 1, 8);
            b.item(Items.APPLE, 8, 1, 6);
            b.item(Items.DANDELION, 7, 1, 5);
            b.item(Items.WHEAT_SEEDS, 6, 1, 8);
            b.item(Items.FEATHER, 6, 1, 3);
            b.item(Items.SWEET_BERRIES, 5, 1, 8);
            b.item(Items.FLINT, 4, 1, 3);
            b.item(Items.COAL, 3, 1, 4);
            b.item(Items.OAK_PLANKS, 2, 1, 16);
            b.item(Items.COBBLESTONE, 1, 1, 16);
        });
    }

    private static void biome(BiConsumer<String, LootTable.Builder> out, String name, Consumer<BiomeBuilder> config) {
        BiomeBuilder builder = new BiomeBuilder();
        config.accept(builder);
        out.accept(name, builder.build());
    }

    private static final class BiomeBuilder {
        private final LootPool.Builder pool = LootPool.lootPool().setRolls(DEFAULT_ROLLS);

        void item(Item item, int weight, int min, int max) {
            NumberProvider count = min == max ? ConstantValue.exactly(min) : UniformGenerator.between(min, max);
            pool.add(LootItem.lootTableItem(item).setWeight(weight)
                    .apply(SetItemCountFunction.setCount(count)));
        }

        LootTable.Builder build() {
            return LootTable.lootTable().withPool(pool);
        }
    }
}

