package com.revilo.gatesofavarice.dungeon;

import com.revilo.gatesofavarice.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative weapon slots used exclusively during dungeon runs. */
public final class DungeonWeaponEquipment {
    public static final int PRIMARY_HOTBAR_SLOT = 0;
    public static final int SECONDARY_HOTBAR_SLOT = 1;

    private DungeonWeaponEquipment() {
    }

    public static ItemStack primary(Player player) {
        return player.getInventory().getItem(PRIMARY_HOTBAR_SLOT).copy();
    }

    public static ItemStack secondary(Player player) {
        return player.getInventory().getItem(SECONDARY_HOTBAR_SLOT).copy();
    }

    public static ItemStack forRole(Player player, String role) {
        return DungeonBoundItems.SECONDARY_WEAPON_ROLE.equals(role) ? secondary(player) : primary(player);
    }

    public static void equip(Player player, ItemStack stack) {
        if (!DungeonBoundItems.isWeapon(stack)) {
            return;
        }
        ItemStack equipped = stack.copy();
        equipped.setCount(1);
        String role = DungeonBoundItems.getWeaponRole(equipped);
        if (DungeonBoundItems.SECONDARY_WEAPON_ROLE.equals(role)) {
            player.setData(ModAttachments.SECONDARY_DUNGEON_WEAPON, equipped);
            player.getInventory().setItem(SECONDARY_HOTBAR_SLOT, equipped.copy());
        } else {
            DungeonBoundItems.markPrimaryWeapon(equipped);
            player.setData(ModAttachments.PRIMARY_DUNGEON_WEAPON, equipped);
            player.getInventory().setItem(PRIMARY_HOTBAR_SLOT, equipped.copy());
        }
        player.inventoryMenu.broadcastChanges();
    }

    public static void clear(Player player) {
        player.setData(ModAttachments.PRIMARY_DUNGEON_WEAPON, ItemStack.EMPTY);
        player.setData(ModAttachments.SECONDARY_DUNGEON_WEAPON, ItemStack.EMPTY);
        clearLockedSlot(player, PRIMARY_HOTBAR_SLOT, DungeonBoundItems.PRIMARY_WEAPON_ROLE);
        clearLockedSlot(player, SECONDARY_HOTBAR_SLOT, DungeonBoundItems.SECONDARY_WEAPON_ROLE);
    }

    public static void enforceHotbarSlots(Player player) {
        enforceHotbarSlot(player, PRIMARY_HOTBAR_SLOT, DungeonBoundItems.PRIMARY_WEAPON_ROLE,
                player.getData(ModAttachments.PRIMARY_DUNGEON_WEAPON));
        enforceHotbarSlot(player, SECONDARY_HOTBAR_SLOT, DungeonBoundItems.SECONDARY_WEAPON_ROLE,
                player.getData(ModAttachments.SECONDARY_DUNGEON_WEAPON));
    }

    private static void enforceHotbarSlot(Player player, int slot, String role, ItemStack expected) {
        if (expected.isEmpty()) return;
        ItemStack current = player.getInventory().getItem(slot);
        if (ItemStack.isSameItemSameComponents(current, expected)) return;
        if (!current.isEmpty()) {
            if (!player.getInventory().add(current.copy())) player.drop(current.copy(), false);
        }
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            if (index != slot && role.equals(DungeonBoundItems.getWeaponRole(player.getInventory().getItem(index)))) {
                player.getInventory().setItem(index, ItemStack.EMPTY);
            }
        }
        player.getInventory().setItem(slot, expected.copy());
        player.inventoryMenu.broadcastChanges();
    }

    private static void clearLockedSlot(Player player, int slot, String role) {
        if (role.equals(DungeonBoundItems.getWeaponRole(player.getInventory().getItem(slot)))) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
    }
}
