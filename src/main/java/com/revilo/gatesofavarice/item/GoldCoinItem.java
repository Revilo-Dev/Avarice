package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.currency.GoldCoinWallet;
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

public class GoldCoinItem extends Item {

    public GoldCoinItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.gatesofavarice.gold_coin.value", GoldCoinStackData.getValue(stack)).withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.translatable("tooltip.gatesofavarice.gold_coin.redeem").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        int value = GoldCoinStackData.getValue(stack) * Math.max(1, stack.getCount());
        if (value <= 0) {
            return InteractionResultHolder.pass(stack);
        }

        GoldCoinWallet.add(serverPlayer, value);
        stack.shrink(stack.getCount());
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || stack.isEmpty()) {
            return;
        }
        int totalValue = GoldCoinStackData.getValue(stack) * stack.getCount();
        if (totalValue <= 0) {
            return;
        }
        GoldCoinWallet.add(player, totalValue);
        stack.setCount(0);
    }
}
