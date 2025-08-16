// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.ui;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.Villager;

/**
 * Handles the inventory key while riding mobs to open their inventory screen.
 */
public class InventoryKeyHandler {
    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }

            if (!(client.player.getVehicle() instanceof Mob mob)) {
                return;
            }

            if (mob instanceof Villager || mob instanceof TamableAnimal) {
                return;
            }

            if (client.options.keyInventory.consumeClick()) {
                client.player.connection.send(new ServerboundPlayerCommandPacket(client.player, ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));
            }
        });
    }
}

