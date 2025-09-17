// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.mixin;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import net.minecraft.world.entity.npc.WanderingTrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents WanderingTraderEntity from despawning if it has chat data or a character sheet.
 */
@Mixin(WanderingTrader.class)
public abstract class MixinWanderingTrader {
    private static final Logger LOGGER = LoggerFactory.getLogger("creaturepals");

    @Inject(method = "maybeDespawn", at = @At("HEAD"), cancellable = true)
    private void preventTraderDespawn(CallbackInfo ci) {
        WanderingTrader trader = (WanderingTrader) (Object) this;

        // Get chat data for this trader
        EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(trader.getStringUUID());

        // If the character sheet is not empty, cancel the function to prevent despawning
        if (!chatData.characterSheet.isEmpty()) {
            ci.cancel();
        }
    }
}
