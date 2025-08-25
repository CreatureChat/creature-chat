// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.inventory;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.utils.TextureLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Shared logic for mob inventory screens.
 */
public abstract class MobInventoryScreenBase extends AbstractContainerScreen<MobInventoryMenu> {
    protected static final TextureLoader textures = new TextureLoader();
    protected static final ResourceLocation FRIEND_TEXTURE = textures.GetUI("inventory");
    protected static final ResourceLocation ENEMY_TEXTURE = textures.GetUI("inventory-enemy");
    protected static final int INF = 1024;
    protected float xMouse;
    protected float yMouse;

    protected MobInventoryScreenBase(MobInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY += 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.xMouse = (float) mouseX;
        this.yMouse = (float) mouseY;
        super.render(guiGraphics, mouseX, mouseY, delta);
        if (this.minecraft.player != null) {
            for (Slot slot : this.menu.slots) {
                if (!slot.mayPickup(this.minecraft.player)) {
                    int x = this.leftPos + slot.x;
                    int y = this.topPos + slot.y;
                    guiGraphics.fill(x, y, x + 16, y + 16, 0x90000000);
                }
            }
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
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
        this.blitBackground(guiGraphics, background, k, l);
        if (mob != null) {
            int boxL = k + 13, boxT = l + 18, boxR = boxL + 52, boxB = boxT + 52;
            int left = boxL + 8, top = boxT + 12, right = boxR - 8, bottom = boxB - 8;
            int w = right - left, h = bottom - top;
            int ROT_PAD = 2;
            float sx = (float)(w - ROT_PAD * 2) / mob.getBbWidth();
            float sy = (float)(h - ROT_PAD * 2) / mob.getBbHeight();
            int scale = (int)Math.floor(Math.min(sx, sy));
            float yOffset = -2f / Math.max(1, scale);
            this.renderMob(guiGraphics, mob, left, top, right, bottom, scale, yOffset);
        }
    }

    protected abstract void blitBackground(GuiGraphics guiGraphics, ResourceLocation background, int x, int y);

    protected abstract void renderMob(GuiGraphics guiGraphics, Mob mob, int left, int top, int right, int bottom, int scale, float yOffset);
}

