package com.revilo.gatesofavarice.client;

import com.revilo.gatesofavarice.dungeon.DungeonHudState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
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
    private static final int SIDEBAR_PADDING = 8;

    private DungeonWaveHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        boolean tabDown = isTabDown(minecraft);
        if (DungeonHudState.inRun()) {
            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();
            if (tabDown) {
                DungeonDeckRenderer.renderFullDeck(guiGraphics, minecraft.font, DungeonHudState.deck(),
                        screenWidth / 2, screenHeight - 8, screenWidth - 24);
                renderPartySidebar(guiGraphics, minecraft);
            } else {
                DungeonDeckRenderer.renderCompact(guiGraphics, minecraft.font, DungeonHudState.deck(),
                        screenWidth - 8, screenHeight - 8);
            }
        }

        if (!DungeonHudState.active()) {
            return;
        }

        renderAmmo(guiGraphics, minecraft, tabDown);

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = 8;

        int total = Math.max(1, DungeonHudState.totalMobs());
        int remaining = Mth.clamp(DungeonHudState.mobsRemaining(), 0, total);
        int filled = Math.round(BAR_WIDTH * ((total - remaining) / (float) total));

        guiGraphics.blit(BAR_TEXTURE, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        if (filled > 0) {
            guiGraphics.enableScissor(x, y, x + filled, y + BAR_HEIGHT);
            guiGraphics.blit(PROGRESS_TEXTURE, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
            guiGraphics.disableScissor();
        }

        int countdownTicks = DungeonHudState.nextWaveCountdownTicks();
        Component waveLabel = DungeonHudState.gatewayOpen()
                ? Component.literal("A gateway has opened to the next floor")
                : DungeonHudState.upgradePhase()
                ? Component.literal("Upgrade phase")
                : Component.literal("Floor " + DungeonHudState.floorNumber() + "  Wave " + DungeonHudState.waveInFloor());
        int waveTextAreaWidth = WAVE_TEXT_RIGHT - WAVE_TEXT_LEFT;
        int waveTextX = x + WAVE_TEXT_LEFT + (waveTextAreaWidth - minecraft.font.width(waveLabel)) / 2;
        guiGraphics.drawString(minecraft.font, waveLabel, waveTextX, y + WAVE_TEXT_TOP, 0xFFFFFF, false);

        if (countdownTicks > 0) {
            int seconds = Mth.ceil(countdownTicks / 20.0F);
            Component countdownLabel = Component.literal("Next wave in " + seconds + "s");
            int countdownWidth = minecraft.font.width(countdownLabel);
            guiGraphics.drawString(minecraft.font, countdownLabel, x + (BAR_WIDTH - countdownWidth) / 2, y + BAR_HEIGHT + 4, 0xFFE36B, false);
        } else if (!DungeonHudState.upgradePhase() && !DungeonHudState.gatewayOpen()) {
            Component mobsLabel = Component.literal(remaining + " mobs remaining");
            int mobsLabelWidth = minecraft.font.width(mobsLabel);
            guiGraphics.drawString(minecraft.font, mobsLabel, x + (BAR_WIDTH - mobsLabelWidth) / 2, y + BAR_HEIGHT + 4, 0xFFFFFF, false);
        }
    }

    private static void renderAmmo(GuiGraphics guiGraphics, Minecraft minecraft, boolean expandedDeck) {
        if (DungeonHudState.ammoCount() <= 0) return;
        ItemStack ammo = new ItemStack(BuiltInRegistries.ITEM.get(DungeonHudState.ammoItem()));
        if (ammo.isEmpty()) return;
        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int y = expandedDeck
                ? minecraft.getWindow().getGuiScaledHeight() - 22
                - DungeonDeckRenderer.fullDeckHeight(minecraft.getWindow().getGuiScaledWidth() - 24)
                : minecraft.getWindow().getGuiScaledHeight() - 48;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX - 5, y, 0.0F);
        guiGraphics.pose().scale(0.625F, 0.625F, 1.0F);
        guiGraphics.renderItem(ammo, 0, 0);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX + 7, y + 2, 0.0F);
        guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
        guiGraphics.drawString(minecraft.font, Component.literal("x" + DungeonHudState.ammoCount()), 0, 0, 0xFFFFFF, true);
        guiGraphics.pose().popPose();
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

}
