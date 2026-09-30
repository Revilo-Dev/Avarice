package com.revilo.gatesofavarice.client;

import com.revilo.gatesofavarice.currency.MythicCoinWallet;
import com.revilo.gatesofavarice.dungeon.DungeonHudState;
import com.revilo.gatesofavarice.dungeon.ModDimensions;
import com.revilo.gatesofavarice.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Always-visible Mythic Coin counter for a dungeon run. */
public final class DungeonMythicCoinHudOverlay {
    private static final int MARGIN = 8;
    private static final int ICON_SIZE = 16;
    private static final int TIME_OFFSET_X = 6;
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "dungeon_mythic_coin_counter");
    private static long pausedPlayTimeTicks;
    private static boolean clockPaused;

    private DungeonMythicCoinHudOverlay() {
    }

    public static void registerGuiLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, DungeonMythicCoinHudOverlay::render);
    }

    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui
                || minecraft.player.level().dimension() != ModDimensions.DUNGEON_LEVEL) {
            return;
        }

        String coins = formatCompactValue(MythicCoinWallet.get(minecraft.player));
        int width = ICON_SIZE + 3 + minecraft.font.width(coins);
        int x = MARGIN;
        int y = MARGIN;

        graphics.renderItem(new ItemStack(ModItems.MYTHIC_COIN.get()), x, y);
        int textX = x + ICON_SIZE + 3;
        int textY = y + 4;
        graphics.drawString(minecraft.font, coins, textX - 1, textY, 0xFF120A1E, false);
        graphics.drawString(minecraft.font, coins, textX + 1, textY, 0xFF120A1E, false);
        graphics.drawString(minecraft.font, coins, textX, textY - 1, 0xFF120A1E, false);
        graphics.drawString(minecraft.font, coins, textX, textY + 1, 0xFF120A1E, false);
        graphics.drawString(minecraft.font, coins, textX, textY, 0xFFD8A3FF, false);

        String timePlayed = "Time " + formatTime(currentPlayTime(minecraft));
        int timeX = x + (width - minecraft.font.width(timePlayed)) / 2 + TIME_OFFSET_X;
        int timeY = y + ICON_SIZE + 2;
        graphics.drawString(minecraft.font, timePlayed, timeX - 1, timeY, 0xFF120A1E, false);
        graphics.drawString(minecraft.font, timePlayed, timeX + 1, timeY, 0xFF120A1E, false);
        graphics.drawString(minecraft.font, timePlayed, timeX, timeY - 1, 0xFF120A1E, false);
        graphics.drawString(minecraft.font, timePlayed, timeX, timeY + 1, 0xFF120A1E, false);
        graphics.drawString(minecraft.font, timePlayed, timeX, timeY, 0xFFF3E8FF, false);
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

    private static String formatTime(long ticks) {
        long totalSeconds = Math.max(0L, ticks) / 20L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return hours > 0L
                ? String.format(java.util.Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
                : String.format(java.util.Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    private static long currentPlayTime(Minecraft minecraft) {
        long playTime = DungeonHudState.playTimeTicks();
        if (minecraft.isPaused()) {
            if (!clockPaused) {
                pausedPlayTimeTicks = playTime;
                clockPaused = true;
            }
            return pausedPlayTimeTicks;
        }
        clockPaused = false;
        return playTime;
    }
}
