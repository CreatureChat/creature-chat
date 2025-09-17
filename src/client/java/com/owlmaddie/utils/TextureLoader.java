// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * The {@code TextureLoader} class registers and returns texture identifiers for resources
 * contained for this mod. UI and Entity icons. Missing textures are logged once.
 * Pre-1.21.5: RenderSystem.setShaderTexture(int, Identifier) is used.
 */
public class TextureLoader {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturepals");
    private static final Set<String> missing = new HashSet<>();

    public TextureLoader() {}

    public ResourceLocation GetUI(String name) {
        String texturePath = "textures/ui/" + name + ".png";
        ResourceLocation textureId = new ResourceLocation("creaturepals", texturePath);
        Optional<Resource> resource = Minecraft
                .getInstance()
                .getResourceManager()
                .getResource(textureId);

        if (resource.isPresent()) {
            // replace bindTexture(...) with RenderSystem
            RenderSystem.setShaderTexture(0, textureId);
            return textureId;
        } else {
            logMissingTextureOnce(texturePath);
            return null;
        }
    }

    public ResourceLocation GetEntity(String texturePath) {
        ResourceLocation textureId = new ResourceLocation("creaturepals", texturePath);
        Optional<Resource> resource = Minecraft
                .getInstance()
                .getResourceManager()
                .getResource(textureId);

        if (resource.isPresent()) {
            RenderSystem.setShaderTexture(0, textureId);
            return textureId;
        } else {
            ResourceLocation notFoundId = new ResourceLocation("creaturepals", "textures/entity/not_found.png");
            RenderSystem.setShaderTexture(0, notFoundId);
            logMissingTextureOnce(texturePath);
            return notFoundId;
        }
    }

    private void logMissingTextureOnce(String texturePath) {
        if (missing.add(texturePath)) {
            LOGGER.info("{} was not found", texturePath);
        }
    }

    /**
     * Binds the given Identifier to the specified texture unit.
     */
    public static void bind(int unit, ResourceLocation textureId) {
        RenderSystem.setShaderTexture(unit, textureId);
    }
}