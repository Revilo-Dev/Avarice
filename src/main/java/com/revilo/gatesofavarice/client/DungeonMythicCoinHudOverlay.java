package com.revilo.gatesofavarice.client;

import com.revilo.gatesofavarice.currency.MythicCoinWallet;
import com.revilo.gatesofavarice.dungeon.ModDimensions;
import com.revilo.gatesofavarice.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Always-visible Mythic Coin counter for a dungeon run. */
public final class DungeonMythicCoinHudOverlay {
    private static final int MARGIN = 8;
    private static final int ICON_SIZE = 16;

    private DungeonMythicCoinHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui
                || minecraft.player.level().dimension() != ModDimensions.DUNGEON_LEVEL) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        String coins = formatCompactValue(MythicCoinWallet.get(minecraft.player));
        int width = ICON_SIZE + 3 + minecraft.font.width(coins);
        int x = minecraft.getWindow().getGuiScaledWidth() - MARGIN - width;
        int y = minecraft.getWindow().getGuiScaledHeight() - MARGIN - ICON_SIZE;

        graphics.fill(x - 4, y - 3, x + width + 4, y + ICON_SIZE + 3, 0xB0100818);
        graphics.renderItem(new ItemStack(ModItems.MYTHIC_COIN.get()), x, y);
        int textX = x + ICON_SIZE + 3;
        int textY = y + 4;
        graphics.drawString(minecraft.font, coins, textX - 1, textY, 0xFF120A1E, false);
        graphics.drawString(minecraft.font, coins, textX + 1, textY, 0xFF120A1E, false);
        graphics.drawString(minecraft.font, coins, textX, textY - 1, 0xFF120A1E, false);
        graphics.drawString(minecraft.font, coins, textX, textY + 1, 0xFF120A1E, false);
        graphics.drawString(minecraft.font, coins, textX, textY, 0xFFD8A3FF, false);
    }

    private static String formatCompactValue(int value) {
        if (value < 1_000) {
            return Integer.toString(value);
        }
        if (value < 1_000_000) {
            return compact(value / 1_000.0D) + "k";
        }
        return compact(value / 1_000_000.0D) + "M";
    }

    private static String compact(double value) {
        String text = String.format(java.util.Locale.ROOT, value < 10.0D ? "%.1f" : "%.0f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }
}
