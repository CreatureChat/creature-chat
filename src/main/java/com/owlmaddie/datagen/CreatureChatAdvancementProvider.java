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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

public class CreatureChatAdvancementProvider extends FabricAdvancementProvider {
    private static final String MODID = "creaturechat";

    public CreatureChatAdvancementProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateAdvancement(Consumer<Advancement> out) {
        Advancement root = make(out, null, "root",
                "CreatureChat", "Talk to mobs. Make friends. Start drama.",
                FrameType.TASK,
                new ResourceLocation("minecraft", "textures/gui/advancements/backgrounds/adventure.png"));

        make(out, root, "ice_breaker",       "Ice Breaker",            "So... weather, huh?",                            FrameType.TASK,      null);
        make(out, root, "friendly_creature", "Friendly Creature",      "Made a new pal!",                                FrameType.TASK,      null);
        make(out, root, "beastie_bestie",    "Beastie Bestie",         "They would share their last potato with you.",   FrameType.GOAL,      null);
        make(out, root, "arch_nemesis",      "Arch Nemesis",           "They hiss in your general direction.",           FrameType.CHALLENGE, null);
        make(out, root, "love_hate",         "Love Hate Relationship", "Best friend, worst enemy... same creature.",     FrameType.CHALLENGE, null);
        make(out, root, "drama_llama",       "Drama Llama",            "It’s not complicated, it’s chaotic.",            FrameType.GOAL,      null);
        make(out, root, "smooth_talker",     "Smooth Talker",          "All persuasion, zero inventory.",                FrameType.GOAL,      null);
        make(out, root, "no_hard_feelings",  "No Hard Feelings",       "Ouch. Sorry. Friends?",                          FrameType.TASK,      null);
        make(out, root, "squad_goals",       "Squad Goals",            "Group selfie energy.",                           FrameType.GOAL,      null);
        make(out, root, "borrowed_forever",  "Borrowed Forever",       "Technically not stealing.",                      FrameType.CHALLENGE, null);
    }

    private static Advancement make(Consumer<Advancement> out,
                                    Advancement parentOrNull,
                                    String path,
                                    String title,
                                    String description,
                                    FrameType type,
                                    ResourceLocation backgroundTextureOrNull) {

        DisplayInfo display = new DisplayInfo(
                new ItemStack(Items.PAPER),
                Component.literal(title),
                Component.literal(description),
                backgroundTextureOrNull,
                type,
                true,   // show_toast
                true,   // announce_to_chat
                false   // hidden
        );

        Advancement.Builder b = Advancement.Builder.advancement()
                .display(display)
                .rewards(AdvancementRewards.EMPTY)
                .addCriterion("triggered", new ImpossibleTrigger.TriggerInstance());

        if (parentOrNull != null) {
            b.parent(parentOrNull);
        }

        Advancement adv = b.save(out, new ResourceLocation(MODID, path).toString());
        return adv;
    }

    @Override
    public String getName() {
        return "CreatureChat Advancements";
    }
}
