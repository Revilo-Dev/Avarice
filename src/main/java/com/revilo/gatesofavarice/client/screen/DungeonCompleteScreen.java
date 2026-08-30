package com.revilo.gatesofavarice.client.screen;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.integration.LevelUpClientIntegration;
import com.revilo.gatesofavarice.network.DungeonCompletePayload;
import com.revilo.gatesofavarice.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class DungeonCompleteScreen extends Screen {

    private static final ResourceLocation GUI = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/dungeon-complete/dungeon-complete-gui.png");
    private static final ResourceLocation SLOT = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/dungeon-complete/slot.png");
    private static final int GUI_W = 200;
    private static final int GUI_H = 166;
    private static final int REWARD_COLS = 4;
    private static final int REWARD_VISIBLE_ROWS = 5;
    private static final int REWARD_VISIBLE = REWARD_COLS * REWARD_VISIBLE_ROWS;
    private static final int REWARD_X = 120;
    private static final int REWARD_Y = 41;
    private static final int GOLD_COIN_Y = 25;

    private final DungeonCompletePayload payload;
    private final List<StatLine> statLines = new ArrayList<>();
    private int leftPos;
    private int topPos;
    private int rewardScroll;
    private int revealedStatCount;
    private int activeStatTick;

    public DungeonCompleteScreen(DungeonCompletePayload payload) {
        super(Component.literal(payload.survived() ? "Dungeon Complete" : "You Died"));
        this.payload = payload;
        buildStatLines();
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - GUI_W) / 2;
        this.topPos = (this.height - GUI_H) / 2;
        this.revealedStatCount = 0;
        this.activeStatTick = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.revealedStatCount >= this.statLines.size()) {
            return;
        }
        this.activeStatTick++;
        if (this.activeStatTick >= 18) {
            this.activeStatTick = 0;
            this.revealedStatCount++;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 1200.0F);
        guiGraphics.blit(GUI, this.leftPos, this.topPos, 0, 0, GUI_W, GUI_H, GUI_W, GUI_H);
        renderHeader(guiGraphics);
        renderStats(guiGraphics);
        renderGoldCoins(guiGraphics);
        renderRewards(guiGraphics);
        renderLevelBar(guiGraphics);
        renderRewardTooltip(guiGraphics, mouseX, mouseY);
        guiGraphics.pose().popPose();
    }

    private void renderHeader(GuiGraphics guiGraphics) {
        Component header = Component.literal(this.payload.survived() ? "Survived" : "You Died")
                .withStyle(this.payload.survived() ? ChatFormatting.GOLD : ChatFormatting.RED, ChatFormatting.BOLD);
        guiGraphics.drawCenteredString(this.font, header, this.leftPos + GUI_W / 2, this.topPos + 11, 0xFFFFFF);
        drawCentered(guiGraphics, "Floor " + this.payload.wavesComplete() + " / Best " + this.payload.bestWave(), 60, 25, 0xFFE36B);
    }

    private void renderStats(GuiGraphics guiGraphics) {
        int x = this.leftPos + 10;
        int y = this.topPos + 41;
        int[] colors = {
                0xB7C5DD,
                0x8FD1FF,
                0xF6D37A,
                0xE39A6B,
                0xD28C8C,
                0xC592C9,
                0x98D8A8,
                0xCAA7E8,
                0xEFE09A,
                0xE29B9B,
                0xD98989
        };
        for (int i = 0; i < this.statLines.size(); i++) {
            if (i > this.revealedStatCount) {
                break;
            }
            float progress = i < this.revealedStatCount ? 1.0F : statRevealProgress();
            float scale = 0.62F * statScale(progress);
            drawScaled(guiGraphics, this.statLines.get(i).render(progress), x, y, scale, colors[Math.min(i, colors.length - 1)]);
            y += 6;
        }
    }

    private void renderRewards(GuiGraphics guiGraphics) {
        if (!this.payload.survived()) {
            return;
        }
        List<ItemStack> rewards = this.payload.rewards();
        int startX = this.leftPos + REWARD_X;
        int startY = this.topPos + REWARD_Y;
        for (int slot = 0; slot < REWARD_VISIBLE; slot++) {
            int col = slot % REWARD_COLS;
            int row = slot / REWARD_COLS;
            int x = startX + col * 18;
            int y = startY + row * 18;
            guiGraphics.blit(SLOT, x, y, 0, 0, 18, 18, 18, 18);

            int rewardIndex = this.rewardScroll * REWARD_COLS + slot;
            if (rewardIndex >= rewards.size()) {
                continue;
            }
            ItemStack stack = rewards.get(rewardIndex);
            guiGraphics.renderItem(stack, x + 1, y + 1);
            guiGraphics.renderItemDecorations(this.font, stack, x + 1, y + 1);
        }
    }

    private void renderGoldCoins(GuiGraphics guiGraphics) {
        if (!this.payload.survived() || this.payload.goldCoinsEarned() <= 0) {
            return;
        }
        int x = this.leftPos + REWARD_X;
        int y = this.topPos + GOLD_COIN_Y;
        ItemStack coin = new ItemStack(ModItems.GOLD_COIN.get());
        guiGraphics.renderItem(coin, x, y - 2);
        drawScaled(guiGraphics, "Converted: " + this.payload.goldCoinsEarned() + " Gold Coins", x + 18, y + 4, 0.58F, 0xAA6C00);
    }

    private void renderLevelBar(GuiGraphics guiGraphics) {
        int targetWidth = 184;
        int targetHeight = 11;
        int barWidth = LevelUpClientIntegration.getLevelBarWidth();
        float scale = barWidth <= 0 ? 1.0F : targetWidth / (float) barWidth;
        int x = (this.width - targetWidth) / 2 - 1;
        int y = this.topPos + 148;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        boolean rendered = LevelUpClientIntegration.renderPlayerLevelBar(guiGraphics, 0, 0);
        guiGraphics.pose().popPose();
        if (!rendered) {
            guiGraphics.fill(x, y, x + targetWidth, y + targetHeight, 0xAA151515);
            guiGraphics.fill(x + 1, y + 1, x + targetWidth - 1, y + targetHeight - 1, 0xAA3A5EAA);
            guiGraphics.drawCenteredString(this.font, Component.literal("Level Progress"), this.leftPos + GUI_W / 2, y - 4, 0x6E5631);
        }
    }

    private void renderRewardTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.payload.survived()) {
            return;
        }
        int index = hoveredRewardIndex(mouseX, mouseY);
        if (index >= 0 && index < this.payload.rewards().size()) {
            guiGraphics.renderTooltip(this.font, this.payload.rewards().get(index), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.payload.survived()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int maxScroll = Math.max(0, Mth.ceil((this.payload.rewards().size() - REWARD_VISIBLE) / (float) REWARD_COLS));
        if (maxScroll <= 0 || !isInRewardArea(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        this.rewardScroll = Mth.clamp(this.rewardScroll - (int) Math.signum(scrollY), 0, maxScroll);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int hoveredRewardIndex(double mouseX, double mouseY) {
        if (!isInRewardArea(mouseX, mouseY)) {
            return -1;
        }
        int localX = (int) mouseX - (this.leftPos + REWARD_X);
        int localY = (int) mouseY - (this.topPos + REWARD_Y);
        int col = localX / 18;
        int row = localY / 18;
        if (col < 0 || col >= REWARD_COLS || row < 0 || row >= REWARD_VISIBLE_ROWS) {
            return -1;
        }
        return this.rewardScroll * REWARD_COLS + row * REWARD_COLS + col;
    }

    private boolean isInRewardArea(double mouseX, double mouseY) {
        return mouseX >= this.leftPos + REWARD_X && mouseX < this.leftPos + REWARD_X + REWARD_COLS * 18
                && mouseY >= this.topPos + REWARD_Y && mouseY < this.topPos + REWARD_Y + REWARD_VISIBLE_ROWS * 18;
    }

    private void drawCentered(GuiGraphics guiGraphics, String text, int centerX, int y, int color) {
        guiGraphics.drawCenteredString(this.font, Component.literal(text), this.leftPos + centerX, this.topPos + y, color);
    }

    private void drawScaled(GuiGraphics guiGraphics, String text, int x, int y, float scale, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(this.font, text, 0, 0, color, false);
        guiGraphics.pose().popPose();
    }

    private static String formatTime(long ticks) {
        long seconds = Math.max(0L, ticks / 20L);
        long minutes = seconds / 60L;
        long remainder = seconds % 60L;
        return String.format(Locale.ROOT, "%d:%02d", minutes, remainder);
    }

    private void buildStatLines() {
        this.statLines.clear();
        String progressVerb = this.payload.survived() ? "earnt" : "gathered";
        this.statLines.add(StatLine.time("Time Spent", this.payload.timeSpentTicks()));
        this.statLines.add(StatLine.number("Level Points " + progressVerb, this.payload.levelPointsEarned(), ""));
        this.statLines.add(StatLine.number("Mythic Coins " + progressVerb, this.payload.coinsEarned(), ""));
        if (this.payload.survived() && this.payload.goldCoinsEarned() > 0) {
            this.statLines.add(StatLine.number("Gold Coins Converted", this.payload.goldCoinsEarned(), ""));
        }
        this.statLines.add(StatLine.number("Mobs Killed", this.payload.mobsKilled(), ""));
        this.statLines.add(StatLine.number("Damage Delt", this.payload.damageDealt(), ""));
        this.statLines.add(StatLine.number("Damage Receieved", this.payload.damageReceived(), ""));
        this.statLines.add(StatLine.number("Experience " + progressVerb, this.payload.experienceEarned(), ""));
        this.statLines.add(StatLine.number("Rarity level", this.payload.rarityLevel(), "%"));
        this.statLines.add(StatLine.number("Qunatity level", this.payload.quantityLevel(), "%"));
        this.statLines.add(StatLine.number("Mob Health", this.payload.mobHealth(), "%"));
        this.statLines.add(StatLine.number("Mob Damage", this.payload.mobDamage(), "%"));
        this.statLines.add(StatLine.number("Lootboxes Looted", this.payload.lootboxesLooted(), ""));
        this.statLines.add(StatLine.number("Coin Piles Looted", this.payload.coinPilesLooted(), ""));
        this.statLines.add(StatLine.number("Knowledge Books Obtained", this.payload.knowledgeBooksObtained(), ""));
    }

    private float statRevealProgress() {
        return Mth.clamp(this.activeStatTick / 18.0F, 0.0F, 1.0F);
    }

    private static float statScale(float progress) {
        if (progress < 0.45F) {
            float local = progress / 0.45F;
            return Mth.lerp(easeOutBack(local), 0.82F, 1.14F);
        }
        float local = (progress - 0.45F) / 0.55F;
        return Mth.lerp(easeOutCubic(local), 1.14F, 1.0F);
    }

    private static float easeOutCubic(float t) {
        return 1.0F - (float) Math.pow(1.0F - t, 3.0D);
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float p = t - 1.0F;
        return 1.0F + c3 * p * p * p + c1 * p * p;
    }

    private record StatLine(String label, int target, String suffix, boolean timeValue) {
        private static StatLine number(String label, int target, String suffix) {
            return new StatLine(label, Math.max(0, target), suffix, false);
        }

        private static StatLine time(String label, long ticks) {
            return new StatLine(label, Math.max(0, (int) (ticks / 20L)), "", true);
        }

        private String render(float progress) {
            int current = Math.max(0, Mth.floor(this.target * Mth.clamp(progress, 0.0F, 1.0F)));
            String value = this.timeValue ? formatSeconds(current) : Integer.toString(current) + this.suffix;
            return this.label + ": " + value;
        }

        private static String formatSeconds(int seconds) {
            int minutes = seconds / 60;
            int remainder = seconds % 60;
            return String.format(Locale.ROOT, "%d:%02d", minutes, remainder);
        }
    }
}
