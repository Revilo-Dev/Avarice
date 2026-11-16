package com.revilo.gatesofavarice.block;

import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DungeonGatewayBlock extends Block {

    public enum Destination {
        RETURN,
        NEXT_FLOOR
    }

    private final Destination destination;

    public DungeonGatewayBlock(Destination destination) {
        super(BlockBehaviour.Properties.of().noCollission().lightLevel(state -> 12).strength(-1.0F).noLootTable());
        this.destination = destination;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || player.isSpectator() || player.isPassenger() || player.isOnPortalCooldown()) {
            return;
        }

        player.setPortalCooldown();
        if (this.destination == Destination.RETURN) {
            DungeonRunManager.returnThroughGatewayBlock(player, pos);
        } else {
            DungeonRunManager.useNextFloorGatewayBlock(player, pos);
        }
    }
}
