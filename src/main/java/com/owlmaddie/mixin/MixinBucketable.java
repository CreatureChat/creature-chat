// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.mixin;

import com.owlmaddie.chat.ChatDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.item.ItemStack;

/**
 * The {@code MixinBucketable} mixin class handles entities that are placed into a bucket, despawned, respawned
 * and updates our chat history for the newly spawned entity.
 */
@Mixin(Bucketable.class)
public interface MixinBucketable {
    //
    @Inject(method = "saveDefaultDataToBucketTag(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"))
    private static void addCPUUIDToStack(Mob entity, ItemStack stack, CallbackInfo ci) {
        Logger LOGGER = LoggerFactory.getLogger("creaturepals");
        UUID originalUUID = entity.getUUID();
        LOGGER.info("Saving original UUID of bucketed entity: " + originalUUID);

        // Add the original UUID to the ItemStack NBT as "CPUUID"
        CompoundTag nbt = stack.getOrCreateTag();
        nbt.putUUID("CPUUID", originalUUID);
    }

    // New method to read CPUUID from NBT
    @Inject(method = "loadDefaultDataFromBucketTag(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private static void readCPUUIDFromNbt(Mob entity, CompoundTag nbt, CallbackInfo ci) {
        Logger LOGGER = LoggerFactory.getLogger("creaturepals");
        UUID newUUID = entity.getUUID();
        if (nbt.contains("CPUUID")) {
            UUID originalUUID = nbt.getUUID("CPUUID");
            LOGGER.info("Duplicating bucketed chat data for original UUID (" + originalUUID + ") to cloned entity: (" + newUUID + ")");
            ChatDataManager.getServerInstance().updateUUID(originalUUID.toString(), newUUID.toString());
        }
    }
}