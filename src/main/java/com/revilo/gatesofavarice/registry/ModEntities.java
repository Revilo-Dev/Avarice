package com.revilo.gatesofavarice.registry;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.entity.GatekeeperEntity;
import com.revilo.gatesofavarice.entity.GatewayCrystalEntity;
import com.revilo.gatesofavarice.entity.MythicCoinOrbEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, GatewayExpansion.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GatekeeperEntity>> GATEKEEPER = ENTITY_TYPES.register("gatekeeper",
            () -> EntityType.Builder.of(GatekeeperEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("gatekeeper"));
    public static final DeferredHolder<EntityType<?>, EntityType<GatewayCrystalEntity>> GATEWAY_CRYSTAL = ENTITY_TYPES.register("gateway_crystal",
            () -> EntityType.Builder.<GatewayCrystalEntity>of(GatewayCrystalEntity::new, MobCategory.MISC)
                    .sized(1.4F, 2.4F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("gateway_crystal"));
    public static final DeferredHolder<EntityType<?>, EntityType<MythicCoinOrbEntity>> MYTHIC_COIN_ORB = ENTITY_TYPES.register("mythic_coin_orb",
            () -> EntityType.Builder.<MythicCoinOrbEntity>of(MythicCoinOrbEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("mythic_coin_orb"));

    private ModEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::onEntityAttributeCreation);
    }

    private static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(GATEKEEPER.get(), GatekeeperEntity.createAttributes().build());
    }
}
