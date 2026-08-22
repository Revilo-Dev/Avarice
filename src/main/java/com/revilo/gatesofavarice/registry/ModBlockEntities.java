package com.revilo.gatesofavarice.registry;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.block.entity.GatewayWorkbenchBlockEntity;
import com.revilo.gatesofavarice.block.entity.LootboxBlockEntity;
import com.revilo.gatesofavarice.block.entity.PoiLootboxBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GatewayExpansion.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GatewayWorkbenchBlockEntity>> GATEWAY_WORKBENCH =
            BLOCK_ENTITY_TYPES.register("gateway_workbench",
                    () -> BlockEntityType.Builder.of(GatewayWorkbenchBlockEntity::new, ModBlocks.GATEWAY_WORKBENCH.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LootboxBlockEntity>> LOOTBOX =
            BLOCK_ENTITY_TYPES.register("lootbox",
                    () -> BlockEntityType.Builder.of(LootboxBlockEntity::new, ModBlocks.LOOTBOX.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PoiLootboxBlockEntity>> POI_LOOTBOX =
            BLOCK_ENTITY_TYPES.register("poi_lootbox",
                    () -> BlockEntityType.Builder.of(PoiLootboxBlockEntity::new, ModBlocks.POI_LOOTBOX.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
