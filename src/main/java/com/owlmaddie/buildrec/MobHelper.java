// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.buildrec;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

/** Utility methods for creating mob instances across versions. */
public class MobHelper {
    private MobHelper() {}

    public static Mob create(EntityType<? extends Mob> type, ServerLevel level) {
        return (Mob) type.create(level);
    }

    public static void initSpawn(Mob mob, ServerLevel level) {
        if (mob != null) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null, null);
        }
    }
}
