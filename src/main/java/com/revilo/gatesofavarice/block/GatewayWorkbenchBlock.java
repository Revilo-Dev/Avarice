package com.revilo.gatesofavarice.block;

import com.mojang.serialization.MapCodec;
import com.revilo.gatesofavarice.block.entity.GatewayWorkbenchBlockEntity;
import com.revilo.gatesofavarice.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

public class GatewayWorkbenchBlock extends BaseEntityBlock implements EntityBlock {

    public static final MapCodec<GatewayWorkbenchBlock> CODEC = simpleCodec(GatewayWorkbenchBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
            box(2.0D, 2.0D, 3.0D, 14.0D, 12.0D, 13.0D),
            box(0.0D, 10.0D, 0.0D, 16.0D, 15.0D, 7.0D),
            box(0.0D, 12.0D, 6.0D, 16.0D, 14.0D, 16.0D),
            box(4.0D, 14.0D, 6.0D, 12.0D, 15.0D, 15.0D)
    );
    private static final VoxelShape SHAPE_EAST = Shapes.or(
            box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
            box(3.0D, 2.0D, 2.0D, 13.0D, 12.0D, 14.0D),
            box(9.0D, 10.0D, 0.0D, 16.0D, 15.0D, 16.0D),
            box(0.0D, 12.0D, 0.0D, 10.0D, 14.0D, 16.0D),
            box(1.0D, 14.0D, 4.0D, 10.0D, 15.0D, 12.0D)
    );
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
            box(2.0D, 2.0D, 3.0D, 14.0D, 12.0D, 13.0D),
            box(0.0D, 10.0D, 9.0D, 16.0D, 15.0D, 16.0D),
            box(0.0D, 12.0D, 0.0D, 16.0D, 14.0D, 10.0D),
            box(4.0D, 14.0D, 1.0D, 12.0D, 15.0D, 10.0D)
    );
    private static final VoxelShape SHAPE_WEST = Shapes.or(
            box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
            box(3.0D, 2.0D, 2.0D, 13.0D, 12.0D, 14.0D),
            box(0.0D, 10.0D, 0.0D, 7.0D, 15.0D, 16.0D),
            box(6.0D, 12.0D, 0.0D, 16.0D, 14.0D, 16.0D),
            box(6.0D, 14.0D, 4.0D, 15.0D, 15.0D, 12.0D)
    );

    public GatewayWorkbenchBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public GatewayWorkbenchBlock() {
        this(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD).requiresCorrectToolForDrops().noOcclusion());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GatewayWorkbenchBlockEntity blockEntity) {
            player.openMenu(blockEntity);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.getShape(state, level, pos, context);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GatewayWorkbenchBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GatewayWorkbenchBlockEntity blockEntity) {
            Containers.dropContents(level, pos, blockEntity);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.GATEWAY_WORKBENCH.get(), GatewayWorkbenchBlockEntity::serverTick);
    }
}
