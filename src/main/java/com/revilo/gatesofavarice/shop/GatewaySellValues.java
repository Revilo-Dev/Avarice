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
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        if (ResourceLocation.fromNamespaceAndPath("architects_palette", "withered_bone").equals(itemId)) return 1;
        if (ResourceLocation.fromNamespaceAndPath("architects_pallet", "withered_bone").equals(itemId)) return 1;
        if (item == Items.ROTTEN_FLESH) return 2;
        if (item == Items.BONE) return 2;
        if (item == Items.COAL) return 2;
        if (item == Items.POTATO) return 1;
        if (item == Items.ARROW) return 1;
        if (item == Items.STONE_SWORD) return 1;
        if (item == Items.SLIME_BALL) return 1;
        if (item == Items.SPIDER_EYE) return 2;
        if (item == Items.STRING) return 1;
        if (ResourceLocation.fromNamespaceAndPath("deeperdarker", "resonarium").equals(itemId)) return 15;
        if (item == ModItems.GRIMSTONE.get()) return 7;
        if (item == ModItems.MYSTIC_ESSENCE.get()) return 10;
        if (item == ModItems.HARDENED_FLESH.get()) return 5;
        if (item == ModItems.SHATTERED_BONES.get()) return 6;
        if (item == ModItems.SCRAP_METAL.get()) return 8;
        if (item == ModItems.RUSTY_COIN.get()) return 6;
        if (item == ModItems.HEART_FRAGMENT.get()) return 25;
        if (item == ModItems.MANA_GEMS.get()) return 24;
        if (item == ModItems.PLASMA.get()) return 30;
        if (item == ModItems.SAPHIRE.get()) return 75;
        if (item == ModItems.MANA_STEEL_SCRAP.get()) return 30;
        if (item == ModItems.MANA_STEEL_INGOT.get()) return 250;
        if (item == ModItems.MAGNETITE_SCRAP.get()) return 40;
        if (item == ModItems.MAGNETITE_INGOT.get()) return 150;
        if (item == ModItems.UPGRADE_BASE.get()) return 100;
        if (item == ModItems.ARCANE_ESSENCE.get()) return 35;
        if (item == ModItems.MANASTONES.get()) return 50;
        if (item == ModItems.ELIXRITE_SCRAP.get()) return 50;
        if (item == ModItems.ELIXRITE_INGOT.get()) return 450;
        if (item == ModItems.ASTRITE_SCRAP.get()) return 80;
        if (item == ModItems.ASTRITE_INGOT.get()) return 750;
        if (item == ModItems.SOLAR_SHARD.get()) return 70;
        if (item == ModItems.PETRIFIED_SOUL_SHARD.get()) return 75;
        if (item == ModItems.RUBY.get()) return 100;
        if (item == ModItems.OPAL.get()) return 150;
        if (item == ModItems.ARCANE_APPLE.get()) return 100;
        if (item == ModItems.ENCHANTED_ARCANE_APPLE.get()) return 400;
        if (item == ModItems.PRISMATIC_SHARD.get()) return 90;
        if (item == ModItems.PRISMATIC_DIAMOND.get()) return 850;
        if (item == ModItems.LUNARIUM_SCRAP.get()) return 150;
        if (item == ModItems.LUNARIUM_INGOT.get()) return 950;
        if (item == ModItems.IGNITE_SCRAP.get()) return 250;
        if (item == ModItems.IGNITE_INGOT.get()) return 1500;
        if (item == ModItems.IRIDIUM_SCRAP.get()) return 300;
        if (item == ModItems.IRIDIUM_INGOT.get()) return 2500;
        if (item == ModItems.MYTHRIL_SCRAP.get()) return 350;
        if (item == ModItems.MYTHRIL_INGOT.get()) return 3000;
        if (item == ModItems.ARCANIUM_SCRAP.get()) return 450;
        if (item == ModItems.ARCANIUM_INGOT.get()) return 4500;
        if (item == ModItems.PRISMATIC_STEEL_SCRAP.get()) return 500;
        if (item == ModItems.PRISMATIC_STEEL_INGOT.get()) return 5000;
        if (item == ModItems.DARK_ESSENCE.get()) return 150;
        if (item == ModItems.PRISMATIC_CORE.get()) return 10000;
        if (item == ModItems.STABILITY_PEARL.get()) return 500;
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
        if (!showSellValuesFromItemTooltips()) {
            return;
        }
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

    public static void appendDungeonSellValueTooltip(ItemStack stack, List<Component> tooltipComponents) {
        int value = getUnitValue(stack);
        if (value <= 0) {
            return;
        }

        tooltipComponents.add(Component.literal("Sell Value: " + value).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static boolean showSellValuesFromItemTooltips() {
        return false;
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
