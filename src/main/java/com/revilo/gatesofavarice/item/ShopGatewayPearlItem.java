package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.shop.GatewaySellValues;
import com.revilo.gatesofavarice.shop.ShopkeeperManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.List;

public class ShopGatewayPearlItem extends Item {

    private static final double SHOPKEEPER_SPACING = 5.0D;

    public ShopGatewayPearlItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos);
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + (shape.isEmpty() ? 0.0D : shape.max(net.minecraft.core.Direction.Axis.Y));
        double z = pos.getZ() + 0.5D;
        if (!level.getEntitiesOfClass(Entity.class,
                new net.minecraft.world.phys.AABB(x - SHOPKEEPER_SPACING, y - SHOPKEEPER_SPACING, z - SHOPKEEPER_SPACING,
                        x + SHOPKEEPER_SPACING, y + SHOPKEEPER_SPACING, z + SHOPKEEPER_SPACING),
                entity -> entity instanceof com.revilo.gatesofavarice.entity.GatekeeperEntity gatekeeper && ShopkeeperManager.isShopkeeper(gatekeeper)).isEmpty()) {
            return InteractionResult.FAIL;
        }

        Entity entity = ShopkeeperManager.spawnShopkeeper(level, x, y, z, player);
        if (entity == null) {
            player.sendSystemMessage(Component.translatable("message.gatesofavarice.shopkeeper_spawn_failed"));
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        GatewaySellValues.appendSellValueTooltip(stack, tooltipComponents);
    }

}
