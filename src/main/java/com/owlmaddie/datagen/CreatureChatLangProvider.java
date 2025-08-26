// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.datagen;

import com.owlmaddie.chat.Advancements;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.i18n.CCText;
import com.owlmaddie.utils.Randomizer;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Generates the English fallback language file.
 */
public class CreatureChatLangProvider extends FabricLanguageProvider {
    public CreatureChatLangProvider(FabricDataOutput output) {
        super(output, "creaturechat");
    }

    @Override
    public void generateTranslations(TranslationBuilder builder) {
        Set<String> added = new HashSet<>();
        Stream.of(
                Randomizer.allErrorText(),
                CCText.UI_TEXT.stream(),
                CCText.CONFIG_TEXT.stream(),
                EntityChatData.ERROR_MISC.stream(),
                EntityChatData.ERROR_SOLUTIONS.stream(),
                Advancements.allText()
        ).flatMap(s -> s).forEach(tr -> {
            if (added.add(tr.key())) {
                builder.add(tr.key(), tr.en());
            }
        });
    }

    @Override
    public String getName() {
        return "CreatureChat Lang";
    }
}
