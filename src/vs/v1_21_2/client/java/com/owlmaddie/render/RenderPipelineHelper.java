// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Helper to blit GUI textures across Minecraft versions.
 * Modified for Minecraft 1.21.2 where a RenderType supplier is required.
 */
public final class RenderPipelineHelper {
    private RenderPipelineHelper() {}

    public static void blitGuiTexture(
            GuiGraphics ctx,
            ResourceLocation tex,
            int x, int y,
            int u, int v,
            int width, int height,
            int texWidth, int texHeight
    ) {
        ctx.blit(RenderType::guiTextured, tex, x, y, (float) u, (float) v, width, height, texWidth, texHeight);
    }
}
