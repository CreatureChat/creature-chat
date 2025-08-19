// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.inventory;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.inventory.MenuType;

/**
 * Registry for custom menus.
 */
public class ModMenus {
    public static MenuType<MobInventoryMenu> MOB_INVENTORY;

    public static void register() {
        MOB_INVENTORY = Registry.register(BuiltInRegistries.MENU,
                new ResourceLocation("creaturechat", "mob_inventory"),
                new ExtendedScreenHandlerType<>((syncId, inv, entityId) -> {
                    Mob mob = (Mob) inv.player.level().getEntity(entityId);
                    return new MobInventoryMenu(syncId, inv, new SimpleContainer(15), mob, null);
                }, StreamCodec.of((buf, value) -> buf.writeVarInt(value), FriendlyByteBuf::readVarInt)));
    }
}
