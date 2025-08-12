// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.inventory;

import com.owlmaddie.utils.TextureLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for mob inventories.
 */
public class MobInventoryScreen extends AbstractContainerScreen<MobInventoryMenu> {
    private static final TextureLoader textures = new TextureLoader();
    private static final ResourceLocation INVENTORY_TEXTURE = textures.GetUI("inventory");
    private float xMouse;
    private float yMouse;

    public MobInventoryScreen(MobInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(INVENTORY_TEXTURE, k, l, 0, 0, this.imageWidth, this.imageHeight);
        Mob mob = this.menu.getMob();
        if (mob != null) {
            float relX = (float)(k + 78) - this.xMouse;
            float relY = (float)(l + 70) - this.yMouse;
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, k + 78, l + 70, 20, relX, relY, mob);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        this.xMouse = (float)i;
        this.yMouse = (float)j;
        super.render(guiGraphics, i, j, f);
        this.renderTooltip(guiGraphics, i, j);
    }
}
