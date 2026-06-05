package com.revilo.gatesofavarice.menu;

import com.revilo.gatesofavarice.currency.MythicCoinWallet;
import com.revilo.gatesofavarice.dungeon.DungeonBoundItems;
import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import com.revilo.gatesofavarice.entity.GatekeeperEntity;
import com.revilo.gatesofavarice.integration.LevelUpIntegration;
import com.revilo.gatesofavarice.progression.ProgressionSystem;
import com.revilo.gatesofavarice.registry.ModMenus;
import com.revilo.gatesofavarice.shop.GatewaySellValues;
import com.revilo.gatesofavarice.shop.ShopOfferDefinition;
import com.revilo.gatesofavarice.shop.ShopkeeperManager;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ShopkeeperMenu extends AbstractContainerMenu {

    public static final int GRID_SLOT_COUNT = 10;
    public static final int REROLL_BUTTON_ID = 100;
    public static final int SELL_BUTTON_ID = 101;
    public static final int START_NEXT_WAVE_BUTTON_ID = 102;
    public static final int BUY_ALL_BUTTON_ID_OFFSET = 200;
    public static final int SELL_SLOT_COUNT = 18;
    private static final int TEMP_OFFER_COUNT = GRID_SLOT_COUNT;
    private static final int DATA_REROLL_COST = 0;
    private static final int DATA_REROLL_COUNT = 1;
    private static final int DATA_TEMP_START = 2;
    private static final int DATA_STOCK_START = DATA_TEMP_START + TEMP_OFFER_COUNT;
    private static final int DATA_PRICE_START = DATA_STOCK_START + GRID_SLOT_COUNT;
    private static final int DATA_DUNGEON_NEXT_WAVE_AVAILABLE = DATA_PRICE_START + GRID_SLOT_COUNT;
    private static final int DATA_SIZE = DATA_DUNGEON_NEXT_WAVE_AVAILABLE + 1;
    private static final int SELL_GRID_X = 8;
    private static final int SELL_GRID_Y = 14;
    private static final int SELL_COLUMNS = 6;
    private static final int SELL_SLOT_SPACING = 18;
    private final Player player;
    private final int shopkeeperId;
    private final SimpleContainer sellContainer = new SimpleContainer(SELL_SLOT_COUNT);
    private final List<ToggleableSlot> sellSlots = new java.util.ArrayList<>();
    private final ContainerData syncedData = new SimpleContainerData(DATA_SIZE);
    private List<ShopOfferDefinition> cachedOffers = List.of();

    public ShopkeeperMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readInt());
    }

    public ShopkeeperMenu(int containerId, Inventory inventory, int shopkeeperId) {
        super(ModMenus.SHOPKEEPER.get(), containerId);
        this.player = inventory.player;
        this.shopkeeperId = shopkeeperId;
        this.addPlayerInventory(inventory);
        this.addDataSlots(this.syncedData);
        this.clearSyncedData();
        this.syncFromTrader();
    }

    @Override
    public void broadcastChanges() {
        if (!this.player.level().isClientSide) {
            this.syncFromTrader();
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < SELL_SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, SELL_SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        }
        else if (GatewaySellValues.isSellable(stack)) {
            if (!this.moveItemStackTo(stack, 0, SELL_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }
        else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        }
        else {
            slot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        Entity entity = serverPlayer.level().getEntity(this.shopkeeperId);
        if (!(entity instanceof GatekeeperEntity trader) || !ShopkeeperManager.isShopkeeper(trader)) {
            return false;
        }

        if (id == REROLL_BUTTON_ID) {
            boolean rerolled = ShopkeeperManager.rerollOffersWithWallet(serverPlayer, trader, false);
            if (rerolled) {
                this.syncFromTrader();
                this.broadcastChanges();
            }
            return rerolled;
        }

        if (id == SELL_BUTTON_ID) {
            int total = this.getSellValue();
            if (total <= 0) {
                return false;
            }

            for (int slotIndex = 0; slotIndex < SELL_SLOT_COUNT; slotIndex++) {
                this.sellContainer.setItem(slotIndex, ItemStack.EMPTY);
            }
            this.addCurrency(serverPlayer, total);
            this.broadcastChanges();
            return true;
        }
        if (id == START_NEXT_WAVE_BUTTON_ID) {
            return DungeonRunManager.startNextWaveFromShop(serverPlayer, this.shopkeeperId);
        }

        if (id >= BUY_ALL_BUTTON_ID_OFFSET && id < BUY_ALL_BUTTON_ID_OFFSET + GRID_SLOT_COUNT) {
            int slotIndex = id - BUY_ALL_BUTTON_ID_OFFSET;
            return this.buyAllFromSlot(serverPlayer, trader, slotIndex);
        }

        ShopOfferDefinition offer = this.getOfferForSlot(id);
        if (offer == null || id < 0 || id >= GRID_SLOT_COUNT) {
            return false;
        }

        if (this.getPlayerLevel() < this.getRequiredLevelForSlot(id)) {
            return false;
        }

        if (!ShopkeeperManager.consumeStock(trader, id)) {
            return false;
        }
        int price = this.getOfferPrice(id);
        if (price <= 0 || !this.spendCurrency(serverPlayer, price)) {
            ShopkeeperManager.restoreStock(trader, id);
            return false;
        }

        this.grantPurchasedReward(serverPlayer, offer);
        this.syncFromTrader();
        this.broadcastChanges();
        return true;
    }

    private boolean buyAllFromSlot(ServerPlayer serverPlayer, GatekeeperEntity trader, int slotIndex) {
        ShopOfferDefinition offer = this.getOfferForSlot(slotIndex);
        if (offer == null || slotIndex < 0 || slotIndex >= GRID_SLOT_COUNT) {
            return false;
        }
        if (this.getPlayerLevel() < this.getRequiredLevelForSlot(slotIndex)) {
            return false;
        }

        int price = this.getOfferPrice(slotIndex);
        if (price <= 0) {
            return false;
        }

        boolean boughtAny = false;
        while (this.getOfferStock(slotIndex) > 0 && this.getWalletBalance() >= price) {
            if (!ShopkeeperManager.consumeStock(trader, slotIndex)) {
                break;
            }
            if (!this.spendCurrency(serverPlayer, price)) {
                ShopkeeperManager.restoreStock(trader, slotIndex);
                break;
            }

            this.grantPurchasedReward(serverPlayer, offer);
            boughtAny = true;
        }

        if (boughtAny) {
            this.syncFromTrader();
            this.broadcastChanges();
        }
        return boughtAny;
    }

    @Override
    public boolean stillValid(Player player) {
        Entity entity = player.level().getEntity(this.shopkeeperId);
        return entity instanceof GatekeeperEntity trader
                && trader.isAlive()
                && ShopkeeperManager.isShopkeeper(trader)
                && player.distanceToSqr(trader) <= 64.0D;
    }

    public int getWalletBalance() {
        return MythicCoinWallet.get(this.player);
    }

    public java.util.List<ShopOfferDefinition> getOffers() {
        this.refreshOffersFromData();
        return this.cachedOffers;
    }

    public int getPlayerLevel() {
        return LevelUpIntegration.getEffectiveLevel(this.player);
    }

    public boolean isOfferSlotUnlocked(int slotIndex) {
        return this.getPlayerLevel() >= this.getRequiredLevelForSlot(slotIndex);
    }

    public int getRequiredLevelForSlot(int slotIndex) {
        this.refreshOffersFromData();
        ShopOfferDefinition offer = this.getOfferDefinition(slotIndex);
        int itemRequirement = offer == null ? 0 : offer.minLevel();
        return ProgressionSystem.shopRequiredLevel(slotIndex, itemRequirement);
    }

    public ShopOfferDefinition getOfferForSlot(int slotIndex) {
        ShopOfferDefinition offer = this.getOfferDefinition(slotIndex);
        if (offer == null || this.getOfferStock(slotIndex) <= 0) {
            return null;
        }
        return offer;
    }

    public ShopOfferDefinition getOfferDefinition(int slotIndex) {
        this.refreshOffersFromData();
        if (slotIndex < 0 || slotIndex >= this.cachedOffers.size() || slotIndex >= GRID_SLOT_COUNT) {
            return null;
        }
        return this.cachedOffers.get(slotIndex);
    }

    public int getOfferStock(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SLOT_COUNT) {
            return 0;
        }
        return Math.max(0, this.syncedData.get(DATA_STOCK_START + slotIndex));
    }

    public int getOfferPrice(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SLOT_COUNT) {
            return 0;
        }
        return Math.max(0, this.syncedData.get(DATA_PRICE_START + slotIndex));
    }

    public boolean canAfford(int slotIndex) {
        int price = this.getOfferPrice(slotIndex);
        return price > 0 && this.getWalletBalance() >= price;
    }

    public boolean canAfford(ShopOfferDefinition offer) {
        int slotIndex = this.cachedOffers.indexOf(offer);
        return offer != null && slotIndex >= 0 && this.canAfford(slotIndex);
    }

    public int getRerollCost() {
        return Math.max(0, this.syncedData.get(DATA_REROLL_COST));
    }

    public int getRerollCount() {
        return Math.max(0, this.syncedData.get(DATA_REROLL_COUNT));
    }

    public int getRemainingRerolls() {
        return Math.max(0, ShopkeeperManager.getMaxRerolls() - this.getRerollCount());
    }

    public boolean hasRerollsRemaining() {
        return this.getRemainingRerolls() > 0;
    }

    public boolean canAffordReroll() {
        return this.hasRerollsRemaining() && this.getWalletBalance() >= this.getRerollCost();
    }

    public boolean canStartNextWave() {
        return this.syncedData.get(DATA_DUNGEON_NEXT_WAVE_AVAILABLE) == 1;
    }

    public boolean usesDungeonTokens() {
        return false;
    }

    public int getSellValue() {
        int total = 0;
        for (int slotIndex = 0; slotIndex < SELL_SLOT_COUNT; slotIndex++) {
            total += GatewaySellValues.getStackValue(this.sellContainer.getItem(slotIndex));
        }
        return total;
    }

    public ItemStack getSellStack(int slotIndex) {
        return slotIndex >= 0 && slotIndex < SELL_SLOT_COUNT ? this.sellContainer.getItem(slotIndex) : ItemStack.EMPTY;
    }

    public void setSellPageActive(boolean active) {
        for (ToggleableSlot slot : this.sellSlots) {
            slot.setActive(active);
        }
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < SELL_COLUMNS; column++) {
                int slotIndex = column + row * SELL_COLUMNS;
                ToggleableSlot slot = new ToggleableSlot(this.sellContainer, slotIndex, SELL_GRID_X + column * SELL_SLOT_SPACING, SELL_GRID_Y + row * SELL_SLOT_SPACING) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return GatewaySellValues.isSellable(stack);
                    }
                };
                slot.setActive(false);
                this.sellSlots.add(slot);
                this.addSlot(slot);
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    private void clearSyncedData() {
        this.syncedData.set(DATA_REROLL_COST, 0);
        this.syncedData.set(DATA_REROLL_COUNT, 0);
        for (int index = 0; index < TEMP_OFFER_COUNT; index++) {
            this.syncedData.set(DATA_TEMP_START + index, -1);
        }
        for (int index = 0; index < GRID_SLOT_COUNT; index++) {
            this.syncedData.set(DATA_STOCK_START + index, 0);
        }
        for (int index = 0; index < GRID_SLOT_COUNT; index++) {
            this.syncedData.set(DATA_PRICE_START + index, 0);
        }
        this.syncedData.set(DATA_DUNGEON_NEXT_WAVE_AVAILABLE, 0);
        this.refreshOffersFromData();
    }

    private void syncFromTrader() {
        Entity entity = this.player.level().getEntity(this.shopkeeperId);
        if (!(entity instanceof GatekeeperEntity trader) || !ShopkeeperManager.isShopkeeper(trader)) {
            return;
        }

        this.syncedData.set(DATA_REROLL_COST, ShopkeeperManager.getRerollCost(trader));
        this.syncedData.set(DATA_REROLL_COUNT, ShopkeeperManager.getRerollCount(trader));
        int[] tempOfferIndexes = ShopkeeperManager.getTempOfferIndexes(trader);
        for (int index = 0; index < TEMP_OFFER_COUNT; index++) {
            int value = index < tempOfferIndexes.length ? tempOfferIndexes[index] : -1;
            this.syncedData.set(DATA_TEMP_START + index, value);
        }
        int[] stocks = ShopkeeperManager.getOfferStocks(trader);
        int[] prices = ShopkeeperManager.getOfferPrices(trader);
        for (int index = 0; index < GRID_SLOT_COUNT; index++) {
            int value = index < stocks.length ? stocks[index] : 0;
            this.syncedData.set(DATA_STOCK_START + index, value);
        }
        for (int index = 0; index < GRID_SLOT_COUNT; index++) {
            int value = index < prices.length ? prices[index] : 0;
            this.syncedData.set(DATA_PRICE_START + index, value);
        }
        int canStartNextWave = this.player instanceof ServerPlayer serverPlayer
                && DungeonRunManager.canOwnerStartNextWaveFromShop(serverPlayer, this.shopkeeperId) ? 1 : 0;
        this.syncedData.set(DATA_DUNGEON_NEXT_WAVE_AVAILABLE, canStartNextWave);
        this.refreshOffersFromData();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide) {
            return;
        }

        for (int slotIndex = 0; slotIndex < SELL_SLOT_COUNT; slotIndex++) {
            ItemStack stack = this.sellContainer.removeItemNoUpdate(slotIndex);
            if (stack.isEmpty()) {
                continue;
            }

            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    private static class ToggleableSlot extends Slot {
        private boolean active = true;

        private ToggleableSlot(net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean isActive() {
            return this.active;
        }

        private void setActive(boolean active) {
            this.active = active;
        }
    }

    private void refreshOffersFromData() {
        java.util.ArrayList<ShopOfferDefinition> offers = new java.util.ArrayList<>(TEMP_OFFER_COUNT);
        for (int index = 0; index < TEMP_OFFER_COUNT; index++) {
            int offerIndex = this.syncedData.get(DATA_TEMP_START + index);
            List<ShopOfferDefinition> allOffers = ShopOfferDefinition.allOffers();
            if (offerIndex >= 0 && offerIndex < allOffers.size()) {
                offers.add(allOffers.get(offerIndex));
            }
        }
        this.cachedOffers = List.copyOf(offers);
    }

    private boolean spendCurrency(ServerPlayer player, int amount) {
        return MythicCoinWallet.spend(player, amount);
    }

    private void grantPurchasedReward(ServerPlayer player, ShopOfferDefinition offer) {
        ItemStack reward = offer.createStack(player.getRandom(), this.getPlayerLevel());
        if (DungeonRunManager.isPlayerInActiveRun(player)) {
            DungeonRunManager.rollAndBindForActiveRun(player, reward, player.getRandom());
            if (DungeonBoundItems.isWeapon(reward)) {
                DungeonRunManager.grantPrimaryWeapon(player, reward);
                return;
            }
        } else {
            DungeonBoundItems.markIfDungeonBound(reward);
        }
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
    }

    private void addCurrency(ServerPlayer player, int amount) {
        MythicCoinWallet.add(player, amount);
    }
}
