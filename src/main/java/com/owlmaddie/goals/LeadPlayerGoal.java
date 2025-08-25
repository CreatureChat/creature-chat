// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.goals;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.AdvancementHelper;
import com.owlmaddie.controls.LookControls;
import com.owlmaddie.network.ServerPackets;
import com.owlmaddie.particle.LeadParticleEffect;
import com.owlmaddie.utils.RandomTargetFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Random;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * The {@code LeadPlayerGoal} class instructs a Mob Entity to lead the player to a random location, consisting
 * of many random waypoints. It supports PathAware and NonPathAware entities.
 */
public class LeadPlayerGoal extends PlayerBaseGoal {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private final Mob entity;
    private final double speed;
    private final Random random = new Random();
    private int currentWaypoint = 0;
    private int totalWaypoints;
    private Vec3 currentTarget = null;
    private boolean foundWaypoint = false;
    private int ticksSinceLastWaypoint = 0;
    private final Vec3 startPos;

    public LeadPlayerGoal(ServerPlayer player, Mob entity, double speed) {
        super(player);
        this.entity = entity;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.totalWaypoints = random.nextInt(14) + 6;
        this.startPos = player.position();
    }

    @Override
    public boolean canUse() {
        return super.canUse() && !foundWaypoint && this.entity.distanceToSqr(this.targetEntity) <= 16 * 16 && !foundWaypoint;
    }

    @Override
    public boolean canContinueToUse() {
        return super.canUse() && !foundWaypoint && this.entity.distanceToSqr(this.targetEntity) <= 16 * 16 && !foundWaypoint;
    }

    @Override
    public void tick() {
        ticksSinceLastWaypoint++;

        if (this.entity.distanceToSqr(this.targetEntity) > 16 * 16) {
            this.entity.getNavigation().stop();
            return;
        }

        // Are we there yet?
        if (currentWaypoint >= totalWaypoints && !foundWaypoint) {
            foundWaypoint = true;
            double distance = this.startPos.distanceTo(this.targetEntity.position());
            if (distance >= 64) {
                AdvancementHelper.guidedTour((ServerPlayer) this.targetEntity);
            }
            LOGGER.info("Tick: You have ARRIVED at your destination");

            ServerPackets.scheduler.scheduleTask(() -> {
                // Prepare a message about the interaction
                String arrivedMessage = "<You have arrived at your destination>";

                ChatDataManager chatDataManager = ChatDataManager.getServerInstance();
                EntityChatData chatData = chatDataManager.getOrCreateChatData(this.entity.getStringUUID());
                if (!chatData.characterSheet.isEmpty()) {
                    ServerPackets.generate_chat("N/A", chatData, (ServerPlayer) this.targetEntity, this.entity, arrivedMessage, true);
                }
            });

            // Stop navigation
            this.entity.getNavigation().stop();

        } else if (this.currentTarget == null || this.entity.distanceToSqr(this.currentTarget) < 2 * 2 || ticksSinceLastWaypoint >= 20 * 10) {
            // Set next waypoint
            setNewTarget();
            moveToTarget();
            ticksSinceLastWaypoint = 0;

        } else {
            moveToTarget();
        }
    }

    private void moveToTarget() {
        if (this.currentTarget != null) {
            if (this.entity instanceof PathfinderMob) {
                 if (!this.entity.getNavigation().isInProgress()) {
                     Path path = this.entity.getNavigation().createPath(this.currentTarget.x, this.currentTarget.y, this.currentTarget.z, 1);
                     if (path != null) {
                         LOGGER.debug("Start moving along path");
                         this.entity.getNavigation().moveTo(path, this.speed);
                     }
                 }
            } else {
                // Make the entity look at the player without moving towards them
                LookControls.lookAtPosition(this.currentTarget, this.entity);

                // Move towards the target for non-path aware entities
                Vec3 entityPos = this.entity.position();
                Vec3 moveDirection = this.currentTarget.subtract(entityPos).normalize();

                // Calculate current speed from the entity's current velocity
                double currentSpeed = this.entity.getDeltaMovement().horizontalDistance();

                // Gradually adjust speed towards the target speed
                currentSpeed = Mth.approach((float) currentSpeed, (float) this.speed, (float) (0.005 * (this.speed / Math.max(currentSpeed, 0.1))));

                // Apply movement with the adjusted speed towards the target
                Vec3 newVelocity = new Vec3(moveDirection.x * currentSpeed, moveDirection.y * currentSpeed, moveDirection.z * currentSpeed);

                this.entity.setDeltaMovement(newVelocity);
                this.entity.hurtMarked = true;
            }
        }
    }

    private void setNewTarget() {
        // Increment waypoint
        currentWaypoint++;
        LOGGER.info("Waypoint " + currentWaypoint + " / " + this.totalWaypoints);
        this.currentTarget = RandomTargetFinder.findRandomTarget(this.entity, 30, 24, 36);
        if (this.currentTarget != null) {
            emitParticlesAlongRaycast(this.entity.position(), this.currentTarget);
        }

        // Stop following current path (if any)
        this.entity.getNavigation().stop();
    }

    private void emitParticleAt(Vec3 position, double angle) {
        if (this.entity.level() instanceof ServerLevel) {
            ServerLevel serverWorld = (ServerLevel) this.entity.level();

            // Pass the angle using the "speed" argument, with deltaX, deltaY, deltaZ set to 0
            LeadParticleEffect effect = new LeadParticleEffect((float)angle);
            serverWorld.sendParticles(effect, position.x, position.y + 0.05, position.z, 1, 0, 0, 0, 0);
        }
    }

    private void emitParticlesAlongRaycast(Vec3 start, Vec3 end) {
        // Calculate the direction vector from the entity (start) to the target (end)
        Vec3 direction = end.subtract(start);

        // Calculate the angle in the XZ-plane using atan2 (this is in radians)
        double angleRadians = Math.atan2(direction.z, direction.x);

        // Convert from radians to degrees
        double angleDegrees = Math.toDegrees(angleRadians);

        // Convert the calculated angle to Minecraft's yaw system:
        double minecraftYaw = (360 - (angleDegrees + 90)) % 360;

        // Correct the 180-degree flip
        minecraftYaw = (minecraftYaw + 180) % 360;
        if (minecraftYaw < 0) {
            minecraftYaw += 360;
        }

        // Emit particles along the ray from startRange to endRange
        double distance = start.distanceTo(end);
        double startRange = Math.min(5, distance);;
        double endRange = Math.min(startRange + 10, distance);
        for (double d = startRange; d <= endRange; d += 5) {
            Vec3 pos = start.add(direction.normalize().scale(d));
            emitParticleAt(pos, Math.toRadians(minecraftYaw));  // Convert back to radians for rendering
        }
    }
}