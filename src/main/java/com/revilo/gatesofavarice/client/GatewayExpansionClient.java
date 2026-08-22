package com.revilo.gatesofavarice.client;

import com.revilo.gatesofavarice.client.model.GatekeeperModel;
import com.revilo.gatesofavarice.client.render.GatekeeperRenderer;
import com.revilo.gatesofavarice.client.render.GatewayCrystalRenderer;
import com.revilo.gatesofavarice.client.render.MythicCoinOrbRenderer;
import com.revilo.gatesofavarice.client.render.GatewayWorkbenchBlockEntityRenderer;
import com.revilo.gatesofavarice.client.screen.DungeonWaveScreen;
import com.revilo.gatesofavarice.client.screen.DungeonLoadoutScreen;
import com.revilo.gatesofavarice.client.screen.ShopkeeperScreen;
import com.revilo.gatesofavarice.client.screen.GatewayWorkbenchScreen;
import com.revilo.gatesofavarice.registry.ModBlockEntities;
import com.revilo.gatesofavarice.registry.ModEntities;
import com.revilo.gatesofavarice.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.resources.ResourceLocation;
import com.revilo.gatesofavarice.GatewayExpansion;

public final class GatewayExpansionClient {

    private GatewayExpansionClient() {
    }

    public static void register(IEventBus modEventBus) {
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.register(ClientEvents.class);
            modEventBus.addListener(MagnetKeybindHandler::registerKeyMappings);
            NeoForge.EVENT_BUS.register(DungeonWaveHudOverlay.class);
            NeoForge.EVENT_BUS.register(InventoryWalletOverlay.class);
            NeoForge.EVENT_BUS.addListener(MagnetKeybindHandler::onClientTick);
        }
    }

    public static final class ClientEvents {

        private ClientEvents() {
        }

        @SubscribeEvent
        public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
            event.register(
                    ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "infinite_library"),
                    new InfiniteLibrarySkyEffects());
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenus.GATEWAY_WORKBENCH.get(), GatewayWorkbenchScreen::new);
            event.register(ModMenus.SHOPKEEPER.get(), ShopkeeperScreen::new);
            event.register(ModMenus.DUNGEON_WAVE.get(), DungeonWaveScreen::new);
            event.register(ModMenus.DUNGEON_LOADOUT.get(), DungeonLoadoutScreen::new);
        }

        @SubscribeEvent
        public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.GATEWAY_WORKBENCH.get(), GatewayWorkbenchBlockEntityRenderer::new);
            event.registerEntityRenderer(ModEntities.GATEKEEPER.get(), GatekeeperRenderer::new);
            event.registerEntityRenderer(ModEntities.GATEWAY_CRYSTAL.get(), GatewayCrystalRenderer::new);
            event.registerEntityRenderer(ModEntities.MYTHIC_COIN_ORB.get(), MythicCoinOrbRenderer::new);
        }

        @SubscribeEvent
        public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(GatekeeperModel.LAYER_LOCATION, GatekeeperModel::createBodyLayer);
        }
    }
}
