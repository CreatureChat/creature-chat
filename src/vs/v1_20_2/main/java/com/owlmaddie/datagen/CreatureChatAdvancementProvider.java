// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.owlmaddie.chat.Advancements;
import com.owlmaddie.utils.AdvancementBackgroundHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CreatureChatAdvancementProvider extends FabricAdvancementProvider {
    public CreatureChatAdvancementProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateAdvancement(Consumer<AdvancementHolder> out) {
        Map<Advancements, AdvancementHolder> built = new HashMap<>();
        for (Advancements adv : Advancements.values()) {
            build(out, adv, built);
        }
    }

    private static AdvancementHolder build(Consumer<AdvancementHolder> out,
                                           Advancements adv,
                                           Map<Advancements, AdvancementHolder> built) {
        if (built.containsKey(adv)) return built.get(adv);

        AdvancementHolder parent = adv.parent == null ? null : build(out, adv.parent, built);

        ResourceLocation bg = AdvancementBackgroundHelper.prependTextures(adv.background);

        DisplayInfo display = new DisplayInfo(
                new ItemStack(adv.icon),
                adv.title.comp(),
                adv.description.comp(),
                bg,
                toFrameType(adv.type),
                true,
                true,
                adv.hidden
        );

        Criterion<?> impossible = CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance());

        AdvancementRewards rewards = adv.rewardXp > 0
                ? AdvancementRewards.Builder.experience(adv.rewardXp).build()
                : AdvancementRewards.EMPTY;

        Advancement.Builder b = Advancement.Builder.advancement()
                .display(display)
                .rewards(rewards)
                .addCriterion("triggered", impossible);

        if (parent != null) {
            b.parent(parent);
        }

        AdvancementHolder saved = b.save(out, adv.id.toString());
        built.put(adv, saved);
        return saved;
    }

    private static FrameType toFrameType(Advancements.Type type) {
        return switch (type) {
            case TASK -> FrameType.TASK;
            case GOAL -> FrameType.GOAL;
            case CHALLENGE -> FrameType.CHALLENGE;
        };
    }

    @Override
    public String getName() {
        return "CreatureChat Advancements (mojmap 1.20.2+)";
    }
}
