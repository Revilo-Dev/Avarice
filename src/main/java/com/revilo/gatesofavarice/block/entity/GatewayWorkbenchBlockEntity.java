package com.revilo.gatesofavarice.block.entity;

import com.revilo.gatesofavarice.menu.GatewayWorkbenchMenu;
import com.revilo.gatesofavarice.registry.ModBlockEntities;
import com.revilo.gatesofavarice.workbench.GatewayWorkbenchSlots;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GatewayWorkbenchBlockEntity extends BlockEntity implements Container, MenuProvider {

    // keeps this inventory aligned with the custom workbench menu slots
    public static final int SLOT_COUNT = GatewayWorkbenchSlots.CUSTOM_SLOT_COUNT;

    // holds the crafting inputs and outputs stored by the placed workbench
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public GatewayWorkbenchBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.GATEWAY_WORKBENCH.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GatewayWorkbenchBlockEntity blockEntity) {
        // marks the inventory dirty regularly so server saves capture its contents
        if (level.getGameTime() % 200L == 0L) {
            setChanged(level, pos, state);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.gatesofavarice.gateway_workbench");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // provides menu access tied to this exact level and block position
        return new GatewayWorkbenchMenu(containerId, playerInventory, this, ContainerLevelAccess.create(Objects.requireNonNull(this.level), this.worldPosition));
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack item : this.items) {
            if (!item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(this.items, slot, amount);
        if (!stack.isEmpty()) {
            this.setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = ContainerHelper.takeItem(this.items, slot);
        if (!stack.isEmpty()) {
            this.setChanged();
        }
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        // closes the menu when the workbench no longer exists or the player walks away
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(this.worldPosition.getCenter()) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // delegates slot rules to the shared workbench slot definitions
        return GatewayWorkbenchSlots.mayPlace(slot, stack);
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        // saves the workbench inventory with the block entity data
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        // restores the workbench inventory after loading the world
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
    }
}
