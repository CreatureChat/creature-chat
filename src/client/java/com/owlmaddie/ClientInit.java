package com.owlmaddie;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.network.ClientPackets;
import com.owlmaddie.particle.CreatureParticleFactory;
import com.owlmaddie.particle.LeadParticleFactory;
import com.owlmaddie.ui.BubbleRenderer;
import com.owlmaddie.ui.ClickHandler;
import com.owlmaddie.ui.ModScreenHandlers;
import com.owlmaddie.ui.PlayerMessageManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import static com.owlmaddie.network.ServerPackets.*;

/**
 * The {@code ClientInit} class initializes this mod in the client and defines all hooks into the
 * render pipeline to draw chat bubbles, text, and entity icons.
 */
public class ClientInit implements ClientModInitializer {
    private static long tickCounter = 0;

    @Override
    public void onInitializeClient() {
        // Register particle factories
        ParticleFactoryRegistry.getInstance().register(HEART_SMALL_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(HEART_BIG_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(FIRE_SMALL_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(FIRE_BIG_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(ATTACK_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(FLEE_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(FOLLOW_FRIEND_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(FOLLOW_ENEMY_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(PROTECT_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(LEAD_FRIEND_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(LEAD_ENEMY_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(LEAD_PARTICLE, LeadParticleFactory::new);

        // Register Creature Tag Editor
        ModScreenHandlers.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickCounter++;
            PlayerMessageManager.tickUpdate();
        });

        // Register events
        ClickHandler.register();
        ClientPackets.register();

        // Register an event callback to render text bubbles
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register((context) -> {
            BubbleRenderer.drawTextAboveEntities(context, tickCounter, context.tickDelta());
        });

        // Register an event callback for when the client disconnects from a server or changes worlds
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // Clear or reset the ChatDataManager
            ChatDataManager.getClientInstance().clearData();
        });
    }
}
