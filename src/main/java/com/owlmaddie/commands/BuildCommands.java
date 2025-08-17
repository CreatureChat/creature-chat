// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.owlmaddie.buildrec.BuildRecorder;
import com.owlmaddie.buildrec.BuildRecorder.Summary;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Commands to record and replay builds.
 */
public class BuildCommands {
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
            dispatcher.register(Commands.literal("creaturechat")
                    .then(Commands.literal("buildrec")
                            .then(Commands.literal("start").executes(BuildCommands::start))
                            .then(Commands.literal("stop").executes(BuildCommands::stop))
                            .then(Commands.literal("replay")
                                    .then(Commands.argument("id", StringArgumentType.string())
                                            .suggests(BuildCommands::suggest)
                                            .executes(ctx -> replay(ctx, 2))
                                            .then(Commands.argument("speed", IntegerArgumentType.integer(1, 4))
                                                    .suggests((c, b) -> {
                                                        b.suggest("1");
                                                        b.suggest("2");
                                                        b.suggest("4");
                                                        return b.buildFuture();
                                                    })
                                                    .executes(ctx -> replay(ctx, IntegerArgumentType.getInteger(ctx, "speed"))))))));
        });
    }

    private static int start(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (BuildRecorder.start(player)) {
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
            Component msg = Component.literal("Saved build " + s.id + ". Blocks: " + s.total + ", additions: " + s.additions + ", destroys: " + s.destroys);
            context.getSource().sendSuccess(() -> msg, false);
            return 1;
        }
        context.getSource().sendSuccess(() -> Component.literal("No active recording").withStyle(ChatFormatting.RED), false);
        return 0;
    }

    private static int replay(CommandContext<CommandSourceStack> context, int speed) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        int spd = (speed == 1 || speed == 2 || speed == 4) ? speed : 1;
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(context, "id");
        if (BuildRecorder.startReplay(player, id, spd)) {
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
        try (Stream<Path> stream = Files.list(dir)) {
            stream.map(p -> p.getFileName().toString()).forEach(builder::suggest);
        } catch (Exception ignored) {
        }
        return builder.buildFuture();
    }
}

