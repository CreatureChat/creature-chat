// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.ui;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.ChatMessage;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.render.EntityTextureHelper;
import com.owlmaddie.render.PoseHelper;
import com.owlmaddie.render.RenderPipelineHelper;
import com.owlmaddie.utils.ClientEntityFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Screen that displays a two-page log of recently chatted entities.
 */
public class BookScreen extends ScreenHelper {
    private static final int BOOK_WIDTH = 300;
    private static final int BOOK_HEIGHT = 200;

    private int index;
    private final List<EntityChatData> ordered;
    private Button prevButton;
    private Button nextButton;
    private EditBox dummyField;
    private static final int PAGE_CONTENT_W = 120;   // width of text block per page
    private static final int PAGE_CONTENT_H = 120;   // height of text block (for scissor)
    private static final int LABEL_COLOR    = 0xFF6B4A3B; // warm brown, matches book UI
    private static final int BODY_COLOR     = 0xFF2A2A2A; // dark text
    private static final int LIGHT_GRAY     = 0xFFB0B0B0;
    private static final Random RNG         = new Random();

    public BookScreen() {
        super(Component.literal("Creature Log"));
        ChatDataManager mgr = ChatDataManager.getClientInstance();
        ordered = new ArrayList<>(mgr.entityChatDataMap.values());
        ordered.sort(Comparator.comparingLong(BookScreen::getLastInteraction).reversed());
    }

    private static long getLastInteraction(EntityChatData data) {
        List<ChatMessage> msgs = data.previousMessages;
        if (msgs != null && !msgs.isEmpty()) {
            Long t = msgs.get(msgs.size() - 1).timestamp;
            return t == null ? 0L : t;
        }
        return 0L;
    }

    @Override
    protected void init() {
        super.init();

        BG_WIDTH = BOOK_WIDTH;
        BG_HEIGHT = BOOK_HEIGHT;
        TITLE_OFFSET = 0;

        bgX = (this.width - BG_WIDTH) / 2;
        bgY = (this.height - BG_HEIGHT) / 2;

        dummyField = new EditBox(font, bgX, bgY, 0, 0, Component.empty());

        prevButton = ButtonHelper.createImageButton(
                bgX + 29, bgY + 155,
                14, 14,
                textures.GetUI("arrow-left"),
                textures.GetUI("arrow-left"),
                b -> {
                    index = Math.max(0, index - 2);
                    updateButtons();
                },
                button -> Component.empty()
        );

        nextButton = ButtonHelper.createImageButton(
                bgX + 257, bgY + 155,
                14, 14,
                textures.GetUI("arrow-right"),
                textures.GetUI("arrow-right"),
                b -> {
                    index = Math.min(ordered.size(), index + 2);
                    updateButtons();
                },
                button -> Component.empty()
        );

        addRenderableWidget(prevButton);
        addRenderableWidget(nextButton);
        updateButtons();
    }

    private void updateButtons() {
        prevButton.active = index > 0;
        nextButton.active = index + 2 < ordered.size();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (prevButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (nextButton.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderContent(net.minecraft.client.gui.GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        renderPage(ctx, bgX + 32, bgY + 51, index);
        renderPage(ctx, bgX + 162, bgY + 51, index + 1);
    }

    private void renderPage(net.minecraft.client.gui.GuiGraphics ctx, int x, int y, int dataIndex) {
        if (dataIndex >= ordered.size()) return;

        EntityChatData data = ordered.get(dataIndex);
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        PlayerData pData = data.getPlayerData(player.getName().getString());
        int friendship = pData != null ? pData.friendship : 0;

        // Character-sheet props (use lorem fallback)
        String csName      = data.getCharacterProp("Name"); // keep real name logic for the title
        String personality = orLoremSeeded(data.getCharacterProp("Personality"),         data.entityId, "Personality", 20, 100);
        String speaking    = orLoremSeeded(firstNonNull(
                data.getCharacterProp("Speaking Style / Tone"),
                data.getCharacterProp("Speaking Style"),
                data.getCharacterProp("Tone")
        ), data.entityId, "SpeakingStyle", 20, 100);
        String skills      = orLoremSeeded(data.getCharacterProp("Skills"),              data.entityId, "Skills",      20, 100);
        String likes       = orLoremSeeded(data.getCharacterProp("Likes"),               data.entityId, "Likes",       20, 100);
        String dislikes    = orLoremSeeded(data.getCharacterProp("Dislikes"),            data.entityId, "Dislikes",    20, 100);
        String background  = orLoremSeeded(data.getCharacterProp("Background"),          data.entityId, "Background",  20, 100);

        String lastMsg = safe(data.currentMessage);
        if (lastMsg.isEmpty()) {
            lastMsg = loremSeeded(30, 90, seedFrom(data.entityId, "LastMessage"));
        }

        // Name to show: sheet name > entity name > "Unknown"
        Entity entity = getEntity(data.entityId);
        String displayName = (csName != null && !csName.equals("N/A")) ? csName
                : (entity != null ? entity.getName().getString() : "Unknown");

        // Title color by friendship
        int titleColor = 0xFF808080;          // neutral
        if (friendship < 0) titleColor = 0xFFFF3A3A; // red
        else if (friendship > 0) titleColor = 0xFF2ECC40; // green

        // Title (slightly larger, soft shadow)
        PoseHelper.push(ctx.pose());
        PoseHelper.translate(ctx.pose(), (float)x, (float)y);
        PoseHelper.scale(ctx.pose(), 1.12f, 1.12f);
        ctx.drawString(this.font, displayName, 1, 1, 0x66000000, false); // shadow
        ctx.drawString(this.font, displayName, 0, 0, titleColor, false);
        PoseHelper.pop(ctx.pose());

        int offsetY = y + Math.round(this.font.lineHeight * 1.12f) + 6;

        // Icon + friendship badge
        if (entity != null) drawEntityIcon(ctx, entity, x, offsetY);
        ResourceLocation frTex = textures.GetUI("friendship" + friendship);
        if (frTex != null) {
            RenderPipelineHelper.blitGuiTexture(ctx, frTex, x + 34, offsetY, 0, 0, 31, 21, 31, 21);
        }

        int lineY = offsetY + 35;

        // Clip everything to the page content box so nothing bleeds outside
        ctx.enableScissor(x, y, x + PAGE_CONTENT_W, y + PAGE_CONTENT_H);

        // Layout: label on one line (small), value below it (wrapped, slightly smaller)
        lineY = drawPair(ctx, "Personality", personality, x, lineY, PAGE_CONTENT_W);
        lineY = drawPair(ctx, "Speaking Style / Tone", speaking, x, lineY, PAGE_CONTENT_W);
        lineY = drawPair(ctx, "Skills", skills, x, lineY, PAGE_CONTENT_W);
        lineY = drawPair(ctx, "Likes", likes, x, lineY, PAGE_CONTENT_W);
        lineY = drawPair(ctx, "Dislikes", dislikes, x, lineY, PAGE_CONTENT_W);
        lineY = drawPair(ctx, "Background", background, x, lineY, PAGE_CONTENT_W);

        // Last Message (cap to 4 lines, smaller and muted)
        ctx.drawString(this.font, "Last Message", x, lineY, LABEL_COLOR, false);
        lineY += this.font.lineHeight;
        lineY = drawWrapped(ctx, lastMsg, x, lineY, PAGE_CONTENT_W, 0.9f, 4, 0xFF444444);

        ctx.disableScissor();

        // UUID footer (tiny light gray) aligned to page bottom
        int debugY = bgY + BG_HEIGHT - 6;
        PoseHelper.push(ctx.pose());
        PoseHelper.translate(ctx.pose(), (float)x, (float)debugY);
        PoseHelper.scale(ctx.pose(), 0.8f, 0.8f);
        ctx.drawString(this.font, data.entityId, 0, 0, LIGHT_GRAY, false);
        PoseHelper.pop(ctx.pose());
    }

// ---------- helpers ----------

    private static String firstNonNull(String... vals) {
        for (String v : vals) if (v != null && !v.isEmpty()) return v;
        return null;
    }
    private static String orLorem(String v) {
        if (v == null || v.isEmpty() || "N/A".equalsIgnoreCase(v)) return lorem(20, 100);
        return v;
    }
    private static String lorem(int min, int max) {
        String[] words = ("lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut " +
                "labore et dolore magna aliqua ut enim ad minim veniam quis nostrud exercitation ullamco laboris nisi ut " +
                "aliquip ex ea commodo consequat duis aute irure dolor in reprehenderit in voluptate velit esse cillum " +
                "dolore eu fugiat nulla pariatur excepteur sint occaecat cupidatat non proident sunt in culpa qui officia " +
                "deserunt mollit anim id est laborum").split(" ");
        int target = min + RNG.nextInt(Math.max(1, max - min + 1));
        StringBuilder sb = new StringBuilder();
        while (sb.length() < target) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(words[RNG.nextInt(words.length)]);
        }
        String s = sb.toString();
        // Trim to nearest word under target and capitalize first letter
        if (s.length() > target) s = s.substring(0, Math.max(0, s.lastIndexOf(' ', target)));
        if (!s.isEmpty()) s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        return s + ".";
    }

    /** Draw a label then a wrapped value; returns next y. */
    private int drawPair(net.minecraft.client.gui.GuiGraphics ctx, String label, String value, int x, int y, int widthPx) {
        ctx.drawString(this.font, label, x, y, LABEL_COLOR, false);
        y += this.font.lineHeight;
        return drawWrapped(ctx, value, x, y, widthPx, 0.92f, 2, BODY_COLOR) + 4; // slight gap after pair
    }

    /** Wrapped text with scaling and maxLines, adds ellipsis if truncated. Returns next y. */
    private int drawWrapped(net.minecraft.client.gui.GuiGraphics ctx, String text, int x, int y,
                            int maxWidthPx, float scale, int maxLines, int color) {
        int avail = (int)Math.floor(maxWidthPx / scale);
        String rest = text;
        int drawnPx = 0;

        PoseHelper.push(ctx.pose());
        PoseHelper.translate(ctx.pose(), (float)x, (float)y);
        PoseHelper.scale(ctx.pose(), scale, scale);

        for (int line = 0; line < maxLines && !rest.isEmpty(); line++) {
            String piece = this.font.plainSubstrByWidth(rest, avail);
            if (piece.isEmpty()) break;
            boolean hasMore = piece.length() < rest.length();
            boolean lastLine = (line == maxLines - 1);
            if (lastLine && hasMore) {
                // ellipsize last line
                while (!piece.isEmpty() && this.font.width(piece + "…") > avail) {
                    piece = piece.substring(0, piece.length() - 1);
                }
                piece = piece + "…";
                rest = "";
            } else {
                rest = rest.substring(piece.length());
            }
            ctx.drawString(this.font, piece, 0, drawnPx, color, false);
            drawnPx += this.font.lineHeight;
        }

        PoseHelper.pop(ctx.pose());
        return y + Math.round(drawnPx * scale);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void drawEntityIcon(net.minecraft.client.gui.GuiGraphics ctx, Entity entity, int x, int y) {
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        var renderer = dispatcher.getRenderer(entity);
        ResourceLocation skinId = EntityTextureHelper.getTexture(renderer, entity);
        if (skinId == null) return;
        ResourceLocation icon = textures.GetEntity(skinId.getPath());
        if (icon == null) return;
        RenderPipelineHelper.blitGuiTexture(ctx, icon,
                x, y,
                0, 0,
                30, 30,
                30, 30);
    }

    private Entity getEntity(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            var level = Minecraft.getInstance().level;
            if (level == null) return null;
            return ClientEntityFinder.getEntityByUUID(level, uuid);
        } catch (Exception e) {
            return null;
        }
    }

    private String getEntityName(String id) {
        Entity e = getEntity(id);
        return e != null ? e.getName().getString() : id;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected EditBox getTextField() {
        return this.dummyField;
    }

    @Override
    protected Component getLabelText() {
        return Component.empty();
    }

    @Override
    protected String getBackgroundTextureId() {
        return "book";
    }

    // Deterministic seed from entityId + field key (FNV-1a 64-bit)
    private static long seedFrom(String id, String key) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < id.length(); i++) { h ^= id.charAt(i); h *= 0x100000001b3L; }
        h ^= 0x9e3779b97f4a7c15L; // scramble between id and key
        for (int i = 0; i < key.length(); i++) { h ^= key.charAt(i); h *= 0x100000001b3L; }
        return h;
    }

    // Lorem with a provided seed (so it's stable per entity/field)
    private static String loremSeeded(int min, int max, long seed) {
        java.util.Random r = new java.util.Random(seed);
        String[] words = ("lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut " +
                "labore et dolore magna aliqua ut enim ad minim veniam quis nostrud exercitation ullamco laboris nisi ut " +
                "aliquip ex ea commodo consequat duis aute irure dolor in reprehenderit in voluptate velit esse cillum " +
                "dolore eu fugiat nulla pariatur excepteur sint occaecat cupidatat non proident sunt in culpa qui officia " +
                "deserunt mollit anim id est laborum").split(" ");
        int target = min + r.nextInt(Math.max(1, max - min + 1));
        StringBuilder sb = new StringBuilder();
        while (sb.length() < target) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(words[r.nextInt(words.length)]);
        }
        String s = sb.toString();
        if (s.length() > target) s = s.substring(0, Math.max(0, s.lastIndexOf(' ', target)));
        if (!s.isEmpty()) s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        return s + ".";
    }

    // If v is null/empty/"N/A", return stable lorem seeded by entityId+field
    private static String orLoremSeeded(String v, String entityId, String fieldKey, int min, int max) {
        if (v == null || v.isEmpty() || "N/A".equalsIgnoreCase(v)) {
            return loremSeeded(min, max, seedFrom(entityId, fieldKey));
        }
        return v;
    }

}

