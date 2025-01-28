package com.owlmaddie.ui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class TagEditorScreenHandler extends ScreenHandler {
    private final Inventory inventory;

    public TagEditorScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ModScreenHandlers.TAG_EDITOR_SCREEN_HANDLER, syncId);
        this.inventory = new SimpleInventory(2); // 2 custom slots: input and output

        // Input slot
        this.addSlot(new Slot(this.inventory, 0, 80, 20));

        // Output slot
        this.addSlot(new Slot(this.inventory, 1, 120, 20) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false; // Prevent inserting items into the output slot
            }
        });

        // Player inventory slots
        int playerInventoryStartY = 84;
        int hotbarStartY = 142;

        // Main inventory slots (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInventoryStartY + row * 18));
            }
        }

        // Hotbar slots (1 row of 9)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarStartY));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true; // Allow the player to use the screen
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            itemStack = originalStack.copy();

            if (index < this.inventory.size()) {
                // If the clicked slot is in the custom inventory (input/output), move to the player's inventory
                if (!this.moveItemStackTo(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // If the clicked slot is in the player's inventory, move to the input slot
                if (!this.moveItemStackTo(originalStack, 0, 1, false)) { // 0 to 1 = input slot range
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return itemStack;
    }

    private boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean moved = false;
        int i = reverseDirection ? endIndex - 1 : startIndex;

        while (stack.getCount() > 0 && (!reverseDirection && i < endIndex || reverseDirection && i >= startIndex)) {
            Slot slot = this.slots.get(i);
            ItemStack currentStack = slot.getStack();

            if (slot.canInsert(stack)) {
                int maxInsert = Math.min(slot.getMaxItemCount(stack), stack.getMaxCount());
                int amountToMove = Math.min(maxInsert - currentStack.getCount(), stack.getCount());

                if (amountToMove > 0) {
                    if (currentStack.isEmpty()) {
                        slot.setStack(stack.split(amountToMove));
                    } else if (ItemStack.canCombine(currentStack, stack)) {
                        currentStack.increment(amountToMove);
                        stack.decrement(amountToMove);
                    }

                    slot.markDirty();
                    moved = true;
                }
            }

            i += reverseDirection ? -1 : 1;
        }

        return moved;
    }
}
