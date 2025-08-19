// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class CreatureChatAdvancementProvider extends FabricAdvancementProvider {
    private static final String MODID = "creaturechat";

    public CreatureChatAdvancementProvider(FabricDataOutput output,
                                           CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generateAdvancement(HolderLookup.Provider lookup, Consumer<AdvancementHolder> out) {
        // Optional visible root tab
        AdvancementHolder root = make(out, null, "root",
                "CreatureChat", "Talk to mobs. Make friends. Start drama.",
                AdvancementType.TASK,
                new ResourceLocation("minecraft", "textures/gui/advancements/backgrounds/adventure.png"));

        make(out, root, "ice_breaker",       "Ice Breaker",            "So... weather, huh?",                            AdvancementType.TASK,      null);
        make(out, root, "friendly_creature", "Friendly Creature",      "Made a new pal!",                                AdvancementType.TASK,      null);
        make(out, root, "beastie_bestie",    "Beastie Bestie",         "They would share their last potato with you.",   AdvancementType.GOAL,      null);
        make(out, root, "arch_nemesis",      "Arch Nemesis",           "They hiss in your general direction.",           AdvancementType.CHALLENGE, null);
        make(out, root, "love_hate",         "Love Hate Relationship", "Best friend, worst enemy... same creature.",     AdvancementType.CHALLENGE, null);
        make(out, root, "drama_llama",       "Drama Llama",            "It’s not complicated, it’s chaotic.",            AdvancementType.GOAL,      null);
        make(out, root, "smooth_talker",     "Smooth Talker",          "All persuasion, zero inventory.",                AdvancementType.GOAL,      null);
        make(out, root, "no_hard_feelings",  "No Hard Feelings",       "Ouch. Sorry. Friends?",                          AdvancementType.TASK,      null);
        make(out, root, "squad_goals",       "Squad Goals",            "Group selfie energy.",                           AdvancementType.GOAL,      null);
        make(out, root, "borrowed_forever",  "Borrowed Forever",       "Technically not stealing.",                      AdvancementType.CHALLENGE, null);
    }

    private static AdvancementHolder make(Consumer<AdvancementHolder> out,
                                          AdvancementHolder parentOrNull,
                                          String path,
                                          String title,
                                          String description,
                                          AdvancementType type,
                                          ResourceLocation backgroundTextureOrNull) {

        Optional<ClientAsset> bg = backgroundTextureOrNull == null
                ? Optional.empty()
                : Optional.of(new ClientAsset(backgroundTextureOrNull));

        DisplayInfo display = new DisplayInfo(
                new ItemStack(Items.PAPER),
                Component.literal(title),
                Component.literal(description),
                bg,
                type,
                true,   // show_toast
                true,   // announce_to_chat
                false   // hidden
        );

        Criterion<?> impossible = CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance());

        Advancement.Builder b = Advancement.Builder.advancement()
                .display(display)
                .rewards(AdvancementRewards.EMPTY)
                .addCriterion("triggered", impossible);

        if (parentOrNull != null) {
            b.parent(parentOrNull);
        }

        AdvancementHolder holder = b.save(out, new ResourceLocation(MODID, path).toString());
        return holder;
    }

    @Override
    public String getName() {
        return "CreatureChat Advancements (mojmap 1.21.7)";
    }
}
