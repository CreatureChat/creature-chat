// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.mixin;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.network.ServerPackets;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The {@code MixinLivingEntity} class modifies the behavior of {@link LivingEntity} to integrate
 * custom friendship, chat, and death message mechanics. It prevents friendly entities from targeting players,
 * generates contextual chat messages on attacks, and broadcasts custom death messages for named entities.
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    private EntityChatData getChatData(LivingEntity entity) {
        ChatDataManager chatDataManager = ChatDataManager.getServerInstance();
        return chatDataManager.getOrCreateChatData(entity.getStringUUID());
    }

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void modifyCanTarget(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (target instanceof Player) {
            LivingEntity thisEntity = (LivingEntity) (Object) this;
            EntityChatData entityData = getChatData(thisEntity);
            PlayerData playerData = entityData.getPlayerData(target.getDisplayName().getString());
            if (playerData.friendship > 0) {
                // Friendly creatures can't target a player
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("RETURN"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            // If damage method returned false, it means the damage was not applied (possibly due to invulnerability).
            return;
        }

        // Get attacker and entity objects
        Entity attacker = source.getEntity();
        LivingEntity thisEntity = (LivingEntity) (Object) this;

        // If PLAYER attacks MOB then
        if (attacker instanceof Player && thisEntity instanceof Mob && !thisEntity.isDeadOrDying()) {
            // Generate attacked message (only if the previous user message was not an attacked message)
            // We don't want to constantly generate messages during a prolonged, multi-damage event
            ServerPlayer player = (ServerPlayer) attacker;
            EntityChatData chatData = getChatData(thisEntity);
            PlayerData playerData = chatData.getPlayerData(player.getDisplayName().getString());
            playerData.lastDamageFriendship = playerData.friendship;
            playerData.wordsmithDamaged = true;
            if (!chatData.characterSheet.isEmpty()) {

                ItemStack weapon = player.getMainHandItem();
                String weaponName = weapon.isEmpty() ? "with fists" : "with " + weapon.getItem().toString();

                // Determine if the damage was indirect
                boolean isIndirect = attacker != null && attacker != source.getDirectEntity();
                String directness = isIndirect ? "indirectly" : "directly";

                String attackedMessage = "<" + player.getDisplayName().getString() + " attacked you " + directness + " with " + weaponName + ">";
                ServerPackets.generate_chat("N/A", chatData, player, (Mob) thisEntity, attackedMessage, true);
            }
        }
    }

    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"))
    private void onDeath(DamageSource source, CallbackInfo info) {
        LivingEntity entity = (LivingEntity) (Object) this;
        Level world = entity.level();

        if (!world.isClientSide() && entity.hasCustomName()) {
            // Skip tamed entities and players
            if (entity instanceof TamableAnimal && ((TamableAnimal) entity).isTame()) {
                return;
            }

            if (entity instanceof Player) {
                return;
            }

            // Get chatData for the entity
            EntityChatData chatData = getChatData(entity);
            if (chatData != null && !chatData.characterSheet.isEmpty()) {
                // Get the original death message
                Component deathMessage = entity.getCombatTracker().getDeathMessage();
                // Broadcast the death message to all players in the world
                ServerPackets.BroadcastMessage(deathMessage);
            }
        }
    }
}
