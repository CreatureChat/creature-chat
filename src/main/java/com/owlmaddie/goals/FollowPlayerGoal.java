// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.goals;

import com.owlmaddie.chat.AdvancementHelper;
import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.controls.LookControls;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

/**
 * The {@code FollowPlayerGoal} class instructs a Mob Entity to follow the current target entity.
 */
public class FollowPlayerGoal extends PlayerBaseGoal {
    private final Mob entity;
    private final PathNavigation navigation;
    private final double speed;

    public FollowPlayerGoal(ServerPlayer player, Mob entity, double speed) {
        super(player);
        this.entity = entity;
        this.speed = speed;
        this.navigation = entity.getNavigation();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Start only if the target player is more than 8 blocks away
        return super.canUse() && this.entity.distanceToSqr(this.targetEntity) > 64;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue unless the entity gets within 3 blocks of the player
        return super.canUse() && this.entity.distanceToSqr(this.targetEntity) > 9;
    }

    @Override
    public void stop() {
        // Stop the entity temporarily
        this.navigation.stop();
    }

    @Override
    public void tick() {
        if (this.targetEntity instanceof ServerPlayer player) {
            ChatDataManager manager = ChatDataManager.getServerInstance();
            EntityChatData data = manager.getOrCreateChatData(this.entity.getStringUUID());
            PlayerData pd = data.getPlayerData(player.getDisplayName().getString());
            if (this.entity.level().dimension() == Level.OVERWORLD) {
                pd.wasInOverworld = true;
            }
            if (this.entity.level().dimension() == Level.END && pd.friendship > 0 && pd.wasInOverworld) {
                AdvancementHelper.enderEscort(player);
                pd.wasInOverworld = false;
            }
        }
        if (this.entity instanceof EnderMan || this.entity instanceof Endermite || this.entity instanceof Shulker) {
            // Certain entities should teleport to the player if they get too far
            if (this.entity.distanceToSqr(this.targetEntity) > 256) {
                Vec3 targetPos = findTeleportPosition(12);
                if (targetPos != null) {
                    this.entity.randomTeleport(targetPos.x, targetPos.y, targetPos.z, true);
                }
            }
        } else {
            // Look at the player and start moving towards them
            if (this.targetEntity instanceof ServerPlayer) {
                LookControls.lookAtPlayer((ServerPlayer)this.targetEntity, this.entity);
            }
            this.navigation.moveTo(this.targetEntity, this.speed);
        }
    }

    private Vec3 findTeleportPosition(int distance) {
        if (this.entity instanceof PathfinderMob) {
            return LandRandomPos.getPosTowards((PathfinderMob) this.entity, distance, distance, this.targetEntity.position());
        }
        return null;
    }
}
