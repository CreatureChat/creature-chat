// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ButtonHelper {
  private static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");

  /**
   * Create an image‐only button that swaps between normal/hover textures.
   * Version‐specific subclasses just override the rendering hook.
   */
  public static Button createImageButton(
      int x, int y,
      int width, int height,
      ResourceLocation normalTex,
      ResourceLocation hoverTex,
      Button.OnPress onPress,
      Button.CreateNarration narrate
  ) {
    final boolean missing = normalTex == null || hoverTex == null;
    if (missing) {
      LOGGER.warn("ButtonHelper: missing texture for button at ({}, {})", x, y);
    }

    return new Button(x, y, width, height, Component.empty(), onPress, narrate) {
      @Override
      public void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        if (missing) {
          // fall back to default rendering when textures are unavailable
          super.renderWidget(ctx, mouseX, mouseY, delta);
          return;
        }
        ResourceLocation tex = isHovered() ? hoverTex : normalTex;
        ctx.blit(tex, getX(), getY(), 0, 0, width, height, width, height);
      }
    };
  }
}
