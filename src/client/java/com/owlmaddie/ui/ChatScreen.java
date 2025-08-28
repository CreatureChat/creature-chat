// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.ui;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.network.ClientPackets;
import com.owlmaddie.utils.TextureLoader;
import com.owlmaddie.i18n.CCText;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

/**
 * The ChatScreen class displays a chat dialog UI for the player
 * and handles keyboard entry events.
 */
public class ChatScreen extends ScreenHelper {
    // Chat background size
    private static final int CHAT_BACKGROUND_WIDTH   = 261;
    private static final int CHAT_BACKGROUND_HEIGHT   = 88;

    // Chat bubble title (enter your message)
    private static final int CHAT_TITLE_OFFSET = 13;

    // text input margins and size
    private static final int TEXT_INPUT_MARGIN_X   = 22;
    private static final int TEXT_INPUT_MARGIN_TOP = 30;
    private static final int TEXT_INPUT_HEIGHT     = 20;

    // button dimensions and margins
    private static final int BUTTON_WIDTH    = 101;
    private static final int BUTTON_HEIGHT   = 21;
    private static final int BUTTON_MARGIN_X = 10;
    private static final int BUTTON_MARGIN_Y = 9;

    private EditBox textField;
    private Button sendButton;
    private Button cancelButton;
    private Entity screenEntity;
    private final Component labelText = CCText.UI_ENTER_MESSAGE.comp();
    private static final TextureLoader textures = new TextureLoader();

    public ChatScreen(Entity entity, Player player) {
        super(CCText.UI_CHAT_TITLE.comp());
        this.screenEntity = entity;
        // tell server that chat opened
        ClientPackets.sendOpenChat(entity);
    }

    @Override
    protected void init() {
        super.init();

        // Update the super background size
        BG_WIDTH = CHAT_BACKGROUND_WIDTH;
        BG_HEIGHT = CHAT_BACKGROUND_HEIGHT;
        TITLE_OFFSET = CHAT_TITLE_OFFSET;

        // center background horizontally, 1/5 down vertically
        bgX = (this.width  - BG_WIDTH)  / 2;
        bgY = (this.height - BG_HEIGHT) / 5;

        // 1) text input
        int inputX = bgX + TEXT_INPUT_MARGIN_X;
        int inputY = bgY + TEXT_INPUT_MARGIN_TOP;
        int inputW = BG_WIDTH - TEXT_INPUT_MARGIN_X * 2;
        textField = new EditBox(
                font,
                inputX, inputY,
                inputW, TEXT_INPUT_HEIGHT,
                Component.literal("")
        );
        textField.setMaxLength(ChatDataManager.MAX_CHAR_IN_USER_MESSAGE);
        textField.setResponder(this::onTextChanged);
        setFocused(textField);
        addRenderableWidget(textField);

        // 2) image buttons anchored to bottom corners
        int btnY = bgY + BG_HEIGHT - BUTTON_HEIGHT - BUTTON_MARGIN_Y;

        // CANCEL / EXIT
        cancelButton = ButtonHelper.createImageButton(
                bgX + BUTTON_MARGIN_X,            // x
                btnY,                             // y
                BUTTON_WIDTH,                     // width
                BUTTON_HEIGHT,                    // height
                textures.GetUI("chat-button-exit"),        // normal texture
                textures.GetUI("chat-button-exit-hover"),  // hover texture
                widget -> onClose(),                // onPress
                widget -> Component.empty()            // narrationSupplier
        );
        addRenderableWidget(cancelButton);

        // SEND / DONE
        sendButton = ButtonHelper.createImageButton(
                bgX + BG_WIDTH - BUTTON_WIDTH - BUTTON_MARGIN_X,  // x
                btnY,                                             // y
                BUTTON_WIDTH,                                     // width
                BUTTON_HEIGHT,                                    // height
                textures.GetUI("chat-button-done"),               // normal texture
                textures.GetUI("chat-button-done-hover"),         // hover texture
                widget -> sendChatMessage(),                      // onPress
                widget -> Component.empty()                            // narrationSupplier
        );
        sendButton.active = false;
        addRenderableWidget(sendButton);
    }

    private void sendChatMessage() {
        // Send message to server
        String message = textField.getValue();
        ClientPackets.sendChat(screenEntity, message);
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && textField.isFocused()
                && !textField.getValue().isEmpty()) {
            sendChatMessage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void onTextChanged(String text) {
        // Enable the button only if the text field is not empty
        sendButton.active = !text.isEmpty();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        ClientPackets.sendCloseChat();
    }

    @Override
    protected EditBox getTextField() {
        return this.textField;
    }

    @Override
    protected Component getLabelText() {
        return this.labelText;
    }
}
