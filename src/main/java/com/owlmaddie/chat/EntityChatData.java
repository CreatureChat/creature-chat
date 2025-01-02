package com.owlmaddie.chat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.owlmaddie.commands.ConfigurationHandler;
import com.owlmaddie.controls.SpeedControls;
import com.owlmaddie.goals.*;
import com.owlmaddie.message.Behavior;
import com.owlmaddie.message.MessageParser;
import com.owlmaddie.message.ParsedMessage;
import com.owlmaddie.network.ServerPackets;
import com.owlmaddie.particle.ParticleEmitter;
import com.owlmaddie.utils.Randomizer;
import com.owlmaddie.utils.ServerEntityFinder;
import com.owlmaddie.utils.VillagerEntityAccessor;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.VillageGossipType;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.owlmaddie.network.ServerPackets.*;

/**
 * The {@code EntityChatData} class represents a conversation between an
 * entity and one or more players, including friendship, character sheets,
 * and the status of the current displayed message.
 */
public class EntityChatData {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    public String entityId;
    public String currentMessage;
    public int currentLineNumber;
    public ChatDataManager.ChatStatus status;
    public String characterSheet;
    public ChatDataManager.ChatSender sender;
    public int auto_generated;
    public List<ChatMessage> previousMessages;
    public Long born;
    public Long death;

    @SerializedName("playerId")
    @Expose(serialize = false)
    private String legacyPlayerId;

    @SerializedName("friendship")
    @Expose(serialize = false)
    public Integer legacyFriendship;

    // The map to store data for each player interacting with this entity
    public Map<String, PlayerData> players;

    public EntityChatData(String entityId) {
        this.entityId = entityId;
        this.players = new HashMap<>();
        this.currentMessage = "";
        this.currentLineNumber = 0;
        this.characterSheet = "";
        this.status = ChatDataManager.ChatStatus.NONE;
        this.sender = ChatDataManager.ChatSender.USER;
        this.auto_generated = 0;
        this.previousMessages = new ArrayList<>();
        this.born = System.currentTimeMillis();;

        // Old, unused migrated properties
        this.legacyPlayerId = null;
        this.legacyFriendship = null;
    }

    // Post-deserialization initialization
    public void postDeserializeInitialization() {
        if (this.players == null) {
            this.players = new HashMap<>(); // Ensure players map is initialized
        }
        if (this.legacyPlayerId != null && !this.legacyPlayerId.isEmpty()) {
            this.migrateData();
        }
    }

    // Migrate old data into the new structure
    private void migrateData() {
        // Ensure the blank player data entry exists
        PlayerData blankPlayerData = this.players.computeIfAbsent("", k -> new PlayerData());

        // Update the previousMessages arraylist and add timestamps if missing
        if (this.previousMessages != null) {
            for (ChatMessage message : this.previousMessages) {
                if (message.timestamp == null) {
                    message.timestamp = System.currentTimeMillis();
                }
                if (message.name == null || message.name.isEmpty()) {
                    message.name = "";
                }
            }
        }
        blankPlayerData.friendship = this.legacyFriendship;
        if (this.born == null) {
            this.born = System.currentTimeMillis();;
        }

        // Clean up old player data
        this.legacyPlayerId = null;
        this.legacyFriendship = null;
    }

    // Get the player data (or fallback to the blank player)
    public PlayerData getPlayerData(String playerName) {
        if (this.players == null) {
            return new PlayerData();
        }

        // Check if the playerId exists in the players map
        if (this.players.containsKey("")) {
            // If a blank migrated legacy entity is found, always return this
            return this.players.get("");

        } else if (this.players.containsKey(playerName)) {
            // Return a specific player's data
            return this.players.get(playerName);

        } else {
            // Return a blank player data
            PlayerData newPlayerData = new PlayerData();
            this.players.put(playerName, newPlayerData);
            return newPlayerData;
        }
    }

    // Generate light version of chat data (no previous messages)
    public EntityChatDataLight toLightVersion(String playerName) {
        return new EntityChatDataLight(this, playerName);
    }

    public String getCharacterProp(String propertyName) {
        // Create a case-insensitive regex pattern to match the property name and capture its value
        Pattern pattern = Pattern.compile("-?\\s*" + Pattern.quote(propertyName) + ":\\s*(.+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(characterSheet);

        if (matcher.find()) {
            // Return the captured value, trimmed of any excess whitespace
            return matcher.group(1).trim().replace("\"", "");
        }

        return "N/A";
    }

    // Generate context object
    public Map<String, String> getPlayerContext(ServerPlayerEntity player, String userLanguage, ConfigurationHandler.Config config) {
        // Add PLAYER context information
        Map<String, String> contextData = new HashMap<>();
        contextData.put("player_name", player.getDisplayName().getString());
        contextData.put("player_health", player.getHealth() + "/" + player.getMaxHealth());
        contextData.put("player_hunger", String.valueOf(player.getHungerManager().getFoodLevel()));
        contextData.put("player_held_item", String.valueOf(player.getMainHandStack().getItem().toString()));
        contextData.put("player_biome", player.getWorld().getBiome(player.getBlockPos()).getKey().get().getValue().getPath());
        contextData.put("player_is_creative", player.isCreative() ? "yes" : "no");
        contextData.put("player_is_swimming", player.isSwimming() ? "yes" : "no");
        contextData.put("player_is_on_ground", player.isOnGround() ? "yes" : "no");
        contextData.put("player_language", userLanguage);

        ItemStack feetArmor = player.getInventory().armor.get(0);
        ItemStack legsArmor = player.getInventory().armor.get(1);
        ItemStack chestArmor = player.getInventory().armor.get(2);
        ItemStack headArmor = player.getInventory().armor.get(3);
        contextData.put("player_armor_head", headArmor.getItem().toString());
        contextData.put("player_armor_chest", chestArmor.getItem().toString());
        contextData.put("player_armor_legs", legsArmor.getItem().toString());
        contextData.put("player_armor_feet", feetArmor.getItem().toString());

        // Get active player effects
        String effectsString = player.getActiveStatusEffects().entrySet().stream()
                .map(entry -> entry.getKey().getTranslationKey() + " x" + (entry.getValue().getAmplifier() + 1))
                .collect(Collectors.joining(", "));
        contextData.put("player_active_effects", effectsString);

        // Add custom story section (if any)
        if (!config.getStory().isEmpty()) {
            contextData.put("story", "Story: " + config.getStory());
        } else {
            contextData.put("story", "");
        }


        // Get World time (as 24 hour value)
        int hours = (int) ((player.getWorld().getTimeOfDay() / 1000 + 6) % 24); // Minecraft day starts at 6 AM
        int minutes = (int) (((player.getWorld().getTimeOfDay() % 1000) / 1000.0) * 60);
        contextData.put("world_time", String.format("%02d:%02d", hours, minutes));
        contextData.put("world_is_raining", player.getWorld().isRaining() ? "yes" : "no");
        contextData.put("world_is_thundering", player.getWorld().isThundering() ? "yes" : "no");
        contextData.put("world_difficulty", player.getWorld().getDifficulty().getName());
        contextData.put("world_is_hardcore", player.getWorld().getLevelProperties().isHardcore() ? "yes" : "no");

        // Get moon phase
        String moonPhaseDescription = switch (player.getWorld().getMoonPhase()) {
            case 0 -> "Full Moon";
            case 1 -> "Waning Gibbous";
            case 2 -> "Last Quarter";
            case 3 -> "Waning Crescent";
            case 4 -> "New Moon";
            case 5 -> "Waxing Crescent";
            case 6 -> "First Quarter";
            case 7 -> "Waxing Gibbous";
            default -> "Unknown";
        };
        contextData.put("world_moon_phase", moonPhaseDescription);

        // Get Entity details
        MobEntity entity = (MobEntity) ServerEntityFinder.getEntityByUUID(player.getServerWorld(), UUID.fromString(entityId));
        if (entity.getCustomName() == null) {
            contextData.put("entity_name", "");
        } else {
            contextData.put("entity_name", entity.getCustomName().getString());
        }
        contextData.put("entity_type", entity.getType().getName().getString());
        contextData.put("entity_health", entity.getHealth() + "/" + entity.getMaxHealth());
        contextData.put("entity_personality", getCharacterProp("Personality"));
        contextData.put("entity_speaking_style", getCharacterProp("Speaking Style / Tone"));
        contextData.put("entity_likes", getCharacterProp("Likes"));
        contextData.put("entity_dislikes", getCharacterProp("Dislikes"));
        contextData.put("entity_age", getCharacterProp("Age"));
        contextData.put("entity_alignment", getCharacterProp("Alignment"));
        contextData.put("entity_class", getCharacterProp("Class"));
        contextData.put("entity_skills", getCharacterProp("Skills"));
        contextData.put("entity_background", getCharacterProp("Background"));

        PlayerData playerData = this.getPlayerData(player.getDisplayName().getString());
        if (playerData != null) {
            contextData.put("entity_friendship", String.valueOf(playerData.friendship));
        } else {
            contextData.put("entity_friendship", String.valueOf(0));
        }

        return contextData;
    }

    // Generate a new character
    public void generateCharacter(String userLanguage, ServerPlayerEntity player, String userMessage, boolean is_auto_message) {
        String systemPrompt = "system-character";
        if (is_auto_message) {
            // Increment an auto-generated message
            this.auto_generated++;
        } else {
            // Reset auto-generated counter
            this.auto_generated = 0;
        }

        // Add USER Message
        this.addMessage(userMessage, ChatDataManager.ChatSender.USER, player, systemPrompt);

        // Get config (api key, url, settings)
        ConfigurationHandler.Config config = new ConfigurationHandler(ServerPackets.serverInstance).loadConfig();
        String promptText = ChatPrompt.loadPromptFromResource(ServerPackets.serverInstance.getResourceManager(), systemPrompt);

        // Add PLAYER context information
        Map<String, String> contextData = getPlayerContext(player, userLanguage, config);

        // fetch HTTP response from ChatGPT
        ChatGPTRequest.fetchMessageFromChatGPT(config, promptText, contextData, previousMessages, false).thenAccept(output_message -> {
            if (output_message != null) {
                // Character Sheet: Remove system-character message from previous messages
                previousMessages.clear();

                // Add NEW CHARACTER sheet & greeting
                this.characterSheet = output_message;
                String shortGreeting = Optional.ofNullable(getCharacterProp("short greeting")).filter(s -> !s.isEmpty()).orElse(Randomizer.getRandomMessage(Randomizer.RandomType.NO_RESPONSE)).replace("\n", " ");
                this.addMessage(shortGreeting, ChatDataManager.ChatSender.ASSISTANT, player, systemPrompt);

            } else {
                // Error / No Chat Message (Failure)
                String randomErrorMessage = Randomizer.getRandomMessage(Randomizer.RandomType.ERROR);
                this.addMessage(randomErrorMessage, ChatDataManager.ChatSender.ASSISTANT, player, systemPrompt);

                // Determine error message to display
                String errorMessage = "Help is available at discord.creaturechat.com";
                if (!ChatGPTRequest.lastErrorMessage.isEmpty()) {
                    errorMessage = "Error: " + truncateString(ChatGPTRequest.lastErrorMessage, 55) + "\n" + errorMessage;
                }

                // Send clickable error message
                ServerPackets.SendClickableError(player,
                        errorMessage, "http://discord.creaturechat.com");

                // Clear history (if no character sheet was generated)
                if (characterSheet.isEmpty()) {
                    previousMessages.clear();
                }
            }
        });
    }

    // Generate greeting
    public void generateMessage(String userLanguage, ServerPlayerEntity player, String userMessage, boolean is_auto_message) {
        String systemPrompt = "system-chat";
        if (is_auto_message) {
            // Increment an auto-generated message
            this.auto_generated++;
        } else {
            // Reset auto-generated counter
            this.auto_generated = 0;
        }

        // Add USER Message
        this.addMessage(userMessage, ChatDataManager.ChatSender.USER, player, systemPrompt);

        // Get config (api key, url, settings)
        ConfigurationHandler.Config config = new ConfigurationHandler(ServerPackets.serverInstance).loadConfig();
        String promptText = ChatPrompt.loadPromptFromResource(ServerPackets.serverInstance.getResourceManager(), systemPrompt);

        // Add PLAYER context information
        Map<String, String> contextData = getPlayerContext(player, userLanguage, config);

        // Get messages for player
        PlayerData playerData = this.getPlayerData(player.getDisplayName().getString());
        if (previousMessages.size() == 1) {
            // No messages exist yet for this player (start with normal greeting)
            String shortGreeting = Optional.ofNullable(getCharacterProp("short greeting")).filter(s -> !s.isEmpty()).orElse(Randomizer.getRandomMessage(Randomizer.RandomType.NO_RESPONSE)).replace("\n", " ");
            previousMessages.add(0, new ChatMessage(shortGreeting, ChatDataManager.ChatSender.ASSISTANT, player.getDisplayName().getString()));
        }

        // fetch HTTP response from ChatGPT
        ChatGPTRequest.fetchMessageFromChatGPT(config, promptText, contextData, previousMessages, false).thenAccept(output_message -> {
            if (output_message != null) {
                // Chat Message: Parse message for behaviors
                ParsedMessage result = MessageParser.parseMessage(output_message.replace("\n", " "));
                MobEntity entity = (MobEntity)ServerEntityFinder.getEntityByUUID(player.getServerWorld(), UUID.fromString(entityId));

                // Determine entity's default speed
                // Some Entities (i.e. Axolotl) set this incorrectly... so adjusting in the SpeedControls class
                float entitySpeed = SpeedControls.getMaxSpeed(entity);
                float entitySpeedMedium = MathHelper.clamp(entitySpeed * 1.15F, 0.5f, 1.15f);
                float entitySpeedFast = MathHelper.clamp(entitySpeed * 1.3F, 0.5f, 1.3f);

                // Apply behaviors (if any)
                for (Behavior behavior : result.getBehaviors()) {
                    LOGGER.info("Behavior: " + behavior.getName() + (behavior.getArgument() != null ?
                            ", Argument: " + behavior.getArgument() : ""));

                    // Apply behaviors to entity
                    if (behavior.getName().equals("FOLLOW")) {
                        FollowPlayerGoal followGoal = new FollowPlayerGoal(player, entity, entitySpeedMedium);
                        EntityBehaviorManager.removeGoal(entity, TalkPlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, LeadPlayerGoal.class);
                        EntityBehaviorManager.addGoal(entity, followGoal, GoalPriority.FOLLOW_PLAYER);
                        if (playerData.friendship >= 0) {
                            ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, FOLLOW_FRIEND_PARTICLE, 0.5, 1);
                        } else {
                            ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, FOLLOW_ENEMY_PARTICLE, 0.5, 1);
                        }

                    } else if (behavior.getName().equals("STOP")) {
                        EntityBehaviorManager.removeGoal(entity, FollowPlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, ProtectPlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, LeadPlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);

                    } else if (behavior.getName().equals("FLEE")) {
                        float fleeDistance = 40F;
                        FleePlayerGoal fleeGoal = new FleePlayerGoal(player, entity, entitySpeedFast, fleeDistance);
                        EntityBehaviorManager.removeGoal(entity, TalkPlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, FollowPlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, ProtectPlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, LeadPlayerGoal.class);
                        EntityBehaviorManager.addGoal(entity, fleeGoal, GoalPriority.FLEE_PLAYER);
                        ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, FLEE_PARTICLE, 0.5, 1);

                    } else if (behavior.getName().equals("ATTACK")) {
                        AttackPlayerGoal attackGoal = new AttackPlayerGoal(player, entity, entitySpeedFast);
                        EntityBehaviorManager.removeGoal(entity, TalkPlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, FollowPlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, ProtectPlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, LeadPlayerGoal.class);
                        EntityBehaviorManager.addGoal(entity, attackGoal, GoalPriority.ATTACK_PLAYER);
                        ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, FLEE_PARTICLE, 0.5, 1);

                    } else if (behavior.getName().equals("PROTECT")) {
                        if (playerData.friendship <= 0) {
                            // force friendship to prevent entity from attacking player when protecting
                            playerData.friendship = 1;
                        }
                        ProtectPlayerGoal protectGoal = new ProtectPlayerGoal(player, entity, 1.0);
                        EntityBehaviorManager.removeGoal(entity, TalkPlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                        EntityBehaviorManager.addGoal(entity, protectGoal, GoalPriority.PROTECT_PLAYER);
                        ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, PROTECT_PARTICLE, 0.5, 1);

                    } else if (behavior.getName().equals("LEAD")) {
                        LeadPlayerGoal leadGoal = new LeadPlayerGoal(player, entity, entitySpeedMedium);
                        EntityBehaviorManager.removeGoal(entity, FollowPlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                        EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                        EntityBehaviorManager.addGoal(entity, leadGoal, GoalPriority.LEAD_PLAYER);
                        if (playerData.friendship >= 0) {
                            ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, LEAD_FRIEND_PARTICLE, 0.5, 1);
                        } else {
                            ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, LEAD_ENEMY_PARTICLE, 0.5, 1);
                        }

                    } else if (behavior.getName().equals("FRIENDSHIP")) {
                        int new_friendship = Math.max(-3, Math.min(3, behavior.getArgument()));

                        // Does friendship improve?
                        if (new_friendship > playerData.friendship) {
                            // Stop any attack/flee if friendship improves
                            EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                            EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);

                            if (entity instanceof EnderDragonEntity && new_friendship == 3) {
                                // Trigger end of game (friendship always wins!)
                                EnderDragonEntity dragon = (EnderDragonEntity) entity;

                                // Emit particles & sound
                                ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, HEART_BIG_PARTICLE, 3, 200);
                                entity.getWorld().playSound(entity, entity.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.PLAYERS, 0.3F, 1.0F);
                                entity.getWorld().playSound(entity, entity.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.5F, 1.0F);

                                // Check if the game rule for mob loot is enabled
                                boolean doMobLoot = entity.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_LOOT);

                                // If this is the first time the dragon is 'befriended', adjust the XP
                                int baseXP = 500;
                                if (dragon.getFight() != null && !dragon.getFight().hasPreviouslyKilled()) {
                                    baseXP = 12000;
                                }

                                // If the world is a server world and mob loot is enabled, spawn XP orbs
                                if (entity.getWorld() instanceof ServerWorld && doMobLoot) {
                                    // Loop to spawn XP orbs
                                    for (int j = 1; j <= 11; j++) {
                                        float xpFraction = (j == 11) ? 0.2F : 0.08F;
                                        int xpAmount = MathHelper.floor((float)baseXP * xpFraction);
                                        ExperienceOrbEntity.spawn((ServerWorld)entity.getWorld(), entity.getPos(), xpAmount);
                                    }
                                }

                                // Mark fight as over
                                dragon.getFight().dragonKilled(dragon);
                            }
                        }

                        // Merchant deals (if friendship changes with a Villager
                        if (entity instanceof VillagerEntity && playerData.friendship != new_friendship) {
                            VillagerEntityAccessor villager = (VillagerEntityAccessor) entity;
                            switch (new_friendship) {
                                case 3:
                                    villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MAJOR_POSITIVE, 20);
                                    villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_POSITIVE, 25);
                                    break;
                                case 2:
                                    villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_POSITIVE, 25);
                                    break;
                                case 1:
                                    villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_POSITIVE, 10);
                                    break;
                                case -1:
                                    villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_NEGATIVE, 10);
                                    break;
                                case -2:
                                    villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_NEGATIVE, 25);
                                    break;
                                case -3:
                                    villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MAJOR_NEGATIVE, 20);
                                    villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_NEGATIVE, 25);
                                    break;
                            }
                        }

                        // Tame best friends and un-tame worst enemies
                        if (entity instanceof TameableEntity && playerData.friendship != new_friendship) {
                            TameableEntity tamableEntity = (TameableEntity) entity;
                            if (new_friendship == 3 && !tamableEntity.isTamed()) {
                                tamableEntity.setOwner(player);
                            } else if (new_friendship == -3 && tamableEntity.isTamed()) {
                                tamableEntity.setTamed(false);
                                tamableEntity.setOwnerUuid(null);
                            }
                        }

                        // Emit friendship particles
                        if (playerData.friendship != new_friendship) {
                            int friendDiff = new_friendship - playerData.friendship;
                            if (friendDiff > 0) {
                                // Heart particles
                                if (new_friendship == 3) {
                                    ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, HEART_BIG_PARTICLE, 0.5, 10);
                                } else {
                                    ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, HEART_SMALL_PARTICLE, 0.1, 1);
                                }

                            } else if (friendDiff < 0) {
                                // Fire particles
                                if (new_friendship == -3) {
                                    ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, FIRE_BIG_PARTICLE, 0.5, 10);
                                } else {
                                    ParticleEmitter.emitCreatureParticle((ServerWorld) entity.getWorld(), entity, FIRE_SMALL_PARTICLE, 0.1, 1);
                                }
                            }
                        }

                        playerData.friendship = new_friendship;
                    }
                }

                // Add ASSISTANT message to history
                this.addMessage(result.getOriginalMessage(), ChatDataManager.ChatSender.ASSISTANT, player, systemPrompt);

                // Get cleaned message (i.e. no <BEHAVIOR> strings)
                String cleanedMessage = result.getCleanedMessage();
                if (cleanedMessage.isEmpty()) {
                    cleanedMessage = Randomizer.getRandomMessage(Randomizer.RandomType.NO_RESPONSE);
                }
                // Update the current message to a 'cleaned version'
                this.currentMessage = cleanedMessage;

            } else {
                // Error / No Chat Message (Failure)
                String randomErrorMessage = Randomizer.getRandomMessage(Randomizer.RandomType.ERROR);
                this.addMessage(randomErrorMessage, ChatDataManager.ChatSender.ASSISTANT, player, systemPrompt);

                // Determine error message to display
                String errorMessage = "Help is available at discord.creaturechat.com";
                if (!ChatGPTRequest.lastErrorMessage.isEmpty()) {
                    errorMessage = "Error: " + truncateString(ChatGPTRequest.lastErrorMessage, 55) + "\n" + errorMessage;
                }

                // Send clickable error message
                ServerPackets.SendClickableError(player,
                        errorMessage, "http://discord.creaturechat.com");

                // Clear history (if no character sheet was generated)
                if (characterSheet.isEmpty()) {
                    previousMessages.clear();
                }
            }

            // Broadcast to all players
            ServerPackets.BroadcastPacketMessage(this, player);
        });
    }

    public static String truncateString(String input, int maxLength) {
        return input.length() > maxLength ? input.substring(0, maxLength - 3) + "..." : input;
    }

    // Add a message to the history and update the current message
    public void addMessage(String message, ChatDataManager.ChatSender sender, ServerPlayerEntity player, String systemPrompt) {
        // Truncate message (prevent crazy long messages... just in case)
        String truncatedMessage = message.substring(0, Math.min(message.length(), ChatDataManager.MAX_CHAR_IN_USER_MESSAGE));

        // Add context-switching logic for USER messages only
        String playerName = player.getDisplayName().getString();
        if (sender == ChatDataManager.ChatSender.USER && previousMessages.size() > 1) {
            ChatMessage lastMessage = previousMessages.get(previousMessages.size() - 1);
            if (lastMessage.name == null || !lastMessage.name.equals(playerName)) {  // Null-safe check
                boolean isReturningPlayer = previousMessages.stream().anyMatch(msg -> playerName.equals(msg.name)); // Avoid NPE here too
                String note = isReturningPlayer
                        ? "<returning player: " + playerName + " resumes the conversation>"
                        : "<a new player has joined the conversation: " + playerName + ">";
                previousMessages.add(new ChatMessage(note, sender, playerName));

                // Log context-switching message
                LOGGER.info("Conversation-switching message: status=PENDING, sender={}, message={}, player={}, entity={}",
                        ChatDataManager.ChatStatus.PENDING, note, playerName, entityId);
            }
        }

        // Add message to history
        previousMessages.add(new ChatMessage(truncatedMessage, sender, playerName));

        // Log regular message addition
        LOGGER.info("Message added: status={}, sender={}, message={}, player={}, entity={}",
                status.toString(), sender.toString(), truncatedMessage, playerName, entityId);

        // Update current message and reset line number of displayed text
        this.currentMessage = truncatedMessage;
        this.currentLineNumber = 0;
        this.sender = sender;

        // Determine status for message
        if (sender == ChatDataManager.ChatSender.ASSISTANT) {
            status = ChatDataManager.ChatStatus.DISPLAY;
        } else if (sender == ChatDataManager.ChatSender.USER && systemPrompt.equals("system-chat")) {
            // Only show system-chat messages above players (not system-character ones)
            status = ChatDataManager.ChatStatus.DISPLAY;
        } else {
            status = ChatDataManager.ChatStatus.PENDING;
        }

        // Broadcast to all players
        ServerPackets.BroadcastPacketMessage(this, player);
    }

    // Get wrapped lines
    public List<String> getWrappedLines() {
        return LineWrapper.wrapLines(this.currentMessage, ChatDataManager.MAX_CHAR_PER_LINE);
    }

    public boolean isEndOfMessage() {
        int totalLines = this.getWrappedLines().size();
        // Check if the current line number plus DISPLAY_NUM_LINES covers or exceeds the total number of lines
        return currentLineNumber + ChatDataManager.DISPLAY_NUM_LINES >= totalLines;
    }

    public void setLineNumber(Integer lineNumber) {
        int totalLines = this.getWrappedLines().size();
        // Ensure the lineNumber is within the valid range
        currentLineNumber = Math.min(Math.max(lineNumber, 0), totalLines);

        // Broadcast to all players
        ServerPackets.BroadcastPacketMessage(this, null);
    }

    public void setStatus(ChatDataManager.ChatStatus new_status) {
        status = new_status;

        // Broadcast to all players
        ServerPackets.BroadcastPacketMessage(this, null);
    }
}