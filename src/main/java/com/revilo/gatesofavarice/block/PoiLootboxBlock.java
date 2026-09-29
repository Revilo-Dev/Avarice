package com.revilo.gatesofavarice.block;

import com.mojang.serialization.MapCodec;
import com.revilo.gatesofavarice.block.entity.PoiLootboxBlockEntity;
import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

// point of interest reward crate with a rarity chosen when placed
public class PoiLootboxBlock extends BaseEntityBlock {
    // registers the block codec for world serialization
    public static final MapCodec<PoiLootboxBlock> CODEC = simpleCodec(PoiLootboxBlock::new);

    public PoiLootboxBlock() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 3.0F));
    }

    private PoiLootboxBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof PoiLootboxBlockEntity lootbox)) return InteractionResult.PASS;
        // opens the stored rewards then marks the dungeon lootbox objective complete
        lootbox.open((ServerLevel) level, pos, serverPlayer);
        DungeonRunManager.recordLootboxLooted(serverPlayer);
        level.levelEvent(2001, pos, Block.getId(Blocks.SPRUCE_PLANKS.defaultBlockState()));
        level.removeBlock(pos, false);
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PoiLootboxBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        // assigns rarity only on the server so the placed crate has one authoritative result
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PoiLootboxBlockEntity lootbox) {
            lootbox.assignRandomRarity(level.random);
        }
    }
}
