package com.revilo.gatesofavarice.menu;

import com.revilo.gatesofavarice.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class LootPouchMenu extends AbstractContainerMenu {
    private static final int POUCH_SLOTS = 4;
    private final Inventory playerInventory;
    private final int pouchSlot;
    private final Container contents;

    public LootPouchMenu(int id, Inventory inventory, net.minecraft.network.FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readVarInt());
    }

    public LootPouchMenu(int id, Inventory inventory, int pouchSlot) {
        super(ModMenus.LOOT_POUCH.get(), id);
        this.playerInventory = inventory;
        this.pouchSlot = pouchSlot;
        this.contents = loadContents(inventory.getItem(pouchSlot));
        for (int index = 0; index < POUCH_SLOTS; index++) {
            addSlot(new Slot(contents, index, 44 + index * 18, 20) {
                @Override public boolean mayPlace(ItemStack stack) { return accepts(stack); }
                @Override public int getMaxStackSize() { return 99; }
            });
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 51 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 109));
    }

    private boolean accepts(ItemStack candidate) {
        if (candidate.isEmpty() || candidate.getMaxStackSize() <= 1) return false;
        for (int index = 0; index < POUCH_SLOTS; index++) {
            ItemStack stored = contents.getItem(index);
            if (!stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, candidate)) return false;
        }
        return true;
    }

    private static Container loadContents(ItemStack pouch) {
        SimpleContainer container = new SimpleContainer(POUCH_SLOTS);
        ItemContainerContents stored = pouch.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> stacks = NonNullList.withSize(POUCH_SLOTS, ItemStack.EMPTY);
        stored.copyInto(stacks);
        for (int index = 0; index < Math.min(POUCH_SLOTS, stacks.size()); index++) container.setItem(index, stacks.get(index));
        return container;
    }

    @Override public boolean stillValid(Player player) { return player.getInventory().getItem(pouchSlot).getItem() instanceof com.revilo.gatesofavarice.item.LootPouchItem; }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < POUCH_SLOTS) {
            if (!moveItemStackTo(stack, POUCH_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (accepts(stack)) {
            if (!moveItemStackTo(stack, 0, POUCH_SLOTS, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            ItemStack pouch = playerInventory.getItem(pouchSlot);
            if (pouch.getItem() instanceof com.revilo.gatesofavarice.item.LootPouchItem) {
                List<ItemStack> stacks = new ArrayList<>(POUCH_SLOTS);
                for (int index = 0; index < POUCH_SLOTS; index++) stacks.add(contents.getItem(index).copy());
                pouch.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
            }
        }
    }
}
