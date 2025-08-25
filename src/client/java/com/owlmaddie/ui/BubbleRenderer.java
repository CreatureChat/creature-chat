// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.render.BlendHelper;
import com.owlmaddie.render.EntityTextureHelper;
import com.owlmaddie.render.QuadBuffer;
import com.owlmaddie.render.ShaderHelper;
import com.owlmaddie.skin.PlayerCustomTexture;
import com.owlmaddie.utils.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The {@code BubbleRenderer} class provides static methods to render the chat UI bubble, entity icons,
 * text, friendship status, and other UI-related rendering code.
 */
public class BubbleRenderer {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    protected static TextureLoader textures = new TextureLoader();
    public static int DISPLAY_PADDING = 2;
    public static int animationFrame = 0;
    public static long lastTick = 0;
    public static int light = 15728880;
    public static int overlay = OverlayTexture.NO_OVERLAY;
    private static final float TEXT_Z_OFFSET = -0.05F;
    public static List<String> whitelist = new ArrayList<>();
    public static List<String> blacklist = new ArrayList<>();
    private static int queryEntityDataCount = 0;
    private static List<Entity> relevantEntities;

    public static void drawTextBubbleBackground(String base_name, PoseStack matrices, float x, float y, float width, float height, int friendship) {
        // Set shader & texture
        ShaderHelper.setTexturedShader();

        // Enable depth test and blending
        BlendHelper.enableBlend();
        BlendHelper.defaultBlendFunc();
        BlendHelper.enableDepthTest();
        BlendHelper.depthMask(true);

        // Get the buffer instance
        QuadBuffer buffer = QuadBuffer.INSTANCE;
        float z = 0.01F;

        // Draw UI text background (based on friendship)
        // Draw TOP
        if (friendship == -3 && !base_name.endsWith("-player")) {
            TextureLoader.bind(0, textures.GetUI(base_name + "-enemy"));
        } else if (friendship == 3 && !base_name.endsWith("-player")) {
            TextureLoader.bind(0, textures.GetUI(base_name + "-friend"));
        } else {
            TextureLoader.bind(0, textures.GetUI(base_name));
        }
        drawTexturePart(matrices, buffer, x - 50, y, z, 228, 40);

        // Draw MIDDLE
        TextureLoader.bind(0, textures.GetUI("text-middle"));
        drawTexturePart(matrices, buffer, x, y + 40, z, width, height);

        // Draw BOTTOM
        TextureLoader.bind(0, textures.GetUI("text-bottom"));
        drawTexturePart(matrices, buffer, x, y + 40 + height, z, width, 5);

        // Disable blending and depth test
        BlendHelper.disableBlend();
        BlendHelper.disableDepthTest();
    }

    private static void drawTexturePart(PoseStack matrices, QuadBuffer buffer, float x, float y, float z, float width, float height) {
        // Define the vertices with color, texture, light, and overlay
        Matrix4f matrix4f = matrices.last().pose();

        // Begin drawing quads with the correct vertex format
        buffer.begin();

        buffer.vertex(matrix4f, x, y + height, z).color(255, 255, 255, 255).texture(0, 1).light(light).overlay(overlay);  // bottom left
        buffer.vertex(matrix4f, x + width, y + height, z).color(255, 255, 255, 255).texture(1, 1).light(light).overlay(overlay);   // bottom right
        buffer.vertex(matrix4f, x + width, y, z).color(255, 255, 255, 255).texture(1, 0).light(light).overlay(overlay);  // top right
        buffer.vertex(matrix4f, x, y, z).color(255, 255, 255, 255).texture(0, 0).light(light).overlay(overlay); // top left
        buffer.draw();
    }

    private static void drawIcon(String ui_icon_name, PoseStack matrices, float x, float y, float width, float height) {
        // Draw button icon
        ResourceLocation button_texture = textures.GetUI(ui_icon_name);

        // Set shader & texture
        ShaderHelper.setTexturedShader();
        TextureLoader.bind(0, button_texture);

        // Enable depth test and blending
        BlendHelper.enableBlend();
        BlendHelper.defaultBlendFunc();
        BlendHelper.enableDepthTest();
        BlendHelper.depthMask(true);

        // Get the buffer instance
        QuadBuffer buffer = QuadBuffer.INSTANCE;

        // Get the current matrix position
        Matrix4f matrix4f = matrices.last().pose();

        // Begin drawing quads with the correct vertex format
        buffer.begin();

        buffer.vertex(matrix4f, x, y + height, 0.0F).color(255, 255, 255, 255).texture(0, 1).light(light).overlay(overlay); // bottom left
        buffer.vertex(matrix4f, x + width, y + height, 0.0F).color(255, 255, 255, 255).texture(1, 1).light(light).overlay(overlay); // bottom right
        buffer.vertex(matrix4f, x + width, y, 0.0F).color(255, 255, 255, 255).texture(1, 0).light(light).overlay(overlay); // top right
        buffer.vertex(matrix4f, x, y, 0.0F).color(255, 255, 255, 255).texture(0, 0).light(light).overlay(overlay); // top left
        buffer.draw();

        // Disable blending and depth test
        BlendHelper.disableBlend();
        BlendHelper.disableDepthTest();
    }

    private static void drawFriendshipStatus(PoseStack matrices, float x, float y, float width, float height, int friendship) {
        // dynamically calculate friendship ui image name
        String ui_icon_name = "friendship" + friendship;

        // Set shader
        ShaderHelper.setTexturedShader();

        // Set texture
        ResourceLocation button_texture = textures.GetUI(ui_icon_name);
        TextureLoader.bind(0, button_texture);

        // Enable depth test and blending
        BlendHelper.enableBlend();
        BlendHelper.defaultBlendFunc();
        BlendHelper.enableDepthTest();
        BlendHelper.depthMask(true);

        // Get the buffer instance
        QuadBuffer buffer = QuadBuffer.INSTANCE;

        // Get the current matrix position
        Matrix4f matrix4f = matrices.last().pose();

        // Begin drawing quads with the correct vertex format
        buffer.begin();

        float z = -0.01F;
        buffer.vertex(matrix4f, x, y + height, z).color(255, 255, 255, 255).texture(0, 1).light(light).overlay(overlay);  // bottom left
        buffer.vertex(matrix4f, x + width, y + height, z).color(255, 255, 255, 255).texture(1, 1).light(light).overlay(overlay);   // bottom right
        buffer.vertex(matrix4f, x + width, y, z).color(255, 255, 255, 255).texture(1, 0).light(light).overlay(overlay);  // top right
        buffer.vertex(matrix4f, x, y, z).color(255, 255, 255, 255).texture(0, 0).light(light).overlay(overlay); // top left
        buffer.draw();

        // Disable blending and depth test
        BlendHelper.disableBlend();
        BlendHelper.disableDepthTest();
    }

    private static void drawEntityIcon(PoseStack matrices, Entity entity, float x, float y, float width, float height) {
        // Get the vanilla skin identifier…
        @SuppressWarnings("rawtypes")
        EntityRenderer renderer = EntityRendererAccessor.getEntityRenderer(entity);
        ResourceLocation skinId = EntityTextureHelper.getTexture(renderer, entity);
        if (skinId == null) return;

        // Extract its path and map to your icon
        String skinPath = skinId.getPath();  // e.g. "textures/entity/zombie/zombie.png"
        ResourceLocation iconId = textures.GetEntity(skinPath);
        if (iconId == null) return;

        // Set shader & texture
        ShaderHelper.setTexturedShader();
        TextureLoader.bind(0, iconId);

        // Enable depth test and blending
        BlendHelper.enableBlend();
        BlendHelper.defaultBlendFunc();
        BlendHelper.enableDepthTest();
        BlendHelper.depthMask(true);

        // Get the buffer instance
        QuadBuffer buffer = QuadBuffer.INSTANCE;

        // Get the current matrix position
        Matrix4f matrix4f = matrices.last().pose();

        // Begin drawing quads with the correct vertex format
        buffer.begin();

        float z = -0.01F;
        buffer.vertex(matrix4f, x, y + height, z).color(255, 255, 255, 255).texture(0, 1).light(light).overlay(overlay);  // bottom left
        buffer.vertex(matrix4f, x + width, y + height, z).color(255, 255, 255, 255).texture(1, 1).light(light).overlay(overlay);   // bottom right
        buffer.vertex(matrix4f, x + width, y, z).color(255, 255, 255, 255).texture(1, 0).light(light).overlay(overlay);  // top right
        buffer.vertex(matrix4f, x, y, z).color(255, 255, 255, 255).texture(0, 0).light(light).overlay(overlay); // top left
        buffer.draw();

        // Disable blending and depth test
        BlendHelper.disableBlend();
        BlendHelper.disableDepthTest();
    }

    private static void drawPlayerIcon(PoseStack matrices, Entity entity, float x, float y, float width, float height) {
        // Get player skin texture
        @SuppressWarnings("rawtypes")
        EntityRenderer renderer = EntityRendererAccessor.getEntityRenderer(entity);
        ResourceLocation playerTexture = EntityTextureHelper.getTexture(renderer, entity);
        if (playerTexture == null) return;

        // Check for black and white pixels (using the Mixin-based check)
        boolean customSkinFound = PlayerCustomTexture.hasCustomIcon(playerTexture);

        // Set shader & texture
        ShaderHelper.setTexturedShader();
        TextureLoader.bind(0, playerTexture);

        // Enable depth test and blending
        BlendHelper.enableBlend();
        BlendHelper.defaultBlendFunc();
        BlendHelper.enableDepthTest();
        BlendHelper.depthMask(true);

        // Get the buffer instance
        QuadBuffer buffer = QuadBuffer.INSTANCE;
        buffer.begin();

        Matrix4f matrix4f = matrices.last().pose();
        float z = -0.01F;

        if (customSkinFound) {
            // Hidden icon UV coordinates
            float[][] newCoordinates = {
                    {0.0F, 0.0F, 8.0F, 8.0F, 0F, 0F},     // Row 1 left
                    {24.0F, 0.0F, 32.0F, 8.0F, 8F, 0F},   // Row 1 middle
                    {32.0F, 0.0F, 40.0F, 8.0F, 16F, 0F},  // Row 1 right
                    {56.0F, 0.0F, 64.0F, 8.0F, 0F, 8F},   // Row 2 left
                    {56.0F, 20.0F, 64.0F, 28.0F, 8F, 8F}, // Row 2 middle
                    {36.0F, 16.0F, 44.0F, 20.0F, 16F, 8F},// Row 2 right top
                    {56.0F, 16.0F, 64.0F, 20.0F, 16F, 12F},// Row 2 right bottom
                    {56.0F, 28.0F, 64.0F, 36.0F, 0F, 16F}, // Row 3 left
                    {56.0F, 36.0F, 64.0F, 44.0F, 8F, 16F}, // Row 3 middle
                    {56.0F, 44.0F, 64.0F, 48, 16F, 16F},   // Row 3 top right
                    {12.0F, 48.0F, 20.0F, 52, 16F, 20F},   // Row 3 bottom right
            };
            float scaleFactor = 0.77F;

            for (float[] coords : newCoordinates) {
                float newU1 = coords[0] / 64.0F;
                float newV1 = coords[1] / 64.0F;
                float newU2 = coords[2] / 64.0F;
                float newV2 = coords[3] / 64.0F;

                float offsetX = coords[4] * scaleFactor;
                float offsetY = coords[5] * scaleFactor;
                float scaledX = x + offsetX;
                float scaledY = y + offsetY;
                float scaledWidth = (coords[2] - coords[0]) * scaleFactor;
                float scaledHeight = (coords[3] - coords[1]) * scaleFactor;

                buffer.vertex(matrix4f, scaledX, scaledY + scaledHeight, z)
                        .color(255, 255, 255, 255).texture(newU1, newV2).light(light).overlay(overlay);
                buffer.vertex(matrix4f, scaledX + scaledWidth, scaledY + scaledHeight, z)
                        .color(255, 255, 255, 255).texture(newU2, newV2).light(light).overlay(overlay);
                buffer.vertex(matrix4f, scaledX + scaledWidth, scaledY, z)
                        .color(255, 255, 255, 255).texture(newU2, newV1).light(light).overlay(overlay);
                buffer.vertex(matrix4f, scaledX, scaledY, z)
                        .color(255, 255, 255, 255).texture(newU1, newV1).light(light).overlay(overlay);
            }
        } else {
            // make skin appear smaller and centered
            x += 2;
            y += 2;
            width -= 4;
            height -= 4;

            // Normal face coordinates
            float u1 = 8.0F / 64.0F;
            float v1 = 8.0F / 64.0F;
            float u2 = 16.0F / 64.0F;
            float v2 = 16.0F / 64.0F;

            buffer.vertex(matrix4f, x, y + height, z)
                    .color(255, 255, 255, 255).texture(u1, v2).light(light).overlay(overlay);
            buffer.vertex(matrix4f, x + width, y + height, z)
                    .color(255, 255, 255, 255).texture(u2, v2).light(light).overlay(overlay);
            buffer.vertex(matrix4f, x + width, y, z)
                    .color(255, 255, 255, 255).texture(u2, v1).light(light).overlay(overlay);
            buffer.vertex(matrix4f, x, y, z)
                    .color(255, 255, 255, 255).texture(u1, v1).light(light).overlay(overlay);

            // Hat layer
            float hatU1 = 40.0F / 64.0F;
            float hatV1 = 8.0F / 64.0F;
            float hatU2 = 48.0F / 64.0F;
            float hatV2 = 16.0F / 64.0F;

            z -= 0.01F;

            buffer.vertex(matrix4f, x, y + height, z)
                    .color(255, 255, 255, 255).texture(hatU1, hatV2).light(light).overlay(overlay);
            buffer.vertex(matrix4f, x + width, y + height, z)
                    .color(255, 255, 255, 255).texture(hatU2, hatV2).light(light).overlay(overlay);
            buffer.vertex(matrix4f, x + width, y, z)
                    .color(255, 255, 255, 255).texture(hatU2, hatV1).light(light).overlay(overlay);
            buffer.vertex(matrix4f, x, y, z)
                    .color(255, 255, 255, 255).texture(hatU1, hatV1).light(light).overlay(overlay);
        }

        buffer.draw();

        // Disable blending and depth test
        BlendHelper.disableBlend();
        BlendHelper.disableDepthTest();
    }

    private static void drawMessageText(Matrix4f matrix, List<String> lines, int starting_line, int ending_line,
                                 MultiBufferSource immediate, float lineSpacing, int fullBright, float yOffset) {
        Font fontRenderer = Minecraft.getInstance().font;
        Matrix4f textMatrix = new Matrix4f(matrix).translate(0.0F, 0.0F, TEXT_Z_OFFSET);
        int currentLineIndex = 0; // We'll use this to track which line we're on

        for (String lineText : lines) {
            // Only draw lines that are within the specified range
            if (currentLineIndex >= starting_line && currentLineIndex < ending_line) {
                fontRenderer.drawInBatch(lineText, -fontRenderer.width(lineText) / 2f, yOffset, 0xffffffff,
                        false, textMatrix, immediate, DisplayMode.NORMAL, 0, fullBright);
                yOffset += fontRenderer.lineHeight + lineSpacing;
            }
            currentLineIndex++;

            if (currentLineIndex > ending_line) {
                break;
            }
        }
    }

    private static void drawEntityName(Entity entity, Matrix4f matrix, MultiBufferSource immediate,
                                int fullBright, float yOffset, boolean truncate) {
        Font fontRenderer = Minecraft.getInstance().font;

        // Get Name of entity
        String nameText = "";
        if (entity instanceof Mob) {
            // Custom Name Tag (MobEntity)
            if (entity.getCustomName() != null) {
                nameText = entity.getCustomName().getString();
            }
        } else if (entity instanceof Player) {
            // Player Name
            nameText = entity.getName().getString();
        }

        // Truncate long names
        if (nameText.length() > 14 && truncate) {
            nameText = nameText.substring(0, 14) + "...";
        }

        Matrix4f textMatrix = new Matrix4f(matrix).translate(0.0F, 0.0F, TEXT_Z_OFFSET);
        fontRenderer.drawInBatch(nameText, -fontRenderer.width(nameText) / 2f, yOffset, 0xffffffff,
                false, textMatrix, immediate, DisplayMode.NORMAL, 0, fullBright);
    }

    public static void drawTextAboveEntities(WorldRenderContext context, long tick, float partialTicks) {
        // Set some rendering constants
        float lineSpacing = 1F;
        float textHeaderHeight = 40F;
        float textFooterHeight = 5F;
        int fullBright = 0xF000F0;
        double renderDistance = 11.0;

        // Get camera
        Camera camera = context.camera();
        Entity cameraEntity = camera.getEntity();
        if (cameraEntity == null) return;
        Level world = cameraEntity.level();

        // Calculate radius of entities
        Vec3 pos = cameraEntity.position();
        AABB area = new AABB(pos.x - renderDistance, pos.y - renderDistance, pos.z - renderDistance,
                pos.x + renderDistance, pos.y + renderDistance, pos.z + renderDistance);

        // Init font render, matrix, and vertex producer
        Font fontRenderer = Minecraft.getInstance().font;
        PoseStack matrices = context.matrixStack();
        MultiBufferSource immediate = context.consumers();

        // Get camera position
        Vec3 interpolatedCameraPos = new Vec3(camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);

        // Increment query counter
        queryEntityDataCount++;

        // This query count helps us cache the list of relevant entities. We can refresh
        // the list every 3rd call to this render function
        if (queryEntityDataCount % 3 == 0 || relevantEntities == null) {
            // Get all entities
            List<Entity> nearbyEntities = world.getEntities(null, area);

            // Filter to include only MobEntity & PlayerEntity but exclude any camera 1st person entity and any entities with passengers
            relevantEntities = nearbyEntities.stream()
                    .filter(entity -> (entity instanceof Mob || entity instanceof Player))
                    .filter(entity -> !entity.isVehicle())
                    .filter(entity -> !(entity.equals(cameraEntity) && !camera.isDetached()))
                    .filter(entity -> !(entity.equals(cameraEntity) && entity.isSpectator()))
                    .filter(entity -> {
                        // Always include PlayerEntity
                        if (entity instanceof Player) {
                            return true;
                        }
                        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                        String entityIdString = entityId.toString();
                        // Check blacklist first
                        if (blacklist.contains(entityIdString)) {
                            return false;
                        }
                        // If whitelist is not empty, only include entities in the whitelist
                        return whitelist.isEmpty() || whitelist.contains(entityIdString);
                    })
                    .collect(Collectors.toList());

            queryEntityDataCount = 0;
        }

        for (Entity entity : relevantEntities) {

            // Push a new matrix onto the stack.
            matrices.pushPose();

            // Get entity height (adjust for specific classes)
            float entityHeight = EntityHeights.getAdjustedEntityHeight(entity);

            // Interpolate entity position (smooth motion)
            double paddingAboveEntity = 0.4D;
            Vec3 interpolatedEntityPos = EntityRenderPosition.getInterpolatedPosition(entity, partialTicks);

            // Determine the chat bubble position
            Vec3 bubblePosition;
            if (entity instanceof EnderDragon) {
                // Ender dragons a unique, and we must use the Head for position
                EnderDragon dragon = (EnderDragon) entity;
                EnderDragonPart head = dragon.head;

                // Interpolate the head position
                Vec3 headPos = EntityRenderPosition.getInterpolatedPosition(head, partialTicks);

                // Just use the head's interpolated position directly
                bubblePosition = headPos.add(0, entityHeight + paddingAboveEntity, 0);
            } else {
                // Calculate the forward offset based on the entity's yaw
                float entityYawRadians = (float) Math.toRadians(entity.getViewYRot(partialTicks));
                Vec3 forwardOffset = new Vec3(-Math.sin(entityYawRadians), 0.0, Math.cos(entityYawRadians));

                // Calculate the forward offset based on the entity's yaw, scaled to 80% towards the front edge
                Vec3 scaledForwardOffset = forwardOffset.scale(entity.getBbWidth() / 2.0 * 0.8);

                // Calculate the position of the chat bubble: above the head and 80% towards the front
                bubblePosition = interpolatedEntityPos.add(scaledForwardOffset)
                        .add(0, entityHeight + paddingAboveEntity, 0);
            }

            // Translate to the chat bubble's position
            matrices.translate(bubblePosition.x - interpolatedCameraPos.x,
                    bubblePosition.y - interpolatedCameraPos.y,
                    bubblePosition.z - interpolatedCameraPos.z);

            // Calculate the difference vector (from entity + padding above to camera)
            Vec3 difference = interpolatedCameraPos.subtract(new Vec3(interpolatedEntityPos.x, interpolatedEntityPos.y + entityHeight + paddingAboveEntity, interpolatedEntityPos.z));

            // Calculate the yaw angle
            double yaw = -(Math.atan2(difference.z, difference.x) + Math.PI / 2D);

            // Convert yaw to Quaternion
            float halfYaw = (float) yaw * 0.5f;
            double sinHalfYaw = Mth.sin(halfYaw);
            double cosHalfYaw = Mth.cos(halfYaw);
            Quaternionf yawRotation = new Quaternionf(0, sinHalfYaw, 0, cosHalfYaw);

            // Apply the yaw rotation to the matrix stack
            matrices.mulPose(yawRotation);

            // Obtain the horizontal distance to the entity
            double horizontalDistance = Math.sqrt(difference.x * difference.x + difference.z * difference.z);
            // Calculate the pitch angle based on the horizontal distance and the y difference
            double pitch = Math.atan2(difference.y, horizontalDistance);

            // Convert pitch to Quaternion
            float halfPitch = (float) pitch * 0.5f;
            double sinHalfPitch = Mth.sin(halfPitch);
            double cosHalfPitch = Mth.cos(halfPitch);
            Quaternionf pitchRotation = new Quaternionf(sinHalfPitch, 0, 0, cosHalfPitch);

            // Apply the pitch rotation to the matrix stack
            matrices.mulPose(pitchRotation);

            // Get position matrix
            Matrix4f matrix = matrices.last().pose();

            // Get the player
            LocalPlayer player = Minecraft.getInstance().player;

            // Get chat message (if any)
            EntityChatData chatData = null;
            PlayerData playerData = null;
            if (entity instanceof Mob) {
                chatData = ChatDataManager.getClientInstance().getOrCreateChatData(entity.getStringUUID());
                if (chatData != null) {
                    playerData = chatData.getPlayerData(player.getDisplayName().getString());
                }
            } else if (entity instanceof Player) {
                chatData = PlayerMessageManager.getMessage(entity.getUUID());
                playerData = new PlayerData(); // no friendship needed for player messages
            }

            float minTextHeight = (ChatDataManager.DISPLAY_NUM_LINES * (fontRenderer.lineHeight + lineSpacing)) + (DISPLAY_PADDING * 2);
            float scaledTextHeight = 0;

            if (chatData != null) {
                // Set the range of lines to display
                List<String> lines = chatData.getWrappedLines();
                float linesDisplayed = 0;
                int starting_line = chatData.currentLineNumber;
                int ending_line = Math.min(chatData.currentLineNumber + ChatDataManager.DISPLAY_NUM_LINES, lines.size());

                // Determine max line length
                linesDisplayed = ending_line - starting_line;

                // Calculate size of text scaled to world
                scaledTextHeight = linesDisplayed * (fontRenderer.lineHeight + lineSpacing);
                scaledTextHeight = Math.max(scaledTextHeight, minTextHeight);

                // Update Bubble Data for Click Handling using UUID (account for scaling)
                BubbleLocationManager.updateBubbleData(entity.getUUID(), bubblePosition,
                        128F / (1 / 0.02F), (scaledTextHeight + 25F) / (1 / 0.02F), yaw, pitch);

                // Scale down before rendering textures (otherwise font is huge)
                matrices.scale(-0.02F, -0.02F, 0.02F);

                // Translate above the entity
                matrices.translate(0F, -scaledTextHeight - textHeaderHeight - textFooterHeight, 0F);

                // Check if conversation has started
                if (chatData.status == ChatDataManager.ChatStatus.NONE) {
                    // Draw 'start chat' button
                    drawIcon("button-chat", matrices, -16, textHeaderHeight, 32, 17);

                    // Draw Entity (Custom Name)
                    drawEntityName(entity, matrix, immediate, fullBright, 24F + DISPLAY_PADDING, true);

                } else if (chatData.status == ChatDataManager.ChatStatus.PENDING) {
                    // Draw 'pending' button
                    drawIcon("button-dot-" + animationFrame, matrices, -16, textHeaderHeight, 32, 17);

                } else if (chatData.sender == ChatDataManager.ChatSender.ASSISTANT && chatData.status != ChatDataManager.ChatStatus.HIDDEN) {
                    // Draw Entity (Custom Name)
                    drawEntityName(entity, matrix, immediate, fullBright, 24F + DISPLAY_PADDING, true);

                    // Draw text background (no smaller than 50F tall)
                    drawTextBubbleBackground("text-top", matrices, -64, 0, 128, scaledTextHeight, playerData.friendship);

                    // Draw face icon of entity
                    drawEntityIcon(matrices, entity, -82, 7, 32, 32);

                    // Draw Friendship status
                    drawFriendshipStatus(matrices, 51, 18, 31, 21, playerData.friendship);

                    // Draw 'arrows' & 'keyboard' buttons
                    if (chatData.currentLineNumber > 0) {
                        drawIcon("arrow-left", matrices, -63, scaledTextHeight + 29, 16, 16);
                    }
                    if (!chatData.isEndOfMessage()) {
                        drawIcon("arrow-right", matrices, 47, scaledTextHeight + 29, 16, 16);
                    } else {
                        drawIcon("keyboard", matrices, 47, scaledTextHeight + 28, 16, 16);
                    }

                    // Render each line of the text
                    drawMessageText(matrix, lines, starting_line, ending_line, immediate, lineSpacing, fullBright, 40.0F + DISPLAY_PADDING);

                } else if (chatData.sender == ChatDataManager.ChatSender.ASSISTANT && chatData.status == ChatDataManager.ChatStatus.HIDDEN) {
                    // Draw Entity (Custom Name)
                    drawEntityName(entity, matrix, immediate, fullBright, 24F + DISPLAY_PADDING, false);

                    // Draw 'resume chat' button
                    if (playerData.friendship == 3) {
                        // Friend chat bubble
                        drawIcon("button-chat-friend", matrices, -16, textHeaderHeight, 32, 17);
                    } else if (playerData.friendship == -3) {
                        // Enemy chat bubble
                        drawIcon("button-chat-enemy", matrices, -16, textHeaderHeight, 32, 17);
                    } else {
                        // Normal chat bubble
                        drawIcon("button-chat", matrices, -16, textHeaderHeight, 32, 17);
                    }

                } else if (chatData.sender == ChatDataManager.ChatSender.USER && chatData.status == ChatDataManager.ChatStatus.DISPLAY) {
                    // Draw Player Name
                    drawEntityName(entity, matrix, immediate, fullBright, 24F + DISPLAY_PADDING, true);

                    // Draw text background
                    drawTextBubbleBackground("text-top-player", matrices, -64, 0, 128, scaledTextHeight, playerData.friendship);

                    // Draw face icon of player
                    drawPlayerIcon(matrices, entity, -75, 14, 18, 18);

                    // Render each line of the player's text
                    drawMessageText(matrix, lines, starting_line, ending_line, immediate, lineSpacing, fullBright, 40.0F + DISPLAY_PADDING);
                }

            } else if (entity instanceof Player) {
                // Scale down before rendering textures (otherwise font is huge)
                matrices.scale(-0.02F, -0.02F, 0.02F);

                boolean showPendingIcon = false;
                if (PlayerMessageManager.isChatUIOpen(entity.getUUID())) {
                    showPendingIcon = true;
                    scaledTextHeight += minTextHeight; // raise height of player name and icon
                } else {
                    scaledTextHeight -= 15; // lower a bit more (when no pending icon is visible)
                }

                // Translate above the player
                matrices.translate(0F, -scaledTextHeight - textHeaderHeight - textFooterHeight, 0F);

                // Draw Player Name (if not self and HUD is visible)
                if (!entity.equals(cameraEntity) && !Minecraft.getInstance().options.hideGui) {
                    drawEntityName(entity, matrices.last().pose(), immediate, fullBright, 24F + DISPLAY_PADDING, true);

                    if (showPendingIcon) {
                        // Draw 'pending' button (when Chat UI is open)
                        drawIcon("button-dot-" + animationFrame, matrices, -16, textHeaderHeight, 32, 17);
                    }
                }
            }

            // Calculate animation frames (0-8) every X ticks
            if (lastTick != tick && tick % 5 == 0) {
                lastTick = tick;
                animationFrame++;
            }
            if (animationFrame > 8) {
                animationFrame = 0;
            }

            // Pop the matrix to return to the original state.
            matrices.popPose();
        }

        // Get list of Entity UUIDs with chat bubbles rendered
        List<UUID> activeEntityUUIDs = relevantEntities.stream()
                .map(Entity::getUUID)
                .collect(Collectors.toList());

        // Purge entities that were not rendered
        BubbleLocationManager.performCleanup(activeEntityUUIDs);
    }
}
