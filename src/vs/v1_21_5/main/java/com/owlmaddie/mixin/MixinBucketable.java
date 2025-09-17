// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.mixin;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.utils.NbtCompoundHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * {@code copyDataFromNbt} – read that tag when the mob respawns and move
 * chat data from the old UUID to the new one. Modified for Minecraft 1.21.0+.
 */
@Mixin(Bucketable.class)
public interface MixinBucketable {

    // capture: mob → bucket
    @Inject(
            method = "saveDefaultDataToBucketTag(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/item/ItemStack;)V",
            at     = @At("TAIL")
    )
    private static void captureCPUUID(Mob entity, ItemStack stack, CallbackInfo ci) {
        UUID oldId = entity.getUUID();

        // Append our CPUUID to the existing BUCKET_ENTITY_DATA component
        CustomData.update(
                DataComponents.BUCKET_ENTITY_DATA,
                stack,
                tag -> NbtCompoundHelper.putUuid(tag, "CPUUID", oldId)
        );

        LoggerFactory.getLogger("creaturepals")
                .info("[Bucket-Capture] stored {}", oldId);
    }

    // release: bucket → mob
    @Inject(
            method = "loadDefaultDataFromBucketTag(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/nbt/CompoundTag;)V",
            at     = @At("TAIL")
    )
    private static void restoreCPUUID(Mob entity, CompoundTag nbt, CallbackInfo ci) {
        if (!NbtCompoundHelper.containsUuid(nbt, "CPUUID")) return;

        UUID oldId = NbtCompoundHelper.getUuid(nbt, "CPUUID");
        UUID newId = entity.getUUID();

        ChatDataManager.getServerInstance()
                .updateUUID(oldId.toString(), newId.toString());

        LoggerFactory.getLogger("creaturepals")
                .info("[Bucket-Release] chat {} → {}", oldId, newId);
    }
}
