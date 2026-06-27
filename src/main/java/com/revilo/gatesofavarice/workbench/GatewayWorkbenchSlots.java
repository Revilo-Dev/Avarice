package com.revilo.gatesofavarice.workbench;

import com.revilo.gatesofavarice.item.CrystalItem;
import com.revilo.gatesofavarice.item.GatewayCardItem;
import net.minecraft.world.item.ItemStack;

public final class GatewayWorkbenchSlots {

    public static final int CRYSTAL_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int FIRST_CARD_SLOT = 2;
    public static final int CARD_SLOT = FIRST_CARD_SLOT;
    public static final int CARD_SLOT_COUNT = 16;
    public static final int CUSTOM_SLOT_COUNT = FIRST_CARD_SLOT + CARD_SLOT_COUNT;

    public static final int CARD_GRID_X = 7;
    public static final int CARD_GRID_Y = 6;
    public static final int CARD_SLOT_SPACING = 18;
    public static final int CRYSTAL_X = 115;
    public static final int CRYSTAL_Y = 16;
    public static final int DISPLAY_CENTER_X = 123;
    public static final int DISPLAY_CENTER_Y = 30;
    public static final int OUTPUT_X = 151;
    public static final int OUTPUT_Y = 62;

    private GatewayWorkbenchSlots() {
    }

    public static boolean isCrystalSlot(int slot) {
        return slot == CRYSTAL_SLOT;
    }

    public static boolean isCardSlot(int slot) {
        return slot >= FIRST_CARD_SLOT && slot < FIRST_CARD_SLOT + CARD_SLOT_COUNT;
    }

    public static int cardSlotX(int cardIndex) {
        return CARD_GRID_X + (cardIndex % 4) * CARD_SLOT_SPACING;
    }

    public static int cardSlotY(int cardIndex) {
        return CARD_GRID_Y + (cardIndex / 4) * CARD_SLOT_SPACING;
    }

    public static boolean mayPlace(int slot, ItemStack stack) {
        if (slot == OUTPUT_SLOT) {
            return false;
        }
        if (isCrystalSlot(slot)) {
            return stack.getItem() instanceof CrystalItem;
        }
        if (isCardSlot(slot)) {
            return stack.getItem() instanceof GatewayCardItem;
        }
        return false;
    }

}
