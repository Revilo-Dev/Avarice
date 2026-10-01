package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.knowledge.KnowledgeManager;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A redeemable knowledge discovery from dungeon library POIs. */
public final class GodlyKnowledgeItem extends Item {
    public GodlyKnowledgeItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        KnowledgeManager.getBookEntry(stack).ifPresentOrElse(entry -> {
            tooltip.add(Component.literal(entry.title()).withStyle(entry.rarity().color()));
            tooltip.add(Component.literal(entry.description()).withStyle(ChatFormatting.GRAY));
        }, () -> tooltip.add(Component.literal("Unknown knowledge").withStyle(ChatFormatting.GRAY)));
        tooltip.add(Component.literal("Give this to the Archive Keeper to learn it.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
