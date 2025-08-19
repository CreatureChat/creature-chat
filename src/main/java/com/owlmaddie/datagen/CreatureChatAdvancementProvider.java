// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.owlmaddie.chat.Advancements;

import java.util.function.Consumer;

public class CreatureChatAdvancementProvider extends FabricAdvancementProvider {
    public CreatureChatAdvancementProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateAdvancement(Consumer<Advancement> out) {
        Advancement root = make(out, null, Advancements.ROOT);
        for (Advancements adv : Advancements.values()) {
            if (adv == Advancements.ROOT) continue;
            make(out, root, adv);
        }
    }

    private static Advancement make(Consumer<Advancement> out,
                                    Advancement parentOrNull,
                                    Advancements adv) {

        DisplayInfo display = new DisplayInfo(
                new ItemStack(Items.PAPER),
                Component.literal(adv.title),
                Component.literal(adv.description),
                adv.background,
                toFrameType(adv.type),
                true,
                true,
                false
        );

        Advancement.Builder b = Advancement.Builder.advancement()
                .display(display)
                .rewards(AdvancementRewards.EMPTY)
                .addCriterion("triggered", new ImpossibleTrigger.TriggerInstance());

        if (parentOrNull != null) {
            b.parent(parentOrNull);
        }

        return b.save(out, adv.id.toString());
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
        return "CreatureChat Advancements";
    }
}
