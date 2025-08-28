// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BookHelper {
    public static boolean isBook(Item item) {
        return item == Items.WRITTEN_BOOK || item == Items.WRITABLE_BOOK || item == Items.BOOK;
    }

    private static String extractText(String json) {
        try {
            JsonElement element = JsonParser.parseString(json);
            return collectText(element);
        } catch (Exception e) {
            return json;
        }
    }

    private static String collectText(JsonElement element) {
        if (element == null) {
            return "";
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        StringBuilder sb = new StringBuilder();
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("text")) {
                sb.append(obj.get("text").getAsString());
            }
            if (obj.has("extra")) {
                JsonArray arr = obj.getAsJsonArray("extra");
                for (JsonElement e : arr) {
                    sb.append(collectText(e));
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) {
                sb.append(collectText(e));
            }
        }
        return sb.toString();
    }

    private static String readBook(ItemStack stack) {
        if (!stack.hasTag()) {
            return "";
        }
        CompoundTag tag = stack.getTag();
        ListTag pages = tag.getList("pages", 8);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            String page = pages.getString(i);
            if (stack.is(Items.WRITTEN_BOOK)) {
                sb.append(extractText(page));
            } else {
                sb.append(page);
            }
        }
        return sb.toString();
    }

    public static String summarizeBook(ItemStack stack) {
        if (!isBook(stack.getItem())) {
            return "N/A";
        }
        String text = readBook(stack);
        if (text.isEmpty()) {
            return "N/A";
        }
        if (text.length() <= 2048) {
            return text;
        }
        int skip = text.length() - 2048;
        String start = text.substring(0, 1024);
        String end = text.substring(text.length() - 1024);
        return start + "... skipped " + skip + " chars ..." + end;
    }
}
