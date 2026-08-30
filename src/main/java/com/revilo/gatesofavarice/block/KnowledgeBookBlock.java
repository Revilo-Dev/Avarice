package com.revilo.gatesofavarice.block;

import com.mojang.serialization.MapCodec;
import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import com.revilo.gatesofavarice.knowledge.KnowledgeManager;
import com.revilo.gatesofavarice.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/** A rare dungeon discovery reserved for the upcoming knowledge system. */
public final class KnowledgeBookBlock extends Block {
    public static final MapCodec<KnowledgeBookBlock> CODEC = simpleCodec(KnowledgeBookBlock::new);

    public KnowledgeBookBlock() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.3F).noOcclusion());
    }

    private KnowledgeBookBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer)) return InteractionResult.PASS;

        ItemStack reward = level.random.nextFloat() < 0.22F
                ? new ItemStack(ModItems.USELESS_KNOWLEDGE_BOOK.get())
                : KnowledgeManager.createGodlyBook((ServerPlayer) player, level.random);
        popResource(level, pos, reward);
        DungeonRunManager.recordKnowledgeBookObtained((ServerPlayer) player);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.1F);
        level.removeBlock(pos, false);
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;
        level.addParticle(ParticleTypes.ENCHANT,
                pos.getX() + 0.25D + random.nextDouble() * 0.5D,
                pos.getY() + 0.2D + random.nextDouble() * 0.45D,
                pos.getZ() + 0.25D + random.nextDouble() * 0.5D,
                (random.nextDouble() - 0.5D) * 0.02D,
                0.015D + random.nextDouble() * 0.025D,
                (random.nextDouble() - 0.5D) * 0.02D);
    }
}
