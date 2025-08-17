// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.utils;

import com.owlmaddie.ui.ClickHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Helper for UseItemCallback, forwarding to the shared shouldCancelAction logic.
 */
public final class UseItemCallbackHelper {
    private UseItemCallbackHelper() {}

    /**
     * Fabric 1.20.x–1.21.1 handler using InteractionResultHolder<ItemStack>.
     */
    public static InteractionResultHolder<ItemStack> handleUseItemAction(
            Player player,
            Level world,
            InteractionHand hand
    ) {
        if (shouldCancelAction(world)) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    /**
     * Mirrors whatever logic you had in ClickHandler.shouldCancelAction.
     * You’ll need to make that method public in ClickHandler so you can call it here.
     */
    private static boolean shouldCancelAction(Level world) {
        return ClickHandler.shouldCancelAction(world);
    }
}
