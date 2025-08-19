// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.mixin;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.commands.ConfigurationHandler;
import com.owlmaddie.inventory.ChatInventory;
import com.owlmaddie.inventory.MobInventoryMenu;
import com.owlmaddie.network.ServerPackets;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.owlmaddie.network.ServerPackets.BroadcastPlayerMessage;

/**
 * The {@code MixinOnChat} mixin class intercepts chat messages from players, and broadcasts them as chat bubbles
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinOnChat {

    @Inject(method = "handleChat(Lnet/minecraft/network/protocol/game/ServerboundChatPacket;)V", at = @At("HEAD"), cancellable = true)
    private void onChatMessage(ServerboundChatPacket packet, CallbackInfo ci) {
        ConfigurationHandler.Config config = new ConfigurationHandler(ServerPackets.serverInstance).loadConfig();
        if (config.getChatBubbles()) {
            ServerGamePacketListenerImpl handler = (ServerGamePacketListenerImpl) (Object) this;
            ServerPlayer player = handler.player;
            String chatMessage = packet.message();
            EntityChatData chatData = new EntityChatData(player.getStringUUID());
            chatData.currentMessage = chatMessage;
            BroadcastPlayerMessage(chatData, player);
        }
    }

    @Inject(method = "handlePlayerCommand", at = @At("HEAD"), cancellable = true)
    private void creaturechat$openMobInventory(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
        if (packet.getAction() != ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY) {
            return;
        }

        ServerGamePacketListenerImpl handler = (ServerGamePacketListenerImpl) (Object) this;
        ServerPlayer serverPlayer = handler.player;

        if (!(serverPlayer.getVehicle() instanceof Mob mob)) {
            return;
        }

        if (mob instanceof Villager || mob instanceof TamableAnimal) {
            return;
        }

        EntityChatData chatData = ChatDataManager.getServerInstance().entityChatDataMap.get(mob.getStringUUID());
        if (chatData == null || chatData.status == ChatDataManager.ChatStatus.NONE) {
            return;
        }

        ExtendedScreenHandlerFactory<Integer> provider = new ExtendedScreenHandlerFactory<>() {
            @Override
            public Integer getScreenOpeningData(ServerPlayer p) {
                return mob.getId();
            }

            @Override
            public Component getDisplayName() {
                return mob.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player p) {
                return new MobInventoryMenu(syncId, playerInventory, ((ChatInventory) mob).creaturechat$getInventory(), mob, serverPlayer);
            }
        };

        serverPlayer.openMenu(provider);
        ci.cancel();
    }
}

