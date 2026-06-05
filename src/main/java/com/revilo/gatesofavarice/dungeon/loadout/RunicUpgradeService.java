package com.revilo.gatesofavarice.dungeon.loadout;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.LoadoutDefinition;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.LoadoutInstance;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCard;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCardType;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCategory;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeContext;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.revilodev.runic.item.custom.RuneItem;
import net.revilodev.runic.runes.RuneSlots;
import net.revilodev.runic.stat.RuneStatType;
import net.revilodev.runic.stat.RuneStats;

public final class RunicUpgradeService {
    private static final int CARD_COUNT = 5;
    private static final float MIN_PERCENT_DISPLAY = 1.0F;
    private static final List<CardSpec> WEAPON_EFFECT_CARDS = List.of(
            statCard("poison_chance", "Effect Card", "Toxic", 0.01F, 0.05F),
            statCard("flame_chance", "Effect Card", "Fire Aspect", 0.01F, 0.05F),
            statCard("withering_chance", "Effect Card", "Withering", 0.01F, 0.05F),
            statCard("bleeding_chance", "Effect Card", "Bleeding", 0.01F, 0.06F),
            statCard("stun_chance", "Effect Card", "Stunning", 0.01F, 0.05F),
            statCard("shocking_chance", "Effect Card", "Shocking", 0.01F, 0.05F),
            statCard("leeching_chance", "Effect Card", "Leeching", 0.01F, 0.05F),
            statCard("freezing_chance", "Effect Card", "Freezing", 0.01F, 0.05F),
            statCard("fangs", "Effect Card", "Fangs", 1.0F, 4.0F)
    );
    private static final List<CardSpec> WEAPON_DAMAGE_CARDS = List.of(
            statCard("attack_damage", "Damage Card", "Attack Damage", 1.0F, 4.0F),
            statCard("undead_damage", "Damage Card", "Undead Damage", 1.0F, 4.0F)
    );
    private static final List<CardSpec> WEAPON_STAT_CARDS = List.of(
            statCard("attack_range", "Stat Card", "Attack Range", 0.10F, 0.20F),
            statCard("attack_speed", "Stat Card", "Attack Speed", 0.05F, 0.12F),
            statCard("sweeping_range", "Stat Card", "Sweeping Range", 0.10F, 0.18F)
    );
    private static final List<CardSpec> ARMOR_EFFECT_CARDS = List.of(
            statCard("health", "Effect Card", "Health Boost", 1.0F, 5.0F),
            statCard("toughness", "Effect Card", "Toughness", 1.0F, 4.0F),
            bootsOnlyStatCard("jump_height", "Effect Card", "Leaping", 0.05F, 0.12F),
            statCard("ability_power", "Effect Card", "Ability Power", 0.50F, 2.50F),
            statCard("movement_speed", "Effect Card", "Movement Speed", 0.04F, 0.10F)
    );
    private static final List<CardSpec> ARMOR_STAT_CARDS = List.of(
            statCard("resistance", "Stat Card", "Resistance", 0.04F, 0.08F),
            statCard("fire_resistance", "Stat Card", "Fire Resistance", 0.04F, 0.08F),
            statCard("projectile_resistance", "Stat Card", "Projectile Resistance", 0.04F, 0.08F),
            statCard("blast_resistance", "Stat Card", "Blast Resistance", 0.04F, 0.08F)
    );
    private static final List<CardSpec> ARMOR_OFFENCE_CARDS = List.of(
            statCard("aegis", "Offence Card", "Aegis", 1.0F, 4.0F),
            statCard("stone", "Offence Card", "Stone Skin", 1.0F, 4.0F)
    );

    private RunicUpgradeService() {}

    public static boolean canUpgradeExistingStat(ItemStack stack, RuneStatType type, float amount, UpgradeContext ctx) {
        if (stack.isEmpty() || amount <= 0.0F) return false;
        RuneStats current = RuneStats.get(stack);
        if (current.isEmpty() || !current.has(type)) return false;
        float cap = effectiveCap(type, stack);
        float currentValue = normalizedStoredValue(type, current.get(type));
        return cap <= 0.0F || currentValue < cap;
    }

    public static ItemStack upgradeExistingStat(ItemStack stack, RuneStatType type, float amount, UpgradeContext ctx) {
        if (!canUpgradeExistingStat(stack, type, amount, ctx)) return stack;
        RuneStats current = RuneStats.get(stack);
        EnumMap<RuneStatType, Float> map = new EnumMap<>(current.view());
        float next = normalizedStoredValue(type, current.get(type)) + amount;
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
        float normalized = normalizedStoredValue(type, value);
        float next = cap > 0.0F ? Math.min(normalized, cap) : normalized;
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

    public static List<UpgradeCard> generateUpgradeCards(ServerPlayer player, ItemStack target, LoadoutInstance loadout, LoadoutDefinition definition, UpgradeCategory category, int waveNumber, long rerollNonce) {
        long seed = loadout.seed()
                ^ ((long) category.ordinal() * 0x9E3779B97F4A7C15L)
                ^ ((long) target.getItem().hashCode() << 17)
                ^ (rerollNonce * 0xC2B2AE3D27D4EB4FL);
        RandomSource random = RandomSource.create(seed);
        if (category == UpgradeCategory.ITEM) {
            return generateItemCards(definition, category, random);
        }

        ArrayList<UpgradeCard> cards = new ArrayList<>(CARD_COUNT);
        String targetLabel = target.isEmpty() ? definition.displayName() : target.getHoverName().getString();
        RuneStats current = RuneStats.get(target);
        Set<String> usedIds = new HashSet<>();

        if (category == UpgradeCategory.ARMOR) {
            appendCardSpecs(cards, ARMOR_EFFECT_CARDS, current, category, targetLabel, target, usedIds, random, 2, waveNumber);
            appendCardSpecs(cards, ARMOR_STAT_CARDS, current, category, targetLabel, target, usedIds, random, 2, waveNumber);
            appendCardSpecs(cards, ARMOR_OFFENCE_CARDS, current, category, targetLabel, target, usedIds, random, 1, waveNumber);
        } else {
            appendCardSpecs(cards, WEAPON_EFFECT_CARDS, current, category, targetLabel, target, usedIds, random, 3, waveNumber);
            appendCardSpecs(cards, WEAPON_DAMAGE_CARDS, current, category, targetLabel, target, usedIds, random, 1, waveNumber);
            appendCardSpecs(cards, WEAPON_STAT_CARDS, current, category, targetLabel, target, usedIds, random, 1, waveNumber);
        }

        while (cards.size() < CARD_COUNT) {
            cards.add(generateFallbackCard(category, targetLabel, current, usedIds, random, waveNumber));
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

    private static void appendCardSpecs(
            List<UpgradeCard> cards,
            List<CardSpec> specs,
            RuneStats current,
            UpgradeCategory category,
            String targetLabel,
            ItemStack target,
            Set<String> usedIds,
            RandomSource random,
            int maxToAdd,
            int waveNumber
    ) {
        if (maxToAdd <= 0) return;
        for (CardSpec spec : specs) {
            if (cards.size() >= CARD_COUNT || maxToAdd <= 0) {
                break;
            }
            if (!isCardSpecAllowed(spec, target) || !usedIds.add(spec.statId())) {
                continue;
            }
            cards.add(buildStatCard(spec, current, category, targetLabel, random, waveNumber));
            maxToAdd--;
        }
    }

    private static UpgradeCard generateFallbackCard(
            UpgradeCategory category,
            String targetLabel,
            RuneStats current,
            Set<String> usedIds,
            RandomSource random,
            int waveNumber
    ) {
        List<CardSpec> fallbacks = category == UpgradeCategory.ARMOR ? ARMOR_STAT_CARDS : WEAPON_DAMAGE_CARDS;
        CardSpec fallback = null;
        for (CardSpec spec : fallbacks) {
            if (usedIds.add(spec.statId())) {
                fallback = spec;
                break;
            }
        }
        if (fallback == null) {
            fallback = category == UpgradeCategory.ARMOR
                    ? statCard("resistance", "Stat Card", "Resistance", 0.05F, 0.20F)
                    : statCard("attack_damage", "Damage Card", "Attack Damage", 1.0F, 3.0F);
        }
        return buildStatCard(fallback, current, category, targetLabel, random, waveNumber);
    }

    private static UpgradeCard buildStatCard(CardSpec spec, RuneStats current, UpgradeCategory category, String targetLabel, RandomSource random, int waveNumber) {
        RuneStatType type = RuneStatType.byId(spec.statId());
        if (type == null) {
            type = category == UpgradeCategory.ARMOR ? RuneStatType.RESISTANCE : RuneStatType.ATTACK_DAMAGE;
            spec = statCard(type.id(), category == UpgradeCategory.ARMOR ? "Stat Card" : "Damage Card", displayStat(type), 0.05F, 0.20F);
        }
        float roll = spec.min() + random.nextFloat() * Math.max(0.01F, spec.max() - spec.min());
        float minimumPercentRoll = minimumPercentRoll(waveNumber);
        if (isPercentLike(type)) {
            roll = Math.max(minimumPercentRoll, roll * 100.0F);
            roll = quantizePercentRoll(roll, waveNumber);
        }
        boolean hasStat = current.has(type);
        UpgradeCardType cardType;
        String currentValue;
        String newValue;
        int tier;

        if (hasStat && random.nextBoolean()) {
            cardType = UpgradeCardType.INCREASE_EXISTING_STAT_FLAT;
            currentValue = formatCurrentValue(type, current.get(type));
            newValue = formatFlatValue(type, roll, waveNumber);
            tier = 1;
        } else if (hasStat) {
            float percent = Math.max(minimumPercentRoll, 0.08F + random.nextFloat() * 0.12F);
            percent = quantizePercentRoll(percent * 100.0F, waveNumber);
            cardType = UpgradeCardType.INCREASE_EXISTING_STAT_PERCENT;
            currentValue = formatCurrentValue(type, current.get(type));
            newValue = String.format(Locale.ROOT, "+%.0f%%", percent);
            tier = 1;
        } else {
            cardType = UpgradeCardType.ADD_NEW_RUNE_STAT;
            currentValue = "-";
            newValue = formatFlatValue(type, roll, waveNumber);
            tier = 2;
        }

        return new UpgradeCard(
                UUID.randomUUID().toString(),
                cardType,
                category,
                spec.title(),
                targetLabel,
                spec.label(),
                currentValue,
                newValue,
                tier,
                0
        );
    }

    private static List<UpgradeCard> generateItemCards(LoadoutDefinition definition, UpgradeCategory category, RandomSource random) {
        ArrayList<UpgradeCard> cards = new ArrayList<>(5);
        String defaultFood = definition.supplies().stream().findFirst().map(s -> s.item().getDescription().getString()).orElse("Rations");
        cards.add(new UpgradeCard(UUID.randomUUID().toString(), UpgradeCardType.ITEM_REWARD_FOOD, category, "Food Card", definition.displayName(), defaultFood, "0", "+16", 1, 0));
        cards.add(new UpgradeCard(UUID.randomUUID().toString(), UpgradeCardType.ITEM_REWARD_RESTOCK, category, "Restock Card", definition.displayName(), "Restock", "5-16 apples", "Apples, arrows, or e-gap", 1, 0));
        cards.add(new UpgradeCard(UUID.randomUUID().toString(), UpgradeCardType.ITEM_REWARD_ABILITY, category, "Ability Card", definition.displayName(), "Arcane Apples", "0", "+1-3", 1, 0));
        cards.add(new UpgradeCard(UUID.randomUUID().toString(), UpgradeCardType.ITEM_REROLL_PRIMARY_WEAPON, category, "Reroll Primary Weapon", definition.displayName(), "Primary", "Current", "New unmodified roll", 2, 0));
        cards.add(new UpgradeCard(UUID.randomUUID().toString(), UpgradeCardType.ITEM_REROLL_SECONDARY_WEAPON, category, "Reroll Secondary Weapon", definition.displayName(), "Secondary", "Current", "Same tier or rare +1", 2, 0));
        return cards;
    }

    public static boolean isPercentLike(RuneStatType type) {
        String id = type.id();
        return id.contains("chance")
                || id.contains("resistance")
                || id.contains("speed")
                || id.contains("range")
                || id.contains("jump")
                || id.contains("knockback")
                || id.contains("sweeping");
    }

    private static String formatCurrentValue(RuneStatType type, float value) {
        if (isPercentLike(type)) {
            return String.format(Locale.ROOT, "%.0f%%", normalizedStoredValue(type, value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatFlatValue(RuneStatType type, float value, int waveNumber) {
        if (isPercentLike(type)) {
            float normalized = quantizePercentRoll(value, waveNumber);
            return String.format(Locale.ROOT, "+%.0f%%", normalized);
        }
        return String.format(Locale.ROOT, "+%.2f", Math.max(0.01F, value));
    }

    private static float minimumPercentRoll(int waveNumber) {
        return minimumPercentDisplay(waveNumber);
    }

    private static int minimumPercentDisplay(int waveNumber) {
        return Math.max(1, (int) MIN_PERCENT_DISPLAY + Math.max(0, waveNumber - 1) / 4);
    }

    private static float quantizePercentRoll(float value, int waveNumber) {
        int displayPercent = Math.max(minimumPercentDisplay(waveNumber), (int) Math.ceil(value));
        return displayPercent;
    }

    private static float normalizedStoredValue(RuneStatType type, float value) {
        if (!isPercentLike(type)) {
            return value;
        }
        return value > 0.0F && value < 1.0F ? value * 100.0F : value;
    }

    private static String displayStat(RuneStatType type) {
        return type.id().replace('_', ' ');
    }

    private static boolean isCardSpecAllowed(CardSpec spec, ItemStack stack) {
        RuneStatType type = RuneStatType.byId(spec.statId());
        if (type == null || !RunicLoadoutService.isStatAllowedForStack(stack, type)) {
            return false;
        }
        if (!spec.bootsOnly()) {
            return true;
        }
        return stack.getItem() instanceof ArmorItem armorItem
                && armorItem.getEquipmentSlot() == net.minecraft.world.entity.EquipmentSlot.FEET;
    }

    private static CardSpec statCard(String statId, String title, String label, float min, float max) {
        return new CardSpec(statId, title, label, min, max, false);
    }

    private static CardSpec bootsOnlyStatCard(String statId, String title, String label, float min, float max) {
        return new CardSpec(statId, title, label, min, max, true);
    }

    private record CardSpec(String statId, String title, String label, float min, float max, boolean bootsOnly) {
    }
}
