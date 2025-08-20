// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.ui;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.ChatMessage;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.network.ClientPackets;
import com.owlmaddie.render.EntityTextureHelper;
import com.owlmaddie.render.PoseHelper;
import com.owlmaddie.render.RenderPipelineHelper;
import com.owlmaddie.utils.ClientEntityFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Screen that displays a two-page log of recently chatted entities.
 */
public class BookScreen extends ScreenHelper {
    private static final int BOOK_WIDTH = 300;
    private static final int BOOK_HEIGHT = 200;

    private static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");

    private int index;
    private final List<EntityChatData> all;
    private List<EntityChatData> ordered;
    private EditBox dummyField;
    private EditBox searchField;
    private boolean searchVisible;
    private Button prevButton;
    private Button nextButton;
    private static final int PREV_X = 29,  PREV_Y = 156, PREV_W = 14, PREV_H = 12;
    private static final int NEXT_X = 257, NEXT_Y = 156, NEXT_W = 14, NEXT_H = 12;
    private static final int SEARCH_BTN_X = 10, SEARCH_BTN_Y = 9,  SEARCH_BTN_W = 31, SEARCH_BTN_H = 21;
    private static final int CLOSE_X = 259, CLOSE_Y = 9,  CLOSE_W = 30, CLOSE_H = 22;
    private static final int PAGE_CONTENT_W = 120;   // width of text block per page
    private static final int PAGE_CONTENT_H = 120;   // height of text block (for scissor)
    private static final int LABEL_COLOR    = 0xFF6B4A3B; // warm brown, matches book UI
    private static final int BODY_COLOR     = 0xFF2A2A2A; // dark text
    private static final int LIGHT_GRAY     = 0xFFB0B0B0;
    private static final Random RNG         = new Random();

    public BookScreen() {
        super(Component.literal("Creature Log"));
        ChatDataManager mgr = ChatDataManager.getClientInstance();
        all = mgr.entityChatDataMap.values().stream()
                .filter(data -> {
                    String nameText = resolveName(data);
                    boolean hasName = nameText != null && !nameText.isBlank();
                    boolean hasMsg = data.currentMessage != null && !data.currentMessage.isBlank();
                    return hasName && hasMsg;
                })
                .sorted(Comparator.comparingLong(BookScreen::getLastInteraction).reversed())
                .collect(Collectors.toList());
        ordered = new ArrayList<>(all);
    }

    private String resolveName(EntityChatData data) {
        Entity entity = getEntity(data.entityId);
        String nameText = null;
        if (entity instanceof Mob) {
            if (entity.getCustomName() != null) {
                nameText = entity.getCustomName().getString();
            }
        } else if (entity instanceof Player) {
            nameText = entity.getName().getString();
        }
        if (nameText == null || nameText.isBlank()) {
            String sheetName = data.getCharacterProp("Name");
            if (sheetName != null && !sheetName.isBlank() && !"N/A".equals(sheetName)) {
                nameText = sheetName;
            }
        }
        return nameText;
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

        searchField = new EditBox(font, bgX + 46, bgY + 9, 208, 21, Component.empty());
        searchField.visible = false;
        searchField.active = false;
        searchField.setResponder(text -> {
            if (searchVisible) onSearchChanged(text);
        });

        addRenderableWidget(searchField);

        prevButton = ButtonHelper.createImageButton(
                bgX + PREV_X, bgY + PREV_Y,
                PREV_W, PREV_H,
                textures.GetUI("book/previous"),
                textures.GetUI("book/previous-hover"),
                w -> {
                    index = Math.max(0, index - 2);
                    updateButtons();
                    requestDataForCurrentPages();
                    LOGGER.info("BookScreen: previous page index={}", index);
                },
                w -> Component.empty()
        );
        addRenderableWidget(prevButton);

        nextButton = ButtonHelper.createImageButton(
                bgX + NEXT_X, bgY + NEXT_Y,
                NEXT_W, NEXT_H,
                textures.GetUI("book/next"),
                textures.GetUI("book/next-hover"),
                w -> {
                    index = Math.min(ordered.size(), index + 2);
                    updateButtons();
                    requestDataForCurrentPages();
                    LOGGER.info("BookScreen: next page index={}", index);
                },
                w -> Component.empty()
        );
        addRenderableWidget(nextButton);

        updateButtons();
        requestDataForCurrentPages();
    }

    private void updateButtons() {
        boolean prevActive = index > 0;
        boolean nextActive = index + 2 < ordered.size();
        if (prevButton != null) {
            prevButton.active = prevActive;
            prevButton.visible = prevActive;
        }
        if (nextButton != null) {
            nextButton.active = nextActive;
            nextButton.visible = nextActive;
        }
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, bgX + SEARCH_BTN_X, bgY + SEARCH_BTN_Y, SEARCH_BTN_W, SEARCH_BTN_H)) {
                toggleSearch();
                return true;
            }
            if (inside(mouseX, mouseY, bgX + CLOSE_X, bgY + CLOSE_Y, CLOSE_W, CLOSE_H)) {
                LOGGER.info("BookScreen: close button pressed");
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void toggleSearch() {
        searchVisible = !searchVisible;
        searchField.visible = searchVisible;
        searchField.active = searchVisible;
        searchField.setFocused(searchVisible);
        if (searchVisible) {
            setFocused(searchField);
            setInitialFocus(searchField);
            searchField.setCursorPosition(searchField.getValue().length());
            LOGGER.info("BookScreen: search opened and focused");
        } else {
            setFocused(null);
            searchField.setValue("");
            ordered = new ArrayList<>(all);
            index = 0;
            updateButtons();
            requestDataForCurrentPages();
            LOGGER.info("BookScreen: search closed");
        }
    }

    private void onSearchChanged(String text) {
        String q = text.toLowerCase(Locale.ROOT);
        ordered = all.stream()
                .filter(d -> {
                    String n = resolveName(d);
                    return n != null && n.toLowerCase(Locale.ROOT).contains(q);
                })
                .collect(Collectors.toList());
        index = 0;
        updateButtons();
        requestDataForCurrentPages();
        LOGGER.info("BookScreen: search '{}' results={}", q, ordered.size());
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

        String playerName = player.getDisplayName().getString();
        PlayerData pData = data.getPlayerData(playerName);
        int friendship = pData != null ? pData.friendship : 0;

        // Character-sheet props (use lorem fallback)
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

        // Name to show: custom name > sheet name > entity name
        Entity entity = getEntity(data.entityId);
        String displayName = resolveName(data);
        if (displayName == null || displayName.isBlank()) {
            displayName = (entity != null ? entity.getName().getString() : "Unknown");
        }

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

    private void requestDataForCurrentPages() {
        if (index < ordered.size()) {
            UUID left = UUID.fromString(ordered.get(index).entityId);
            ClientPackets.requestEntityData(left);
        }
        if (index + 1 < ordered.size()) {
            UUID right = UUID.fromString(ordered.get(index + 1).entityId);
            ClientPackets.requestEntityData(right);
        }
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
        return "book/book";
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

