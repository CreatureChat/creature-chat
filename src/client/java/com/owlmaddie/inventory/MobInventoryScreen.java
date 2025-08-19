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
        int xCenter = (left + right) / 2;
        int yBottom = bottom;
        float relX = (float) xCenter - this.xMouse;
        float relY = (float) yBottom - this.yMouse;
        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, xCenter, yBottom, scale, relX, relY, mob);
    }
}
