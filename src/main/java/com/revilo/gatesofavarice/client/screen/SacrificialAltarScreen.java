package com.revilo.gatesofavarice.client.screen;

import com.revilo.gatesofavarice.menu.SacrificialAltarMenu;
import com.revilo.gatesofavarice.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class SacrificialAltarScreen extends AbstractContainerScreen<SacrificialAltarMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/shop/shop-sell-gui.png");
    private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.withDefaultNamespace("widget/button");
    private static final int BUTTON_X = 117;
    private static final int BUTTON_Y = 56;
    private static final int BUTTON_WIDTH = 51;
    private static final int BUTTON_HEIGHT = 15;

    public SacrificialAltarScreen(SacrificialAltarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        titleLabelY = 10000;
        inventoryLabelY = 10000;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        int value = menu.getSacrificeValue();
        int buttonX = leftPos + BUTTON_X;
        int buttonY = topPos + BUTTON_Y;
        graphics.blit(BUTTON_TEXTURE, buttonX, buttonY, 0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT);
        graphics.drawCenteredString(font, Component.literal("SACRIFICE"), buttonX + BUTTON_WIDTH / 2, buttonY + 4, value > 0 ? 0xF4E9FF : 0x777777);
        graphics.renderItem(new net.minecraft.world.item.ItemStack(ModItems.MYTHIC_COIN.get()), leftPos + 134, topPos + 23);
        graphics.drawCenteredString(font, Component.literal(Integer.toString(value)), leftPos + 150, topPos + 44, 0xB06CFF);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.literal("Sacrifice items for 50% value"), 8, 78, 0xD7F0D9, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= leftPos + BUTTON_X && mouseX < leftPos + BUTTON_X + BUTTON_WIDTH
                && mouseY >= topPos + BUTTON_Y && mouseY < topPos + BUTTON_Y + BUTTON_HEIGHT && menu.getSacrificeValue() > 0) {
            Minecraft minecraft = this.minecraft;
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SacrificialAltarMenu.SACRIFICE_BUTTON_ID);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
