package com.owlmaddie.buildrec;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;

/** Utility methods for creating mob instances across versions (1.21+). */
public class MobHelper {
    private MobHelper() {}

    public static Mob create(EntityType<? extends Mob> type, ServerLevel level) {
        return (Mob) type.create(level, EntitySpawnReason.COMMAND);
    }

    public static void initSpawn(Mob mob, ServerLevel level) {
        if (mob != null) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.COMMAND, (SpawnGroupData) null);
        }
    }
}
