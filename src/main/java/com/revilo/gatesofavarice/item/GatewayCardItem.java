package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.item.data.GatewayCardData;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class GatewayCardItem extends Item {
    public GatewayCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.addAll(GatewayCardData.tooltip(stack));
    }
}
