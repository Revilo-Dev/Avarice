package com.revilo.gatesofavarice.dungeon.loadout;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class LoadoutModels {
    private LoadoutModels() {}

    public enum LoadoutTheme {
        ASSASSIN, KNIGHT, BERSERKER, VANGUARD, SAMURAI, REAPER, RANGER, MARKSMAN, GLADIATOR, SPELLBLADE, WARLORD, NOMAD
    }

    public record StatRollRange(String statId, float min, float max) {}

    public record EffectSpec(ResourceLocation enchantmentId, int minLevel, int maxLevel) {}

    public record FoodSupplySpec(Item item, int minCount, int maxCount) {}

    public record ArmorSetDefinition(
            String setId,
            String displayName,
            float armorMin,
            float armorMax,
            float resistanceMin,
            float resistanceMax,
            float knockbackResistanceMin,
            float knockbackResistanceMax
    ) {}

    public record LoadoutDefinition(
            String id,
            String displayName,
            LoadoutTheme theme,
            String primaryWeaponKind,
            String secondaryWeaponKind,
            ArmorSetDefinition armorSet,
            List<StatRollRange> armorRunicStatPool,
            List<StatRollRange> primaryRunicStatPool,
            List<StatRollRange> secondaryRunicStatPool,
            List<EffectSpec> armorEffectPool,
            List<EffectSpec> weaponEffectPool,
            List<FoodSupplySpec> supplies
    ) {}

    public record LoadoutInstance(
            java.util.UUID instanceId,
            String definitionId,
            long seed
    ) {}

    public enum UpgradeCategory {
        PRIMARY_WEAPON, SECONDARY_WEAPON, ARMOR, ITEM
    }

    public enum UpgradeCardType {
        INCREASE_EXISTING_STAT_PERCENT,
        INCREASE_EXISTING_STAT_FLAT,
        ADD_NEW_RUNE_STAT,
        ADD_OR_UPGRADE_EFFECT,
        ADD_IMPLICIT,
        UPGRADE_ARMOR_BASE_STAT,
        UPGRADE_ITEM_SUPPLY,
        ITEM_REWARD_FOOD,
        ITEM_REWARD_RESTOCK,
        ITEM_REWARD_ABILITY,
        ITEM_REROLL_PRIMARY_WEAPON,
        ITEM_REROLL_SECONDARY_WEAPON
    }

    public record UpgradeCard(
            String id,
            UpgradeCardType type,
            UpgradeCategory category,
            String title,
            String targetLabel,
            String changeLabel,
            String currentValue,
            String newValue,
            int tier,
            int cost
    ) {}

    public record UpgradeContext(
            java.util.UUID playerId,
            java.util.UUID loadoutInstanceId,
            float statRollMultiplier,
            int effectLevelCap
    ) {}

    public record ItemUpgradeDefinition(
            String id,
            UpgradeCategory category,
            String summary
    ) {}
}
