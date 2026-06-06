package com.revilo.gatesofavarice.dungeon;

import com.revilo.gatesofavarice.item.MagnetItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.Unbreakable;

public final class DungeonGearRoller {
    private DungeonGearRoller() {
    }

    public static void rollAndBind(ItemStack stack, RandomSource random) {
        rollAndBind(stack, random, 1, 0L, null);
    }

    public static void rollAndBind(ItemStack stack, RandomSource random, int playerLevel, long timeInDungeonTicks) {
        rollAndBind(stack, random, playerLevel, timeInDungeonTicks, null);
    }

    public static void rollAndBind(ItemStack stack, RandomSource random, int playerLevel, long timeInDungeonTicks, RegistryAccess registryAccess) {
        if (stack.isEmpty()) {
            return;
        }
        if (!isDungeonGear(stack)) {
            DungeonBoundItems.markIfDungeonBound(stack);
            return;
        }
        DungeonBoundItems.markIfDungeonBound(stack);
        stack.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
        stack.set(DataComponents.LORE, ItemLore.EMPTY);
        if (registryAccess != null && stack.getItem() instanceof CrossbowItem) {
            var multishot = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.MULTISHOT).orElse(null);
            if (multishot != null) {
                net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(multishot, 1));
            }
        }
        // No custom stat/variance/thorns/health modifiers are applied here.
        // RUNIC is the only custom stat/effect system for dungeon loadout gear.
    }

    public static boolean isDungeonGear(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem
                || stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof MagnetItem
                || isArsenalWeapon(stack);
    }

    public static boolean isArsenalWeapon(ItemStack stack) {
        net.minecraft.resources.ResourceLocation id = stack.getItemHolder().unwrapKey().map(key -> key.location()).orElse(null);
        return id != null && "arsenal".equals(id.getNamespace()) && (id.getPath().contains("sword")
                || id.getPath().contains("dagger")
                || id.getPath().contains("machete")
                || id.getPath().contains("glaive")
                || id.getPath().contains("hammer")
                || id.getPath().contains("broadsword")
                || id.getPath().contains("longsword")
                || id.getPath().contains("gaundao"));
    }
}
