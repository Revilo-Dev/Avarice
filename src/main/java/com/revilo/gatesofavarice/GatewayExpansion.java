package com.revilo.gatesofavarice;

import com.revilo.gatesofavarice.command.CoinCommands;
import com.revilo.gatesofavarice.command.DungeonCommands;
import com.revilo.gatesofavarice.command.PartyCommands;
import com.revilo.gatesofavarice.client.GatewayExpansionClient;
import com.revilo.gatesofavarice.config.GatewayExpansionConfig;
import com.revilo.gatesofavarice.dungeon.DungeonInstanceManager;
import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import com.revilo.gatesofavarice.dungeon.DungeonUpgradeManager;
import com.revilo.gatesofavarice.integration.GatewayDrownedHandler;
import com.revilo.gatesofavarice.integration.StabilityPearlHandler;
import com.revilo.gatesofavarice.integration.CuriosCompat;
import com.revilo.gatesofavarice.integration.DungeonBoundTooltipHandler;
import com.revilo.gatesofavarice.integration.ArsenalWeaponTraitHandler;
import com.revilo.gatesofavarice.integration.MagnetHandler;
import com.revilo.gatesofavarice.integration.ModCompat;
import com.revilo.gatesofavarice.network.GatewayExpansionNetwork;
import com.revilo.gatesofavarice.registry.ModAttachments;
import com.revilo.gatesofavarice.registry.ModAttributes;
import com.revilo.gatesofavarice.registry.ModBlockEntities;
import com.revilo.gatesofavarice.registry.ModBlocks;
import com.revilo.gatesofavarice.registry.ModCreativeTabs;
import com.revilo.gatesofavarice.registry.ModEntities;
import com.revilo.gatesofavarice.registry.LoadoutArmorRegistry;
import com.revilo.gatesofavarice.registry.ModItems;
import com.revilo.gatesofavarice.registry.ModMenus;
import com.revilo.gatesofavarice.registry.ModMobEffects;
import com.revilo.gatesofavarice.registry.ModRecipes;
import com.revilo.gatesofavarice.shop.ShopkeeperManager;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(GatewayExpansion.MOD_ID)
public final class GatewayExpansion {

    public static final String MOD_ID = "gatesofavarice";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GatewayExpansion(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, GatewayExpansionConfig.SPEC);
        ModItems.register(modEventBus);
        LoadoutArmorRegistry.register(modEventBus);
        ModEntities.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModAttributes.register(modEventBus);
        ModMenus.register(modEventBus);
        ModMobEffects.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        GatewayExpansionNetwork.register(modEventBus);
        GatewayExpansionClient.register(modEventBus);
        if (ModCompat.isAnyLoaded("curios")) {
            CuriosCompat.register(modEventBus);
        }
        NeoForge.EVENT_BUS.register(CoinCommands.class);
        NeoForge.EVENT_BUS.register(DungeonCommands.class);
        NeoForge.EVENT_BUS.register(PartyCommands.class);
        NeoForge.EVENT_BUS.register(DungeonBoundTooltipHandler.class);
        NeoForge.EVENT_BUS.register(MagnetHandler.class);
        NeoForge.EVENT_BUS.register(ArsenalWeaponTraitHandler.class);
        NeoForge.EVENT_BUS.register(DungeonInstanceManager.class);
        NeoForge.EVENT_BUS.register(DungeonRunManager.class);
        NeoForge.EVENT_BUS.register(DungeonUpgradeManager.class);
        NeoForge.EVENT_BUS.register(ShopkeeperManager.class);
        NeoForge.EVENT_BUS.register(GatewayDrownedHandler.class);
        NeoForge.EVENT_BUS.register(StabilityPearlHandler.class);
    }
}
