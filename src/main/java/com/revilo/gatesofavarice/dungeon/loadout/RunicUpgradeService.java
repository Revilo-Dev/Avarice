package com.revilo.gatesofavarice.dungeon.loadout;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.config.GatewayExpansionConfig;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.EffectSpec;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.LoadoutDefinition;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.LoadoutInstance;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.StatRollRange;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCard;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCardType;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCategory;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeContext;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.revilodev.runic.item.custom.RuneItem;
import net.revilodev.runic.runes.RuneSlots;
import net.revilodev.runic.stat.RuneStatType;
import net.revilodev.runic.stat.RuneStats;

public final class RunicUpgradeService {
    private static final List<String> WEAPON_PRIORITY_STATS = List.of(
            "attack_damage", "attack_speed", "attack_range", "sweeping_range", "stun_chance",
            "leeching_chance", "bleeding_chance", "shocking_chance", "poison_chance", "bonus_chance",
            "draw_speed", "movement_speed", "power"
    );
    private static final List<String> ARMOR_PRIORITY_STATS = List.of(
            "resistance", "health", "toughness", "knockback_resistance", "projectile_resistance",
            "fire_resistance", "blast_resistance", "movement_speed", "jump_height", "aegis", "stone", "power"
    );

    private RunicUpgradeService() {}

    public static boolean canUpgradeExistingStat(ItemStack stack, RuneStatType type, float amount, UpgradeContext ctx) {
        if (stack.isEmpty() || amount <= 0.0F) return false;
        RuneStats current = RuneStats.get(stack);
        if (current.isEmpty() || !current.has(type)) return false;
        float cap = effectiveCap(type, stack);
        return cap <= 0.0F || current.get(type) < cap;
    }

    public static ItemStack upgradeExistingStat(ItemStack stack, RuneStatType type, float amount, UpgradeContext ctx) {
        if (!canUpgradeExistingStat(stack, type, amount, ctx)) return stack;
        RuneStats current = RuneStats.get(stack);
        EnumMap<RuneStatType, Float> map = new EnumMap<>(current.view());
        float next = current.get(type) + amount;
        float cap = effectiveCap(type, stack);
        if (cap > 0.0F) next = Math.min(next, cap);
        map.put(type, next);
        RuneStats.set(stack, new RuneStats(map));
        RuneSlots.syncUsedToContents(stack);
        return stack;
    }

    public static boolean canAddNewStat(ItemStack stack, RuneStatType type, float value, UpgradeContext ctx) {
        if (stack.isEmpty() || value <= 0.0F) return false;
        RuneStats current = RuneStats.get(stack);
        return !current.has(type);
    }

    public static ItemStack addNewStat(ItemStack stack, RuneStatType type, float value, UpgradeContext ctx) {
        if (!canAddNewStat(stack, type, value, ctx)) return stack;
        RuneStats current = RuneStats.get(stack);
        EnumMap<RuneStatType, Float> map = new EnumMap<>(current.view());
        float cap = effectiveCap(type, stack);
        float next = cap > 0.0F ? Math.min(value, cap) : value;
        map.put(type, next);
        RuneStats.set(stack, new RuneStats(map));
        RuneSlots.syncUsedToContents(stack);
        return stack;
    }

    public static boolean canAddOrUpgradeEffect(ItemStack stack, Holder<Enchantment> effect, int level, UpgradeContext ctx) {
        return !stack.isEmpty() && level > 0 && RuneItem.isEffectEnchantment(effect);
    }

    public static ItemStack addOrUpgradeEffect(ItemStack stack, Holder<Enchantment> effect, int requestedLevel, UpgradeContext ctx) {
        if (!canAddOrUpgradeEffect(stack, effect, requestedLevel, ctx)) return stack;
        int lvl = RuneItem.clampEffectLevel(effect, requestedLevel);
        ItemEnchantments cur = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(cur);
        mut.set(effect, Math.max(mut.getLevel(effect), lvl));
        stack.set(DataComponents.ENCHANTMENTS, mut.toImmutable());
        RuneSlots.syncUsedToContents(stack);
        return stack;
    }

    public static List<UpgradeCard> generateUpgradeCards(ServerPlayer player, ItemStack target, LoadoutInstance loadout, LoadoutDefinition definition, UpgradeCategory category) {
        int count = 5;
        RandomSource random = RandomSource.create(loadout.seed() ^ category.ordinal() ^ target.getItem().hashCode());
        ArrayList<UpgradeCard> cards = new ArrayList<>(count);
        List<StatRollRange> pool = switch (category) {
            case PRIMARY_WEAPON -> definition.primaryRunicStatPool();
            case SECONDARY_WEAPON -> definition.secondaryRunicStatPool();
            case ARMOR -> definition.armorRunicStatPool();
            case ITEM -> List.of(new StatRollRange("ability_power", 0.5F, 1.2F), new StatRollRange("draw_speed", 0.01F, 0.04F), new StatRollRange("bonus_chance", 0.01F, 0.03F));
        };
        if (category != UpgradeCategory.ITEM) {
            pool = RunicLoadoutService.filterStatsForStack(target, pool);
            if (pool.isEmpty()) {
                pool = List.of(new StatRollRange(
                        target.getItem() instanceof net.minecraft.world.item.ArmorItem ? "resistance" : "attack_damage",
                        0.05F,
                        0.25F));
            }
        }
        if (category == UpgradeCategory.ITEM) {
            cards.addAll(generateItemCards(definition, category, random));
            return List.copyOf(cards);
        }

        String targetLabel = target.isEmpty() ? definition.displayName() : target.getHoverName().getString();
        RuneStats current = RuneStats.get(target);
        List<StatRollRange> prioritizedPool = prioritizePoolForCategory(pool, category);
        Set<String> usedIds = new HashSet<>();

        appendPreferredStatCards(cards, prioritizedPool, current, category, targetLabel, usedIds, random, 3);
        appendEffectCards(cards, definition, category, targetLabel, random, 1);
        appendPreferredStatCards(cards, prioritizedPool, current, category, targetLabel, usedIds, random, count - cards.size());

        while (cards.size() < count) {
            cards.add(generateFallbackCard(category, targetLabel, current, prioritizedPool, usedIds, random));
        }
        return List.copyOf(cards);
    }

    public static Holder.Reference<Enchantment> resolveEffect(ServerLevel level, ResourceLocation id) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT, id)).orElse(null);
    }

    private static float effectiveCap(RuneStatType type, ItemStack stack) {
        if (type.cap() <= 0.0F) return -1.0F;
        return type.cap() * RunicLoadoutService.cursedMultiplier(stack);
    }

    private static void appendPreferredStatCards(
            List<UpgradeCard> cards,
            List<StatRollRange> pool,
            RuneStats current,
            UpgradeCategory category,
            String targetLabel,
            Set<String> usedIds,
            RandomSource random,
            int maxToAdd
    ) {
        if (maxToAdd <= 0) return;
        List<String> preferredIds = category == UpgradeCategory.ARMOR ? ARMOR_PRIORITY_STATS : WEAPON_PRIORITY_STATS;
        for (String statId : preferredIds) {
            if (cards.size() >= 5 || maxToAdd <= 0) {
                return;
            }
            StatRollRange range = findRange(pool, statId);
            if (range == null || !usedIds.add(statId)) {
                continue;
            }
            cards.add(buildStatCard(range, current, category, targetLabel, random));
            maxToAdd--;
        }
    }

    private static void appendEffectCards(List<UpgradeCard> cards, LoadoutDefinition definition, UpgradeCategory category, String targetLabel, RandomSource random, int maxToAdd) {
        if (maxToAdd <= 0 || definition.allowedEffectPool().isEmpty()) return;
        List<EffectSpec> pool = definition.allowedEffectPool();
        for (int i = 0; i < maxToAdd && cards.size() < 5; i++) {
            EffectSpec spec = pool.get(random.nextInt(pool.size()));
            int level = spec.minLevel() + random.nextInt(Math.max(1, spec.maxLevel() - spec.minLevel() + 1));
            cards.add(new UpgradeCard(
                    UUID.randomUUID().toString(),
                    UpgradeCardType.ADD_OR_UPGRADE_EFFECT,
                    category,
                    "Runic Effect",
                    targetLabel,
                    "effect:" + spec.enchantmentId(),
                    "Lv?",
                    "Lv" + level,
                    2,
                    0
            ));
        }
    }

    private static UpgradeCard generateFallbackCard(
            UpgradeCategory category,
            String targetLabel,
            RuneStats current,
            List<StatRollRange> pool,
            Set<String> usedIds,
            RandomSource random
    ) {
        StatRollRange fallback = null;
        for (StatRollRange range : pool) {
            if (usedIds.add(range.statId())) {
                fallback = range;
                break;
            }
        }
        if (fallback == null) {
            fallback = new StatRollRange(category == UpgradeCategory.ARMOR ? "resistance" : "attack_damage", 0.05F, 0.20F);
        }
        return buildStatCard(fallback, current, category, targetLabel, random);
    }

    private static UpgradeCard buildStatCard(StatRollRange range, RuneStats current, UpgradeCategory category, String targetLabel, RandomSource random) {
        RuneStatType type = RuneStatType.byId(range.statId());
        if (type == null) {
            type = category == UpgradeCategory.ARMOR ? RuneStatType.RESISTANCE : RuneStatType.ATTACK_DAMAGE;
            range = new StatRollRange(type.id(), 0.05F, 0.20F);
        }
        float roll = range.min() + random.nextFloat() * Math.max(0.01F, range.max() - range.min());
        boolean hasStat = current.has(type);
        UpgradeCardType cardType;
        String title;
        String currentValue;
        String newValue;
        int tier;

        if (hasStat && random.nextBoolean()) {
            cardType = UpgradeCardType.INCREASE_EXISTING_STAT_FLAT;
            title = "Raise " + displayStat(type);
            currentValue = String.format(Locale.ROOT, "%.2f", current.get(type));
            newValue = String.format(Locale.ROOT, "+%.2f", Math.max(0.01F, roll));
            tier = 1;
        } else if (hasStat) {
            float percent = 0.08F + random.nextFloat() * 0.12F;
            cardType = UpgradeCardType.INCREASE_EXISTING_STAT_PERCENT;
            title = "Empower " + displayStat(type);
            currentValue = String.format(Locale.ROOT, "%.2f", current.get(type));
            newValue = String.format(Locale.ROOT, "+%.0f%%", percent * 100.0F);
            tier = 1;
        } else {
            cardType = UpgradeCardType.ADD_NEW_RUNE_STAT;
            title = "Add " + displayStat(type);
            currentValue = "-";
            newValue = String.format(Locale.ROOT, "+%.2f", Math.max(0.01F, roll));
            tier = 2;
        }

        return new UpgradeCard(
                UUID.randomUUID().toString(),
                cardType,
                category,
                title,
                targetLabel,
                type.id(),
                currentValue,
                newValue,
                tier,
                0
        );
    }

    private static List<StatRollRange> prioritizePoolForCategory(List<StatRollRange> pool, UpgradeCategory category) {
        List<String> preferredIds = category == UpgradeCategory.ARMOR ? ARMOR_PRIORITY_STATS : WEAPON_PRIORITY_STATS;
        ArrayList<StatRollRange> prioritized = new ArrayList<>(pool.size());
        Set<String> seen = new HashSet<>();
        for (String statId : preferredIds) {
            StatRollRange range = findRange(pool, statId);
            if (range != null && seen.add(range.statId())) {
                prioritized.add(range);
            }
        }
        for (StatRollRange range : pool) {
            if (seen.add(range.statId())) {
                prioritized.add(range);
            }
        }
        return List.copyOf(prioritized);
    }

    private static StatRollRange findRange(List<StatRollRange> pool, String statId) {
        for (StatRollRange range : pool) {
            if (range.statId().equals(statId)) {
                return range;
            }
        }
        return null;
    }

    private static List<UpgradeCard> generateItemCards(LoadoutDefinition definition, UpgradeCategory category, RandomSource random) {
        String upgradeItem = definition.supplies().stream().map(s -> s.item().getDescription().getString()).findFirst().orElse("Supplies");
        ArrayList<UpgradeCard> cards = new ArrayList<>(5);
        cards.add(new UpgradeCard(UUID.randomUUID().toString(), UpgradeCardType.UPGRADE_ITEM_SUPPLY, category, "Resupply", definition.displayName(), "supply", "+" + (4 + random.nextInt(5)), "+" + (8 + random.nextInt(9)), 1, 0));
        cards.add(new UpgradeCard(UUID.randomUUID().toString(), UpgradeCardType.UPGRADE_ITEM_SUPPLY, category, "Combat Stock", definition.displayName(), "supply", upgradeItem, upgradeItem + " +1 bundle", 2, 0));
        cards.add(new UpgradeCard(UUID.randomUUID().toString(), UpgradeCardType.ADD_NEW_RUNE_STAT, category, "Utility Rune", definition.displayName(), RuneStatType.ABILITY_POWER.id(), "-", "+0.50", 2, 0));
        cards.add(new UpgradeCard(UUID.randomUUID().toString(), UpgradeCardType.INCREASE_EXISTING_STAT_FLAT, category, "Quickness", definition.displayName(), RuneStatType.MOVEMENT_SPEED.id(), "current", "+0.02", 1, 0));
        cards.add(new UpgradeCard(UUID.randomUUID().toString(), UpgradeCardType.INCREASE_EXISTING_STAT_PERCENT, category, "Sharpen Focus", definition.displayName(), RuneStatType.BONUS_CHANCE.id(), "current", "+10%", 1, 0));
        return cards;
    }

    private static String displayStat(RuneStatType type) {
        return type.id().replace('_', ' ');
    }

    private static String categoryTitle(UpgradeCategory category) {
        return switch (category) {
            case PRIMARY_WEAPON -> "Primary Weapon";
            case SECONDARY_WEAPON -> "Secondary Weapon";
            case ARMOR -> "Armor";
            case ITEM -> "Item";
        };
    }
}
