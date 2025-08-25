// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.mixin;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.inventory.ChatInventory;
import com.owlmaddie.inventory.MobInventoryMenu;
import com.owlmaddie.network.ServerPackets;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The {@code MixinMobEntity} mixin class exposes the goalSelector field from the MobEntity class.
 */
@Mixin(Mob.class)
public class MixinMobEntity implements ChatInventory, HasCustomInventoryScreen {

    private final SimpleContainer creaturechat$inventory = new SimpleContainer(15);

    @Override
    public SimpleContainer creaturechat$getInventory() {
        return creaturechat$inventory;
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        Mob thisEntity = (Mob) (Object) this;
        if (thisEntity instanceof Villager || thisEntity instanceof TamableAnimal) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ExtendedScreenHandlerFactory<Integer> provider = new ExtendedScreenHandlerFactory<>() {
                @Override
                public Integer getScreenOpeningData(ServerPlayer p) {
                    return thisEntity.getId();
                }

                @Override
                public Component getDisplayName() {
                    return thisEntity.getDisplayName();
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player p) {
                    return new MobInventoryMenu(syncId, playerInventory, creaturechat$inventory, thisEntity, serverPlayer);
                }
            };
            serverPlayer.openMenu(provider);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void creaturechat$openInventory(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.level().isClientSide()) {
            return;
        }

        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!player.isSecondaryUseActive()) {
            return;
        }

        Mob thisEntity = (Mob) (Object) this;

        if (thisEntity instanceof Villager || thisEntity instanceof TamableAnimal) {
            return;
        }

        // Only open the inventory if chat data exists and has been used
        EntityChatData chatData = ChatDataManager.getServerInstance().entityChatDataMap.get(thisEntity.getStringUUID());
        if (chatData == null || chatData.status == ChatDataManager.ChatStatus.NONE) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ExtendedScreenHandlerFactory<Integer> provider = new ExtendedScreenHandlerFactory<>() {
                @Override
                public Integer getScreenOpeningData(ServerPlayer p) {
                    return thisEntity.getId();
                }

                @Override
                public Component getDisplayName() {
                    return thisEntity.getDisplayName();
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player p) {
                    return new MobInventoryMenu(syncId, playerInventory, creaturechat$inventory, thisEntity, serverPlayer);
                }
            };
            serverPlayer.openMenu(provider);
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void creaturechat$saveInventory(ValueOutput tag, CallbackInfo ci) {
        creaturechat$inventory.storeAsItemList(tag.list("CreatureChatInventory", ItemStack.CODEC));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void creaturechat$loadInventory(ValueInput tag, CallbackInfo ci) {
        tag.childrenList("CreatureChatInventory").ifPresent(list -> {
            if (!list.isEmpty()) {
                ValueInput first = list.stream().findFirst().orElse(null);
                if (first != null && first.child("Item").isPresent()) {
                    for (ValueInput wrapper : list) {
                        int slot = wrapper.getByteOr("Slot", (byte) 0) & 255;
                        if (slot >= 0 && slot < creaturechat$inventory.getContainerSize()) {
                            wrapper.read("Item", ItemStack.CODEC)
                                .ifPresent(stack -> creaturechat$inventory.setItem(slot, stack));
                        }
                    }
                    return;
                }
            }
            tag.list("CreatureChatInventory", ItemStack.CODEC)
                .ifPresent(creaturechat$inventory::fromItemList);
        });
    }

    @Inject(method = "interact", at = @At(value = "RETURN"))
    private void onItemGiven(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        // Only process interactions on the server side
        if (player.level().isClientSide()) {
            return;
        }

        // Only process interactions for the main hand
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        ItemStack itemStack = player.getItemInHand(hand);
        Mob thisEntity = (Mob) (Object) this;

        // Don't interact with Villagers (avoid issues with trade UI) OR Tameable (i.e. sit / no-sit)
        if (thisEntity instanceof Villager || thisEntity instanceof TamableAnimal) {
            return;
        }

        // Determine if the item is a bucket
        // We don't want to interact on buckets
        Item item = itemStack.getItem();
        if (item == Items.BUCKET ||
                item == Items.WATER_BUCKET ||
                item == Items.LAVA_BUCKET ||
                item == Items.POWDER_SNOW_BUCKET ||
                item == Items.MILK_BUCKET ||
                item == Items.PUFFERFISH_BUCKET ||
                item == Items.SALMON_BUCKET ||
                item == Items.COD_BUCKET ||
                item == Items.TROPICAL_FISH_BUCKET ||
                item == Items.AXOLOTL_BUCKET ||
                item == Items.TADPOLE_BUCKET) {
            return;
        }

        // Get chat data for entity
        ChatDataManager chatDataManager = ChatDataManager.getServerInstance();
        EntityChatData entityData = chatDataManager.getOrCreateChatData(thisEntity.getStringUUID());
        PlayerData playerData = entityData.getPlayerData(player.getDisplayName().getString());

        // Check if the player successfully interacts with an item
        if (player instanceof ServerPlayer) {
            // Player has item in hand
            if (!itemStack.isEmpty()) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                String itemName = itemStack.getItem().getName(itemStack).getString();
                int itemCount = itemStack.getCount();

                // Decide verb
                String action_verb = " shows ";
                if (cir.getReturnValue().consumesAction()) {
                    action_verb = " gives ";
                }

                // Prepare a message about the interaction
                String giveItemMessage = "<" + serverPlayer.getDisplayName().getString() +
                        action_verb + "you " + itemCount + " " + itemName + ">";

                if (!entityData.characterSheet.isEmpty()) {
                    ServerPackets.generate_chat("N/A", entityData, serverPlayer, thisEntity, giveItemMessage, true);
                }

            } else if (itemStack.isEmpty() && playerData.friendship == 3) {
                // Player's hand is empty, Ride your best friend!
                player.startRiding(thisEntity, true);
            }
        }
    }
}