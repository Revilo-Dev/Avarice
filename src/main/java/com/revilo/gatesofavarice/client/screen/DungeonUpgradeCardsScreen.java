package com.revilo.gatesofavarice.client.screen;

import com.revilo.gatesofavarice.client.DungeonUpgradeClientState;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCard;
import com.revilo.gatesofavarice.network.SelectUpgradeCardPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

public class DungeonUpgradeCardsScreen extends Screen {
    private static final ResourceLocation EFFECT_CARD = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/effect-card.png");
    private static final ResourceLocation EFFECT_CARD_HOVERED = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/effect-card_hovered.png");
    private static final ResourceLocation DAMAGE_CARD = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/damage-card.png");
    private static final ResourceLocation DAMAGE_CARD_HOVERED = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/damage-card_hovered.png");
    private static final ResourceLocation STAT_CARD = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/stat-card.png");
    private static final ResourceLocation STAT_CARD_HOVERED = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/stat-card_hovered.png");
    private static final ResourceLocation UPGRADE_CARD = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/upgrade-card.png");
    private static final ResourceLocation UPGRADE_CARD_HOVERED = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/upgrade-card_hovered.png");
    private static final ResourceLocation ICON_BASE = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/icons/");
    private static final int CARD_W = 76;
    private static final int CARD_H = 103;
    private static final int CARD_GAP = 3;

    private final List<Button> cardButtons = new ArrayList<>();
    private final List<Float> hoverScales = new ArrayList<>();
    private int centerX;
    private int top;

    public DungeonUpgradeCardsScreen() {
        super(Component.literal("Upgrade Cards"));
    }

    @Override
    protected void init() {
        this.cardButtons.clear();
        this.hoverScales.clear();
        this.centerX = this.width / 2;
        this.top = this.height / 2 - 98;
        int totalWidth = DungeonUpgradeClientState.cards.size() * CARD_W + Math.max(0, DungeonUpgradeClientState.cards.size() - 1) * CARD_GAP;
        int startX = this.centerX - totalWidth / 2;
        int cardY = this.top + 88;
        for (int i = 0; i < DungeonUpgradeClientState.cards.size(); i++) {
            int x = startX + i * (CARD_W + CARD_GAP);
            int idx = i;
            this.cardButtons.add(this.addRenderableWidget(new CardButton(x, cardY, CARD_W, CARD_H, b -> choose(idx))));
            this.hoverScales.add(1.0F);
        }
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gg, mouseX, mouseY, partialTick);
        super.render(gg, mouseX, mouseY, partialTick);
        gg.drawCenteredString(this.font, Component.literal("UPGRADE - " + DungeonUpgradeClientState.categoryName).withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW), this.centerX, this.top + 4, 0xFFFFFF);
        gg.drawCenteredString(this.font, DungeonUpgradeClientState.previewStack.getHoverName().copy().withStyle(ChatFormatting.GOLD), this.centerX, this.top + 62, 0xF3D78A);

        int itemX = this.centerX;
        int itemY = this.top + 36;
        gg.pose().pushPose();
        gg.pose().translate(itemX, itemY, 0.0F);
        gg.pose().scale(2.6F, 2.6F, 1.0F);
        gg.renderFakeItem(DungeonUpgradeClientState.previewStack, -8, -8);
        gg.pose().popPose();
        if (mouseX >= itemX - 22 && mouseX <= itemX + 22 && mouseY >= itemY - 22 && mouseY <= itemY + 22) {
            gg.renderTooltip(this.font, DungeonUpgradeClientState.previewStack, mouseX, mouseY);
        }

        for (int i = 0; i < this.cardButtons.size() && i < DungeonUpgradeClientState.cards.size(); i++) {
            Button button = this.cardButtons.get(i);
            float scale = this.hoverScales.get(i);
            UpgradeCard card = DungeonUpgradeClientState.cards.get(i);
            ResourceLocation texture = resolveCardTexture(card, button.isHoveredOrFocused());
            drawCard(gg, texture, button.getX(), button.getY(), scale);
            renderCardContents(gg, button, card, scale);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        super.tick();
        for (int i = 0; i < this.cardButtons.size(); i++) {
            float current = this.hoverScales.get(i);
            float target = this.cardButtons.get(i).isHoveredOrFocused() ? 1.10F : 1.0F;
            this.hoverScales.set(i, Mth.lerp(0.38F, current, target));
        }
    }

    private void renderCardContents(GuiGraphics gg, Button button, UpgradeCard card, float scale) {
        float scaledW = CARD_W * scale;
        float scaledH = CARD_H * scale;
        float offsetX = (scaledW - CARD_W) / 2.0F;
        float offsetY = (scaledH - CARD_H) / 2.0F;
        gg.pose().pushPose();
        gg.pose().translate(button.getX() - offsetX, button.getY() - offsetY, 0.0F);
        gg.pose().scale(scale, scale, 1.0F);

        int center = CARD_W / 2;
        int rowY = 8;
        for (String line : wrap(card.title(), 14)) {
            drawScaledCentered(gg, line, center, rowY, 0.75F, 0xF3D78A);
            rowY += 8;
            if (rowY > 24) break;
        }

        ResourceLocation icon = resolveCardIcon(card);
        if (icon != null) {
            gg.blit(icon, center - 8, 30, 0, 0, 16, 16, 16, 16);
        }

        int detailY = 50;
        for (String line : wrap(card.changeLabel(), 16)) {
            drawScaledCentered(gg, line, center, detailY, 0.58F, 0xD7F0D9);
            detailY += 7;
            if (detailY > 58) break;
        }

        drawScaledCentered(gg, card.currentValue(), center, 68, 0.58F, 0xFFD5D5);
        drawScaledCentered(gg, "\u2192", center, 76, 0.60F, 0xE1B85A);
        drawScaledCentered(gg, card.newValue(), center, 85, 0.62F, 0xFFFFFF);
        gg.pose().popPose();
    }

    private void choose(int idx) {
        if (idx < 0 || idx >= DungeonUpgradeClientState.cards.size()) {
            return;
        }
        PacketDistributor.sendToServer(new SelectUpgradeCardPayload(DungeonUpgradeClientState.sessionId, DungeonUpgradeClientState.cards.get(idx).id()));
    }

    private static ResourceLocation resolveCardTexture(UpgradeCard card, boolean hovered) {
        return switch (card.title()) {
            case "Effect Card" -> hovered ? EFFECT_CARD_HOVERED : EFFECT_CARD;
            case "Damage Card", "Offence Card" -> hovered ? DAMAGE_CARD_HOVERED : DAMAGE_CARD;
            case "Stat Card" -> hovered ? STAT_CARD_HOVERED : STAT_CARD;
            default -> hovered ? UPGRADE_CARD_HOVERED : UPGRADE_CARD;
        };
    }

    private static ResourceLocation resolveCardIcon(UpgradeCard card) {
        String iconName = switch (card.changeLabel()) {
            case "Toxic" -> "poison_chance";
            case "Fire Aspect" -> "flame";
            case "Withering" -> "withering_chance";
            case "Bleeding" -> "bleeding_chance";
            case "Stunning" -> "stun_chance";
            case "Shocking" -> "shocking_chance";
            case "Leeching" -> "leeching_chance";
            case "Freezing" -> "freezing_chance";
            case "Fangs" -> "fangs";
            case "Health Boost" -> "health";
            case "Toughness" -> "toughness";
            case "Leaping" -> "jump_height";
            case "Ability Power" -> "power";
            case "Movement Speed" -> "movement_speed";
            case "Resistance" -> "resistance";
            case "Fire Resistance" -> "fire_resistance";
            case "Projectile Resistance" -> "projectile_resistance";
            case "Blast Resistance" -> "blast_resistance";
            case "Attack Damage" -> "attack_damage";
            case "Undead Damage" -> "undead_damage";
            case "Attack Range" -> "attack_range";
            case "Attack Speed" -> "attack_speed";
            case "Sweeping Range" -> "sweeping_range";
            case "Aegis" -> "aegis";
            case "Stone Skin" -> "stone_skin";
            default -> inferIconName(card.changeLabel());
        };
        return ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/icons/" + iconName + ".png");
    }

    private static String inferIconName(String label) {
        return switch (label) {
            case "supply" -> "capacity";
            case "current" -> "power";
            default -> label.toLowerCase(Locale.ROOT).replace(' ', '_');
        };
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
