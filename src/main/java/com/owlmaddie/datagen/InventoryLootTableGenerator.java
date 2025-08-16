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
    private static final NumberProvider DEFAULT_ROLLS = UniformGenerator.between(1.0F, 5.0F);

    private InventoryLootTableGenerator() {}

    /**
     * Emits loot tables for every supported biome.
     *
     * @param out callback receiving the biome name and built table
     */
    public static void generate(BiConsumer<String, LootTable.Builder> out) {
        biome(out, "aquatic", b -> {
            b.item(Items.KELP, 12, 2, 7);
            b.item(Items.SEAGRASS, 10, 2, 5);
            b.item(Items.SAND, 8, 3, 6);
            b.item(Items.CLAY_BALL, 7, 3, 6);
            b.item(Items.COD, 5, 1, 3);
            b.item(Items.SALMON, 5, 1, 3);
            b.item(Items.BONE, 3, 1, 3);
            b.item(Items.PRISMARINE_SHARD, 2, 1, 2);
            b.item(Items.SEA_PICKLE, 2, 1, 4);
            b.item(Items.NAUTILUS_SHELL, 1, 1, 1);
        });
        biome(out, "beach", b -> {
            b.item(Items.SAND, 12, 1, 7);
            b.item(Items.STICK, 10, 1, 7);
            b.item(Items.SUGAR_CANE, 8, 1, 4);
            b.item(Items.KELP, 8, 1, 5);
            b.item(Items.SEAGRASS, 8, 1, 4);
            b.item(Items.SANDSTONE, 6, 1, 3);
            b.item(Items.STRING, 5, 1, 3);
            b.item(Items.GLASS_BOTTLE, 3, 1, 1);
            b.item(Items.SEA_PICKLE, 2, 1, 4);
            b.item(Items.NAUTILUS_SHELL, 1, 1, 1);
        });
        biome(out, "cave", b -> {
            b.item(Items.COBBLESTONE, 12, 1, 7);
            b.item(Items.COAL, 10, 1, 5);
            b.item(Items.FLINT, 8, 1, 4);
            b.item(Items.TORCH, 8, 2, 6);
            b.item(Items.STRING, 6, 1, 3);
            b.item(Items.BONE, 6, 1, 3);
            b.item(Items.SPIDER_EYE, 3, 1, 1);
            b.item(Items.IRON_NUGGET, 2, 1, 3);
            b.item(Items.DRIPSTONE_BLOCK, 4, 1, 3);
            b.item(Items.GLOW_BERRIES, 3, 1, 3);
        });
        biome(out, "dry_overworld", b -> {
            b.item(Items.SAND, 10, 2, 7);
            b.item(Items.RED_SAND, 8, 2, 5);
            b.item(Items.SANDSTONE, 6, 1, 3);
            b.item(Items.CACTUS, 8, 1, 3);
            b.item(Items.DEAD_BUSH, 6, 1, 1);
            b.item(Items.TERRACOTTA, 4, 1, 3);
            b.item(Items.ORANGE_TERRACOTTA, 4, 1, 3);
            b.item(Items.STICK, 6, 2, 5);
            b.item(Items.RABBIT_HIDE, 2, 1, 1);
            b.item(Items.GOLD_NUGGET, 1, 1, 1);
        });
        biome(out, "end", b -> {
            b.item(Items.END_STONE, 12, 2, 7);
            b.item(Items.CHORUS_FRUIT, 8, 1, 4);
            b.item(Items.POPPED_CHORUS_FRUIT, 4, 1, 3);
            b.item(Items.PURPUR_BLOCK, 6, 2, 4);
            b.item(Items.PURPUR_PILLAR, 4, 1, 3);
            b.item(Items.OBSIDIAN, 2, 1, 1);
            b.item(Items.PURPLE_DYE, 2, 1, 3);
            b.item(Items.CHORUS_FLOWER, 2, 1, 1);
            b.item(Items.END_ROD, 1, 1, 2);
            b.item(Items.GLASS_BOTTLE, 3, 1, 1);
        });
        biome(out, "forest", b -> {
            b.item(Items.STICK, 12, 2, 7);
            b.item(Items.OAK_SAPLING, 10, 1, 1);
            b.item(Items.BIRCH_SAPLING, 8, 1, 1);
            b.item(Items.APPLE, 6, 1, 1);
            b.item(Items.BROWN_MUSHROOM, 6, 1, 3);
            b.item(Items.RED_MUSHROOM, 6, 1, 3);
            b.item(Items.WHEAT_SEEDS, 8, 2, 7);
            b.item(Items.DANDELION, 5, 2, 4);
            b.item(Items.POPPY, 5, 2, 4);
            b.item(Items.OAK_LOG, 3, 1, 1);
        });
        biome(out, "jungle", b -> {
            b.item(Items.BAMBOO, 12, 2, 7);
            b.item(Items.STICK, 10, 2, 6);
            b.item(Items.VINE, 8, 2, 5);
            b.item(Items.COCOA_BEANS, 8, 2, 5);
            b.item(Items.SUGAR_CANE, 6, 2, 5);
            b.item(Items.MELON_SLICE, 6, 2, 4);
            b.item(Items.JUNGLE_SAPLING, 8, 1, 1);
            b.item(Items.MELON, 4, 1, 1);
            b.item(Items.TROPICAL_FISH, 3, 1, 3);
            b.item(Items.FEATHER, 3, 1, 3);
        });
        biome(out, "mushroom", b -> {
            b.item(Items.BROWN_MUSHROOM, 12, 2, 7);
            b.item(Items.RED_MUSHROOM, 10, 2, 7);
            b.item(Items.MUSHROOM_STEW, 2, 1, 1);
            b.item(Items.BOWL, 3, 2, 3);
            b.item(Items.BONE_MEAL, 6, 2, 5);
            b.item(Items.DIRT, 6, 3, 5);
            b.item(Items.COMPOSTER, 2, 1, 1);
            b.item(Items.SUGAR, 3, 1, 3);
            b.item(Items.OAK_SAPLING, 3, 1, 1);
            b.item(Items.MYCELIUM, 1, 1, 1);
        });
        biome(out, "nether", b -> {
            b.item(Items.NETHERRACK, 12, 2, 7);
            b.item(Items.SOUL_SAND, 8, 1, 4);
            b.item(Items.BASALT, 8, 2, 5);
            b.item(Items.BLACKSTONE, 6, 2, 4);
            b.item(Items.NETHER_WART, 5, 1, 3);
            b.item(Items.WARPED_FUNGUS, 4, 1, 3);
            b.item(Items.CRIMSON_FUNGUS, 4, 1, 3);
            b.item(Items.GLOWSTONE_DUST, 3, 2, 4);
            b.item(Items.QUARTZ, 3, 1, 3);
            b.item(Items.MAGMA_CREAM, 1, 1, 1);
        });
        biome(out, "plains", b -> {
            b.item(Items.WHEAT_SEEDS, 12, 2, 7);
            b.item(Items.STICK, 10, 2, 6);
            b.item(Items.DANDELION, 8, 2, 5);
            b.item(Items.POPPY, 8, 2, 5);
            b.item(Items.OAK_SAPLING, 6, 1, 1);
            b.item(Items.BEETROOT_SEEDS, 6, 2, 7);
            b.item(Items.PUMPKIN_SEEDS, 5, 2, 7);
            b.item(Items.MELON_SEEDS, 5, 2, 7);
            b.item(Items.BONE_MEAL, 3, 2, 4);
            b.item(Items.BREAD, 2, 1, 1);
        });
        biome(out, "snowy", b -> {
            b.item(Items.SNOWBALL, 12, 2, 7);
            b.item(Items.SNOW_BLOCK, 10, 1, 3);
            b.item(Items.SPRUCE_SAPLING, 8, 1, 1);
            b.item(Items.SWEET_BERRIES, 6, 2, 4);
            b.item(Items.STICK, 6, 2, 5);
            b.item(Items.ICE, 4, 1, 3);
            b.item(Items.PACKED_ICE, 3, 1, 1);
            b.item(Items.WHITE_DYE, 3, 1, 3);
            b.item(Items.RABBIT_HIDE, 2, 1, 1);
            b.item(Items.BLUE_ICE, 1, 1, 1);
        });
        biome(out, "swamp", b -> {
            b.item(Items.CLAY_BALL, 10, 3, 7);
            b.item(Items.STICK, 10, 2, 6);
            b.item(Items.LILY_PAD, 8, 1, 4);
            b.item(Items.VINE, 8, 2, 5);
            b.item(Items.SUGAR_CANE, 7, 2, 5);
            b.item(Items.MUD, 6, 1, 4);
            b.item(Items.MANGROVE_PROPAGULE, 3, 1, 1);
            b.item(Items.MANGROVE_ROOTS, 4, 1, 3);
            b.item(Items.SLIME_BALL, 2, 1, 1);
            b.item(Items.BOWL, 3, 2, 3);
        });
        biome(out, "taiga", b -> {
            b.item(Items.SPRUCE_SAPLING, 10, 1, 1);
            b.item(Items.STICK, 10, 2, 6);
            b.item(Items.SWEET_BERRIES, 8, 2, 5);
            b.item(Items.SNOWBALL, 6, 1, 7);
            b.item(Items.FERN, 4, 1, 3);
            b.item(Items.SPRUCE_LOG, 6, 1, 1);
            b.item(Items.BONE, 4, 1, 3);
            b.item(Items.COBBLESTONE, 5, 1, 6);
            b.item(Items.WHITE_DYE, 3, 1, 3);
            b.item(Items.RABBIT_HIDE, 3, 1, 1);
        });
        biome(out, "catch_all", b -> {
            b.item(Items.STICK, 12, 1, 7);
            b.item(Items.DIRT, 10, 1, 7);
            b.item(Items.COBBLESTONE, 10, 1, 7);
            b.item(Items.FLINT, 8, 1, 5);
            b.item(Items.STRING, 6, 1, 4);
            b.item(Items.FEATHER, 6, 1, 4);
            b.item(Items.BONE, 6, 1, 4);
            b.item(Items.LEATHER, 4, 1, 3);
            b.item(Items.APPLE, 3, 1, 1);
            b.item(Items.COAL, 3, 1, 4);
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

