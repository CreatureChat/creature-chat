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
            int boxL = k + 13, boxT = l + 18, boxR = boxL + 52, boxB = boxT + 52;
            int left = boxL + 8, top = boxT + 12, right = boxR - 8, bottom = boxB - 8;
            int w = right - left, h = bottom - top;

            int ROT_PAD = 2;
            float sx = (float)(w - ROT_PAD * 2) / mob.getBbWidth();
            float sy = (float)(h - ROT_PAD * 2) / mob.getBbHeight();
            int scale = (int)Math.floor(Math.min(sx, sy));

            float yOffset = -2f / Math.max(1, scale); // 12 vs 8 margins

            float cx = (left + right) * 0.5f, cy = (top + bottom) * 0.5f;
            int INF_W = 4096, INF_H = 4096; // huge rect, same center -> no clipping
            int L = (int)(cx - INF_W), T = (int)(cy - INF_H), R = (int)(cx + INF_W), B = (int)(cy + INF_H);

            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, L, T, R, B, scale, yOffset, this.xMouse, this.yMouse, mob);
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
