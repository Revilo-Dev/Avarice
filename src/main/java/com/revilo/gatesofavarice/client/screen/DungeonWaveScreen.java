package com.revilo.gatesofavarice.client.screen;

import com.revilo.gatesofavarice.client.DungeonDeckRenderer;
import com.revilo.gatesofavarice.dungeon.DungeonDeck;
import com.revilo.gatesofavarice.dungeon.DungeonDeck.CardState;
import com.revilo.gatesofavarice.dungeon.DungeonDeck.CardType;
import com.revilo.gatesofavarice.menu.DungeonWaveMenu;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class DungeonWaveScreen extends AbstractContainerScreen<DungeonWaveMenu> {

    private static final ResourceLocation BOOSTER_BACKGROUND = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/boosterpack/booster_background.png");
    private static final ResourceLocation BOOSTER_BACKGROUND_HOVERED = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/boosterpack/booster_background_hover.png");
    private static final ResourceLocation BOOSTER_TEAR = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/boosterpack/booster_tear-background.png");
    private static final List<ResourceLocation> BOOSTER_FOREGROUNDS = List.of(
            ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/boosterpack/booster_basic-foreground.png"),
            ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/boosterpack/booster_normal-foreground.png"),
            ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/boosterpack/booster_hard-foreground.png"),
            ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/boosterpack/booster_challenging-foreground.png"));
    private static final ResourceLocation UPGRADE_CARD = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/upgrade-card.png");
    private static final ResourceLocation UPGRADE_CARD_HOVERED = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/upgrade-card_hovered.png");
    private static final ResourceLocation ITEM_CARD = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/item-card.png");
    private static final ResourceLocation ITEM_CARD_HOVERED = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/item-card_hovered.png");
    private static final ResourceLocation DICE_ICON = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/icon/dice.png");
    private static final int CARD_W = 76;
    private static final int CARD_H = 103;
    private static final int BOOSTER_H = 102;
    private static final int CARD_GAP = 3;
    private static final int BOOSTER_FOCUS_TICKS = 14;
    private static final int BOOSTER_TEAR_TICKS = 10;
    private static final int BOOSTER_REVEAL_TICKS = 20;
    private static final int BOOSTER_REVEAL_STAGGER_TICKS = 2;
    private static final int BOOSTER_HOLD_TICKS = 40;

    private final List<Button> optionButtons = new ArrayList<>();
    private Button rerollButton;
    private Button skipButton;
    private final List<Integer> baseCardX = new ArrayList<>();
    private final List<Integer> baseCardY = new ArrayList<>();
    private final List<Integer> animatedCardX = new ArrayList<>();
    private final List<Integer> animatedCardY = new ArrayList<>();
    private final List<Float> hoverScales = new ArrayList<>();
    private AnimationState animationState = AnimationState.APPEARING;
    private int animationTick = 0;
    private int settleHoldTicks = 0;
    private int selectedCard = -1;
    private int pendingClickButtonId = Integer.MIN_VALUE;
    private final List<BoosterParticle> boosterParticles = new ArrayList<>();

    public DungeonWaveScreen(DungeonWaveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 340;
        this.imageHeight = 230;
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        this.optionButtons.clear();
        this.baseCardX.clear();
        this.baseCardY.clear();
        this.animatedCardX.clear();
        this.animatedCardY.clear();
        this.hoverScales.clear();
        this.animationState = AnimationState.APPEARING;
        this.animationTick = 0;
        this.selectedCard = -1;
        this.pendingClickButtonId = Integer.MIN_VALUE;
        this.settleHoldTicks = 0;
        this.boosterParticles.clear();

        int optionCount = Math.max(1, this.menu.options().size());
        int totalWidth = optionCount * CARD_W + (optionCount - 1) * CARD_GAP;
        int x = this.leftPos + (this.imageWidth - totalWidth) / 2;
        int startY = this.menu.stage() == DungeonWaveMenu.STAGE_BOOSTER
                ? boosterSelectionY()
                : this.topPos + 34;

        for (int index = 0; index < this.menu.options().size(); index++) {
            final int optionIndex = index;
            Button button = this.addRenderableWidget(new CardButton(
                    x + index * (CARD_W + CARD_GAP),
                    startY,
                    CARD_W,
                    CARD_H,
                    click -> this.selectOption(optionIndex)));
            this.baseCardX.add(x + index * (CARD_W + CARD_GAP));
            this.baseCardY.add(startY);
            this.animatedCardX.add(this.baseCardX.get(index));
            this.animatedCardY.add(this.baseCardY.get(index));
            this.hoverScales.add(1.0F);
            button.active = false;
            this.optionButtons.add(button);
        }

        this.rerollButton = this.addRenderableWidget(Button.builder(
                        Component.literal("Reroll (" + this.menu.rerollsLeft() + ") - " + this.menu.rerollCost() + " Mythic Coins").withStyle(ChatFormatting.GOLD),
                        click -> this.selectReroll())
                .pos(this.leftPos + 58, this.topPos + 175)
                .size(224, 20)
                .build());
        this.rerollButton.visible = false;
        this.rerollButton.active = false;

        this.skipButton = this.addRenderableWidget(Button.builder(
                        Component.literal("Skip").withStyle(ChatFormatting.GRAY),
                        click -> this.selectSkip())
                .pos(this.leftPos + 58, this.topPos + 199)
                .size(224, 20)
                .build());
        this.skipButton.visible = this.menu.stage() == DungeonWaveMenu.STAGE_UPGRADE;
        this.skipButton.active = this.menu.ownerCanSelect() && this.menu.stage() == DungeonWaveMenu.STAGE_UPGRADE;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (this.menu.stage() == DungeonWaveMenu.STAGE_BOOSTER) {
            renderBoosterStage(guiGraphics, partialTick);
            return;
        }
        for (int i = 0; i < this.optionButtons.size(); i++) {
            Button button = this.optionButtons.get(i);
            boolean hovered = button.isHoveredOrFocused() && this.animationState == AnimationState.IDLE;
            ResourceLocation tex = resolveCardTexture(hovered);
            float scale = i < this.hoverScales.size() ? this.hoverScales.get(i) : 1.0F;
            drawCard(guiGraphics, tex, button.getX(), button.getY(), scale);
        }
    }

    private void renderBoosterStage(GuiGraphics guiGraphics, float partialTick) {
        DungeonWaveMenu.WaveOptionView selectedOption = selectedBoosterOption();
        boolean showingPull = selectedOption != null
                && (this.animationState == AnimationState.BOOSTER_REVEALING
                || this.animationState == AnimationState.BOOSTER_REVEALED);
        List<CardState> displayedDeck = showingPull
                ? DungeonDeck.withPulls(this.menu.deck(), selectedOption.pulledCardCounts())
                : this.menu.deck();
        DungeonDeckRenderer.renderFullDeck(guiGraphics, this.font, displayedDeck,
                this.width / 2, this.height - 8, Math.max(120, this.width - 24));

        if (showingPull) {
            renderPulledCards(guiGraphics, selectedOption);
        }

        for (int index = 0; index < this.optionButtons.size(); index++) {
            if ((this.animationState == AnimationState.BOOSTER_TEARING
                    || this.animationState == AnimationState.BOOSTER_REVEALING
                    || this.animationState == AnimationState.BOOSTER_REVEALED)
                    && index != this.selectedCard) {
                continue;
            }
            Button button = this.optionButtons.get(index);
            float scale = index < this.hoverScales.size() ? this.hoverScales.get(index) : 1.0F;
            if (scale <= 0.02F) continue;
            ResourceLocation foreground = boosterForeground(index);
            if (index == this.selectedCard && this.animationState == AnimationState.BOOSTER_TEARING) {
                int frame = Mth.clamp(this.animationTick, 0, 9);
                drawBoosterTear(guiGraphics, foreground, button.getX(), button.getY(), scale, frame);
            } else if (index == this.selectedCard && this.animationState == AnimationState.BOOSTER_REVEALING) {
                drawBoosterTear(guiGraphics, foreground, button.getX(), button.getY(), scale, 9);
            } else if (this.animationState != AnimationState.BOOSTER_REVEALED) {
                boolean hovered = this.animationState == AnimationState.IDLE && button.isHoveredOrFocused();
                drawBoosterPack(guiGraphics, hovered ? BOOSTER_BACKGROUND_HOVERED : BOOSTER_BACKGROUND,
                        foreground, button.getX(), button.getY(), scale);
            }
        }
        renderBoosterParticles(guiGraphics, partialTick);
    }

    private void renderPulledCards(GuiGraphics guiGraphics, DungeonWaveMenu.WaveOptionView option) {
        ArrayList<Integer> pulledTypes = new ArrayList<>();
        for (int index = 0; index < option.pulledCardCounts().size(); index++) {
            if (option.pulledCardCounts().get(index) > 0) pulledTypes.add(index);
        }
        if (pulledTypes.isEmpty()) return;

        int gap = 4;
        float scale = Math.min(0.78F, (this.width - 24.0F - (pulledTypes.size() - 1) * gap)
                / (pulledTypes.size() * DungeonDeckRenderer.cardWidth()));
        int cardWidth = Math.round(DungeonDeckRenderer.cardWidth() * scale);
        int cardHeight = Math.round(DungeonDeckRenderer.cardHeight() * scale);
        int totalWidth = pulledTypes.size() * cardWidth + (pulledTypes.size() - 1) * gap;
        int startX = (this.width - totalWidth) / 2;
        int targetY = Math.max(28, Math.min(boosterFocusY() - 62, boosterDeckTop() - cardHeight - 14));
        int originX = (this.width - cardWidth) / 2;
        int originY = boosterFocusY() + (BOOSTER_H - cardHeight) / 2;
        int flightTicks = Math.max(6, BOOSTER_REVEAL_TICKS
                - Math.max(0, pulledTypes.size() - 1) * BOOSTER_REVEAL_STAGGER_TICKS);

        for (int order = 0; order < pulledTypes.size(); order++) {
            int typeIndex = pulledTypes.get(order);
            int count = option.pulledCardCounts().get(typeIndex);
            float progress = this.animationState == AnimationState.BOOSTER_REVEALED
                    ? 1.0F
                    : Mth.clamp((this.animationTick - order * BOOSTER_REVEAL_STAGGER_TICKS)
                    / (float) flightTicks, 0.0F, 1.0F);
            progress = easeOutCubic(progress);
            int targetX = startX + order * (cardWidth + gap);
            int drawX = Mth.floor(Mth.lerp(progress, originX, targetX));
            int drawY = Mth.floor(Mth.lerp(progress, originY, targetY));
            CardType type = CardType.values()[typeIndex];
            DungeonDeckRenderer.renderCard(guiGraphics, this.font, type,
                    new CardState(count, count * type.effectPercent()), drawX, drawY, scale);
        }
    }

    private void drawBoosterPack(GuiGraphics guiGraphics, ResourceLocation background, ResourceLocation foreground,
            int x, int y, float scale) {
        withBoosterTransform(guiGraphics, x, y, scale, () -> {
            guiGraphics.blit(background, 0, 0, 0, 0, CARD_W, BOOSTER_H, CARD_W, BOOSTER_H);
            guiGraphics.blit(foreground, 0, 0, 0, 0, CARD_W, BOOSTER_H, CARD_W, BOOSTER_H);
        });
    }

    private void drawBoosterTear(GuiGraphics guiGraphics, ResourceLocation foreground, int x, int y, float scale, int frame) {
        withBoosterTransform(guiGraphics, x, y, scale, () -> {
            int clampedFrame = Mth.clamp(frame, 0, 9);
            guiGraphics.blit(BOOSTER_TEAR, 0, 0, 0, clampedFrame * BOOSTER_H,
                    CARD_W, BOOSTER_H, CARD_W, BOOSTER_H * 10);
            guiGraphics.blit(foreground, 0, 0, 0, 0, CARD_W, BOOSTER_H, CARD_W, BOOSTER_H);
        });
    }

    private void withBoosterTransform(GuiGraphics guiGraphics, int x, int y, float scale, Runnable draw) {
        float scaledWidth = CARD_W * scale;
        float scaledHeight = BOOSTER_H * scale;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x - (scaledWidth - CARD_W) / 2.0F,
                y - (scaledHeight - BOOSTER_H) / 2.0F, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        draw.run();
        guiGraphics.pose().popPose();
    }

    private ResourceLocation boosterForeground(int index) {
        int difficulty = index < this.menu.options().size() ? this.menu.options().get(index).difficultyRating() : index + 1;
        return BOOSTER_FOREGROUNDS.get(Mth.clamp(difficulty - 1, 0, BOOSTER_FOREGROUNDS.size() - 1));
    }

    private DungeonWaveMenu.WaveOptionView selectedBoosterOption() {
        return this.selectedCard >= 0 && this.selectedCard < this.menu.options().size()
                ? this.menu.options().get(this.selectedCard)
                : null;
    }

    private int boosterDeckTop() {
        return this.height - 8 - DungeonDeckRenderer.fullDeckHeight(Math.max(120, this.width - 24));
    }

    private int boosterSelectionY() {
        return Math.max(28, Math.min(this.topPos + 34, boosterDeckTop() - BOOSTER_H - 28));
    }

    private int boosterFocusY() {
        return Math.max(48, Math.min(boosterDeckTop() - BOOSTER_H - 22,
                (boosterDeckTop() - BOOSTER_H) / 2 + 36));
    }

    private void renderBoosterLabels(GuiGraphics guiGraphics) {
        if (this.animationState != AnimationState.APPEARING && this.animationState != AnimationState.IDLE) return;
        for (int index = 0; index < this.optionButtons.size() && index < this.menu.options().size(); index++) {
            Button button = this.optionButtons.get(index);
            DungeonWaveMenu.WaveOptionView option = this.menu.options().get(index);
            int centerX = button.getX() - this.leftPos + CARD_W / 2;
            int labelY = button.getY() - this.topPos + BOOSTER_H + 3;
            drawScaledCentered(guiGraphics, option.title().getString(), centerX, labelY, 0.72F, 0xFFF3D78A);
            String totalCards = option.details().getString().split("\\n", 2)[0];
            drawScaledCentered(guiGraphics, totalCards, centerX, labelY + 8, 0.58F, 0xFFD0D0D0);
        }
    }

    private void renderBoosterParticles(GuiGraphics guiGraphics, float partialTick) {
        for (BoosterParticle particle : this.boosterParticles) {
            float life = particle.life / (float) particle.maxLife;
            int alpha = Mth.clamp(Math.round(255.0F * life), 0, 255);
            int color = alpha << 24 | particle.color;
            int x = Mth.floor(particle.x + particle.velocityX * partialTick);
            int y = Mth.floor(particle.y + particle.velocityY * partialTick);
            guiGraphics.fill(x, y, x + particle.size, y + particle.size, color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String stageTitle = this.menu.stage() == DungeonWaveMenu.STAGE_BOOSTER
                ? "Select booster pack"
                : (this.menu.stage() == DungeonWaveMenu.STAGE_LOADOUT ? "Loadout Selection" : "Upgrade Selection");
        String heading = this.menu.stage() == DungeonWaveMenu.STAGE_BOOSTER
                ? stageTitle
                : "Floor " + this.menu.waveNumber() + " - " + stageTitle;
        guiGraphics.drawCenteredString(this.font, Component.literal(heading).withStyle(ChatFormatting.BOLD), this.imageWidth / 2, 2, 0xFFE36B);
        if (this.menu.stage() == DungeonWaveMenu.STAGE_BOOSTER) {
            guiGraphics.drawCenteredString(this.font, Component.literal("Floor " + this.menu.waveNumber()), this.imageWidth / 2, 13, 0xB8B8B8);
            renderBoosterLabels(guiGraphics);
            return;
        }
        if (this.menu.ownerCanSelect()) {
            String promptKey = this.menu.stage() == DungeonWaveMenu.STAGE_LOADOUT
                    ? "screen.gatesofavarice.dungeon_wave.select_loadout_prompt"
                    : "screen.gatesofavarice.dungeon_wave.select_upgrade_prompt";
            guiGraphics.drawCenteredString(this.font, Component.translatable(promptKey), this.imageWidth / 2, 218, 0x6C6C6C);
        } else {
            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.gatesofavarice.dungeon_wave.waiting_owner"), this.imageWidth / 2, 218, 0x6C6C6C);
        }

        for (int index = 0; index < this.menu.options().size() && index < this.optionButtons.size(); index++) {
            DungeonWaveMenu.WaveOptionView option = this.menu.options().get(index);
            Button button = this.optionButtons.get(index);
            float cardScale = index < this.hoverScales.size() ? this.hoverScales.get(index) : 1.0F;
            renderCardContents(guiGraphics, button, option, cardScale, mouseX, mouseY);
        }
    }

    private void renderCardContents(GuiGraphics guiGraphics, Button button, DungeonWaveMenu.WaveOptionView option, float cardScale, int mouseX, int mouseY) {
        float scaledW = CARD_W * cardScale;
        float scaledH = CARD_H * cardScale;
        float offsetX = (scaledW - CARD_W) / 2.0F;
        float offsetY = (scaledH - CARD_H) / 2.0F;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(button.getX() - this.leftPos - offsetX, button.getY() - this.topPos - offsetY, 0.0F);
        guiGraphics.pose().scale(cardScale, cardScale, 1.0F);

        int centerX = CARD_W / 2;
        int textMaxChars = 16;
        int rowY = 8;
        for (String line : wrap(option.title().getString(), textMaxChars)) {
            drawScaledCentered(guiGraphics, line, centerX, rowY, 0.75F, 0xF3D78A);
            rowY += 8;
            if (rowY > 28) break;
        }
        if (this.menu.stage() == DungeonWaveMenu.STAGE_LOADOUT) {
            boolean lowerHover = mouseX >= button.getX() && mouseX < button.getX() + CARD_W
                    && mouseY >= button.getY() + CARD_H / 2 && mouseY < button.getY() + CARD_H;
            renderLoadoutCardContents(guiGraphics, option, centerX, lowerHover);
            guiGraphics.pose().popPose();
            return;
        }
        if (!option.displayStack().isEmpty()) {
            int iconY = 29;
            int iconX = option.secondaryDisplayStack().isEmpty() ? centerX - 8 : centerX - 18;
            guiGraphics.renderItem(option.displayStack(), iconX, iconY);
            if (!option.secondaryDisplayStack().isEmpty()) {
                guiGraphics.renderItem(option.secondaryDisplayStack(), centerX + 2, iconY);
            }
        } else if (this.menu.stage() != DungeonWaveMenu.STAGE_LOADOUT) {
            drawScaledCentered(guiGraphics, "*", centerX, 35, 0.75F, 0x6E6E6E);
        }
        rowY = this.menu.stage() == DungeonWaveMenu.STAGE_LOADOUT ? 49 : 50;
        for (String raw : option.details().getString().split("\\n")) {
            for (String line : wrap(raw, this.menu.stage() == DungeonWaveMenu.STAGE_LOADOUT ? 18 : textMaxChars)) {
                drawScaledCentered(guiGraphics, line, centerX, rowY, this.menu.stage() == DungeonWaveMenu.STAGE_LOADOUT ? 0.56F : 0.75F, detailColor(line));
                rowY += this.menu.stage() == DungeonWaveMenu.STAGE_LOADOUT ? 7 : 8;
            }
            if (rowY > CARD_H - 10) break;
        }
        if (option.difficultyRating() > 0) {
            renderDifficultyDots(guiGraphics, option.difficultyRating(), centerX, CARD_H - 12);
        }
        guiGraphics.pose().popPose();
    }

    private void renderLoadoutCardContents(GuiGraphics guiGraphics, DungeonWaveMenu.WaveOptionView option, int centerX, boolean lowerHover) {
        if (option.displayStack().isEmpty() && option.secondaryDisplayStack().isEmpty()) {
            guiGraphics.blit(DICE_ICON, centerX - 8, 39, 0, 0, 16, 16, 16, 16);
            return;
        }

        int iconY = 28;
        guiGraphics.renderItem(option.displayStack(), centerX - 18, iconY);
        guiGraphics.renderItem(option.secondaryDisplayStack(), centerX + 2, iconY);

        if (!option.ammoStack().isEmpty() && option.ammoCount() > 0) {
            String count = "x" + option.ammoCount();
            float scale = 0.65F;
            int totalWidth = 10 + 2 + Math.round(this.font.width(count) * scale);
            int left = centerX - totalWidth / 2;
            renderScaledItem(guiGraphics, option.ammoStack(), left, 47, 0.625F);
            drawScaled(guiGraphics, count, left + 12, 49, scale, 0xFFFFFF);
        }

        if (lowerHover) {
            ItemStack[] armor = {option.helmetStack(), option.chestStack(), option.legsStack(), option.feetStack()};
            for (int index = 0; index < armor.length; index++) guiGraphics.renderItem(armor[index], 4 + index * 17, 65);
            drawScaledCentered(guiGraphics, "Hover gear for stats", centerX, 84, 0.5F, 0xD8D8D8);
        } else {
            renderStatRow(guiGraphics, "Speed", option.speedRating(), 9, 62);
            renderStatRow(guiGraphics, "Damage", option.damageRating(), 9, 71);
            renderStatRow(guiGraphics, "Defence", option.defenceRating(), 9, 80);
            renderStatRow(guiGraphics, "Atk Spd", option.attackSpeedRating(), 9, 89);
        }
    }

    private void renderScaledItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y, float scale) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.renderItem(stack, 0, 0);
        guiGraphics.pose().popPose();
    }

    private void renderStatRow(GuiGraphics guiGraphics, String label, int rating, int x, int y) {
        drawScaled(guiGraphics, label, x, y, 0.55F, 0xF4F4F4);
        renderGoldenDots(guiGraphics, rating, CARD_W - 23, y);
    }

    private void renderGoldenDots(GuiGraphics guiGraphics, int rating, int x, int y) {
        if (rating <= 0) {
            return;
        }
        String dots = "\u25CF".repeat(Math.max(1, Math.min(5, rating)));
        int borderColor = 0x7A4A12;
        drawScaled(guiGraphics, dots, x - 1, y, 0.55F, borderColor);
        drawScaled(guiGraphics, dots, x + 1, y, 0.55F, borderColor);
        drawScaled(guiGraphics, dots, x, y - 1, 0.55F, borderColor);
        drawScaled(guiGraphics, dots, x, y + 1, 0.55F, borderColor);
        drawScaled(guiGraphics, dots, x, y, 0.55F, 0xFFE36B);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
        if (this.menu.stage() == DungeonWaveMenu.STAGE_BOOSTER) {
            if (this.animationState == AnimationState.IDLE) {
                for (int index = 0; index < this.optionButtons.size() && index < this.menu.options().size(); index++) {
                    Button button = this.optionButtons.get(index);
                    if (x >= button.getX() && x < button.getX() + CARD_W
                            && y >= button.getY() && y < button.getY() + BOOSTER_H) {
                        DungeonWaveMenu.WaveOptionView option = this.menu.options().get(index);
                        ArrayList<Component> lines = new ArrayList<>();
                        lines.add(Component.literal(option.title().getString() + " Booster Pack").withStyle(ChatFormatting.GOLD));
                        for (String detail : option.details().getString().split("\\n")) {
                            lines.add(Component.literal(detail).withStyle(ChatFormatting.GRAY));
                        }
                        guiGraphics.renderTooltip(this.font, lines, java.util.Optional.empty(), x, y);
                        return;
                    }
                }
            }
            return;
        }
        for (int i = 0; i < this.optionButtons.size() && i < this.menu.options().size(); i++) {
            DungeonWaveMenu.WaveOptionView option = this.menu.options().get(i);
            if (option.displayStack().isEmpty()) continue;
            Button button = this.optionButtons.get(i);
            int centerX = button.getX() + CARD_W / 2;
            int itemX = option.secondaryDisplayStack().isEmpty() ? centerX - 8 : centerX - 18;
            int itemY = button.getY() + 29;
            if (x >= itemX && x < itemX + 16 && y >= itemY && y < itemY + 16) {
                guiGraphics.renderTooltip(this.font, option.displayStack(), x, y);
                return;
            }
            if (!option.secondaryDisplayStack().isEmpty()) {
                int secondaryX = centerX + 2;
                if (x >= secondaryX && x < secondaryX + 16 && y >= itemY && y < itemY + 16) {
                    guiGraphics.renderTooltip(this.font, option.secondaryDisplayStack(), x, y);
                    return;
                }
            }
        }
        if (this.menu.stage() == DungeonWaveMenu.STAGE_LOADOUT) {
            for (int i = 0; i < this.optionButtons.size() && i < this.menu.options().size(); i++) {
                DungeonWaveMenu.WaveOptionView option = this.menu.options().get(i);
                Button button = this.optionButtons.get(i);
                boolean lowerHover = x >= button.getX() && x < button.getX() + CARD_W
                        && y >= button.getY() + CARD_H / 2 && y < button.getY() + CARD_H;
                if (!lowerHover) continue;
                if (option.displayStack().isEmpty()) continue;
                ItemStack[] armor = {option.helmetStack(), option.chestStack(), option.legsStack(), option.feetStack()};
                for (int armorIndex = 0; armorIndex < armor.length; armorIndex++) {
                    int iconX = button.getX() + 4 + armorIndex * 17;
                    int iconY = button.getY() + 65;
                    if (x >= iconX && x < iconX + 16 && y >= iconY && y < iconY + 16) {
                        guiGraphics.renderTooltip(this.font, armor[armorIndex], x, y);
                        return;
                    }
                }
            }
        }
        for (int i = 0; i < this.optionButtons.size() && i < this.menu.options().size(); i++) {
            DungeonWaveMenu.WaveOptionView option = this.menu.options().get(i);
            if (option.difficultyRating() <= 0) continue;
            Button button = this.optionButtons.get(i);
            int centerX = button.getX() + CARD_W / 2;
            int dotCount = Math.max(1, Math.min(5, option.difficultyRating()));
            int width = Math.max(18, (int) (this.font.width("\u25CF".repeat(dotCount)) * 0.72F));
            int left = centerX - width / 2;
            int top = button.getY() + CARD_H - 14;
            if (x >= left && x <= left + width && y >= top && y <= top + 10) {
                guiGraphics.renderTooltip(this.font, difficultyTooltip(option.difficultyRating()), x, y);
                return;
            }
        }
    }

    private void selectOption(int optionIndex) {
        if (this.animationState != AnimationState.IDLE) {
            return;
        }
        this.selectedCard = optionIndex;
        this.pendingClickButtonId = optionIndex;
        this.animationState = this.menu.stage() == DungeonWaveMenu.STAGE_BOOSTER
                ? AnimationState.BOOSTER_FOCUSING
                : AnimationState.DISCARDING_SELECT;
        this.animationTick = 0;
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void triggerDiscardAndSend(int buttonId) {
        if (this.animationState != AnimationState.IDLE) {
            return;
        }
        this.selectedCard = -1;
        this.pendingClickButtonId = buttonId;
        this.animationState = AnimationState.DISCARDING_ALL;
        this.animationTick = 0;
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void sendPendingClick() {
        if (this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }
        if (this.pendingClickButtonId == Integer.MIN_VALUE) {
            return;
        }
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, this.pendingClickButtonId);
        this.pendingClickButtonId = Integer.MIN_VALUE;
    }

    private void selectReroll() {
        triggerDiscardAndSend(DungeonWaveMenu.REROLL_BUTTON_ID);
    }

    private void selectSkip() {
        triggerDiscardAndSend(DungeonWaveMenu.SKIP_BUTTON_ID);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void drawScaledCentered(GuiGraphics gg, String text, int x, int y, float scale, int color) {
        int w = this.font.width(text);
        gg.pose().pushPose();
        gg.pose().translate(x - (w * scale) / 2.0F, y, 0.0F);
        gg.pose().scale(scale, scale, 1.0F);
        gg.drawString(this.font, text, 0, 0, color, false);
        gg.pose().popPose();
    }

    private void drawScaled(GuiGraphics gg, String text, int x, int y, float scale, int color) {
        gg.pose().pushPose();
        gg.pose().translate(x, y, 0.0F);
        gg.pose().scale(scale, scale, 1.0F);
        gg.drawString(this.font, text, 0, 0, color, false);
        gg.pose().popPose();
    }

    private int detailColor(String line) {
        if (isNegativeModifier(line)) return 0xFFD5D5;
        if (isPositiveModifier(line)) return 0xD7F0D9;
        return 0xF4F4F4;
    }

    private boolean isNegativeModifier(String line) {
        String normalized = line.toLowerCase(Locale.ROOT);
        return normalized.contains("elite")
                || normalized.contains("mob speed")
                || normalized.contains("mob health")
                || normalized.contains("mob damage")
                || normalized.contains("mob leech")
                || normalized.contains("mob resistance")
                || normalized.contains("mob regen")
                || normalized.contains("spawn chance")
                || normalized.contains("hoard")
                || normalized.contains("tank mobs")
                || normalized.contains("archer mobs")
                || normalized.contains("assassin mobs");
    }

    private boolean isPositiveModifier(String line) {
        String normalized = line.toLowerCase(Locale.ROOT);
        return normalized.contains("quantity")
                || normalized.contains("rarity")
                || normalized.contains("coins")
                || normalized.contains("xp")
                || normalized.contains("levels");
    }

    private void renderDifficultyDots(GuiGraphics guiGraphics, int difficulty, int centerX, int y) {
        if (difficulty <= 0) {
            return;
        }
        String dots = "\u25CF".repeat(Math.max(1, Math.min(5, difficulty)));
        int borderColor = 0xE1B85A;
        drawScaledCentered(guiGraphics, dots, centerX - 1, y, 0.72F, borderColor);
        drawScaledCentered(guiGraphics, dots, centerX + 1, y, 0.72F, borderColor);
        drawScaledCentered(guiGraphics, dots, centerX, y - 1, 0.72F, borderColor);
        drawScaledCentered(guiGraphics, dots, centerX, y + 1, 0.72F, borderColor);
        drawScaledCentered(guiGraphics, dots, centerX, y, 0.72F, difficultyColor(difficulty));
    }

    private Component difficultyTooltip(int difficulty) {
        String label = switch (difficulty) {
            case 1 -> "Easy";
            case 2 -> "Normal";
            case 3 -> "Medium";
            case 4 -> "Hard";
            default -> "Extreme";
        };
        return Component.literal(label);
    }
    private int difficultyColor(int difficulty) {
        return switch (difficulty) {
            case 1 -> 0x2F6B2F;
            case 2 -> 0x49A24A;
            case 3 -> 0xE2C85A;
            case 4 -> 0xE38B44;
            default -> 0xD34A4A;
        };
    }

    private List<String> wrap(String input, int max) {
        List<String> out = new ArrayList<>();
        String[] parts = input.split(" ");
        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            if (current.isEmpty()) current.append(part);
            else if (current.length() + 1 + part.length() <= max) current.append(" ").append(part);
            else {
                out.add(current.toString());
                current = new StringBuilder(part);
            }
        }
        if (!current.isEmpty()) out.add(current.toString());
        return out;
    }

    private final class CardButton extends Button {
        private CardButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // Intentionally blank: card art is rendered in screen background.
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        tickAnimations();
    }

    private void tickAnimations() {
        if (this.menu.stage() == DungeonWaveMenu.STAGE_BOOSTER) {
            tickBoosterAnimations();
            return;
        }
        this.animationTick++;
        float appearDelayTicks = 4.2F;
        float appearDurationTicks = 9.6F;
        float discardDurationTicks = 11.8F;

        for (int i = 0; i < this.optionButtons.size(); i++) {
            int baseX = this.baseCardX.get(i);
            int baseY = this.baseCardY.get(i);
            int targetX = baseX;
            int targetY = baseY;

            if (this.animationState == AnimationState.APPEARING) {
                float p = Mth.clamp((this.animationTick - i * appearDelayTicks) / (float) appearDurationTicks, 0.0F, 1.0F);
                p = easeOutCubic(p);
                int centerCardX = this.leftPos + (this.imageWidth - CARD_W) / 2;
                int centerCardY = baseY - 8;
                targetX = Mth.floor(Mth.lerp(p, centerCardX, baseX));
                targetY = Mth.floor(Mth.lerp(p, this.height + CARD_H + 24, centerCardY));
            } else if (this.animationState == AnimationState.DISCARDING_SELECT) {
                float p = Mth.clamp(this.animationTick / (float) discardDurationTicks, 0.0F, 1.0F);
                float eased = easeInOutCubic(p);
                if (i == this.selectedCard) {
                    int centerX = this.leftPos + (this.imageWidth - CARD_W) / 2;
                    targetX = Mth.floor(Mth.lerp(eased, baseX, centerX));
                    targetY = Mth.floor(Mth.lerp(eased, baseY, baseY - 10));
                } else {
                    int centerCardX = this.leftPos + (this.imageWidth - CARD_W) / 2;
                    float inward = easeInOutCubic(Math.min(p / 0.58F, 1.0F));
                    float drop = easeInCubic(Math.max(0.0F, (p - 0.28F) / 0.72F));
                    int centeredY = Mth.floor(Mth.lerp(inward, baseY, baseY - 8));
                    targetX = Mth.floor(Mth.lerp(inward, baseX, centerCardX));
                    targetY = Mth.floor(Mth.lerp(drop, centeredY, this.height + CARD_H + 24));
                }
            } else if (this.animationState == AnimationState.DISCARDING_ALL) {
                float p = Mth.clamp(this.animationTick / (float) discardDurationTicks, 0.0F, 1.0F);
                int centerCardX = this.leftPos + (this.imageWidth - CARD_W) / 2;
                float inward = easeInOutCubic(Math.min(p / 0.58F, 1.0F));
                float drop = easeInCubic(Math.max(0.0F, (p - 0.28F) / 0.72F));
                int centeredY = Mth.floor(Mth.lerp(inward, baseY, baseY - 8));
                targetX = Mth.floor(Mth.lerp(inward, baseX, centerCardX));
                targetY = Mth.floor(Mth.lerp(drop, centeredY, this.height + CARD_H + 24));
            }

            this.animatedCardX.set(i, targetX);
            this.animatedCardY.set(i, targetY);
            this.optionButtons.get(i).setX(targetX);
            this.optionButtons.get(i).setY(targetY);
        }

        boolean idle = this.animationState == AnimationState.IDLE;
        for (int i = 0; i < this.optionButtons.size(); i++) {
            Button optionButton = this.optionButtons.get(i);
            optionButton.active = idle && this.menu.ownerCanSelect();
            float current = i < this.hoverScales.size() ? this.hoverScales.get(i) : 1.0F;
            float target = idle && optionButton.isHoveredOrFocused() ? 1.10F : 1.0F;
            if (i < this.hoverScales.size()) {
                this.hoverScales.set(i, Mth.lerp(0.38F, current, target));
            }
        }
        this.rerollButton.active = false;
        this.skipButton.active = idle && this.menu.ownerCanSelect() && this.menu.stage() == DungeonWaveMenu.STAGE_UPGRADE;

        if (this.animationState == AnimationState.APPEARING && this.animationTick > Mth.ceil((this.optionButtons.size() - 1) * appearDelayTicks + appearDurationTicks)) {
            this.animationState = AnimationState.IDLE;
            this.animationTick = 0;
        } else if (this.animationState == AnimationState.DISCARDING_SELECT || this.animationState == AnimationState.DISCARDING_ALL) {
            if (this.animationTick >= Mth.ceil(discardDurationTicks)) {
                this.settleHoldTicks++;
                if (this.settleHoldTicks >= 20) {
                    sendPendingClick();
                }
            } else {
                this.settleHoldTicks = 0;
            }
        }
    }

    private void tickBoosterAnimations() {
        this.animationTick++;
        tickBoosterParticles();
        float appearDelayTicks = 4.2F;
        float appearDurationTicks = 9.6F;
        int focusX = (this.width - CARD_W) / 2;
        int focusY = boosterFocusY();

        for (int index = 0; index < this.optionButtons.size(); index++) {
            int baseX = this.baseCardX.get(index);
            int baseY = this.baseCardY.get(index);
            int targetX = baseX;
            int targetY = baseY;
            float targetScale = 1.0F;

            if (this.animationState == AnimationState.APPEARING) {
                float progress = Mth.clamp((this.animationTick - index * appearDelayTicks) / appearDurationTicks, 0.0F, 1.0F);
                progress = easeOutCubic(progress);
                targetX = Mth.floor(Mth.lerp(progress, focusX, baseX));
                targetY = Mth.floor(Mth.lerp(progress, this.height + BOOSTER_H + 24, baseY));
            } else if (this.animationState == AnimationState.BOOSTER_FOCUSING) {
                float progress = easeInOutCubic(Mth.clamp(this.animationTick / (float) BOOSTER_FOCUS_TICKS, 0.0F, 1.0F));
                targetX = Mth.floor(Mth.lerp(progress, baseX, focusX));
                targetY = Mth.floor(Mth.lerp(progress, baseY, focusY));
                if (index == this.selectedCard) {
                    targetScale = progress < 0.35F
                            ? Mth.lerp(progress / 0.35F, 1.0F, 0.78F)
                            : Mth.lerp((progress - 0.35F) / 0.65F, 0.78F, 0.95F);
                } else {
                    targetScale = Mth.lerp(progress, 1.0F, 0.0F);
                }
            } else if (this.animationState == AnimationState.BOOSTER_TEARING) {
                targetX = focusX;
                targetY = focusY;
                targetScale = index == this.selectedCard ? 0.95F : 0.0F;
            } else if (this.animationState == AnimationState.BOOSTER_REVEALING) {
                targetX = focusX;
                targetY = focusY;
                float shrink = easeOutCubic(Mth.clamp(this.animationTick / 8.0F, 0.0F, 1.0F));
                targetScale = index == this.selectedCard ? Mth.lerp(shrink, 0.95F, 0.0F) : 0.0F;
            } else if (this.animationState == AnimationState.BOOSTER_REVEALED) {
                targetX = focusX;
                targetY = focusY;
                targetScale = 0.0F;
            }

            Button button = this.optionButtons.get(index);
            button.setX(targetX);
            button.setY(targetY);
            this.animatedCardX.set(index, targetX);
            this.animatedCardY.set(index, targetY);
            if (this.animationState == AnimationState.IDLE && button.isHoveredOrFocused()) {
                float current = this.hoverScales.get(index);
                this.hoverScales.set(index, Mth.lerp(0.38F, current, 1.10F));
            } else {
                this.hoverScales.set(index, targetScale);
            }
            button.active = this.animationState == AnimationState.IDLE && this.menu.ownerCanSelect();
        }

        this.rerollButton.active = false;
        this.skipButton.active = false;

        if (this.animationState == AnimationState.APPEARING
                && this.animationTick > Mth.ceil((this.optionButtons.size() - 1) * appearDelayTicks + appearDurationTicks)) {
            this.animationState = AnimationState.IDLE;
            this.animationTick = 0;
        } else if (this.animationState == AnimationState.BOOSTER_FOCUSING
                && this.animationTick >= BOOSTER_FOCUS_TICKS) {
            this.animationState = AnimationState.BOOSTER_TEARING;
            this.animationTick = 0;
        } else if (this.animationState == AnimationState.BOOSTER_TEARING
                && this.animationTick >= BOOSTER_TEAR_TICKS) {
            spawnBoosterParticles();
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.FIREWORK_ROCKET_BLAST, 0.9F));
            this.animationState = AnimationState.BOOSTER_REVEALING;
            this.animationTick = 0;
        } else if (this.animationState == AnimationState.BOOSTER_REVEALING
                && this.animationTick >= boosterRevealTicks()) {
            this.animationState = AnimationState.BOOSTER_REVEALED;
            this.animationTick = 0;
        } else if (this.animationState == AnimationState.BOOSTER_REVEALED
                && this.animationTick >= BOOSTER_HOLD_TICKS) {
            sendPendingClick();
        }
    }

    private int boosterRevealTicks() {
        return BOOSTER_REVEAL_TICKS;
    }

    private void spawnBoosterParticles() {
        RandomSource random = RandomSource.create();
        double centerX = this.width / 2.0D;
        double centerY = boosterFocusY() + BOOSTER_H / 2.0D;
        int[] colors = {0xF6D365, 0xF05A7E, 0xA855F7, 0x38BDF8, 0xF8FAFC};
        for (int index = 0; index < 46; index++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double speed = 0.8D + random.nextDouble() * 2.4D;
            int life = 18 + random.nextInt(18);
            this.boosterParticles.add(new BoosterParticle(
                    centerX,
                    centerY,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 0.45D,
                    life,
                    life,
                    1 + random.nextInt(3),
                    colors[random.nextInt(colors.length)]));
        }
    }

    private void tickBoosterParticles() {
        Iterator<BoosterParticle> iterator = this.boosterParticles.iterator();
        while (iterator.hasNext()) {
            BoosterParticle particle = iterator.next();
            particle.life--;
            if (particle.life <= 0) {
                iterator.remove();
                continue;
            }
            particle.x += particle.velocityX;
            particle.y += particle.velocityY;
            particle.velocityX *= 0.97D;
            particle.velocityY = particle.velocityY * 0.97D + 0.045D;
        }
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

    private ResourceLocation resolveCardTexture(boolean hovered) {
        if (this.menu.stage() == DungeonWaveMenu.STAGE_UPGRADE) {
            return hovered ? ITEM_CARD_HOVERED : ITEM_CARD;
        }
        return hovered ? UPGRADE_CARD_HOVERED : UPGRADE_CARD;
    }

    private enum AnimationState {
        APPEARING,
        IDLE,
        BOOSTER_FOCUSING,
        BOOSTER_TEARING,
        BOOSTER_REVEALING,
        BOOSTER_REVEALED,
        DISCARDING_SELECT,
        DISCARDING_ALL
    }

    private static final class BoosterParticle {
        private double x;
        private double y;
        private double velocityX;
        private double velocityY;
        private int life;
        private final int maxLife;
        private final int size;
        private final int color;

        private BoosterParticle(double x, double y, double velocityX, double velocityY,
                int life, int maxLife, int size, int color) {
            this.x = x;
            this.y = y;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.life = life;
            this.maxLife = maxLife;
            this.size = size;
            this.color = color;
        }
    }
}
