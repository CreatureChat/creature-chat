// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.chat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code ChatPrompt} class is used to load a prompt from the Minecraft resource manager
 */
public class ChatPrompt {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturepals");

    // This method should be called in an appropriate context where ResourceManager is available
    public static String loadPromptFromResource(ResourceManager resourceManager, String promptName) {
        ResourceLocation fileIdentifier = new ResourceLocation("creaturepals", "prompts/" + promptName);
        try (InputStream inputStream = resourceManager.getResource(fileIdentifier).get().open();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            StringBuilder contentBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
            return contentBuilder.toString();
        } catch (Exception e) {
            LOGGER.error("Failed to read prompt file", e);
        }
        return null;
    }
}
