package com.revilo.gatesofavarice.shop;

import com.revilo.gatesofavarice.registry.ModItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GatewaySellValues {

    private GatewaySellValues() {
    }

    public static boolean isSellable(ItemStack stack) {
        return getUnitValue(stack) > 0;
    }

    public static int getUnitValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        Item item = stack.getItem();
        if (item == Items.ROTTEN_FLESH) return 2;
        if (item == Items.SLIME_BALL) return 3;
        if (item == Items.SPIDER_EYE) return 2;
        if (item == Items.STRING) return 1;
        if (item == ModItems.GRIMSTONE.get()) return 18;
        if (item == ModItems.MYSTIC_ESSENCE.get()) return 20;
        if (item == ModItems.HARDENED_FLESH.get()) return 10;
        if (item == ModItems.SHATTERED_BONES.get()) return 12;
        if (item == ModItems.SCRAP_METAL.get()) return 16;
        if (item == ModItems.RUSTY_COIN.get()) return 12;
        if (item == ModItems.HEART_FRAGMENT.get()) return 52;
        if (item == ModItems.MANA_GEMS.get()) return 48;
        if (item == ModItems.PLASMA.get()) return 58;
        if (item == ModItems.SAPHIRE.get()) return 175;
        if (item == ModItems.MANA_STEEL_SCRAP.get()) return 36;
        if (item == ModItems.MANA_STEEL_INGOT.get()) return 324;
        if (item == ModItems.MAGNETITE_SCRAP.get()) return 44;
        if (item == ModItems.MAGNETITE_INGOT.get()) return 396;
        if (item == ModItems.UPGRADE_BASE.get()) return 200;
        if (item == ModItems.MANA_STEEL_MAGNET.get()) return 760;
        if (item == ModItems.MANA_STEEL_UPGRADE_TEMPLATE.get()) return 488;
        if (item == ModItems.ARCANE_ESSENCE.get()) return 60;
        if (item == ModItems.MANASTONES.get()) return 75;
        if (item == ModItems.ELIXRITE_SCRAP.get()) return 68;
        if (item == ModItems.ELIXRITE_INGOT.get()) return 612;
        if (item == ModItems.ELIXRITE_UPGRADE_TEMPLATE.get()) return 744;
        if (item == ModItems.ELIXRITE_MAGNET.get()) return 1380;
        if (item == ModItems.ASTRITE_SCRAP.get()) return 96;
        if (item == ModItems.ASTRITE_INGOT.get()) return 864;
        if (item == ModItems.ASTRITE_UPGRADE_TEMPLATE.get()) return 968;
        if (item == ModItems.ASTRITE_MAGNET.get()) return 1860;
        if (item == ModItems.SOLAR_SHARD.get()) return 140;
        if (item == ModItems.PETRIFIED_SOUL_SHARD.get()) return 170;
        if (item == ModItems.RUBY.get()) return 185;
        if (item == ModItems.OPAL.get()) return 320;
        if (item == ModItems.ARCANE_APPLE.get()) return 180;
        if (item == ModItems.ENCHANTED_ARCANE_APPLE.get()) return 420;
        if (item == ModItems.PRISMATIC_SHARD.get()) return 32;
        if (item == ModItems.PRISMATIC_DIAMOND.get()) return 360;
        if (item == ModItems.LUNARIUM_SCRAP.get()) return 180;
        if (item == ModItems.LUNARIUM_INGOT.get()) return 1620;
        if (item == ModItems.LUNARIUM_UPGRADE_TEMPLATE.get()) return 1640;
        if (item == ModItems.LUNARIUM_MAGNET.get()) return 3480;
        if (item == ModItems.IGNITE_SCRAP.get()) return 220;
        if (item == ModItems.IGNITE_INGOT.get()) return 1980;
        if (item == ModItems.IGNITE_UPGRADE_TEMPLATE.get()) return 1960;
        if (item == ModItems.IGNITE_MAGNET.get()) return 4220;
        if (item == ModItems.IRIDIUM_SCRAP.get()) return 280;
        if (item == ModItems.IRIDIUM_INGOT.get()) return 2520;
        if (item == ModItems.IRIDIUM_UPGRADE_TEMPLATE.get()) return 2440;
        if (item == ModItems.IRIDIUM_MAGNET.get()) return 5120;
        if (item == ModItems.MYTHRIL_SCRAP.get()) return 340;
        if (item == ModItems.MYTHRIL_INGOT.get()) return 3060;
        if (item == ModItems.MYTHRIL_UPGRADE_TEMPLATE.get()) return 24680;
        if (item == ModItems.MYTHRIL_MAGNET.get()) return 6180;
        if (item == ModItems.ARCANIUM_SCRAP.get()) return 440;
        if (item == ModItems.ARCANIUM_INGOT.get()) return 3960;
        if (item == ModItems.ARCANIUM_UPGRADE_TEMPLATE.get()) return 31880;
        if (item == ModItems.ARCANIUM_MAGNET.get()) return 7420;
        if (item == ModItems.PRISMATIC_STEEL_SCRAP.get()) return 560;
        if (item == ModItems.PRISMATIC_STEEL_INGOT.get()) return 5040;
        if (item == ModItems.PRISMATIC_STEEL_UPGRADE_TEMPLATE.get()) return 40520;
        if (item == ModItems.PRISMATIC_STEEL_MAGNET.get()) return 9480;
        if (item == ModItems.DARK_ESSENCE.get()) return 210;
        if (item == ModItems.PRISMATIC_CORE.get()) return 540;
        if (item == ModItems.TIER_1_CRYSTAL.get()) return 0;
        if (item == ModItems.TIER_2_CRYSTAL.get()) return 0;
        if (item == ModItems.TIER_3_CRYSTAL.get()) return 0;
        if (item == ModItems.TIER_4_CRYSTAL.get()) return 0;
        if (item == ModItems.TIER_5_CRYSTAL.get()) return 0;
        if (item == ModItems.STABILITY_PEARL.get()) return 520;
        if (item == ModItems.MANA_STEEL_PAXEL.get()) return 980;
        if (item == ModItems.MANA_STEEL_SWORD.get()) return 1320;
        if (item == ModItems.ELIXRITE_PAXEL.get()) return 1840;
        if (item == ModItems.ELIXRITE_SWORD.get()) return 2080;
        if (item == ModItems.ASTRITE_PAXEL.get()) return 2480;
        if (item == ModItems.ASTRITE_SWORD.get()) return 2920;
        if (item == ModItems.LUNARIUM_PAXEL.get()) return 4860;
        if (item == ModItems.LUNARIUM_SWORD.get()) return 5240;
        if (item == ModItems.IGNITE_PAXEL.get()) return 5920;
        if (item == ModItems.IGNITE_SWORD.get()) return 6380;
        if (item == ModItems.IRIDIUM_PAXEL.get()) return 7240;
        if (item == ModItems.IRIDIUM_SWORD.get()) return 7760;
        if (item == ModItems.MYTHRIL_PAXEL.get()) return 8680;
        if (item == ModItems.MYTHRIL_SWORD.get()) return 9300;
        if (item == ModItems.ARCANIUM_PAXEL.get()) return 10360;
        if (item == ModItems.ARCANIUM_SWORD.get()) return 11120;
        if (item == ModItems.PRISMATIC_STEEL_PAXEL.get()) return 12600;
        if (item == ModItems.PRISMATIC_STEEL_SWORD.get()) return 14000;
        if (item == ModItems.SHOP_GATEWAY.get()) return 260;
        if (item == ModItems.GATEWAY_WORKBENCH.get()) return 420;
        int runicValue = getRunicUnitValue(item);
        if (runicValue > 0) {
            return runicValue;
        }

        return 0;
    }

    public static int getSuggestedBuyPrice(ItemStack stack) {
        int unitValue = getUnitValue(stack);
        return unitValue <= 0 ? 0 : Math.max(1, (int) Math.ceil(unitValue * rarityBuyMultiplier(stack)));
    }

    public static int getStackValue(ItemStack stack) {
        return getUnitValue(stack) * stack.getCount();
    }

    public static void appendSellValueTooltip(ItemStack stack, List<Component> tooltipComponents) {
        int value = getUnitValue(stack);
        if (value <= 0) {
            return;
        }

        tooltipComponents.add(Component.literal("⛂ " + value + " Sell Value").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    public static void appendShopRuneSellValueTooltip(ItemStack stack, List<Component> tooltipComponents) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || !"runic".equals(id.getNamespace())) {
            return;
        }
        appendSellValueTooltip(stack, tooltipComponents);
    }

    private static int getRunicUnitValue(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null || !"runic".equals(id.getNamespace())) {
            return 0;
        }

        return switch (id.getPath()) {
            case "blank_etching" -> 22;
            case "etching" -> 34;
            case "blank_inscription" -> 26;
            case "repair_rune" -> 34;
            case "enhanced_rune" -> 52;
            case "reroll_inscription" -> 78;
            case "expansion_rune" -> 92;
            case "nullification_rune" -> 104;
            case "upgrade_rune" -> 118;
            case "wild_inscription" -> 126;
            case "extraction_inscription" -> 142;
            case "cursed_inscription" -> 168;
            case "artisans_workbench" -> 180;
            case "etching_table" -> 220;
            default -> 0;
        };
    }

    private static double rarityBuyMultiplier(ItemStack stack) {
        return switch (stack.getRarity()) {
            case COMMON -> 2.5D;
            case UNCOMMON -> 4.5D;
            case RARE -> 6.5D;
            case EPIC -> 9.0D;
        };
    }
}
