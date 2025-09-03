// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.goals;

import com.owlmaddie.chat.AdvancementHelper;
import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.controls.LookControls;
import com.owlmaddie.network.ServerPackets;
import com.owlmaddie.particle.LeadParticleEffect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Leads a player toward a fixed destination.
 */
public class LeadPlayerGoal extends PlayerBaseGoal {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private final Mob entity;
    private final double speed;
    private final Vec3 destination;
    private final Vec3 startPos;
    private boolean arrived = false;
    private int ticksSinceParticle = 0;
    private int ticksSinceProgressCheck = 0;
    private Vec3 lastProgressPos;

    public LeadPlayerGoal(ServerPlayer player, Mob entity, double speed, Vec3 destination) {
        super(player);
        this.entity = entity;
        this.speed = speed;
        this.destination = destination;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.startPos = player.position();
        this.lastProgressPos = entity.position();
        LOGGER.info("Lead goal destination set to ({}, {}, {})", destination.x, destination.y, destination.z);
        emitParticlesAlongRaycast(this.entity.position(), this.destination);
    }

    @Override
    public boolean canUse() {
        return super.canUse() && !arrived && this.entity.distanceToSqr(this.targetEntity) <= 16 * 16;
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() && !arrived && this.entity.distanceToSqr(this.targetEntity) <= 16 * 16;
    }

    @Override
    public void tick() {
        ticksSinceParticle++;
        ticksSinceProgressCheck++;

        if (ticksSinceParticle % 20 == 0) {
            emitParticlesAlongRaycast(this.entity.position(), this.destination);
        }

        if (ticksSinceProgressCheck >= 40) {
            Vec3 pos = this.entity.position();
            if (pos.distanceToSqr(this.lastProgressPos) < 1) {
                LOGGER.debug("Repathing to destination due to being stuck at ({}, {}, {})", pos.x, pos.y, pos.z);
                this.entity.getNavigation().stop();
                moveToTarget();
            }
            this.lastProgressPos = pos;
            ticksSinceProgressCheck = 0;
        }

        if (this.entity.distanceToSqr(this.targetEntity) > 16 * 16) {
            this.entity.getNavigation().stop();
            return;
        }

        Vec3 entityPos = this.entity.position();
        double dx = entityPos.x - this.destination.x;
        double dz = entityPos.z - this.destination.z;
        if (dx * dx + dz * dz < 2 * 2) {
            if (!arrived) {
                arrived = true;
                double distance = this.startPos.distanceTo(this.targetEntity.position());
                if (distance >= 64) {
                    AdvancementHelper.guidedTour((ServerPlayer) this.targetEntity);
                }
                LOGGER.info("Arrived at destination ({}, {}, {})", destination.x, destination.y, destination.z);

                ServerPackets.scheduler.scheduleTask(() -> {
                    String arrivedMessage = "<You have arrived at your destination>";

                    ChatDataManager chatDataManager = ChatDataManager.getServerInstance();
                    EntityChatData chatData = chatDataManager.getOrCreateChatData(this.entity.getStringUUID());
                    if (!chatData.characterSheet.isEmpty()) {
                        ServerPackets.generate_chat("N/A", chatData, (ServerPlayer) this.targetEntity, this.entity, arrivedMessage, true);
                    }
                });

                this.entity.getNavigation().stop();
            }
        } else {
            moveToTarget();
        }
    }

    private void moveToTarget() {
        if (this.entity instanceof PathfinderMob) {
            if (!this.entity.getNavigation().isInProgress()) {
                Path path = this.entity.getNavigation().createPath(destination.x, destination.y, destination.z, 1);
                if (path != null) {
                    LOGGER.debug("Start moving along path");
                    this.entity.getNavigation().moveTo(path, this.speed);
                }
            }
        } else {
            LookControls.lookAtPosition(destination, this.entity);
            Vec3 entityPos = this.entity.position();
            Vec3 moveDirection = destination.subtract(entityPos).normalize();
            double currentSpeed = this.entity.getDeltaMovement().horizontalDistance();
            currentSpeed = Mth.approach((float) currentSpeed, (float) this.speed,
                    (float) (0.005 * (this.speed / Math.max(currentSpeed, 0.1))));
            Vec3 newVelocity = new Vec3(moveDirection.x * currentSpeed, moveDirection.y * currentSpeed,
                    moveDirection.z * currentSpeed);
            this.entity.setDeltaMovement(newVelocity);
            this.entity.hurtMarked = true;
        }
    }

    private void emitParticleAt(Vec3 position, double angle) {
        if (this.entity.level() instanceof ServerLevel serverWorld) {
            LeadParticleEffect effect = new LeadParticleEffect((float) angle);
            serverWorld.sendParticles(effect, position.x, position.y + 0.05, position.z, 1, 0, 0, 0, 0);
        }
    }

    private void emitParticlesAlongRaycast(Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start);
        double angleRadians = Math.atan2(direction.z, direction.x);
        double angleDegrees = Math.toDegrees(angleRadians);
        double minecraftYaw = (360 - (angleDegrees + 90)) % 360;
        minecraftYaw = (minecraftYaw + 180) % 360;
        if (minecraftYaw < 0) {
            minecraftYaw += 360;
        }
        double distance = start.distanceTo(end);
        double startRange = Math.min(2, distance);
        double endRange = Math.min(startRange + 10, distance);
        for (double d = startRange; d <= endRange; d += 4) {
            Vec3 pos = start.add(direction.normalize().scale(d));
            emitParticleAt(pos, Math.toRadians(minecraftYaw));
        }
    }
}

