// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataProvider;

/**
 * Registers all data generation providers for the mod.
 */
public class CreatureChatDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(CreatureChatLootTableProvider::new);
        pack.addProvider(CreatureChatAdvancementProvider::new);
        pack.addProvider(CreatureChatEnglishLangProvider::new);

        // Load the client-side model provider reflectively so the common
        // sources don't depend on client-only classes at compile time.
        pack.addProvider((FabricDataOutput out) -> createModelProvider(out));
        pack.addProvider(CreatureChatEnglishLangProvider::new);
    }

    private DataProvider createModelProvider(FabricDataOutput output) {
        try {
            Class<?> clazz = Class.forName("com.owlmaddie.datagen.CreatureChatModelProvider");
            return (DataProvider) clazz
                    .getConstructor(FabricDataOutput.class)
                    .newInstance(output);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to load model provider", e);
        }
    }
}

