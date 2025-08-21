// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.datagen;

import com.owlmaddie.items.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

/**
 * Generates the English language translations for the mod.
 */
public class CreatureChatEnglishLangProvider extends FabricLanguageProvider {
    public CreatureChatEnglishLangProvider(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registries) {
        super(dataOutput, "en_us", registries);
    }

    @Override
    public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder builder) {
        builder.add(ModItems.BOOK, "Creature Book");
    }
}
