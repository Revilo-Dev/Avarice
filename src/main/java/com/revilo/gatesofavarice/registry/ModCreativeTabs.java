package com.revilo.gatesofavarice.registry;

import com.revilo.gatesofavarice.GatewayExpansion;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {

    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GatewayExpansion.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.gatesofavarice.main"))
                    .icon(() -> new ItemStack(ModItems.GATEWAY_WORKBENCH.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.GATEWAY_WORKBENCH.get());
                        output.accept(ModItems.LOOTBOX.get());
                        output.accept(ModItems.POI_LOOTBOX.get());
                        output.accept(ModItems.MYTHIC_COIN_PILE.get());
                        output.accept(ModItems.KNOWLEDGE_BOOK.get());
                        output.accept(ModItems.SACRIFICIAL_ALTAR.get());

                        output.accept(ModItems.TIER_1_CRYSTAL.get());
                        output.accept(ModItems.TIER_2_CRYSTAL.get());
                        output.accept(ModItems.TIER_3_CRYSTAL.get());
                        output.accept(ModItems.TIER_4_CRYSTAL.get());
                        output.accept(ModItems.TIER_5_CRYSTAL.get());

                        output.accept(ModItems.MYTHIC_COIN.get());
                        output.accept(ModItems.USELESS_KNOWLEDGE_BOOK.get());
                        output.accept(ModItems.GODLY_KNOWLEDGE_BOOK.get());
                        output.accept(ModItems.GOLD_COIN.get());
                        output.accept(ModItems.BAIL_STONE.get());
                        output.accept(ModItems.GATEWAY_CARD.get());
                        output.accept(ModItems.COMMON_BOOSTER_PACK.get());
                        output.accept(ModItems.UNCOMMON_BOOSTER_PACK.get());
                        output.accept(ModItems.RARE_BOOSTER_PACK.get());
                        output.accept(ModItems.EPIC_BOOSTER_PACK.get());
                        output.accept(ModItems.LEGENDARY_BOOSTER_PACK.get());

                        output.accept(ModItems.GRIMSTONE.get());
                        output.accept(ModItems.MYSTIC_ESSENCE.get());
                        output.accept(ModItems.ARCANE_ESSENCE.get());
                        output.accept(ModItems.DARK_ESSENCE.get());
                        output.accept(ModItems.HARDENED_FLESH.get());
                        output.accept(ModItems.SHATTERED_BONES.get());
                        output.accept(ModItems.HEART_FRAGMENT.get());
                        output.accept(ModItems.MANASTONES.get());
                        output.accept(ModItems.MANA_GEMS.get());
                        output.accept(ModItems.PLASMA.get());
                        output.accept(ModItems.SILK_SPOOL.get());
                        output.accept(ModItems.SAPHIRE.get());
                        output.accept(ModItems.RUBY.get());
                        output.accept(ModItems.OPAL.get());
                        output.accept(ModItems.PRISMATIC_SHARD.get());
                        output.accept(ModItems.PRISMATIC_DIAMOND.get());
                        output.accept(ModItems.PRISMATIC_CORE.get());
                        output.accept(ModItems.SOLAR_SHARD.get());
                        output.accept(ModItems.PETRIFIED_SOUL_SHARD.get());
                        output.accept(ModItems.SCRAP_METAL.get());
                        output.accept(ModItems.RUSTY_COIN.get());

                        output.accept(ModItems.METAL_NUGGET.get());
                        output.accept(ModItems.METAL_INGOT.get());
                        output.accept(ModItems.METAL_BLOCK.get());
                        output.accept(ModItems.UPGRADE_BASE.get());

                        output.accept(ModItems.MANA_STEEL_SCRAP.get());
                        output.accept(ModItems.MANA_STEEL_INGOT.get());
                        output.accept(ModItems.MANA_STEEL_UPGRADE_TEMPLATE.get());
                        output.accept(ModItems.ELIXRITE_SCRAP.get());
                        output.accept(ModItems.ELIXRITE_INGOT.get());
                        output.accept(ModItems.ELIXRITE_UPGRADE_TEMPLATE.get());
                        output.accept(ModItems.ASTRITE_SCRAP.get());
                        output.accept(ModItems.ASTRITE_INGOT.get());
                        output.accept(ModItems.ASTRITE_UPGRADE_TEMPLATE.get());
                        output.accept(ModItems.LUNARIUM_SCRAP.get());
                        output.accept(ModItems.LUNARIUM_INGOT.get());
                        output.accept(ModItems.LUNARIUM_UPGRADE_TEMPLATE.get());
                        output.accept(ModItems.IGNITE_SCRAP.get());
                        output.accept(ModItems.IGNITE_INGOT.get());
                        output.accept(ModItems.IGNITE_UPGRADE_TEMPLATE.get());
                        output.accept(ModItems.IRIDIUM_SCRAP.get());
                        output.accept(ModItems.IRIDIUM_INGOT.get());
                        output.accept(ModItems.IRIDIUM_UPGRADE_TEMPLATE.get());
                        output.accept(ModItems.MYTHRIL_SCRAP.get());
                        output.accept(ModItems.MYTHRIL_INGOT.get());
                        output.accept(ModItems.MYTHRIL_UPGRADE_TEMPLATE.get());
                        output.accept(ModItems.ARCANIUM_SCRAP.get());
                        output.accept(ModItems.ARCANIUM_INGOT.get());
                        output.accept(ModItems.ARCANIUM_UPGRADE_TEMPLATE.get());
                        output.accept(ModItems.PRISMATIC_STEEL_SCRAP.get());
                        output.accept(ModItems.PRISMATIC_STEEL_INGOT.get());
                        output.accept(ModItems.PRISMATIC_STEEL_UPGRADE_TEMPLATE.get());

                        output.accept(ModItems.MAGNETITE_SCRAP.get());
                        output.accept(ModItems.MAGNETITE_INGOT.get());

                        output.accept(ModItems.STABILITY_PEARL.get());
                        output.accept(ModItems.ARCANE_APPLE.get());
                        output.accept(ModItems.ENCHANTED_ARCANE_APPLE.get());

                        output.accept(ModItems.DUNGEON_MAGNET.get());
                        output.accept(ModItems.MANA_STEEL_MAGNET.get());
                        output.accept(ModItems.ELIXRITE_MAGNET.get());
                        output.accept(ModItems.ASTRITE_MAGNET.get());
                        output.accept(ModItems.LUNARIUM_MAGNET.get());
                        output.accept(ModItems.IGNITE_MAGNET.get());
                        output.accept(ModItems.IRIDIUM_MAGNET.get());
                        output.accept(ModItems.MYTHRIL_MAGNET.get());
                        output.accept(ModItems.ARCANIUM_MAGNET.get());
                        output.accept(ModItems.PRISMATIC_STEEL_MAGNET.get());

                        output.accept(ModItems.MANA_STEEL_PAXEL.get());
                        output.accept(ModItems.ELIXRITE_PAXEL.get());
                        output.accept(ModItems.ASTRITE_PAXEL.get());
                        output.accept(ModItems.LUNARIUM_PAXEL.get());
                        output.accept(ModItems.IGNITE_PAXEL.get());
                        output.accept(ModItems.IRIDIUM_PAXEL.get());
                        output.accept(ModItems.MYTHRIL_PAXEL.get());
                        output.accept(ModItems.ARCANIUM_PAXEL.get());
                        output.accept(ModItems.PRISMATIC_STEEL_PAXEL.get());

                        output.accept(ModItems.MANA_STEEL_SWORD.get());
                        output.accept(ModItems.ELIXRITE_SWORD.get());
                        output.accept(ModItems.ASTRITE_SWORD.get());
                        output.accept(ModItems.LUNARIUM_SWORD.get());
                        output.accept(ModItems.IGNITE_SWORD.get());
                        output.accept(ModItems.IRIDIUM_SWORD.get());
                        output.accept(ModItems.MYTHRIL_SWORD.get());
                        output.accept(ModItems.ARCANIUM_SWORD.get());
                        output.accept(ModItems.PRISMATIC_STEEL_SWORD.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
    }
}
