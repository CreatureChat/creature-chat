// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.goals;

import com.owlmaddie.buildrec.BuildRecorder;
import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.network.ServerPackets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

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
    private String actualType;
    private BlockPos buildPos;

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
            buildPos = findStartPos(this.targetEntity.blockPosition());
        } else {
            BlockPos cursor = BuildRecorder.getReplayCursor(this.entity);
            if (cursor != null) {
                buildPos = cursor;
            }
        }
        if (!this.entity.getNavigation().moveTo(buildPos.getX(), buildPos.getY(), buildPos.getZ(), this.speed) && !startedReplay) {
            buildPos = this.entity.blockPosition();
        }
    }

    @Override
    public void stop() {
        BuildRecorder.pauseReplay(this.entity);
    }

    @Override
    public void tick() {
        if (completed) return;

        if (!startedReplay) {
            double dist = this.entity.distanceToSqr(buildPos.getX() + 0.5, buildPos.getY(), buildPos.getZ() + 0.5);
            if (dist <= 1.0 || !this.entity.getNavigation().isInProgress()) {
                EntityChatData data = ChatDataManager.getServerInstance().getOrCreateChatData(this.entity.getStringUUID());
                String file = BuildRecorder.randomBuildFile(this.entity.getBbHeight(), buildType, data.buildLevel);
                if (file != null && BuildRecorder.startReplay((ServerPlayer) this.targetEntity, this.entity, file, 1)) {
                    startedReplay = true;
                    actualType = (buildType == null || buildType.isEmpty() || "unknown".equalsIgnoreCase(buildType)) ? file.split("/")[0] : buildType;
                } else if (this.targetEntity instanceof ServerPlayer player) {
                    String msg = (buildType == null || buildType.isEmpty()) ? "<you do not know how to build that>" : "<you do not know how to build a \"" + buildType + "\">";
                    data.addMessage(msg, ChatDataManager.ChatSender.ASSISTANT, player, "system-chat");
                    completed = true;
                }
            } else {
                this.entity.getNavigation().moveTo(buildPos.getX(), buildPos.getY(), buildPos.getZ(), this.speed);
            }
            return;
        }

        if (BuildRecorder.isReplaying(this.entity)) {
            BlockPos cursor = BuildRecorder.getReplayCursor(this.entity);
            if (cursor != null) {
                buildPos = cursor;
                double dist = this.entity.distanceToSqr(cursor.getX() + 0.5, cursor.getY(), cursor.getZ() + 0.5);
                if (dist > 1.0) {
                    BuildRecorder.pauseReplay(this.entity);
                    this.entity.getNavigation().moveTo(cursor.getX(), cursor.getY(), cursor.getZ(), this.speed);
                } else {
                    BuildRecorder.resumeReplay(this.entity);
                }
            }
        } else if (!finishing && this.targetEntity instanceof ServerPlayer player) {
            finishing = true;
            this.entity.getNavigation().moveTo(player, this.speed);
        } else if (finishing && this.targetEntity instanceof ServerPlayer player) {
            if (this.entity.distanceTo(player) <= 4) {
                EntityChatData data = ChatDataManager.getServerInstance().getOrCreateChatData(this.entity.getStringUUID());
                String type = (actualType == null || actualType.isEmpty()) ? "structure" : actualType;
                String msg = "<I'm done with your " + type + " build!>";
                ServerPackets.generate_chat("N/A", data, player, this.entity, msg, true);
                data.buildLevel = Math.min(5, data.buildLevel + 1);
                completed = true;
            } else {
                this.entity.getNavigation().moveTo(player, this.speed);
            }
        }
    }

    private BlockPos findStartPos(BlockPos target) {
        BlockPos current = this.entity.blockPosition();
        if (current.closerThan(target, 1.5)) {
            return current;
        }
        for (BlockPos pos : BlockPos.betweenClosed(target.offset(-1, -1, -1), target.offset(1, 1, 1))) {
            if (this.entity.getNavigation().createPath(pos.getX(), pos.getY(), pos.getZ(), 1) != null) {
                return pos.immutable();
            }
        }
        return current;
    }
}
