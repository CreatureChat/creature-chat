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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Map;

/**
 * Goal that triggers build replays. The underlying replay is handled by
 * {@link BuildRecorder}; this goal merely starts, pauses, and resumes the
 * replay as the goal starts and stops.
 */
public class BuildPlayerGoal extends PlayerBaseGoal {
    private static final double PLAYER_REACH_DIST_SQR = 25.0;
    private final Mob entity;
    private final String buildType;
    private final double speed;
    private boolean completed = false;
    private boolean startedReplay = false;
    private boolean finishing = false;
    private boolean reachedPlayer = false;
    private String actualType;
    private BlockPos buildPos;
    private boolean fetchingMaterials = false;
    private boolean sentRecipe = false;
    private int materialWaitTicks = 0;
    private boolean controlsReleased = false;
    private int stuckTicks = 0;
    private int rerouteAttempts = 0;
    private boolean sentStuckMessage = false;
    private boolean loggedMissingCursor = false;
    private int aiCheckTicks = 0;
    private boolean aiPause = false;
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
        if (completed || this.targetEntity == null || !this.targetEntity.isAlive()) {
            BuildRecorder.cancelReplay(this.entity);
        } else {
            BuildRecorder.pauseReplay(this.entity);
        }
    }

    @Override
    public void tick() {
        if (completed) return;

        if (!startedReplay) {
            if (!reachedPlayer) {
                double distToPlayer = this.entity.distanceToSqr(this.targetEntity);
                if (distToPlayer <= PLAYER_REACH_DIST_SQR) {
                    buildPos = findStartPos(BlockPos.containing(this.targetEntity.position()));
                    reachedPlayer = true;
                    LOGGER.info("[BuildGoal] reached player choose buildPos {}", buildPos);
                    this.entity.getNavigation().moveTo(buildPos.getX(), buildPos.getY() + 1, buildPos.getZ(), this.speed);
                } else {
                    this.entity.getNavigation().moveTo(this.targetEntity, this.speed);
                    return;
                }
            }

            double dist = this.entity.distanceToSqr(buildPos.getX() + 0.5, buildPos.getY() + 1, buildPos.getZ() + 0.5);
            if (dist <= 1.0 || !this.entity.getNavigation().isInProgress()) {
                // ensure the actor stands on the surface so replay bases aren't one block too low
                this.entity.teleportTo(buildPos.getX() + 0.5, buildPos.getY() + 1, buildPos.getZ() + 0.5);
                EntityChatData data = ChatDataManager.getServerInstance().getOrCreateChatData(this.entity.getStringUUID());
                int tier = this.entity.getBbHeight() < 1 ? 1 : (this.entity.getBbHeight() < 2 ? 2 : 3);
                String file = BuildRecorder.randomBuildFile(this.entity.getBbHeight(), buildType, data.buildLevel);
                LOGGER.info("[BuildGoal] select build skill={} type={} heightTier={} file={}", data.buildLevel, buildType, tier, file);
                if (file != null && BuildRecorder.startReplay((ServerPlayer) this.targetEntity, this.entity, file, 1)) {
                    startedReplay = true;
                    actualType = (buildType == null || buildType.isEmpty() || "unknown".equalsIgnoreCase(buildType)) ? file.split("/")[0] : buildType;
                    LOGGER.info("[BuildGoal] started replay type={} at {}", actualType, buildPos);
                } else if (this.targetEntity instanceof ServerPlayer player) {
                    String prompt = (buildType == null || buildType.isEmpty())
                            ? "Explain to the player that you don't know how to build that."
                            : "Explain to the player that you don't know how to build a " + buildType + ".";
                    ServerPackets.generate_chat("N/A", data, player, this.entity, prompt, true);
                    completed = true;
                    LOGGER.info("[BuildGoal] failed to start replay type={}", buildType);
                }
            } else {
                this.entity.getNavigation().moveTo(buildPos.getX(), buildPos.getY() + 1, buildPos.getZ(), this.speed);
            }
            return;
        }

            if (BuildRecorder.isReplaying(this.entity)) {
            if (!fetchingMaterials) {
                if (aiPause) {
                    aiPause = false;
                    BuildRecorder.resumeReplay(this.entity);
                } else if (++aiCheckTicks >= 40) {
                    aiCheckTicks = 0;
                    aiPause = true;
                    BuildRecorder.pauseReplay(this.entity);
                    return;
                }
            }

            Map<String, Integer> recipe = BuildRecorder.getMissingRecipe(this.entity);
            if (recipe != null) {
                if (!fetchingMaterials) {
                    materialWaitTicks = 0;
                }
                fetchingMaterials = true;
                double distToPlayer = this.entity.distanceToSqr(this.targetEntity);
                if (distToPlayer > PLAYER_REACH_DIST_SQR && !controlsReleased) {
                    this.entity.getNavigation().moveTo(this.targetEntity, this.speed);
                } else {
                    if (!controlsReleased) {
                        this.entity.getNavigation().stop();
                    }
                    if (this.targetEntity instanceof ServerPlayer player) {
                        LookControls.lookAtPlayer(player, this.entity);
                        if (!sentRecipe) {
                            EntityChatData data = ChatDataManager.getServerInstance().getOrCreateChatData(this.entity.getStringUUID());
                            String nextItem = BuildRecorder.getNextMissingItem(this.entity);
                            if (nextItem == null) nextItem = "unknown";
                            LOGGER.info("[BuildGoal] next missing item={} remaining={}", nextItem, BuildRecorder.recipeToString(recipe));
                            String limited = BuildRecorder.recipeToString(recipe, 2);
                            String msg = "Next item needed: " + nextItem.replace('_', ' ') + ". Build paused - missing inventory items: " + limited + ". In your reply, ask the player for these items and confirm you'll continue building once they arrive.";
                            ServerPackets.generate_chat("N/A", data, player, this.entity, msg, true);
                            if (this.entity.level() instanceof ServerLevel level) {
                                String broadcast = BuildRecorder.recipeToDisplayString(recipe, 2);
                                Component text = Component.literal(broadcast).withStyle(ChatFormatting.WHITE);
                                for (ServerPlayer p : level.players()) {
                                    if (p.distanceToSqr(this.entity) <= 1024) {
                                        p.displayClientMessage(text, false);
                                    }
                                }
                            }
                            sentRecipe = true;
                        }
                    }
                }
                if (!controlsReleased) {
                    if (materialWaitTicks++ >= 80) {
                        BuildRecorder.pauseReplay(this.entity);
                        this.setFlags(EnumSet.noneOf(Flag.class));
                        this.entity.getNavigation().moveTo(buildPos.getX(), buildPos.getY() + 1, buildPos.getZ(), this.speed);
                        controlsReleased = true;
                    }
                } else {
                    double distToBuild = this.entity.distanceToSqr(buildPos.getX() + 0.5, buildPos.getY() + 1, buildPos.getZ() + 0.5);
                    if (distToBuild > 36 && !this.entity.getNavigation().isInProgress()) {
                        this.entity.getNavigation().moveTo(buildPos.getX(), buildPos.getY() + 1, buildPos.getZ(), this.speed);
                    }
                }
                return;
            } else if (fetchingMaterials) {
                fetchingMaterials = false;
                sentRecipe = false;
                materialWaitTicks = 0;
                controlsReleased = false;
                stuckTicks = 0;
                rerouteAttempts = 0;
                sentStuckMessage = false;
                aiCheckTicks = 0;
                aiPause = false;
                this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
                if (buildPos != null) {
                    this.entity.getNavigation().moveTo(buildPos.getX(), buildPos.getY() + 1, buildPos.getZ(), this.speed);
                }
            }
            BlockPos cursor = BuildRecorder.getReplayCursor(this.entity);
            if (cursor != null) {
                loggedMissingCursor = false;
                buildPos = cursor;
                double dist = this.entity.distanceToSqr(cursor.getX() + 0.5, cursor.getY() + 1, cursor.getZ() + 0.5);
                if (dist > 4.0) {
                    LOGGER.info("[BuildGoal] pause replay move to cursor {} (dist={})", cursor, dist);
                    BuildRecorder.pauseReplay(this.entity);
                    if (!this.entity.getNavigation().isInProgress()) {
                        Path path = this.entity.getNavigation().createPath(cursor.getX(), cursor.getY() + 1, cursor.getZ(), 1);
                        if (path != null) {
                            this.entity.getNavigation().moveTo(path, this.speed);
                            stuckTicks = 0;
                        } else if (++stuckTicks > 80) {
                            stuckTicks = 0;
                            if (++rerouteAttempts >= 3 && !sentStuckMessage && this.targetEntity instanceof ServerPlayer player) {
                                EntityChatData data = ChatDataManager.getServerInstance().getOrCreateChatData(this.entity.getStringUUID());
                                String msg = "I can't find where I left off in the build. Please help me get back on track.";
                                ServerPackets.generate_chat("N/A", data, player, this.entity, msg, true);
                                sentStuckMessage = true;
                            }
                        }
                    } else {
                        stuckTicks = 0;
                    }
                } else {
                    stuckTicks = 0;
                    rerouteAttempts = 0;
                    LOGGER.info("[BuildGoal] resume replay at cursor {} (dist={})", cursor, dist);
                    BuildRecorder.resumeReplay(this.entity);
                }
            } else if (!loggedMissingCursor) {
                LOGGER.info("[BuildGoal] waiting for replay cursor");
                loggedMissingCursor = true;
            }
        } else if (!finishing && this.targetEntity instanceof ServerPlayer player) {
            finishing = true;
            LOGGER.info("[BuildGoal] replay finished returning to player");
            this.entity.getNavigation().moveTo(player, this.speed);
        } else if (finishing && this.targetEntity instanceof ServerPlayer player) {
            LookControls.lookAtPlayer(player, this.entity);
            if (this.entity.distanceToSqr(player) <= PLAYER_REACH_DIST_SQR) {
                EntityChatData data = ChatDataManager.getServerInstance().getOrCreateChatData(this.entity.getStringUUID());
                String type = (actualType == null || actualType.isEmpty()) ? "structure" : actualType;
                String msg = "<you have successfully completed the \"" + type + "\" build>";
                ServerPackets.generate_chat("N/A", data, player, this.entity, msg, true);
                data.buildLevel = Math.min(5, data.buildLevel + 1);
                ServerPackets.BroadcastEntityMessage(data);
                completed = true;
                LOGGER.info("[BuildGoal] completion message sent");
            } else {
                this.entity.getNavigation().moveTo(player, this.speed);
            }
        }
    }

    private BlockPos findStartPos(BlockPos target) {
        BlockPos ground = findGround(target);
        if (isValidBuildPos(ground) &&
                this.entity.getNavigation().createPath(ground.getX(), ground.getY() + 1, ground.getZ(), 1) != null) {
            return ground;
        }
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(target.offset(-3, -1, -3), target.offset(3, 1, 3))) {
            BlockPos g = findGround(pos);
            if (!isValidBuildPos(g)) {
                continue;
            }
            if (this.entity.getNavigation().createPath(g.getX(), g.getY() + 1, g.getZ(), 1) != null) {
                double d = g.distSqr(target);
                if (d < bestDist) {
                    bestDist = d;
                    best = g.immutable();
                }
            }
        }
        return best != null ? best : ground;
    }

    private BlockPos findGround(BlockPos pos) {
        Level level = this.entity.level();
        BlockPos ground = pos;
        while (ground.getY() > -64) {
            if (isSolidGround(level, ground)) {
                return ground;
            }
            ground = ground.below();
        }
        return pos;
    }

    private boolean isSolidGround(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        return state.isCollisionShapeFullBlock(level, pos);
    }

    private boolean isValidBuildPos(BlockPos ground) {
        int height = Mth.ceil(this.entity.getBbHeight());
        for (int i = 1; i <= height; i++) {
            if (!this.entity.level().isEmptyBlock(ground.above(i))) {
                return false;
            }
        }
        return true;
    }
}
