// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * The {@code TextureLoader} class registers and returns texture identifiers for resources
 * contained for this mod. UI and Entity icons. Missing textures are logged once.
 * Modified for 1.21.5.
 */
public class TextureLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("creaturepals");
    private static final Set<String> missing = new HashSet<>();
    public static GpuTexture lastTexture = null;
    public static ResourceLocation lastTextureId = null;

    public TextureLoader() {}

    /**
     * Load and bind a UI texture (assets/creaturechat/textures/ui/{name}.png).
     * Returns the Identifier if found, or null if missing.
     */
    public ResourceLocation GetUI(String name) {
        return load(new ResourceLocation("creaturepals", "textures/ui/" + name + ".png"));
    }

    /**
     * Load and bind an entity texture (assets/creaturechat/{texturePath}).
     * Returns the Identifier if found, or falls back to not_found.png.
     */
    public ResourceLocation GetEntity(String texturePath) {
        ResourceLocation id = new ResourceLocation("creaturepals", texturePath);
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        if (rm.getResource(id).isPresent()) {
            return load(id);
        } else {
            if (missing.add(texturePath)) {
                LOGGER.info("Missing texture: {}", texturePath);
            }
            return load(new ResourceLocation("creaturepals", "textures/entity/not_found.png"));
        }
    }

    private ResourceLocation load(ResourceLocation id) {
        Minecraft client = Minecraft.getInstance();
        ResourceManager rm = client.getResourceManager();
        Optional<Resource> res = rm.getResource(id);

        if (res.isEmpty()) {
            // first time missing: log once
            if (missing.add(id.toString())) {
                LOGGER.info("Missing texture: {}", id);
            }
            return null;
        }
        return id;
    }

    /**
     * Bind any already-registered texture at the given unit,
     * or register+bind it if it's not yet in the TextureManager.
     */
    public static void bind(int unit, ResourceLocation id) {
        Minecraft client = Minecraft.getInstance();
        TextureManager tm = client.getTextureManager();
        AbstractTexture tex = tm.getTexture(id);
        if (tex == null) {
            // register a ResourceTexture so the manager will load it from assets
            tex = new SimpleTexture(id);
            tm.register(id, tex);
        }

        // Store last GpuTexture
        GpuTexture gpu = tex.getTexture();
        lastTexture = gpu;
        lastTextureId = id;

        // Set Texture
        RenderSystem.setShaderTexture(unit, gpu);
    }
}
