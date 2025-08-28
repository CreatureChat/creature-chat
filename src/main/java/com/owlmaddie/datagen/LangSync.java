// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * Updates existing static translation files with keys from the English map.
 * New keys are added with English text and missing keys are removed.
 */
public final class LangSync {
    private static Path findLangDir() {
        Path dir = Path.of(System.getProperty("user.dir"));
        for (int i = 0; i < 5 && dir != null; i++) {
            Path candidate = dir.resolve(Path.of("src", "main", "resources", "assets", "creaturechat", "lang"));
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return null;
    }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private LangSync() {}

    public static void sync(Map<String, String> en) {
        Path langDir = findLangDir();
        if (langDir == null) {
            return;
        }
        Map<String, String> sorted = new TreeMap<>(en);
        try (var stream = Files.newDirectoryStream(langDir, "*.json")) {
            for (Path file : stream) {
                String name = file.getFileName().toString();
                if ("en_us.json".equals(name)) continue;

                JsonObject existing = Files.exists(file)
                        ? GSON.fromJson(Files.readString(file), JsonObject.class)
                        : new JsonObject();
                JsonObject updated = new JsonObject();

                for (var entry : sorted.entrySet()) {
                    if (existing.has(entry.getKey())) {
                        updated.add(entry.getKey(), existing.get(entry.getKey()));
                    } else {
                        updated.addProperty(entry.getKey(), entry.getValue());
                    }
                }

                Files.writeString(file, GSON.toJson(updated) + System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

