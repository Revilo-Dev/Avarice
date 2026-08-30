package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.item.data.CrystalForgeData;
import com.revilo.gatesofavarice.entity.GatewayCrystalEntity;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class CrystalItem extends Item {

    private final CrystalTier crystalTier;

    public CrystalItem(CrystalTier crystalTier, Properties properties) {
        super(properties);
        this.crystalTier = crystalTier;
    }

    public CrystalTier crystalTier() {
        return this.crystalTier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.gatesofavarice.crystal.description").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.gatesofavarice.crystal.tier", this.crystalTier.tier()).withStyle(ChatFormatting.AQUA));
        tooltipComponents.addAll(CrystalForgeData.buildCrystalTooltip(stack, isAltDown()));
    }

    private static boolean isAltDown() {
        try {
            Class<?> screenClass = Class.forName("net.minecraft.client.gui.screens.Screen");
            Method method = screenClass.getMethod("hasAltDown");
            return Boolean.TRUE.equals(method.invoke(null));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        GatewayCrystalEntity gateway = new GatewayCrystalEntity(serverLevel);
        Vec3 spawnPos = resolveSpawnPosition(player);
        gateway.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), 0.0F);
        gateway.setCrystalTier(this.crystalTier.tier());
        gateway.setOwnerId(player.getUUID());
        gateway.setGuaranteedOwnerLoadout(CrystalForgeData.hasLoadoutCard(stack));
        serverLevel.addFreshEntity(gateway);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }

    private static Vec3 resolveSpawnPosition(Player player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 maxReach = eyePos.add(look.scale(8.0D));

        BlockHitResult hitResult = player.level().clip(new ClipContext(eyePos, maxReach, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos spawnBlock = hitResult.getBlockPos().above();
            return new Vec3(spawnBlock.getX() + 0.5D, spawnBlock.getY(), spawnBlock.getZ() + 0.5D);
        }

        Vec3 fallback = player.position().add(look.scale(2.5D));
        return new Vec3(fallback.x, player.getY(), fallback.z);
    }

    /** Crystal tier now controls deck capacity only. */
    public record CrystalTier(int tier) {
    }
}
