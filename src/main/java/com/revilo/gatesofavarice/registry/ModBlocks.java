package com.revilo.gatesofavarice.registry;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.block.GatewayWorkbenchBlock;
import com.revilo.gatesofavarice.block.LootboxBlock;
import com.revilo.gatesofavarice.block.PoiLootboxBlock;
import com.revilo.gatesofavarice.block.SacrificialAltarBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GatewayExpansion.MOD_ID);

    public static final DeferredBlock<Block> GATEWAY_WORKBENCH = BLOCKS.register("gateway_workbench", () -> new GatewayWorkbenchBlock());
    public static final DeferredBlock<Block> LOOTBOX = BLOCKS.register("lootbox", LootboxBlock::new);
    public static final DeferredBlock<Block> POI_LOOTBOX = BLOCKS.register("poi_lootbox", PoiLootboxBlock::new);
    public static final DeferredBlock<Block> SACRIFICIAL_ALTAR = BLOCKS.register("sacrificial_altar", () -> new SacrificialAltarBlock());
    public static final DeferredBlock<Block> METAL_BLOCK = BLOCKS.register("metal_block",
            () -> new Block(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
