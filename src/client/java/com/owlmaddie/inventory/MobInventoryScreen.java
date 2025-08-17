// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.inventory;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.utils.TextureLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Client screen for mob inventories.
 */
public class MobInventoryScreen extends AbstractContainerScreen<MobInventoryMenu> {
    private static final TextureLoader textures = new TextureLoader();
    private static final ResourceLocation FRIEND_TEXTURE = textures.GetUI("inventory");
    private static final ResourceLocation ENEMY_TEXTURE = textures.GetUI("inventory-enemy");
    private float xMouse;
    private float yMouse;

    public MobInventoryScreen(MobInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY += 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;
        Mob mob = this.menu.getMob();
        ResourceLocation background = FRIEND_TEXTURE;
        if (mob != null && this.minecraft.player != null) {
            int friendship = ChatDataManager.getClientInstance()
                    .getOrCreateChatData(mob.getStringUUID())
                    .getPlayerData(this.minecraft.player.getDisplayName().getString())
                    .friendship;
            if (friendship <= 0) {
                background = ENEMY_TEXTURE;
            }
        }
        guiGraphics.blit(background, k, l, 0, 0, this.imageWidth, this.imageHeight);
        if (mob != null) {
            int left = k + 13 + 8;
            int top = l + 18 + 12;
            int width = 52 - 8 - 8;
            int height = 52 - 12 - 8;
            float widthScale = (float) width / mob.getBbWidth();
            float heightScale = (float) height / mob.getBbHeight();
            float scale = Math.min(widthScale, heightScale);
            int xCenter = left + width / 2;
            int yBottom = top + height;
            float relX = (float) xCenter - this.xMouse;
            float relY = (float) yBottom - this.yMouse;
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, xCenter, yBottom , (int) scale, relX, relY, mob);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        this.xMouse = (float)i;
        this.yMouse = (float)j;
        super.render(guiGraphics, i, j, f);
        if (this.minecraft.player != null) {
            for (Slot slot : this.menu.slots) {
                if (!slot.mayPickup(this.minecraft.player)) {
                    int x = this.leftPos + slot.x;
                    int y = this.topPos + slot.y;
                    guiGraphics.fill(x, y, x + 16, y + 16, 0x90000000);
                }
            }
        }
        this.renderTooltip(guiGraphics, i, j);
    }
}
