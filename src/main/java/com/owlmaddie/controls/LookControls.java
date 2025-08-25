// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.controls;

import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Squid;
import com.owlmaddie.controls.LookControlsHelper;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.Vec3;

/**
 * The {@code LookControls} class enables entities to look at the player,
 * with specific adjustments for certain mobs like Slimes and Squids.
 */
public class LookControls {
    public static void lookAtPlayer(ServerPlayer player, Mob entity) {
        // Get the player's eye line position
        Vec3 playerPos = player.position();
        float eyeHeight = player.getEyeHeight(player.getPose());
        Vec3 eyePos = new Vec3(playerPos.x, playerPos.y + eyeHeight, playerPos.z);

        lookAtPosition(eyePos, entity);
    }

    public static void lookAtPosition(Vec3 targetPos, Mob entity) {
        if (entity instanceof Slime) {
            handleSlimeLook((Slime) entity, targetPos);
        } else if (entity instanceof Squid) {
            handleSquidLook((Squid) entity, targetPos);
        } else if (LookControlsHelper.isGhast(entity)) {
            handleFlyingEntity(entity, targetPos, 10F);
        } else if (entity instanceof FlyingAnimal || entity instanceof Vex || entity instanceof Phantom) {
            handleFlyingEntity(entity, targetPos, 4F);
        } else {
            // Make the entity look at the player
            entity.getLookControl().setLookAt(targetPos.x, targetPos.y, targetPos.z, 10.0F, (float)entity.getMaxHeadXRot());
        }
    }

    private static void handleSlimeLook(Slime slime, Vec3 targetPos) {
        float yawChange = calculateYawChange(slime, targetPos);
        ((Slime.SlimeMoveControl) slime.getMoveControl()).setDirection(slime.getYRot() + yawChange, false);
    }

    private static void handleSquidLook(Squid squid, Vec3 targetPos) {
        Vec3 toPlayer = calculateNormalizedDirection(squid, targetPos);
        Vec3 swimVec  = toPlayer.scale(0.15f);

        // Force the internal swimVec so tickMovement() picks it up
        ((ISquidEntity)squid).forceSwimVector(swimVec);

        // Drive motion (so server and client both move)
        squid.setDeltaMovement(swimVec);

        if (squid.position().distanceTo(targetPos) < 3.5) {
            ((ISquidEntity)squid).forceSwimVector(Vec3.ZERO);
            squid.setDeltaMovement(0, 0, 0);
        }
    }

    // Ghast, Phantom, etc...
    private static void handleFlyingEntity(Mob flyingEntity, Vec3 targetPos, float stopDistance) {
        Vec3 flyingPosition = flyingEntity.position();
        Vec3 toPlayer = targetPos.subtract(flyingPosition).normalize();

        // Calculate the yaw to align the flyingEntity's facing direction with the movement direction
        float targetYaw = (float)(Mth.atan2(toPlayer.z, toPlayer.x) * (180 / Math.PI) - 90);
        flyingEntity.setYRot(targetYaw);

        // Look at player while adjusting yaw
        flyingEntity.getLookControl().setLookAt(targetPos.x, targetPos.y, targetPos.z, 10.0F, (float)flyingEntity.getMaxHeadXRot());

        float initialSpeed = 0.15F;
        flyingEntity.setDeltaMovement(
                (float) toPlayer.x * initialSpeed,
                (float) toPlayer.y * initialSpeed,
                (float) toPlayer.z * initialSpeed
        );

        double distanceToPlayer = flyingEntity.position().distanceTo(targetPos);
        if (distanceToPlayer < stopDistance) {
            // Stop motion when close
            flyingEntity.setDeltaMovement(0, 0, 0);
        }
    }

    public static float calculateYawChange(Mob entity, Vec3 targetPos) {
        Vec3 toPlayer = calculateNormalizedDirection(entity, targetPos);
        float targetYaw = (float) Math.toDegrees(Math.atan2(toPlayer.z, toPlayer.x)) - 90.0F;
        float yawDifference = Mth.wrapDegrees(targetYaw - entity.getYRot());
        return Mth.clamp(yawDifference, -10.0F, 10.0F);
    }

    public static Vec3 calculateNormalizedDirection(Mob entity, Vec3 targetPos) {
        Vec3 entityPos = entity.position();
        return targetPos.subtract(entityPos).normalize();
    }
}