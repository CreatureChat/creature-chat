package com.owlmaddie.ui;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.network.ClientPackets;
import com.owlmaddie.utils.ClientEntityFinder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static boolean wasClicked = false;

    public static void register() {
        UseItemCallback.EVENT.register(ClickHandler::handleUseItemAction);

        // Handle empty hand right-click
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.options.useKey.isPressed()) {
                if (!wasClicked && client.player != null && client.player.getMainHandStack().isEmpty()) {
                    if (handleUseKeyClick(client)) {
                        wasClicked = true;
                    }
                }
            } else {
                wasClicked = false;
            }
        });
    }

    // Handle use-item right-click (non-empty hand)
    private static TypedActionResult<ItemStack> handleUseItemAction(PlayerEntity player, World world, Hand hand) {
        if (shouldCancelAction(world)) {
            return TypedActionResult.fail(player.getStackInHand(hand));
        }
        return TypedActionResult.pass(player.getStackInHand(hand));
    }

    private static boolean shouldCancelAction(World world) {
        if (world.isClient) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.options.useKey.isPressed()) {
                return handleUseKeyClick(client);
            }
        }
        return false;
    }

    public static boolean handleUseKeyClick(MinecraftClient client) {
        Camera camera = client.gameRenderer.getCamera();
        Entity cameraEntity = camera.getFocusedEntity();
        if (cameraEntity == null) return false;

        // Get the player from the client
        ClientPlayerEntity player = client.player;

        // Get the camera position for ray start to support both first-person and third-person views
        Vec3d startRay = camera.getPos();

        // Use the player's looking direction to define the ray's direction
        Vec3d lookVec = player.getRotationVec(1.0F);

        // Track the closest object details
        double closestDistance = Double.MAX_VALUE;
        Optional<Vec3d> closestHitResult = null;
        UUID closestEntityUUID = null;
        BubbleLocationManager.BubbleData closestBubbleData = null;

        // Iterate over cached rendered chat bubble data in BubbleLocationManager
        for (Map.Entry<UUID, BubbleLocationManager.BubbleData> entry : BubbleLocationManager.getAllBubbleData().entrySet()) {
            UUID entityUUID = entry.getKey();
            BubbleLocationManager.BubbleData bubbleData = entry.getValue();

            // Define a bounding box that accurately represents the text bubble
            Vec3d[] corners = getBillboardCorners(bubbleData.position, camera.getPos(), bubbleData.height, bubbleData.width, bubbleData.yaw, bubbleData.pitch);

            // DEBUG CODE
            //drawCorners(player.getWorld(), corners);
            //drawRay(startRay, lookVec, player.getWorld());

            // Cast ray and determine intersection with chat bubble
            Optional<Vec3d> hitResult = rayIntersectsPolygon(startRay, lookVec, corners);
            if (hitResult.isPresent()) {
                double distance = startRay.squaredDistanceTo(hitResult.get());
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
            MobEntity closestEntity = ClientEntityFinder.getEntityByUUID(client.world, closestEntityUUID);
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
                        //client.setScreen(new ChatScreen(closestEntity, client.player));
                        TagEditorScreenHandler handler = new TagEditorScreenHandler(0, player.getInventory());
                        client.setScreen(new CreatureTagEditorScreen(handler, player));
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

    public static Vec3d[] getBillboardCorners(Vec3d center, Vec3d cameraPos, double height, double width, double yaw, double pitch) {
        // Convert yaw and pitch to radians for rotation calculations
        double radYaw = Math.toRadians(yaw);
        double radPitch = Math.toRadians(pitch);

        // Calculate the vector pointing from the center to the camera
        Vec3d toCamera = cameraPos.subtract(center).normalize();

        // Calculate initial 'right' and 'up' vectors assuming 'up' is the global Y-axis (0, 1, 0)
        Vec3d globalUp = new Vec3d(0, 1, 0);
        Vec3d right = globalUp.crossProduct(toCamera).normalize();
        Vec3d up = toCamera.crossProduct(right).normalize();

        // Rotate 'right' and 'up' vectors based on yaw and pitch
        right = rotateVector(right, radYaw, radPitch);
        up = rotateVector(up, radYaw, radPitch);

        // Adjust the center point to move it to the bottom center of the rectangle
        Vec3d adjustedCenter = center.add(up.multiply(height / 2));  // Move the center upwards by half the height

        // Calculate the corners using the adjusted center, right, and up vectors
        Vec3d topLeft = adjustedCenter.subtract(right.multiply(width / 2)).add(up.multiply(height / 2));
        Vec3d topRight = adjustedCenter.add(right.multiply(width / 2)).add(up.multiply(height / 2));
        Vec3d bottomRight = adjustedCenter.add(right.multiply(width / 2)).subtract(up.multiply(height / 2));
        Vec3d bottomLeft = adjustedCenter.subtract(right.multiply(width / 2)).subtract(up.multiply(height / 2));

        // Return an array of Vec3d representing each corner of the billboard
        return new Vec3d[] {topLeft, topRight, bottomRight, bottomLeft};
    }

    private static Vec3d rotateVector(Vec3d vector, double yaw, double pitch) {
        // Rotation around Y-axis (yaw)
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        Vec3d yawRotated = new Vec3d(
                vector.x * cosYaw + vector.z * sinYaw,
                vector.y,
                -vector.x * sinYaw + vector.z * cosYaw
        );

        // Rotation around X-axis (pitch)
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        return new Vec3d(
                yawRotated.x,
                yawRotated.y * cosPitch - yawRotated.z * sinPitch,
                yawRotated.y * sinPitch + yawRotated.z * cosPitch
        );
    }

    public static void drawCorners(World world, Vec3d[] corners) {
        // Iterate over the corners to place glow particles
        for (Vec3d corner : corners) {
            world.addParticle(
                    ParticleTypes.GLOW,  // Using glow particles
                    corner.x, corner.y, corner.z,  // Coordinates of the particle
                    0.0, 0.0, 0.0  // No motion
            );
        }
    }

    public static void drawRay(Vec3d origin, Vec3d direction, World world) {
        Vec3d point = origin;
        double step = 0.5;
        int count = 100;  // Draw the ray for 100 steps
        for (int i = 0; i < count; i++) {
            point = point.add(direction.multiply(step));
            world.addParticle(ParticleTypes.END_ROD, point.x, point.y, point.z, 0, 0, 0);
        }
    }

    public static Optional<Vec3d> rayIntersectsPolygon(Vec3d rayOrigin, Vec3d rayDirection, Vec3d[] vertices) {
        rayDirection = rayDirection.normalize();  // Ensure direction is normalized
        // Check two triangles formed by the quad
        return Stream.of(
                        rayIntersectsTriangle(rayOrigin, rayDirection, vertices[0], vertices[1], vertices[2]),
                        rayIntersectsTriangle(rayOrigin, rayDirection, vertices[0], vertices[2], vertices[3])
                ).filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.empty());
    }

    public static Optional<Vec3d> rayIntersectsTriangle(Vec3d rayOrigin, Vec3d rayDirection, Vec3d v0, Vec3d v1, Vec3d v2) {
        Vec3d edge1 = v1.subtract(v0);
        Vec3d edge2 = v2.subtract(v0);
        Vec3d h = rayDirection.crossProduct(edge2);
        double a = edge1.dotProduct(h);

        if (Math.abs(a) < 1e-6) return Optional.empty();  // Ray is parallel to the triangle

        double f = 1.0 / a;
        Vec3d s = rayOrigin.subtract(v0);
        double u = f * s.dotProduct(h);
        if (u < 0.0 || u > 1.0) return Optional.empty();

        Vec3d q = s.crossProduct(edge1);
        double v = f * rayDirection.dotProduct(q);
        if (v < 0.0 || u + v > 1.0) return Optional.empty();

        double t = f * edge2.dotProduct(q);
        if (t > 1e-6) {
            return Optional.of(rayOrigin.add(rayDirection.multiply(t)));
        }
        return Optional.empty();
    }

    public static String determineHitRegion(Vec3d hitPoint, Vec3d center, Camera camera, double height) {
        Vec3d cameraPos = camera.getPos();
        Vec3d toCamera = cameraPos.subtract(center).normalize();

        // Assuming a standard global up vector (aligned with the y-axis)
        Vec3d globalUp = new Vec3d(0, 1, 0);

        // Calculate the "RIGHT" vector as perpendicular to the 'toCamera' vector and the global up vector
        Vec3d right = globalUp.crossProduct(toCamera).normalize();

        // Handle the case where the camera is looking straight down or up, making the cross product degenerate
        if (right.lengthSquared() == 0) {
            // If directly above or below, define an arbitrary right vector (assuming world x-axis)
            right = new Vec3d(1, 0, 0);
        }

        // Recalculate "UP" vector to ensure it's orthogonal to both "RIGHT" and "toCamera"
        Vec3d up = toCamera.crossProduct(right).normalize();

        // Calculate the relative position of the hit point to the center of the billboard
        Vec3d relPosition = hitPoint.subtract(center);
        double relX = relPosition.dotProduct(right);  // Project onto "RIGHT"
        double relY = relPosition.dotProduct(up);     // Project onto "UP"

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