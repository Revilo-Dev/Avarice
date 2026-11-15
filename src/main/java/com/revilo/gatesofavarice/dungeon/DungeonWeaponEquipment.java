package com.revilo.gatesofavarice.dungeon;

import com.revilo.gatesofavarice.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative weapon slots used exclusively during dungeon runs. */
public final class DungeonWeaponEquipment {
    private DungeonWeaponEquipment() {
    }

    public static ItemStack primary(Player player) {
        return player.getData(ModAttachments.PRIMARY_DUNGEON_WEAPON).copy();
    }

    public static ItemStack secondary(Player player) {
        return player.getData(ModAttachments.SECONDARY_DUNGEON_WEAPON).copy();
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
        } else {
            DungeonBoundItems.markPrimaryWeapon(equipped);
            player.setData(ModAttachments.PRIMARY_DUNGEON_WEAPON, equipped);
        }
    }

    public static void clear(Player player) {
        player.setData(ModAttachments.PRIMARY_DUNGEON_WEAPON, ItemStack.EMPTY);
        player.setData(ModAttachments.SECONDARY_DUNGEON_WEAPON, ItemStack.EMPTY);
    }
}
