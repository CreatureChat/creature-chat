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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
    private static final double PLAYER_MESSAGE_DIST = 4.0;
    private static final double PLAYER_MESSAGE_DIST_SQR = PLAYER_MESSAGE_DIST * PLAYER_MESSAGE_DIST;
    private static final double START_CLOSE_DIST_SQR = 0.25;
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
    private String buildFile;
    private BuildRecorder.ReplayBounds buildBounds;
    private BuildRecorder.ReplayBounds effectiveBounds;
    private boolean waitingForClearSpace = false;
    private boolean sentClearSpaceMessage = false;
    private boolean returningToBuildPos = false;
    private BlockPos clearAreaSignPos;
    private boolean buildPosLocked = false;
    private BlockPos resumePos;
    private boolean waitingForResumePos = false;
    private boolean resumePosPaused = false;
    private boolean waitingForMaterials = false;
    private long nextMaterialCheckTick = 0;
    private String pendingMessage;
    private boolean pendingMessageSent = false;
    private long messagePauseUntilTick = 0;
    private boolean completeAfterMessage = false;
    private long clearSpaceWaitUntilTick = 0;
    private long nextClearSpaceCheckTick = 0;
    private static final double BUILD_WAIT_RETURN_DIST_SQR = 9.0;
    private static final double BUILD_WAIT_MAX_DIST_SQR = 36.0;
    private static final int CLEAR_SPACE_PAUSE_TICKS = 60;
    private static final int CLEAR_SPACE_CHECK_INTERVAL_TICKS = 100;
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
            if (waitingForClearSpace && buildPos != null) {
                moveTowardBuildPos(false);
            } else if (!buildPosLocked) {
                reachedPlayer = false;
                buildPos = null;
                moveTowardPlayer(false);
            }
        } else {
            BlockPos cursor = BuildRecorder.getReplayCursor(this.entity);
            if (cursor != null) {
                LOGGER.info("[BuildGoal] resume navigation toward replay cursor {}", cursor);
                this.entity.getNavigation().moveTo(cursor.getX(), cursor.getY() + 1, cursor.getZ(), this.speed);
            }
        }
    }

    @Override
    public void stop() {
        LOGGER.info("[BuildGoal] stop goal pause replay");
        if (completed || this.targetEntity == null || !this.targetEntity.isAlive()) {
            BuildRecorder.cancelReplay(this.entity);
            removeClearAreaSign();
        } else {
            BuildRecorder.pauseReplay(this.entity);
        }
    }

    @Override
    public void tick() {
        if (completed) return;
        if (buildPos != null && !buildPosLocked) {
            buildPosLocked = true;
        }

        if (pendingMessage != null) {
            handlePendingMessage();
            return;
        }

        if (!startedReplay) {
            if (waitingForClearSpace && buildPos != null && buildBounds != null) {
                long now = this.entity.level().getGameTime();
                if (now < clearSpaceWaitUntilTick) {
                    this.setFlags(EnumSet.noneOf(Flag.class));
                    return;
                }
                if (now < nextClearSpaceCheckTick) {
                    moveTowardBuildPos(false);
                    return;
                }
                nextClearSpaceCheckTick = now + CLEAR_SPACE_CHECK_INTERVAL_TICKS;
                if (effectiveBounds != null && !isFloorClearWithLog(buildPos, effectiveBounds)) {
                    moveTowardBuildPos(false);
                    return;
                }
                double distToBuild = this.entity.distanceToSqr(buildPos.getX() + 0.5, buildPos.getY() + 1, buildPos.getZ() + 0.5);
                if (distToBuild > BUILD_WAIT_RETURN_DIST_SQR) {
                    moveTowardBuildPos(true);
                    return;
                }
                waitingForClearSpace = false;
                sentClearSpaceMessage = false;
                returningToBuildPos = false;
                this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
                removeClearAreaSign();
            }
            if (!reachedPlayer) {
                if (isStartCloseToPlayer()) {
                    BlockPos playerPos = BlockPos.containing(this.targetEntity.position());
                    buildPos = findPreferredStartPos(playerPos);
                    reachedPlayer = true;
                    buildPosLocked = true;
                    LOGGER.info("[BuildGoal] reached player choose buildPos {}", buildPos);
                } else {
                    double distToPlayer = this.entity.distanceToSqr(this.targetEntity);
                    moveTowardPlayer(distToPlayer <= 4.0);
                    return;
                }
            }

            this.entity.getNavigation().stop();
            EntityChatData data = ChatDataManager.getServerInstance().getOrCreateChatData(this.entity.getStringUUID());
            int tier = this.entity.getBbHeight() < 1 ? 1 : (this.entity.getBbHeight() < 2 ? 2 : 3);
            if (buildFile == null) {
                buildFile = BuildRecorder.randomBuildFile(this.entity.getBbHeight(), buildType, data.buildLevel);
                buildBounds = buildFile != null ? BuildRecorder.getReplayBounds(buildFile, false) : null;
                LOGGER.info("[BuildGoal] select build skill={} type={} heightTier={} file={}", data.buildLevel, buildType, tier, buildFile);
                effectiveBounds = buildBounds != null ? expandBoundsForEntity(buildBounds) : null;
                if (effectiveBounds != null) {
                    LOGGER.info("[BuildGoal] replay bounds size={}x{}x{} startOffsetMin=({}, {}, {}) startOffsetMax=({}, {}, {})",
                            effectiveBounds.sizeX, effectiveBounds.sizeY, effectiveBounds.sizeZ,
                            effectiveBounds.minX, effectiveBounds.minY, effectiveBounds.minZ,
                            effectiveBounds.maxX, effectiveBounds.maxY, effectiveBounds.maxZ);
                }
            }
            if (buildFile != null && effectiveBounds != null && !isFloorClear(buildPos, effectiveBounds)) {
                waitingForClearSpace = true;
                moveTowardBuildPos(false);
                updateClearAreaSign(buildPos, effectiveBounds);
                if (!sentClearSpaceMessage) {
                    String msg = "In your reply, ask the player to clear a flat " + effectiveBounds.sizeX + "x" + effectiveBounds.sizeZ + " area so you can build safely, and confirm you'll start once it's ready.";
                    if (queueMessage(msg)) {
                        sentClearSpaceMessage = true;
                        clearSpaceWaitUntilTick = this.entity.level().getGameTime() + CLEAR_SPACE_PAUSE_TICKS;
                        nextClearSpaceCheckTick = clearSpaceWaitUntilTick + CLEAR_SPACE_CHECK_INTERVAL_TICKS;
                    }
                }
                return;
            }
            sentClearSpaceMessage = false;
            if (buildFile != null && buildPos != null) {
                removeClearAreaSign();
            }
            if (buildFile != null && BuildRecorder.startReplay((ServerPlayer) this.targetEntity, this.entity, buildFile, 1)) {
                startedReplay = true;
                actualType = (buildType == null || buildType.isEmpty() || "unknown".equalsIgnoreCase(buildType)) ? buildFile.split("/")[0] : buildType;
                LOGGER.info("[BuildGoal] started replay type={} at {}", actualType, buildPos);
            } else {
                String prompt = (buildType == null || buildType.isEmpty())
                        ? "Explain to the player that you don't know how to build that."
                        : "Explain to the player that you don't know how to build a " + buildType + ".";
                if (queueMessage(prompt)) {
                    completeAfterMessage = true;
                }
                LOGGER.info("[BuildGoal] failed to start replay type={}", buildType);
            }
            return;
        }

        if (BuildRecorder.isReplaying(this.entity)) {
            if (waitingForMaterials) {
                long now = this.entity.level().getGameTime();
                if (now < nextMaterialCheckTick) {
                    moveTowardBuildPos(false);
                    return;
                }
                nextMaterialCheckTick = now + CLEAR_SPACE_CHECK_INTERVAL_TICKS;
                if (BuildRecorder.getMissingRecipe(this.entity) != null) {
                    moveTowardBuildPos(false);
                    return;
                }
                waitingForMaterials = false;
                resumePosPaused = false;
                if (resumePos == null) {
                    resumePos = buildPos;
                }
                waitingForResumePos = true;
                moveTowardResumePos();
                return;
            }
            if (waitingForResumePos) {
                moveTowardResumePos();
            } else if (!fetchingMaterials) {
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
                    resumePos = BuildRecorder.getReplayCursor(this.entity);
                    if (resumePos == null) {
                        resumePos = buildPos;
                    }
                    waitingForResumePos = true;
                    resumePosPaused = false;
                    waitingForMaterials = true;
                    nextMaterialCheckTick = this.entity.level().getGameTime() + CLEAR_SPACE_CHECK_INTERVAL_TICKS;
                }
                fetchingMaterials = true;
                if (!sentRecipe) {
                    String nextItem = BuildRecorder.getNextMissingItem(this.entity);
                    if (nextItem == null) nextItem = "unknown";
                    LOGGER.info("[BuildGoal] next missing item={} remaining={}", nextItem, BuildRecorder.recipeToString(recipe));
                    String limited = BuildRecorder.recipeToString(recipe, 2);
                    String msg = "Next item needed: " + nextItem.replace('_', ' ') + ". Build paused - missing inventory items: " + limited + ". In your reply, ask the player for these items and confirm you'll continue building once they arrive.";
                    if (queueMessage(msg)) {
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
                    return;
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
                resumePosPaused = false;
                waitingForMaterials = false;
                this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
                if (resumePos == null) {
                    resumePos = buildPos;
                }
                if (resumePos != null) {
                    waitingForResumePos = true;
                    moveTowardResumePos();
                }
            }
            BlockPos cursor = BuildRecorder.getReplayCursor(this.entity);
            if (cursor != null) {
                loggedMissingCursor = false;
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
                            if (++rerouteAttempts >= 3 && !sentStuckMessage) {
                                String msg = "I can't find where I left off in the build. Please help me get back on track.";
                                if (queueMessage(msg)) {
                                    sentStuckMessage = true;
                                }
                            }
                        }
                    } else {
                        stuckTicks = 0;
                    }
                } else {
                    stuckTicks = 0;
                    rerouteAttempts = 0;
                    if (!fetchingMaterials && !waitingForResumePos) {
                        LOGGER.info("[BuildGoal] resume replay at cursor {} (dist={})", cursor, dist);
                        BuildRecorder.resumeReplay(this.entity);
                    }
                }
            } else if (!loggedMissingCursor) {
                LOGGER.info("[BuildGoal] waiting for replay cursor");
                loggedMissingCursor = true;
            }
        } else if (!finishing && this.targetEntity instanceof ServerPlayer player) {
            finishing = true;
            LOGGER.info("[BuildGoal] replay finished returning to player");
                moveTowardPlayerStopDistance(PLAYER_MESSAGE_DIST);
        } else if (finishing && this.targetEntity instanceof ServerPlayer player) {
            LookControls.lookAtPlayer(player, this.entity);
            if (this.entity.distanceToSqr(player) <= PLAYER_MESSAGE_DIST_SQR) {
                this.entity.getNavigation().stop();
                EntityChatData data = ChatDataManager.getServerInstance().getOrCreateChatData(this.entity.getStringUUID());
                String type = (actualType == null || actualType.isEmpty()) ? "structure" : actualType;
                String msg = "<you have successfully completed the \"" + type + "\" build>";
                if (queueMessage(msg)) {
                    completeAfterMessage = true;
                }
                data.buildLevel = Math.min(5, data.buildLevel + 1);
                ServerPackets.BroadcastEntityMessage(data);
                LOGGER.info("[BuildGoal] completion message sent");
            } else {
                moveTowardPlayerStopDistance(PLAYER_MESSAGE_DIST);
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

    private BlockPos findPreferredStartPos(BlockPos target) {
        BlockPos ground = findGround(target);
        if (isValidBuildPos(ground) &&
                this.entity.getNavigation().createPath(ground.getX(), ground.getY() + 1, ground.getZ(), 1) != null) {
            return ground;
        }
        return findStartPos(target);
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

    private boolean isFloorClear(BlockPos ground, BuildRecorder.ReplayBounds bounds) {
        if (ground == null || bounds == null) {
            return true;
        }
        int baseX = ground.getX();
        int baseZ = ground.getZ();
        int y = ground.getY();
        Level level = this.entity.level();
        for (int x = bounds.minX; x <= bounds.maxX; x++) {
            for (int z = bounds.minZ; z <= bounds.maxZ; z++) {
                BlockPos pos = new BlockPos(baseX + x, y, baseZ + z);
                BlockPos above = pos.above();
                if (clearAreaSignPos != null && clearAreaSignPos.equals(above)) {
                    if (!isSolidGround(level, pos)) {
                        return false;
                    }
                    continue;
                }
                if (!isSolidGround(level, pos) || isSolidGround(level, above)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isFloorClearWithLog(BlockPos ground, BuildRecorder.ReplayBounds bounds) {
        if (ground == null || bounds == null) {
            return true;
        }
        int total = 0;
        int clear = 0;
        int baseX = ground.getX();
        int baseZ = ground.getZ();
        int y = ground.getY();
        Level level = this.entity.level();
        for (int x = bounds.minX; x <= bounds.maxX; x++) {
            for (int z = bounds.minZ; z <= bounds.maxZ; z++) {
                total++;
                BlockPos pos = new BlockPos(baseX + x, y, baseZ + z);
                BlockPos above = pos.above();
                boolean ok;
                if (clearAreaSignPos != null && clearAreaSignPos.equals(above)) {
                    ok = isSolidGround(level, pos);
                } else {
                    ok = isSolidGround(level, pos) && !isSolidGround(level, above);
                }
                if (ok) {
                    clear++;
                }
            }
        }
        int pct = total == 0 ? 0 : (int) Math.round((clear * 100.0) / total);
        LOGGER.info("[BuildGoal] ground clearance {}% ({}/{}) at {}", pct, clear, total, ground);
        return clear == total;
    }

    private BlockPos findNearbyClearStart(BlockPos center, BuildRecorder.ReplayBounds bounds) {
        if (center == null || bounds == null) {
            return null;
        }
        BlockPos ground = findGround(center);
        if (isValidBuildPos(ground) && isFloorClear(ground, bounds)) {
            return ground;
        }
        BlockPos best = null;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            if (pos.equals(center)) {
                continue;
            }
            BlockPos candidate = findGround(pos);
            if (!isValidBuildPos(candidate)) {
                continue;
            }
            if (this.entity.getNavigation().createPath(candidate.getX(), candidate.getY() + 1, candidate.getZ(), 1) == null) {
                continue;
            }
            if (isFloorClear(candidate, bounds)) {
                best = candidate.immutable();
                break;
            }
        }
        return best;
    }

    private void updateClearAreaSign(BlockPos ground, BuildRecorder.ReplayBounds bounds) {
        if (ground == null || bounds == null || this.entity.level().isClientSide) {
            return;
        }
        BlockPos signPos = ground.above();
        if (clearAreaSignPos != null && !clearAreaSignPos.equals(signPos)) {
            removeClearAreaSign();
        }
        Level level = this.entity.level();
        if (clearAreaSignPos == null) {
            if (!level.getBlockState(signPos).isAir()) {
                return;
            }
            level.setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 0), 3);
            clearAreaSignPos = signPos;
        }
        if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            SignText text = sign.getFrontText();
            text = text.setMessage(0, Component.literal("Clear Ground"));
            text = text.setMessage(1, Component.literal(bounds.sizeX + "x" + bounds.sizeZ));
            sign.setText(text, true);
            sign.setChanged();
            level.sendBlockUpdated(signPos, level.getBlockState(signPos), level.getBlockState(signPos), 3);
        }
    }

    private void removeClearAreaSign() {
        if (clearAreaSignPos == null || this.entity.level().isClientSide) {
            return;
        }
        Level level = this.entity.level();
        if (level.getBlockState(clearAreaSignPos).getBlock() instanceof SignBlock) {
            level.removeBlock(clearAreaSignPos, false);
        }
        clearAreaSignPos = null;
    }

    private BuildRecorder.ReplayBounds expandBoundsForEntity(BuildRecorder.ReplayBounds bounds) {
        if (bounds == null) {
            return null;
        }
        int expandXZ = Mth.ceil(this.entity.getBbWidth() / 2.0);
        int expandY = Math.max(0, Mth.ceil(this.entity.getBbHeight()) - 1);
        if (expandXZ == 0 && expandY == 0) {
            return bounds;
        }
        return new BuildRecorder.ReplayBounds(
                bounds.minX - expandXZ,
                bounds.minY - expandY,
                bounds.minZ - expandXZ,
                bounds.maxX + expandXZ,
                bounds.maxY + expandY,
                bounds.maxZ + expandXZ
        );
    }

    private boolean isStartCloseToPlayer() {
        if (this.targetEntity == null) {
            return false;
        }
        AABB a = this.entity.getBoundingBox();
        AABB b = this.targetEntity.getBoundingBox();
        if (a.intersects(b)) {
            return true;
        }
        double dx = Math.max(0.0, Math.max(b.minX - a.maxX, a.minX - b.maxX));
        double dy = Math.max(0.0, Math.max(b.minY - a.maxY, a.minY - b.maxY));
        double dz = Math.max(0.0, Math.max(b.minZ - a.maxZ, a.minZ - b.maxZ));
        return dx * dx + dy * dy + dz * dz <= START_CLOSE_DIST_SQR;
    }

    private void moveTowardPlayer(boolean forceClose) {
        if (this.targetEntity == null) {
            return;
        }
        if (forceClose) {
            this.entity.getNavigation().stop();
            this.entity.getMoveControl().setWantedPosition(
                    this.targetEntity.getX(),
                    this.targetEntity.getY(),
                    this.targetEntity.getZ(),
                    this.speed
            );
        } else {
            this.entity.getNavigation().moveTo(
                    this.targetEntity.getX(),
                    this.targetEntity.getY(),
                    this.targetEntity.getZ(),
                    this.speed
            );
        }
    }

    private boolean queueMessage(String msg) {
        if (pendingMessage != null || msg == null) {
            return false;
        }
        pendingMessage = msg;
        pendingMessageSent = false;
        messagePauseUntilTick = 0;
        return true;
    }

    private void handlePendingMessage() {
        long now = this.entity.level().getGameTime();
        if (now < messagePauseUntilTick) {
            this.entity.getNavigation().stop();
            this.setFlags(EnumSet.noneOf(Flag.class));
            return;
        }
        if (!pendingMessageSent) {
            if (this.targetEntity == null) {
                pendingMessage = null;
                return;
            }
            double distToPlayer = this.entity.distanceToSqr(this.targetEntity);
            if (distToPlayer > PLAYER_MESSAGE_DIST_SQR) {
                moveTowardPlayerStopDistance(PLAYER_MESSAGE_DIST);
                return;
            }
            if (this.targetEntity instanceof ServerPlayer player) {
                EntityChatData data = ChatDataManager.getServerInstance().getOrCreateChatData(this.entity.getStringUUID());
                LookControls.lookAtPlayer(player, this.entity);
                ServerPackets.generate_chat("N/A", data, player, this.entity, pendingMessage, true);
            }
            pendingMessageSent = true;
            messagePauseUntilTick = now + CLEAR_SPACE_PAUSE_TICKS;
            return;
        }
        pendingMessage = null;
        pendingMessageSent = false;
        if (completeAfterMessage) {
            completeAfterMessage = false;
            completed = true;
        }
    }

    private void moveTowardBuildPos(boolean forceReturn) {
        if (buildPos == null) {
            return;
        }
        double dist = this.entity.distanceToSqr(buildPos.getX() + 0.5, buildPos.getY() + 1, buildPos.getZ() + 0.5);
        boolean shouldReturn = forceReturn || dist > BUILD_WAIT_MAX_DIST_SQR || returningToBuildPos;
        if (shouldReturn && dist > BUILD_WAIT_RETURN_DIST_SQR) {
            returningToBuildPos = true;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
            moveTowardTarget(buildPos.getX() + 0.5, buildPos.getY() + 1, buildPos.getZ() + 0.5);
            return;
        }
        if (returningToBuildPos) {
            this.entity.getNavigation().stop();
        }
        returningToBuildPos = false;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    private void moveTowardPlayerStopDistance(double stopDistance) {
        if (this.targetEntity == null) {
            return;
        }
        Vec3 playerPos = this.targetEntity.position();
        Vec3 entityPos = this.entity.position();
        Vec3 offset = entityPos.subtract(playerPos);
        double len = offset.length();
        if (len < 0.001) {
            this.entity.getNavigation().moveTo(this.targetEntity, this.speed);
            return;
        }
        Vec3 targetPos = playerPos.add(offset.scale(stopDistance / len));
        moveTowardTarget(targetPos.x, targetPos.y, targetPos.z);
    }

    private void moveTowardResumePos() {
        if (resumePos == null) {
            waitingForResumePos = false;
            return;
        }
        if (!resumePosPaused) {
            BuildRecorder.pauseReplay(this.entity);
            resumePosPaused = true;
        }
        double dist = this.entity.distanceToSqr(resumePos.getX() + 0.5, resumePos.getY() + 1, resumePos.getZ() + 0.5);
        if (dist > 4.0) {
            moveTowardTarget(resumePos.getX() + 0.5, resumePos.getY() + 1, resumePos.getZ() + 0.5);
            return;
        }
        waitingForResumePos = false;
        resumePos = null;
        this.entity.getNavigation().stop();
        if (!fetchingMaterials) {
            BuildRecorder.resumeReplay(this.entity);
        }
    }

    private void moveTowardTarget(double x, double y, double z) {
        if (this.entity instanceof PathfinderMob) {
            this.entity.getNavigation().moveTo(x, y, z, this.speed);
            return;
        }
        LookControls.lookAtPosition(new Vec3(x, y, z), this.entity);
        Vec3 entityPos = this.entity.position();
        Vec3 moveDirection = new Vec3(x, y, z).subtract(entityPos).normalize();
        double currentSpeed = this.entity.getDeltaMovement().horizontalDistance();
        currentSpeed = Mth.approach((float) currentSpeed, (float) this.speed,
                (float) (0.005 * (this.speed / Math.max(currentSpeed, 0.1))));
        Vec3 newVelocity = new Vec3(moveDirection.x * currentSpeed, moveDirection.y * currentSpeed,
                moveDirection.z * currentSpeed);
        this.entity.setDeltaMovement(newVelocity);
        this.entity.hurtMarked = true;
    }
}
