package com.owlmaddie.ui;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.resource.featuretoggle.FeatureFlags;

public class ModScreenHandlers {
    public static final ScreenHandlerType<TagEditorScreenHandler> TAG_EDITOR_SCREEN_HANDLER;

    static {
        TAG_EDITOR_SCREEN_HANDLER = new ScreenHandlerType<>(
            (syncId, playerInventory) -> new TagEditorScreenHandler(syncId, playerInventory),
            FeatureFlags.VANILLA_FEATURES
        );

        Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier("mymod", "tag_editor"), // Replace "mymod" with your mod ID
            TAG_EDITOR_SCREEN_HANDLER
        );
    }

    public static void register() {
        // Ensure this is called during mod initialization to register the handler
    }
}
