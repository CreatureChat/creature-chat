// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.inventory;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.network.ServerPackets;
import com.owlmaddie.utils.ServerEntityFinder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class PickupMessageBatcher {
    private static final int DEBOUNCE_TICKS = 40;
    private static final Map<Key, Pending> PENDING = new ConcurrentHashMap<>();

    private PickupMessageBatcher() {
    }

    public static void init() {
        ServerTickEvents.START_SERVER_TICK.register(PickupMessageBatcher::tick);
    }

    public static void recordPickup(Mob mob, Player player, ItemStack stack, int pickedUp) {
        if (mob == null || player == null || stack == null || pickedUp <= 0) {
            return;
        }
        if (mob.level().isClientSide()) {
            return;
        }
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        MinecraftServer server = mob.level().getServer();
        if (server == null) {
            return;
        }
        long tick = server.getTickCount();
        Key key = new Key(mob.getUUID(), player.getUUID());
        Pending pending = PENDING.computeIfAbsent(key, k -> new Pending());
        pending.entityId = mob.getUUID();
        pending.playerId = player.getUUID();
        pending.lastTick = tick;
        pending.counts.merge(stack.getItem(), pickedUp, Integer::sum);
    }

    private static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        for (Map.Entry<Key, Pending> entry : PENDING.entrySet()) {
            Pending pending = entry.getValue();
            if (now - pending.lastTick < DEBOUNCE_TICKS) {
                continue;
            }
            sendMessage(server, pending);
            PENDING.remove(entry.getKey());
        }
    }

    private static void sendMessage(MinecraftServer server, Pending pending) {
        ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId);
        if (player == null) {
            return;
        }
        Mob mob = findMob(server, pending.entityId);
        if (mob == null) {
            return;
        }
        EntityChatData chatData = ChatDataManager.getServerInstance().entityChatDataMap.get(mob.getStringUUID());
        if (chatData == null || chatData.status == ChatDataManager.ChatStatus.NONE) {
            return;
        }
        PlayerData playerData = chatData.getPlayerData(player.getDisplayName().getString());
        if (playerData.friendship <= 0) {
            return;
        }
        String message = "<" + player.getDisplayName().getString() + " gave you " + joinCounts(pending.counts) + ">";
        ServerPackets.generate_chat("N/A", chatData, player, mob, message, true);
    }

    private static Mob findMob(MinecraftServer server, UUID entityId) {
        for (ServerLevel level : server.getAllLevels()) {
            LivingEntity entity = ServerEntityFinder.getEntityByUUID(level, entityId);
            if (entity instanceof Mob mob) {
                return mob;
            }
        }
        return null;
    }

    private static String joinCounts(Map<Item, Integer> map) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<Item, Integer> entry : map.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(entry.getValue()).append(" ")
                   .append(new ItemStack(entry.getKey()).getHoverName().getString());
            first = false;
        }
        return builder.toString();
    }

    private static final class Pending {
        private UUID entityId;
        private UUID playerId;
        private long lastTick;
        private final Map<Item, Integer> counts = new HashMap<>();
    }

    private record Key(UUID entityId, UUID playerId) {
    }
}
