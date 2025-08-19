// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.chat;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class AdvancementHelper {
    private static final ResourceLocation ICE_BREAKER = new ResourceLocation("creaturechat", "ice_breaker");
    private static final ResourceLocation FRIENDLY_CREATURE = new ResourceLocation("creaturechat", "friendly_creature");
    private static final ResourceLocation BEASTIE_BESTIE = new ResourceLocation("creaturechat", "beastie_bestie");
    private static final ResourceLocation ARCH_NEMESIS = new ResourceLocation("creaturechat", "arch_nemesis");
    private static final ResourceLocation LOVE_HATE = new ResourceLocation("creaturechat", "love_hate");
    private static final ResourceLocation DRAMA_LLAMA = new ResourceLocation("creaturechat", "drama_llama");
    private static final ResourceLocation SMOOTH_TALKER = new ResourceLocation("creaturechat", "smooth_talker");
    private static final ResourceLocation NO_HARD_FEELINGS = new ResourceLocation("creaturechat", "no_hard_feelings");
    private static final ResourceLocation SQUAD_GOALS = new ResourceLocation("creaturechat", "squad_goals");
    private static final ResourceLocation BORROWED_FOREVER = new ResourceLocation("creaturechat", "borrowed_forever");

    private static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");

    private static void award(ServerPlayer player, ResourceLocation id) {
        if (player == null) {
            return;
        }
        MinecraftServer server = player.getServer();
        AdvancementHolder adv = server.getAdvancements().get(id);
        if (adv != null) {
            boolean awarded = player.getAdvancements().award(adv, "triggered");
            if (!awarded) {
                LOGGER.info("Unable to award advancement {} to {}", id, player.getScoreboardName());
            }
        } else {
            LOGGER.info("Advancement {} not found for {}", id, player.getScoreboardName());
        }
    }

    public static void chatExchange(ServerPlayer player, EntityChatData data) {
        if (data.previousMessages.size() >= 2) {
            award(player, ICE_BREAKER);
        }
    }

    public static void friendshipChanged(ServerPlayer player, PlayerData data, int oldFriendship, int newFriendship, Mob entity) {
        if (oldFriendship < 1 && newFriendship >= 1) {
            award(player, FRIENDLY_CREATURE);
        }
        if (newFriendship == 3) {
            award(player, BEASTIE_BESTIE);
        }
        if (newFriendship == -3) {
            award(player, ARCH_NEMESIS);
        }
        if ((oldFriendship > 0 && newFriendship < 0) || (oldFriendship < 0 && newFriendship > 0)) {
            award(player, LOVE_HATE);
        }
        if (oldFriendship == 0 && newFriendship >= 2) {
            award(player, SMOOTH_TALKER);
        }
        long now = System.currentTimeMillis();
        // If we were already high before this change and we just fell low quickly -> award
        if (oldFriendship >= 2 && newFriendship <= -2 && data.swingStartTime != 0 && (now - data.swingStartTime) <= 1_200_000L) {
            award(player, DRAMA_LLAMA);
            data.swingStartTime = 0; // reset after awarding
        }

        // If we just entered the high range (from below), start the timer
        if (oldFriendship < 2 && newFriendship >= 2) {
            data.swingStartTime = now;
        }

        // If we fell out of the high range without awarding, clear the timer to avoid stale hits
        if (newFriendship < 2 && data.swingStartTime != 0 && !(oldFriendship >= 2 && newFriendship <= -2)) {
            data.swingStartTime = 0;
        }
        if (data.lastDamageTime != 0 && now - data.lastDamageTime <= 30_000L && newFriendship >= 0) {
            award(player, NO_HARD_FEELINGS);
            data.lastDamageTime = 0;
        }
        if (newFriendship >= 2) {
            String playerId = player.getDisplayName().getString();
            ChatDataManager manager = ChatDataManager.getServerInstance();
            List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(8.0));
            int count = 0;
            for (Mob m : mobs) {
                if (m == entity) continue; // don't count the focused mob itself
                EntityChatData other = manager.entityChatDataMap.get(m.getStringUUID());
                if (other == null) continue;
                PlayerData pd = other.getPlayerData(playerId);
                if (pd != null && pd.friendship >= 2) {
                    if (++count >= 3) {
                        award(player, SQUAD_GOALS);
                        break;
                    }
                }
            }
        }
    }

    public static void itemTaken(ServerPlayer player, PlayerData data) {
        if (data.friendship == 3) {
            award(player, BORROWED_FOREVER);
        }
    }
}
