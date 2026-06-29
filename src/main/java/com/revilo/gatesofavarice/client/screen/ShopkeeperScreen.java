package com.revilo.gatesofavarice.client.screen;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.client.DungeonUpgradeClientState;
import com.revilo.gatesofavarice.dungeon.DungeonBoundItems;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCard;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCategory;
import com.revilo.gatesofavarice.menu.ShopkeeperMenu;
import com.revilo.gatesofavarice.network.SyncUpgradeCardsPayload;
import com.revilo.gatesofavarice.registry.ModItems;
import com.revilo.gatesofavarice.shop.GatewaySellValues;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.lwjgl.glfw.GLFW;
import net.revilodev.runic.stat.RuneStatType;
import net.revilodev.runic.stat.RuneStats;

public class ShopkeeperScreen extends AbstractContainerScreen<ShopkeeperMenu> {
    private static final ResourceLocation BUY_GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/shop/shop-gui.png");
    private static final ResourceLocation SELL_GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/shop/shop-sell-gui.png");
    private static final ResourceLocation REROLL_TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/shop/re-roll.png");
    private static final ResourceLocation REROLL_DISABLED_TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/shop/re-roll-disabled.png");
    private static final ResourceLocation BACK_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/shop/back_button.png");
    private static final ResourceLocation TAB_TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/shop/tab.png");
    private static final ResourceLocation TAB_SELECTED_TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/shop/tab_selected.png");
    private static final ResourceLocation CATEGORY_CARD = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/dungeon/item-card.png");
    private static final ResourceLocation CATEGORY_CARD_HOVERED = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/dungeon/item-card_hovered.png");
    private static final ResourceLocation EFFECT_CARD = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/dungeon/effect-card.png");
    private static final ResourceLocation EFFECT_CARD_HOVERED = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/dungeon/effect-card_hovered.png");
    private static final ResourceLocation DAMAGE_CARD = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/dungeon/damage-card.png");
    private static final ResourceLocation DAMAGE_CARD_HOVERED = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/dungeon/damage-card_hovered.png");
    private static final ResourceLocation STAT_CARD = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/dungeon/stat-card.png");
    private static final ResourceLocation STAT_CARD_HOVERED = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/dungeon/stat-card_hovered.png");
    private static final ResourceLocation UPGRADE_CARD = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/dungeon/upgrade-card.png");
    private static final ResourceLocation UPGRADE_CARD_HOVERED = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/dungeon/upgrade-card_hovered.png");
    private static final ResourceLocation TAROT_CARD = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/dungeon/tarrot-card.png");
    private static final ResourceLocation TAROT_CARD_HOVERED = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/dungeon/tarrot-card-hovered.png");
    private static final int CARD_W = 76;
    private static final int CARD_H = 103;
    private static final int BUY_AREA_LEFT = 7;
    private static final int BUY_AREA_TOP = 13;
    private static final int BUY_AREA_RIGHT = 168;
    private static final int BUY_AREA_BOTTOM = 74;
    private static final float CATEGORY_CARD_SCALE = 0.44F;
    private static final int CATEGORY_CARD_GAP = 4;
    private static final float UPGRADE_CARD_SCALE = 0.372F;
    private static final int UPGRADE_CARD_GAP = 1;
    private static final int REROLL_X = 153;
    private static final int REROLL_Y = 67;
    private static final int REROLL_RENDER_SIZE = 10;
    private static final int BACK_BUTTON_X = 133;
    private static final int BACK_BUTTON_Y = 68;
    private static final int BACK_BUTTON_W = 18;
    private static final int BACK_BUTTON_H = 10;
    private static final float BACK_BUTTON_SCALE = 0.8F;
    private static final int TAB_X = -32;
    private static final int TAB_BUY_Y = 9;
    private static final int TAB_SELL_Y = 37;
    private static final int TAB_WIDTH = 35;
    private static final int TAB_HEIGHT = 27;
    private static final int SELL_BUTTON_X = 117;
    private static final int SELL_BUTTON_Y = 56;
    private static final int SELL_BUTTON_WIDTH = 51;
    private static final int SELL_BUTTON_HEIGHT = 15;
    private static final int SELL_COIN_X = 144;
    private static final int SELL_COIN_Y = 27;
    private static final int SELL_TOTAL_X = 133;
    private static final int SELL_TOTAL_Y = 44;
    private static final int SELL_PRICE_ICON_SHIFT_X = -10;
    private static final int NEXT_WAVE_BUTTON_WIDTH = 200;
    private static final int NEXT_WAVE_BUTTON_HEIGHT = 20;
    private static final float SELL_HOLD_TICKS = 20.0F;
    private static final int SELL_COIN_PARTICLE_LIMIT = 24;
    private static final int COIN_TRAIL_SEGMENTS = 4;
    private static final int DRAW_STAGGER_TICKS = 3;
    private static final int DRAW_DURATION_TICKS = 8;
    private static final int DISCARD_DURATION_TICKS = 10;
    private static final int SELECT_SETTLE_TICKS = 6;
    private static final float HOVER_SCALE_MULTIPLIER = 1.10F;
    private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED_TEXTURE = ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final ResourceLocation BUTTON_DISABLED_TEXTURE = ResourceLocation.withDefaultNamespace("widget/button_disabled");

    private final List<CoinFlight> coinFlights = new ArrayList<>();
    private Button buyButton;
    private Button nextWaveButton;
    private Page activePage = Page.BUY;
    private int lastWalletBalance;
    private int walletPulseTicks;
    private float sellHoldTicks;
    private boolean sellHolding;
    private boolean coinHoveredLastFrame;
    private double lastMouseX;
    private double lastMouseY;
    private boolean categorySelection = true;
    private ItemStack upgradePreviewStack = ItemStack.EMPTY;
    private List<UpgradeCard> upgradeCards = List.of();
    private int selectedCardCount = 0;
    private int maxCardSelections = 5;
    private int runeSlotsUsed = 0;
    private int runeSlotsCapacity = 0;
    private AnimationState animationState = AnimationState.DRAWING;
    private int animationTick = 0;
    private int selectedCardIndex = -1;
    private int pendingButtonId = Integer.MIN_VALUE;
    private boolean awaitingRerollSync = false;
    private boolean pendingStateSync = false;
    private float coinTargetX;
    private float coinTargetY;

    public ShopkeeperScreen(ShopkeeperMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
        this.lastWalletBalance = menu.getWalletBalance();
    }

    @Override
    protected void init() {
        super.init();
        this.loadUpgradeState();
        this.startDrawAnimation();
        this.buyButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.gatesofavarice.shopkeeper.buy"), button -> {})
                .pos(0, 0)
                .size(1, 1)
                .build());
        this.nextWaveButton = this.addRenderableWidget(Button.builder(Component.literal("Next Wave"), button -> this.startNextWave())
                .pos(this.leftPos + (this.imageWidth - NEXT_WAVE_BUTTON_WIDTH) / 2, this.topPos + this.imageHeight + 4)
                .size(NEXT_WAVE_BUTTON_WIDTH, NEXT_WAVE_BUTTON_HEIGHT)
                .build());
        this.applyPageLayout();
        this.updateBuyButton();
        this.updateNextWaveButton();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.tickUpgradeAnimations();
        if (this.walletPulseTicks > 0) {
            this.walletPulseTicks--;
        }

        int walletBalance = this.menu.getWalletBalance();
        if (walletBalance > this.lastWalletBalance) {
            this.walletPulseTicks = 8;
        }
        this.lastWalletBalance = walletBalance;

        if (this.activePage == Page.SELL && this.sellHolding) {
            Minecraft minecraft = this.minecraft;
            long window = minecraft != null ? minecraft.getWindow().getWindow() : 0L;
            boolean stillDown = window != 0L && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            if (!stillDown || !this.isHoveringSellButton(this.lastMouseX, this.lastMouseY)) {
                this.sellHolding = false;
                this.sellHoldTicks = 0.0F;
            } else {
                this.sellHoldTicks = Math.min(SELL_HOLD_TICKS, this.sellHoldTicks + 1.0F);
                if (this.sellHoldTicks >= SELL_HOLD_TICKS) {
                    this.sellHolding = false;
                    this.sellHoldTicks = 0.0F;
                    this.sellStagedItems();
                }
            }
        }

        this.tickCoinFlights();
        this.updateBuyButton();
        this.updateNextWaveButton();
    }

    public void applyUpgradeCategoryState() {
        this.loadUpgradeState();
        this.pendingStateSync = false;
        this.awaitingRerollSync = false;
        this.startDrawAnimation();
    }

    public void applyUpgradeCardsState(SyncUpgradeCardsPayload payload) {
        this.loadUpgradeState();
        this.pendingStateSync = false;
        this.awaitingRerollSync = false;
        this.startDrawAnimation();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTabs(guiGraphics);
        if (this.activePage == Page.SELL) {
            this.renderUnsellableOverlays(guiGraphics);
        }
        this.renderCoinFlights(guiGraphics, partialTick);

        if (this.activePage == Page.BUY && this.renderUpgradeTooltip(guiGraphics, mouseX, mouseY)) {
            return;
        }
        if (this.renderWalletTooltip(guiGraphics, mouseX, mouseY)) {
            return;
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(this.activePage == Page.BUY ? BUY_GUI_TEXTURE : SELL_GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        this.renderWallet(guiGraphics);
        if (this.activePage == Page.BUY) {
            this.renderUpgradePanel(guiGraphics, mouseX, mouseY);
            this.renderRerollButton(guiGraphics);
            this.renderBackButton(guiGraphics);
        } else {
            this.renderSellPanel(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.translatable("screen.gatesofavarice.shopkeeper.title"), 6, 4, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (this.clickTab(mouseX, mouseY)) {
                return true;
            }
            if (this.activePage == Page.BUY) {
                if (!this.categorySelection && this.isHoveringBackButton(mouseX, mouseY) && this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ShopkeeperMenu.BACK_BUTTON_ID);
                    return true;
                }
                if (!this.categorySelection && this.isHoveringReroll(mouseX, mouseY) && this.minecraft != null && this.minecraft.gameMode != null && this.menu.canAffordReroll()) {
                    if (this.animationState == AnimationState.IDLE) {
                        this.animationState = AnimationState.REROLLING;
                        this.animationTick = 0;
                        this.selectedCardIndex = -1;
                    }
                    return true;
                }
                int hoveredIndex = this.getUpgradeSelectionIndex(mouseX, mouseY);
                if (hoveredIndex >= 0) {
                    this.handleUpgradeClick(hoveredIndex);
                    return true;
                }
            } else if (this.isHoveringSellButton(mouseX, mouseY) && this.menu.getSellValue() > 0) {
                this.sellHolding = true;
                this.sellHoldTicks = 0.0F;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.sellHolding) {
            this.sellHolding = false;
            this.sellHoldTicks = 0.0F;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void renderUpgradePanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.categorySelection) {
            this.renderCategoryCards(guiGraphics, mouseX, mouseY);
            guiGraphics.drawCenteredString(this.font, Component.literal("Pick upgrade deck").withStyle(ChatFormatting.GOLD), this.leftPos + 88, this.topPos + 64, 0xFFF0B8);
            return;
        }

        Component title = this.upgradePreviewStack.isEmpty()
                ? Component.literal("Item").withStyle(ChatFormatting.GOLD)
                : this.upgradePreviewStack.getHoverName().copy().withStyle(ChatFormatting.GOLD);
        guiGraphics.drawCenteredString(this.font, title, this.leftPos + 88, this.topPos + 15, 0xF3D78A);
        if (!this.upgradePreviewStack.isEmpty()
                && mouseX >= this.leftPos + 42 && mouseX <= this.leftPos + 134
                && mouseY >= this.topPos + 8 && mouseY <= this.topPos + 28) {
            renderUpgradePreviewTooltip(guiGraphics, mouseX, mouseY);
        }
        this.renderUpgradeCards(guiGraphics, mouseX, mouseY);
        this.renderSelectionCounter(guiGraphics);
    }

    private void renderSelectionCounter(GuiGraphics guiGraphics) {
        int remaining = Math.max(0, this.maxCardSelections - this.selectedCardCount);
        Component label = Component.literal("Select " + remaining + " Cards").withStyle(remaining > 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED);
        guiGraphics.drawCenteredString(this.font, label, this.leftPos + BUY_AREA_LEFT + buyAreaWidth() / 2, this.topPos + BUY_AREA_BOTTOM - 10, 0xFFFFFF);
    }

    private void renderCategoryCards(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int count = UpgradeCategory.values().length;
        int totalWidth = Math.round(count * CARD_W * CATEGORY_CARD_SCALE + Math.max(0, count - 1) * CATEGORY_CARD_GAP);
        int startX = this.leftPos + BUY_AREA_LEFT + (buyAreaWidth() - totalWidth) / 2;
        int y = this.topPos + BUY_AREA_TOP + 2;
        guiGraphics.enableScissor(this.leftPos + BUY_AREA_LEFT, this.topPos + BUY_AREA_TOP, this.leftPos + BUY_AREA_RIGHT, this.topPos + BUY_AREA_BOTTOM);
        for (int i = 0; i < count; i++) {
            int x = startX + Math.round(i * CARD_W * CATEGORY_CARD_SCALE) + (i * CATEGORY_CARD_GAP);
            boolean hovered = this.isMouseOverScaledCard(mouseX, mouseY, x, y, CATEGORY_CARD_SCALE);
            float drawScale = hovered && this.animationState == AnimationState.IDLE ? CATEGORY_CARD_SCALE * HOVER_SCALE_MULTIPLIER : CATEGORY_CARD_SCALE;
            int drawX = animatedCardX(x, i, startX, CATEGORY_CARD_SCALE);
            int drawY = animatedCardY(y, i, CATEGORY_CARD_SCALE);
            this.drawCard(guiGraphics, hovered ? CATEGORY_CARD_HOVERED : CATEGORY_CARD, drawX, drawY, drawScale);
            this.renderCategoryCardContents(guiGraphics, drawX, drawY, i, drawScale);
        }
        guiGraphics.disableScissor();
    }

    private void renderUpgradeCards(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int count = Math.min(5, this.upgradeCards.size());
        int totalWidth = Math.round(count * CARD_W * UPGRADE_CARD_SCALE + Math.max(0, count - 1) * UPGRADE_CARD_GAP);
        int startX = this.leftPos + BUY_AREA_LEFT + (buyAreaWidth() - totalWidth) / 2;
        int y = this.topPos + BUY_AREA_TOP + 12;
        guiGraphics.enableScissor(this.leftPos + BUY_AREA_LEFT, this.topPos + BUY_AREA_TOP, this.leftPos + BUY_AREA_RIGHT, this.topPos + BUY_AREA_BOTTOM);
        for (int i = 0; i < count; i++) {
            UpgradeCard card = this.upgradeCards.get(i);
            int x = startX + Math.round(i * CARD_W * UPGRADE_CARD_SCALE) + (i * UPGRADE_CARD_GAP);
            boolean blocked = this.isBlockedByRuneSlots(card);
            boolean hovered = !blocked && this.isMouseOverScaledCard(mouseX, mouseY, x, y, UPGRADE_CARD_SCALE);
            float drawScale = hovered && this.animationState == AnimationState.IDLE ? UPGRADE_CARD_SCALE * HOVER_SCALE_MULTIPLIER : UPGRADE_CARD_SCALE;
            int drawX = animatedCardX(x, i, startX, UPGRADE_CARD_SCALE);
            int drawY = animatedCardY(y, i, UPGRADE_CARD_SCALE);
            this.drawCard(guiGraphics, resolveCardTexture(card, hovered), drawX, drawY, drawScale);
            this.renderUpgradeCardContents(guiGraphics, drawX, drawY, card, drawScale);
            if (blocked) {
                guiGraphics.fill(drawX, drawY, drawX + Math.round(CARD_W * drawScale), drawY + Math.round(CARD_H * drawScale), 0x99000000);
            }
        }
        guiGraphics.disableScissor();
    }

    private void renderCategoryCardContents(GuiGraphics guiGraphics, int x, int y, int index, float scale) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        int center = CARD_W / 2;
        int rowY = 16;
        for (String line : wrap(fullCategoryName(index), 12)) {
            drawScaledCentered(guiGraphics, line, center, rowY, 0.92F, 0xF3D78A);
            rowY += 10;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(center - 12, 43, 0.0F);
        guiGraphics.pose().scale(1.65F, 1.65F, 1.0F);
        guiGraphics.renderItem(categoryIcon(index), 0, 0);
        guiGraphics.pose().popPose();
        guiGraphics.pose().popPose();
    }

    private void renderUpgradeCardContents(GuiGraphics guiGraphics, int x, int y, UpgradeCard card, float scale) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        int center = CARD_W / 2;
        int rowY = 7;
        for (String line : wrap(card.title(), 14)) {
            drawScaledCentered(guiGraphics, line, center, rowY, 0.75F, 0xF3D78A);
            rowY += 8;
            if (rowY > 24) {
                break;
            }
        }
        ResourceLocation icon = resolveCardIcon(card);
        if (icon != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(center - 11, 26, 0.0F);
            guiGraphics.pose().scale(1.35F, 1.35F, 1.0F);
            guiGraphics.blit(icon, 0, 0, 0, 0, 16, 16, 16, 16);
            guiGraphics.pose().popPose();
        }
        int detailY = 50;
        for (String line : wrap(card.changeLabel(), 16)) {
            drawScaledCentered(guiGraphics, line, center, detailY, 0.58F, 0xD7F0D9);
            detailY += 7;
            if (detailY > 58) {
                break;
            }
        }
        drawScaledCentered(guiGraphics, card.currentValue(), center, 68, 0.58F, 0xFFD5D5);
        drawScaledCentered(guiGraphics, card.newValue(), center, 85, 0.62F, 0xFFFFFF);
        guiGraphics.pose().popPose();
    }

    private void tickUpgradeAnimations() {
        if (this.animationState == AnimationState.IDLE) {
            return;
        }
        this.animationTick++;
        int cardCount = Math.max(1, this.categorySelection ? UpgradeCategory.values().length : Math.min(5, this.upgradeCards.size()));
        int drawEnd = (cardCount - 1) * DRAW_STAGGER_TICKS + DRAW_DURATION_TICKS;
        if (this.animationState == AnimationState.DRAWING && this.animationTick > drawEnd) {
            this.animationState = AnimationState.IDLE;
            this.animationTick = 0;
            return;
        }
        if (this.animationState == AnimationState.REROLLING && this.animationTick >= DISCARD_DURATION_TICKS) {
            if (!this.awaitingRerollSync && this.minecraft != null && this.minecraft.gameMode != null) {
                this.awaitingRerollSync = true;
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ShopkeeperMenu.REROLL_BUTTON_ID);
            }
            return;
        }
        if (this.animationState == AnimationState.SELECTING && this.animationTick >= DISCARD_DURATION_TICKS + SELECT_SETTLE_TICKS) {
            if (this.pendingButtonId != Integer.MIN_VALUE && this.minecraft != null && this.minecraft.gameMode != null) {
                int toSend = this.pendingButtonId;
                this.pendingButtonId = Integer.MIN_VALUE;
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, toSend);
            }
        }
    }

    private void startDrawAnimation() {
        this.animationState = AnimationState.DRAWING;
        this.animationTick = 0;
        this.selectedCardIndex = -1;
        this.pendingButtonId = Integer.MIN_VALUE;
        this.coinTargetX = this.leftPos + BUY_AREA_LEFT + buyAreaWidth() / 2.0F;
        this.coinTargetY = this.topPos + BUY_AREA_TOP + buyAreaHeight() / 2.0F;
    }

    private int animatedCardX(int baseX, int index, int startX, float scale) {
        if (this.animationState == AnimationState.DRAWING) {
            float t = entryProgress(index);
            int launchX = this.leftPos + BACK_BUTTON_X + 16;
            return Mth.floor(Mth.lerp(easeOutCubic(t), launchX, baseX));
        }
        if (this.animationState == AnimationState.SELECTING && !this.categorySelection) {
            float t = Mth.clamp(this.animationTick / (float) DISCARD_DURATION_TICKS, 0.0F, 1.0F);
            if (index == this.selectedCardIndex) {
                int centerX = this.leftPos + BUY_AREA_LEFT + buyAreaWidth() / 2 - Math.round(CARD_W * scale) / 2;
                return Mth.floor(Mth.lerp(easeInOutCubic(t), baseX, centerX));
            }
        }
        return baseX;
    }

    private int animatedCardY(int baseY, int index, float scale) {
        int cardHeight = Math.round(CARD_H * scale);
        if (this.animationState == AnimationState.DRAWING) {
            float t = entryProgress(index);
            int launchY = this.topPos + BACK_BUTTON_Y + 12;
            return Mth.floor(Mth.lerp(easeOutCubic(t), launchY, baseY));
        }
        if (this.animationState == AnimationState.REROLLING) {
            float t = Mth.clamp(this.animationTick / (float) DISCARD_DURATION_TICKS, 0.0F, 1.0F);
            return Mth.floor(Mth.lerp(easeInCubic(t), baseY, this.topPos + BUY_AREA_BOTTOM + cardHeight + 12));
        }
        if (this.animationState == AnimationState.SELECTING && !this.categorySelection) {
            float t = Mth.clamp(this.animationTick / (float) DISCARD_DURATION_TICKS, 0.0F, 1.0F);
            if (index == this.selectedCardIndex) {
                int centerY = this.topPos + BUY_AREA_TOP + (buyAreaHeight() - cardHeight) / 2;
                return Mth.floor(Mth.lerp(easeInOutCubic(t), baseY, centerY));
            }
            return Mth.floor(Mth.lerp(easeInCubic(t), baseY, this.topPos + BUY_AREA_BOTTOM + cardHeight + 18));
        }
        return baseY;
    }

    private float entryProgress(int index) {
        return Mth.clamp((this.animationTick - index * DRAW_STAGGER_TICKS) / (float) DRAW_DURATION_TICKS, 0.0F, 1.0F);
    }

    private void renderSellPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean coinHovered = this.isHoveringSellCoin(mouseX, mouseY);
        if (coinHovered && !this.coinHoveredLastFrame) {
            this.playDing(1.15F);
        }
        this.coinHoveredLastFrame = coinHovered;

        float hoverScale = coinHovered ? 1.1F : 1.0F;
        WorkbenchCrystalRenderer.render(guiGraphics, coinStack(), this.leftPos + SELL_COIN_X, this.topPos + SELL_COIN_Y, 0.0F, hoverScale * 0.9F, WorkbenchCrystalRenderer.BASE_SPIN_SPEED);
        this.renderCoinPrice(guiGraphics, this.leftPos + SELL_TOTAL_X, this.topPos + SELL_TOTAL_Y, this.menu.getSellValue(), 0.75F, this.menu.getSellValue() > 0 ? 0xB06CFF : 0x7B6A8D, SELL_PRICE_ICON_SHIFT_X);

        int buttonLeft = this.leftPos + SELL_BUTTON_X;
        int buttonTop = this.topPos + SELL_BUTTON_Y;
        int buttonBottom = buttonTop + SELL_BUTTON_HEIGHT;
        boolean hovered = this.isHoveringSellButton(mouseX, mouseY);
        boolean enabled = this.menu.getSellValue() > 0;
        guiGraphics.blitSprite(enabled ? (hovered ? BUTTON_HIGHLIGHTED_TEXTURE : BUTTON_TEXTURE) : BUTTON_DISABLED_TEXTURE, buttonLeft, buttonTop, SELL_BUTTON_WIDTH, SELL_BUTTON_HEIGHT);
        int progressWidth = Math.round((SELL_BUTTON_WIDTH - 4) * (this.sellHoldTicks / SELL_HOLD_TICKS));
        if (progressWidth > 0) {
            guiGraphics.fill(buttonLeft + 2, buttonTop + 2, buttonLeft + 2 + progressWidth, buttonBottom - 2, 0xCCB06CFF);
        }
        guiGraphics.drawCenteredString(this.font, Component.translatable("screen.gatesofavarice.shopkeeper.sell"), buttonLeft + SELL_BUTTON_WIDTH / 2, buttonTop + 4, 0xF4E9FF);
    }

    private void renderWallet(GuiGraphics guiGraphics) {
        WalletLayout walletLayout = this.getWalletLayout();
        boolean hovered = walletLayout.isMouseOver(this.lastMouseX, this.lastMouseY);
        float pulse = 1.0F + (this.walletPulseTicks > 0 ? 0.1F * Mth.sin((8 - this.walletPulseTicks) / 8.0F * Mth.PI) : 0.0F);
        float hoverScale = hovered ? 1.1F : 1.0F;
        float scale = 0.75F * pulse * hoverScale;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(walletLayout.iconX(), walletLayout.iconY(), 0.0F);
        guiGraphics.pose().scale(0.5F * hoverScale, 0.5F * hoverScale, 1.0F);
        guiGraphics.renderItem(coinStack(), 0, 0);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(walletLayout.textX(), walletLayout.textY(), 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(this.font, walletLayout.text(), -1, 0, 0x101010, false);
        guiGraphics.drawString(this.font, walletLayout.text(), 1, 0, 0x101010, false);
        guiGraphics.drawString(this.font, walletLayout.text(), 0, -1, 0x101010, false);
        guiGraphics.drawString(this.font, walletLayout.text(), 0, 1, 0x101010, false);
        guiGraphics.drawString(this.font, walletLayout.text(), 0, 0, 0xB06CFF, false);
        guiGraphics.pose().popPose();
    }

    private void renderCoinPrice(GuiGraphics guiGraphics, int x, int y, int price, float scale, int textColor, int iconShiftX) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 200.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.renderItem(coinStack(), iconShiftX, 0);
        String text = formatCompactValue(price);
        guiGraphics.drawString(this.font, text, 9, 4, 0x101010, false);
        guiGraphics.drawString(this.font, text, 11, 4, 0x101010, false);
        guiGraphics.drawString(this.font, text, 10, 3, 0x101010, false);
        guiGraphics.drawString(this.font, text, 10, 5, 0x101010, false);
        guiGraphics.drawString(this.font, text, 10, 4, textColor, false);
        guiGraphics.pose().popPose();
    }

    private void renderRerollButton(GuiGraphics guiGraphics) {
        if (this.categorySelection) {
            return;
        }
        ResourceLocation texture = this.menu.hasRerollsRemaining() && this.menu.canAffordReroll() ? REROLL_TEXTURE : REROLL_DISABLED_TEXTURE;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.leftPos + REROLL_X, this.topPos + REROLL_Y, 150.0F);
        guiGraphics.pose().scale(REROLL_RENDER_SIZE / 16.0F, REROLL_RENDER_SIZE / 16.0F, 1.0F);
        guiGraphics.blit(texture, 0, 0, 0, 0, 16, 16, 16, 16);
        guiGraphics.pose().popPose();
    }

    private void renderBackButton(GuiGraphics guiGraphics) {
        if (this.categorySelection) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.leftPos + BACK_BUTTON_X, this.topPos + BACK_BUTTON_Y, 150.0F);
        guiGraphics.pose().scale(BACK_BUTTON_SCALE, BACK_BUTTON_SCALE, 1.0F);
        guiGraphics.blit(BACK_BUTTON_TEXTURE, 0, 0, 0, 0, BACK_BUTTON_W, BACK_BUTTON_H, BACK_BUTTON_W, BACK_BUTTON_H);
        guiGraphics.pose().popPose();
    }

    private void renderTabs(GuiGraphics guiGraphics) {
        this.renderTab(guiGraphics, Page.BUY, TAB_BUY_Y, Component.translatable("screen.gatesofavarice.shopkeeper.tab_buy"));
        this.renderTab(guiGraphics, Page.SELL, TAB_SELL_Y, Component.translatable("screen.gatesofavarice.shopkeeper.tab_sell"));
    }

    private void renderTab(GuiGraphics guiGraphics, Page page, int tabY, Component label) {
        int x = this.leftPos + TAB_X;
        int y = this.topPos + tabY;
        boolean selected = this.activePage == page;
        if (!selected) {
            x += 1;
        }
        guiGraphics.blit(selected ? TAB_SELECTED_TEXTURE : TAB_TEXTURE, x, y, 0, 0, TAB_WIDTH, TAB_HEIGHT, TAB_WIDTH, TAB_HEIGHT);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + 9, y + 10, 210.0F);
        guiGraphics.pose().scale(0.75F, 0.75F, 1.0F);
        guiGraphics.drawString(this.font, label, 0, 0, 0x101010, false);
        guiGraphics.pose().popPose();
    }

    private boolean renderUpgradeTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.categorySelection && this.isHoveringBackButton(mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal("Back").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            return true;
        }
        if (!this.categorySelection && this.isHoveringReroll(mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("screen.gatesofavarice.shopkeeper.reroll"),
                    Component.translatable("screen.gatesofavarice.shopkeeper.reroll_cost", this.menu.getRerollCost()).withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.literal("Rerolls Left: " + this.menu.getRemainingRerolls()).withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
            return true;
        }
        int hoveredIndex = this.getUpgradeSelectionIndex(mouseX, mouseY);
        if (hoveredIndex >= 0) {
            if (this.categorySelection) {
                guiGraphics.renderComponentTooltip(this.font, List.of(
                        Component.literal(fullCategoryName(hoveredIndex)).withStyle(ChatFormatting.GOLD),
                        Component.literal(categorySummary(hoveredIndex)).withStyle(ChatFormatting.GRAY)
                ), mouseX, mouseY);
                return true;
            }
            if (hoveredIndex < this.upgradeCards.size()) {
                UpgradeCard card = this.upgradeCards.get(hoveredIndex);
                guiGraphics.renderComponentTooltip(this.font, List.of(
                        Component.literal(card.title()).withStyle(ChatFormatting.GOLD),
                        Component.literal(card.changeLabel()).withStyle(ChatFormatting.WHITE),
                        Component.literal(card.currentValue() + " -> " + card.newValue()).withStyle(ChatFormatting.GRAY),
                        Component.literal("Cost: " + card.cost() + " Mythic Coins").withStyle(card.cost() > this.menu.getWalletBalance() ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE)
                ), mouseX, mouseY);
                return true;
            }
        }
        return false;
    }

    private boolean renderWalletTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        WalletLayout walletLayout = this.getWalletLayout();
        if (!walletLayout.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        ItemStack coinStack = coinStack();
        guiGraphics.renderTooltip(this.font, List.of(
                Component.literal(this.menu.usesDungeonTokens() ? "Dungeon Tokens" : "Mythic Coins"),
                Component.literal(Integer.toString(this.menu.getWalletBalance())).withStyle(ChatFormatting.LIGHT_PURPLE)
        ), coinStack.getTooltipImage(), coinStack, mouseX, mouseY);
        return true;
    }

    private void loadUpgradeState() {
        this.categorySelection = DungeonUpgradeClientState.categorySelection;
        this.upgradePreviewStack = DungeonUpgradeClientState.previewStack.copy();
        this.upgradeCards = List.copyOf(DungeonUpgradeClientState.cards);
        this.selectedCardCount = DungeonUpgradeClientState.selectedCardCount;
        this.maxCardSelections = Math.max(0, DungeonUpgradeClientState.maxCardSelections);
        this.runeSlotsUsed = Math.max(0, DungeonUpgradeClientState.runeSlotsUsed);
        this.runeSlotsCapacity = Math.max(0, DungeonUpgradeClientState.runeSlotsCapacity);
    }

    private void handleUpgradeClick(int index) {
        if (this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }
        if (this.animationState != AnimationState.IDLE) {
            return;
        }
        if (this.categorySelection) {
            if (index >= 0 && index < UpgradeCategory.values().length) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ShopkeeperMenu.CATEGORY_BUTTON_ID_OFFSET + index);
            }
            return;
        }
        if (index < 0 || index >= this.upgradeCards.size()) {
            return;
        }
        if (this.selectedCardCount >= this.maxCardSelections) {
            return;
        }
        UpgradeCard card = this.upgradeCards.get(index);
        if (this.isBlockedByRuneSlots(card)) {
            return;
        }
        if (card.cost() > this.menu.getWalletBalance()) {
            return;
        }
        this.coinTargetX = this.leftPos + BUY_AREA_LEFT + buyAreaWidth() / 2.0F;
        this.coinTargetY = this.topPos + BUY_AREA_TOP + buyAreaHeight() / 2.0F;
        this.createBuyCoinFlights(card.cost(), 1);
        this.selectedCardIndex = index;
        this.pendingButtonId = ShopkeeperMenu.CARD_BUTTON_ID_OFFSET + index;
        this.animationState = AnimationState.SELECTING;
        this.animationTick = 0;
    }

    private void updateBuyButton() {
        if (this.buyButton == null) {
            return;
        }
        this.buyButton.visible = false;
        this.buyButton.active = false;
        this.buyButton.setMessage(Component.translatable("screen.gatesofavarice.shopkeeper.buy").setStyle(Style.EMPTY.withColor(0xD05050)));
    }

    private void updateNextWaveButton() {
        if (this.nextWaveButton == null) {
            return;
        }
        boolean show = this.menu.canStartNextWave();
        this.nextWaveButton.visible = show;
        this.nextWaveButton.active = show;
    }

    private boolean isBlockedByRuneSlots(UpgradeCard card) {
        return (card.type() == com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCardType.ADD_OR_UPGRADE_EFFECT
                || card.type() == com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCardType.ADD_NEW_RUNE_STAT)
                && this.runeSlotsCapacity > 0
                && this.runeSlotsUsed >= this.runeSlotsCapacity;
    }

    private void renderUpgradePreviewTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<Component> lines = buildModifiedStatsTooltip(this.upgradePreviewStack, this.runeSlotsUsed, this.runeSlotsCapacity);
        guiGraphics.renderComponentTooltip(this.font, lines, mouseX, liftedTooltipY(mouseY, lines.size()));
    }

    private static List<Component> buildModifiedStatsTooltip(ItemStack stack, int runeSlotsUsed, int runeSlotsCapacity) {
        ArrayList<Component> lines = new ArrayList<>();
        int available = Math.max(0, runeSlotsCapacity - runeSlotsUsed);
        ChatFormatting color = available <= 0 && runeSlotsCapacity > 0 ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE;
        lines.add(Component.literal("Rune Slots: " + runeSlotsUsed + "/" + runeSlotsCapacity).withStyle(color));
        lines.add(Component.literal("Available Slots: " + available).withStyle(color));
        RuneStats stats = RuneStats.get(stack);
        if (!stats.view().isEmpty()) {
            lines.add(Component.literal("Modified Stats").withStyle(ChatFormatting.GRAY));
            stats.view().forEach((type, value) -> lines.add(Component.literal(formatRuneStat(type, value)).withStyle(ChatFormatting.AQUA)));
        }
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (!enchantments.keySet().isEmpty()) {
            lines.add(Component.literal("Rune Effects").withStyle(ChatFormatting.GRAY));
            for (var holder : enchantments.keySet()) {
                String effectName = holder.unwrapKey()
                        .map(key -> titleCase(key.location().getPath().replace('_', ' ')))
                        .orElse("Effect");
                lines.add(Component.literal(effectName + " " + enchantments.getLevel(holder)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        if (lines.size() == 2) {
            lines.add(Component.literal("No modified stats").withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private static int liftedTooltipY(int mouseY, int lineCount) {
        return Math.max(8, mouseY - 18 - lineCount * 10);
    }

    private static String formatRuneStat(RuneStatType type, float value) {
        String label = titleCase(type.id().replace('_', ' '));
        if (isPercentLike(type.id())) {
            return label + ": +" + String.format(Locale.ROOT, "%.1f%%", value);
        }
        return label + ": +" + String.format(Locale.ROOT, "%.1f", value);
    }

    private static boolean isPercentLike(String id) {
        return id.contains("chance")
                || id.contains("resistance")
                || id.contains("speed")
                || id.contains("range")
                || id.contains("jump")
                || id.contains("knockback")
                || id.contains("sweeping");
    }

    private static String titleCase(String raw) {
        String[] words = raw.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    private void startNextWave() {
        if (this.minecraft == null || this.minecraft.gameMode == null || !this.menu.canStartNextWave()) {
            return;
        }
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ShopkeeperMenu.START_NEXT_WAVE_BUTTON_ID);
    }

    private void sellStagedItems() {
        if (this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }
        int totalValue = this.menu.getSellValue();
        if (totalValue <= 0) {
            return;
        }
        this.createCoinFlights();
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ShopkeeperMenu.SELL_BUTTON_ID);
    }

    private void createCoinFlights() {
        this.coinFlights.clear();
        int totalParticles = 0;
        for (int slotIndex = 0; slotIndex < ShopkeeperMenu.SELL_SLOT_COUNT; slotIndex++) {
            ItemStack stack = this.menu.getSellStack(slotIndex);
            int stackValue = GatewaySellValues.getStackValue(stack);
            if (stack.isEmpty() || stackValue <= 0) {
                continue;
            }
            Slot slot = this.menu.slots.get(slotIndex);
            int particles = Math.min(SELL_COIN_PARTICLE_LIMIT, Math.max(1, stackValue));
            for (int particleIndex = 0; particleIndex < particles; particleIndex++) {
                float offsetX = ((particleIndex % 4) - 1.5F) * 2.0F;
                float offsetY = ((particleIndex / 4) - 1.0F) * 2.0F;
                this.coinFlights.add(new CoinFlight(
                        this.leftPos + slot.x + 8.0F + offsetX,
                        this.topPos + slot.y + 8.0F + offsetY,
                        this.getWalletIconX() + 4.0F,
                        this.topPos + 10.0F,
                        totalParticles + particleIndex,
                        particleIndex == particles - 1 && slotIndex == this.findLastSellSlot()
                ));
            }
            totalParticles += particles;
        }
    }

    private void createBuyCoinFlights(int unitCost, int purchaseCount) {
        int totalCost = unitCost * purchaseCount;
        int existing = this.coinFlights.size();
        int particles = Math.min(SELL_COIN_PARTICLE_LIMIT, Math.max(1, totalCost));
        float startX = this.getWalletIconX() + 4.0F;
        float startY = this.topPos + 10.0F;
        float endX = this.coinTargetX;
        float endY = this.coinTargetY;
        for (int index = 0; index < particles; index++) {
            this.coinFlights.add(new CoinFlight(startX, startY, endX, endY, existing + index, false));
        }
    }

    private int findLastSellSlot() {
        for (int slotIndex = ShopkeeperMenu.SELL_SLOT_COUNT - 1; slotIndex >= 0; slotIndex--) {
            if (GatewaySellValues.getStackValue(this.menu.getSellStack(slotIndex)) > 0) {
                return slotIndex;
            }
        }
        return -1;
    }

    private void tickCoinFlights() {
        for (int index = this.coinFlights.size() - 1; index >= 0; index--) {
            CoinFlight flight = this.coinFlights.get(index);
            flight.age++;
            if (flight.age >= flight.duration) {
                this.coinFlights.remove(index);
                if (flight.finalFlight) {
                    this.playDing(1.2F);
                    this.walletPulseTicks = 8;
                }
            }
        }
    }

    private void renderCoinFlights(GuiGraphics guiGraphics, float partialTick) {
        for (CoinFlight flight : this.coinFlights) {
            float progress = Mth.clamp((flight.age + partialTick) / flight.duration, 0.0F, 1.0F);
            float x = this.getCoinFlightX(flight, progress);
            float y = this.getCoinFlightY(flight, progress);
            this.renderCoinTrail(guiGraphics, flight, progress);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 260.0F);
            guiGraphics.pose().scale(0.6F, 0.6F, 1.0F);
            guiGraphics.renderItem(coinStack(), 0, 0);
            guiGraphics.pose().popPose();
        }
    }

    private void renderCoinTrail(GuiGraphics guiGraphics, CoinFlight flight, float progress) {
        for (int segment = COIN_TRAIL_SEGMENTS; segment >= 1; segment--) {
            float trailProgress = Mth.clamp(progress - (segment * 0.08F), 0.0F, 1.0F);
            float trailX = this.getCoinFlightX(flight, trailProgress);
            float trailY = this.getCoinFlightY(flight, trailProgress);
            int alpha = Math.max(24, 140 - (segment * 28));
            int size = Math.max(1, 4 - segment / 2);
            int color = (alpha << 24) | 0xC48CFF;
            guiGraphics.fill(Mth.floor(trailX) + 3, Mth.floor(trailY) + 3, Mth.floor(trailX) + 3 + size, Mth.floor(trailY) + 3 + size, color);
        }
    }

    private float getCoinFlightX(CoinFlight flight, float progress) {
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        float baseX = Mth.lerp(eased, flight.startX, flight.endX);
        float arc = Mth.sin(progress * Mth.PI);
        return baseX + (flight.scatterX * arc) + Mth.sin((progress * Mth.TWO_PI) + flight.wobblePhase) * flight.wobbleAmount;
    }

    private float getCoinFlightY(CoinFlight flight, float progress) {
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        float baseY = Mth.lerp(eased, flight.startY, flight.endY);
        float arc = Mth.sin(progress * Mth.PI);
        return baseY - (flight.arcHeight * arc) + (flight.scatterY * arc) * 0.35F;
    }

    private void renderUnsellableOverlays(GuiGraphics guiGraphics) {
        for (int slotIndex = ShopkeeperMenu.SELL_SLOT_COUNT; slotIndex < this.menu.slots.size(); slotIndex++) {
            Slot slot = this.menu.slots.get(slotIndex);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || GatewaySellValues.isSellable(stack)) {
                continue;
            }
            guiGraphics.fill(this.leftPos + slot.x, this.topPos + slot.y, this.leftPos + slot.x + 16, this.topPos + slot.y + 16, 0xCC303030);
        }
    }

    private int getUpgradeSelectionIndex(double mouseX, double mouseY) {
        if (this.categorySelection) {
            int count = UpgradeCategory.values().length;
            int totalWidth = Math.round(count * CARD_W * CATEGORY_CARD_SCALE + Math.max(0, count - 1) * CATEGORY_CARD_GAP);
            int startX = this.leftPos + BUY_AREA_LEFT + (buyAreaWidth() - totalWidth) / 2;
            int y = this.topPos + BUY_AREA_TOP + 2;
            for (int i = 0; i < count; i++) {
                int x = startX + Math.round(i * CARD_W * CATEGORY_CARD_SCALE) + (i * CATEGORY_CARD_GAP);
                if (this.isMouseOverScaledCard(mouseX, mouseY, x, y, CATEGORY_CARD_SCALE)) {
                    return i;
                }
            }
            return -1;
        }

        int count = Math.min(5, this.upgradeCards.size());
        int totalWidth = Math.round(count * CARD_W * UPGRADE_CARD_SCALE + Math.max(0, count - 1) * UPGRADE_CARD_GAP);
        int startX = this.leftPos + BUY_AREA_LEFT + (buyAreaWidth() - totalWidth) / 2;
        int y = this.topPos + BUY_AREA_TOP + 12;
        for (int i = 0; i < count; i++) {
            int x = startX + Math.round(i * CARD_W * UPGRADE_CARD_SCALE) + (i * UPGRADE_CARD_GAP);
            if (this.isMouseOverScaledCard(mouseX, mouseY, x, y, UPGRADE_CARD_SCALE)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isMouseOverScaledCard(double mouseX, double mouseY, int x, int y, float scale) {
        int width = Math.round(CARD_W * scale);
        int height = Math.round(CARD_H * scale);
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean isHoveringReroll(double mouseX, double mouseY) {
        int x = this.leftPos + REROLL_X;
        int y = this.topPos + REROLL_Y;
        return mouseX >= x && mouseX < x + REROLL_RENDER_SIZE && mouseY >= y && mouseY < y + REROLL_RENDER_SIZE;
    }

    private boolean isHoveringBackButton(double mouseX, double mouseY) {
        int x = this.leftPos + BACK_BUTTON_X;
        int y = this.topPos + BACK_BUTTON_Y;
        int width = Math.round(BACK_BUTTON_W * BACK_BUTTON_SCALE);
        int height = Math.round(BACK_BUTTON_H * BACK_BUTTON_SCALE);
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean isHoveringSellButton(double mouseX, double mouseY) {
        int x = this.leftPos + SELL_BUTTON_X;
        int y = this.topPos + SELL_BUTTON_Y;
        return mouseX >= x && mouseX < x + SELL_BUTTON_WIDTH && mouseY >= y && mouseY < y + SELL_BUTTON_HEIGHT;
    }

    private boolean isHoveringSellCoin(double mouseX, double mouseY) {
        int x = this.leftPos + SELL_COIN_X - 9;
        int y = this.topPos + SELL_COIN_Y - 9;
        return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
    }

    private boolean clickTab(double mouseX, double mouseY) {
        if (this.isHoveringTab(mouseX, mouseY, TAB_BUY_Y)) {
            this.setActivePage(Page.BUY);
            return true;
        }
        if (this.isHoveringTab(mouseX, mouseY, TAB_SELL_Y)) {
            this.setActivePage(Page.SELL);
            return true;
        }
        return false;
    }

    private boolean isHoveringTab(double mouseX, double mouseY, int tabY) {
        int x = this.leftPos + TAB_X;
        int y = this.topPos + tabY;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + TAB_HEIGHT;
    }

    private void setActivePage(Page page) {
        if (this.activePage == page) {
            return;
        }
        this.activePage = page;
        this.sellHolding = false;
        this.sellHoldTicks = 0.0F;
        this.coinFlights.clear();
        this.applyPageLayout();
        this.updateBuyButton();
        this.updateNextWaveButton();
    }

    private void applyPageLayout() {
        this.menu.setSellPageActive(this.activePage == Page.SELL);
    }

    private static ResourceLocation resolveCardTexture(UpgradeCard card, boolean hovered) {
        return switch (card.title()) {
            case "Effect Card" -> hovered ? EFFECT_CARD_HOVERED : EFFECT_CARD;
            case "Damage Card", "Offence Card" -> hovered ? DAMAGE_CARD_HOVERED : DAMAGE_CARD;
            case "Stat Card" -> hovered ? STAT_CARD_HOVERED : STAT_CARD;
            case "Ability Card" -> hovered ? EFFECT_CARD_HOVERED : EFFECT_CARD;
            case "Skill Card" -> hovered ? STAT_CARD_HOVERED : STAT_CARD;
            case "Restock Card" -> hovered ? STAT_CARD_HOVERED : STAT_CARD;
            case "Reroll Primary Weapon" -> hovered ? TAROT_CARD_HOVERED : TAROT_CARD;
            case "Reroll Secondary Weapon" -> hovered ? DAMAGE_CARD_HOVERED : DAMAGE_CARD;
            default -> hovered ? UPGRADE_CARD_HOVERED : UPGRADE_CARD;
        };
    }

    private static ResourceLocation resolveCardIcon(UpgradeCard card) {
        String iconName = switch (card.changeLabel()) {
            case "Restock", "Apple Bundle", "Arrow Bundle", "Food Bundle" -> "capacity";
            case "Magnet" -> "movement_speed";
            case "Food", "Heart Fragment", "Heart Fragments" -> "health";
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
            case "Rampage" -> "attack_speed";
            case "Strength" -> "attack_damage";
            case "Agility" -> "movement_speed";
            case "Poison" -> "poison_chance";
            case "Fire" -> "flame";
            case "Ice" -> "freezing_chance";
            case "Lightning" -> "shocking_chance";
            case "Force" -> "aegis";
            case "Wind" -> "withering_chance";
            case "Magic" -> "power";
            case "Power" -> "power";
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
            default -> card.title().equals("Food Card") || card.title().equals("Heart Fragment Card") ? "health" : inferIconName(card.changeLabel());
        };
        return ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/dungeon/icons/" + iconName + ".png");
    }

    private static String inferIconName(String label) {
        return switch (label) {
            case "supply" -> "capacity";
            case "current" -> "power";
            default -> label.toLowerCase(Locale.ROOT).replace(' ', '_');
        };
    }

    private void drawCard(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, float scale) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.blit(texture, 0, 0, 0, 0, CARD_W, CARD_H, CARD_W, CARD_H);
        guiGraphics.pose().popPose();
    }

    private void drawScaledCentered(GuiGraphics guiGraphics, String text, int x, int y, float scale, int color) {
        int width = this.font.width(text);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x - (width * scale) / 2.0F, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(this.font, text, 0, 0, color, false);
        guiGraphics.pose().popPose();
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

    private static String fullCategoryName(int index) {
        return switch (index) {
            case 0 -> "Primary Weapon";
            case 1 -> "Secondary Weapon";
            case 2 -> "Armor";
            case 3 -> "Item";
            default -> "";
        };
    }

    private static String categorySummary(int index) {
        return switch (index) {
            case 0 -> "Upgrade primary weapon";
            case 1 -> "Upgrade secondary weapon";
            case 2 -> "Upgrade armor set";
            case 3 -> "Utility and rerolls";
            default -> "";
        };
    }

    private ItemStack categoryIcon(int index) {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null && minecraft.player != null) {
            if (index == 0 || index == 1) {
                String role = index == 0 ? DungeonBoundItems.PRIMARY_WEAPON_ROLE : DungeonBoundItems.SECONDARY_WEAPON_ROLE;
                ItemStack roleStack = this.findWeaponByRole(role);
                if (!roleStack.isEmpty()) {
                    return roleStack;
                }
                ItemStack heldFallback = index == 0 ? minecraft.player.getMainHandItem() : minecraft.player.getOffhandItem();
                if (!heldFallback.isEmpty()) {
                    return heldFallback;
                }
            }
            if (index == 2) {
                ItemStack armorStack = this.findArmorIcon();
                if (!armorStack.isEmpty()) {
                    return armorStack;
                }
            }
        }
        return switch (index) {
            case 0, 1 -> new ItemStack(Items.IRON_SWORD);
            case 2 -> new ItemStack(Items.IRON_CHESTPLATE);
            case 3 -> new ItemStack(ModItems.LOOTBOX.get());
            default -> ItemStack.EMPTY;
        };
    }

    private ItemStack findWeaponByRole(String role) {
        Minecraft minecraft = this.minecraft;
        if (minecraft == null || minecraft.player == null) {
            return ItemStack.EMPTY;
        }
        for (ItemStack stack : minecraft.player.getInventory().items) {
            if (!stack.isEmpty() && role.equals(DungeonBoundItems.getWeaponRole(stack))) {
                return stack;
            }
        }
        for (ItemStack stack : minecraft.player.getInventory().offhand) {
            if (!stack.isEmpty() && role.equals(DungeonBoundItems.getWeaponRole(stack))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack findArmorIcon() {
        Minecraft minecraft = this.minecraft;
        if (minecraft == null || minecraft.player == null) {
            return ItemStack.EMPTY;
        }
        for (ItemStack stack : minecraft.player.getInventory().armor) {
            if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem armorItem && armorItem.getType() == ArmorItem.Type.CHESTPLATE) {
                return stack;
            }
        }
        for (ItemStack stack : minecraft.player.getInventory().armor) {
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private int buyAreaWidth() {
        return BUY_AREA_RIGHT - BUY_AREA_LEFT;
    }

    private int buyAreaHeight() {
        return BUY_AREA_BOTTOM - BUY_AREA_TOP;
    }

    private int getWalletIconX() {
        return this.getWalletLayout().iconX();
    }

    private WalletLayout getWalletLayout() {
        String walletText = formatCompactValue(this.menu.getWalletBalance());
        int textWidth = Math.round(this.font.width(walletText) * 0.75F);
        int textX = this.leftPos + this.imageWidth - 8 - textWidth;
        int iconX = textX - 5;
        int iconY = this.topPos + 4;
        int textY = this.topPos + 5;
        int hoverLeft = iconX - 2;
        int hoverRight = textX + textWidth + 2;
        int hoverTop = this.topPos + 2;
        int hoverBottom = this.topPos + 15;
        return new WalletLayout(walletText, textX + 3, textY, iconX, iconY, hoverLeft, hoverTop, hoverRight, hoverBottom);
    }

    private static String formatCompactValue(int value) {
        if (value < 1000) {
            return Integer.toString(value);
        }
        if (value < 1_000_000) {
            return trimCompactDecimal(value / 1000.0D, value < 10_000 ? 1 : 2) + "k";
        }
        return trimCompactDecimal(value / 1_000_000.0D, value < 100_000_000 ? 1 : 2) + "M";
    }

    private static String trimCompactDecimal(double value, int maxDecimals) {
        String format = "%." + maxDecimals + "f";
        String text = String.format(Locale.ROOT, format, value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private void playDing(float pitch) {
        if (this.minecraft == null) {
            return;
        }
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, pitch));
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

    private enum Page {
        BUY,
        SELL
    }

    private enum AnimationState {
        DRAWING,
        IDLE,
        REROLLING,
        SELECTING
    }

    private static final class CoinFlight {
        private final float startX;
        private final float startY;
        private final float endX;
        private final float endY;
        private final float scatterX;
        private final float scatterY;
        private final float arcHeight;
        private final float wobblePhase;
        private final float wobbleAmount;
        private final int duration;
        private final boolean finalFlight;
        private int age;

        private CoinFlight(float startX, float startY, float endX, float endY, int index, boolean finalFlight) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.scatterX = (((index % 7) - 3.0F) * 5.0F) + (((index / 3) % 3) - 1.0F) * 3.0F;
            this.scatterY = (((index % 5) - 2.0F) * 3.0F);
            this.arcHeight = 8.0F + (index % 5) * 3.0F;
            this.wobblePhase = index * 0.85F;
            this.wobbleAmount = 1.5F + (index % 4) * 0.45F;
            this.duration = 12 + Math.min(index, 12);
            this.finalFlight = finalFlight;
        }
    }

    private record WalletLayout(String text, int textX, int textY, int iconX, int iconY, int hoverLeft, int hoverTop, int hoverRight, int hoverBottom) {
        private boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= this.hoverLeft && mouseX <= this.hoverRight && mouseY >= this.hoverTop && mouseY <= this.hoverBottom;
        }
    }

    private static ItemStack coinStack() {
        return new ItemStack(ModItems.MYTHIC_COIN.get());
    }
}
