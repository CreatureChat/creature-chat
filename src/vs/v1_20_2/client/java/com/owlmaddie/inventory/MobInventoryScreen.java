// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for mob inventories.
 */
public class MobInventoryScreen extends MobInventoryScreenBase {

    public MobInventoryScreen(MobInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void blitBackground(GuiGraphics guiGraphics, net.minecraft.resources.ResourceLocation background, int x, int y) {
        guiGraphics.blit(background, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderMob(GuiGraphics guiGraphics, Mob mob, int left, int top, int right, int bottom, int scale, float yOffset) {
        float cx = (left + right) * 0.5f;
        float cy = (top + bottom) * 0.5f;
        int L = (int)(cx - INF);
        int T = (int)(cy - INF);
        int R = (int)(cx + INF);
        int B = (int)(cy + INF);
        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, L, T, R, B, scale, yOffset, this.xMouse, this.yMouse, mob);
    }
}
