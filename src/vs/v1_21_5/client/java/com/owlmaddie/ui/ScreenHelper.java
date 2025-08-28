// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.ui;

import com.owlmaddie.render.BlendHelper;
import com.owlmaddie.utils.TextureLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Provides a Screen class which renders a chat-background, and can be modified
 * for different versions of Minecraft (as API changes happen). Modified for Minecraft 1.21.2.
 */
public abstract class ScreenHelper extends Screen {
    protected int BG_WIDTH, BG_HEIGHT, bgX, bgY, TITLE_OFFSET;
    private boolean skipNextBackground = false;
    protected static final TextureLoader textures = new TextureLoader();

    protected ScreenHelper(Component title) {
        super(title);
    }

    /**
     * Subclass must return its TextFieldWidget instance here
     */
    protected abstract EditBox getTextField();

    /**
     * Subclass must return its label Text here
     */
    protected abstract Component getLabelText();

    /** Subclass must supply the UI texture id for its background */
    protected abstract String getBackgroundTextureId();

    /** Hook for subclasses to render additional content above the background */
    protected void renderContent(GuiGraphics context, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Draw the vanilla gradient once
        super.renderBackground(context, mouseX, mouseY, delta);

        // Draw the background texture
        ResourceLocation bgTex = textures.GetUI(getBackgroundTextureId());
        if (bgTex != null) {
            BlendHelper.enableBlend();
            BlendHelper.defaultBlendFunc();
            context.blit(
                    RenderType::guiTextured,
                    bgTex,
                    bgX, bgY,
                    0f, 0f,
                    BG_WIDTH, BG_HEIGHT,
                    BG_WIDTH, BG_HEIGHT
            );
            BlendHelper.disableBlend();
        }

        // Allow subclass content between background and widgets
        renderContent(context, mouseX, mouseY, delta);

        // Render children/widgets but suppress their background call
        skipNextBackground = true;
        super.render(context, mouseX, mouseY, delta);
        skipNextBackground = false;

        // Draw the "Enter your message:" label
        EditBox tf = getTextField();
        Component           lbl = getLabelText();
        int lw = font.width(lbl);
        int lx = (this.width - lw) / 2;
        int ly = tf.getY() - TITLE_OFFSET;
        context.drawString(font, lbl, lx, ly, 0xFFFFFF);
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (!skipNextBackground) {
            super.renderBackground(context, mouseX, mouseY, delta);
        }
    }
}
