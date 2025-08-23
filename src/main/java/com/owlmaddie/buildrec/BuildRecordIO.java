// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.buildrec;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Utility methods for reading build recording files.
 */
public final class BuildRecordIO {
    public static final Gson GSON = new Gson();

    private BuildRecordIO() {}

    public static Loaded read(Path file) throws IOException, JsonParseException {
        try (JsonReader reader = new JsonReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8))) {
            List<Action> actions = new ArrayList<>();
            reader.beginArray();
            JsonElement first = GSON.fromJson(reader, JsonElement.class);
            Meta meta;
            if (first != null && first.isJsonObject() && first.getAsJsonObject().has("action") && "meta".equals(first.getAsJsonObject().get("action").getAsString())) {
                meta = GSON.fromJson(first, Meta.class);
            } else {
                meta = new Meta();
                if (first != null) {
                    Action firstAction = GSON.fromJson(first, Action.class);
                    if (firstAction != null) actions.add(firstAction);
                }
            }
            while (reader.hasNext()) {
                Action a = GSON.fromJson(reader, Action.class);
                if (a != null) actions.add(a);
            }
            reader.endArray();
            return new Loaded(meta, actions);
        }
    }

    public static class Loaded {
        public final Meta meta;
        public final List<Action> actions;
        public Loaded(Meta meta, List<Action> actions) {
            this.meta = meta;
            this.actions = actions;
        }
    }

    public static class Meta {
        public String action = "meta";
        public double eyeHeight;
        public double bbWidth;
        public double bbHeight;
        public Map<String, Integer> recipe = new LinkedHashMap<>();
        public int uniqueBlocks;
        public int sizeX;
        public int sizeY;
        public int sizeZ;
        public List<String> palette = new ArrayList<>();
        public Meta() {}
        public Meta(double eyeHeight, double bbWidth, double bbHeight, Map<String, Integer> recipe, int uniqueBlocks, int sizeX, int sizeY, int sizeZ, List<String> palette) {
            this.eyeHeight = eyeHeight;
            this.bbWidth = bbWidth;
            this.bbHeight = bbHeight;
            this.recipe = recipe;
            this.uniqueBlocks = uniqueBlocks;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.palette = palette;
        }
    }

    public static class Action {
        public String action;
        public int blockId;
        public int bx, by, bz;
        public int dt;
        public double px, py, pz;
        public float yaw, pitch;
    }
}

