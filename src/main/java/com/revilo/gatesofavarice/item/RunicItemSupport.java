package com.revilo.gatesofavarice.item;

import net.minecraft.world.item.ItemStack;
import net.revilodev.runic.registry.ModDataComponents;

public final class RunicItemSupport {
    private RunicItemSupport() {
    }

    public static void ensureRunicData(ItemStack stack, int runeSlots) {
        if (runeSlots <= 0) {
            return;
        }
        stack.set(ModDataComponents.RUNE_SLOTS_CAPACITY.get(), runeSlots);
        Integer used = stack.getOrDefault(ModDataComponents.RUNE_SLOTS_USED.get(), 0);
        if (used > runeSlots) {
            stack.set(ModDataComponents.RUNE_SLOTS_USED.get(), runeSlots);
        }
        stack.set(ModDataComponents.RUNE_EXPANSIONS_USED.get(), stack.getOrDefault(ModDataComponents.RUNE_EXPANSIONS_USED.get(), 0));
    }
}
