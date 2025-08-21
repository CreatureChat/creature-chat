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
import com.owlmaddie.utils.EntityCreationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import java.time.Instant;
import java.time.ZoneId;
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

    private enum Mode { SUMMARY, DETAIL }
    private Mode mode = Mode.SUMMARY;

    // summary state
    private int summaryIndex;
    private int hoveredSummary = -1;

    // detail state
    private EntityChatData detailEntity;
    private int detailPage;            // left page index
    private int detailTotalPages;      // total single pages for detailEntity
    private List<Pair> detailSections; // character sheet sections
    private List<List<Pair>> sectionPages;
    private List<ChatMessage> detailMessages;
    private List<List<ChatMessage>> messagePages;
    private int sectionRemainingSpace;
    private boolean messagesOnSectionPage;

    private final List<EntityChatData> all;
    private List<EntityChatData> ordered;
    private EditBox dummyField;
    private EditBox searchField;
    private boolean searchVisible;
    private Button prevButton;
    private Button nextButton;
    private static final int PREV_X = 28,  PREV_Y = 170, PREV_W = 14, PREV_H = 12;
    private static final int NEXT_X = 267, NEXT_Y = 170, NEXT_W = 14, NEXT_H = 12;
    private static final int SEARCH_BTN_X = 10, SEARCH_BTN_Y = 9,  SEARCH_BTN_W = 31, SEARCH_BTN_H = 21;
    private static final int CLOSE_X = 259, CLOSE_Y = 9,  CLOSE_W = 30, CLOSE_H = 22;
    private static final int PAGE_CONTENT_W = 116;   // width of text block per page
    private static final int PAGE_CONTENT_H = 124;   // height of text block (for scissor)
    private static final int PAGE1_X = 28, PAGE1_Y = 46; // left page top-left
    private static final int PAGE2_X = 157, PAGE2_Y = 46; // right page top-left
    private static final int LABEL_COLOR    = 0xFF6B4A3B; // warm brown, matches book UI
    private static final int BODY_COLOR     = 0xFF2A2A2A; // dark text
    private static final int LIGHT_GRAY     = 0xFFB0B0B0;
    private static final Random RNG         = new Random();

    private static final int SUMMARY_ROWS_PER_PAGE = 4; // per single page
    private static final int SUMMARY_ROW_H = 30;

    private static class Pair {
        final String label;
        final String value;
        Pair(String l, String v) { this.label = l; this.value = v; }
    }

    public BookScreen() {
        super(Component.literal("Creature Log"));
        ChatDataManager mgr = ChatDataManager.getClientInstance();
        all = mgr.entityChatDataMap.values().stream()
                .filter(data -> {
                    String nameText = resolveName(data);
                    boolean hasName = nameText != null && !nameText.isBlank();
                    boolean hasMsg = data.currentMessage != null && !data.currentMessage.isBlank();
                    return hasName && (hasMsg || data.death != null);
                })
                .sorted(Comparator.comparingLong(BookScreen::getLastInteraction).reversed())
                .collect(Collectors.toList());
        ordered = new ArrayList<>(all);
        sortOrdered();
        summaryIndex = 0;
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
            if (data.entityName != null && !data.entityName.isBlank()) {
                nameText = data.entityName;
            }
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

    private String friendlyTime(long millis) {
        long minutes = millis / 60000L;
        if (minutes < 60) return minutes + "min";
        long hours = minutes / 60;
        long mins = minutes % 60;
        if (hours < 24) {
            String res = hours + "Hr";
            if (hours < 4 && mins > 0) res += " " + mins + "min";
            return res;
        }
        long days = hours / 24;
        if (days < 7) return days + (days == 1 ? " day" : " days");
        long weeks = days / 7;
        if (weeks < 4) return weeks + (weeks == 1 ? " week" : " weeks");
        long months = days / 30;
        return months + (months == 1 ? " month" : " months");
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

        prevButton = createPageButton(
                bgX + PREV_X, bgY + PREV_Y,
                PREV_W, PREV_H,
                textures.GetUI("book/previous"),
                textures.GetUI("book/previous-hover"),
                w -> {
                    if (mode == Mode.SUMMARY) {
                        summaryIndex = Math.max(0, summaryIndex - SUMMARY_ROWS_PER_PAGE * 2);
                    } else {
                        if (detailPage == 0) {
                            mode = Mode.SUMMARY;
                            detailEntity = null;
                        } else {
                            detailPage = Math.max(0, detailPage - 2);
                        }
                    }
                    updateButtons();
                    requestDataForCurrentPages();
                    LOGGER.info("BookScreen: previous clicked mode={} summaryIndex={} detailPage={}", mode, summaryIndex, detailPage);
                }
        );
        addRenderableWidget(prevButton);

        nextButton = createPageButton(
                bgX + NEXT_X, bgY + NEXT_Y,
                NEXT_W, NEXT_H,
                textures.GetUI("book/next"),
                textures.GetUI("book/next-hover"),
                w -> {
                    if (mode == Mode.SUMMARY) {
                        int spread = SUMMARY_ROWS_PER_PAGE * 2;
                        int maxIndex = Math.max(0, ((ordered.size() - 1) / spread) * spread);
                        summaryIndex = Math.min(summaryIndex + spread, maxIndex);
                    } else {
                        int maxLeft = ((detailTotalPages - 1) / 2) * 2;
                        detailPage = Math.min(detailPage + 2, maxLeft);
                    }
                    updateButtons();
                    requestDataForCurrentPages();
                    LOGGER.info("BookScreen: next clicked mode={} summaryIndex={} detailPage={}", mode, summaryIndex, detailPage);
                }
        );
        addRenderableWidget(nextButton);

        updateButtons();
        requestDataForCurrentPages();
    }

    private void updateButtons() {
        boolean prevActive;
        boolean nextActive;
        if (mode == Mode.SUMMARY) {
            prevActive = summaryIndex > 0;
            nextActive = summaryIndex + SUMMARY_ROWS_PER_PAGE * 2 < ordered.size();
        } else {
            prevActive = true; // always allow returning to summary
            nextActive = detailPage + 2 < detailTotalPages;
        }
        if (prevButton != null) {
            prevButton.active = prevActive;
            prevButton.visible = prevActive;
        }
        if (nextButton != null) {
            nextButton.active = nextActive;
            nextButton.visible = nextActive;
        }
    }

    private Button createPageButton(int x, int y, int width, int height,
                                    ResourceLocation normalTex, ResourceLocation hoverTex,
                                    Button.OnPress onPress) {
        return new Button(x, y, width, height, Component.empty(), onPress, w -> Component.empty()) {
            @Override
            public void renderWidget(net.minecraft.client.gui.GuiGraphics ctx, int mouseX, int mouseY, float delta) {
                ResourceLocation tex = isHovered() ? hoverTex : normalTex;
                RenderPipelineHelper.blitGuiTexture(ctx, tex, getX(), getY(), 0, 0, width, height, width, height);
            }

            @Override
            public void playDownSound(net.minecraft.client.sounds.SoundManager mgr) {
                mgr.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
            }
        };
    }

    private void sortOrdered() {
        ordered.sort(Comparator.comparingLong(BookScreen::getLastInteraction).reversed());
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
            if (mode == Mode.SUMMARY && hoveredSummary >= 0) {
                openDetail(hoveredSummary);
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
            sortOrdered();
            summaryIndex = 0;
            mode = Mode.SUMMARY;
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
        sortOrdered();
        summaryIndex = 0;
        mode = Mode.SUMMARY;
        updateButtons();
        requestDataForCurrentPages();
        LOGGER.info("BookScreen: search '{}' results={}", q, ordered.size());
    }

    private void openDetail(int idx) {
        if (idx < 0 || idx >= ordered.size()) return;
        detailEntity = ordered.get(idx);
        detailPage = 0;
        detailSections = new ArrayList<>();
        if (detailEntity.death != null) {
            String deathDate = Instant.ofEpochMilli(detailEntity.death)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toString();
            detailSections.add(new Pair("Death Date", deathDate));
        }

        detailSections.add(new Pair("Personality", orLoremSeeded(detailEntity.getCharacterProp("Personality"), detailEntity.entityId, "Personality", 20, 100)));
        detailSections.add(new Pair("Speaking Style / Tone", orLoremSeeded(firstNonNull(
                detailEntity.getCharacterProp("Speaking Style / Tone"),
                detailEntity.getCharacterProp("Speaking Style"),
                detailEntity.getCharacterProp("Tone")
        ), detailEntity.entityId, "SpeakingStyle", 20, 100)));
        detailSections.add(new Pair("Skills", orLoremSeeded(detailEntity.getCharacterProp("Skills"), detailEntity.entityId, "Skills", 20, 100)));
        detailSections.add(new Pair("Likes", orLoremSeeded(detailEntity.getCharacterProp("Likes"), detailEntity.entityId, "Likes", 20, 100)));
        detailSections.add(new Pair("Dislikes", orLoremSeeded(detailEntity.getCharacterProp("Dislikes"), detailEntity.entityId, "Dislikes", 20, 100)));
        detailSections.add(new Pair("Background", orLoremSeeded(detailEntity.getCharacterProp("Background"), detailEntity.entityId, "Background", 20, 100)));

        detailMessages = new ArrayList<>();
        if (detailEntity.previousMessages != null) {
            Player player = Minecraft.getInstance().player;
            String pName = player != null ? player.getDisplayName().getString() : "";
            List<ChatMessage> filtered = detailEntity.previousMessages.stream()
                    .filter(m -> !(m.sender == ChatDataManager.ChatSender.USER && !pName.equals(m.name)))
                    .collect(Collectors.toList());
            if (detailEntity.death != null) {
                for (int i = filtered.size() - 1; i >= 0; i--) {
                    ChatMessage m = filtered.get(i);
                    if (m.sender == ChatDataManager.ChatSender.ASSISTANT) {
                        detailMessages.add(m);
                        break;
                    }
                }
            } else {
                for (int i = Math.max(0, filtered.size() - 4); i < filtered.size(); i++) {
                    detailMessages.add(filtered.get(i));
                }
                Collections.reverse(detailMessages);
            }
        }

        paginateSections();
        paginateMessages();

        detailTotalPages = sectionPages.size() + messagePages.size() - (messagesOnSectionPage ? 1 : 0);
        mode = Mode.DETAIL;
        updateButtons();
        requestDataForCurrentPages();
    }

    private void paginateSections() {
        sectionPages = new ArrayList<>();
        sectionRemainingSpace = 0;
        List<Pair> page = new ArrayList<>();
        int used = 0;
        int headerHeight = Math.round(this.font.lineHeight * 1.12f) + 6 + 35;
        for (Pair p : detailSections) {
            int h = this.font.lineHeight;
            h += measureWrappedHeight(p.value, PAGE_CONTENT_W, 0.92f, 3);
            h += 4;
            int available = sectionPages.isEmpty() ? PAGE_CONTENT_H - headerHeight : PAGE_CONTENT_H;
            if (used + h >= available && !page.isEmpty()) {
                sectionPages.add(page);
                page = new ArrayList<>();
                used = 0;
                available = PAGE_CONTENT_H;
            }
            page.add(p);
            used += h;
        }
        if (!page.isEmpty()) {
            sectionPages.add(page);
        }
        if (sectionPages.isEmpty()) {
            sectionPages.add(Collections.emptyList());
            sectionRemainingSpace = PAGE_CONTENT_H - headerHeight;
        } else {
            int available = sectionPages.size() == 1 ? PAGE_CONTENT_H - headerHeight : PAGE_CONTENT_H;
            sectionRemainingSpace = Math.max(0, available - used);
        }
    }

    private void paginateMessages() {
        messagePages = new ArrayList<>();
        int headerH = this.font.lineHeight + 2;
        int minMsgH = this.font.lineHeight + 1 + Math.round(this.font.lineHeight * 0.9f) + 4;
        messagesOnSectionPage = sectionRemainingSpace >= headerH + minMsgH && !detailMessages.isEmpty();
        int available = messagesOnSectionPage ? sectionRemainingSpace - headerH : PAGE_CONTENT_H - headerH;
        if (available < 0) available = 0;
        List<ChatMessage> page = new ArrayList<>();
        int used = 0;
        for (ChatMessage m : detailMessages) {
            int h = font.lineHeight + 1;
            h += measureWrappedHeight(safe(m.message), PAGE_CONTENT_W - 4, 0.9f, 4) + 4;
            if (used + h >= available && !page.isEmpty()) {
                messagePages.add(page);
                page = new ArrayList<>();
                used = 0;
                available = PAGE_CONTENT_H - headerH;
            }
            if (used + h >= available && page.isEmpty()) {
                // ensure progress even if single message exceeds available
                available = PAGE_CONTENT_H - headerH;
            }
            page.add(m);
            used += h;
        }
        if (!page.isEmpty()) {
            messagePages.add(page);
        }
        if (messagePages.isEmpty()) {
            messagePages.add(Collections.emptyList());
        }
    }

    @Override
    protected void renderContent(net.minecraft.client.gui.GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        if (mode == Mode.SUMMARY) {
            hoveredSummary = -1;
            renderSummaryPage(ctx, bgX + PAGE1_X, bgY + PAGE1_Y, summaryIndex, mouseX, mouseY);
            renderSummaryPage(ctx, bgX + PAGE2_X, bgY + PAGE2_Y, summaryIndex + SUMMARY_ROWS_PER_PAGE, mouseX, mouseY);
        } else if (detailEntity != null) {
            renderDetailPage(ctx, bgX + PAGE1_X, bgY + PAGE1_Y, detailPage);
            renderDetailPage(ctx, bgX + PAGE2_X, bgY + PAGE2_Y, detailPage + 1);
        }
    }

    private void renderSummaryPage(net.minecraft.client.gui.GuiGraphics ctx, int x, int y, int startIndex, int mouseX, int mouseY) {
        ctx.enableScissor(x, y, x + PAGE_CONTENT_W, y + PAGE_CONTENT_H);
        for (int i = 0; i < SUMMARY_ROWS_PER_PAGE; i++) {
            int idx = startIndex + i;
            if (idx >= ordered.size()) break;
            int rowY = y + i * SUMMARY_ROW_H;
            boolean hover = mouseX >= x && mouseX <= x + PAGE_CONTENT_W && mouseY >= rowY && mouseY <= rowY + SUMMARY_ROW_H;
            if (hover) {
                ctx.fill(x, rowY, x + PAGE_CONTENT_W, rowY + SUMMARY_ROW_H, 0x406B4A3B);
                hoveredSummary = idx;
            }
            EntityChatData data = ordered.get(idx);
            Entity entity = getEntity(data.entityId);
            if (entity == null && data.entityType != null) {
                EntityType<?> type = EntityType.byString(data.entityType).orElse(null);
                if (type != null) {
                    entity = EntityCreationHelper.create(type);
                }
            }
            if (entity != null) {
                PoseHelper.push(ctx.pose());
                PoseHelper.scale(ctx.pose(), 0.6f, 0.6f);
                drawEntityIcon(ctx, entity, (int) (x / 0.6f), (int) (rowY / 0.6f));
                PoseHelper.pop(ctx.pose());
            }

            String name = resolveName(data);
            if (name == null) name = "Unknown";
            int avail = PAGE_CONTENT_W - 22;
            if (data.death != null) avail -= this.font.width("RIP: ");
            name = this.font.plainSubstrByWidth(name, avail);
            if (data.death != null) {
                Component comp = Component.literal("RIP: ")
                        .append(Component.literal(name).withStyle(Style.EMPTY.withStrikethrough(true)));
                ctx.drawString(this.font, comp, x + 22, rowY + 2, BODY_COLOR, false);
            } else {
                ctx.drawString(this.font, name, x + 22, rowY + 2, BODY_COLOR, false);
            }

            Player player = Minecraft.getInstance().player;
            PlayerData pData = player != null ? data.getPlayerData(player.getDisplayName().getString()) : null;
            int friendship = pData != null ? pData.friendship : 0;
            ResourceLocation frTex = textures.GetUI("friendship" + friendship);
            int iconW = 0;
            if (frTex != null) {
                PoseHelper.push(ctx.pose());
                PoseHelper.translate(ctx.pose(), x + 22, rowY + 12);
                PoseHelper.scale(ctx.pose(), 0.6f, 0.6f);
                RenderPipelineHelper.blitGuiTexture(ctx, frTex, 0, 0, 0, 0, 31, 21, 31, 21);
                PoseHelper.pop(ctx.pose());
                iconW = Math.round(31 * 0.6f);
            }

            long last = getLastInteraction(data);
            String time = friendlyTime(System.currentTimeMillis() - last);
            ctx.drawString(this.font, time, x + 22 + iconW + 4, rowY + this.font.lineHeight + 4, BODY_COLOR, false);
        }
        ctx.disableScissor();
    }

    private void renderDetailPage(net.minecraft.client.gui.GuiGraphics ctx, int x, int y, int pageIndex) {
        if (pageIndex >= detailTotalPages) return;
        if (detailEntity == null) return;

        Player player = Minecraft.getInstance().player;
        String playerName = player != null ? player.getDisplayName().getString() : "";
        PlayerData pData = detailEntity.getPlayerData(playerName);
        int friendship = pData != null ? pData.friendship : 0;
        Entity entity = getEntity(detailEntity.entityId);
        if (entity == null && detailEntity.entityType != null) {
            EntityType<?> type = EntityType.byString(detailEntity.entityType).orElse(null);
            if (type != null) {
                entity = EntityCreationHelper.create(type);
            }
        }

        int sectionPageCount = sectionPages.size();

        if (pageIndex < sectionPageCount) { // character sheet pages
            List<Pair> page = sectionPages.get(pageIndex);
            int lineY = y;
            if (pageIndex == 0) {
                String displayName = resolveName(detailEntity);
                if (displayName == null || displayName.isBlank()) {
                    displayName = entity != null ? entity.getName().getString() : "Unknown";
                }
                int titleColor = 0xFF000000;
                if (friendship < 0) titleColor = 0xFFFF3A3A;
                else if (friendship > 0) titleColor = 0xFF2ECC40;
                int availNameW = (int)Math.floor(PAGE_CONTENT_W / 1.12f);
                if (detailEntity.death != null) availNameW -= this.font.width("RIP: ");
                displayName = this.font.plainSubstrByWidth(displayName, availNameW);
                Component comp = detailEntity.death != null
                        ? Component.literal("RIP: ").append(Component.literal(displayName).withStyle(Style.EMPTY.withStrikethrough(true)))
                        : Component.literal(displayName);
                PoseHelper.push(ctx.pose());
                PoseHelper.translate(ctx.pose(), (float)x, (float)y);
                PoseHelper.scale(ctx.pose(), 1.12f, 1.12f);
                ctx.drawString(this.font, comp, 1, 1, 0x66000000, false);
                ctx.drawString(this.font, comp, 0, 0, titleColor, false);
                PoseHelper.pop(ctx.pose());
                lineY = y + Math.round(this.font.lineHeight * 1.12f) + 6;
                if (entity != null) drawEntityIcon(ctx, entity, x, lineY);
                ResourceLocation frTex = textures.GetUI("friendship" + friendship);
                if (frTex != null) {
                    RenderPipelineHelper.blitGuiTexture(ctx, frTex, x + 34, lineY, 0, 0, 31, 21, 31, 21);
                }
                lineY += 35;
            }
            ctx.enableScissor(x, y, x + PAGE_CONTENT_W, y + PAGE_CONTENT_H);
            for (Pair p : page) {
                lineY = drawPair(ctx, p.label, p.value, x, lineY, PAGE_CONTENT_W);
            }
            // append messages on same page if space was reserved
            if (messagesOnSectionPage && pageIndex == sectionPageCount - 1) {
                lineY = renderMessagesPage(ctx, x, lineY, messagePages.get(0));
            }
            ctx.disableScissor();
        } else { // messages pages
            int msgPage = pageIndex - sectionPageCount + (messagesOnSectionPage ? 1 : 0);
            List<ChatMessage> msgs = messagePages.get(msgPage);
            ctx.enableScissor(x, y, x + PAGE_CONTENT_W, y + PAGE_CONTENT_H);
            renderMessagesPage(ctx, x, y, msgs);
            ctx.disableScissor();
        }

        int debugY = bgY + BG_HEIGHT - 6;
        PoseHelper.push(ctx.pose());
        PoseHelper.translate(ctx.pose(), (float)x, (float)debugY);
        PoseHelper.scale(ctx.pose(), 0.8f, 0.8f);
        ctx.drawString(this.font, detailEntity.entityId, 0, 0, LIGHT_GRAY, false);
        PoseHelper.pop(ctx.pose());
    }

    private int renderMessagesPage(net.minecraft.client.gui.GuiGraphics ctx, int x, int startY, List<ChatMessage> msgs) {
        int lineY = startY;
        String header = detailEntity != null && detailEntity.death != null ? "Last Words" : "Recent Messages";
        ctx.drawString(this.font, header, x, lineY, LABEL_COLOR, false);
        lineY += this.font.lineHeight + 2;
        for (ChatMessage m : msgs) {
            String speaker = m.sender == ChatDataManager.ChatSender.USER ? "You" : resolveName(detailEntity);
            if (speaker == null || speaker.isBlank()) speaker = "Unknown";
            long ts = m.timestamp == null ? 0L : m.timestamp;
            String ago = friendlyTime(System.currentTimeMillis() - ts) + " ago";
            int timeW = this.font.width(ago);
            speaker = this.font.plainSubstrByWidth(speaker, PAGE_CONTENT_W - timeW - 2);
            ctx.drawString(this.font, speaker, x, lineY, LABEL_COLOR, false);
            ctx.drawString(this.font, ago, x + PAGE_CONTENT_W - timeW, lineY, LABEL_COLOR, false);
            lineY += this.font.lineHeight + 1;
            lineY = drawWrapped(ctx, safe(m.message), x + 4, lineY, PAGE_CONTENT_W - 4, 0.9f, 4, BODY_COLOR);
            lineY += 4;
        }
        return lineY;
    }

    private void requestDataForCurrentPages() {
        sortOrdered();
        if (mode == Mode.SUMMARY) {
            for (int i = 0; i < SUMMARY_ROWS_PER_PAGE * 2; i++) {
                int idx = summaryIndex + i;
                if (idx >= ordered.size()) break;
                UUID id = UUID.fromString(ordered.get(idx).entityId);
                ClientPackets.requestEntityData(id);
            }
        } else if (detailEntity != null) {
            UUID id = UUID.fromString(detailEntity.entityId);
            ClientPackets.requestEntityData(id);
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
        return drawWrapped(ctx, value, x, y, widthPx, 0.92f, 3, BODY_COLOR) + 4; // slight gap after pair
    }

    /** Wrapped text with scaling and maxLines, adds ellipsis if truncated. Returns next y. */
    private int drawWrapped(net.minecraft.client.gui.GuiGraphics ctx, String text, int x, int y,
                            int maxWidthPx, float scale, int maxLines, int color) {
        List<String> lines = wrapLines(text, maxWidthPx, scale, maxLines);
        PoseHelper.push(ctx.pose());
        PoseHelper.translate(ctx.pose(), (float)x, (float)y);
        PoseHelper.scale(ctx.pose(), scale, scale);
        int drawnPx = 0;
        for (String line : lines) {
            ctx.drawString(this.font, line, 0, drawnPx, color, false);
            drawnPx += this.font.lineHeight;
        }
        PoseHelper.pop(ctx.pose());
        return y + Math.round(drawnPx * scale);
    }

    private int measureWrappedHeight(String text, int maxWidthPx, float scale, int maxLines) {
        List<String> lines = wrapLines(text, maxWidthPx, scale, maxLines);
        return (int)Math.ceil(lines.size() * this.font.lineHeight * scale);
    }

    private List<String> wrapLines(String text, int maxWidthPx, float scale, int maxLines) {
        int avail = (int)Math.floor(maxWidthPx / scale);
        List<String> lines = new ArrayList<>();
        String rest = text == null ? "" : text.trim();
        while (!rest.isEmpty() && lines.size() < maxLines) {
            String piece = this.font.plainSubstrByWidth(rest, avail);
            int cut = piece.length();
            if (cut <= 0) break;
            if (cut < rest.length()) {
                int space = piece.lastIndexOf(' ');
                if (space > 0) {
                    cut = space;
                    piece = piece.substring(0, space);
                }
            }
            lines.add(piece.trim());
            rest = rest.substring(Math.min(rest.length(), cut)).trim();
        }
        if (!rest.isEmpty() && !lines.isEmpty()) {
            String last = lines.get(lines.size() - 1);
            while (!last.isEmpty() && this.font.width(last + "…") > avail) {
                last = last.substring(0, last.length() - 1);
            }
            lines.set(lines.size() - 1, last + "…");
        }
        return lines;
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

