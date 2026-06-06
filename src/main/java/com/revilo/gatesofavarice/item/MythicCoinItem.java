package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.entity.MythicCoinOrbEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public class MythicCoinItem extends Item {

    public MythicCoinItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.gatesofavarice.mythic_coin.value", MythicCoinStackData.getValue(stack)).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltipComponents.add(Component.translatable("tooltip.gatesofavarice.mythic_coin.redeem").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        int value = MythicCoinStackData.getValue(stack) * Math.max(1, stack.getCount());
        if (value <= 0) {
            return InteractionResultHolder.pass(stack);
        }

        MythicCoinOrbEntity.spawn(serverPlayer.serverLevel(), serverPlayer.getX(), serverPlayer.getY() + 0.6D, serverPlayer.getZ(), value);
        stack.shrink(stack.getCount());
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || stack.isEmpty()) {
            return;
        }
        int totalValue = MythicCoinStackData.getValue(stack) * stack.getCount();
        if (totalValue <= 0) {
            return;
        }
        MythicCoinOrbEntity.spawn(player.serverLevel(), player.getX(), player.getY() + 0.6D, player.getZ(), totalValue);
        stack.setCount(0);
    }
}
