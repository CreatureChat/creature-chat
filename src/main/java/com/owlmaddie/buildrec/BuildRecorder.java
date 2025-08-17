// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.buildrec;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility to record and replay player build actions.
 */
public class BuildRecorder {
    private static final Gson GSON = new Gson();
    private static final Map<UUID, Recording> RECORDINGS = new ConcurrentHashMap<>();
    private static final List<Replay> REPLAYS = new ArrayList<>();

    static {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide || !(player instanceof ServerPlayer sp)) return net.minecraft.world.InteractionResult.PASS;
            BlockPos target = hitResult.getBlockPos();
            BlockPos placePos = target.relative(hitResult.getDirection());
            BlockState beforeTarget = world.getBlockState(target);
            BlockState beforePlace = world.getBlockState(placePos);
            double px = player.getX(), py = player.getY(), pz = player.getZ();
            float yaw = player.getYRot(), pitch = player.getXRot();
            world.getServer().execute(() -> {
                BlockState afterPlace = world.getBlockState(placePos);
                if (!afterPlace.isAir() && !afterPlace.equals(beforePlace)) {
                    recordPlace(sp, placePos, afterPlace, px, py, pz, yaw, pitch);
                } else {
                    BlockState afterTarget = world.getBlockState(target);
                    if (!afterTarget.equals(beforeTarget)) {
                        recordInteract(sp, target, afterTarget, px, py, pz, yaw, pitch);
                    }
                }
            });
            return net.minecraft.world.InteractionResult.PASS;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer sp) {
                double px = player.getX(), py = player.getY(), pz = player.getZ();
                float yaw = player.getYRot(), pitch = player.getXRot();
                recordBreak(sp, (ServerLevel) world, pos, state, px, py, pz, yaw, pitch);
            }
        });
        ServerTickEvents.START_SERVER_TICK.register(BuildRecorder::tick);
    }

    public static void init() {
        // Ensure static initializer runs
    }

    public static boolean start(ServerPlayer player) {
        if (RECORDINGS.containsKey(player.getUUID())) {
            return false;
        }
        RECORDINGS.put(player.getUUID(), new Recording(player));
        return true;
    }

    public static Summary stop(ServerPlayer player, String name) {
        Recording rec = RECORDINGS.remove(player.getUUID());
        if (rec == null) return null;
        return rec.save(name);
    }

    public static boolean startReplay(ServerPlayer player, String fileName, int speed) {
        Path dir = buildDir();
        Path file = dir.resolve(fileName);
        if (!Files.exists(file)) return false;
        try (JsonReader reader = new JsonReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8))) {
            List<Action> actions = new ArrayList<>();
            reader.beginArray();
            while (reader.hasNext()) {
                actions.add(GSON.fromJson(reader, Action.class));
            }
            reader.endArray();
            ServerLevel level = (ServerLevel) player.level();
            Pig pig = new Pig(EntityType.PIG, level);
            pig.teleportTo(player.getX(), player.getY(), player.getZ());
            pig.setYRot(player.getYRot());
            pig.setXRot(player.getXRot());
            pig.yHeadRot = player.getYRot();
            pig.yBodyRot = player.getYRot();
            pig.setNoAi(true);
            pig.setInvulnerable(true);
            level.addFreshEntity(pig);
            REPLAYS.add(new Replay(pig, actions, speed));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void recordPlace(ServerPlayer player, BlockPos pos, BlockState state,
                                    double px, double py, double pz, float yaw, float pitch) {
        Recording rec = RECORDINGS.get(player.getUUID());
        if (rec != null) {
            long tick = player.level().getServer().getTickCount();
            rec.addAction(tick, "place", pos, state, px, py, pz, yaw, pitch);
            if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
                BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
                BlockState otherState = player.level().getBlockState(otherPos);
                rec.addAction(tick, "place", otherPos, otherState, px, py, pz, yaw, pitch);
            }
        }
    }

    private static void recordBreak(ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state,
                                    double px, double py, double pz, float yaw, float pitch) {
        Recording rec = RECORDINGS.get(player.getUUID());
        if (rec != null) {
            long tick = player.level().getServer().getTickCount();
            rec.addAction(tick, "break", pos, state, px, py, pz, yaw, pitch);
            if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
                BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
                rec.addAction(tick, "break", otherPos, state, px, py, pz, yaw, pitch);
            }
        }
    }

    private static void recordInteract(ServerPlayer player, BlockPos pos, BlockState state,
                                       double px, double py, double pz, float yaw, float pitch) {
        Recording rec = RECORDINGS.get(player.getUUID());
        if (rec != null) {
            long tick = player.level().getServer().getTickCount();
            rec.addAction(tick, "interact", pos, state, px, py, pz, yaw, pitch);
            if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
                BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
                BlockState otherState = player.level().getBlockState(otherPos);
                rec.addAction(tick, "interact", otherPos, otherState, px, py, pz, yaw, pitch);
            }
        }
    }

    private static void tick(MinecraftServer server) {
        RECORDINGS.forEach((uuid, rec) -> {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) {
                rec.tick(p);
            }
        });

        Iterator<Replay> it = REPLAYS.iterator();
        while (it.hasNext()) {
            Replay r = it.next();
            double advance = r.speed;
            while (advance > 0) {
                if (r.action == null) {
                    if (r.index >= r.actions.size()) {
                        r.pig.discard();
                        it.remove();
                        break;
                    }
                    r.action = r.actions.get(r.index++);
                    r.progress = 0;
                    r.sx = r.pig.getX();
                    r.sy = r.pig.getY();
                    r.sz = r.pig.getZ();
                    r.syaw = r.pig.getYRot();
                    r.spitch = r.pig.getXRot();
                    r.tx = r.baseX + r.action.px;
                    r.ty = r.baseY + r.action.py;
                    r.tz = r.baseZ + r.action.pz;
                    r.tyaw = r.action.yaw;
                    r.tpitch = r.action.pitch;
                }
                double remain = r.action.dt - r.progress;
                if (remain <= advance) {
                    r.progress += remain;
                    advance -= remain;
                    r.pig.teleportTo(r.tx, r.ty, r.tz);
                    r.pig.setYRot(r.tyaw);
                    r.pig.setXRot(r.tpitch);
                    r.pig.yHeadRot = r.tyaw;
                    r.pig.yBodyRot = r.tyaw;
                    if ("place".equals(r.action.action) || "break".equals(r.action.action) || "interact".equals(r.action.action)) {
                        BlockPos bpos = new BlockPos(Mth.floor(r.baseX + r.action.bx), Mth.floor(r.baseY + r.action.by), Mth.floor(r.baseZ + r.action.bz));
                        r.pig.level().getChunkAt(bpos);
                        if ("place".equals(r.action.action)) {
                            BlockState state = Block.stateById(r.action.stateId);
                            boolean upper = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
                            r.pig.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(state.getBlock()));
                            r.pig.level().setBlock(bpos, state, 3);
                            if (!upper) {
                                r.pig.level().playSound(null, bpos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1f, 1f);
                                r.pig.swing(InteractionHand.MAIN_HAND);
                            }
                        } else if ("break".equals(r.action.action)) {
                            BlockState state = r.pig.level().getBlockState(bpos);
                            boolean upper = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
                            if (!upper) {
                                r.pig.level().levelEvent(2001, bpos, Block.getId(state));
                                r.pig.swing(InteractionHand.MAIN_HAND);
                            }
                            r.pig.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                            r.pig.level().removeBlock(bpos, false);
                        } else {
                            BlockState state = Block.stateById(r.action.stateId);
                            boolean upper = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
                            r.pig.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                            r.pig.level().setBlock(bpos, state, 3);
                            if (!upper) {
                                r.pig.level().playSound(null, bpos, state.getSoundType().getHitSound(), SoundSource.BLOCKS, 1f, 1f);
                                r.pig.swing(InteractionHand.MAIN_HAND);
                            }
                        }
                    }
                    r.action = null;
                } else {
                    r.progress += advance;
                    double t = r.progress / r.action.dt;
                    double px = Mth.lerp(t, r.sx, r.tx);
                    double py = Mth.lerp(t, r.sy, r.ty);
                    double pz = Mth.lerp(t, r.sz, r.tz);
                    r.pig.teleportTo(px, py, pz);
                    r.pig.setYRot(r.syaw);
                    r.pig.setXRot(r.spitch);
                    r.pig.yHeadRot = r.syaw;
                    r.pig.yBodyRot = r.syaw;
                    advance = 0;
                }
            }
        }
    }

    private static Path buildDir() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("creaturechat").resolve("builds");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        return dir;
    }


    public static class Summary {
        public final String id;
        public final int total;
        public final int additions;
        public final int destroys;

        public Summary(String id, int total, int additions, int destroys) {
            this.id = id;
            this.total = total;
            this.additions = additions;
            this.destroys = destroys;
        }
    }

    private static class Recording {
        final int ox, oy, oz;
        final List<Action> actions = new ArrayList<>();
        int additions = 0;
        int destroys = 0;
        int poseTick = 0;
        long lastTick;
        double lastPx, lastPy, lastPz;
        float lastYaw, lastPitch;
        boolean poseInit = false;

        Recording(ServerPlayer player) {
            BlockPos p = player.blockPosition();
            this.ox = p.getX();
            this.oy = p.getY();
            this.oz = p.getZ();
            this.lastTick = player.level().getServer().getTickCount();
        }

        void tick(ServerPlayer player) {
            if (++poseTick >= 4) {
                poseTick = 0;
                double px = player.getX();
                double py = player.getY();
                double pz = player.getZ();
                float yaw = player.getYRot();
                float pitch = player.getXRot();
                if (!poseInit || px != lastPx || py != lastPy || pz != lastPz || yaw != lastYaw || pitch != lastPitch) {
                    addPose(player.level().getServer().getTickCount(), px, py, pz, yaw, pitch);
                    lastPx = px;
                    lastPy = py;
                    lastPz = pz;
                    lastYaw = yaw;
                    lastPitch = pitch;
                    poseInit = true;
                }
            }
        }

        void addPose(long tick, double px, double py, double pz, float yaw, float pitch) {
            Action a = new Action();
            a.action = "pose";
            a.dt = (int)(tick - lastTick);
            lastTick = tick;
            a.px = px - ox;
            a.py = py - oy;
            a.pz = pz - oz;
            a.yaw = yaw;
            a.pitch = pitch;
            actions.add(a);
        }

        void addAction(long tick, String type, BlockPos pos, BlockState state,
                       double px, double py, double pz, float yaw, float pitch) {
            Action a = new Action();
            a.action = type;
            a.dt = (int)(tick - lastTick);
            lastTick = tick;
            a.stateId = Block.getId(state);
            a.bx = pos.getX() - ox;
            a.by = pos.getY() - oy;
            a.bz = pos.getZ() - oz;
            a.px = px - ox;
            a.py = py - oy;
            a.pz = pz - oz;
            a.yaw = yaw;
            a.pitch = pitch;
            actions.add(a);
            if ("place".equals(type)) additions++;
            else if ("break".equals(type)) destroys++;
        }

        Summary save(String name) {
            String base = (name == null || name.isBlank()) ? UUID.randomUUID().toString().split("-")[0] : name.replaceAll("[^a-zA-Z0-9-_]", "_");
            String id = base + ".json.gz";
            Path file = buildDir().resolve(id);
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(file)), StandardCharsets.UTF_8))) {
                w.write("[\n");
                for (int i = 0; i < actions.size(); i++) {
                    w.write(GSON.toJson(actions.get(i)));
                    if (i < actions.size() - 1) w.write(",\n");
                }
                w.write("\n]");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new Summary(id, actions.size(), additions, destroys);
        }
    }

    private static class Replay {
        final Pig pig;
        final List<Action> actions;
        final double baseX, baseY, baseZ;
        final int speed;
        int index = 0;
        Action action = null;
        double progress = 0;
        double sx, sy, sz, tx, ty, tz;
        float syaw, spitch, tyaw, tpitch;

        Replay(Pig pig, List<Action> actions, int speed) {
            this.pig = pig;
            this.actions = actions;
            this.speed = speed;
            BlockPos p = pig.blockPosition();
            this.baseX = p.getX();
            this.baseY = p.getY();
            this.baseZ = p.getZ();
        }
    }

    private static class Action {
        String action;
        int stateId;
        int bx, by, bz;
        int dt;
        double px, py, pz;
        float yaw, pitch;
    }
}

