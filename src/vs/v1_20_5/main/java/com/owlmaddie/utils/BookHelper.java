// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.component.WritableBookContent;

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
        StringBuilder sb = new StringBuilder();
        if (stack.is(Items.WRITTEN_BOOK)) {
            WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (content == null) {
                return "";
            }
            for (var page : content.pages()) {
                Component c = page.raw();
                sb.append(c.getString());
            }
        } else {
            WritableBookContent content = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
            if (content == null) {
                return "";
            }
            for (var page : content.pages()) {
                String raw = page.raw();
                // Writable books store raw JSON strings; attempt to extract plain text
                sb.append(extractText(raw));
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
