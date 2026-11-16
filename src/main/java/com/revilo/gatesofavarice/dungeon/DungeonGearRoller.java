package com.revilo.gatesofavarice.dungeon;

import com.revilo.gatesofavarice.item.MagnetItem;
import com.revilo.gatesofavarice.dungeon.loadout.RunicLoadoutService;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.network.chat.Component;
import net.revilodev.runic.stat.RuneStatType;
import net.revilodev.runic.stat.RuneStats;

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
        RunicLoadoutService.applyMaterialRuneSlots(stack);
    }

    /** Gives weapon drops one strong identity stat and two small supporting stats. */
    public static void rollWeaponAffixes(ItemStack stack, RandomSource random) {
        String path = stack.getItemHolder().unwrapKey().map(key -> key.location().getPath()).orElse("");
        int materialBonus = Math.max(0, RunicLoadoutService.runeSlotsForGear(stack) - 5);
        if (path.contains("dagger")) {
            setName(stack, "Poison Dagger");
            addStat(stack, "poison_chance", 12 + materialBonus * 2 + random.nextInt(4));
            addStat(stack, "attack_speed", 3 + materialBonus);
            addStat(stack, "leeching_chance", 1 + materialBonus / 2);
        } else if (path.contains("machete")) {
            setName(stack, "Serrated Machete");
            addStat(stack, "bleeding_chance", 12 + materialBonus * 2 + random.nextInt(4));
            addStat(stack, "attack_damage", 2 + materialBonus / 2);
            addStat(stack, "attack_speed", 2 + materialBonus / 2);
        } else if (path.contains("broadsword")) {
            setName(stack, "Agile Broadsword");
            addStat(stack, "attack_speed", 10 + materialBonus * 2 + random.nextInt(4));
            addStat(stack, "attack_damage", 2 + materialBonus / 2);
            addStat(stack, "sweeping_range", 15 + materialBonus * 2);
        } else if (path.contains("longsword")) {
            setName(stack, "Keen Longsword");
            addStat(stack, "attack_damage", 5 + materialBonus + random.nextInt(3));
            addStat(stack, "attack_range", 25 + materialBonus * 3);
            addStat(stack, "bonus_chance", 2 + materialBonus / 2);
        } else if (path.contains("glaive")) {
            setName(stack, "Reaping Glaive");
            addStat(stack, "sweeping_range", 35 + materialBonus * 3 + random.nextInt(6));
            addStat(stack, "undead_damage", 3 + materialBonus / 2);
            addStat(stack, "leeching_chance", 2 + materialBonus / 2);
        } else if (path.contains("hammer") || path.contains("paxel")) {
            setName(stack, "Heavy Battle Axe");
            addStat(stack, "attack_damage", 8 + materialBonus * 2 + random.nextInt(4));
            addStat(stack, "stun_chance", 3 + materialBonus / 2);
            addStat(stack, "knockback_resistance", 3 + materialBonus / 2);
        } else if (path.contains("gaundao")) {
            setName(stack, "Reach Gaundao");
            addStat(stack, "attack_range", 45 + materialBonus * 4 + random.nextInt(8));
            addStat(stack, "sweeping_range", 20 + materialBonus * 2);
            addStat(stack, "attack_damage", 2 + materialBonus / 2);
        } else if (path.contains("bow") || path.contains("crossbow")) {
            setName(stack, path.contains("crossbow") ? "Piercing Crossbow" : "Swift Bow");
            addStat(stack, "power", 8 + materialBonus * 2 + random.nextInt(4));
            addStat(stack, "draw_speed", 5 + materialBonus);
        } else {
            setName(stack, "Tempered Weapon");
            addStat(stack, "attack_damage", 5 + materialBonus + random.nextInt(3));
            addStat(stack, "attack_speed", 3 + materialBonus / 2);
        }
    }

    public static void nameLoadoutWeapon(ItemStack stack, String loadoutId, boolean primary) {
        String role = primary ? "" : "Backup ";
        String name = switch (loadoutId) {
            case "assassin" -> "Poison Dagger";
            case "berserker" -> primary ? "Heavy Battle Axe" : "Serrated Machete";
            case "vanguard", "warlord" -> primary ? "Crushing Hammer" : "Guardbreaker Weapon";
            case "samurai" -> primary ? "Keen Gaundao" : "Swift Dagger";
            case "reaper" -> primary ? "Soul Glaive" : "Soul Dagger";
            case "ranger" -> primary ? "Hunter's Bow" : "Serrated Machete";
            case "marksman" -> primary ? "Piercing Crossbow" : "Keen Longsword";
            case "gladiator" -> primary ? "Arena Broadsword" : "Swift Dagger";
            case "spellblade" -> primary ? "Arcane Longsword" : "Arcane Glaive";
            case "nomad" -> primary ? "Serrated Machete" : "Traveler's Bow";
            default -> "Knight's Weapon";
        };
        setName(stack, role + name);
    }

    private static void setName(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
    }

    private static void addStat(ItemStack stack, String id, float value) {
        RuneStatType type = RuneStatType.byId(id);
        if (type != null) {
            RuneStats.set(stack, RuneStats.combine(RuneStats.get(stack), RuneStats.single(type, value)));
        }
    }

    public static boolean isDungeonGear(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem
                || stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof ShieldItem
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
