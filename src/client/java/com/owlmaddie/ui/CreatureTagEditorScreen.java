package com.owlmaddie.ui;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;

public class CreatureTagEditorScreen extends HandledScreen<TagEditorScreenHandler> {
    private TextFieldWidget nameField;

    public CreatureTagEditorScreen(TagEditorScreenHandler handler, PlayerEntity player) {
        super(handler, player.getInventory(), Text.literal("Creature Tag Editor"));
    }

    @Override
    protected void init() {
        super.init();
        this.nameField = new TextFieldWidget(this.textRenderer, this.x + 10, this.y + 10, 150, 20, Text.literal("Enter name"));
        this.addDrawableChild(this.nameField);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawTextWithShadow(this.textRenderer, this.title, 8, 6, 0xFFFFFF);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.fillGradient(this.x, this.y, this.x + this.backgroundWidth, this.y + this.backgroundHeight, 0xFFCCCCCC, 0xFF888888);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.nameField.isMouseOver(mouseX, mouseY)) {
            this.nameField.setFocused(true);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
