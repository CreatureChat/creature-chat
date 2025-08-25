// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.network;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.ChatDataSaverScheduler;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.commands.ConfigurationHandler;
import com.owlmaddie.goals.EntityBehaviorManager;
import com.owlmaddie.goals.GoalPriority;
import com.owlmaddie.goals.TalkPlayerGoal;
import com.owlmaddie.inventory.ChatInventory;
import com.owlmaddie.inventory.InventoryLootTables;
import com.owlmaddie.inventory.LootTableHelper;
import com.owlmaddie.particle.Particles;
import com.owlmaddie.utils.Compression;
import com.owlmaddie.utils.Randomizer;
import com.owlmaddie.utils.ServerEntityFinder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * The {@code ServerPackets} class provides methods to send packets to/from the client for generating greetings,
 * updating message details, and sending user messages.
 */
public class ServerPackets {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    public static MinecraftServer serverInstance;
    public static ChatDataSaverScheduler scheduler = null;
    public static final ResourceLocation PACKET_C2S_GREETING = new ResourceLocation("creaturechat", "packet_c2s_greeting");
    public static final ResourceLocation PACKET_C2S_READ_NEXT = new ResourceLocation("creaturechat", "packet_c2s_read_next");
    public static final ResourceLocation PACKET_C2S_SET_STATUS = new ResourceLocation("creaturechat", "packet_c2s_set_status");
    public static final ResourceLocation PACKET_C2S_OPEN_CHAT = new ResourceLocation("creaturechat", "packet_c2s_open_chat");
    public static final ResourceLocation PACKET_C2S_CLOSE_CHAT = new ResourceLocation("creaturechat", "packet_c2s_close_chat");
    public static final ResourceLocation PACKET_C2S_SEND_CHAT = new ResourceLocation("creaturechat", "packet_c2s_send_chat");
    public static final ResourceLocation PACKET_S2C_ENTITY_MESSAGE = new ResourceLocation("creaturechat", "packet_s2c_entity_message");
    public static final ResourceLocation PACKET_S2C_PLAYER_MESSAGE = new ResourceLocation("creaturechat", "packet_s2c_player_message");
    public static final ResourceLocation PACKET_S2C_LOGIN = new ResourceLocation("creaturechat", "packet_s2c_login");
    public static final ResourceLocation PACKET_S2C_WHITELIST = new ResourceLocation("creaturechat", "packet_s2c_whitelist");
    public static final ResourceLocation PACKET_S2C_PLAYER_STATUS = new ResourceLocation("creaturechat", "packet_s2c_player_status");
    public static final ParticleType<?> HEART_SMALL_PARTICLE = Particles.HEART_SMALL_PARTICLE;
    public static final ParticleType<?> HEART_BIG_PARTICLE = Particles.HEART_BIG_PARTICLE;
    public static final ParticleType<?> FIRE_SMALL_PARTICLE = Particles.FIRE_SMALL_PARTICLE;
    public static final ParticleType<?> FIRE_BIG_PARTICLE = Particles.FIRE_BIG_PARTICLE;
    public static final ParticleType<?> ATTACK_PARTICLE = Particles.ATTACK_PARTICLE;
    public static final ParticleType<?> FLEE_PARTICLE = Particles.FLEE_PARTICLE;
    public static final ParticleType<?> FOLLOW_FRIEND_PARTICLE = Particles.FOLLOW_FRIEND_PARTICLE;
    public static final ParticleType<?> FOLLOW_ENEMY_PARTICLE = Particles.FOLLOW_ENEMY_PARTICLE;
    public static final ParticleType<?> PROTECT_PARTICLE = Particles.PROTECT_PARTICLE;
    public static final ParticleType<?> LEAD_FRIEND_PARTICLE = Particles.LEAD_FRIEND_PARTICLE;
    public static final ParticleType<?> LEAD_ENEMY_PARTICLE = Particles.LEAD_ENEMY_PARTICLE;
    public static final ParticleType<?> LEAD_PARTICLE = Particles.LEAD_PARTICLE;

    public static void register() {
        // Register custom particles
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "heart_small"), HEART_SMALL_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "heart_big"), HEART_BIG_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "fire_small"), FIRE_SMALL_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "fire_big"), FIRE_BIG_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "attack"), ATTACK_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "flee"), FLEE_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "follow_enemy"), FOLLOW_ENEMY_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "follow_friend"), FOLLOW_FRIEND_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "protect"), PROTECT_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "lead_enemy"), LEAD_ENEMY_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "lead_friend"), LEAD_FRIEND_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "lead"), LEAD_PARTICLE);

        // Handle packet for Greeting
        PacketHelper.registerReceiver(PACKET_C2S_GREETING, (server, player, buf) -> {
            UUID entityId = UUID.fromString(buf.readUtf());
            String userLanguage = buf.readUtf(32767);

            // Ensure that the task is synced with the server thread
            server.execute(() -> {
                Mob entity = (Mob)ServerEntityFinder.getEntityByUUID((ServerLevel)player.level(), entityId);
                if (entity != null) {
                    EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entity.getStringUUID());
                    if (chatData.characterSheet.isEmpty()) {
                        generate_character(userLanguage, chatData, player, entity, false);
                    }
                }
            });
        });

        // Handle packet for reading lines of message
        PacketHelper.registerReceiver(PACKET_C2S_READ_NEXT, (server, player, buf) -> {
            UUID entityId = UUID.fromString(buf.readUtf());
            int lineNumber = buf.readInt();

            // Ensure that the task is synced with the server thread
            server.execute(() -> {
                Mob entity = (Mob)ServerEntityFinder.getEntityByUUID((ServerLevel)player.level(), entityId);
                if (entity != null) {
                    // Set talk to player goal (prevent entity from walking off)
                    TalkPlayerGoal talkGoal = new TalkPlayerGoal(player, entity, 3.5F);
                    EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);

                    EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entity.getStringUUID());
                    LOGGER.debug("Update read lines to " + lineNumber + " for: " + entity.getType().toString());
                    chatData.setLineNumber(lineNumber);
                }
            });
        });

        // Handle packet for setting status of chat bubbles
        PacketHelper.registerReceiver(PACKET_C2S_SET_STATUS, (server, player, buf) -> {
            UUID entityId = UUID.fromString(buf.readUtf());
            String status_name = buf.readUtf(32767);

            // Ensure that the task is synced with the server thread
            server.execute(() -> {
                Mob entity = (Mob)ServerEntityFinder.getEntityByUUID((ServerLevel)player.level(), entityId);
                if (entity != null) {
                    // Set talk to player goal (prevent entity from walking off)
                    TalkPlayerGoal talkGoal = new TalkPlayerGoal(player, entity, 3.5F);
                    EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);

                    EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entity.getStringUUID());
                    LOGGER.debug("Hiding chat bubble for: " + entity.getType().toString());
                    chatData.setStatus(ChatDataManager.ChatStatus.valueOf(status_name));
                }
            });
        });

        // Handle packet for Open Chat
        PacketHelper.registerReceiver(PACKET_C2S_OPEN_CHAT, (server, player, buf) -> {
            UUID entityId = UUID.fromString(buf.readUtf());

            // Ensure that the task is synced with the server thread
            server.execute(() -> {
                Mob entity = (Mob)ServerEntityFinder.getEntityByUUID((ServerLevel)player.level(), entityId);
                if (entity != null) {
                    // Set talk to player goal (prevent entity from walking off)
                    TalkPlayerGoal talkGoal = new TalkPlayerGoal(player, entity, 7F);
                    EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);
                }

                // Sync player UI status to all clients
                BroadcastPlayerStatus(player, true);
            });
        });

        // Handle packet for Close Chat
        PacketHelper.registerReceiver(PACKET_C2S_CLOSE_CHAT, (server, player, buf) -> {

            server.execute(() -> {
                // Sync player UI status to all clients
                BroadcastPlayerStatus(player, false);
            });
        });

        // Handle packet for new chat message
        PacketHelper.registerReceiver(PACKET_C2S_SEND_CHAT, (server, player, buf) -> {
            UUID entityId = UUID.fromString(buf.readUtf());
            String message = buf.readUtf(32767);
            String userLanguage = buf.readUtf(32767);

            // Ensure that the task is synced with the server thread
            server.execute(() -> {
                Mob entity = (Mob)ServerEntityFinder.getEntityByUUID((ServerLevel)player.level(), entityId);
                if (entity != null) {
                    EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entity.getStringUUID());
                    if (chatData.characterSheet.isEmpty()) {
                        generate_character(userLanguage, chatData, player, entity, false);
                    } else {
                        generate_chat(userLanguage, chatData, player, entity, message, false);
                    }
                }
            });
        });

        // Send lite chat data JSON to new player (to populate client data)
        // Data is sent in chunks, to prevent exceeding the 32767 limit per String.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;

            // Send entire whitelist / blacklist to logged in player
            send_whitelist_blacklist(player);

            LOGGER.info("Server send compressed, chunked login message packets to player: " + player.getDisplayName().getString());
            // Get lite JSON data & compress to byte array
            String chatDataJSON = ChatDataManager.getServerInstance().GetLightChatData(player.getDisplayName().getString());
            byte[] compressedData = Compression.compressString(chatDataJSON);
            if (compressedData == null) {
                LOGGER.error("Failed to compress chat data.");
                return;
            }

            final int chunkSize = 32000; // Define chunk size
            int totalPackets = (int) Math.ceil((double) compressedData.length / chunkSize);

            // Loop through each chunk of bytes, and send bytes to player
            for (int i = 0; i < totalPackets; i++) {
                int start = i * chunkSize;
                int end = Math.min(compressedData.length, start + chunkSize);

                FriendlyByteBuf buffer = BufferHelper.create();
                buffer.writeInt(i); // Packet sequence number
                buffer.writeInt(totalPackets); // Total number of packets

                // Write chunk as byte array
                byte[] chunk = Arrays.copyOfRange(compressedData, start, end);
                buffer.writeByteArray(chunk);

                PacketHelper.send(player, PACKET_S2C_LOGIN, buffer);
            }
        });

        ServerWorldEvents.LOAD.register((server, world) -> {
            String world_name = world.dimension().location().getPath();
            if (world_name.equals("overworld")) {
                serverInstance = server;
                ChatDataManager.getServerInstance().loadChatData(server);

                // Start the auto-save task to save every X minutes
                scheduler = new ChatDataSaverScheduler();
                scheduler.startAutoSaveTask(server, 15, TimeUnit.MINUTES);
            }
        });
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            String world_name = world.dimension().location().getPath();
            if (world_name.equals("overworld")) {
                ChatDataManager manager = ChatDataManager.getServerInstance();
                manager.saveChatData(server);
                manager.clearData();
                serverInstance = null;

                // Shutdown auto scheduler
                scheduler.stopAutoSaveTask();
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            String entityUUID = entity.getStringUUID();
            if (entity.getRemovalReason() == Entity.RemovalReason.KILLED && ChatDataManager.getServerInstance().entityChatDataMap.containsKey(entityUUID)) {
                LOGGER.debug("Entity killed (" + entityUUID + "), updating death time stamp.");
                ChatDataManager.getServerInstance().entityChatDataMap.get(entityUUID).death = System.currentTimeMillis();
            }
        });
    }

    public static void send_whitelist_blacklist(ServerPlayer player) {
        ConfigurationHandler.Config config = new ConfigurationHandler(ServerPackets.serverInstance).loadConfig();
        FriendlyByteBuf buffer = BufferHelper.create();

        // Write the whitelist data to the buffer
        List<String> whitelist = config.getWhitelist();
        buffer.writeInt(whitelist.size());
        for (String entry : whitelist) {
            buffer.writeUtf(entry);
        }

        // Write the blacklist data to the buffer
        List<String> blacklist = config.getBlacklist();
        buffer.writeInt(blacklist.size());
        for (String entry : blacklist) {
            buffer.writeUtf(entry);
        }

        if (player != null) {
            // Send packet to specific player
            LOGGER.info("Sending whitelist / blacklist packet to player: " + player.getDisplayName().getString());
            PacketHelper.send(player, PACKET_S2C_WHITELIST, buffer);
        } else {
            // Iterate over all players and send the packet
            for (ServerPlayer serverPlayer : serverInstance.getPlayerList().getPlayers()) {
                PacketHelper.send(serverPlayer, PACKET_S2C_WHITELIST, buffer);
            }
        }
    }

    public static void generate_character(String userLanguage, EntityChatData chatData, ServerPlayer player, Mob entity, boolean is_auto_message) {
        ConfigurationHandler.Config config = new ConfigurationHandler(serverInstance).loadConfig();
        ChatDataManager manager = ChatDataManager.getServerInstance();
        if (!manager.handleAutoResponse(chatData, player, is_auto_message, config)) {
            return;
        }
        // Set talk to player goal (prevent entity from walking off)
        TalkPlayerGoal talkGoal = new TalkPlayerGoal(player, entity, 3.5F);
        EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);

        // Grab random adjective
        String randomAdjective = Randomizer.getRandomMessage(Randomizer.RandomType.ADJECTIVE);
        String randomClass = Randomizer.getRandomMessage(Randomizer.RandomType.CLASS);
        String randomAlignment = Randomizer.getRandomMessage(Randomizer.RandomType.ALIGNMENT);
        String randomSpeakingStyle = Randomizer.getRandomMessage(Randomizer.RandomType.SPEAKING_STYLE);

        // Generate random name parameters
        String randomLetter = Randomizer.RandomLetter();
        int randomSyllables = Randomizer.RandomNumber(5) + 1;

        // Build the message
        StringBuilder userMessageBuilder = new StringBuilder();
        userMessageBuilder.append("Please generate a ").append(randomAdjective).append(" character. ");
        userMessageBuilder.append("This character is a ").append(randomClass).append(" class, who is ").append(randomAlignment).append(". ");
        if (entity.getCustomName() != null && !entity.getCustomName().getString().equals("N/A")) {
            userMessageBuilder.append("Their name is '").append(entity.getCustomName().getString()).append("'. ");
        } else {
            userMessageBuilder.append("Their name starts with the letter '").append(randomLetter)
                    .append("' and is ").append(randomSyllables).append(" syllables long. ");
        }
        userMessageBuilder.append("They speak in '").append(userLanguage).append("' with a ").append(randomSpeakingStyle).append(" style.");

        // Generate new character
        chatData.generateCharacter(userLanguage, player, userMessageBuilder.toString(), is_auto_message);

        // Populate inventory with some simple starter items if empty
        if (entity instanceof ChatInventory chatInv) {
            Container inv = chatInv.creaturechat$getInventory();

            // Server-side only to avoid client duplicates
            if (entity.level().isClientSide) return;

            // If this entity has a chest, copy its inventory into ours
            if (entity instanceof AbstractChestedHorse chested && chested.hasChest()) {
                int chestStart = AbstractHorse.CHEST_SLOT_OFFSET + 1; // skip the chest item slot
                int chestSlots = chested.getInventoryColumns() * 3;

                for (int i = 0; i < chestSlots && i < inv.getContainerSize(); i++) {
                    ItemStack stack = chested.getSlot(chestStart + i).get();
                    if (!stack.isEmpty()) {
                        inv.setItem(i, stack.copy());
                        chested.getSlot(chestStart + i).set(ItemStack.EMPTY);
                    }
                }
            }

            boolean empty = true;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (!inv.getItem(i).isEmpty()) {
                    empty = false;
                    break;
                }
            }

            if (empty && entity.level() instanceof ServerLevel level) {
                Holder<Biome> biome = level.getBiome(entity.blockPosition());
                ResourceLocation tableId = InventoryLootTables.forBiome(biome);
                LootTable table = LootTableHelper.get(level, tableId);
                LootParams params = new LootParams.Builder(level)
                        .withParameter(LootContextParams.ORIGIN, entity.position())
                        .withOptionalParameter(LootContextParams.THIS_ENTITY, entity)
                        .create(LootContextParamSets.COMMAND);
                List<ItemStack> stacks = table.getRandomItems(params);
                if (stacks.isEmpty()) {
                    LOGGER.info("No loot matched for {}", tableId);
                } else {
                    // Randomly assign inventory loot items into random slots
                    RandomSource random = entity.getRandom();
                    int limit = Math.min(3, Math.min(inv.getContainerSize(), stacks.size()));
                    List<Integer> slots = new ArrayList<>();
                    for (int i = 0; i < inv.getContainerSize(); i++) {
                        slots.add(i);
                    }
                    Collections.shuffle(slots, new java.util.Random(random.nextLong()));
                    for (int i = 0; i < limit; i++) {
                        inv.setItem(slots.get(i), stacks.get(i));
                    }
                }
            }
        }
    }

    public static void generate_chat(String userLanguage, EntityChatData chatData, ServerPlayer player, Mob entity, String message, boolean is_auto_message) {
        ConfigurationHandler.Config config = new ConfigurationHandler(serverInstance).loadConfig();
        ChatDataManager manager = ChatDataManager.getServerInstance();
        if (!manager.handleAutoResponse(chatData, player, is_auto_message, config)) {
            return;
        }

        // Set talk to player goal (prevent entity from walking off)
        TalkPlayerGoal talkGoal = new TalkPlayerGoal(player, entity, 3.5F);
        EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);

        // Add new message
        chatData.generateMessage(userLanguage, player, message, is_auto_message);
    }

    // Writing a Map<String, PlayerData> to the buffer
    public static void writePlayerDataMap(FriendlyByteBuf buffer, Map<String, PlayerData> map) {
        buffer.writeInt(map.size()); // Write the size of the map
        for (Map.Entry<String, PlayerData> entry : map.entrySet()) {
            buffer.writeUtf(entry.getKey()); // Write the key (playerName)
            PlayerData data = entry.getValue();
            buffer.writeInt(data.friendship); // Write PlayerData field(s)
        }
    }

    // Send new message to all connected players
    public static void BroadcastEntityMessage(EntityChatData chatData) {
        // Log useful information before looping through all players
        LOGGER.info("Broadcasting entity message: entityId={}, status={}, currentMessage={}, currentLineNumber={}, senderType={}",
                chatData.entityId, chatData.status,
                chatData.currentMessage.length() > 24 ? chatData.currentMessage.substring(0, 24) + "..." : chatData.currentMessage,
                chatData.currentLineNumber, chatData.sender);

        for (ServerLevel world : serverInstance.getAllLevels()) {
            // Find Entity by UUID and update custom name
            UUID entityId = UUID.fromString(chatData.entityId);
            Mob entity = (Mob)ServerEntityFinder.getEntityByUUID(world, entityId);
            if (entity != null) {
                String characterName = chatData.getCharacterProp("name");
                if (!characterName.isEmpty() && !characterName.equals("N/A") && entity.getCustomName() == null) {
                    LOGGER.debug("Setting entity name to " + characterName + " for " + chatData.entityId);
                    entity.setCustomName(Component.literal(characterName));
                    entity.setCustomNameVisible(true);
                    entity.setPersistenceRequired();
                }

                // Make auto-generated message appear as a pending icon (attack, show/give, arrival)
                if (chatData.sender == ChatDataManager.ChatSender.USER && chatData.auto_generated > 0) {
                    chatData.status = ChatDataManager.ChatStatus.PENDING;
                }

                // Iterate over all players and send the packet
                for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
                    FriendlyByteBuf buffer = BufferHelper.create();
                    buffer.writeUtf(chatData.entityId);
                    buffer.writeUtf(chatData.currentMessage);
                    buffer.writeInt(chatData.currentLineNumber);
                    buffer.writeUtf(chatData.status.toString());
                    buffer.writeUtf(chatData.sender.toString());
                    writePlayerDataMap(buffer, chatData.players);

                    // Send message to player
                    PacketHelper.send(player, PACKET_S2C_ENTITY_MESSAGE, buffer);
                }
                break;
            }
        }
    }

    // Send new message to all connected players
    public static void BroadcastPlayerMessage(EntityChatData chatData, ServerPlayer sender) {
        // Log the specific data being sent
        LOGGER.info("Broadcasting player message: senderUUID={}, message={}", sender.getStringUUID(),
                chatData.currentMessage);

        // Create the buffer for the packet
        FriendlyByteBuf buffer = BufferHelper.create();

        // Write the sender's UUID and the chat message to the buffer
        buffer.writeUtf(sender.getStringUUID());
        buffer.writeUtf(sender.getDisplayName().getString());
        buffer.writeUtf(chatData.currentMessage);

        // Iterate over all connected players and send the packet
        for (ServerPlayer serverPlayer : serverInstance.getPlayerList().getPlayers()) {
            PacketHelper.send(serverPlayer, PACKET_S2C_PLAYER_MESSAGE, buffer);
        }
    }

    // Send new message to all connected players
    public static void BroadcastPlayerStatus(Player player, boolean isChatOpen) {
        FriendlyByteBuf buffer = BufferHelper.create();

        // Write the entity's chat updated data
        buffer.writeUtf(player.getStringUUID());
        buffer.writeBoolean(isChatOpen);

        // Iterate over all players and send the packet
        for (ServerPlayer serverPlayer : serverInstance.getPlayerList().getPlayers()) {
            LOGGER.debug("Server broadcast " + player.getDisplayName().getString() + " player status to client: " + serverPlayer.getDisplayName().getString() + " | isChatOpen: " + isChatOpen);
            PacketHelper.send(serverPlayer, PACKET_S2C_PLAYER_STATUS, buffer);
        }
    }

    // Send a chat message to all players (i.e. death message)
    public static void BroadcastMessage(Component message) {
        for (ServerPlayer serverPlayer : serverInstance.getPlayerList().getPlayers()) {
            serverPlayer.displayClientMessage(message, false);
        };
    }

    // Send a chat message to a player which is clickable (for error messages with a link for help)
    public static void SendClickableError(Player player, String message, String url) {
        MutableComponent text = Component.literal(message)
                .withStyle(ChatFormatting.BLUE)
                .withStyle(style -> style
                        .withClickEvent(ClickEventHelper.openUrl(url))
                        .withUnderlined(true));
        player.displayClientMessage(text, false);
    }

    // Send a clickable message to ALL Ops
    public static void sendErrorToAllOps(MinecraftServer server, String message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Check if the player is an operator
            if (server.getPlayerList().isOp(player.getGameProfile())) {
                ServerPackets.SendClickableError(player, message, "http://discord.creaturechat.com");
            }
        }
    }
}
