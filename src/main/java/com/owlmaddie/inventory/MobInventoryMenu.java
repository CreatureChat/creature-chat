// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.inventory;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.chat.AdvancementHelper;
import com.owlmaddie.network.ServerPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
    private final PlayerData playerData;
    private final Map<Item, Integer> initialCounts = new HashMap<>();
    private final int mobInvSize;
    private final int rows;
    private final int mainHandSlot;
    private final int offHandSlot;
    private final ItemStack initialMainHand;
    private final ItemStack initialOffHand;

    public MobInventoryMenu(int syncId, Inventory playerInventory, Container inventory, Mob mob, ServerPlayer player) {
        super(ModMenus.MOB_INVENTORY, syncId);
        this.inventory = inventory;
        this.mob = mob;
        this.serverPlayer = player;
        ChatDataManager chatDataManager = player == null ? ChatDataManager.getClientInstance() : ChatDataManager.getServerInstance();
        EntityChatData chatData = chatDataManager.getOrCreateChatData(mob.getStringUUID());
        String playerName;
        if (player != null) {
            playerName = player.getDisplayName().getString();
        } else if (playerInventory.player != null) {
            playerName = playerInventory.player.getDisplayName().getString();
        } else {
            playerName = "";
        }
        this.playerData = chatData.getPlayerData(playerName);
        if (player != null && playerData.friendship > 0 && !playerData.openedInventory) {
            playerData.openedInventory = true;
            AdvancementHelper.openSesame(player);
        }
        if (playerData.wordsmithActive) {
            playerData.wordsmithOpenedInventory = true;
        }
        this.mobInvSize = inventory.getContainerSize();
        this.rows = (mobInvSize + 4) / 5;
        int bottomRowStart = (rows - 1) * 5;
        this.mainHandSlot = bottomRowStart;
        this.offHandSlot = bottomRowStart + 1;
        this.initialMainHand = mob.getMainHandItem().copy();
        this.initialOffHand = mob.getOffhandItem().copy();

        syncHandSlots();

        for (int i = 0; i < mobInvSize; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                initialCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        inventory.startOpen(playerInventory.player);

        int slot = 0;
        boolean canAccessInventory = playerData.friendship > 0;
        boolean canAccessHands = playerData.friendship >= 3;
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < 5 && slot < mobInvSize; ++col) {
                // shift the mob inventory grid two columns to the right so it no longer overlaps the entity preview
                int x = 75 + col * 18;
                int y = 18 + row * 18;
                if (slot == mainHandSlot) {
                    this.addSlot(canAccessHands ? new HandSlot(inventory, slot++, x, y, mob, EquipmentSlot.MAINHAND)
                                               : new LockedSlot(inventory, slot++, x, y));
                } else if (slot == offHandSlot) {
                    this.addSlot(canAccessHands ? new HandSlot(inventory, slot++, x, y, mob, EquipmentSlot.OFFHAND)
                                               : new LockedSlot(inventory, slot++, x, y));
                } else {
                    this.addSlot(canAccessInventory ? new Slot(inventory, slot++, x, y)
                                                    : new LockedSlot(inventory, slot++, x, y));
                }
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
            if (!slot.mayPickup(player)) {
                return ItemStack.EMPTY;
            }
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
            if (playerData.friendship <= 0) {
                return;
            }
            Map<Item, Integer> finalCounts = new HashMap<>();
            for (int i = 0; i < mobInvSize; i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    finalCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
                }
            }
            Set<Item> all = new HashSet<>(initialCounts.keySet());
            all.addAll(finalCounts.keySet());
            Map<Item, Integer> added = new HashMap<>();
            Map<Item, Integer> removed = new HashMap<>();
            for (Item item : all) {
                int before = initialCounts.getOrDefault(item, 0);
                int after = finalCounts.getOrDefault(item, 0);
                int diff = after - before;
                if (diff > 0) {
                    added.put(item, diff);
                } else if (diff < 0) {
                    removed.put(item, -diff);
                }
            }
            List<String> disarmedToInventory = new ArrayList<>();
            List<String> disarmedTaken = new ArrayList<>();
            ItemStack finalMain = mob.getMainHandItem();
            ItemStack finalOff = mob.getOffhandItem();
            collectDisarmed(initialMainHand, finalMain, finalOff, disarmedToInventory, disarmedTaken, removed);
            collectDisarmed(initialOffHand, finalMain, finalOff, disarmedToInventory, disarmedTaken, removed);
            boolean swapped = handsSwapped(initialMainHand, initialOffHand, finalMain, finalOff);
            boolean handChanged = (!sameItem(initialMainHand, finalMain) && !finalMain.isEmpty()) ||
                                 (!sameItem(initialOffHand, finalOff) && !finalOff.isEmpty());
            if (!added.isEmpty() || !removed.isEmpty() || !disarmedToInventory.isEmpty() || !disarmedTaken.isEmpty() || swapped || handChanged) {
                ChatDataManager chatDataManager = ChatDataManager.getServerInstance();
                EntityChatData chatData = chatDataManager.getOrCreateChatData(mob.getStringUUID());
                PlayerData pd = chatData.getPlayerData(player.getDisplayName().getString());
                if (pd.wordsmithActive) {
                    pd.wordsmithGaveItem = true;
                }
                String verbBase = pd.friendship >= 3 ? "borrowed" : pd.friendship == 2 ? "took" : "stole";
                String verb = " " + verbBase + " ";
                if (!removed.isEmpty()) {
                    AdvancementHelper.itemTaken(serverPlayer, pd);
                    if (removed.containsKey(Items.DIAMOND)) {
                        AdvancementHelper.theHeist(serverPlayer);
                    }
                }
                if (!added.isEmpty() && pd.friendship > 0) {
                    pd.gaveItem = true;
                    AdvancementHelper.checkSharedStash(serverPlayer);
                }
                if (handChanged && pd.friendship == 3) {
                    AdvancementHelper.sleightOfHand(serverPlayer);
                }
                if (mob.getType() == net.minecraft.world.entity.EntityType.PIG && pd.friendship == 3 && pd.pigProtect &&
                        (finalMain.getItem() == Items.DIAMOND_SWORD || finalMain.getItem() == Items.NETHERITE_SWORD ||
                         finalOff.getItem() == Items.DIAMOND_SWORD || finalOff.getItem() == Items.NETHERITE_SWORD)) {
                    AdvancementHelper.aLegend(serverPlayer);
                    pd.pigProtect = false;
                }
                if (mob.getType() == net.minecraft.world.entity.EntityType.PIG && pd.friendship == 3) {
                    boolean allPotatoes = true;
                    for (int i = 0; i < mobInvSize; i++) {
                        ItemStack stack = inventory.getItem(i);
                        if (stack.isEmpty() || stack.getItem() != Items.POTATO || stack.getCount() < stack.getMaxStackSize()) {
                            allPotatoes = false;
                            break;
                        }
                    }
                    if (allPotatoes) {
                        AdvancementHelper.potatoWar(serverPlayer);
                    }
                }
                StringBuilder msg = new StringBuilder("<" + player.getDisplayName().getString());
                boolean first = true;
                if (swapped) {
                    msg.append(" swapped the items in your hands");
                    first = false;
                } else if (handChanged) {
                    msg.append(" swapped your held items to ").append(formatHands(finalMain, finalOff));
                    first = false;
                }
                if (!added.isEmpty()) {
                    if (!first) { msg.append(", and"); }
                    msg.append(" gave you ").append(joinCounts(added));
                    first = false;
                }
                if (!removed.isEmpty()) {
                    if (!first) { msg.append(", and"); }
                    msg.append(verb).append(joinCounts(removed));
                    first = false;
                }
                if (!disarmedToInventory.isEmpty()) {
                    if (!first) { msg.append(", and"); }
                    msg.append(" disarmed you and placed ").append(String.join(", ", disarmedToInventory)).append(" into your inventory");
                    first = false;
                }
                if (!disarmedTaken.isEmpty()) {
                    if (!first) { msg.append(", and"); }
                    msg.append(" disarmed you and ").append(verbBase).append(" ").append(String.join(", ", disarmedTaken));
                }
                msg.append(">");
                ServerPackets.generate_chat("N/A", chatData, serverPlayer, mob, msg.toString(), true);
            }
        }
    }

    private void syncHandSlots() {
        handleHandSlot(mainHandSlot, mob.getMainHandItem());
        handleHandSlot(offHandSlot, mob.getOffhandItem());
    }

    private void handleHandSlot(int slotIndex, ItemStack handStack) {
        if (slotIndex >= mobInvSize) {
            return;
        }
        ItemStack current = inventory.getItem(slotIndex);
        if (handStack.isEmpty()) {
            if (!current.isEmpty()) {
                moveOrDrop(slotIndex, current);
            }
            inventory.setItem(slotIndex, ItemStack.EMPTY);
        } else {
            if (!current.isEmpty() && !ItemStack.isSameItem(current, handStack)) {
                moveOrDrop(slotIndex, current);
            }
            inventory.setItem(slotIndex, handStack.copy());
        }
    }

    private void moveOrDrop(int fromSlot, ItemStack stack) {
        int empty = findEmptySlot(fromSlot);
        inventory.setItem(fromSlot, ItemStack.EMPTY);
        if (empty >= 0) {
            inventory.setItem(empty, stack);
        } else if (mob.level() instanceof ServerLevel serverLevel) {
            ItemEntity item = new ItemEntity(serverLevel, mob.getX(), mob.getY(), mob.getZ(), stack);
            item.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(item);
        }
    }

    private int findEmptySlot(int skip) {
        for (int i = 0; i < mobInvSize; i++) {
            if (i == mainHandSlot || i == offHandSlot || i == skip) {
                continue;
            }
            if (inventory.getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private void collectDisarmed(ItemStack initial, ItemStack finalMain, ItemStack finalOff,
                                 List<String> toInventory, List<String> taken, Map<Item, Integer> removed) {
        if (initial.isEmpty()) {
            return;
        }
        if (ItemStack.isSameItem(initial, finalMain) || ItemStack.isSameItem(initial, finalOff)) {
            return;
        }
        String name = initial.getHoverName().getString();
        for (int i = 0; i < mobInvSize; i++) {
            if (i == mainHandSlot || i == offHandSlot) {
                continue;
            }
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItem(stack, initial)) {
                toInventory.add(name);
                return;
            }
        }
        taken.add(name);
        removed.computeIfPresent(initial.getItem(), (item, count) -> {
            int remaining = count - initial.getCount();
            return remaining > 0 ? remaining : null;
        });
    }

    private static String joinCounts(Map<Item, Integer> map) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : map.entrySet()) {
            parts.add(entry.getValue() + " " + new ItemStack(entry.getKey()).getHoverName().getString());
        }
        return String.join(", ", parts);
    }

    private static String formatHands(ItemStack main, ItemStack off) {
        String mainName = main.isEmpty() ? "" : main.getHoverName().getString();
        String offName = off.isEmpty() ? "" : off.getHoverName().getString();
        if (!mainName.isEmpty() && !offName.isEmpty()) {
            return mainName + " and " + offName;
        }
        return !mainName.isEmpty() ? mainName : offName;
    }

    private static boolean sameItem(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItem(a, b);
    }

    private static boolean handsSwapped(ItemStack initMain, ItemStack initOff, ItemStack finalMain, ItemStack finalOff) {
        return sameItem(initMain, finalOff) && sameItem(initOff, finalMain) &&
               !(sameItem(initMain, finalMain) && sameItem(initOff, finalOff));
    }

    private static class HandSlot extends Slot {
        private final Mob mob;
        private final EquipmentSlot equipmentSlot;

        HandSlot(Container container, int index, int x, int y, Mob mob, EquipmentSlot equipmentSlot) {
            super(container, index, x, y);
            this.mob = mob;
            this.equipmentSlot = equipmentSlot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.isEmpty() || mob.canHoldItem(stack);
        }

        @Override
        public void set(ItemStack stack) {
            super.set(stack);
            mob.setItemSlot(equipmentSlot, stack.copy());
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            mob.setItemSlot(equipmentSlot, this.getItem().copy());
        }
    }

    private static class LockedSlot extends Slot {
        LockedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
