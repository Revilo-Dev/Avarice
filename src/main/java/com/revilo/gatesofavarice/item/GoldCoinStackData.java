package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class GoldCoinStackData {

    private static final String ROOT_KEY = "gatesofavarice_gold_coin";
    private static final String VALUE_KEY = "value";

    private GoldCoinStackData() {
    }

    public static ItemStack createStack(int amount) {
        int value = Math.max(1, amount);
        ItemStack stack = new ItemStack(ModItems.GOLD_COIN.get(), 1);
        setValue(stack, value);
        return stack;
    }

    public static int getValue(ItemStack stack) {
        if (!stack.is(ModItems.GOLD_COIN.get())) {
            return 0;
        }

        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(ROOT_KEY);
        if (root.contains(VALUE_KEY)) {
            return Math.max(1, root.getInt(VALUE_KEY));
        }

        return Math.max(1, stack.getCount());
    }

    public static void setValue(ItemStack stack, int value) {
        if (!stack.is(ModItems.GOLD_COIN.get())) {
            return;
        }

        int clampedValue = Math.max(1, value);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag root = tag.getCompound(ROOT_KEY);
            root.putInt(VALUE_KEY, clampedValue);
            tag.put(ROOT_KEY, root);
        });
        stack.setCount(1);
    }
}
