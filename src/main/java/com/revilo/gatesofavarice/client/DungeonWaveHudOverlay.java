package com.revilo.gatesofavarice.client;

import com.revilo.gatesofavarice.dungeon.DungeonHudState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class DungeonWaveHudOverlay {
    private static final ResourceLocation BAR_TEXTURE = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/levelbar/bar.png");
    private static final ResourceLocation PROGRESS_TEXTURE = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/levelbar/progress.png");
    private static final int BAR_WIDTH = 200;
    private static final int BAR_HEIGHT = 20;
    private static final int WAVE_TEXT_LEFT = 62;
    private static final int WAVE_TEXT_TOP = 12;
    private static final int WAVE_TEXT_RIGHT = 139;
    private static final int WAVE_TEXT_BOTTOM = 18;

    private DungeonWaveHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui || !DungeonHudState.active()) {
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

        Component waveLabel = Component.literal("Wave " + DungeonHudState.waveNumber());
        int waveTextAreaWidth = WAVE_TEXT_RIGHT - WAVE_TEXT_LEFT;
        int waveTextX = x + WAVE_TEXT_LEFT + (waveTextAreaWidth - minecraft.font.width(waveLabel)) / 2;
        event.getGuiGraphics().drawString(minecraft.font, waveLabel, waveTextX, y + WAVE_TEXT_TOP, 0xFFFFFF, false);

        Component mobsLabel = Component.literal(remaining + " mobs remaining");
        int mobsLabelWidth = minecraft.font.width(mobsLabel);
        event.getGuiGraphics().drawString(minecraft.font, mobsLabel, x + (BAR_WIDTH - mobsLabelWidth) / 2, y + BAR_HEIGHT + 4, 0xFFFFFF, false);
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        DungeonHudState.clear();
    }
}
