// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.owlmaddie.buildrec.BuildRecorder;
import com.owlmaddie.buildrec.BuildRecorder.Summary;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Commands to record and replay builds.
 */
public class BuildCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
            dispatcher.register(
                    Commands.literal("creaturechat")
                            .then(Commands.literal("build")
                                    .then(Commands.literal("record")
                                            .requires(src -> src.getEntity() instanceof ServerPlayer sp && !BuildRecorder.isRecording(sp))
                                            .then(Commands.argument("type", StringArgumentType.word())
                                                    .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.asList("house", "statue", "pond", "garden", "farm", "castle", "mine", "trap", "campfire"), b))
                                                    .then(Commands.argument("height", StringArgumentType.word())
                                                            .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.asList("Any", "1", "2", "3", "4"), b))
                                                            .then(Commands.argument("name", StringArgumentType.string())
                                                                    .executes(ctx -> record(ctx,
                                                                            StringArgumentType.getString(ctx, "type"),
                                                                            StringArgumentType.getString(ctx, "height"),
                                                                            StringArgumentType.getString(ctx, "name")))))))
                                    .then(Commands.literal("stop")
                                            .requires(src -> src.getEntity() instanceof ServerPlayer sp && BuildRecorder.isRecording(sp))
                                            .executes(BuildCommands::stop))
                                    .then(Commands.literal("replay")
                                            .then(Commands.argument("id", StringArgumentType.string())
                                                    .suggests(BuildCommands::suggest)
                                                    .then(Commands.argument("entity", ResourceLocationArgument.id())
                                                            .suggests((c, b) -> SharedSuggestionProvider.suggestResource(getLivingEntityIds(), b))
                                                            .executes(ctx -> replay(ctx, ResourceLocationArgument.getId(ctx, "entity"), 1))
                                                            .then(Commands.argument("speed", IntegerArgumentType.integer(1, 32))
                                                                    .executes(ctx -> replay(ctx, ResourceLocationArgument.getId(ctx, "entity"), IntegerArgumentType.getInteger(ctx, "speed"))))))))
            );
        });
    }

    private static int record(CommandContext<CommandSourceStack> context, String type, String height, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        LOGGER.info("[BuildRec] command record player={} type={} height={} name={}",
                player.getGameProfile().getName(), type, height, name);
        if (BuildRecorder.start(player, type, height, name)) {
            context.getSource().sendSuccess(() -> Component.literal("Recording started"), false);
            return 1;
        }
        context.getSource().sendSuccess(() -> Component.literal("Already recording").withStyle(ChatFormatting.RED), false);
        return 0;
    }

    private static int stop(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Summary s = BuildRecorder.stop(player);
        if (s != null) {
            String recipeStr = s.recipe.entrySet().stream()
                    .map(e -> e.getValue() + " " + e.getKey())
                    .collect(Collectors.joining(", "));
            Component msg = Component.literal(
                    recipeStr + " (" + s.uniqueBlocks + " Unique blocks)\n" +
                    s.sizeX + " length x " + s.sizeZ + " width x " + s.sizeY + " height\n" +
                    s.finalBlocks + " blocks placed, " + s.destroys + " blocks destroyed\n" +
                    "Saved build " + s.id);
            context.getSource().sendSuccess(() -> msg, false);
            LOGGER.info("[BuildRec] command stop player={} file={} placed={} destroys={} unique={} size={}x{}x{}", player.getGameProfile().getName(), s.id, s.finalBlocks, s.destroys, s.uniqueBlocks, s.sizeX, s.sizeZ, s.sizeY);
            return 1;
        }
        context.getSource().sendSuccess(() -> Component.literal("No active recording").withStyle(ChatFormatting.RED), false);
        return 0;
    }

    private static int replay(CommandContext<CommandSourceStack> context, ResourceLocation entityId, int speed) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        int spd = speed;
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(context, "id").replace('\\', '/');
        EntityType<? extends Mob> type = null;
        if (entityId != null && !"player".equals(entityId.getPath())) {
            EntityType<?> raw = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
            if (raw != null) {
                @SuppressWarnings("unchecked")
                EntityType<? extends Mob> cast = (EntityType<? extends Mob>) raw;
                type = cast;
            }
        }
        var entityStr = (entityId == null ? "null"
                : "player".equals(entityId.getPath()) ? "player"
                : entityId.toString());
        LOGGER.info("[BuildRec] command replay player={} file={} entity={} speed={}",
                player.getGameProfile().getName(), id, entityStr, spd);
        if (BuildRecorder.startReplay(player, id, type, spd)) {
            final int fs = spd;
            final String fid = id;
            context.getSource().sendSuccess(() -> Component.literal("Replaying build " + fid + " at " + fs + "x"), false);
            return 1;
        }
        context.getSource().sendSuccess(() -> Component.literal("Could not replay " + id).withStyle(ChatFormatting.RED), false);
        return 0;
    }

    private static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("creaturechat").resolve("builds");
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                    .map(p -> dir.relativize(p)
                            .toString()
                            .replaceFirst("\\.json\\.gz$", "")
                            .replace('\\', '/'))
                    .forEach(id -> builder.suggest("\"" + id + "\""));
        } catch (Exception ignored) {
        }
        return builder.buildFuture();
    }

    public static List<ResourceLocation> getLivingEntityIds() {
        return BuiltInRegistries.ENTITY_TYPE
                .keySet()
                .stream()
                .filter(id ->
                        // getOptional(...) returns Optional<EntityType<?>> on all versions
                        BuiltInRegistries.ENTITY_TYPE
                                .getOptional(id)
                                .map(type -> type.getCategory() != MobCategory.MISC
                                        || isIncludedEntity(type))
                                .orElse(false)
                )
                .collect(Collectors.toList());
    }


    private static boolean isIncludedEntity(EntityType<?> entityType) {
        return entityType == EntityType.VILLAGER
                || entityType == EntityType.IRON_GOLEM
                || entityType == EntityType.SNOW_GOLEM;
    }
}

