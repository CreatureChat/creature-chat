// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.owlmaddie.buildrec.BuildRecordIO;
import com.owlmaddie.buildrec.BuildRecordIO.Meta;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Generates an index of bundled build recordings.
 */
public class BuildIndexProvider implements DataProvider {
    private final FabricDataOutput output;

    public BuildIndexProvider(FabricDataOutput output) {
        this.output = output;
    }

    @Override
    public String getName() {
        return "Build Index";
    }

    @Override
    public java.util.concurrent.CompletableFuture<?> run(CachedOutput cache) {
        List<Entry> entries = new ArrayList<>();
        try {
            java.net.URL url = BuildIndexProvider.class.getClassLoader().getResource("assets/creaturechat/builds");
            if (url != null) {
                Path buildsDir = Path.of(url.toURI());
                if (Files.exists(buildsDir)) {
                    Files.walk(buildsDir).filter(p -> p.toString().endsWith(".json.gz")).forEach(p -> {
                        try {
                            BuildRecordIO.Loaded loaded = BuildRecordIO.read(p);
                            Entry e = new Entry();
                            Path rel = buildsDir.relativize(p);
                            e.file = rel.getFileName().toString();
                            e.type = rel.getName(0).toString();
                            e.height = rel.getNameCount() > 2 ? rel.getName(1).toString() : "any";
                            e.recipe = loaded.meta.recipe;
                            e.raw = rawScore(loaded);
                            entries.add(e);
                        } catch (IOException | RuntimeException ex) {
                            // ignore malformed files
                        }
                    });
                }
            }
        } catch (IOException | java.net.URISyntaxException ignored) {}
        entries.sort(Comparator.comparingDouble(a -> a.raw));
        int n = entries.size();
        for (int i = 0; i < n; i++) {
            entries.get(i).score = (int) Math.min(5, Math.floor((double) i * 5 / n) + 1);
        }
        JsonArray arr = new JsonArray();
        for (Entry e : entries) {
            JsonObject o = new JsonObject();
            o.addProperty("type", e.type);
            o.addProperty("height", e.height);
            o.addProperty("file", e.file);
            JsonObject recipe = new JsonObject();
            for (Map.Entry<String, Integer> r : e.recipe.entrySet()) {
                recipe.addProperty(r.getKey(), r.getValue());
            }
            o.add("recipe", recipe);
            o.addProperty("score", e.score);
            arr.add(o);
        }
        JsonObject root = new JsonObject();
        root.add("builds", arr);
        Path out = output.getOutputFolder().resolve("assets/creaturechat/builds/index.json");
        return DataProvider.saveStable(cache, root, out);
    }

    private static double rawScore(BuildRecordIO.Loaded loaded) {
        Meta meta = loaded.meta;
        Map<String, Integer> recipe = meta.recipe;
        int unique = recipe.size();
        int total = recipe.values().stream().mapToInt(Integer::intValue).sum();
        int steps = loaded.actions.size();
        int duration = loaded.actions.stream().mapToInt(a -> a.dt).sum();
        int rarity = recipe.entrySet().stream().mapToInt(e -> {
            String name = e.getKey();
            int base = 1;
            if (name.contains("diamond") || name.contains("netherite")) base = 4;
            else if (name.contains("gold") || name.contains("emerald")) base = 3;
            else if (name.contains("iron") || name.contains("copper")) base = 2;
            return base * e.getValue();
        }).sum();
        return unique * 5 + total + steps + (duration / 20.0) + rarity * 3;
    }

    private static class Entry {
        String type;
        String height;
        String file;
        Map<String, Integer> recipe;
        double raw;
        int score;
    }
}

