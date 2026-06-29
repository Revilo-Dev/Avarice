package com.revilo.gatesofavarice.dungeon;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.CustomData;

public final class DungeonBoundItems {

    public static final String DUNGEON_BOUND_KEY = "dungeon_bound";
    public static final String WEAPON_ROLE_KEY = "dungeon_weapon_role";
    public static final String PRIMARY_WEAPON_ROLE = "primary";
    public static final String SECONDARY_WEAPON_ROLE = "secondary";

    private DungeonBoundItems() {
    }

    public static boolean isDungeonBound(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(GatewayExpansion.MOD_ID);
        return root.getBoolean(DUNGEON_BOUND_KEY);
    }

    public static void markIfDungeonBound(ItemStack stack) {
        if (!isBoundCandidate(stack)) {
            return;
        }
        forceMarkDungeonBound(stack);
    }

    public static void forceMarkDungeonBound(ItemStack stack) {
        if (stack.is(ModItems.GATEWAY_CARD.get())) {
            return;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag root = tag.getCompound(GatewayExpansion.MOD_ID);
        root.putBoolean(DUNGEON_BOUND_KEY, true);
        tag.put(GatewayExpansion.MOD_ID, root);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        applyArmorBindingCurse(stack);
    }

    public static void markPrimaryWeapon(ItemStack stack) {
        markWeaponRole(stack, PRIMARY_WEAPON_ROLE);
    }

    public static void markSecondaryWeapon(ItemStack stack) {
        markWeaponRole(stack, SECONDARY_WEAPON_ROLE);
    }

    public static String getWeaponRole(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(GatewayExpansion.MOD_ID);
        return root.getString(WEAPON_ROLE_KEY);
    }

    public static boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof DiggerItem
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof TridentItem;
    }

    public static boolean replaceRoleWeapon(Player player, ItemStack stack) {
        String role = getWeaponRole(stack);
        if (role.isBlank()) {
            return false;
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack existing = inv.items.get(i);
            if (role.equals(getWeaponRole(existing))) {
                inv.items.set(i, stack);
                player.inventoryMenu.broadcastChanges();
                player.containerMenu.broadcastChanges();
                return true;
            }
        }
        for (int i = 0; i < inv.offhand.size(); i++) {
            ItemStack existing = inv.offhand.get(i);
            if (role.equals(getWeaponRole(existing))) {
                inv.offhand.set(i, stack);
                player.inventoryMenu.broadcastChanges();
                player.containerMenu.broadcastChanges();
                return true;
            }
        }
        return false;
    }

    public static void removeBoundItems(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            if (isDungeonBound(inv.items.get(i))) {
                inv.items.set(i, ItemStack.EMPTY);
            }
        }
        for (int i = 0; i < inv.armor.size(); i++) {
            if (isDungeonBound(inv.armor.get(i))) {
                inv.armor.set(i, ItemStack.EMPTY);
            }
        }
        for (int i = 0; i < inv.offhand.size(); i++) {
            if (isDungeonBound(inv.offhand.get(i))) {
                inv.offhand.set(i, ItemStack.EMPTY);
            }
        }
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    private static boolean isBoundCandidate(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem
                || stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof DiggerItem
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof ShieldItem
                || stack.is(ModItems.HEART_FRAGMENT.get());
    }

    private static void markWeaponRole(ItemStack stack, String role) {
        if (!isWeapon(stack)) {
            return;
        }
        markIfDungeonBound(stack);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag root = tag.getCompound(GatewayExpansion.MOD_ID);
        root.putString(WEAPON_ROLE_KEY, role);
        tag.put(GatewayExpansion.MOD_ID, root);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void applyArmorBindingCurse(ItemStack stack) {
        // Applied in server-authoritative loadout flow where registry access is available.
    }
}
