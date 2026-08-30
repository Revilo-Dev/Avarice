package com.revilo.gatesofavarice.block;

import com.mojang.serialization.MapCodec;
import com.revilo.gatesofavarice.currency.MythicCoinWallet;
import com.revilo.gatesofavarice.entity.MythicCoinOrbEntity;
import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import com.revilo.gatesofavarice.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

// A dungeon reward cache which bursts into the collectible Mythic Coins
public final class MythicCoinPileBlock extends Block {
    public static final MapCodec<MythicCoinPileBlock> CODEC = simpleCodec(MythicCoinPileBlock::new);
    private static final int MIN_VALUE = 20;
    private static final int MAX_VALUE = 50;
    private static final int BASE_CLUMP_SIZE = 3;

    public MythicCoinPileBlock() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(0.5F).noOcclusion());
    }

    private MythicCoinPileBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        burst((ServerLevel) level, pos, serverPlayer);
        DungeonRunManager.recordCoinPileLooted(serverPlayer);
        level.removeBlock(pos, false);
        return InteractionResult.CONSUME;
    }

    @Override
    public void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
    }

    private static void burst(ServerLevel level, BlockPos pos, ServerPlayer player) {
        int totalValue = Mth.nextInt(level.random, MIN_VALUE, MAX_VALUE);
        int clumpSize = Mth.clamp((int) Math.ceil(BASE_CLUMP_SIZE * MythicCoinWallet.getTotalMultiplier(player)), 0, totalValue);
        for (int index = 0; index < clumpSize; index++) {
            int remainingOrbs = clumpSize - index;
            int value = (totalValue + remainingOrbs - 1) / remainingOrbs;
            totalValue -= value;
            MythicCoinOrbEntity.spawn(level,
                    pos.getX() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.55D,
                    pos.getY() + 0.35D + level.random.nextDouble() * 0.35D,
                    pos.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.55D,
                    value);
        }
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.35F);
    }
}
