package com.revilo.gatesofavarice.client.screen;

import com.mojang.math.Axis;
import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.menu.ShopkeeperMenu;
import com.revilo.gatesofavarice.registry.ModItems;
import com.revilo.gatesofavarice.shop.GatewaySellValues;
import com.revilo.gatesofavarice.shop.ShopOfferDefinition;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class ShopkeeperScreen extends AbstractContainerScreen<ShopkeeperMenu> {

    private static final ResourceLocation BUY_GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/shop/shop-gui.png");
    private static final ResourceLocation SELL_GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/shop/shop-sell-gui.png");
    private static final ResourceLocation REROLL_TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/shop/re-roll.png");
    private static final ResourceLocation REROLL_DISABLED_TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/shop/re-roll-disabled.png");
    private static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/shop/slot.png");
    private static final ResourceLocation UNPURCHASABLE_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/shop/slot-unpurchaseable.png");
    private static final ResourceLocation LOCKED_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/shop/slot-locked.png");
    private static final ResourceLocation TAB_TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/shop/tab.png");
    private static final ResourceLocation TAB_SELECTED_TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/shop/tab_selected.png");
    private static final int GRID_COLUMNS = 5;
    private static final int GRID_START_X = 9;
    private static final int GRID_START_Y = 16;
    private static final int SLOT_SPACING_X = 25;
    private static final int SLOT_SPACING_Y = 25;
    private static final int SLOT_SIZE = 24;
    private static final int SLOT_ITEM_SIZE = 16;
    private static final int SELECTED_ITEM_X = 146;
    private static final int SELECTED_ITEM_Y = 24;
    private static final int BUY_BUTTON_X = 137;
    private static final int BUY_BUTTON_Y = 56;
    private static final int BUY_BUTTON_WIDTH = 34;
    private static final int BUY_BUTTON_HEIGHT = 17;
    private static final int BUY_PRICE_X = 148;
    private static final int BUY_PRICE_Y = 44;
    private static final int BUY_PRICE_ICON_SHIFT_X = -10;
    private static final int REROLL_X = 123;
    private static final int REROLL_Y = 68;
    private static final int REROLL_RENDER_SIZE = 12;
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
    private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED_TEXTURE = ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final ResourceLocation BUTTON_DISABLED_TEXTURE = ResourceLocation.withDefaultNamespace("widget/button_disabled");
    private float selectedItemHoverScale = 1.0F;

    private final List<CoinFlight> coinFlights = new ArrayList<>();
    private Button buyButton;
    private Button nextWaveButton;
    private Page activePage = Page.BUY;
    private int selectedSlot = 0;
    private int lastWalletBalance;
    private int walletPulseTicks;
    private float sellHoldTicks;
    private boolean sellHolding;
    private boolean coinHoveredLastFrame;
    private double lastMouseX;
    private double lastMouseY;

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
        this.selectedSlot = this.findInitialSelection();
        this.buyButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.gatesofavarice.shopkeeper.buy"), button -> this.buySelectedOffer())
                .pos(this.leftPos + BUY_BUTTON_X, this.topPos + BUY_BUTTON_Y)
                .size(BUY_BUTTON_WIDTH, BUY_BUTTON_HEIGHT)
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
        if (this.menu.getOfferDefinition(this.selectedSlot) == null) {
            this.selectedSlot = this.findInitialSelection();
        }

        if (this.walletPulseTicks > 0) {
            this.walletPulseTicks--;
        }

        int walletBalance = this.menu.getWalletBalance();
        if (walletBalance > this.lastWalletBalance) {
            this.walletPulseTicks = 8;
        }
        this.lastWalletBalance = walletBalance;
        this.selectedItemHoverScale = Mth.lerp(0.25F, this.selectedItemHoverScale, 1.0F);

        if (this.activePage == Page.SELL && this.sellHolding) {
            Minecraft minecraft = this.minecraft;
            long window = minecraft != null ? minecraft.getWindow().getWindow() : 0L;
            boolean stillDown = window != 0L && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            if (!stillDown || !this.isHoveringSellButton(this.lastMouseX, this.lastMouseY)) {
                this.sellHolding = false;
                this.sellHoldTicks = 0.0F;
            }
            else {
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

        if (this.activePage == Page.BUY && this.renderOfferTooltip(guiGraphics, mouseX, mouseY)) {
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
            this.renderOfferGrid(guiGraphics, mouseX, mouseY);
            this.renderSelectedOfferPanel(guiGraphics, mouseX, mouseY, partialTick);
            this.renderRerollButton(guiGraphics);
        }
        else {
            this.renderSellPanel(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.translatable("screen.gatesofavarice.shopkeeper.title"), 6, 4, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 74, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (this.clickTab(mouseX, mouseY)) {
                return true;
            }

            if (this.activePage == Page.BUY) {
                if (this.isHoveringReroll(mouseX, mouseY) && this.minecraft != null && this.minecraft.gameMode != null && this.menu.canAffordReroll()) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, ShopkeeperMenu.REROLL_BUTTON_ID);
                    return true;
                }

                int hoveredSlot = this.getOfferSlotAt(mouseX, mouseY);
                if (hoveredSlot >= 0 && this.menu.isOfferSlotUnlocked(hoveredSlot) && this.menu.getOfferDefinition(hoveredSlot) != null) {
                    this.selectedSlot = hoveredSlot;
                    this.updateBuyButton();
                    return true;
                }
            }
            else if (this.isHoveringSellButton(mouseX, mouseY) && this.menu.getSellValue() > 0) {
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

    private void renderOfferGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (int slotIndex = 0; slotIndex < ShopkeeperMenu.GRID_SLOT_COUNT; slotIndex++) {
            int slotX = this.leftPos + GRID_START_X + (slotIndex % GRID_COLUMNS) * SLOT_SPACING_X;
            int slotY = this.topPos + GRID_START_Y + (slotIndex / GRID_COLUMNS) * SLOT_SPACING_Y;
            boolean unlocked = this.menu.isOfferSlotUnlocked(slotIndex);
            ShopOfferDefinition offer = this.menu.getOfferDefinition(slotIndex);
            boolean outOfStock = offer == null || this.menu.getOfferStock(slotIndex) <= 0;
            boolean unaffordable = offer != null && !this.menu.canAfford(slotIndex);

            ResourceLocation slotTexture = SLOT_TEXTURE;
            if (!unlocked) {
                slotTexture = LOCKED_SLOT_TEXTURE;
            }
            else if (outOfStock || unaffordable) {
                slotTexture = UNPURCHASABLE_SLOT_TEXTURE;
            }

            guiGraphics.blit(slotTexture, slotX, slotY, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);

            if (slotIndex == this.selectedSlot && offer != null) {
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, 0x55FFFFFF);
            }

            if (offer == null || !unlocked) {
                continue;
            }

            guiGraphics.renderItem(offer.previewStack(), slotX + (SLOT_SIZE - SLOT_ITEM_SIZE) / 2, slotY + 4);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(slotX + 15, slotY + 15, 220.0F);
            guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
            String stockText = Integer.toString(this.menu.getOfferStock(slotIndex));
            guiGraphics.drawString(this.font, stockText, -1, 0, 0x101010, false);
            guiGraphics.drawString(this.font, stockText, 1, 0, 0x101010, false);
            guiGraphics.drawString(this.font, stockText, 0, -1, 0x101010, false);
            guiGraphics.drawString(this.font, stockText, 0, 1, 0x101010, false);
            guiGraphics.drawString(this.font, stockText, 0, 0, 0xFFFFFF, false);
            guiGraphics.pose().popPose();
        }
    }

    private void renderSellPanel(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean coinHovered = this.isHoveringSellCoin(mouseX, mouseY);
        if (coinHovered && !this.coinHoveredLastFrame) {
            this.playDing(1.15F);
        }
        this.coinHoveredLastFrame = coinHovered;

        float hoverScale = coinHovered ? 1.1F : 1.0F;
        WorkbenchCrystalRenderer.render(guiGraphics, coinStack(), this.leftPos + SELL_COIN_X, this.topPos + SELL_COIN_Y, partialTick, hoverScale * 0.9F, WorkbenchCrystalRenderer.BASE_SPIN_SPEED);

        this.renderCoinPrice(guiGraphics, this.leftPos + SELL_TOTAL_X, this.topPos + SELL_TOTAL_Y, this.menu.getSellValue(), 0.75F, this.menu.getSellValue() > 0 ? 0xB06CFF : 0x7B6A8D, SELL_PRICE_ICON_SHIFT_X);

        int buttonLeft = this.leftPos + SELL_BUTTON_X;
        int buttonTop = this.topPos + SELL_BUTTON_Y;
        int buttonRight = buttonLeft + SELL_BUTTON_WIDTH;
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

    private void renderSelectedOfferPanel(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ShopOfferDefinition offer = this.getSelectedOffer();
        if (offer == null || !this.menu.isOfferSlotUnlocked(this.selectedSlot)) {
            return;
        }

        boolean hovered = this.isHoveringSelectedItem(mouseX, mouseY);
        float bob = Mth.sin((((this.minecraft != null && this.minecraft.level != null) ? this.minecraft.level.getGameTime() : 0) + partialTick) * 0.15F) * 2.0F;
        float targetScale = hovered ? 1.1F : 1.0F;
        this.selectedItemHoverScale = Mth.lerp(0.35F, this.selectedItemHoverScale, targetScale);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.leftPos + SELECTED_ITEM_X + 8.0F, this.topPos + SELECTED_ITEM_Y + 8.0F + bob, 220.0F);
        guiGraphics.pose().scale(this.selectedItemHoverScale, this.selectedItemHoverScale, 1.0F);
        guiGraphics.renderItem(offer.previewStack(), -8, -8);
        guiGraphics.pose().popPose();
        int slotPrice = this.menu.getOfferPrice(this.selectedSlot);
        this.renderCoinPrice(guiGraphics, this.leftPos + BUY_PRICE_X, this.topPos + BUY_PRICE_Y, slotPrice, 0.75F, this.menu.canAfford(this.selectedSlot) ? 0xB06CFF : 0xE07A7A, BUY_PRICE_ICON_SHIFT_X);
    }

    private void renderCoinPrice(GuiGraphics guiGraphics, int x, int y, int price, float scale, int textColor) {
        this.renderCoinPrice(guiGraphics, x, y, price, scale, textColor, 0);
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
        boolean active = this.menu.hasRerollsRemaining() && this.menu.canAffordReroll();
        boolean hovered = this.isHoveringReroll(this.lastMouseX, this.lastMouseY);
        float scale = hovered ? 1.1F : 1.0F;
        ResourceLocation texture = active ? REROLL_TEXTURE : REROLL_DISABLED_TEXTURE;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.leftPos + REROLL_X + (REROLL_RENDER_SIZE * (1.0F - scale) * 0.5F), this.topPos + REROLL_Y + (REROLL_RENDER_SIZE * (1.0F - scale) * 0.5F), 150.0F);
        guiGraphics.pose().scale((REROLL_RENDER_SIZE / 16.0F) * scale, (REROLL_RENDER_SIZE / 16.0F) * scale, 1.0F);
        guiGraphics.blit(texture, 0, 0, 0, 0, 16, 16, 16, 16);
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

    private boolean renderOfferTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.isHoveringReroll(mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(
                    this.font,
                    List.of(
                            Component.translatable("screen.gatesofavarice.shopkeeper.reroll"),
                            Component.translatable("screen.gatesofavarice.shopkeeper.reroll_cost", this.menu.getRerollCost()).withStyle(ChatFormatting.LIGHT_PURPLE),
                            Component.literal("Rerolls Left: " + this.menu.getRemainingRerolls()).withStyle(ChatFormatting.GRAY)
                    ),
                    mouseX,
                    mouseY
            );
            return true;
        }

        int hoveredSlot = this.getOfferSlotAt(mouseX, mouseY);
        if (hoveredSlot >= 0) {
            if (!this.menu.isOfferSlotUnlocked(hoveredSlot)) {
                guiGraphics.renderComponentTooltip(
                        this.font,
                        List.of(
                                Component.translatable("screen.gatesofavarice.shopkeeper.locked"),
                                Component.translatable("screen.gatesofavarice.shopkeeper.unlocks_at", this.menu.getRequiredLevelForSlot(hoveredSlot))
                        ),
                        mouseX,
                        mouseY
                );
                return true;
            }

            ShopOfferDefinition hoveredOffer = this.menu.getOfferForSlot(hoveredSlot);
            if (hoveredOffer != null) {
                this.renderItemTooltip(guiGraphics, hoveredOffer.previewStack(), mouseX, mouseY, hoveredOffer.description());
                return true;
            }
        }

        ShopOfferDefinition selectedOffer = this.getSelectedOffer();
        if (selectedOffer != null && this.isHoveringSelectedItem(mouseX, mouseY)) {
            this.renderItemTooltip(guiGraphics, selectedOffer.previewStack(), mouseX, mouseY, selectedOffer.description());
            return true;
        }

        return false;
    }

    private boolean renderWalletTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        WalletLayout walletLayout = this.getWalletLayout();
        if (!walletLayout.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        ItemStack coinStack = coinStack();
        guiGraphics.renderTooltip(
                this.font,
                List.of(
                        Component.literal(this.menu.usesDungeonTokens() ? "Dungeon Tokens" : "Mythic Coins"),
                        Component.literal(Integer.toString(this.menu.getWalletBalance())).withStyle(ChatFormatting.LIGHT_PURPLE)
                ),
                coinStack.getTooltipImage(),
                coinStack,
                mouseX,
                mouseY);
        return true;
    }

    private void renderItemTooltip(GuiGraphics guiGraphics, ItemStack stack, int mouseX, int mouseY, Component description) {
        List<Component> tooltip = new ArrayList<>(Screen.getTooltipFromItem(this.minecraft, stack));
        GatewaySellValues.appendShopRuneSellValueTooltip(stack, tooltip);
        tooltip.add(Component.empty());
        tooltip.add(description.copy().withStyle(ChatFormatting.GRAY));
        guiGraphics.renderTooltip(this.font, tooltip, stack.getTooltipImage(), stack, mouseX, mouseY);
    }

    private void updateBuyButton() {
        if (this.buyButton == null) {
            return;
        }

        ShopOfferDefinition offer = this.getSelectedOffer();
        boolean canBuy = offer != null
                && this.menu.isOfferSlotUnlocked(this.selectedSlot)
                && this.menu.getOfferStock(this.selectedSlot) > 0
                && this.menu.canAfford(this.selectedSlot);
        boolean tooExpensive = offer != null && this.menu.isOfferSlotUnlocked(this.selectedSlot) && this.menu.getOfferStock(this.selectedSlot) > 0 && !this.menu.canAfford(this.selectedSlot);
        this.buyButton.visible = this.activePage == Page.BUY;
        this.buyButton.active = this.activePage == Page.BUY && canBuy;
        this.buyButton.setMessage(tooExpensive
                ? Component.translatable("screen.gatesofavarice.shopkeeper.buy").setStyle(Style.EMPTY.withColor(0xD05050))
                : Component.translatable("screen.gatesofavarice.shopkeeper.buy"));
    }

    private void buySelectedOffer() {
        ShopOfferDefinition offer = this.getSelectedOffer();
        if (offer == null
                || !this.menu.isOfferSlotUnlocked(this.selectedSlot)
                || this.menu.getOfferStock(this.selectedSlot) <= 0
                || !this.menu.canAfford(this.selectedSlot)
                || this.minecraft == null
                || this.minecraft.gameMode == null) {
            return;
        }

        int slotPrice = this.menu.getOfferPrice(this.selectedSlot);
        if (slotPrice <= 0) {
            return;
        }

        int stock = this.menu.getOfferStock(this.selectedSlot);
        int purchases = hasShiftDown() ? Math.min(stock, this.menu.getWalletBalance() / slotPrice) : 1;
        if (purchases <= 0) {
            return;
        }

        this.createBuyCoinFlights(slotPrice, purchases);
        int buttonId = hasShiftDown() ? ShopkeeperMenu.BUY_ALL_BUTTON_ID_OFFSET + this.selectedSlot : this.selectedSlot;
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
    }

    private void updateNextWaveButton() {
        if (this.nextWaveButton == null) {
            return;
        }
        boolean show = this.menu.canStartNextWave();
        this.nextWaveButton.visible = show;
        this.nextWaveButton.active = show;
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
                        particleIndex == particles - 1 && slotIndex == this.findLastSellSlot()));
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
        float endX = this.leftPos + SELECTED_ITEM_X + 8.0F;
        float endY = this.topPos + SELECTED_ITEM_Y + 8.0F;
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

    private ShopOfferDefinition getSelectedOffer() {
        return this.menu.getOfferForSlot(this.selectedSlot);
    }

    private int findInitialSelection() {
        for (int slotIndex = 0; slotIndex < ShopkeeperMenu.GRID_SLOT_COUNT; slotIndex++) {
            if (this.menu.isOfferSlotUnlocked(slotIndex) && this.menu.getOfferDefinition(slotIndex) != null) {
                return slotIndex;
            }
        }
        return 0;
    }

    private int getOfferSlotAt(double mouseX, double mouseY) {
        int localX = Mth.floor(mouseX) - this.leftPos;
        int localY = Mth.floor(mouseY) - this.topPos;
        int gridEndX = GRID_START_X + ((GRID_COLUMNS - 1) * SLOT_SPACING_X) + SLOT_SIZE;
        int gridEndY = GRID_START_Y + SLOT_SPACING_Y + SLOT_SIZE;
        if (localX < GRID_START_X || localY < GRID_START_Y || localX > gridEndX || localY > gridEndY) {
            return -1;
        }

        for (int slotIndex = 0; slotIndex < ShopkeeperMenu.GRID_SLOT_COUNT; slotIndex++) {
            int slotX = GRID_START_X + (slotIndex % GRID_COLUMNS) * SLOT_SPACING_X;
            int slotY = GRID_START_Y + (slotIndex / GRID_COLUMNS) * SLOT_SPACING_Y;
            if (localX >= slotX && localX < slotX + SLOT_SIZE && localY >= slotY && localY < slotY + SLOT_SIZE) {
                return slotIndex;
            }
        }

        return -1;
    }

    private boolean isHoveringSelectedItem(double mouseX, double mouseY) {
        int x = this.leftPos + SELECTED_ITEM_X;
        int y = this.topPos + SELECTED_ITEM_Y;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private boolean isHoveringReroll(double mouseX, double mouseY) {
        int x = this.leftPos + REROLL_X;
        int y = this.topPos + REROLL_Y;
        int expandedSize = Math.round(REROLL_RENDER_SIZE * 1.1F);
        int offset = (expandedSize - REROLL_RENDER_SIZE) / 2;
        return mouseX >= x - offset && mouseX < x - offset + expandedSize && mouseY >= y - offset && mouseY < y - offset + expandedSize;
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
        String text = String.format(java.util.Locale.ROOT, format, value);
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

    private enum Page {
        BUY,
        SELL
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
