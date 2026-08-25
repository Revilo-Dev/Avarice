package com.revilo.gatesofavarice.menu;

import com.revilo.gatesofavarice.block.entity.SacrificialAltarBlockEntity;
import com.revilo.gatesofavarice.registry.ModMenus;
import com.revilo.gatesofavarice.shop.GatewaySellValues;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class SacrificialAltarMenu extends AbstractContainerMenu {
    private final Container altar;

    public SacrificialAltarMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    private SacrificialAltarMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, containerAt(inventory, pos), pos);
    }

    public SacrificialAltarMenu(int containerId, Inventory inventory, Container altar, BlockPos ignored) {
        super(ModMenus.SACRIFICIAL_ALTAR.get(), containerId);
        this.altar = altar;
        for (int row = 0; row < 3; row++) for (int column = 0; column < 6; column++) {
            addSlot(new Slot(altar, column + row * 6, 8 + column * 18, 18 + row * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return GatewaySellValues.isSellable(stack); }
            });
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
    }

    private static Container containerAt(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof SacrificialAltarBlockEntity altar ? altar : new SimpleContainer(SacrificialAltarBlockEntity.SLOT_COUNT);
    }

    @Override public boolean stillValid(Player player) { return altar.stillValid(player); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < SacrificialAltarBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, SacrificialAltarBlockEntity.SLOT_COUNT, slots.size(), true)) return ItemStack.EMPTY;
        } else if (GatewaySellValues.isSellable(stack)) {
            if (!moveItemStackTo(stack, 0, SacrificialAltarBlockEntity.SLOT_COUNT, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer && altar instanceof SacrificialAltarBlockEntity altarBlock) altarBlock.sacrificeContents(serverPlayer);
    }
}
