// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;

/**
 * Utility for creating client-side entity instances across versions.
 *
 * <p>In 1.21+, {@link EntityType#create(Level, EntitySpawnReason)} replaces the
 * older overload. This implementation supplies the {@code TRIGGERED} spawn reason.</p>
 */
public class EntityCreationHelper {
    public static Entity create(EntityType<?> type) {
        Level level = Minecraft.getInstance().level;
        return level == null ? null : type.create(level, EntitySpawnReason.TRIGGERED);
    }
}
