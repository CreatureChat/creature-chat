// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.goals;

import com.owlmaddie.buildrec.BuildRecorder;
import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.controls.LookControls;
import com.owlmaddie.network.ServerPackets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * Goal that triggers build replays. The underlying replay is handled by
 * {@link BuildRecorder}; this goal merely starts, pauses, and resumes the
 * replay as the goal starts and stops.
 */
public class BuildPlayerGoal extends PlayerBaseGoal {
    private final Mob entity;
    private final String buildType;
    private final double speed;
    private boolean completed = false;
    private boolean startedReplay = false;
    private boolean finishing = false;
    private boolean reachedPlayer = false;
    private String actualType;
    private BlockPos buildPos;
    private static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");

    public BuildPlayerGoal(ServerPlayer player, Mob entity, double speed, String buildType) {
        super(player);
        this.entity = entity;
        this.buildType = buildType;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !completed && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !completed && super.canUse();
    }

    @Override
    public void start() {
        if (!startedReplay) {
            LOGGER.info("[BuildGoal] start navigation toward player");
            reachedPlayer = false;
            buildPos = null;
            this.entity.getNavigation().moveTo(this.targetEntity, this.speed);
        } else {
            BlockPos cursor = BuildRecorder.getReplayCursor(this.entity);
            if (cursor != null) {
                buildPos = cursor;
                LOGGER.info("[BuildGoal] resume navigation toward replay cursor {}", buildPos);
                this.entity.getNavigation().moveTo(buildPos.getX(), buildPos.getY() + 1, buildPos.getZ(), this.speed);
            }
        }
    }

    @Override
    public void stop() {
        LOGGER.info("[BuildGoal] stop goal pause replay");
        BuildRecorder.pauseReplay(this.entity);
    }

    @Override
    public void tick() {
        if (completed) return;

        if (!startedReplay) {
            if (!reachedPlayer) {
                double distToPlayer = this.entity.distanceToSqr(this.targetEntity);
                if (distToPlayer <= 1.0) {
                    buildPos = findGround(this.targetEntity.blockPosition());
                    reachedPlayer = true;
                    LOGGER.info("[BuildGoal] reached player choose buildPos {}", buildPos);
                    this.entity.getNavigation().moveTo(buildPos.getX(), buildPos.getY() + 1, buildPos.getZ(), this.speed);
                } else {
                    this.entity.getNavigation().moveTo(this.targetEntity, this.speed);
                    if (!this.entity.getNavigation().isInProgress()) {
                        buildPos = findStartPos(this.targetEntity.blockPosition());
                        reachedPlayer = true;
                        LOGGER.info("[BuildGoal] using nearest buildPos {}", buildPos);
                        this.entity.getNavigation().moveTo(buildPos.getX(), buildPos.getY() + 1, buildPos.getZ(), this.speed);
                    } else {
                        return;
                    }
                }
            }

            double dist = this.entity.distanceToSqr(buildPos.getX() + 0.5, buildPos.getY() + 1, buildPos.getZ() + 0.5);
            if (dist <= 1.0 || !this.entity.getNavigation().isInProgress()) {
                EntityChatData data = ChatDataManager.getServerInstance().getOrCreateChatData(this.entity.getStringUUID());
                String file = BuildRecorder.randomBuildFile(this.entity.getBbHeight(), buildType, data.buildLevel);
                if (file != null && BuildRecorder.startReplay((ServerPlayer) this.targetEntity, this.entity, file, 1)) {
                    startedReplay = true;
                    actualType = (buildType == null || buildType.isEmpty() || "unknown".equalsIgnoreCase(buildType)) ? file.split("/")[0] : buildType;
                    LOGGER.info("[BuildGoal] started replay type={} at {}", actualType, buildPos);
                } else if (this.targetEntity instanceof ServerPlayer player) {
                    String msg = (buildType == null || buildType.isEmpty()) ? "<you do not know how to build that>" : "<you do not know how to build a \"" + buildType + "\">";
                    data.addMessage(msg, ChatDataManager.ChatSender.ASSISTANT, player, "system-chat");
                    completed = true;
                    LOGGER.info("[BuildGoal] failed to start replay type={}", buildType);
                }
            } else {
                this.entity.getNavigation().moveTo(buildPos.getX(), buildPos.getY() + 1, buildPos.getZ(), this.speed);
            }
            return;
        }

        if (BuildRecorder.isReplaying(this.entity)) {
            BlockPos cursor = BuildRecorder.getReplayCursor(this.entity);
            if (cursor != null) {
                buildPos = cursor;
                double dist = this.entity.distanceToSqr(cursor.getX() + 0.5, cursor.getY() + 1, cursor.getZ() + 0.5);
                if (dist > 1.0) {
                    LOGGER.info("[BuildGoal] pause replay move to cursor {}", cursor);
                    BuildRecorder.pauseReplay(this.entity);
                    this.entity.getNavigation().moveTo(cursor.getX(), cursor.getY() + 1, cursor.getZ(), this.speed);
                } else {
                    LOGGER.info("[BuildGoal] resume replay at cursor {}", cursor);
                    BuildRecorder.resumeReplay(this.entity);
                }
            }
        } else if (!finishing && this.targetEntity instanceof ServerPlayer player) {
            finishing = true;
            LOGGER.info("[BuildGoal] replay finished returning to player");
            this.entity.getNavigation().moveTo(player, this.speed);
        } else if (finishing && this.targetEntity instanceof ServerPlayer player) {
            LookControls.lookAtPlayer(player, this.entity);
            if (this.entity.distanceTo(player) <= 5) {
                EntityChatData data = ChatDataManager.getServerInstance().getOrCreateChatData(this.entity.getStringUUID());
                String type = (actualType == null || actualType.isEmpty()) ? "structure" : actualType;
                String msg = "<you have successfully completed the \"" + type + "\" build>";
                ServerPackets.generate_chat("N/A", data, player, this.entity, msg, true);
                data.buildLevel = Math.min(5, data.buildLevel + 1);
                completed = true;
                LOGGER.info("[BuildGoal] completion message sent");
            } else {
                this.entity.getNavigation().moveTo(player, this.speed);
            }
        }
    }

    private BlockPos findStartPos(BlockPos target) {
        BlockPos ground = findGround(target);
        if (this.entity.getNavigation().createPath(ground.getX(), ground.getY() + 1, ground.getZ(), 1) != null) {
            return ground;
        }
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(target.offset(-3, -1, -3), target.offset(3, 1, 3))) {
            BlockPos g = findGround(pos);
            if (this.entity.getNavigation().createPath(g.getX(), g.getY() + 1, g.getZ(), 1) != null) {
                double d = g.distSqr(target);
                if (d < bestDist) {
                    bestDist = d;
                    best = g.immutable();
                }
            }
        }
        return best != null ? best : this.entity.blockPosition();
    }

    private BlockPos findGround(BlockPos pos) {
        Level level = this.entity.level();
        BlockPos ground = pos;
        while (level.isEmptyBlock(ground) && ground.getY() > -64) {
            ground = ground.below();
        }
        return ground;
    }
}
