package com.revilo.gatesofavarice.client.screen;

import com.revilo.gatesofavarice.menu.LootPouchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class LootPouchScreen extends AbstractContainerScreen<LootPouchMenu> {
    public LootPouchScreen(LootPouchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 133;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF20172C);
        graphics.fill(leftPos + 4, topPos + 14, leftPos + imageWidth - 4, topPos + 43, 0xFF352248);
        graphics.drawCenteredString(font, Component.literal("One item type only • 4 × 99 capacity"), imageWidth / 2, 5, 0xFFD8B9FF);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 16, 0xFFFFFF);
        graphics.drawString(font, playerInventoryTitle, 8, 40, 0xFFFFFF);
    }
}
