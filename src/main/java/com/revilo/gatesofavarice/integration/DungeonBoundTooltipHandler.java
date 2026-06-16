package com.revilo.gatesofavarice.integration;

import com.revilo.gatesofavarice.dungeon.DungeonBoundItems;
import com.revilo.gatesofavarice.dungeon.ModDimensions;
import com.revilo.gatesofavarice.shop.GatewaySellValues;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class DungeonBoundTooltipHandler {

    private DungeonBoundTooltipHandler() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getEntity() != null && event.getEntity().level().dimension().equals(ModDimensions.DUNGEON_LEVEL)) {
            GatewaySellValues.appendDungeonSellValueTooltip(event.getItemStack(), event.getToolTip());
        }
        if (!DungeonBoundItems.isDungeonBound(event.getItemStack())) {
            return;
        }
        event.getToolTip().addAll(List.of(Component.literal("Dungeon Bound").withStyle(ChatFormatting.RED)));
    }
}
