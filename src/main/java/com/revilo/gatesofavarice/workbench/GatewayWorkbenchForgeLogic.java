package com.revilo.gatesofavarice.workbench;

import com.revilo.gatesofavarice.gateway.builder.GatewayForgeService;
import com.revilo.gatesofavarice.integration.LevelUpIntegration;
import com.revilo.gatesofavarice.item.CrystalItem;
import com.revilo.gatesofavarice.item.GatewayCardItem;
import com.revilo.gatesofavarice.item.data.CrystalForgeData;
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
        ItemStack card = container.getItem(GatewayWorkbenchSlots.CARD_SLOT);
        if (card.getItem() instanceof GatewayCardItem) {
            int playerLevel = player == null ? 1 : Math.max(1, LevelUpIntegration.getEffectiveLevel(player));
            return crystal.getItem() instanceof CrystalItem && CrystalForgeData.canAddCard(crystal, card, playerLevel);
        }
        return GatewayForgeService.canForge(player, container);
    }

    public static boolean forge(Player player, Container container) {
        if (!canForge(player, container)) {
            return false;
        }
        ItemStack card = container.getItem(GatewayWorkbenchSlots.CARD_SLOT);
        if (card.getItem() instanceof GatewayCardItem) {
            int playerLevel = player == null ? 1 : Math.max(1, LevelUpIntegration.getEffectiveLevel(player));
            ItemStack crystal = container.getItem(GatewayWorkbenchSlots.CRYSTAL_SLOT);
            if (!CrystalForgeData.addCard(crystal, card, playerLevel)) {
                return false;
            }
            card.shrink(1);
            container.setItem(GatewayWorkbenchSlots.CARD_SLOT, card.isEmpty() ? ItemStack.EMPTY : card);
            container.setChanged();
            return true;
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            GatewayForgeService.forge(serverPlayer, container);
            return true;
        }
        return false;
    }
}
