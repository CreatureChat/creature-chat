// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.ui;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.network.ClientPackets;
import com.owlmaddie.utils.ClientEntityFinder;
import com.owlmaddie.utils.UseItemCallbackHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The {@code ClickHandler} class is used for the client to interact with the Entity chat UI. This class helps
 * to receive messages from the server, cast rays to see what the user clicked on, and communicate these events
 * back to the server.
 */
public class ClickHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturepals");
    private static boolean wasClicked = false;

    public static void register() {
        UseItemCallback.EVENT.register(UseItemCallbackHelper::handleUseItemAction);

        // Handle empty hand right-click
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.options.keyUse.isDown()) {
                if (!wasClicked && client.player != null && client.player.getMainHandItem().isEmpty()) {
                    if (handleUseKeyClick(client)) {
                        wasClicked = true;
                    }
                }
            } else {
                wasClicked = false;
            }
        });
    }

    public static boolean shouldCancelAction(Level world) {
        if (world.isClientSide) {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.options.keyUse.isDown()) {
                return handleUseKeyClick(client);
            }
        }
        return false;
    }

    public static boolean handleUseKeyClick(Minecraft client) {
        Camera camera = client.gameRenderer.getMainCamera();
        Entity cameraEntity = camera.getEntity();
        if (cameraEntity == null) return false;

        // Get the player from the client
        LocalPlayer player = client.player;

        // Get the camera position for ray start to support both first-person and third-person views
        Vec3 startRay = camera.getPosition();

        // Use the player's looking direction to define the ray's direction
        Vec3 lookVec = player.getViewVector(1.0F);

        // Track the closest object details
        double closestDistance = Double.MAX_VALUE;
        Optional<Vec3> closestHitResult = null;
        UUID closestEntityUUID = null;
        BubbleLocationManager.BubbleData closestBubbleData = null;

        // Iterate over cached rendered chat bubble data in BubbleLocationManager
        for (Map.Entry<UUID, BubbleLocationManager.BubbleData> entry : BubbleLocationManager.getAllBubbleData().entrySet()) {
            UUID entityUUID = entry.getKey();
            BubbleLocationManager.BubbleData bubbleData = entry.getValue();

            // Define a bounding box that accurately represents the text bubble
            Vec3[] corners = getBillboardCorners(bubbleData.position, camera.getPosition(), bubbleData.height, bubbleData.width, bubbleData.yaw, bubbleData.pitch);

            // Cast ray and determine intersection with chat bubble
            Optional<Vec3> hitResult = rayIntersectsPolygon(startRay, lookVec, corners);
            if (hitResult.isPresent()) {
                double distance = startRay.distanceToSqr(hitResult.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntityUUID = entityUUID;
                    closestHitResult = hitResult;
                    closestBubbleData = bubbleData;
                }
            }
        }

        // Handle the click for the closest entity after the loop
        if (closestEntityUUID != null) {
            Mob closestEntity = ClientEntityFinder.getEntityByUUID(client.level, closestEntityUUID);
            if (closestEntity != null) {
                // Look-up conversation
                EntityChatData chatData = ChatDataManager.getClientInstance().getOrCreateChatData(closestEntityUUID.toString());

                // Determine area clicked inside chat bubble (top, left, right)
                String hitRegion = determineHitRegion(closestHitResult.get(), closestBubbleData.position, camera, closestBubbleData.height);
                LOGGER.debug("Clicked region: " + hitRegion);

                if (chatData.status == ChatDataManager.ChatStatus.NONE) {
                    // Start conversation
                    ClientPackets.sendGenerateGreeting(closestEntity);

                } else if (chatData.status == ChatDataManager.ChatStatus.DISPLAY) {
                    if (hitRegion.equals("RIGHT") && !chatData.isEndOfMessage()) {
                        // Update lines read > next lines
                        ClientPackets.sendUpdateLineNumber(closestEntity, chatData.currentLineNumber + ChatDataManager.DISPLAY_NUM_LINES);
                    } else if (hitRegion.equals("LEFT") && chatData.currentLineNumber > 0) {
                        // Update lines read < previous lines
                        ClientPackets.sendUpdateLineNumber(closestEntity, chatData.currentLineNumber - ChatDataManager.DISPLAY_NUM_LINES);
                    } else if (hitRegion.equals("RIGHT") && chatData.isEndOfMessage()) {
                        // End of chat (open player chat screen)
                        client.setScreen(new ChatScreen(closestEntity, client.player));
                    } else if (hitRegion.equals("TOP")) {
                        // Hide chat
                        ClientPackets.setChatStatus(closestEntity, ChatDataManager.ChatStatus.HIDDEN);
                    }
                } else if (chatData.status == ChatDataManager.ChatStatus.HIDDEN) {
                    // Show chat
                    ClientPackets.setChatStatus(closestEntity, ChatDataManager.ChatStatus.DISPLAY);
                }
                return true;
            }
        }
        return false;
    }

    public static Vec3[] getBillboardCorners(Vec3 center, Vec3 cameraPos, double height, double width, double yaw, double pitch) {
        // Convert yaw and pitch to radians for rotation calculations
        double radYaw = Math.toRadians(yaw);
        double radPitch = Math.toRadians(pitch);

        // Calculate the vector pointing from the center to the camera
        Vec3 toCamera = cameraPos.subtract(center).normalize();

        // Calculate initial 'right' and 'up' vectors assuming 'up' is the global Y-axis (0, 1, 0)
        Vec3 globalUp = new Vec3(0, 1, 0);
        Vec3 right = globalUp.cross(toCamera).normalize();
        Vec3 up = toCamera.cross(right).normalize();

        // Rotate 'right' and 'up' vectors based on yaw and pitch
        right = rotateVector(right, radYaw, radPitch);
        up = rotateVector(up, radYaw, radPitch);

        // Adjust the center point to move it to the bottom center of the rectangle
        Vec3 adjustedCenter = center.add(up.scale(height / 2));  // Move the center upwards by half the height

        // Calculate the corners using the adjusted center, right, and up vectors
        Vec3 topLeft = adjustedCenter.subtract(right.scale(width / 2)).add(up.scale(height / 2));
        Vec3 topRight = adjustedCenter.add(right.scale(width / 2)).add(up.scale(height / 2));
        Vec3 bottomRight = adjustedCenter.add(right.scale(width / 2)).subtract(up.scale(height / 2));
        Vec3 bottomLeft = adjustedCenter.subtract(right.scale(width / 2)).subtract(up.scale(height / 2));

        // Return an array of Vec3d representing each corner of the billboard
        return new Vec3[] {topLeft, topRight, bottomRight, bottomLeft};
    }

    private static Vec3 rotateVector(Vec3 vector, double yaw, double pitch) {
        // Rotation around Y-axis (yaw)
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        Vec3 yawRotated = new Vec3(
                vector.x * cosYaw + vector.z * sinYaw,
                vector.y,
                -vector.x * sinYaw + vector.z * cosYaw
        );

        // Rotation around X-axis (pitch)
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        return new Vec3(
                yawRotated.x,
                yawRotated.y * cosPitch - yawRotated.z * sinPitch,
                yawRotated.y * sinPitch + yawRotated.z * cosPitch
        );
    }

    public static Optional<Vec3> rayIntersectsPolygon(Vec3 rayOrigin, Vec3 rayDirection, Vec3[] vertices) {
        rayDirection = rayDirection.normalize();  // Ensure direction is normalized
        // Check two triangles formed by the quad
        return Stream.of(
                        rayIntersectsTriangle(rayOrigin, rayDirection, vertices[0], vertices[1], vertices[2]),
                        rayIntersectsTriangle(rayOrigin, rayDirection, vertices[0], vertices[2], vertices[3])
                ).filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.empty());
    }

    public static Optional<Vec3> rayIntersectsTriangle(Vec3 rayOrigin, Vec3 rayDirection, Vec3 v0, Vec3 v1, Vec3 v2) {
        Vec3 edge1 = v1.subtract(v0);
        Vec3 edge2 = v2.subtract(v0);
        Vec3 h = rayDirection.cross(edge2);
        double a = edge1.dot(h);

        if (Math.abs(a) < 1e-6) return Optional.empty();  // Ray is parallel to the triangle

        double f = 1.0 / a;
        Vec3 s = rayOrigin.subtract(v0);
        double u = f * s.dot(h);
        if (u < 0.0 || u > 1.0) return Optional.empty();

        Vec3 q = s.cross(edge1);
        double v = f * rayDirection.dot(q);
        if (v < 0.0 || u + v > 1.0) return Optional.empty();

        double t = f * edge2.dot(q);
        if (t > 1e-6) {
            return Optional.of(rayOrigin.add(rayDirection.scale(t)));
        }
        return Optional.empty();
    }

    public static String determineHitRegion(Vec3 hitPoint, Vec3 center, Camera camera, double height) {
        Vec3 cameraPos = camera.getPosition();
        Vec3 toCamera = cameraPos.subtract(center).normalize();

        // Assuming a standard global up vector (aligned with the y-axis)
        Vec3 globalUp = new Vec3(0, 1, 0);

        // Calculate the "RIGHT" vector as perpendicular to the 'toCamera' vector and the global up vector
        Vec3 right = globalUp.cross(toCamera).normalize();

        // Handle the case where the camera is looking straight down or up, making the cross product degenerate
        if (right.lengthSqr() == 0) {
            // If directly above or below, define an arbitrary right vector (assuming world x-axis)
            right = new Vec3(1, 0, 0);
        }

        // Recalculate "UP" vector to ensure it's orthogonal to both "RIGHT" and "toCamera"
        Vec3 up = toCamera.cross(right).normalize();

        // Calculate the relative position of the hit point to the center of the billboard
        Vec3 relPosition = hitPoint.subtract(center);
        double relX = relPosition.dot(right);  // Project onto "RIGHT"
        double relY = relPosition.dot(up);     // Project onto "UP"

        // Determine hit region based on relative coordinates
        if (relY > 0.70 * height) {
            return "TOP";
        } else {
            // Determine left or right (0 is center)
            // Offset this to give the left a smaller target (going backwards is less common)
            return relX < -0.5 ? "LEFT" : "RIGHT";
        }
    }
}