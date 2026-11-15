package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.menu.LootPouchMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class LootPouchItem extends Item {
    public LootPouchItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack pouch = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new LootPouchMenu(id, inventory, slot), Component.translatable("item.gatesofavarice.loot_pouch")), buffer -> buffer.writeVarInt(slot));
        }
        return InteractionResultHolder.sidedSuccess(pouch, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Stores four stacks of one item type.").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Stackable items hold up to 99 each.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
