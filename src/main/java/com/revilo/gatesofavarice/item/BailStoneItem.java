package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
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

/** A run-only checkpoint escape. It is deliberately never consumed. */
public final class BailStoneItem extends Item {
    public BailStoneItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(player instanceof ServerPlayer serverPlayer) || !DungeonRunManager.canUseBailStone(serverPlayer)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.literal("The Bail Stone can only be used between dungeon floors.").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (DungeonRunManager.bailWithStone(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.literal("Dungeon run saved. Use the gateway to return.").withStyle(ChatFormatting.GOLD), true);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Right-click between floors to leave and save your run.").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Your dungeon loadout and upgrades are kept.").withStyle(ChatFormatting.GREEN));
    }
}
