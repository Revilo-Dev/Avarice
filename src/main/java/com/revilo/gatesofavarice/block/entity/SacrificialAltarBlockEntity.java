package com.revilo.gatesofavarice.block.entity;

import com.revilo.gatesofavarice.currency.MythicCoinWallet;
import com.revilo.gatesofavarice.menu.SacrificialAltarMenu;
import com.revilo.gatesofavarice.registry.ModBlockEntities;
import com.revilo.gatesofavarice.shop.GatewaySellValues;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.MenuProvider;

/** Temporary sell inventory.  Contents are sacrificed at half value when its menu closes. */
public final class SacrificialAltarBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int SLOT_COUNT = 18;
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public SacrificialAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SACRIFICIAL_ALTAR.get(), pos, state);
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < SLOT_COUNT ? items.get(slot) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) { ItemStack result = ContainerHelper.removeItem(items, slot, amount); if (!result.isEmpty()) setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { if (slot >= 0 && slot < SLOT_COUNT) { items.set(slot, stack); setChanged(); } }
    @Override public boolean stillValid(Player player) { return level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D; }
    @Override public void clearContent() { items.clear(); setChanged(); }

    @Override public Component getDisplayName() { return Component.translatable("block.gatesofavarice.sacrificial_altar"); }

    @Override public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SacrificialAltarMenu(containerId, inventory, this, worldPosition);
    }

    public void sacrificeContents(ServerPlayer player) {
        int total = 0;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = removeItemNoUpdate(slot);
            if (stack.isEmpty()) continue;
            int value = GatewaySellValues.getStackValue(stack) / 2;
            if (GatewaySellValues.isSellable(stack) && value > 0) {
                total += value;
            } else if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        if (total > 0) {
            MythicCoinWallet.add(player, total);
            player.level().playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.75F, 1.25F);
            player.displayClientMessage(Component.literal("Sacrificed items for " + total + " Mythic Coins (50% value)."), true);
        }
        setChanged();
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
    }
}
