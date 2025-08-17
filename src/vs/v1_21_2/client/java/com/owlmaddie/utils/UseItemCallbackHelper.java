// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.utils;

import com.owlmaddie.ui.ClickHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Helper for UseItemCallback, forwarding to the shared shouldCancelAction logic.
 */
public final class UseItemCallbackHelper {
    private UseItemCallbackHelper() {}

    /**
     * Fabric 1.21.2+ handler returning InteractionResult.
     */
    public static InteractionResult handleUseItemAction(
            Player player,
            Level world,
            InteractionHand hand
    ) {
        return shouldCancelAction(world) ? InteractionResult.FAIL : InteractionResult.PASS;
    }

    private static boolean shouldCancelAction(Level world) {
        return ClickHandler.shouldCancelAction(world);
    }
}
