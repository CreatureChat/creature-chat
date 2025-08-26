package com.owlmaddie.datagen;

import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.utils.Randomizer;
import com.owlmaddie.i18n.CCText;
import com.owlmaddie.chat.Advancements;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Generates the English fallback language file.
 */
public class CreatureChatLangProvider extends FabricLanguageProvider {
    public CreatureChatLangProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder builder) {
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
