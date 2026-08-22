package com.revilo.gatesofavarice.client.screen;

import com.revilo.gatesofavarice.menu.DungeonWaveMenu;
import java.util.ArrayList;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class DungeonWaveScreen extends AbstractContainerScreen<DungeonWaveMenu> {

    private static final ResourceLocation TAROT_CARD = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/tarrot-card.png");
    private static final ResourceLocation TAROT_CARD_HOVERED = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/tarrot-card-hovered.png");
    private static final ResourceLocation UPGRADE_CARD = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/upgrade-card.png");
    private static final ResourceLocation UPGRADE_CARD_HOVERED = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/upgrade-card_hovered.png");
    private static final ResourceLocation ITEM_CARD = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/item-card.png");
    private static final ResourceLocation ITEM_CARD_HOVERED = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/dungeon/item-card_hovered.png");
    private static final ResourceLocation DICE_ICON = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "textures/gui/icon/dice.png");
    private static final int CARD_W = 76;
    private static final int CARD_H = 103;
    private static final int CARD_GAP = 3;

    private final List<Button> optionButtons = new ArrayList<>();
    private Button rerollButton;
    private Button skipButton;
    private boolean showRunChanges = false;
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

        int totalWidth = DungeonWaveMenu.OPTION_COUNT * CARD_W + (DungeonWaveMenu.OPTION_COUNT - 1) * CARD_GAP;
        int x = this.leftPos + (this.imageWidth - totalWidth) / 2;
        int startY = this.topPos + 34;

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
        this.skipButton.visible = this.menu.stage() == 1;
        this.skipButton.active = this.menu.ownerCanSelect() && this.menu.stage() == 1;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        for (int i = 0; i < this.optionButtons.size(); i++) {
            Button button = this.optionButtons.get(i);
            boolean hovered = button.isHoveredOrFocused() && this.animationState == AnimationState.IDLE;
            ResourceLocation tex = resolveCardTexture(hovered);
            float scale = i < this.hoverScales.size() ? this.hoverScales.get(i) : 1.0F;
            drawCard(guiGraphics, tex, button.getX(), button.getY(), scale);
        }
        if (this.showRunChanges) {
            int boxW = 130;
            int boxH = 140;
            int x = this.leftPos + this.imageWidth - boxW - 8;
            int y = this.topPos + (this.imageHeight - boxH) / 2;
            guiGraphics.fill(x, y, x + boxW, y + boxH, 0xD0101010);
            guiGraphics.drawCenteredString(this.font, Component.literal("Run Changes"), x + boxW / 2, y + 8, 0xFFFFFF);
            int lineY = y + 22;
            for (Component line : this.menu.runChanges()) {
                drawScaledCentered(guiGraphics, line.getString(), x + boxW / 2 - this.leftPos, lineY - this.topPos, 0.70F, runChangeColor(line.getString()));
                lineY += 9;
                if (lineY > y + boxH - 10) break;
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String stageTitle = this.menu.stage() == 0 ? "Tarot Selection" : (this.menu.stage() == 2 ? "Loadout Selection" : "Upgrade Selection");
        guiGraphics.drawCenteredString(this.font, Component.literal("Floor " + this.menu.waveNumber() + " - " + stageTitle).withStyle(ChatFormatting.BOLD), this.imageWidth / 2, 2, 0xFFE36B);
        if (this.menu.ownerCanSelect()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.gatesofavarice.dungeon_wave.select_prompt"), this.imageWidth / 2, 218, 0x6C6C6C);
        } else {
            guiGraphics.drawCenteredString(this.font, Component.translatable("screen.gatesofavarice.dungeon_wave.waiting_owner"), this.imageWidth / 2, 218, 0x6C6C6C);
        }

        for (int index = 0; index < this.menu.options().size() && index < this.optionButtons.size(); index++) {
            DungeonWaveMenu.WaveOptionView option = this.menu.options().get(index);
            Button button = this.optionButtons.get(index);
            float cardScale = index < this.hoverScales.size() ? this.hoverScales.get(index) : 1.0F;
            renderCardContents(guiGraphics, button, option, cardScale);
        }
    }

    private void renderCardContents(GuiGraphics guiGraphics, Button button, DungeonWaveMenu.WaveOptionView option, float cardScale) {
        float scaledW = CARD_W * cardScale;
        float scaledH = CARD_H * cardScale;
        float offsetX = (scaledW - CARD_W) / 2.0F;
        float offsetY = (scaledH - CARD_H) / 2.0F;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(button.getX() - this.leftPos - offsetX, button.getY() - this.topPos - offsetY, 0.0F);
        guiGraphics.pose().scale(cardScale, cardScale, 1.0F);

        int centerX = CARD_W / 2;
        int textMaxChars = 16;
        if (this.menu.stage() == 0) {
            int rowY = 8;
            for (String line : option.details().getString().split("\\n")) {
                if (line.startsWith("---")) {
                    drawScaledCentered(guiGraphics, "----------", centerX, rowY, 0.58F, 0x7A6A52);
                    rowY += 7;
                    continue;
                }
                for (String wrapped : wrap(line, 18)) {
                    drawScaledCentered(guiGraphics, wrapped, centerX, rowY, 0.58F, tarotLineColor(line));
                    rowY += 7;
                }
                if (rowY > CARD_H - 7) break;
            }
            renderDifficultyDots(guiGraphics, option.difficultyRating(), centerX, CARD_H - 12);
            guiGraphics.pose().popPose();
            return;
        }

        int rowY = 8;
        for (String line : wrap(option.title().getString(), textMaxChars)) {
            drawScaledCentered(guiGraphics, line, centerX, rowY, 0.75F, 0xF3D78A);
            rowY += 8;
            if (rowY > 28) break;
        }
        if (this.menu.stage() == 2) {
            renderLoadoutCardContents(guiGraphics, option, centerX);
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
        } else if (this.menu.stage() != 2) {
            drawScaledCentered(guiGraphics, "*", centerX, 35, 0.75F, 0x6E6E6E);
        }
        rowY = this.menu.stage() == 2 ? 49 : 50;
        for (String raw : option.details().getString().split("\\n")) {
            for (String line : wrap(raw, this.menu.stage() == 2 ? 18 : textMaxChars)) {
                drawScaledCentered(guiGraphics, line, centerX, rowY, this.menu.stage() == 2 ? 0.56F : 0.75F, detailColor(line));
                rowY += this.menu.stage() == 2 ? 7 : 8;
            }
            if (rowY > CARD_H - 10) break;
        }
        if (option.difficultyRating() > 0) {
            renderDifficultyDots(guiGraphics, option.difficultyRating(), centerX, CARD_H - 12);
        }
        guiGraphics.pose().popPose();
    }

    private void renderLoadoutCardContents(GuiGraphics guiGraphics, DungeonWaveMenu.WaveOptionView option, int centerX) {
        if (option.displayStack().isEmpty() && option.secondaryDisplayStack().isEmpty()) {
            guiGraphics.blit(DICE_ICON, centerX - 8, 39, 0, 0, 16, 16, 16, 16);
            return;
        }

        int iconY = 28;
        guiGraphics.renderItem(option.displayStack(), centerX - 18, iconY);
        guiGraphics.renderItem(option.secondaryDisplayStack(), centerX + 2, iconY);

        String armorLine = option.details().getString().split("\\n", 2)[0];
        drawScaledCentered(guiGraphics, armorLine, centerX, 51, 0.56F, 0xFFE36B);
        renderStatRow(guiGraphics, "Speed", option.speedRating(), 9, 62);
        renderStatRow(guiGraphics, "Damage", option.damageRating(), 9, 71);
        renderStatRow(guiGraphics, "Defence", option.defenceRating(), 9, 80);
        renderStatRow(guiGraphics, "Atk Spd", option.attackSpeedRating(), 9, 89);
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
        if (this.menu.stage() == 2) {
            for (int i = 0; i < this.optionButtons.size() && i < this.menu.options().size(); i++) {
                DungeonWaveMenu.WaveOptionView option = this.menu.options().get(i);
                if (option.helmetStack().isEmpty()) continue;
                Button button = this.optionButtons.get(i);
                String armorLine = option.details().getString().split("\\n", 2)[0];
                int centerX = button.getX() + CARD_W / 2;
                int textWidth = Math.max(24, (int) (this.font.width(armorLine) * 0.56F));
                int left = centerX - textWidth / 2;
                int top = button.getY() + 51;
                if (x >= left && x <= left + textWidth && y >= top && y <= top + 7) {
                    ItemStack stack = armorTooltipStack(option, x, left, textWidth);
                    guiGraphics.renderTooltip(this.font, stack, x, y);
                    return;
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

    private ItemStack armorTooltipStack(DungeonWaveMenu.WaveOptionView option, int mouseX, int left, int width) {
        int segment = Mth.clamp((int) (((mouseX - left) / (float) Math.max(1, width)) * 4.0F), 0, 3);
        return switch (segment) {
            case 0 -> option.helmetStack();
            case 1 -> option.chestStack();
            case 2 -> option.legsStack();
            default -> option.feetStack();
        };
    }

    private void selectOption(int optionIndex) {
        if (this.animationState != AnimationState.IDLE) {
            return;
        }
        this.selectedCard = optionIndex;
        this.pendingClickButtonId = optionIndex;
        this.animationState = AnimationState.DISCARDING_SELECT;
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
            this.showRunChanges = !this.showRunChanges;
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

    private int tarotLineColor(String line) {
        if (isNegativeModifier(line) || line.toLowerCase(Locale.ROOT).contains("hoard")
                || line.toLowerCase(Locale.ROOT).contains("tank")
                || line.toLowerCase(Locale.ROOT).contains("archer")
                || line.toLowerCase(Locale.ROOT).contains("assassin")) {
            return 0xFFD5D5;
        }
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

    private int runChangeColor(String line) {
        String normalized = line.toLowerCase(Locale.ROOT);
        if (normalized.contains("+elite spawns")
                || normalized.contains("+mob speed")
                || normalized.contains("+mob health")
                || normalized.contains("+mob damage")
                || normalized.contains("+mob leech")
                || normalized.contains("+mob resistance")
                || normalized.contains("+mob regen")
                || normalized.contains("+spawn chance")) {
            return 0xB13A3A;
        }
        if (normalized.contains("+quantity")
                || normalized.contains("+rarity")
                || normalized.contains("+coins")
                || normalized.contains("+xp")
                || normalized.contains("+levels")) {
            return 0x2F8E42;
        }
        return 0xCFCFCF;
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
        this.skipButton.active = idle && this.menu.ownerCanSelect() && this.menu.stage() == 1;

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
        if (this.menu.stage() == 0) {
            return hovered ? TAROT_CARD_HOVERED : TAROT_CARD;
        }
        if (this.menu.stage() == 1) {
            return hovered ? ITEM_CARD_HOVERED : ITEM_CARD;
        }
        return hovered ? UPGRADE_CARD_HOVERED : UPGRADE_CARD;
    }

    private enum AnimationState {
        APPEARING,
        IDLE,
        DISCARDING_SELECT,
        DISCARDING_ALL
    }
}
