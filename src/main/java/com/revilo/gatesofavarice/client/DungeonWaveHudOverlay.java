package com.revilo.gatesofavarice.client;

import com.revilo.gatesofavarice.dungeon.DungeonHudState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;

public final class DungeonWaveHudOverlay {
    private static final ResourceLocation BAR_TEXTURE = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/levelbar/bar.png");
    private static final ResourceLocation PROGRESS_TEXTURE = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/levelbar/progress.png");
    private static final int BAR_WIDTH = 200;
    private static final int BAR_HEIGHT = 20;
    private static final int WAVE_TEXT_LEFT = 62;
    private static final int WAVE_TEXT_TOP = 12;
    private static final int WAVE_TEXT_RIGHT = 139;
    private static final int WAVE_TEXT_BOTTOM = 18;
    private static final int SIDEBAR_WIDTH = 158;
    private static final int SIDEBAR_PADDING = 8;

    private DungeonWaveHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }

        if (DungeonHudState.hasRunStats() && isTabDown(minecraft)) {
            renderStatsSidebar(event.getGuiGraphics(), minecraft);
            renderPartySidebar(event.getGuiGraphics(), minecraft);
        }

        if (!DungeonHudState.active()) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = 8;

        int total = Math.max(1, DungeonHudState.totalMobs());
        int remaining = Mth.clamp(DungeonHudState.mobsRemaining(), 0, total);
        int filled = Math.round(BAR_WIDTH * ((total - remaining) / (float) total));

        event.getGuiGraphics().blit(BAR_TEXTURE, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        if (filled > 0) {
            event.getGuiGraphics().enableScissor(x, y, x + filled, y + BAR_HEIGHT);
            event.getGuiGraphics().blit(PROGRESS_TEXTURE, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
            event.getGuiGraphics().disableScissor();
        }

        int countdownTicks = DungeonHudState.nextWaveCountdownTicks();
        Component waveLabel = DungeonHudState.gatewayOpen()
                ? Component.literal("A gateway has opened to the next floor")
                : DungeonHudState.upgradePhase()
                ? Component.literal("Upgrade phase")
                : Component.literal("Floor " + DungeonHudState.floorNumber() + "  Wave " + DungeonHudState.waveInFloor());
        int waveTextAreaWidth = WAVE_TEXT_RIGHT - WAVE_TEXT_LEFT;
        int waveTextX = x + WAVE_TEXT_LEFT + (waveTextAreaWidth - minecraft.font.width(waveLabel)) / 2;
        event.getGuiGraphics().drawString(minecraft.font, waveLabel, waveTextX, y + WAVE_TEXT_TOP, 0xFFFFFF, false);

        if (countdownTicks > 0) {
            int seconds = Mth.ceil(countdownTicks / 20.0F);
            Component countdownLabel = Component.literal("Next wave in " + seconds + "s");
            int countdownWidth = minecraft.font.width(countdownLabel);
            event.getGuiGraphics().drawString(minecraft.font, countdownLabel, x + (BAR_WIDTH - countdownWidth) / 2, y + BAR_HEIGHT + 4, 0xFFE36B, false);
        } else if (!DungeonHudState.upgradePhase() && !DungeonHudState.gatewayOpen()) {
            Component mobsLabel = Component.literal(remaining + " mobs remaining");
            int mobsLabelWidth = minecraft.font.width(mobsLabel);
            event.getGuiGraphics().drawString(minecraft.font, mobsLabel, x + (BAR_WIDTH - mobsLabelWidth) / 2, y + BAR_HEIGHT + 4, 0xFFFFFF, false);
        }
    }

    private static void renderStatsSidebar(GuiGraphics guiGraphics, Minecraft minecraft) {
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int x = screenWidth - SIDEBAR_WIDTH + 12;
        int y = 40;
        int lineHeight = 11;
        int statCount = Math.max(1, DungeonHudState.statLines().size());
        int height = Math.min(screenHeight - y - 8, SIDEBAR_PADDING * 2 + 12 + lineHeight * (4 + statCount));

        guiGraphics.fill(x, y, x + SIDEBAR_WIDTH, y + height, 0xD0101010);
        guiGraphics.fill(x, y, x + SIDEBAR_WIDTH, y + 1, 0x80E0B85A);
        guiGraphics.fill(x, y + height - 1, x + SIDEBAR_WIDTH, y + height, 0x803F3320);

        int textX = x + SIDEBAR_PADDING;
        int textY = y + SIDEBAR_PADDING;
        guiGraphics.drawString(minecraft.font, Component.literal("Play Time: " + formatTime(DungeonHudState.playTimeTicks())), textX, textY, 0xFFFFFF, false);
        textY += lineHeight;
        guiGraphics.drawString(minecraft.font, Component.literal("Mob Kills: " + DungeonHudState.mobsKilled()), textX, textY, 0xFFFFFF, false);
        textY += lineHeight + 4;
        guiGraphics.drawString(minecraft.font, Component.literal("Modified Stats"), textX, textY, 0xFFE36B, false);
        textY += lineHeight;

        if (DungeonHudState.statLines().isEmpty()) {
            guiGraphics.drawString(minecraft.font, Component.literal("No modifiers yet"), textX, textY, 0xA8A8A8, false);
            return;
        }

        int bottom = y + height - SIDEBAR_PADDING;
        for (String statLine : DungeonHudState.statLines()) {
            if (textY + 9 > bottom) {
                break;
            }
            guiGraphics.drawString(minecraft.font, Component.literal(statLine), textX, textY, statColor(statLine), false);
            textY += lineHeight;
        }
    }

    private static void renderPartySidebar(GuiGraphics guiGraphics, Minecraft minecraft) {
        if (DungeonHudState.partyMembers().isEmpty()) return;
        int x = -3;
        int y = 40;
        int lineHeight = 11;
        int width = 174;
        int height = SIDEBAR_PADDING * 2 + 14 + lineHeight * DungeonHudState.partyMembers().size();
        guiGraphics.fill(x, y, x + width, y + height, 0xD0101010);
        guiGraphics.fill(x, y, x + width, y + 1, 0x806EC8E8);
        int textY = y + SIDEBAR_PADDING;
        guiGraphics.drawString(minecraft.font, Component.literal("Party: " + DungeonHudState.partyName()), x + SIDEBAR_PADDING, textY, 0xFF8FE9FF, false);
        textY += 14;
        for (String encoded : DungeonHudState.partyMembers()) {
            String[] member = encoded.split("\\|", 3);
            String name = member.length > 0 ? member[0] : "Unknown";
            String state = member.length > 1 ? member[1] : "OFFLINE";
            String health = member.length > 2 ? member[2] : "0";
            int color = "OFFLINE".equals(state) ? 0xFF777777 : ("IN DUNGEON".equals(state) ? 0xFF8DFF9D : 0xFFFFE28A);
            guiGraphics.drawString(minecraft.font, Component.literal(name + "  " + state + "  ♥" + health), x + SIDEBAR_PADDING, textY, color, false);
            textY += lineHeight;
        }
    }

    private static boolean isTabDown(Minecraft minecraft) {
        return GLFW.glfwGetKey(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_TAB) == GLFW.GLFW_PRESS;
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        DungeonHudState.clear();
    }

    private static String formatTime(long ticks) {
        long totalSeconds = Math.max(0L, ticks / 20L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes + ":" + String.format(java.util.Locale.ROOT, "%02d", seconds);
    }

    private static int statColor(String statLine) {
        String normalized = statLine.toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("mob ") || normalized.startsWith("elite ")) {
            return 0xFFD5D5;
        }
        return 0xD7F0D9;
    }
}
