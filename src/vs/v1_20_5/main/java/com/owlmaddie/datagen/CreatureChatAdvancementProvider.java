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
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.owlmaddie.chat.Advancements;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class CreatureChatAdvancementProvider extends FabricAdvancementProvider {
    public CreatureChatAdvancementProvider(FabricDataOutput output,
                                           CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generateAdvancement(HolderLookup.Provider lookup, Consumer<AdvancementHolder> out) {
        AdvancementHolder root = make(out, null, Advancements.ROOT);
        for (Advancements adv : Advancements.values()) {
            if (adv == Advancements.ROOT) continue;
            make(out, root, adv);
        }
    }

    private static AdvancementHolder make(Consumer<AdvancementHolder> out,
                                          AdvancementHolder parentOrNull,
                                          Advancements adv) {

        Optional<ResourceLocation> bg = adv.background == null
                ? Optional.empty()
                : Optional.of(adv.background);

        DisplayInfo display = new DisplayInfo(
                new ItemStack(Items.PAPER),
                Component.literal(adv.title),
                Component.literal(adv.description),
                bg,
                toAdvancementType(adv.type),
                true,
                true,
                false
        );

        Criterion<?> impossible = CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance());

        Advancement.Builder b = Advancement.Builder.advancement()
                .display(display)
                .rewards(AdvancementRewards.EMPTY)
                .addCriterion("triggered", impossible);

        if (parentOrNull != null) {
            b.parent(parentOrNull);
        }

        return b.save(out, adv.id.toString());
    }

    private static AdvancementType toAdvancementType(Advancements.Type type) {
        return switch (type) {
            case TASK -> AdvancementType.TASK;
            case GOAL -> AdvancementType.GOAL;
            case CHALLENGE -> AdvancementType.CHALLENGE;
        };
    }

    @Override
    public String getName() {
        return "CreatureChat Advancements (mojmap 1.20.5)";
    }
}
