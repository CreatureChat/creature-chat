// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC – unauthorized use prohibited
package com.owlmaddie.mixin;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/*  This is modified for Minecraft 1.21.6. */
@Mixin(Entity.class)
public abstract class MixinEntityChatData {

    @Shadow
    public abstract UUID getUUID();

    /** Add "CPUUID" when the entity already owns chat data. */
    @Inject(method = "saveWithoutId(Lnet/minecraft/world/level/storage/ValueOutput;)V",
            at = @At("TAIL"))
    private void writeChatData(ValueOutput valueOutput, CallbackInfo ci) {
        UUID uuid = this.getUUID();
        EntityChatData chatData =
                ChatDataManager.getServerInstance().getOrCreateChatData(uuid.toString());

        if (!chatData.characterSheet.isEmpty()) {
            valueOutput.store("CPUUID", UUIDUtil.CODEC, uuid);
        }
    }

    /** Re-link old chat data after the entity is deserialized. */
    @Inject(method = "load(Lnet/minecraft/world/level/storage/ValueInput;)V",
            at = @At("TAIL"))
    private void readChatData(ValueInput valueInput, CallbackInfo ci) {
        UUID current = this.getUUID();

        valueInput.read("CPUUID", UUIDUtil.CODEC).ifPresent(original -> {
            if (!original.equals(current)) {
                ChatDataManager.getServerInstance()
                        .updateUUID(original.toString(), current.toString());
            }
        });
    }
}
