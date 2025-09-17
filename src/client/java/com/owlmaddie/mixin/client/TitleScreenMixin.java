package com.owlmaddie.mixin.client;

import com.owlmaddie.player2.Player2StartupHandler;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    
    @Inject(
        method = "render",
        at = @At("HEAD")
    )
    private void onTitleScreenRender(CallbackInfo ci) {
        Logger LOGGER = LoggerFactory.getLogger("creaturepals");

        // Check Player2 API key when title screen is rendered
        // This ensures it happens after the mixin is fully loaded
        if (!Player2StartupHandler.hasCheckedApiKey()) {
            LOGGER.info("TitleScreenMixin: Title screen rendering, checking API key...");
            Player2StartupHandler.checkApiKeyOnStartup();
        }
    }
}
