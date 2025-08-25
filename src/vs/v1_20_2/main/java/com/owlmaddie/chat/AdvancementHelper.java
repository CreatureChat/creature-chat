// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.chat;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import com.owlmaddie.chat.Advancements;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import com.owlmaddie.utils.ServerEntityFinder;

public class AdvancementHelper {
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
            award(player, Advancements.ICE_BREAKER.id);
        }
        PlayerData pd = data.getPlayerData(player.getDisplayName().getString());
        pd.conversationCount++;
        pd.messageCount++;
        if (pd.friendship < 0) {
            pd.droppedBelowZero = true;
        }
        if (!pd.droppedBelowZero && pd.conversationCount >= 50) {
            award(player, Advancements.THE_NEVERENDING_STORY.id);
            pd.conversationCount = 0;
            pd.droppedBelowZero = false;
        }
        checkSocialButterfly(player);
    }

    public static void checkInnerCircle(ServerPlayer player, Mob entity) {
        String playerName = player.getDisplayName().getString();
        ChatDataManager manager = ChatDataManager.getServerInstance();
        List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(8.0));
        int count = 0;
        for (Mob m : mobs) {
            if (m == entity) continue;
            EntityChatData other = manager.entityChatDataMap.get(m.getStringUUID());
            if (other == null) continue;
            PlayerData pd = other.getPlayerData(playerName);
            if (pd != null && pd.friendship >= 2) {
                if (++count >= 5) {
                    award(player, Advancements.INNER_CIRCLE.id);
                    break;
                }
            }
        }
    }

    public static void friendshipChanged(ServerPlayer player, PlayerData data, int oldFriendship, int newFriendship, Mob entity) {
        if (oldFriendship < 1 && newFriendship >= 1) {
            award(player, Advancements.FIRST_IMPRESSIONS.id);
        }
        if (newFriendship == 3) {
            award(player, Advancements.TRUE_COMPANION.id);
            data.reachedPos3 = true;
        }
        if (newFriendship == -3) {
            award(player, Advancements.ARCH_NEMESIS.id);
            data.reachedNeg3 = true;
        }
        if (data.reachedPos3 && data.reachedNeg3) {
            award(player, Advancements.FRIEND_OR_FOE.id);
            data.reachedPos3 = false;
            data.reachedNeg3 = false;
        }
        int newSign = Integer.compare(newFriendship, 0);
        if (newSign != 0 && newSign != data.lastSign) {
            data.signFlipCount++;
            data.lastSign = newSign;
        }
        if (data.signFlipCount >= 4) {
            award(player, Advancements.LOVE_HATE_RELATIONSHIP.id);
            data.signFlipCount = 0;
        }
        if (newFriendship >= 2) {
            data.seenHigh = true;
        }
        if (newFriendship <= -2) {
            data.seenLow = true;
        }
        if (entity.getType() == net.minecraft.world.entity.EntityType.LLAMA && data.seenHigh && data.seenLow) {
            award(player, Advancements.DRAMA_LLAMA.id);
            data.seenHigh = false;
            data.seenLow = false;
        }
        if (data.lastDamageFriendship != Integer.MIN_VALUE && newFriendship == data.lastDamageFriendship) {
            award(player, Advancements.NO_HARD_FEELINGS.id);
            data.lastDamageFriendship = Integer.MIN_VALUE;
        }
        if (newFriendship - oldFriendship >= 2) {
            award(player, Advancements.GRAND_GESTURE.id);
        }
        if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon && newFriendship == 3) {
            award(player, Advancements.TRUE_PACIFIST.id);
        }
        if (newFriendship < 0) {
            data.wordsmithActive = true;
            data.wordsmithOpenedInventory = false;
            data.wordsmithGaveItem = false;
            data.wordsmithDamaged = false;
        }
        if (data.wordsmithActive && newFriendship == 3 && !data.wordsmithOpenedInventory && !data.wordsmithGaveItem && !data.wordsmithDamaged) {
            award(player, Advancements.WORDSMITH.id);
            data.wordsmithActive = false;
        }
        checkInnerCircle(player, entity);
        checkPopularOpinion(player);
    }

    public static void follow(ServerPlayer player) {
        award(player, Advancements.TAG_ALONG.id);
    }

    public static void lead(ServerPlayer player) {
        award(player, Advancements.LEAD_THE_WAY.id);
    }

    public static void bodyguard(ServerPlayer player) {
        award(player, Advancements.SWORN_OATH.id);
    }

    public static void calmTheStorm(ServerPlayer player) {
        award(player, Advancements.CALM_THE_STORM.id);
    }

    public static void standYourGround(ServerPlayer player) {
        award(player, Advancements.STAND_YOUR_GROUND.id);
    }

    public static void itemTaken(ServerPlayer player, PlayerData data) {
        if (data.friendship == 3) {
            award(player, Advancements.FINDERS_KEEPERS.id);
        }
    }

    public static void checkSharedStash(ServerPlayer player) {
        String playerName = player.getDisplayName().getString();
        ChatDataManager manager = ChatDataManager.getServerInstance();
        int count = 0;
        for (EntityChatData chat : manager.entityChatDataMap.values()) {
            PlayerData pd = chat.players.get(playerName);
            if (pd != null && pd.gaveItem && pd.friendship > 0) {
                if (++count >= 5) {
                    award(player, Advancements.SHARED_STASH.id);
                    break;
                }
            }
        }
    }

    public static void checkSocialButterfly(ServerPlayer player) {
        String playerName = player.getDisplayName().getString();
        ChatDataManager manager = ChatDataManager.getServerInstance();
        Set<String> types = new HashSet<>();
        for (Map.Entry<String, EntityChatData> entry : manager.entityChatDataMap.entrySet()) {
            PlayerData pd = entry.getValue().players.get(playerName);
            if (pd != null && pd.friendship > 0 && pd.messageCount >= 2) {
                Mob mob = (Mob) ServerEntityFinder.getEntityByUUID((ServerLevel) player.level(), UUID.fromString(entry.getKey()));
                if (mob != null) {
                    types.add(mob.getType().toString());
                    if (types.size() >= 10) {
                        award(player, Advancements.SOCIAL_BUTTERFLY.id);
                        return;
                    }
                }
            }
        }
    }

    public static void checkPopularOpinion(ServerPlayer player) {
        String playerName = player.getDisplayName().getString();
        ChatDataManager manager = ChatDataManager.getServerInstance();
        List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(12.0));
        int count = 0;
        for (Mob m : mobs) {
            EntityChatData chat = manager.entityChatDataMap.get(m.getStringUUID());
            if (chat == null) continue;
            PlayerData pd = chat.players.get(playerName);
            if (pd != null && pd.friendship >= 2) {
                if (++count >= 10) {
                    award(player, Advancements.POPULAR_OPINION.id);
                    break;
                }
            }
        }
    }

    public static void openSesame(ServerPlayer player) {
        award(player, Advancements.OPEN_SESAME.id);
    }

    public static void sleightOfHand(ServerPlayer player) {
        award(player, Advancements.SLEIGHT_OF_HAND.id);
    }

    public static void guidedTour(ServerPlayer player) {
        award(player, Advancements.GUIDED_TOUR.id);
    }

    public static void potatoWar(ServerPlayer player) {
        award(player, Advancements.POTATO_WAR.id);
    }

    public static void aLegend(ServerPlayer player) {
        award(player, Advancements.A_LEGEND.id);
    }

    public static void theHeist(ServerPlayer player) {
        award(player, Advancements.THE_HEIST.id);
    }

    public static void enderEscort(ServerPlayer player) {
        award(player, Advancements.ENDER_ESCORT.id);
    }
}
