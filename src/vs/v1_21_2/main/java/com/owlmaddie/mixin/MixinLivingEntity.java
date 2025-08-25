// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.mixin;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.network.ServerPackets;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * Modifies LivingEntity: prevents friendly targeting, auto-chat on damage,
 * and custom death messages.
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    private EntityChatData getChatData(LivingEntity entity) {
        ChatDataManager chatDataManager = ChatDataManager.getServerInstance();
        return chatDataManager.getOrCreateChatData(entity.getStringUUID());
    }

    @Inject(
            method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void modifyCanAttack(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
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

    @Inject(
            method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at     = @At("RETURN")
    )
    private void onHurt(ServerLevel world,
                        DamageSource source,
                        float amount,
                        CallbackInfoReturnable<Boolean> cir) {
        this.handleOnDamage(source, amount, cir);
    }

    /**
     * Shared logic for post-damage chat generation.
     */
    private void handleOnDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;

        Entity attacker = source.getEntity();
        LivingEntity self = (LivingEntity)(Object)this;

        if (attacker instanceof Player player
                && self instanceof Mob mob
                && !mob.isDeadOrDying()) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            EntityChatData data = ChatDataManager
                    .getServerInstance()
                    .getOrCreateChatData(mob.getStringUUID());

            PlayerData pd = data.getPlayerData(serverPlayer.getDisplayName().getString());
            pd.lastDamageFriendship = pd.friendship;
            pd.wordsmithDamaged = true;
            if (!data.characterSheet.isEmpty()) {
                ItemStack weapon = serverPlayer.getMainHandItem();
                String weaponName = weapon.isEmpty()
                        ? "with fists"
                        : "with " + weapon.getItem().toString();

                boolean indirect = source.getDirectEntity() != attacker;
                String directness = indirect ? "indirectly" : "directly";

                String msg = "<" + player.getDisplayName().getString()
                        + " attacked you " + directness
                        + " " + weaponName + ">";
                ServerPackets.generate_chat("N/A", data, serverPlayer, mob, msg, true);
            }
        }
    }

    @Inject(
            method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At("HEAD")
    )
    private void onDeath(DamageSource source, CallbackInfo ci) {
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
