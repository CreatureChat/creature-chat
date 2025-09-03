// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.goals;

import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

/**
 * Locates targets for the LEAD goal within a search radius.
 */
public class LeadTargetLocator {
    private LeadTargetLocator() {}

    public static Vec3 locate(ServerLevel world, BlockPos origin, LeadTarget target, int radius) {
        switch (target.getType()) {
            case STRUCTURE:
                return locateStructure(world, origin, target.getPrimary(), radius);
            case BIOME:
                return locateBiome(world, origin, target.getPrimary(), radius);
            case TAG:
                return locateTag(world, origin, target.getPrimary(), radius);
            case POI:
                return locatePoi(world, origin, target.getPrimary(), radius);
            default:
                return null;
        }
    }

    private static Vec3 locateStructure(ServerLevel world, BlockPos origin, String name, int radius) {
        HolderLookup<Structure> lookup = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        ResourceLocation loc = name.contains(":") ? ResourceLocation.tryParse(name) : new ResourceLocation("minecraft", name);
        if (loc == null) {
            return null;
        }
        ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, loc);
        Optional<Holder.Reference<Structure>> holder = lookup.get(key);
        if (holder.isEmpty()) {
            return null;
        }
        HolderSet<Structure> set = HolderSet.direct(holder.get());
        Pair<BlockPos, Holder<Structure>> result = world.getChunkSource().getGenerator()
                .findNearestMapStructure(world, set, origin, radius, false);
        return result != null ? Vec3.atCenterOf(result.getFirst()) : null;
    }

    private static Vec3 locateBiome(ServerLevel world, BlockPos origin, String name, int radius) {
        ResourceLocation loc = name.contains(":") ? ResourceLocation.tryParse(name) : new ResourceLocation("minecraft", name);
        if (loc == null) {
            return null;
        }
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, loc);
        Pair<BlockPos, Holder<Biome>> result = world.findClosestBiome3d(
                holder -> holder.is(key),
                origin,
                radius,
                32,
                64);
        return result != null ? Vec3.atCenterOf(result.getFirst()) : null;
    }

    private static Vec3 locateTag(ServerLevel world, BlockPos origin, String name, int radius) {
        ResourceLocation tagLoc = name.contains(":") ? ResourceLocation.tryParse(name) : new ResourceLocation("minecraft", name);
        if (tagLoc == null) {
            return null;
        }
        TagKey<Biome> biomeTag = TagKey.create(Registries.BIOME, tagLoc);
        Pair<BlockPos, Holder<Biome>> biomeResult = world.findClosestBiome3d(
                holder -> holder.is(biomeTag),
                origin,
                radius,
                32,
                64);
        if (biomeResult != null) {
            return Vec3.atCenterOf(biomeResult.getFirst());
        }

        TagKey<Structure> structureTag = TagKey.create(Registries.STRUCTURE, tagLoc);
        HolderLookup<Structure> lookup = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Optional<HolderSet.Named<Structure>> set = lookup.get(structureTag);
        if (set.isPresent()) {
            Pair<BlockPos, Holder<Structure>> result = world.getChunkSource().getGenerator()
                    .findNearestMapStructure(world, set.get(), origin, radius, false);
            if (result != null) {
                return Vec3.atCenterOf(result.getFirst());
            }
        }
        return null;
    }

    private static Vec3 locatePoi(ServerLevel world, BlockPos origin, String name, int radius) {
        HolderLookup<PoiType> lookup = world.registryAccess().lookupOrThrow(Registries.POINT_OF_INTEREST_TYPE);
        ResourceLocation loc = name.contains(":") ? ResourceLocation.tryParse(name) : new ResourceLocation("minecraft", name);
        if (loc == null) {
            return null;
        }
        ResourceKey<PoiType> key = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, loc);
        Optional<Holder.Reference<PoiType>> holder = lookup.get(key);
        if (holder.isEmpty()) {
            return null;
        }
        Optional<BlockPos> pos = world.getPoiManager().findClosest(p -> p == holder.get(), origin, radius, PoiManager.Occupancy.ANY);
        return pos.map(p -> Vec3.atCenterOf(p)).orElse(null);
    }
}

