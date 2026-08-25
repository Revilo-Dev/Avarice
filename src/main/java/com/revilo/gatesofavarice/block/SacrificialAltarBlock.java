package com.revilo.gatesofavarice.block;

import com.mojang.serialization.MapCodec;
import com.revilo.gatesofavarice.currency.MythicCoinWallet;
import com.revilo.gatesofavarice.block.entity.SacrificialAltarBlockEntity;
import com.revilo.gatesofavarice.shop.GatewaySellValues;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

// allows for selling items mid run, uses sell GUI, sell value is -50%
public final class SacrificialAltarBlock extends BaseEntityBlock {
    public static final MapCodec<SacrificialAltarBlock> CODEC = simpleCodec(SacrificialAltarBlock::new);

    public SacrificialAltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public SacrificialAltarBlock() { this(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.5F, 6.0F)); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, net.minecraft.world.level.block.state.BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer) || !GatewaySellValues.isSellable(stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int value = GatewaySellValues.getStackValue(stack) / 2;
        if (value <= 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        stack.setCount(0);
        MythicCoinWallet.add(serverPlayer, value);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.65F, 1.35F);
        serverPlayer.displayClientMessage(Component.literal("Sacrificed for " + value + " Mythic Coins (50% value).").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(net.minecraft.world.level.block.state.BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof SacrificialAltarBlockEntity altar) {
            serverPlayer.openMenu(altar, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { return new SacrificialAltarBlockEntity(pos, state); }
    @Override public RenderShape getRenderShape(net.minecraft.world.level.block.state.BlockState state) { return RenderShape.MODEL; }

    @Override public void onRemove(net.minecraft.world.level.block.state.BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SacrificialAltarBlockEntity altar) {
            Containers.dropContents(level, pos, altar);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
