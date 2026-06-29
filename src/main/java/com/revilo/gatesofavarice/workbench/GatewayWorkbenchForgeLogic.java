package com.revilo.gatesofavarice.workbench;

import com.revilo.gatesofavarice.item.CrystalItem;
import com.revilo.gatesofavarice.item.GatewayCardItem;
import com.revilo.gatesofavarice.item.data.CrystalForgeData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class GatewayWorkbenchForgeLogic {

    private GatewayWorkbenchForgeLogic() {
    }

    public static boolean canForge(Player player, Container container) {
        if (!container.getItem(GatewayWorkbenchSlots.OUTPUT_SLOT).isEmpty()) {
            return false;
        }
        ItemStack crystal = container.getItem(GatewayWorkbenchSlots.CRYSTAL_SLOT);
        if (!(crystal.getItem() instanceof CrystalItem)) {
            return false;
        }
        List<Integer> cardSlots = filledCardSlots(container);
        if (!cardSlots.isEmpty()) {
            int existingCards = CrystalForgeData.readCards(crystal).size();
            return existingCards + cardSlots.size() <= CrystalForgeData.maxCardsForCrystal(crystal);
        }
        return false;
    }

    public static boolean forge(Player player, Container container) {
        if (!canForge(player, container)) {
            return false;
        }
        List<Integer> cardSlots = filledCardSlots(container);
        if (!cardSlots.isEmpty()) {
            ItemStack crystal = container.getItem(GatewayWorkbenchSlots.CRYSTAL_SLOT);
            for (int slot : cardSlots) {
                ItemStack card = container.getItem(slot);
                if (!CrystalForgeData.addCard(crystal, card, 1)) {
                    return false;
                }
            }
            for (int slot : cardSlots) {
                ItemStack card = container.getItem(slot);
                card.shrink(1);
                container.setItem(slot, card.isEmpty() ? ItemStack.EMPTY : card);
            }
            container.setChanged();
            return true;
        }
        return false;
    }

    private static List<Integer> filledCardSlots(Container container) {
        ArrayList<Integer> slots = new ArrayList<>();
        for (int slot = GatewayWorkbenchSlots.FIRST_CARD_SLOT; slot < GatewayWorkbenchSlots.FIRST_CARD_SLOT + GatewayWorkbenchSlots.CARD_SLOT_COUNT; slot++) {
            if (container.getItem(slot).getItem() instanceof GatewayCardItem) {
                slots.add(slot);
            }
        }
        return slots;
    }
}
