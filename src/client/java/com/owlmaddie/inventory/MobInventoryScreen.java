// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for mob inventories.
 */
public class MobInventoryScreen extends AbstractContainerScreen<MobInventoryMenu> {
    private static final ResourceLocation CHEST_SLOTS_SPRITE = ResourceLocation.withDefaultNamespace("container/horse/chest_slots");
    private static final ResourceLocation HORSE_INVENTORY_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/horse.png");
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
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, HORSE_INVENTORY_LOCATION, k, l, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, CHEST_SLOTS_SPRITE, 90, 54, 0, 0, k + 79, l + 17, 90, 54);
        Mob mob = this.menu.getMob();
        if (mob != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, k + 26, l + 18, k + 78, l + 70, 17, 0.25F, this.xMouse, this.yMouse, mob);
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
