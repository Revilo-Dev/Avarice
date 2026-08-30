package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.knowledge.KnowledgeManager;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A redeemable knowledge discovery from dungeon library POIs. */
public final class GodlyKnowledgeItem extends Item {
    public GodlyKnowledgeItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.sidedSuccess(stack, true);
        return KnowledgeManager.redeem(serverPlayer, stack)
                ? InteractionResultHolder.sidedSuccess(stack, false)
                : InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        KnowledgeManager.getBookEntry(stack).ifPresentOrElse(entry -> {
            tooltip.add(Component.literal(entry.title()).withStyle(entry.rarity().color()));
            tooltip.add(Component.literal(entry.description()).withStyle(ChatFormatting.GRAY));
        }, () -> tooltip.add(Component.literal("Unknown knowledge").withStyle(ChatFormatting.GRAY)));
        tooltip.add(Component.literal("Right-click to add this discovery to your library.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
