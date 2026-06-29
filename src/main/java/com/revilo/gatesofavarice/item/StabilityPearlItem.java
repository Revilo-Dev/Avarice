package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.registry.ModMobEffects;
import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import com.revilo.gatesofavarice.shop.GatewaySellValues;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class StabilityPearlItem extends Item {

    private static final double NEGATIVE_STAT_REDUCTION = 0.05D;
    private static final int HEALTH_PENALTY_TICKS = Integer.MAX_VALUE;

    public StabilityPearlItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            if (DungeonRunManager.reduceNegativeRunModifiers(serverPlayer, NEGATIVE_STAT_REDUCTION)) {
                applyHealthDrain(player);
                spawnShatterEffect((ServerLevel) level, player, stack);
                player.sendSystemMessage(Component.literal("Stability Pearl reduced dungeon modifiers by 5%."));
                if (!player.hasInfiniteMaterials()) {
                    stack.shrink(1);
                }
                return InteractionResultHolder.success(stack);
            }
            player.sendSystemMessage(Component.translatable("message.gatesofavarice.no_active_gateway"));
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.gatesofavarice.stability_pearl.effect").withStyle(ChatFormatting.AQUA));
        GatewaySellValues.appendSellValueTooltip(stack, tooltipComponents);
    }

    private static void applyHealthDrain(Player player) {
        player.addEffect(new MobEffectInstance(ModMobEffects.STABILITY_DRAIN, HEALTH_PENALTY_TICKS, 0, false, true, true));
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
    }

    private static void spawnShatterEffect(ServerLevel level, Player player, ItemStack stack) {
        ItemStack particleStack = stack.copy();
        particleStack.setCount(1);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, particleStack),
                player.getX(), player.getEyeY() - 0.2D, player.getZ(),
                16,
                0.25D, 0.2D, 0.25D,
                0.08D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.15F);
    }

}
