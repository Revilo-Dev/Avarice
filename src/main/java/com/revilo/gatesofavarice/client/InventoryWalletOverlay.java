package com.revilo.gatesofavarice.client;

import com.revilo.gatesofavarice.currency.GoldCoinWallet;
import com.revilo.gatesofavarice.currency.MythicCoinWallet;
import com.revilo.gatesofavarice.dungeon.ModDimensions;
import com.revilo.gatesofavarice.registry.ModItems;
import java.text.DecimalFormat;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class InventoryWalletOverlay {

    private static final DecimalFormat MULTIPLIER_FORMAT = new DecimalFormat("0.0##");
    private static final int PLAYER_BOX_CENTER_X = 51;
    private static final int PLAYER_BOX_CENTER_Y = 66;
    private static final int ICON_SIZE = 16;
    private static final float BASE_TEXT_SCALE = 0.75F;
    private static final float WALLET_Z = 150.0F;
    private static final float TOOLTIP_Z = 500.0F;

    private InventoryWalletOverlay() {
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) event.getScreen();
        boolean inDungeon = minecraft.player.level().dimension() == ModDimensions.DUNGEON_LEVEL;
        WalletLayout layout = WalletLayout.create(minecraft, screen, inDungeon);
        boolean hovered = layout.isMouseOver(event.getMouseX(), event.getMouseY());

        renderWallet(event.getGuiGraphics(), minecraft, layout, hovered, inDungeon);

        if (hovered) {
            ItemStack coinStack = coinStack(inDungeon);
            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(0.0F, 0.0F, TOOLTIP_Z);
            List<Component> tooltip = inDungeon
                    ? List.of(
                            Component.literal("Mythic Coins"),
                            Component.literal(Integer.toString(MythicCoinWallet.get(minecraft.player))).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE),
                            Component.empty(),
                            Component.literal("Multiplier " + MULTIPLIER_FORMAT.format(MythicCoinWallet.getTotalMultiplier(minecraft.player)) + "x")
                    )
                    : List.of(
                            Component.literal("Gold Coins"),
                            Component.literal(Integer.toString(GoldCoinWallet.get(minecraft.player))).withStyle(net.minecraft.ChatFormatting.GOLD)
                    );
            event.getGuiGraphics().renderTooltip(
                    minecraft.font,
                    tooltip,
                    coinStack.getTooltipImage(),
                    coinStack,
                    event.getMouseX(),
                    event.getMouseY());
            event.getGuiGraphics().pose().popPose();
        }
    }

    private static void renderWallet(GuiGraphics guiGraphics, Minecraft minecraft, WalletLayout layout, boolean hovered, boolean inDungeon) {
        float hoverScale = hovered ? 1.1F : 1.0F;

        ItemStack coinStack = coinStack(inDungeon);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(layout.iconX(), layout.iconY(), WALLET_Z);
        guiGraphics.pose().scale(hoverScale, hoverScale, 1.0F);
        guiGraphics.renderItem(coinStack, 0, 0);
        guiGraphics.pose().popPose();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(layout.textX(), layout.textY(), WALLET_Z);
        guiGraphics.pose().scale(layout.textScale() * hoverScale, layout.textScale() * hoverScale, 1.0F);
        guiGraphics.drawString(minecraft.font, layout.text(), -1, 0, 0x101010, false);
        guiGraphics.drawString(minecraft.font, layout.text(), 1, 0, 0x101010, false);
        guiGraphics.drawString(minecraft.font, layout.text(), 0, -1, 0x101010, false);
        guiGraphics.drawString(minecraft.font, layout.text(), 0, 1, 0x101010, false);
        guiGraphics.drawString(minecraft.font, layout.text(), 0, 0, inDungeon ? 0xB06CFF : 0xFFD34D, false);
        guiGraphics.pose().popPose();
    }

    private record WalletLayout(String text, float textScale, int iconX, int iconY, float textX, float textY, int hoverLeft, int hoverTop, int hoverRight, int hoverBottom) {

        private static WalletLayout create(Minecraft minecraft, AbstractContainerScreen<?> screen, boolean inDungeon) {
            String text = formatCompactValue(inDungeon ? MythicCoinWallet.get(minecraft.player) : GoldCoinWallet.get(minecraft.player));
            int digits = text.length();
            int extraDigits = Math.max(0, digits - 4);
            float textScale = Math.max(0.42F, BASE_TEXT_SCALE - (extraDigits * 0.08F));
            float textWidth = minecraft.font.width(text) * textScale;
            float gap = 1.5F + (extraDigits * 1.0F);
            float groupWidth = ICON_SIZE + gap + textWidth;
            float centerX = screen.getGuiLeft() + PLAYER_BOX_CENTER_X;
            float centerY = screen.getGuiTop() + PLAYER_BOX_CENTER_Y + 5.0F;
            float groupLeft = centerX - (groupWidth / 2.0F);
            int iconX = Mth.floor(groupLeft);
            int iconY = Mth.floor(centerY - 8.0F);
            float textX = groupLeft + ICON_SIZE + gap - 2.0F;
            float textY = centerY - (4.0F * textScale);
            int hoverLeft = Mth.floor(groupLeft) - 2;
            int hoverTop = Mth.floor(centerY - 10.0F);
            int hoverRight = Mth.ceil(groupLeft + groupWidth) + 2;
            int hoverBottom = Mth.ceil(centerY + 8.0F);
            return new WalletLayout(text, textScale, iconX, iconY, textX, textY, hoverLeft, hoverTop, hoverRight, hoverBottom);
        }

        private boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= this.hoverLeft && mouseX <= this.hoverRight && mouseY >= this.hoverTop && mouseY <= this.hoverBottom;
        }
    }

    private static String formatCompactValue(int value) {
        if (value < 1000) {
            return Integer.toString(value);
        }
        if (value < 1_000_000) {
            return trimCompactDecimal(value / 1000.0D, value < 10_000 ? 1 : 2) + "k";
        }
        return trimCompactDecimal(value / 1_000_000.0D, value < 100_000_000 ? 1 : 2) + "M";
    }

    private static String trimCompactDecimal(double value, int maxDecimals) {
        String format = "%." + maxDecimals + "f";
        String text = String.format(java.util.Locale.ROOT, format, value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private static ItemStack coinStack(boolean inDungeon) {
        return new ItemStack(inDungeon ? ModItems.MYTHIC_COIN.get() : ModItems.GOLD_COIN.get());
    }
}
