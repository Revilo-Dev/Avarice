package com.revilo.gatesofavarice.client.screen;

import com.revilo.gatesofavarice.client.DungeonUpgradeClientState;
import com.revilo.gatesofavarice.network.SelectUpgradeCategoryPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

public class DungeonUpgradeCategoryScreen extends Screen {
    private static final ResourceLocation CARD = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/upgrade-card.png");
    private static final ResourceLocation CARD_HOVERED = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/upgrade-card_hovered.png");
    private static final int CARD_W = 76;
    private static final int CARD_H = 103;
    private static final int CARD_GAP = 6;

    private final List<Button> categoryButtons = new ArrayList<>();
    private final List<Float> hoverScales = new ArrayList<>();

    public DungeonUpgradeCategoryScreen() {
        super(Component.literal("Upgrade Categories"));
    }

    @Override
    protected void init() {
        this.categoryButtons.clear();
        this.hoverScales.clear();
        int totalWidth = 4 * CARD_W + 3 * CARD_GAP;
        int startX = this.width / 2 - totalWidth / 2;
        int y = this.height / 2 - 10;
        addCategoryButton(startX, y, 0);
        addCategoryButton(startX + CARD_W + CARD_GAP, y, 1);
        addCategoryButton(startX + (CARD_W + CARD_GAP) * 2, y, 2);
        addCategoryButton(startX + (CARD_W + CARD_GAP) * 3, y, 3);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gg, mouseX, mouseY, partialTick);
        super.render(gg, mouseX, mouseY, partialTick);
        gg.drawCenteredString(this.font, Component.literal("CHOOSE UPGRADE CATEGORY").withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW), this.width / 2, this.height / 2 - 102, 0xFFFFFF);
        gg.drawCenteredString(this.font, Component.literal("Loadout: " + DungeonUpgradeClientState.loadoutName), this.width / 2, this.height / 2 - 86, 0xE8D6A2);
        gg.drawCenteredString(this.font, Component.literal("Theme: " + DungeonUpgradeClientState.theme), this.width / 2, this.height / 2 - 74, 0xB5B5B5);

        for (int i = 0; i < this.categoryButtons.size(); i++) {
            Button button = this.categoryButtons.get(i);
            float scale = this.hoverScales.get(i);
            ResourceLocation texture = button.isHoveredOrFocused() ? CARD_HOVERED : CARD;
            drawCard(gg, texture, button.getX(), button.getY(), scale);
            renderCategoryContents(gg, button, i, scale);
        }
    }

    @Override
    public void tick() {
        super.tick();
        for (int i = 0; i < this.categoryButtons.size(); i++) {
            float current = this.hoverScales.get(i);
            float target = this.categoryButtons.get(i).isHoveredOrFocused() ? 1.10F : 1.0F;
            this.hoverScales.set(i, Mth.lerp(0.38F, current, target));
        }
    }

    private void addCategoryButton(int x, int y, int ordinal) {
        this.categoryButtons.add(this.addRenderableWidget(new CardButton(x, y, CARD_W, CARD_H, b -> select(ordinal))));
        this.hoverScales.add(1.0F);
    }

    private void renderCategoryContents(GuiGraphics gg, Button button, int ordinal, float scale) {
        float scaledW = CARD_W * scale;
        float scaledH = CARD_H * scale;
        float offsetX = (scaledW - CARD_W) / 2.0F;
        float offsetY = (scaledH - CARD_H) / 2.0F;
        gg.pose().pushPose();
        gg.pose().translate(button.getX() - offsetX, button.getY() - offsetY, 0.0F);
        gg.pose().scale(scale, scale, 1.0F);

        int center = CARD_W / 2;
        int rowY = 14;
        for (String line : wrap(categoryTitle(ordinal), 12)) {
            drawScaledCentered(gg, line, center, rowY, 0.72F, 0xF3D78A);
            rowY += 9;
        }

        drawScaledCentered(gg, categorySubtitle(ordinal), center, 62, 0.58F, 0xD7F0D9);
        drawScaledCentered(gg, "Select", center, 84, 0.62F, 0xFFFFFF);
        gg.pose().popPose();
    }

    private static String categoryTitle(int ordinal) {
        return switch (ordinal) {
            case 0 -> "Primary Weapon";
            case 1 -> "Secondary Weapon";
            case 2 -> "Armor";
            default -> "Item";
        };
    }

    private static String categorySubtitle(int ordinal) {
        return switch (ordinal) {
            case 0 -> "3 effect, 1 dmg";
            case 1 -> "3 effect, 1 dmg";
            case 2 -> "2 effect, 2 stat";
            default -> "utility";
        };
    }

    private void select(int ordinal) {
        PacketDistributor.sendToServer(new SelectUpgradeCategoryPayload(DungeonUpgradeClientState.sessionId, ordinal));
    }

    private void drawCard(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, float scale) {
        if (scale == 1.0F) {
            guiGraphics.blit(texture, x, y, 0, 0, CARD_W, CARD_H, CARD_W, CARD_H);
            return;
        }
        float scaledW = CARD_W * scale;
        float scaledH = CARD_H * scale;
        float offsetX = (scaledW - CARD_W) / 2.0F;
        float offsetY = (scaledH - CARD_H) / 2.0F;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x - offsetX, y - offsetY, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.blit(texture, 0, 0, 0, 0, CARD_W, CARD_H, CARD_W, CARD_H);
        guiGraphics.pose().popPose();
    }

    private void drawScaledCentered(GuiGraphics gg, String text, int x, int y, float scale, int color) {
        int w = this.font.width(text);
        gg.pose().pushPose();
        gg.pose().translate(x - (w * scale) / 2.0F, y, 0.0F);
        gg.pose().scale(scale, scale, 1.0F);
        gg.drawString(this.font, text, 0, 0, color, false);
        gg.pose().popPose();
    }

    private List<String> wrap(String input, int max) {
        List<String> out = new ArrayList<>();
        String[] parts = input.split(" ");
        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            if (current.isEmpty()) {
                current.append(part);
            } else if (current.length() + 1 + part.length() <= max) {
                current.append(" ").append(part);
            } else {
                out.add(current.toString());
                current = new StringBuilder(part);
            }
        }
        if (!current.isEmpty()) {
            out.add(current.toString());
        }
        return out;
    }

    private static final class CardButton extends Button {
        private CardButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        }
    }
}
