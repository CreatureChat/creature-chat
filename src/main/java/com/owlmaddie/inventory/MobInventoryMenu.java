// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.inventory;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.network.ServerPackets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Menu for mob inventories.
 */
public class MobInventoryMenu extends AbstractContainerMenu {
    private final Container inventory;
    private final Mob mob;
    private final ServerPlayer serverPlayer;
    private final Map<Item, Integer> initialCounts = new HashMap<>();
    private final int mobInvSize;
    private final int rows;

    public MobInventoryMenu(int syncId, Inventory playerInventory, Container inventory, Mob mob, ServerPlayer player) {
        super(ModMenus.MOB_INVENTORY, syncId);
        this.inventory = inventory;
        this.mob = mob;
        this.serverPlayer = player;
        this.mobInvSize = inventory.getContainerSize();
        for (int i = 0; i < mobInvSize; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                initialCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        inventory.startOpen(playerInventory.player);

        this.rows = (mobInvSize + 4) / 5;
        int slot = 0;
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < 5 && slot < mobInvSize; ++col) {
                // shift the mob inventory grid two columns to the right so it no longer overlaps the entity preview
                this.addSlot(new Slot(inventory, slot++, 75 + col * 18, 18 + row * 18));
            }
        }

        int startY = 18 + rows * 18 + 14 - 2;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, startY + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, startY + 58));
        }
    }

    public Mob getMob() { return mob; }

    public int getRows() { return rows; }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemStack = stackInSlot.copy();
            if (index < mobInvSize) {
                if (!this.moveItemStackTo(stackInSlot, mobInvSize, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 0, mobInvSize, false)) {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        inventory.stopOpen(player);
        if (!player.level().isClientSide() && player == serverPlayer) {
            Map<Item, Integer> finalCounts = new HashMap<>();
            for (int i = 0; i < mobInvSize; i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    finalCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
                }
            }
            Set<Item> all = new HashSet<>(initialCounts.keySet());
            all.addAll(finalCounts.keySet());
            List<String> added = new ArrayList<>();
            List<String> removed = new ArrayList<>();
            for (Item item : all) {
                int before = initialCounts.getOrDefault(item, 0);
                int after = finalCounts.getOrDefault(item, 0);
                int diff = after - before;
                if (diff > 0) {
                    added.add(diff + " " + new ItemStack(item).getHoverName().getString());
                } else if (diff < 0) {
                    removed.add(-diff + " " + new ItemStack(item).getHoverName().getString());
                }
            }
            if (!added.isEmpty() || !removed.isEmpty()) {
                ChatDataManager chatDataManager = ChatDataManager.getServerInstance();
                EntityChatData chatData = chatDataManager.getOrCreateChatData(mob.getStringUUID());
                PlayerData playerData = chatData.getPlayerData(player.getDisplayName().getString());
                String verb = playerData.friendship > 0 ? " borrowed " : " stole ";
                StringBuilder msg = new StringBuilder("<" + player.getName().getString());
                if (!added.isEmpty()) {
                    msg.append(" gave you ").append(String.join(", ", added));
                    if (!removed.isEmpty()) {
                        msg.append(", and");
                    }
                }
                if (!removed.isEmpty()) {
                    msg.append(verb).append(String.join(", ", removed));
                }
                msg.append(">");
                ServerPackets.generate_chat("N/A", chatData, serverPlayer, mob, msg.toString(), true);
            }
        }
    }
}
