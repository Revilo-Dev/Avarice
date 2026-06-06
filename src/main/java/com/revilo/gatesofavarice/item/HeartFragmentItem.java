package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.item.data.LootRarity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class HeartFragmentItem extends LootMaterialItem {
    public HeartFragmentItem(LootRarity rarity, Properties properties) {
        super(rarity, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.literal("Heals half a heart instantly").withStyle(ChatFormatting.RED));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (player.getHealth() >= player.getMaxHealth()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            player.heal(1.0F);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.getFoodData().eat(0, 0.0F);
                ServerLevel serverLevel = serverPlayer.serverLevel();
                serverLevel.sendParticles(ParticleTypes.HEART, serverPlayer.getX(), serverPlayer.getY() + 1.0D, serverPlayer.getZ(), 4, 0.2D, 0.25D, 0.2D, 0.0D);
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, serverPlayer.getX(), serverPlayer.getY() + 0.75D, serverPlayer.getZ(), 6, 0.25D, 0.3D, 0.25D, 0.0D);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.45F, 1.35F);
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
