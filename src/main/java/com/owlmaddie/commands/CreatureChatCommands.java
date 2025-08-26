// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.owlmaddie.network.ServerPackets;
import com.owlmaddie.i18n.CCText;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The {@code CreatureChatCommands} class registers custom commands to set new API key, model, and url.
 * Permission level set to 4 (server owner), since this deals with API keys and potential costs.
 */
public class CreatureChatCommands {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
            registerCommands(dispatcher);
        });
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("creaturechat")
                .then(registerSetCommand("key", "API Key", StringArgumentType.string()))
                .then(registerSetCommand("url", "URL", StringArgumentType.string()))
                .then(registerSetCommand("model", "Model", StringArgumentType.string()))
                .then(registerSetCommand("timeout", "Timeout (seconds)", IntegerArgumentType.integer()))
                .then(registerStoryCommand())
                .then(registerWhitelistCommand())
                .then(registerBlacklistCommand())
                .then(registerChatBubbleCommand())
                .then(registerHelpCommand()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerSetCommand(String settingName, String settingDescription, ArgumentType<?> valueType) {
        return Commands.literal(settingName)
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("set")
                        .then(Commands.argument("value", valueType)
                                .then(addConfigArgs((context, useServerConfig) -> {
                                    if (valueType instanceof StringArgumentType)
                                        return setConfig(context.getSource(), settingName, StringArgumentType.getString(context, "value"), useServerConfig, settingDescription);
                                    else if (valueType instanceof IntegerArgumentType)
                                        return setConfig(context.getSource(), settingName, IntegerArgumentType.getInteger(context, "value"), useServerConfig, settingDescription);
                                    return 1;
                                }))
                                .executes(context -> {
                                    if (valueType instanceof StringArgumentType)
                                        return setConfig(context.getSource(), settingName, StringArgumentType.getString(context, "value"), false, settingDescription);
                                    else if (valueType instanceof IntegerArgumentType)
                                        return setConfig(context.getSource(), settingName, IntegerArgumentType.getInteger(context, "value"), false, settingDescription);
                                    return 1;
                                })
                        ));
    }

    private static List<ResourceLocation> getLivingEntityIds() {
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

    private static List<String> getLivingEntityTypeNames() {
        return getLivingEntityIds().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toList());
    }
    private static LiteralArgumentBuilder<CommandSourceStack> registerChatBubbleCommand() {
        return Commands.literal("chatbubble")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("set")
                        .then(Commands.literal("on")
                                .then(addConfigArgs((context, useServerConfig) -> setChatBubbleEnabled(context, true, useServerConfig)))
                                .executes(context -> setChatBubbleEnabled(context, true, false)))
                        .then(Commands.literal("off")
                                .then(addConfigArgs((context, useServerConfig) -> setChatBubbleEnabled(context, false, useServerConfig)))
                                .executes(context -> setChatBubbleEnabled(context, false, false))));
    }

    private static int setChatBubbleEnabled(CommandContext<CommandSourceStack> context, boolean enabled, boolean useServerConfig) {
        CommandSourceStack source = context.getSource();
        ConfigurationHandler configHandler = new ConfigurationHandler(source.getServer());
        ConfigurationHandler.Config config = configHandler.loadConfig();

        config.setChatBubbles(enabled);

        if (configHandler.saveConfig(config, useServerConfig)) {
            Component feedbackMessage = (enabled ? CCText.CONFIG_CHATBUBBLE_ENABLED : CCText.CONFIG_CHATBUBBLE_DISABLED)
                    .comp().withStyle(ChatFormatting.GREEN);
            source.sendSuccess(() -> feedbackMessage, true);
            return 1;
        } else {
            Component feedbackMessage = CCText.CONFIG_CHATBUBBLE_UPDATE_FAILED.comp().withStyle(ChatFormatting.RED);
            source.sendSuccess(() -> feedbackMessage, false);
            return 0;
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerWhitelistCommand() {
        return Commands.literal("whitelist")
                .requires(source -> source.hasPermission(4))
                .then(Commands.argument("entityType", ResourceLocationArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(getLivingEntityIds(), builder))
                        .then(addConfigArgs((context, useServerConfig) -> modifyList(context, "whitelist", ResourceLocationArgument.getId(context, "entityType").toString(), useServerConfig)))
                        .executes(context -> modifyList(context, "whitelist", ResourceLocationArgument.getId(context, "entityType").toString(), false)))
                .then(Commands.literal("all")
                        .then(addConfigArgs((context, useServerConfig) -> modifyList(context, "whitelist", "all", useServerConfig)))
                        .executes(context -> modifyList(context, "whitelist", "all", false)))
                .then(Commands.literal("clear")
                        .then(addConfigArgs((context, useServerConfig) -> modifyList(context, "whitelist", "clear", useServerConfig)))
                        .executes(context -> modifyList(context, "whitelist", "clear", false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerBlacklistCommand() {
        return Commands.literal("blacklist")
                .requires(source -> source.hasPermission(4))
                .then(Commands.argument("entityType", ResourceLocationArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(getLivingEntityIds(), builder))
                        .then(addConfigArgs((context, useServerConfig) -> modifyList(context, "blacklist", ResourceLocationArgument.getId(context, "entityType").toString(), useServerConfig)))
                        .executes(context -> modifyList(context, "blacklist", ResourceLocationArgument.getId(context, "entityType").toString(), false)))
                .then(Commands.literal("all")
                        .then(addConfigArgs((context, useServerConfig) -> modifyList(context, "blacklist", "all", useServerConfig)))
                        .executes(context -> modifyList(context, "blacklist", "all", false)))
                .then(Commands.literal("clear")
                        .then(addConfigArgs((context, useServerConfig) -> modifyList(context, "blacklist", "clear", useServerConfig)))
                        .executes(context -> modifyList(context, "blacklist", "clear", false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerHelpCommand() {
        return Commands.literal("help")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> CCText.CONFIG_HELP.comp(), false);
                    return 1;
                });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerStoryCommand() {
        return Commands.literal("story")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("set")
                        .then(Commands.argument("value", StringArgumentType.string())
                                .then(addConfigArgs((context, useServerConfig) -> {
                                    String story = StringArgumentType.getString(context, "value");
                                    ConfigurationHandler.Config config = new ConfigurationHandler(context.getSource().getServer()).loadConfig();
                                    config.setStory(story);
                                    if (new ConfigurationHandler(context.getSource().getServer()).saveConfig(config, useServerConfig)) {
                                        context.getSource().sendSuccess(() -> CCText.CONFIG_STORY_SET_SUCCESS.comp(story).withStyle(ChatFormatting.GREEN), true);
                                        return 1;
                                    } else {
                                        context.getSource().sendSuccess(() -> CCText.CONFIG_STORY_SET_FAILED.comp().withStyle(ChatFormatting.RED), false);
                                        return 0;
                                    }
                                }))))
                .then(Commands.literal("clear")
                        .then(addConfigArgs((context, useServerConfig) -> {
                            ConfigurationHandler.Config config = new ConfigurationHandler(context.getSource().getServer()).loadConfig();
                            config.setStory("");
                            if (new ConfigurationHandler(context.getSource().getServer()).saveConfig(config, useServerConfig)) {
                            context.getSource().sendSuccess(() -> CCText.CONFIG_STORY_CLEARED_SUCCESS.comp().withStyle(ChatFormatting.GREEN), true);
                                return 1;
                            } else {
                            context.getSource().sendSuccess(() -> CCText.CONFIG_STORY_CLEARED_FAILED.comp().withStyle(ChatFormatting.RED), false);
                                return 0;
                            }
                        })))
                .then(Commands.literal("display")
                .executes(context -> {
                    ConfigurationHandler.Config config = new ConfigurationHandler(context.getSource().getServer()).loadConfig();
                    String story = config.getStory();
                    if (story == null || story.isEmpty()) {
                        context.getSource().sendSuccess(() -> CCText.CONFIG_STORY_NOT_SET.comp().withStyle(ChatFormatting.RED), false);
                        return 0;
                    } else {
                        context.getSource().sendSuccess(() -> CCText.CONFIG_STORY_SHOW.comp(story).withStyle(ChatFormatting.AQUA), false);
                        return 1;
                    }
                }));
    }

    private static <T> int setConfig(CommandSourceStack source, String settingName, T value, boolean useServerConfig, String settingDescription) {
        ConfigurationHandler configHandler = new ConfigurationHandler(source.getServer());
        ConfigurationHandler.Config config = configHandler.loadConfig();
        try {
            switch (settingName) {
                case "key":
                    config.setApiKey((String) value);
                    break;
                case "url":
                    config.setUrl((String) value);
                    break;
                case "model":
                    config.setModel((String) value);
                    break;
                case "timeout":
                    if (value instanceof Integer) {
                        config.setTimeout((Integer) value);
                    } else {
                        throw new IllegalArgumentException(CCText.CONFIG_TIMEOUT_INVALID_TYPE.comp().getString());
                    }
                    break;
                default:
                    throw new IllegalArgumentException(CCText.CONFIG_UNKNOWN_SETTING.comp(settingName).getString());
            }
        } catch (ClassCastException e) {
            Component errorMessage = CCText.CONFIG_INVALID_SETTING_TYPE.comp(settingName).withStyle(ChatFormatting.RED);
            source.sendSuccess(() -> errorMessage, false);
            LOGGER.error("Type mismatch during configuration setting for: " + settingName, e);
            return 0;
        } catch (IllegalArgumentException e) {
            Component errorMessage = Component.literal(e.getMessage()).withStyle(ChatFormatting.RED);
            source.sendSuccess(() -> errorMessage, false);
            LOGGER.error("Error setting configuration: " + e.getMessage(), e);
            return 0;
        }

        Component feedbackMessage;
        if (configHandler.saveConfig(config, useServerConfig)) {
            feedbackMessage = CCText.CONFIG_SETTING_SET_SUCCESS.comp(settingDescription).withStyle(ChatFormatting.GREEN);
            source.sendSuccess(() -> feedbackMessage, false);
            LOGGER.info("Command executed: " + feedbackMessage.getString());
            return 1;
        } else {
            feedbackMessage = CCText.CONFIG_SETTING_SET_FAILED.comp(settingDescription).withStyle(ChatFormatting.RED);
            source.sendSuccess(() -> feedbackMessage, false);
            LOGGER.info("Command executed: " + feedbackMessage.getString());
            return 0;
        }
    }

    private static int modifyList(CommandContext<CommandSourceStack> context, String listName, String action, boolean useServerConfig) {
        CommandSourceStack source = context.getSource();
        ConfigurationHandler configHandler = new ConfigurationHandler(source.getServer());
        ConfigurationHandler.Config config = configHandler.loadConfig();
        List<String> entityTypes = getLivingEntityTypeNames();

        try {
            if ("all".equals(action)) {
                if ("whitelist".equals(listName)) {
                    config.setWhitelist(entityTypes);
                    config.setBlacklist(new ArrayList<>()); // Clear blacklist
                } else if ("blacklist".equals(listName)) {
                    config.setBlacklist(entityTypes);
                    config.setWhitelist(new ArrayList<>()); // Clear whitelist
                }
            } else if ("clear".equals(action)) {
                if ("whitelist".equals(listName)) {
                    config.setWhitelist(new ArrayList<>());
                } else if ("blacklist".equals(listName)) {
                    config.setBlacklist(new ArrayList<>());
                }
            } else {
                if (!entityTypes.contains(action)) {
                    throw new IllegalArgumentException("Invalid entity type: " + action);
                }
                if ("whitelist".equals(listName)) {
                    List<String> whitelist = new ArrayList<>(config.getWhitelist());
                    if (!whitelist.contains(action)) {
                        whitelist.add(action);
                        config.setWhitelist(whitelist);
                    }
                    // Remove from blacklist if present
                    List<String> blacklist = new ArrayList<>(config.getBlacklist());
                    blacklist.remove(action);
                    config.setBlacklist(blacklist);
                } else if ("blacklist".equals(listName)) {
                    List<String> blacklist = new ArrayList<>(config.getBlacklist());
                    if (!blacklist.contains(action)) {
                        blacklist.add(action);
                        config.setBlacklist(blacklist);
                    }
                    // Remove from whitelist if present
                    List<String> whitelist = new ArrayList<>(config.getWhitelist());
                    whitelist.remove(action);
                    config.setWhitelist(whitelist);
                }
            }
        } catch (IllegalArgumentException e) {
            Component errorMessage = Component.literal(e.getMessage()).withStyle(ChatFormatting.RED);
            source.sendSuccess(() -> errorMessage, false);
            LOGGER.error("Error modifying list: " + e.getMessage(), e);
            return 0;
        }

        if (configHandler.saveConfig(config, useServerConfig)) {
            Component feedbackMessage = CCText.CONFIG_LIST_UPDATE_SUCCESS.comp(listName, action).withStyle(ChatFormatting.GREEN);
            source.sendSuccess(() -> feedbackMessage, false);

            // Send whitelist / blacklist to all players
            ServerPackets.send_whitelist_blacklist(null);
            return 1;
        } else {
            Component feedbackMessage = CCText.CONFIG_LIST_UPDATE_FAILED.comp(listName).withStyle(ChatFormatting.RED);
            source.sendSuccess(() -> feedbackMessage, false);
            return 0;
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> addConfigArgs(CommandExecutor executor) {
        return Commands.literal("--config")
                .then(Commands.literal("default").executes(context -> executor.run(context, false)))
                .then(Commands.literal("server").executes(context -> executor.run(context, true)))
                .executes(context -> executor.run(context, false));
    }

    @FunctionalInterface
    private interface CommandExecutor {
        int run(CommandContext<CommandSourceStack> context, boolean useServerConfig) throws CommandSyntaxException;
    }
}
