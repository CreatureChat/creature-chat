// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.mixin;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.inventory.ChatInventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Redirect abstract villager inventory to CreatureChat inventory when chat data exists.
 */
@Mixin(AbstractVillager.class)
public abstract class MixinAbstractVillagerEntity {

    @Shadow @Final @Mutable
    private SimpleContainer inventory;

    @Inject(method = "getInventory", at = @At("HEAD"), cancellable = true)
    private void creaturechat$useChatInventory(CallbackInfoReturnable<SimpleContainer> cir) {
        Mob mob = (Mob) (Object) this;
        ChatDataManager manager = ChatDataManager.getServerInstance();
        if (manager.entityChatDataMap.containsKey(mob.getStringUUID())) {
            SimpleContainer chatInv = (SimpleContainer) ((ChatInventory) mob).creaturechat$getInventory();
            if (inventory != chatInv) {
                if (!inventory.isEmpty()) {
                    for (int i = 0; i < Math.min(inventory.getContainerSize(), chatInv.getContainerSize()); i++) {
                        chatInv.setItem(i, inventory.getItem(i));
                        inventory.setItem(i, ItemStack.EMPTY);
                    }
                }
                inventory = chatInv;
            }
            cir.setReturnValue(chatInv);
        }
    }
}
