// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CreatureChatAdvancementProvider implements DataProvider {
    private final FabricDataOutput output;

    public CreatureChatAdvancementProvider(FabricDataOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        futures.add(save(cache, "ice_breaker", advancement("Ice Breaker", "So… weather, huh?", "task")));
        futures.add(save(cache, "friendly_creature", advancement("Friendly Creature", "Made a new pal!", "task")));
        futures.add(save(cache, "beastie_bestie", advancement("Beastie Bestie", "They would share their last potato with you.", "goal")));
        futures.add(save(cache, "arch_nemesis", advancement("Arch Nemesis", "They hiss in your general direction.", "challenge")));
        futures.add(save(cache, "love_hate", advancement("Love Hate Relationship", "Best friend, worst enemy… same creature.", "challenge")));
        futures.add(save(cache, "drama_llama", advancement("Drama Llama", "It’s not complicated, it’s chaotic.", "goal")));
        futures.add(save(cache, "smooth_talker", advancement("Smooth Talker", "All persuasion, zero inventory.", "goal")));
        futures.add(save(cache, "no_hard_feelings", advancement("No Hard Feelings", "Ouch. Sorry. Friends?", "task")));
        futures.add(save(cache, "squad_goals", advancement("Squad Goals", "Group selfie energy.", "goal")));
        futures.add(save(cache, "borrowed_forever", advancement("Borrowed Forever", "Technically not stealing.", "challenge")));
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> save(CachedOutput cache, String name, JsonObject json) {
        Path path = this.output.getOutputFolder().resolve("data/creaturechat/advancements/" + name + ".json");
        return DataProvider.saveStable(cache, json, path);
    }

    private JsonObject advancement(String title, String description, String frame) {
        JsonObject root = new JsonObject();

        JsonObject display = new JsonObject();
        JsonObject icon = new JsonObject();
        icon.addProperty("item", "minecraft:paper");
        display.add("icon", icon);

        JsonObject titleObj = new JsonObject();
        titleObj.addProperty("text", title);
        display.add("title", titleObj);
        JsonObject descObj = new JsonObject();
        descObj.addProperty("text", description);
        display.add("description", descObj);

        display.addProperty("frame", frame);
        display.addProperty("show_toast", true);
        display.addProperty("announce_to_chat", true);
        display.addProperty("hidden", false);
        root.add("display", display);

        JsonObject criterion = new JsonObject();
        criterion.addProperty("trigger", "minecraft:impossible");
        criterion.add("conditions", new JsonObject());

        JsonObject criteria = new JsonObject();
        criteria.add("triggered", criterion);
        root.add("criteria", criteria);

        JsonArray requirements = new JsonArray();
        JsonArray inner = new JsonArray();
        inner.add("triggered");
        requirements.add(inner);
        root.add("requirements", requirements);

        return root;
    }

    @Override
    public String getName() {
        return "CreatureChat Advancements";
    }
}
