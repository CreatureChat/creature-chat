// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.buildrec;

import com.google.gson.JsonParseException;
import com.owlmaddie.buildrec.BuildRecordIO.Action;
import com.owlmaddie.buildrec.BuildRecordIO.Meta;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.Container;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

import com.owlmaddie.inventory.ChatInventory;
import com.owlmaddie.goals.BuildPlayerGoal;
import com.owlmaddie.goals.EntityBehaviorManager;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

/**
 * Utility to record and replay player build actions.
 */
public class BuildRecorder {
    private static final Map<UUID, Recording> RECORDINGS = new ConcurrentHashMap<>();
    private static final List<Replay> REPLAYS = new ArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static final int MAX_IDLE_TICKS = 20; // 1 second
    private static List<BuildIndex> BUILD_INDEX;
    private static final Map<Mob, Map<String, Integer>> MISSING_RECIPES = new ConcurrentHashMap<>();

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
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> cancelAllReplays());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cancelAllReplays());
    }

    public static void init() {
        // Ensure static initializer runs
    }

    private static class BuildIndex {
        final String type;
        final String height;
        final String file;
        final int score;

        BuildIndex(String type, String height, String file, int score) {
            this.type = type;
            this.height = height;
            this.file = file;
            this.score = score;
        }
    }

    private static int heightTier(double h) {
        return h < 1 ? 1 : (h < 2 ? 2 : 3);
    }

    private static void loadBuildIndex() {
        if (BUILD_INDEX != null) return;
        List<BuildIndex> list = new ArrayList<>();
        FabricLoader.getInstance().getModContainer("creaturechat")
                .flatMap(m -> m.findPath("assets/creaturechat/builds/index.json"))
                .ifPresent(path -> {
                    try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                        JsonObject root = BuildRecordIO.GSON.fromJson(reader, JsonObject.class);
                        JsonArray arr = root.getAsJsonArray("builds");
                        for (var el : arr) {
                            JsonObject o = el.getAsJsonObject();
                            list.add(new BuildIndex(o.get("type").getAsString(), o.get("height").getAsString(),
                                    o.get("file").getAsString(), o.get("score").getAsInt()));
                        }
                    } catch (IOException | JsonParseException e) {
                        LOGGER.error("[BuildRec] failed to read build index", e);
                    }
                });
        BUILD_INDEX = list;
    }

    public static String randomBuildFile(double entityHeight, String type, int level) {
        loadBuildIndex();
        int tier = heightTier(entityHeight);
        String t = (type == null || type.isEmpty() || "unknown".equalsIgnoreCase(type)) ? null : type.toLowerCase();
        List<BuildIndex> filtered = new ArrayList<>();
        for (BuildIndex e : BUILD_INDEX) {
            if (t != null && !e.type.equalsIgnoreCase(t)) continue;
            if (!"any".equalsIgnoreCase(e.height) && Integer.parseInt(e.height) < tier) continue;
            if (e.score > level) continue;
            filtered.add(e);
        }
        if (filtered.isEmpty()) return null;
        BuildIndex pick = filtered.get(new Random().nextInt(filtered.size()));
        String path = pick.type + ("any".equalsIgnoreCase(pick.height) ? "" : "/" + pick.height) + "/" + pick.file;
        return path;
    }

    public static boolean start(ServerPlayer player, String type, String height, String name) {
        if (RECORDINGS.containsKey(player.getUUID())) {
            LOGGER.info("[BuildRec] start ignored already recording player={}", player.getGameProfile().getName());
            return false;
        }
        RECORDINGS.put(player.getUUID(), new Recording(player, type, height, name));
        LOGGER.info("[BuildRec] start type={} height={} name={} player={}", type, height, name, player.getGameProfile().getName());
        player.getServer().getCommands().sendCommands(player);
        return true;
    }

    public static boolean isRecording(ServerPlayer player) {
        return RECORDINGS.containsKey(player.getUUID());
    }

    public static Summary stop(ServerPlayer player) {
        Recording rec = RECORDINGS.remove(player.getUUID());
        if (rec == null) {
            LOGGER.info("[BuildRec] stop ignored no active recording player={}", player.getGameProfile().getName());
            return null;
        }
        Summary summary = rec.finish(player);
        LOGGER.info("[BuildRec] stop player={} file={} placed={} destroys={} unique={} size={}x{}x{}",
                player.getGameProfile().getName(), summary.id, summary.finalBlocks, summary.destroys, summary.uniqueBlocks,
                summary.sizeX, summary.sizeZ, summary.sizeY);
        player.getServer().getCommands().sendCommands(player);
        return summary;
    }

    public static boolean startReplay(ServerPlayer player, String fileName, EntityType<? extends Mob> entityType, int speed) {
        return startReplayInternal(player, fileName, entityType, speed, null);
    }

    @SuppressWarnings("unchecked")
    public static boolean startReplay(ServerPlayer player, Mob actor, String fileName, int speed) {
        return startReplayInternal(player, fileName, (EntityType<? extends Mob>) actor.getType(), speed, actor);
    }

    private static boolean startReplayInternal(ServerPlayer player, String fileName, EntityType<? extends Mob> entityType, int speed, Mob existingActor) {
        Path dir = buildRootDir();
        String actual = fileName.endsWith(".json.gz") ? fileName : fileName + ".json.gz";
        Path file = dir.resolve(actual);
        LOGGER.info("[BuildRec] replay file={} entity={} speed={} player={}", actual,
                entityType != null ? BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString() : "pig", speed,
                player.getGameProfile().getName());
        if (!Files.exists(file)) {
            LOGGER.info("[BuildRec] replay missing file={}", file);
            return false;
        }
        try {
            BuildRecordIO.Loaded loaded = BuildRecordIO.read(file);
            List<Action> actions = loaded.actions;
            if (actions.isEmpty()) {
                LOGGER.info("[BuildRec] replay file={} has no actions", file);
                return false;
            }
            Meta meta = loaded.meta;
            double recEye = meta.eyeHeight > 0 ? meta.eyeHeight : player.getEyeHeight();
            double recWidth = meta.bbWidth;
            double recHeight = meta.bbHeight;
            List<BlockState> palette = new ArrayList<>();
            var lookup = player.level().registryAccess().lookupOrThrow(Registries.BLOCK);
            for (String s : meta.palette) {
                try {
                    var res = BlockStateParser.parseForBlock(lookup, new StringReader(s), false);
                    palette.add(res.blockState());
                } catch (CommandSyntaxException e) {
                    LOGGER.error("[BuildRec] invalid block state {}", s, e);
                    palette.add(Blocks.AIR.defaultBlockState());
                }
            }
            ServerLevel level = (ServerLevel) player.level();
            Mob actor;
            boolean preserve = existingActor != null;
            if (preserve) {
                actor = existingActor;
            } else if (entityType == null) {
                actor = new Pig(EntityType.PIG, level);
            } else {
                actor = MobHelper.create(entityType, level);
                if (actor == null) {
                    LOGGER.info("[BuildRec] replay could not create {}", BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
                    return false;
                }
            }
            MobHelper.initSpawn(actor, level);
            if (!preserve) {
                actor.teleportTo(player.getX(), player.getY(), player.getZ());
                actor.setYRot(player.getYRot());
                float sp = adjustPitch(level, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), recEye, actor.getEyeHeight(), actor);
                actor.setXRot(sp);
                actor.yHeadRot = player.getYRot();
                actor.yBodyRot = player.getYRot();
                level.addFreshEntity(actor);
            }
            actor.setNoAi(true);
            actor.setInvulnerable(true);
            actor.setPersistenceRequired();
            REPLAYS.add(new Replay(actor, actions, speed, recEye, recWidth, recHeight, palette, preserve, player.isCreative()));
            LOGGER.info("[BuildRec] replay loaded actions={} eyeHeight={} bbW={} bbH={}", actions.size(), recEye, recWidth, recHeight);
            return true;
        } catch (IOException | JsonParseException e) {
            LOGGER.error("[BuildRec] replay failed to load {}", file, e);
        } catch (Exception e) {
            LOGGER.error("[BuildRec] replay runtime error {}", file, e);
        }
        return false;
    }

    public static void pauseReplay(Mob actor) {
        REPLAYS.stream().filter(r -> r.actor == actor).forEach(r -> {
            r.paused = true;
            r.actor.setNoAi(false);
        });
    }

    public static void resumeReplay(Mob actor) {
        REPLAYS.stream().filter(r -> r.actor == actor).forEach(r -> {
            r.paused = false;
            r.actor.setNoAi(true);
            MISSING_RECIPES.remove(actor);
        });
    }

    public static void cancelReplay(Mob actor) {
        Iterator<Replay> it = REPLAYS.iterator();
        while (it.hasNext()) {
            Replay r = it.next();
            if (r.actor != actor) {
                continue;
            }
            r.actor.setNoAi(false);
            r.actor.setInvulnerable(false);
            MISSING_RECIPES.remove(r.actor);
            if (!r.preserveActor) {
                r.actor.discard();
            }
            it.remove();
            break;
        }
    }

    public static void cancelAllReplays() {
        Iterator<Replay> it = REPLAYS.iterator();
        while (it.hasNext()) {
            Replay r = it.next();
            r.actor.setNoAi(false);
            r.actor.setInvulnerable(false);
            MISSING_RECIPES.remove(r.actor);
            EntityBehaviorManager.removeGoal(r.actor, BuildPlayerGoal.class);
            if (!r.preserveActor) {
                r.actor.discard();
            }
            it.remove();
        }
    }

    public static boolean isReplaying(Mob actor) {
        return REPLAYS.stream().anyMatch(r -> r.actor == actor);
    }

    public static Map<String, Integer> getMissingRecipe(Mob actor) {
        return MISSING_RECIPES.get(actor);
    }

    public static String getNextMissingItem(Mob actor) {
        for (Replay r : REPLAYS) {
            if (r.actor == actor && r.requiredItem != null) {
                return BuiltInRegistries.ITEM.getKey(r.requiredItem).getPath();
            }
        }
        return null;
    }

    public static String recipeToString(Map<String, Integer> recipe) {
        return recipeToString(recipe, 0);
    }

    public static String recipeToString(Map<String, Integer> recipe, int limit) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Integer> e : recipe.entrySet()) {
            if (limit > 0 && i >= limit) break;
            if (i++ > 0) sb.append(", ");
            sb.append(e.getValue()).append(" ").append(e.getKey().replace('_', ' '));
        }
        return sb.toString();
    }

    public static String recipeToDisplayString(Map<String, Integer> recipe) {
        return recipeToDisplayString(recipe, 0);
    }

    public static String recipeToDisplayString(Map<String, Integer> recipe, int limit) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Integer> e : recipe.entrySet()) {
            if (limit > 0 && i >= limit) break;
            ResourceLocation id = ResourceLocation.tryParse(e.getKey());
            Item item = RegistryUtil.getItem(id);
            String name = item != null ? new ItemStack(item).getHoverName().getString() : e.getKey().replace('_', ' ');
            if (i++ > 0) sb.append(", ");
            sb.append(e.getValue()).append(" x ").append(name);
        }
        return sb.toString();
    }

    private static Map<String, Integer> computeRemainingRecipe(Replay r) {
        Map<String, Integer> recipe = new LinkedHashMap<>();
        for (int i = r.index - 1; i < r.actions.size(); i++) {
            Action a = r.actions.get(i);
            if ("place".equals(a.action)) {
                BlockState st = r.palette.get(a.blockId);
                String name = BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath();
                recipe.merge(name, 1, Integer::sum);
            }
        }
        return recipe;
    }

    private static boolean consume(Container inv, Item item) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inv.setItem(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    public static BlockPos getReplayCursor(Mob actor) {
        for (Replay r : REPLAYS) {
            if (r.actor == actor) {
                if (r.action != null) {
                    return new BlockPos(Mth.floor(r.tx), Mth.floor(r.ty), Mth.floor(r.tz));
                }
                return new BlockPos(Mth.floor(r.baseX), Mth.floor(r.baseY), Mth.floor(r.baseZ));
            }
        }
        return null;
    }

    private static float adjustPitch(ServerLevel level, double x, double y, double z, float yaw, float pitch,
                                     double fromEye, double toEye, Entity ctx) {
        if (fromEye <= 0 || toEye <= 0) return pitch;
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double dx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dy = -Math.sin(pitchRad);
        double dz = Math.cos(yawRad) * Math.cos(pitchRad);
        Vec3 start = new Vec3(x, y + fromEye, z);
        Vec3 end = start.add(dx * 64, dy * 64, dz * 64);
        var hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ctx));
        Vec3 target = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        Vec3 newEye = new Vec3(x, y + toEye, z);
        Vec3 diff = target.subtract(newEye);
        double horiz = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        return (float) -Math.toDegrees(Math.atan2(diff.y, horiz));
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
            if (r.paused) {
                if (MISSING_RECIPES.containsKey(r.actor) && r.requiredItem != null && !r.creative && r.actor instanceof ChatInventory inv) {
                    Container c = inv.creaturechat$getInventory();
                    if (consume(c, r.requiredItem)) {
                        MISSING_RECIPES.remove(r.actor);
                        r.requiredItem = null;
                    }
                }
                if (r.paused) {
                    continue;
                }
            }
            double advance = r.speed;
            while (advance > 0) {
                if (r.action == null) {
                    if (r.index >= r.actions.size()) {
                        LOGGER.info("[BuildRec] replay finished actor={} actions={} speed={}",
                                r.actor.getType().toShortString(), r.actions.size(), r.speed);
                        if (r.preserveActor) {
                            r.actor.setNoAi(false);
                        } else {
                            r.actor.discard();
                        }
                        it.remove();
                        break;
                    }
                    r.action = r.actions.get(r.index++);
                    r.progress = 0;
                    r.sx = r.actor.getX();
                    r.sy = r.actor.getY();
                    r.sz = r.actor.getZ();
                    r.syaw = r.actor.getYRot();
                    r.spitch = r.actor.getXRot();
                    r.tx = r.baseX + r.action.px;
                    r.ty = r.baseY + r.action.py;
                    r.tz = r.baseZ + r.action.pz;
                    r.tyaw = r.action.yaw;
                    r.tpitch = adjustPitch((ServerLevel) r.actor.level(), r.baseX + r.action.px, r.baseY + r.action.py, r.baseZ + r.action.pz, r.action.yaw, r.action.pitch, r.recordEyeHeight, r.actor.getEyeHeight(), r.actor);
                }
                double remain = r.action.dt - r.progress;
                if (remain <= advance) {
                    r.progress += remain;
                    advance -= remain;
                    r.actor.teleportTo(r.tx, r.ty, r.tz);
                    r.actor.setYRot(r.tyaw);
                    r.actor.setXRot(r.tpitch);
                    r.actor.yHeadRot = r.tyaw;
                    r.actor.yBodyRot = r.tyaw;
                    if ("place".equals(r.action.action) || "break".equals(r.action.action) || "interact".equals(r.action.action)) {
                        BlockPos bpos = new BlockPos(Mth.floor(r.baseX + r.action.bx), Mth.floor(r.baseY + r.action.by), Mth.floor(r.baseZ + r.action.bz));
                        r.actor.level().getChunkAt(bpos);
                        if ("place".equals(r.action.action)) {
                            BlockState state = r.palette.get(r.action.blockId);
                            if (!r.creative) {
                                Item item = state.getBlock().asItem();
                                Container c = (r.actor instanceof ChatInventory inv) ? inv.creaturechat$getInventory() : null;
                                if (c == null || !consume(c, item)) {
                                    MISSING_RECIPES.put(r.actor, computeRemainingRecipe(r));
                                    r.requiredItem = state.getBlock().asItem();
                                    String missingName = BuiltInRegistries.ITEM.getKey(r.requiredItem).getPath();
                                    LOGGER.info("[BuildRec] next missing item={} remaining={}", missingName, recipeToString(MISSING_RECIPES.get(r.actor)));
                                    pauseReplay(r.actor);
                                    advance = 0;
                                    break;
                                }
                            }
                            boolean upper = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
                            r.actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(state.getBlock()));
                            r.actor.level().setBlock(bpos, state, 3);
                            if (!upper) {
                                r.actor.level().playSound(null, bpos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1f, 1f);
                                r.actor.swing(InteractionHand.MAIN_HAND);
                            }
                        } else if ("break".equals(r.action.action)) {
                            BlockState state = r.actor.level().getBlockState(bpos);
                            boolean upper = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
                            if (!upper) {
                                r.actor.level().levelEvent(2001, bpos, Block.getId(state));
                                r.actor.swing(InteractionHand.MAIN_HAND);
                            }
                            r.actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                            r.actor.level().removeBlock(bpos, false);
                        } else {
                            BlockState state = r.palette.get(r.action.blockId);
                            boolean upper = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
                            r.actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                            r.actor.level().setBlock(bpos, state, 3);
                            if (!upper) {
                                r.actor.level().playSound(null, bpos, state.getSoundType().getHitSound(), SoundSource.BLOCKS, 1f, 1f);
                                r.actor.swing(InteractionHand.MAIN_HAND);
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
                    r.actor.teleportTo(px, py, pz);
                    r.actor.setYRot(r.syaw);
                    r.actor.setXRot(r.spitch);
                    r.actor.yHeadRot = r.syaw;
                    r.actor.yBodyRot = r.syaw;
                    advance = 0;
                }
            }
        }
    }

    private static Path buildRootDir() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("creaturechat").resolve("builds");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        return dir;
    }

    private static Path buildDir(String type, String height) {
        Path dir = buildRootDir().resolve(type);
        if (height != null && !height.equalsIgnoreCase("any")) {
            dir = dir.resolve(height);
        }
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
        public final Map<String, Integer> recipe;
        public final int uniqueBlocks;
        public final int sizeX;
        public final int sizeY;
        public final int sizeZ;
        public final int finalBlocks;

        public Summary(String id, int total, int additions, int destroys,
                       Map<String, Integer> recipe, int uniqueBlocks,
                       int sizeX, int sizeY, int sizeZ, int finalBlocks) {
            this.id = id;
            this.total = total;
            this.additions = additions;
            this.destroys = destroys;
            this.recipe = recipe;
            this.uniqueBlocks = uniqueBlocks;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.finalBlocks = finalBlocks;
        }
    }

    private static class Recording {
        final int ox, oy, oz;
        final List<Action> actions = new ArrayList<>();
        final Map<BlockPos, BlockState> finalBlocks = new HashMap<>();
        final Map<BlockState, Integer> stateIds = new LinkedHashMap<>();
        final List<String> statePalette = new ArrayList<>();
        final String name;
        final String type;
        final String height;
        final double eyeHeight;
        final double bbWidth;
        final double bbHeight;
        int additions = 0;
        int destroys = 0;
        int poseTick = 0;
        long lastTick;
        double lastPx, lastPy, lastPz;
        float lastYaw, lastPitch;
        boolean poseInit = false;

        Recording(ServerPlayer player, String type, String height, String name) {
            this.name = name;
            this.type = type;
            this.height = height;
            BlockPos p = player.blockPosition();
            this.ox = p.getX();
            this.oy = p.getY();
            this.oz = p.getZ();
            this.lastTick = player.level().getServer().getTickCount();
            this.eyeHeight = player.getEyeHeight();
            this.bbWidth = player.getBbWidth();
            this.bbHeight = player.getBbHeight();
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
            int dt = (int)(tick - lastTick);
            a.dt = dt > MAX_IDLE_TICKS ? MAX_IDLE_TICKS : dt;
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
            int dt = (int)(tick - lastTick);
            a.dt = dt > MAX_IDLE_TICKS ? MAX_IDLE_TICKS : dt;
            lastTick = tick;
            int id = stateIds.computeIfAbsent(state, s -> {
                int idx = statePalette.size();
                statePalette.add(encodeState(s));
                return idx;
            });
            a.blockId = id;
            a.bx = pos.getX() - ox;
            a.by = pos.getY() - oy;
            a.bz = pos.getZ() - oz;
            a.px = px - ox;
            a.py = py - oy;
            a.pz = pz - oz;
            a.yaw = yaw;
            a.pitch = pitch;
            actions.add(a);
            BlockPos rel = new BlockPos(a.bx, a.by, a.bz);
            if ("place".equals(type)) {
                additions++;
                finalBlocks.put(rel, state);
            } else if ("break".equals(type)) {
                destroys++;
                finalBlocks.remove(rel);
            }
        }

        private static String encodeState(BlockState state) {
            StringBuilder sb = new StringBuilder();
            sb.append(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
            Map<Property<?>, Comparable<?>> props = state.getValues();
            if (!props.isEmpty()) {
                sb.append('[');
                boolean first = true;
                for (Map.Entry<Property<?>, Comparable<?>> e : props.entrySet()) {
                    if (!first) sb.append(',');
                    @SuppressWarnings("rawtypes") Property p = (Property) e.getKey();
                    sb.append(p.getName()).append('=').append(p.getName(e.getValue()));
                    first = false;
                }
                sb.append(']');
            }
            return sb.toString();
        }
        Summary save() {
            String base = (name == null || name.isBlank()) ? UUID.randomUUID().toString().split("-")[0] : name.replaceAll("[^a-zA-Z0-9-_]", "_");
            String fileName = base + ".json.gz";
            Path file = buildDir(type, height).resolve(fileName);

            Map<String, Integer> recipe = new LinkedHashMap<>();
            int minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0;
            boolean first = true;
            for (Map.Entry<BlockPos, BlockState> e : finalBlocks.entrySet()) {
                BlockPos p = e.getKey();
                BlockState st = e.getValue();
                String name = BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath();
                recipe.merge(name, 1, Integer::sum);
                if (first) {
                    minX = maxX = p.getX();
                    minY = maxY = p.getY();
                    minZ = maxZ = p.getZ();
                    first = false;
                } else {
                    if (p.getX() < minX) minX = p.getX();
                    if (p.getX() > maxX) maxX = p.getX();
                    if (p.getY() < minY) minY = p.getY();
                    if (p.getY() > maxY) maxY = p.getY();
                    if (p.getZ() < minZ) minZ = p.getZ();
                    if (p.getZ() > maxZ) maxZ = p.getZ();
                }
            }
            int sizeX = first ? 0 : (maxX - minX + 1);
            int sizeY = first ? 0 : (maxY - minY + 1);
            int sizeZ = first ? 0 : (maxZ - minZ + 1);

            int unique = stateIds.size();
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(file)), StandardCharsets.UTF_8))) {
                w.write("[\n");
                w.write(BuildRecordIO.GSON.toJson(new Meta(eyeHeight, bbWidth, bbHeight, recipe, unique, sizeX, sizeY, sizeZ, statePalette)));
                for (Action a : actions) {
                    w.write(",\n");
                    w.write(BuildRecordIO.GSON.toJson(a));
                }
                w.write("\n]");
            } catch (IOException e) {
                LOGGER.error("[BuildRec] save failed file={}", fileName, e);
            }
            LOGGER.info("[BuildRec] save file={} actions={} additions={} destroys={}", fileName, actions.size(), additions, destroys);
            String rel = buildRootDir().relativize(file).toString().replace('\\', '/');
            int finalCount = finalBlocks.size();
            return new Summary(rel, actions.size(), additions, destroys, recipe, unique, sizeX, sizeY, sizeZ, finalCount);
        }

        Summary finish(ServerPlayer player) {
            return save();
        }
    }

    private static class Replay {
        final Mob actor;
        final List<Action> actions;
        final List<BlockState> palette;
        final double baseX, baseY, baseZ;
        final int speed;
        final double recordEyeHeight;
        final double recordBbWidth;
        final double recordBbHeight;
        final boolean preserveActor;
        final boolean creative;
        Item requiredItem;
        boolean paused = false;
        int index = 0;
        Action action = null;
        double progress = 0;
        double sx, sy, sz, tx, ty, tz;
        float syaw, spitch, tyaw, tpitch;

        Replay(Mob actor, List<Action> actions, int speed, double recordEyeHeight, double recordBbWidth, double recordBbHeight, List<BlockState> palette, boolean preserveActor, boolean creative) {
            this.actor = actor;
            this.actions = actions;
            this.speed = speed;
            this.recordEyeHeight = recordEyeHeight;
            this.recordBbWidth = recordBbWidth;
            this.recordBbHeight = recordBbHeight;
            this.palette = palette;
            this.preserveActor = preserveActor;
            this.creative = creative;
            BlockPos p = actor.blockPosition();
            this.baseX = p.getX();
            this.baseY = p.getY();
            this.baseZ = p.getZ();
        }
    }

}

