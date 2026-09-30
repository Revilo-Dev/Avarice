package com.revilo.gatesofavarice.client.screen;

import com.revilo.gatesofavarice.network.ConfirmDungeonExitPayload;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DungeonExitConfirmationScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 132;
    private static final Component MESSAGE = Component.literal("Exiting the dungeon will discard your current deck, and convert your mythic coins to gold coins, are you sure you want to exit the dungeon?");

    private final int portalEntityId;

    public DungeonExitConfirmationScreen(int portalEntityId) {
        super(Component.literal("Exit Dungeon"));
        this.portalEntityId = portalEntityId;
    }

    @Override
    protected void init() {
        int panelLeft = (this.width - PANEL_WIDTH) / 2;
        int buttonY = (this.height - PANEL_HEIGHT) / 2 + 98;
        this.addRenderableWidget(Button.builder(Component.literal("Exit").withStyle(ChatFormatting.RED), button -> respond(true))
                .pos(panelLeft + 48, buttonY)
                .size(104, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Return").withStyle(ChatFormatting.GREEN), button -> respond(false))
                .pos(panelLeft + 168, buttonY)
                .size(104, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        int panelLeft = (this.width - PANEL_WIDTH) / 2;
        int panelTop = (this.height - PANEL_HEIGHT) / 2;
        guiGraphics.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, 0xE0160F24);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + 1, 0xFFB06CFF);
        guiGraphics.drawCenteredString(this.font, Component.literal("Leave Dungeon?").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                this.width / 2, panelTop + 12, 0xFFFFFF);
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(MESSAGE, PANEL_WIDTH - 34);
        int lineY = panelTop + 35;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            guiGraphics.drawCenteredString(this.font, line, this.width / 2, lineY, 0xF3E8FF);
            lineY += 11;
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void respond(boolean exitDungeon) {
        PacketDistributor.sendToServer(new ConfirmDungeonExitPayload(this.portalEntityId, exitDungeon));
        this.onClose();
    }
}
