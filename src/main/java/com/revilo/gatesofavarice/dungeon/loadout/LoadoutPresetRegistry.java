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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Fixed, class-focused starter kits. Random affixes belong on dungeon loot, not loadouts. */
public final class LoadoutPresetRegistry {
    private static final List<LoadoutDefinition> PRESETS = build();

    private LoadoutPresetRegistry() {}

    public static List<LoadoutDefinition> all() {
        return PRESETS;
    }

    public static Optional<LoadoutDefinition> byId(String id) {
        return PRESETS.stream().filter(preset -> preset.id().equals(id)).findFirst();
    }

    private static List<LoadoutDefinition> build() {
        ArrayList<LoadoutDefinition> presets = new ArrayList<>();
        presets.add(preset("assassin", "Assassin", LoadoutTheme.ASSASSIN, "dagger", "dagger", requiredItem("arsenal:poison_bomb"), armor("shadow_set", "Assassin Set", 2, .01F, .01F),
                stats("movement_speed", .09F, "attack_speed", .05F, "aura:ability_power", .15F, "aura:ability_force_rampage_bonus", 1),
                stats("attack_speed", .06F, "bleeding_chance", .02F, "poison_chance", .12F, "leeching_chance", .01F), List.of(), effects("farmersdelight:backstabbing", 1), supplies(Items.GOLDEN_APPLE, 2)));
        presets.add(preset("knight", "Knight", LoadoutTheme.KNIGHT, "longsword", "dual_crossbow", Items.SHIELD, armor("steel_knight_set", "Knight Set", 11, .06F, .06F),
                stats("resistance", .07F, "movement_speed", .06F, "health", 3, "aura:ability_power", .15F, "aura:skill_strength_bonus", 2),
                stats("resistance", .05F, "health", 2, "attack_damage", 2, "attack_range", .4F, "toughness", 2), List.of(), effects("expanded_combat:blocking", 1), List.of()));
        presets.add(preset("berserker", "Berserker", LoadoutTheme.BERSERKER, "axe", "machete", requiredItem("arsenal:golden_bomb"), armor("rage_set", "Berserker Set", 9, .04F, .02F),
                stats("resistance", .05F, "movement_speed", .04F, "health", 2, "aura:ability_power", .15F, "aura:ability_force_rampage_bonus", 1, "aura:skill_strength_bonus", 1),
                stats("attack_damage", 6, "attack_speed", .03F, "health", 3, "bleeding_chance", .02F, "leeching_chance", .01F), effects("minecraft:thorns", 1), effects("twilightforest:destruction", 1), supplies(Items.GOLDEN_APPLE, 5)));
        presets.add(preset("vanguard", "Vanguard", LoadoutTheme.VANGUARD, "hammer", "broadsword", Items.SHIELD, armor("fortress_set", "Vanguard Set", 15, .11F, .11F),
                stats("resistance", .11F, "movement_speed", .01F, "health", 8, "aura:ability_power", .10F, "aura:ability_lightning_bonus", 1),
                stats("resistance", .09F, "toughness", 4, "health", 5, "knockback_resistance", .08F, "stun_chance", .02F), effects("minecraft:thorns", 1), effects("expanded_combat:ground_slam", 1), supplies(Items.GOLDEN_APPLE, 5)));
        presets.add(preset("samurai", "Samurai", LoadoutTheme.SAMURAI, "gaundao", "dagger", ModItems.DUNGEON_MAGNET.get(), armor("windwalker_set", "Samurai Set", 10, .05F, .04F),
                stats("movement_speed", .07F, "resistance", .05F, "health", 3, "aura:ability_power", .18F, "aura:ability_poison_bonus", 1),
                stats("attack_range", .5F, "attack_speed", .04F, "sweeping_range", .3F, "attack_damage", 2), effects("combat_roll:acrobat", 1, "combat_roll:longfooted", 1), List.of(), supplies(ModItems.ARCANE_APPLE.get(), 2)));
        presets.add(preset("reaper", "Reaper", LoadoutTheme.REAPER, "glaive", "dagger", ModItems.DUNGEON_MAGNET.get(), armor("soulbound_set", "Reaper Set", 10, .05F, .05F),
                stats("resistance", .05F, "movement_speed", .04F, "aura:ability_power", .55F, "aura:ability_wind_bonus", 1),
                stats("withering_chance", .02F, "leeching_chance", .02F, "fangs", 2, "undead_damage", 3, "sweeping_range", .3F), List.of(), effects("simplyswords:soul_siphoner", 1, "mysticalagriculture:soul_siphoner", 1), supplies(ModItems.ARCANE_APPLE.get(), 3)));
        presets.add(preset("ranger", "Ranger", LoadoutTheme.RANGER, "short_bow", "machete", Items.SHIELD, armor("hunter_set", "Ranger Set", 7, .02F, .02F),
                stats("movement_speed", .07F, "resistance", .05F, "health", 2, "aura:ability_power", .15F, "aura:skill_power_bonus", 1, "aura:skill_agility_bonus", 1),
                stats("draw_speed", .05F, "projectile_resistance", .04F, "bonus_chance", .02F, "attack_damage", 2), List.of(), effects("minecraft:flame", 1, "minecraft:punch", 1), supplies(ModItems.ARCANE_APPLE.get(), 1)));
        presets.add(preset("marksman", "Marksman", LoadoutTheme.MARKSMAN, "heavy_crossbow", "longsword", Items.SHIELD, armor("sharpshooter_set", "Marksman Set", 9, .03F, .03F),
                stats("resistance", .05F, "movement_speed", .08F, "health", 2, "aura:ability_power", .35F, "aura:skill_power_bonus", 1, "aura:skill_resistance_bonus", 1),
                stats("draw_speed", .06F, "projectile_resistance", .04F, "attack_damage", 2, "bonus_chance", .02F, "attack_range", .5F), List.of(), effects("minecraft:power", 1, "minecraft:punch", 1, "minecraft:flame", 1), supplies(ModItems.ARCANE_APPLE.get(), 2)));
        presets.add(preset("gladiator", "Gladiator", LoadoutTheme.GLADIATOR, "broadsword", "dagger", Items.SHIELD, armor("arena_set", "Gladiator Set", 11, .05F, .05F),
                stats("resistance", .06F, "movement_speed", .04F, "health", 3, "aura:ability_power", .35F, "aura:skill_resistance_bonus", 1, "aura:skill_strength_bonus", 1),
                stats("attack_damage", 3, "sweeping_range", .4F, "resistance", .04F, "health", 3, "stun_chance", .02F), effects("minecraft:thorns", 1), effects("expanded_combat:blocking", 1), supplies(Items.GOLDEN_APPLE, 1)));
        presets.add(preset("spellblade", "Spellblade", LoadoutTheme.SPELLBLADE, "longsword", "glaive", ModItems.DUNGEON_MAGNET.get(), armor("arcane_set", "Spellblade Set", 10, .05F, .03F),
                stats("resistance", .05F, "movement_speed", .04F, "aura:ability_power", .75F, "health", 2, "aura:ability_magic_bonus", 1, "aura:ability_ice_bonus", 1),
                stats("shocking_chance", .02F, "fangs", 2, "bonus_chance", .02F, "attack_damage", 2, "attack_speed", .03F), List.of(), effects("aether:renewal", 1, "simplyswords:catalysis", 1, "deeperdarker:catalysis", 1), supplies(ModItems.ARCANE_APPLE.get(), 4)));
        presets.add(preset("warlord", "Warlord", LoadoutTheme.WARLORD, "hammer", "scatter_crossbow", requiredItem("arsenal:bomb"), armor("tyrant_set", "Warlord Set", 14, .11F, .09F),
                stats("resistance", .11F, "health", 6, "movement_speed", .06F, "aura:ability_power", .35F, "aura:ability_fire_bonus", 1, "aura:skill_strength_bonus", 1),
                stats("resistance", .09F, "toughness", 4, "health", 5, "knockback_resistance", .08F, "stun_chance", .02F, "attack_damage", 4), effects("minecraft:thorns", 1), effects("expanded_combat:ground_slam", 1, "dungeons_arise:ensnaring", 1), supplies(Items.GOLDEN_APPLE, 3)));
        presets.add(preset("nomad", "Nomad", LoadoutTheme.NOMAD, "machete", "long_bow", Items.SHIELD, armor("traveler_set", "Nomad Set", 7, .02F, .02F),
                stats("movement_speed", .09F, "resistance", .03F, "health", 2, "aura:ability_power", .35F, "aura:ability_fire_bonus", 1, "aura:ability_poison_bonus", 1),
                stats("attack_speed", .04F, "draw_speed", .05F, "durability", 12), effects("combat_roll:longfooted", 1), List.of(), List.of()));
        return List.copyOf(presets);
    }

    private static LoadoutDefinition preset(String id, String name, LoadoutTheme theme, String primary, String secondary, Item utility, ArmorSetDefinition armor, List<StatRollRange> armorStats, List<StatRollRange> weaponStats, List<EffectSpec> armorEffects, List<EffectSpec> weaponEffects, List<FoodSupplySpec> supplies) {
        return new LoadoutDefinition(id, name, theme, primary, secondary, utility, armor, armorStats, weaponStats, weaponStats, armorEffects, weaponEffects, supplies);
    }

    private static ArmorSetDefinition armor(String id, String name, float armor, float resistance, float knockbackResistance) {
        return new ArmorSetDefinition(id, name, armor, armor, resistance, resistance, knockbackResistance, knockbackResistance, 5);
    }

    private static List<StatRollRange> stats(Object... entries) {
        ArrayList<StatRollRange> stats = new ArrayList<>(entries.length / 2);
        for (int index = 0; index < entries.length; index += 2) {
            String id = (String) entries[index];
            float value = ((Number) entries[index + 1]).floatValue();
            stats.add(new StatRollRange(id, value, value));
        }
        return List.copyOf(stats);
    }

    private static List<EffectSpec> effects(String id, int level, Object... rest) {
        ArrayList<EffectSpec> effects = new ArrayList<>();
        effects.add(effect(id, level));
        for (int index = 0; index < rest.length; index += 2) effects.add(effect((String) rest[index], (Integer) rest[index + 1]));
        return List.copyOf(effects);
    }

    private static EffectSpec effect(String id, int level) {
        return new EffectSpec(ResourceLocation.parse(id), level, level);
    }

    private static List<FoodSupplySpec> supplies(Item item, int count) {
        return List.of(new FoodSupplySpec(item, count, count));
    }

    private static Item requiredItem(String id) {
        ResourceLocation key = ResourceLocation.parse(id);
        return BuiltInRegistries.ITEM.getOptional(key)
                .orElseThrow(() -> new IllegalStateException("Required loadout item is missing: " + id));
    }
}
