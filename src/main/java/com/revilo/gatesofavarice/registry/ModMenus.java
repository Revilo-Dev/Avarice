package com.revilo.gatesofavarice.registry;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.menu.DungeonWaveMenu;
import com.revilo.gatesofavarice.menu.DungeonLoadoutMenu;
import com.revilo.gatesofavarice.menu.GatewayWorkbenchMenu;
import com.revilo.gatesofavarice.menu.ShopkeeperMenu;
import com.revilo.gatesofavarice.menu.SacrificialAltarMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, GatewayExpansion.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<GatewayWorkbenchMenu>> GATEWAY_WORKBENCH =
            MENUS.register("gateway_workbench", () -> new MenuType<>(GatewayWorkbenchMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<ShopkeeperMenu>> SHOPKEEPER =
            MENUS.register("shopkeeper", () -> IMenuTypeExtension.create(ShopkeeperMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<DungeonWaveMenu>> DUNGEON_WAVE =
            MENUS.register("dungeon_wave", () -> IMenuTypeExtension.create(DungeonWaveMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<DungeonLoadoutMenu>> DUNGEON_LOADOUT =
            MENUS.register("dungeon_loadout", () -> IMenuTypeExtension.create(DungeonLoadoutMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<SacrificialAltarMenu>> SACRIFICIAL_ALTAR =
            MENUS.register("sacrificial_altar", () -> IMenuTypeExtension.create(SacrificialAltarMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
