// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.datagen;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Generates item model JSON files.
 */
public class CreatureChatModelProvider implements DataProvider {
    private final FabricDataOutput output;

    public CreatureChatModelProvider(FabricDataOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", "creaturechat:item/book");
        root.add("textures", textures);

        Path path = this.output.getOutputFolder().resolve("assets/creaturechat/models/item/book.json");
        return DataProvider.saveStable(cachedOutput, root, path);
    }

    @Override
    public String getName() {
        return "CreatureChatModelProvider";
    }
}

