package com.revilo.gatesofavarice.workbench;

import com.revilo.gatesofavarice.item.CrystalItem;
import com.revilo.gatesofavarice.item.GatewayCardItem;
import net.minecraft.world.item.ItemStack;

public final class GatewayWorkbenchSlots {

    public static final int CRYSTAL_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int CARD_SLOT = 2;
    public static final int CUSTOM_SLOT_COUNT = 3;

    public static final int CRYSTAL_X = 80;
    public static final int CRYSTAL_Y = 62;
    public static final int CARD_X = 118;
    public static final int CARD_Y = 62;
    public static final int DISPLAY_CENTER_X = 88;
    public static final int DISPLAY_CENTER_Y = 32;
    public static final int OUTPUT_X = DISPLAY_CENTER_X - 8;
    public static final int OUTPUT_Y = DISPLAY_CENTER_Y - 8;

    private GatewayWorkbenchSlots() {
    }

    public static boolean isCrystalSlot(int slot) {
        return slot == CRYSTAL_SLOT;
    }

    public static boolean mayPlace(int slot, ItemStack stack) {
        if (slot == OUTPUT_SLOT) {
            return false;
        }
        if (isCrystalSlot(slot)) {
            return stack.getItem() instanceof CrystalItem;
        }
        if (slot == CARD_SLOT) {
            return stack.getItem() instanceof GatewayCardItem;
        }
        return false;
    }

}
