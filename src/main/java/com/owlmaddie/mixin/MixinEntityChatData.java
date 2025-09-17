// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.mixin;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.utils.NbtCompoundHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

@Mixin(Entity.class)
public abstract class MixinEntityChatData {
    @Shadow
    public abstract UUID getUUID();

    /**
     * When writing NBT data, if the entity has chat data then store its UUID under "CPUUID".
     */
    @Inject(method = "saveWithoutId(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;", at = @At("TAIL"))
    private void writeChatData(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir) {
        UUID currentUUID = this.getUUID();

        // Retrieve or create the chat data for this entity.
        EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(currentUUID.toString());
        // If the entity actually has chat data (for example, if its character sheet is non-empty), add CPUUID.
        if (!chatData.characterSheet.isEmpty()) {
            // Note: cir.getReturnValue() returns the NBT compound the method is about to return.
            CompoundTag returned = cir.getReturnValue();
            NbtCompoundHelper.putUuid(returned, "CPUUID", currentUUID);
        }
    }

    /**
     * When reading NBT data, if there is a "CPUUID" entry and it does not match the entity’s current UUID,
     * update our chat data key to reflect the change.
     */
    @Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void readChatData(CompoundTag nbt, CallbackInfo ci) {
        UUID currentUUID = this.getUUID();
        if (nbt.contains("CPUUID")) {
            UUID originalUUID = NbtCompoundHelper.getUuid(nbt, "CPUUID");
            if (!originalUUID.equals(currentUUID)) {
                ChatDataManager.getServerInstance().updateUUID(originalUUID.toString(), currentUUID.toString());
            }
        }
    }
}
