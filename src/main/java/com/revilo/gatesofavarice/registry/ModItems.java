package com.revilo.gatesofavarice.registry;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.item.ArcaneAppleItem;
import com.revilo.gatesofavarice.catalyst.CatalystArchetype;
import com.revilo.gatesofavarice.item.AugmentItem;
import com.revilo.gatesofavarice.item.CatalystItem;
import com.revilo.gatesofavarice.item.CrystalItem;
import com.revilo.gatesofavarice.item.GatewaySwordItem;
import com.revilo.gatesofavarice.item.HeartFragmentItem;
import com.revilo.gatesofavarice.item.LootMaterialItem;
import com.revilo.gatesofavarice.item.MagnetItem;
import com.revilo.gatesofavarice.item.MythicCoinItem;
import com.revilo.gatesofavarice.item.PaxelItem;
import com.revilo.gatesofavarice.item.RarityTintedSmithingTemplateItem;
import com.revilo.gatesofavarice.item.ShopGatewayPearlItem;
import com.revilo.gatesofavarice.item.StabilityPearlItem;
import com.revilo.gatesofavarice.item.data.AugmentDifficultyTier;
import com.revilo.gatesofavarice.item.data.LootRarity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GatewayExpansion.MOD_ID);
    private static final List<ResourceLocation> SWORD_SLOT_ICONS = List.of(ResourceLocation.withDefaultNamespace("item/empty_slot_sword"));
    private static final List<ResourceLocation> INGOT_SLOT_ICONS = List.of(ResourceLocation.withDefaultNamespace("item/empty_slot_ingot"));
    private static final FoodProperties ARCANE_APPLE_FOOD = new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).alwaysEdible().build();

    public static final DeferredHolder<Item, CrystalItem> TIER_1_CRYSTAL = ITEMS.register("tier_1_crystal",
            () -> new CrystalItem(new CrystalItem.CrystalTier(1, 0, 19), new Item.Properties()));
    public static final DeferredHolder<Item, CrystalItem> TIER_2_CRYSTAL = ITEMS.register("tier_2_crystal",
            () -> new CrystalItem(new CrystalItem.CrystalTier(2, 20, 49), new Item.Properties()));
    public static final DeferredHolder<Item, CrystalItem> TIER_3_CRYSTAL = ITEMS.register("tier_3_crystal",
            () -> new CrystalItem(new CrystalItem.CrystalTier(3, 50, 69), new Item.Properties()));
    public static final DeferredHolder<Item, CrystalItem> TIER_4_CRYSTAL = ITEMS.register("tier_4_crystal",
            () -> new CrystalItem(new CrystalItem.CrystalTier(4, 70, 89), new Item.Properties()));
    public static final DeferredHolder<Item, CrystalItem> TIER_5_CRYSTAL = ITEMS.register("tier_5_crystal",
            () -> new CrystalItem(new CrystalItem.CrystalTier(5, 90, 100), new Item.Properties()));

    public static final DeferredHolder<Item, AugmentItem> EASY_AUGMENT = ITEMS.register("easy_augment",
            () -> new AugmentItem(AugmentDifficultyTier.EASY, new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, AugmentItem> MEDIUM_AUGMENT = ITEMS.register("medium_augment",
            () -> new AugmentItem(AugmentDifficultyTier.MEDIUM, new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, AugmentItem> HARD_AUGMENT = ITEMS.register("hard_augment",
            () -> new AugmentItem(AugmentDifficultyTier.HARD, new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, AugmentItem> EXTREME_AUGMENT = ITEMS.register("extreme_augment",
            () -> new AugmentItem(AugmentDifficultyTier.EXTREME, new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, CatalystItem> TIME_CATALYST = ITEMS.register("time_catalyst",
            () -> new CatalystItem(CatalystArchetype.TIME, new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, CatalystItem> STAT_CATALYST = ITEMS.register("stat_catalyst",
            () -> new CatalystItem(CatalystArchetype.STAT, new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, CatalystItem> LOOT_CATALYST = ITEMS.register("loot_catalyst",
            () -> new CatalystItem(CatalystArchetype.LOOT, new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, CatalystItem> HIGHRISK_CATALYST = ITEMS.register("highrisk_catalyst",
            () -> new CatalystItem(CatalystArchetype.VOLATILE, new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, MythicCoinItem> MYTHIC_COIN = ITEMS.register("mythic_coin",
            () -> new MythicCoinItem(new Item.Properties().stacksTo(99)));
    public static final DeferredHolder<Item, StabilityPearlItem> STABILITY_PEARL = ITEMS.register("stability_pearl",
            () -> new StabilityPearlItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, LootMaterialItem> GRIMSTONE = ITEMS.register("grimstone",
            () -> new LootMaterialItem(LootRarity.COMMON, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, LootMaterialItem> MYSTIC_ESSENCE = ITEMS.register("mystic_essence",
            () -> new LootMaterialItem(LootRarity.COMMON, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, LootMaterialItem> HARDENED_FLESH = ITEMS.register("hardened_flesh",
            () -> new LootMaterialItem(LootRarity.COMMON, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, LootMaterialItem> SHATTERED_BONES = ITEMS.register("shattered_bones",
            () -> new LootMaterialItem(LootRarity.COMMON, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, LootMaterialItem> DARK_ESSENCE = ITEMS.register("dark_essence",
            () -> new LootMaterialItem(LootRarity.RARE, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, LootMaterialItem> ARCANE_ESSENCE = ITEMS.register("arcane_essence",
            () -> new LootMaterialItem(LootRarity.UNCOMMON, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, HeartFragmentItem> HEART_FRAGMENT = ITEMS.register("heart_fragment",
            () -> new HeartFragmentItem(LootRarity.UNCOMMON, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, LootMaterialItem> MANASTONES = ITEMS.register("manastones",
            () -> new LootMaterialItem(LootRarity.UNCOMMON, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, LootMaterialItem> MANA_GEMS = ITEMS.register("mana_gems",
            () -> new LootMaterialItem(LootRarity.UNCOMMON, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, LootMaterialItem> PLASMA = ITEMS.register("plasma",
            () -> new LootMaterialItem(LootRarity.UNCOMMON, new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, LootMaterialItem> SAPHIRE = ITEMS.register("saphire",
            () -> new LootMaterialItem(LootRarity.RARE, new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, LootMaterialItem> MANA_STEEL_SCRAP = ITEMS.register("mana_steel_scrap",
            () -> new LootMaterialItem(LootRarity.COMMON, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, LootMaterialItem> MANA_STEEL_INGOT = ITEMS.register("mana_steel_ingot",
            () -> new LootMaterialItem(LootRarity.COMMON, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, LootMaterialItem> MAGNETITE_SCRAP = ITEMS.register("magnetite_scrap",
            () -> new LootMaterialItem(LootRarity.UNCOMMON, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, LootMaterialItem> MAGNETITE_INGOT = ITEMS.register("magnetite_ingot",
            () -> new LootMaterialItem(LootRarity.UNCOMMON, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, MagnetItem> MANA_STEEL_MAGNET = ITEMS.register("mana_steel_magnet",
            () -> new MagnetItem(LootRarity.COMMON.color(), 1, 5, 2, new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, LootMaterialItem> SCRAP_METAL = ITEMS.register("scrap_metal",
            () -> new LootMaterialItem(LootRarity.COMMON, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, LootMaterialItem> RUSTY_COIN = ITEMS.register("rusty_coin",
            () -> new LootMaterialItem(LootRarity.COMMON, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, LootMaterialItem> ELIXRITE_SCRAP = ITEMS.register("elixrite_scrap",
            () -> new LootMaterialItem(LootRarity.UNCOMMON, new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, LootMaterialItem> ELIXRITE_INGOT = ITEMS.register("elixrite_ingot",
            () -> new LootMaterialItem(LootRarity.UNCOMMON, new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, MagnetItem> ELIXRITE_MAGNET = ITEMS.register("elixrite_magnet",
            () -> new MagnetItem(LootRarity.UNCOMMON.color(), 2, 7, 2, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, LootMaterialItem> ASTRITE_SCRAP = ITEMS.register("astrite_scrap",
            () -> new LootMaterialItem(LootRarity.UNCOMMON, new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, LootMaterialItem> ASTRITE_INGOT = ITEMS.register("astrite_ingot",
            () -> new LootMaterialItem(LootRarity.UNCOMMON, new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, MagnetItem> ASTRITE_MAGNET = ITEMS.register("astrite_magnet",
            () -> new MagnetItem(LootRarity.UNCOMMON.color(), 3, 9, 2, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, LootMaterialItem> PRISMATIC_CORE = ITEMS.register("prismatic_core",
            () -> new LootMaterialItem(LootRarity.LEGENDARY, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, LootMaterialItem> SOLAR_SHARD = ITEMS.register("solar_shard",
            () -> new LootMaterialItem(LootRarity.RARE, new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, LootMaterialItem> PETRIFIED_SOUL_SHARD = ITEMS.register("petrified_soul_shard",
            () -> new LootMaterialItem(LootRarity.RARE, new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, LootMaterialItem> RUBY = ITEMS.register("ruby",
            () -> new LootMaterialItem(LootRarity.RARE, new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, LootMaterialItem> OPAL = ITEMS.register("opal",
            () -> new LootMaterialItem(LootRarity.EPIC, new Item.Properties().stacksTo(64).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, ArcaneAppleItem> ARCANE_APPLE = ITEMS.register("arcane_apple",
            () -> new ArcaneAppleItem(
                    LootRarity.RARE,
                    new Item.Properties().stacksTo(16).rarity(Rarity.RARE).food(ARCANE_APPLE_FOOD),
                    ResourceLocation.fromNamespaceAndPath("codex", "ability_power_boost"),
                    10 * 20,
                    0,
                    false));
    public static final DeferredHolder<Item, ArcaneAppleItem> ENCHANTED_ARCANE_APPLE = ITEMS.register("enchanted_arcane_apple",
            () -> new ArcaneAppleItem(
                    LootRarity.EPIC,
                    new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).food(ARCANE_APPLE_FOOD),
                    ResourceLocation.fromNamespaceAndPath("codex", "ability_power_boost"),
                    5 * 20,
                    2,
                    true));
    public static final DeferredHolder<Item, LootMaterialItem> PRISMATIC_SHARD = ITEMS.register("prismatic_shard",
            () -> new LootMaterialItem(LootRarity.EPIC, new Item.Properties().stacksTo(64).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, LootMaterialItem> PRISMATIC_DIAMOND = ITEMS.register("prismatic_diamond",
            () -> new LootMaterialItem(LootRarity.LEGENDARY, new Item.Properties().stacksTo(64).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, LootMaterialItem> LUNARIUM_SCRAP = ITEMS.register("lunarium_scrap",
            () -> new LootMaterialItem(LootRarity.RARE, new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, LootMaterialItem> LUNARIUM_INGOT = ITEMS.register("lunarium_ingot",
            () -> new LootMaterialItem(LootRarity.RARE, new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, MagnetItem> LUNARIUM_MAGNET = ITEMS.register("lunarium_magnet",
            () -> new MagnetItem(LootRarity.RARE.color(), 3, 11, 2, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, LootMaterialItem> UPGRADE_BASE = ITEMS.register("upgrade_base",
            () -> new LootMaterialItem(LootRarity.UNCOMMON, new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, SmithingTemplateItem> MANA_STEEL_UPGRADE_TEMPLATE = ITEMS.register("mana_steel_upgrade_template",
            () -> swordUpgradeTemplate("mana_steel"));
    public static final DeferredHolder<Item, SmithingTemplateItem> ELIXRITE_UPGRADE_TEMPLATE = ITEMS.register("elixrite_upgrade_template",
            () -> swordUpgradeTemplate("elixrite"));
    public static final DeferredHolder<Item, SmithingTemplateItem> ASTRITE_UPGRADE_TEMPLATE = ITEMS.register("astrite_upgrade_template",
            () -> swordUpgradeTemplate("astrite"));
    public static final DeferredHolder<Item, SmithingTemplateItem> LUNARIUM_UPGRADE_TEMPLATE = ITEMS.register("lunarium_upgrade_template",
            () -> swordUpgradeTemplate("lunarium"));
    public static final DeferredHolder<Item, SmithingTemplateItem> IGNITE_UPGRADE_TEMPLATE = ITEMS.register("ignite_upgrade_template",
            () -> swordUpgradeTemplate("ignite"));
    public static final DeferredHolder<Item, SmithingTemplateItem> IRIDIUM_UPGRADE_TEMPLATE = ITEMS.register("iridium_upgrade_template",
            () -> swordUpgradeTemplate("iridium"));
    public static final DeferredHolder<Item, SmithingTemplateItem> MYTHRIL_UPGRADE_TEMPLATE = ITEMS.register("mythril_upgrade_template",
            () -> swordUpgradeTemplate("mythril"));
    public static final DeferredHolder<Item, SmithingTemplateItem> ARCANIUM_UPGRADE_TEMPLATE = ITEMS.register("arcanium_upgrade_template",
            () -> swordUpgradeTemplate("arcanium"));
    public static final DeferredHolder<Item, SmithingTemplateItem> PRISMATIC_STEEL_UPGRADE_TEMPLATE = ITEMS.register("prismatic_steel_upgrade_template",
            () -> swordUpgradeTemplate("prismatic_steel"));
    public static final DeferredHolder<Item, GatewaySwordItem> MANA_STEEL_SWORD = ITEMS.register("mana_steel_sword",
            () -> new GatewaySwordItem(ModToolTiers.MANA_STEEL, new Item.Properties(), LootRarity.COMMON.color(), 6.0F, -2.4F, 0, 0, 0, 0.0D, 2, List.of()));
    public static final DeferredHolder<Item, GatewaySwordItem> ELIXRITE_SWORD = ITEMS.register("elixrite_sword",
            () -> new GatewaySwordItem(ModToolTiers.ELIXRITE, new Item.Properties().rarity(Rarity.UNCOMMON), LootRarity.UNCOMMON.color(), 6.0F, -2.4F, 0, 0, 0, 0.0D, 2, List.of()));
    public static final DeferredHolder<Item, GatewaySwordItem> ASTRITE_SWORD = ITEMS.register("astrite_sword",
            () -> new GatewaySwordItem(ModToolTiers.ASTRITE, new Item.Properties().rarity(Rarity.UNCOMMON), LootRarity.UNCOMMON.color(), 6.0F, -2.3F, 0, 0, 0, 0.0D, 3, List.of()));
    public static final DeferredHolder<Item, GatewaySwordItem> LUNARIUM_SWORD = ITEMS.register("lunarium_sword",
            () -> new GatewaySwordItem(ModToolTiers.LUNARIUM, new Item.Properties().rarity(Rarity.RARE), LootRarity.RARE.color(), 6.0F, -2.3F, 60, 0, 0, 0.0D, 3,
                    List.of(Component.literal("slowness hit"))));
    public static final DeferredHolder<Item, PaxelItem> MANA_STEEL_PAXEL = ITEMS.register("mana_steel_paxel",
            () -> new PaxelItem(ModToolTiers.MANA_STEEL, new Item.Properties(), LootRarity.COMMON.color(), 4.0F, -2.9F, 3));
    public static final DeferredHolder<Item, PaxelItem> ELIXRITE_PAXEL = ITEMS.register("elixrite_paxel",
            () -> new PaxelItem(ModToolTiers.ELIXRITE, new Item.Properties().rarity(Rarity.UNCOMMON), LootRarity.UNCOMMON.color(), 5.0F, -2.8F, 4));
    public static final DeferredHolder<Item, PaxelItem> ASTRITE_PAXEL = ITEMS.register("astrite_paxel",
            () -> new PaxelItem(ModToolTiers.ASTRITE, new Item.Properties().rarity(Rarity.UNCOMMON), LootRarity.UNCOMMON.color(), 5.0F, -2.8F, 5));
    public static final DeferredHolder<Item, PaxelItem> LUNARIUM_PAXEL = ITEMS.register("lunarium_paxel",
            () -> new PaxelItem(ModToolTiers.LUNARIUM, new Item.Properties().rarity(Rarity.RARE), LootRarity.RARE.color(), 6.0F, -2.8F, 6));
    public static final DeferredHolder<Item, LootMaterialItem> IGNITE_SCRAP = ITEMS.register("ignite_scrap",
            () -> new LootMaterialItem(LootRarity.RARE, new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, LootMaterialItem> IGNITE_INGOT = ITEMS.register("ignite_ingot",
            () -> new LootMaterialItem(LootRarity.RARE, new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, MagnetItem> IGNITE_MAGNET = ITEMS.register("ignite_magnet",
            () -> new MagnetItem(LootRarity.RARE.color(), 4, 13, 2, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, GatewaySwordItem> IGNITE_SWORD = ITEMS.register("ignite_sword",
            () -> new GatewaySwordItem(ModToolTiers.IGNITE, new Item.Properties().rarity(Rarity.RARE), LootRarity.RARE.color(), 6.0F, -2.3F, 60, 0, 4, 0.25D, 4,
                    List.of(Component.literal("slowness hit"), Component.literal("Fire aspect"))));
    public static final DeferredHolder<Item, PaxelItem> IGNITE_PAXEL = ITEMS.register("ignite_paxel",
            () -> new PaxelItem(ModToolTiers.IGNITE, new Item.Properties().rarity(Rarity.RARE), LootRarity.RARE.color(), 6.5F, -2.75F, 7));
    public static final DeferredHolder<Item, LootMaterialItem> IRIDIUM_SCRAP = ITEMS.register("iridium_scrap",
            () -> new LootMaterialItem(LootRarity.EPIC, new Item.Properties().stacksTo(64).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, LootMaterialItem> IRIDIUM_INGOT = ITEMS.register("iridium_ingot",
            () -> new LootMaterialItem(LootRarity.EPIC, new Item.Properties().stacksTo(64).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, MagnetItem> IRIDIUM_MAGNET = ITEMS.register("iridium_magnet",
            () -> new MagnetItem(LootRarity.EPIC.color(), 4, 15, 2, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, GatewaySwordItem> IRIDIUM_SWORD = ITEMS.register("iridium_sword",
            () -> new GatewaySwordItem(ModToolTiers.IRIDIUM, new Item.Properties().rarity(Rarity.EPIC), LootRarity.EPIC.color(), 6.0F, -2.3F, 60, 0, 4, 0.25D, 4,
                    List.of(Component.literal("slowness hit"), Component.literal("Fire aspect"))));
    public static final DeferredHolder<Item, PaxelItem> IRIDIUM_PAXEL = ITEMS.register("iridium_paxel",
            () -> new PaxelItem(ModToolTiers.IRIDIUM, new Item.Properties().rarity(Rarity.EPIC), LootRarity.EPIC.color(), 7.0F, -2.7F, 8));
    public static final DeferredHolder<Item, LootMaterialItem> MYTHRIL_SCRAP = ITEMS.register("mythril_scrap",
            () -> new LootMaterialItem(LootRarity.EPIC, new Item.Properties().stacksTo(64).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, LootMaterialItem> MYTHRIL_INGOT = ITEMS.register("mythril_ingot",
            () -> new LootMaterialItem(LootRarity.EPIC, new Item.Properties().stacksTo(64).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, MagnetItem> MYTHRIL_MAGNET = ITEMS.register("mythril_magnet",
            () -> new MagnetItem(LootRarity.EPIC.color(), 5, 17, 2, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, GatewaySwordItem> MYTHRIL_SWORD = ITEMS.register("mythril_sword",
            () -> new GatewaySwordItem(ModToolTiers.MYTHRIL, new Item.Properties().rarity(Rarity.EPIC), LootRarity.EPIC.color(), 6.0F, -2.2F, 60, 0, 4, 0.25D, 5,
                    List.of(Component.literal("slowness hit"), Component.literal("Fire aspect"))));
    public static final DeferredHolder<Item, PaxelItem> MYTHRIL_PAXEL = ITEMS.register("mythril_paxel",
            () -> new PaxelItem(ModToolTiers.MYTHRIL, new Item.Properties().rarity(Rarity.EPIC), LootRarity.EPIC.color(), 7.5F, -2.7F, 9));
    public static final DeferredHolder<Item, LootMaterialItem> ARCANIUM_SCRAP = ITEMS.register("arcanium_scrap",
            () -> new LootMaterialItem(LootRarity.LEGENDARY, new Item.Properties().stacksTo(64).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, LootMaterialItem> ARCANIUM_INGOT = ITEMS.register("arcanium_ingot",
            () -> new LootMaterialItem(LootRarity.LEGENDARY, new Item.Properties().stacksTo(64).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, MagnetItem> ARCANIUM_MAGNET = ITEMS.register("arcanium_magnet",
            () -> new MagnetItem(LootRarity.LEGENDARY.color(), 6, 18, 2, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, GatewaySwordItem> ARCANIUM_SWORD = ITEMS.register("arcanium_sword",
            () -> new GatewaySwordItem(ModToolTiers.ARCANIUM, new Item.Properties().rarity(Rarity.EPIC), LootRarity.LEGENDARY.color(), 6.0F, -2.2F, 60, 80, 4, 0.25D, 5,
                    List.of(Component.literal("slowness hit"), Component.literal("Fire aspect"), Component.literal("poison hit"))));
    public static final DeferredHolder<Item, PaxelItem> ARCANIUM_PAXEL = ITEMS.register("arcanium_paxel",
            () -> new PaxelItem(ModToolTiers.ARCANIUM, new Item.Properties().rarity(Rarity.EPIC), LootRarity.LEGENDARY.color(), 8.0F, -2.65F, 10));
    public static final DeferredHolder<Item, LootMaterialItem> PRISMATIC_STEEL_SCRAP = ITEMS.register("prismatic_steel_scrap",
            () -> new LootMaterialItem(LootRarity.LEGENDARY, new Item.Properties().stacksTo(64).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, LootMaterialItem> PRISMATIC_STEEL_INGOT = ITEMS.register("prismatic_steel_ingot",
            () -> new LootMaterialItem(LootRarity.LEGENDARY, new Item.Properties().stacksTo(64).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, MagnetItem> PRISMATIC_STEEL_MAGNET = ITEMS.register("prismatic_steel_magnet",
            () -> new MagnetItem(LootRarity.LEGENDARY.color(), 8, 20, 3, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, GatewaySwordItem> PRISMATIC_STEEL_SWORD = ITEMS.register("prismatic_steel_sword",
            () -> new GatewaySwordItem(ModToolTiers.PRISMATIC_STEEL, new Item.Properties().rarity(Rarity.EPIC), LootRarity.LEGENDARY.color(), 10.0F, -2.2F, 60, 80, 4, 0.25D, 6,
                    List.of(Component.literal("slowness hit"), Component.literal("Fire aspect"), Component.literal("poison hit"))));
    public static final DeferredHolder<Item, PaxelItem> PRISMATIC_STEEL_PAXEL = ITEMS.register("prismatic_steel_paxel",
            () -> new PaxelItem(ModToolTiers.PRISMATIC_STEEL, new Item.Properties().rarity(Rarity.EPIC), LootRarity.LEGENDARY.color(), 9.0F, -2.6F, 11));
    public static final DeferredHolder<Item, ShopGatewayPearlItem> SHOP_GATEWAY = ITEMS.register("shop_gateway",
            () -> new ShopGatewayPearlItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, BlockItem> GATEWAY_WORKBENCH = ITEMS.register("gateway_workbench",
            () -> new BlockItem(ModBlocks.GATEWAY_WORKBENCH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> LOOTBOX = ITEMS.register("lootbox",
            () -> new BlockItem(ModBlocks.LOOTBOX.get(), new Item.Properties()));

    private ModItems() {
    }

    private static SmithingTemplateItem swordUpgradeTemplate(String upgradeName) {
        LootRarity rarity = switch (upgradeName) {
            case "mana_steel" -> LootRarity.COMMON;
            case "elixrite", "astrite" -> LootRarity.UNCOMMON;
            case "lunarium", "ignite" -> LootRarity.RARE;
            case "iridium", "mythril" -> LootRarity.EPIC;
            case "arcanium", "prismatic_steel" -> LootRarity.LEGENDARY;
            default -> LootRarity.COMMON;
        };
        String descriptionId = Util.makeDescriptionId("upgrade", ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, upgradeName));
        return new RarityTintedSmithingTemplateItem(
                rarity.color(),
                Component.translatable("item.gatesofavarice.smithing_template.sword_upgrade.applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.gatesofavarice.smithing_template.sword_upgrade.ingredients", Component.translatable("item.gatesofavarice." + upgradeName + "_ingot")).withStyle(ChatFormatting.BLUE),
                Component.translatable(descriptionId).withStyle(ChatFormatting.GRAY),
                Component.translatable("item.gatesofavarice.smithing_template.sword_upgrade.base_slot_description"),
                Component.translatable("item.gatesofavarice.smithing_template.sword_upgrade.additions_slot_description"),
                SWORD_SLOT_ICONS,
                INGOT_SLOT_ICONS
        );
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
