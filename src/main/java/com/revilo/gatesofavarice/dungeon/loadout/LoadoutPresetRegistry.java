package com.revilo.gatesofavarice.dungeon.loadout;

import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.ArmorSetDefinition;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.EffectSpec;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.FoodSupplySpec;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.LoadoutDefinition;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.LoadoutTheme;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.StatRollRange;
import com.revilo.gatesofavarice.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public final class LoadoutPresetRegistry {
    private static final List<LoadoutDefinition> PRESETS = build();

    private LoadoutPresetRegistry() {}

    public static List<LoadoutDefinition> all() {
        return PRESETS;
    }

    public static Optional<LoadoutDefinition> byId(String id) {
        return PRESETS.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    private static List<LoadoutDefinition> build() {
        ArrayList<LoadoutDefinition> out = new ArrayList<>();
        out.add(preset("assassin", "Assassin", LoadoutTheme.ASSASSIN, "dagger", "dagger", armor("shadow_set", "Shadow Set", 4.0F, 7.0F, 0.00F, 0.03F, 0.00F, 0.03F),
                List.of(stat("movement_speed", 0.02F, 0.08F), stat("attack_speed", 0.01F, 0.05F), stat("bleeding_chance", 0.02F, 0.08F), stat("poison_chance", 0.01F, 0.04F), stat("leeching_chance", 0.01F, 0.03F)),
                List.of(effect("farmersdelight:backstabbing", 1, 1), effect("minecraft:swift_sneak", 1, 2)),
                List.of(supply(Items.GOLDEN_APPLE, 2, 2), supply(Items.COOKED_PORKCHOP, 16, 16))));
        out.add(preset("knight", "Knight", LoadoutTheme.KNIGHT, "longsword", "crossbow", armor("steel_knight_set", "Steel Knight Set", 8.0F, 12.0F, 0.03F, 0.08F, 0.03F, 0.08F),
                List.of(stat("resistance", 0.02F, 0.08F), stat("health", 1.0F, 4.0F), stat("attack_damage", 1.0F, 3.0F), stat("attack_range", 0.2F, 0.7F), stat("toughness", 1.0F, 3.0F)),
                List.of(effect("expanded_combat:blocking", 1, 1)),
                List.of(supply(Items.COOKED_BEEF, 16, 16), supply(Items.GOLDEN_CARROT, 8, 8), supply(Items.ARROW, 32, 32))));
        out.add(preset("berserker", "Berserker", LoadoutTheme.BERSERKER, "axe", "machete", armor("rage_set", "Rage Set", 7.0F, 10.0F, 0.02F, 0.06F, 0.00F, 0.03F),
                List.of(stat("attack_damage", 1.0F, 4.0F), stat("attack_speed", 0.01F, 0.05F), stat("health", 2.0F, 5.0F), stat("bleeding_chance", 0.02F, 0.08F), stat("leeching_chance", 0.01F, 0.05F)),
                List.of(effect("twilightforest:destruction", 1, 1)),
                List.of(supply(Items.COOKED_BEEF, 24, 24), supply(Items.GOLDEN_APPLE, 1, 1))));
        out.add(preset("vanguard", "Vanguard", LoadoutTheme.VANGUARD, "hammer", "broadsword", armor("fortress_set", "Fortress Set", 12.0F, 16.0F, 0.08F, 0.14F, 0.08F, 0.15F),
                List.of(stat("resistance", 0.05F, 0.12F), stat("toughness", 2.0F, 5.0F), stat("health", 3.0F, 7.0F), stat("knockback_resistance", 0.04F, 0.12F), stat("stun_chance", 0.01F, 0.05F)),
                List.of(effect("expanded_combat:ground_slam", 1, 1)),
                List.of(supply(Items.COOKED_BEEF, 16, 16), supply(Items.GOLDEN_APPLE, 3, 3))));
        out.add(preset("samurai", "Samurai", LoadoutTheme.SAMURAI, "gaundao", "dagger", armor("windwalker_set", "Windwalker Set", 8.0F, 11.0F, 0.02F, 0.07F, 0.02F, 0.06F),
                List.of(stat("attack_range", 0.2F, 0.8F), stat("attack_speed", 0.01F, 0.06F), stat("movement_speed", 0.02F, 0.08F), stat("sweeping_range", 0.1F, 0.5F), stat("ability_power", 0.3F, 1.2F)),
                List.of(effect("combat_roll:acrobat", 1, 1), effect("combat_roll:longfooted", 1, 1)),
                List.of(supply(Items.COOKED_SALMON, 16, 16), supply(Items.GOLDEN_CARROT, 12, 12), supply(ModItems.ARCANE_APPLE.get(), 2, 2))));
        out.add(preset("reaper", "Reaper", LoadoutTheme.REAPER, "glaive", "dagger", armor("soulbound_set", "Soulbound Set", 8.0F, 11.0F, 0.03F, 0.07F, 0.02F, 0.07F),
                List.of(stat("withering_chance", 0.01F, 0.06F), stat("leeching_chance", 0.01F, 0.05F), stat("fangs", 1.0F, 4.0F), stat("ability_power", 1.5F, 5.0F), stat("sweeping_range", 0.1F, 0.5F)),
                List.of(effect("simplyswords:soul_siphoner", 1, 1), effect("mysticalagriculture:soul_siphoner", 1, 1)),
                List.of(supply(ModItems.ARCANE_APPLE.get(), 3, 3), supply(Items.COOKED_BEEF, 16, 16))));
        out.add(preset("ranger", "Ranger", LoadoutTheme.RANGER, "bow", "machete", armor("hunter_set", "Hunter Set", 5.0F, 8.0F, 0.00F, 0.04F, 0.00F, 0.03F),
                List.of(stat("draw_speed", 0.02F, 0.08F), stat("movement_speed", 0.02F, 0.09F), stat("projectile_resistance", 0.01F, 0.06F), stat("bonus_chance", 0.01F, 0.05F), stat("attack_damage", 1.0F, 3.0F)),
                List.of(effect("minecraft:flame", 1, 1), effect("minecraft:infinity", 1, 1), effect("minecraft:punch", 1, 2)),
                List.of(supply(Items.COOKED_CHICKEN, 16, 16), supply(Items.SWEET_BERRIES, 32, 32), supply(Items.ARROW, 64, 64))));
        out.add(preset("marksman", "Marksman", LoadoutTheme.MARKSMAN, "crossbow", "longsword", armor("sharpshooter_set", "Sharpshooter Set", 7.0F, 10.0F, 0.01F, 0.05F, 0.01F, 0.05F),
                List.of(stat("draw_speed", 0.02F, 0.08F), stat("projectile_resistance", 0.01F, 0.06F), stat("attack_damage", 1.0F, 3.0F), stat("bonus_chance", 0.01F, 0.05F), stat("attack_range", 0.2F, 0.8F)),
                List.of(effect("minecraft:multishot", 1, 1), effect("minecraft:piercing", 1, 3), effect("dungeons_arise:voltaic_shot", 1, 1)),
                List.of(supply(Items.GOLDEN_CARROT, 16, 16), supply(Items.PUMPKIN_PIE, 8, 8), supply(Items.ARROW, 64, 64))));
        out.add(preset("gladiator", "Gladiator", LoadoutTheme.GLADIATOR, "broadsword", "dagger", armor("arena_set", "Arena Set", 9.0F, 13.0F, 0.02F, 0.08F, 0.02F, 0.08F),
                List.of(stat("attack_damage", 1.0F, 4.0F), stat("sweeping_range", 0.1F, 0.6F), stat("resistance", 0.01F, 0.06F), stat("health", 2.0F, 5.0F), stat("stun_chance", 0.01F, 0.04F)),
                List.of(effect("expanded_combat:blocking", 1, 1)),
                List.of(supply(Items.COOKED_BEEF, 20, 20), supply(Items.GOLDEN_APPLE, 1, 1))));
        out.add(preset("spellblade", "Spellblade", LoadoutTheme.SPELLBLADE, "longsword", "glaive", armor("arcane_set", "Arcane Set", 8.0F, 11.0F, 0.02F, 0.07F, 0.01F, 0.05F),
                List.of(stat("ability_power", 2.0F, 7.0F), stat("aegis", 1.0F, 4.0F), stat("fangs", 1.0F, 4.0F), stat("stone", 1.0F, 4.0F), stat("bonus_chance", 0.01F, 0.05F), stat("attack_damage", 1.0F, 3.0F)),
                List.of(effect("aether:renewal", 1, 1), effect("simplyswords:catalysis", 1, 1), effect("deeperdarker:catalysis", 1, 1)),
                List.of(supply(ModItems.ARCANE_APPLE.get(), 4, 4), supply(Items.GOLDEN_CARROT, 16, 16), supply(Items.HONEY_BOTTLE, 6, 6))));
        out.add(preset("warlord", "Warlord", LoadoutTheme.WARLORD, "hammer", "crossbow", armor("tyrant_set", "Tyrant Set", 12.0F, 16.0F, 0.08F, 0.14F, 0.06F, 0.12F),
                List.of(stat("resistance", 0.05F, 0.12F), stat("toughness", 2.0F, 5.0F), stat("health", 3.0F, 7.0F), stat("knockback_resistance", 0.04F, 0.12F), stat("stun_chance", 0.01F, 0.05F), stat("attack_damage", 1.0F, 4.0F)),
                List.of(effect("expanded_combat:ground_slam", 1, 1), effect("dungeons_arise:ensnaring", 1, 1)),
                List.of(supply(Items.COOKED_MUTTON, 24, 24), supply(Items.GOLDEN_APPLE, 3, 3), supply(Items.ARROW, 32, 32))));
        out.add(preset("nomad", "Nomad", LoadoutTheme.NOMAD, "machete", "bow", armor("traveler_set", "Traveler Set", 5.0F, 8.0F, 0.00F, 0.03F, 0.00F, 0.03F),
                List.of(stat("movement_speed", 0.02F, 0.10F), stat("jump_height", 0.03F, 0.12F), stat("attack_speed", 0.01F, 0.06F), stat("draw_speed", 0.02F, 0.07F), stat("durability", 5.0F, 18.0F)),
                List.of(effect("combat_roll:longfooted", 1, 1), effect("minecraft:feather_falling", 1, 3), effect("minecraft:soul_speed", 1, 2)),
                List.of(supply(Items.BREAD, 24, 24), supply(Items.COOKED_COD, 12, 12), supply(Items.ARROW, 32, 32))));
        return List.copyOf(out);
    }

    private static LoadoutDefinition preset(String id, String displayName, LoadoutTheme theme, String primary, String secondary, ArmorSetDefinition armorSet, List<StatRollRange> statPool, List<EffectSpec> effectPool, List<FoodSupplySpec> supplies) {
        return new LoadoutDefinition(id, displayName, theme, primary, secondary, armorSet, statPool, statPool, statPool, effectPool, supplies);
    }

    private static ArmorSetDefinition armor(String setId, String displayName, float armorMin, float armorMax, float resistanceMin, float resistanceMax, float kbrMin, float kbrMax) {
        return new ArmorSetDefinition(setId, displayName, armorMin, armorMax, resistanceMin, resistanceMax, kbrMin, kbrMax);
    }

    private static StatRollRange stat(String statId, float min, float max) {
        return new StatRollRange(statId, min, max);
    }

    private static EffectSpec effect(String id, int min, int max) {
        return new EffectSpec(ResourceLocation.parse(id), min, max);
    }

    private static FoodSupplySpec supply(net.minecraft.world.item.Item item, int min, int max) {
        return new FoodSupplySpec(item, min, max);
    }
}
