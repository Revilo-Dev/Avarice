package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.item.data.GatewayCardData;
import com.revilo.gatesofavarice.registry.ModItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BoosterPackItem extends Item {
    private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(Vec3.fromRGB24(0xF6C64A).toVector3f(), 1.0F);
    private final GatewayCardData.BoosterRarity boosterRarity;

    public BoosterPackItem(GatewayCardData.BoosterRarity boosterRarity, Properties properties) {
        super(properties);
        this.boosterRarity = boosterRarity;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack pack = player.getItemInHand(usedHand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            RandomSource random = serverPlayer.getRandom();
            int playerLevel = Math.max(1, com.revilo.gatesofavarice.integration.LevelUpIntegration.getEffectiveLevel(serverPlayer));
            ItemStack card = GatewayCardData.createFromBooster(ModItems.GATEWAY_CARD.get(), this.boosterRarity, playerLevel, random);
            serverPlayer.drop(card, false);
            level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.45F, 1.2F + random.nextFloat() * 0.25F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(GOLD_DUST, serverPlayer.getX(), serverPlayer.getY() + 1.0D, serverPlayer.getZ(), 18, 0.35D, 0.45D, 0.35D, 0.03D);
            }
            if (!serverPlayer.getAbilities().instabuild) {
                pack.shrink(1);
            }
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(pack, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        MutableComponent name = Component.translatable(this.getDescriptionId(stack));
        return name.withStyle(switch (this.boosterRarity) {
            case COMMON -> ChatFormatting.WHITE;
            case UNCOMMON -> ChatFormatting.GREEN;
            case RARE -> ChatFormatting.AQUA;
            case EPIC -> ChatFormatting.LIGHT_PURPLE;
            case LEGENDARY -> ChatFormatting.YELLOW;
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Opens into a random Gateway Card.").withStyle(ChatFormatting.GRAY));
        if (this.boosterRarity.allowsMultiplier()) {
            tooltipComponents.add(Component.literal("Can roll multiplier cards.").withStyle(ChatFormatting.GOLD));
        }
        if (this.boosterRarity.allowsLoadout()) {
            tooltipComponents.add(Component.literal("Can roll Loadout Cards.").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
