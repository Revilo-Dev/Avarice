package com.revilo.gatesofavarice.client.screen;

import com.revilo.gatesofavarice.client.DungeonUpgradeClientState;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCard;
import com.revilo.gatesofavarice.network.RerollUpgradeCardsPayload;
import com.revilo.gatesofavarice.network.SelectUpgradeCardPayload;
import com.revilo.gatesofavarice.network.SyncUpgradeCardsPayload;
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
import net.minecraft.world.item.ItemStack;
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
    private static final float TRANSITION_STEP = 0.11F;
    private static final int CARD_W = 76;
    private static final int CARD_H = 103;
    private static final int CARD_GAP = 3;

    private final List<Button> cardButtons = new ArrayList<>();
    private final List<Float> hoverScales = new ArrayList<>();
    private List<UpgradeCard> cards = List.of();
    private ItemStack previewStack = ItemStack.EMPTY;
    private String sessionId = "";
    private String categoryName = "";
    private int rerollsLeft = 0;
    private int rerollCost = 0;
    private int selectedCardCount = 0;
    private int maxCardSelections = 5;
    private Button rerollButton;
    private int centerX;
    private int top;
    private boolean animatingOut = false;
    private boolean animatingIn = true;
    private boolean awaitingRerollSync = false;
    private boolean pendingSync = false;
    private float transitionProgress = 1.0F;

    public DungeonUpgradeCardsScreen() {
        super(Component.literal("Upgrade Cards"));
        loadFromClientState();
    }

    @Override
    protected void init() {
        this.centerX = this.width / 2;
        this.top = this.height / 2 - 98;
        rebuildCardWidgets();
        updateButtonStates();
    }

    public void applySyncPayload(SyncUpgradeCardsPayload payload) {
        if (!payload.sessionId().equals(this.sessionId)) {
            return;
        }
        this.pendingSync = true;
        if (!this.animatingOut) {
            applyPendingSync();
        }
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gg, mouseX, mouseY, partialTick);
        super.render(gg, mouseX, mouseY, partialTick);
        gg.drawCenteredString(this.font, Component.literal("UPGRADE - " + formatCategoryName(this.categoryName)).withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW), this.centerX, this.top + 4, 0xFFFFFF);
        gg.drawCenteredString(this.font, Component.literal("Select " + Math.max(0, this.maxCardSelections - this.selectedCardCount) + " Cards").withStyle(ChatFormatting.LIGHT_PURPLE), this.centerX, this.top + 16, 0xFFFFFF);
        if (!this.previewStack.isEmpty()) {
            gg.drawCenteredString(this.font, this.previewStack.getHoverName().copy().withStyle(ChatFormatting.GOLD), this.centerX, this.top + 62, 0xF3D78A);
            int itemX = this.centerX;
            int itemY = this.top + 36;
            gg.pose().pushPose();
            gg.pose().translate(itemX, itemY, 0.0F);
            gg.pose().scale(2.6F, 2.6F, 1.0F);
            gg.renderFakeItem(this.previewStack, -8, -8);
            gg.pose().popPose();
            if (mouseX >= itemX - 22 && mouseX <= itemX + 22 && mouseY >= itemY - 22 && mouseY <= itemY + 22) {
                gg.renderTooltip(this.font, this.previewStack, mouseX, mouseY);
            }
        }

        for (int i = 0; i < this.cardButtons.size() && i < this.cards.size(); i++) {
            Button button = this.cardButtons.get(i);
            float scale = this.hoverScales.get(i);
            UpgradeCard card = this.cards.get(i);
            int drawX = animatedCardX(button.getX(), i);
            int drawY = animatedCardY(button.getY(), i);
            ResourceLocation texture = resolveCardTexture(card, button.isHoveredOrFocused());
            drawCard(gg, texture, drawX, drawY, scale);
            renderCardContents(gg, drawX, drawY, card, scale);
        }
    }

    @Override
    public void tick() {
        super.tick();
        for (int i = 0; i < this.cardButtons.size(); i++) {
            float current = this.hoverScales.get(i);
            float target = this.cardButtons.get(i).isHoveredOrFocused() && !isTransitioning() ? 1.10F : 1.0F;
            this.hoverScales.set(i, Mth.lerp(0.38F, current, target));
        }
        if (this.animatingOut) {
            this.transitionProgress = Math.min(1.0F, this.transitionProgress + TRANSITION_STEP);
            if (this.transitionProgress >= 1.0F && this.pendingSync) {
                this.animatingOut = false;
                applyPendingSync();
            }
        } else if (this.animatingIn) {
            this.transitionProgress = Math.max(0.0F, this.transitionProgress - TRANSITION_STEP);
            if (this.transitionProgress <= 0.0F) {
                this.animatingIn = false;
            }
        }
        updateButtonStates();
    }

    private void renderCardContents(GuiGraphics gg, int x, int y, UpgradeCard card, float scale) {
        float scaledW = CARD_W * scale;
        float scaledH = CARD_H * scale;
        float offsetX = (scaledW - CARD_W) / 2.0F;
        float offsetY = (scaledH - CARD_H) / 2.0F;
        gg.pose().pushPose();
        gg.pose().translate(x - offsetX, y - offsetY, 0.0F);
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
        if (isTransitioning() || idx < 0 || idx >= this.cards.size()) {
            return;
        }
        PacketDistributor.sendToServer(new SelectUpgradeCardPayload(this.sessionId, this.cards.get(idx).id()));
    }

    private void reroll() {
        if (this.rerollsLeft <= 0 || isTransitioning()) {
            return;
        }
        this.awaitingRerollSync = true;
        this.pendingSync = false;
        this.animatingOut = true;
        this.animatingIn = false;
        this.transitionProgress = 0.0F;
        updateButtonStates();
        PacketDistributor.sendToServer(new RerollUpgradeCardsPayload(this.sessionId));
    }

    private void loadFromClientState() {
        this.sessionId = DungeonUpgradeClientState.sessionId;
        this.categoryName = DungeonUpgradeClientState.categoryName;
        this.previewStack = DungeonUpgradeClientState.previewStack.copy();
        this.cards = List.copyOf(DungeonUpgradeClientState.cards);
        this.rerollsLeft = DungeonUpgradeClientState.rerollsLeft;
        this.rerollCost = DungeonUpgradeClientState.rerollCost;
        this.selectedCardCount = DungeonUpgradeClientState.selectedCardCount;
        this.maxCardSelections = Math.max(0, DungeonUpgradeClientState.maxCardSelections);
    }

    private void rebuildCardWidgets() {
        clearWidgets();
        this.cardButtons.clear();
        this.hoverScales.clear();
        int totalWidth = this.cards.size() * CARD_W + Math.max(0, this.cards.size() - 1) * CARD_GAP;
        int startX = this.centerX - totalWidth / 2;
        int cardY = this.top + 88;
        for (int i = 0; i < this.cards.size(); i++) {
            int x = startX + i * (CARD_W + CARD_GAP);
            int idx = i;
            this.cardButtons.add(this.addRenderableWidget(new CardButton(x, cardY, CARD_W, CARD_H, b -> choose(idx))));
            this.hoverScales.add(1.0F);
        }
        this.rerollButton = this.addRenderableWidget(Button.builder(rerollLabel(), b -> reroll())
                .pos(this.centerX - 112, cardY + CARD_H + 10)
                .size(224, 20)
                .build());
    }

    private void updateButtonStates() {
        boolean active = !isTransitioning() && !this.awaitingRerollSync;
        for (Button button : this.cardButtons) {
            button.active = active;
            button.visible = true;
        }
        if (this.rerollButton != null) {
            this.rerollButton.setMessage(rerollLabel());
            this.rerollButton.visible = this.rerollsLeft > 0;
            this.rerollButton.active = active && this.rerollsLeft > 0;
        }
    }

    private void applyPendingSync() {
        loadFromClientState();
        this.awaitingRerollSync = false;
        this.pendingSync = false;
        rebuildCardWidgets();
        this.animatingIn = true;
        this.animatingOut = false;
        this.transitionProgress = 1.0F;
        updateButtonStates();
    }

    private boolean isTransitioning() {
        return this.animatingOut || this.animatingIn;
    }

    private int animatedCardX(int baseX, int index) {
        if (!isTransitioning()) {
            return baseX;
        }
        float progress = transitionAmount();
        int centerCardX = this.centerX - CARD_W / 2;
        float inward = easeInOutCubic(Math.min(progress / 0.58F, 1.0F));
        float spread = easeOutCubic(Math.max(0.0F, (progress - 0.34F) / 0.66F));
        int centerAligned = Mth.floor(Mth.lerp(inward, baseX, centerCardX));
        if (this.animatingOut) {
            return centerAligned;
        }
        return Mth.floor(Mth.lerp(spread, centerCardX, baseX));
    }

    private int animatedCardY(int baseY, int index) {
        if (!isTransitioning()) {
            return baseY;
        }
        int bottomY = this.height + CARD_H + 24;
        float progress = transitionAmount();
        float inward = easeInOutCubic(Math.min(progress / 0.58F, 1.0F));
        float drop = this.animatingOut
                ? easeInCubic(Math.max(0.0F, (progress - 0.28F) / 0.72F))
                : easeOutCubic(Math.max(0.0F, (progress - 0.28F) / 0.72F));
        int centeredY = Mth.floor(Mth.lerp(inward, baseY, baseY - 8));
        if (this.animatingOut) {
            return Mth.floor(Mth.lerp(drop, centeredY, bottomY));
        }
        return Mth.floor(Mth.lerp(drop, bottomY, centeredY));
    }

    private float transitionAmount() {
        return this.animatingOut ? this.transitionProgress : 1.0F - this.transitionProgress;
    }

    private static float easeOutCubic(float t) {
        return 1.0F - (float) Math.pow(1.0F - t, 3.0F);
    }

    private static float easeInCubic(float t) {
        return t * t * t;
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5F
                ? 4.0F * t * t * t
                : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 3.0F) / 2.0F;
    }

    private Component rerollLabel() {
        return Component.literal("Reroll (" + this.rerollsLeft + ") - " + this.rerollCost + " Mythic Coins").withStyle(ChatFormatting.GOLD);
    }

    private static ResourceLocation resolveCardTexture(UpgradeCard card, boolean hovered) {
        return switch (card.title()) {
            case "Effect Card" -> hovered ? EFFECT_CARD_HOVERED : EFFECT_CARD;
            case "Damage Card", "Offence Card" -> hovered ? DAMAGE_CARD_HOVERED : DAMAGE_CARD;
            case "Stat Card" -> hovered ? STAT_CARD_HOVERED : STAT_CARD;
            case "Ability Card" -> hovered ? EFFECT_CARD_HOVERED : EFFECT_CARD;
            case "Restock Card" -> hovered ? STAT_CARD_HOVERED : STAT_CARD;
            case "Reroll Primary Weapon" -> hovered ? ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/tarrot-card-hovered.png") : ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/tarrot-card.png");
            case "Reroll Secondary Weapon" -> hovered ? DAMAGE_CARD_HOVERED : DAMAGE_CARD;
            default -> hovered ? UPGRADE_CARD_HOVERED : UPGRADE_CARD;
        };
    }

    private static ResourceLocation resolveCardIcon(UpgradeCard card) {
        String iconName = switch (card.changeLabel()) {
            case "Restock" -> "capacity";
            case "Magnet", "Arcane Apples + Magnet" -> "movement_speed";
            case "Primary" -> "attack_damage";
            case "Secondary" -> "undead_damage";
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
            case "Thorns" -> "health";
            case "Stone Skin" -> "stone_skin";
            default -> card.title().equals("Food Card") ? "health" : inferIconName(card.changeLabel());
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

    private static String formatCategoryName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = raw.toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder formatted = new StringBuilder(normalized.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (capitalizeNext && Character.isLetter(c)) {
                formatted.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                formatted.append(c);
            }
            if (c == ' ') {
                capitalizeNext = true;
            }
        }
        return formatted.toString();
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
