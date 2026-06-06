package com.revilo.gatesofavarice.dungeon.loadout;

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
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.revilodev.runic.item.custom.RuneItem;
import net.revilodev.runic.runes.RuneSlots;
import net.revilodev.runic.stat.RuneStatType;
import net.revilodev.runic.stat.RuneStats;

public final class RunicUpgradeService {
    private static final int CARD_COUNT = 5;
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
    private static final List<CardSpec> DEFAULT_ARMOR_EFFECT_CARDS = List.of(
            statCard("health", "Effect Card", "Health Boost", 1.0F, 5.0F),
            bootsOnlyStatCard("jump_height", "Effect Card", "Leaping", 0.05F, 0.12F),
            statCard("power", "Effect Card", "Ability Power", 0.50F, 2.50F),
            statCard("movement_speed", "Effect Card", "Movement Speed", 0.04F, 0.10F)
    );
    private static final List<CardSpec> DEFAULT_ARMOR_STAT_CARDS = List.of(
            statCard("resistance", "Stat Card", "Resistance", 0.04F, 0.08F),
            statCard("fire_resistance", "Stat Card", "Fire Resistance", 0.04F, 0.08F),
            statCard("projectile_resistance", "Stat Card", "Projectile Resistance", 0.04F, 0.08F),
            statCard("blast_resistance", "Stat Card", "Blast Resistance", 0.04F, 0.08F),
            statCard("knockback_resistance", "Stat Card", "Knockback Resistance", 0.04F, 0.08F)
    );
    private static final List<CardSpec> DEFAULT_ARMOR_OFFENCE_CARDS = List.of(
            effectCard("minecraft:thorns", "Offence Card", "Thorns", 1),
            statCard("aegis", "Offence Card", "Aegis", 1.0F, 4.0F)
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
        int playerLevel = Math.max(1, com.revilo.gatesofavarice.integration.LevelUpIntegration.getEffectiveLevel(player));
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
            List<CardSpec> armorEffectCards = armorCardsFor(definition, ArmorCardBucket.EFFECT);
            List<CardSpec> armorStatCards = armorCardsFor(definition, ArmorCardBucket.STAT);
            List<CardSpec> armorOffenceCards = armorCardsFor(definition, ArmorCardBucket.OFFENCE);
            appendCardSpecs(cards, armorEffectCards.isEmpty() ? DEFAULT_ARMOR_EFFECT_CARDS : armorEffectCards, current, category, targetLabel, target, usedIds, random, 2, waveNumber, playerLevel);
            appendCardSpecs(cards, armorStatCards.isEmpty() ? DEFAULT_ARMOR_STAT_CARDS : armorStatCards, current, category, targetLabel, target, usedIds, random, 2, waveNumber, playerLevel);
            appendCardSpecs(cards, armorOffenceCards.isEmpty() ? DEFAULT_ARMOR_OFFENCE_CARDS : armorOffenceCards, current, category, targetLabel, target, usedIds, random, 1, waveNumber, playerLevel);
        } else {
            appendCardSpecs(cards, WEAPON_EFFECT_CARDS, current, category, targetLabel, target, usedIds, random, 3, waveNumber, playerLevel);
            appendCardSpecs(cards, WEAPON_DAMAGE_CARDS, current, category, targetLabel, target, usedIds, random, 1, waveNumber, playerLevel);
            appendCardSpecs(cards, WEAPON_STAT_CARDS, current, category, targetLabel, target, usedIds, random, 1, waveNumber, playerLevel);
        }

        while (cards.size() < CARD_COUNT) {
            cards.add(generateFallbackCard(category, targetLabel, current, usedIds, random, waveNumber, playerLevel));
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
            int waveNumber,
            int playerLevel
    ) {
        if (maxToAdd <= 0 || specs.isEmpty()) return;
        int startIndex = random.nextInt(specs.size());
        for (int offset = 0; offset < specs.size() && maxToAdd > 0 && cards.size() < CARD_COUNT; offset++) {
            CardSpec spec = specs.get((startIndex + offset) % specs.size());
            if (cards.size() >= CARD_COUNT || maxToAdd <= 0) {
                break;
            }
            if (!isCardSpecAllowed(spec, target) || !usedIds.add(spec.uniqueId())) {
                continue;
            }
            cards.add(buildStatCard(spec, current, category, targetLabel, random, waveNumber, playerLevel));
            maxToAdd--;
        }
    }

    private static UpgradeCard generateFallbackCard(
            UpgradeCategory category,
            String targetLabel,
            RuneStats current,
            Set<String> usedIds,
            RandomSource random,
            int waveNumber,
            int playerLevel
    ) {
        List<CardSpec> fallbacks = category == UpgradeCategory.ARMOR ? DEFAULT_ARMOR_STAT_CARDS : WEAPON_DAMAGE_CARDS;
        CardSpec fallback = null;
        int startIndex = fallbacks.isEmpty() ? 0 : random.nextInt(fallbacks.size());
        for (int offset = 0; offset < fallbacks.size(); offset++) {
            CardSpec spec = fallbacks.get((startIndex + offset) % fallbacks.size());
            if (usedIds.add(spec.uniqueId())) {
                fallback = spec;
                break;
            }
        }
        if (fallback == null) {
            fallback = category == UpgradeCategory.ARMOR
                    ? statCard("resistance", "Stat Card", "Resistance", 0.05F, 0.20F)
                    : statCard("attack_damage", "Damage Card", "Attack Damage", 1.0F, 3.0F);
        }
        return buildStatCard(fallback, current, category, targetLabel, random, waveNumber, playerLevel);
    }

    private static UpgradeCard buildStatCard(CardSpec spec, RuneStats current, UpgradeCategory category, String targetLabel, RandomSource random, int waveNumber, int playerLevel) {
        if (spec.effectId() != null) {
            boolean alreadyHasEffect = false;
            String currentValue = "-";
            return new UpgradeCard(
                    UUID.randomUUID().toString(),
                    UpgradeCardType.ADD_OR_UPGRADE_EFFECT,
                    category,
                    spec.title(),
                    targetLabel,
                    spec.label(),
                    currentValue,
                    "Lv " + spec.effectLevel(),
                    alreadyHasEffect ? 1 : 2,
                    0
            );
        }
        RuneStatType type = RuneStatType.byId(spec.statId());
        if (type == null) {
            type = category == UpgradeCategory.ARMOR ? RuneStatType.RESISTANCE : RuneStatType.ATTACK_DAMAGE;
            spec = statCard(type.id(), category == UpgradeCategory.ARMOR ? "Stat Card" : "Damage Card", displayStat(type), 0.05F, 0.20F);
        }

        float roll = scaledRoll(spec, type, random, waveNumber, playerLevel);
        boolean hasStat = current.has(type);
        String currentValue = hasStat ? formatCurrentValue(type, current.get(type)) : "-";
        String newValue = formatFlatValue(type, roll);
        int tier = hasStat ? 1 : 2;

        return new UpgradeCard(
                UUID.randomUUID().toString(),
                hasStat ? UpgradeCardType.INCREASE_EXISTING_STAT_FLAT : UpgradeCardType.ADD_NEW_RUNE_STAT,
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
        cards.add(new UpgradeCard(UUID.randomUUID().toString(), UpgradeCardType.ITEM_REWARD_FOOD, category, "Food Card", definition.displayName(), "Heart Fragment", "0", "+8-16", 1, 0));
        cards.add(new UpgradeCard(UUID.randomUUID().toString(), UpgradeCardType.ITEM_REWARD_RESTOCK, category, "Restock Card", definition.displayName(), "Restock", "5-16 apples", "Apples, arrows, or e-gap", 1, 0));
        cards.add(new UpgradeCard(UUID.randomUUID().toString(), UpgradeCardType.ITEM_REWARD_ABILITY, category, "Ability Card", definition.displayName(), "Arcane Apples + Magnet", "0", "+1 magnet +1-3 apples", 1, 0));
        cards.add(new UpgradeCard(UUID.randomUUID().toString(), UpgradeCardType.ITEM_REROLL_PRIMARY_WEAPON, category, "Reroll Primary Weapon", definition.displayName(), "Primary", "Current", "Same tier or rare +1", 2, 0));
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
        return formatDecimal(normalizedStoredValue(type, value));
    }

    private static String formatFlatValue(RuneStatType type, float value) {
        if (isPercentLike(type)) {
            return String.format(Locale.ROOT, "+%.0f%%", quantizePercentRoll(value));
        }
        return "+" + formatDecimal(Math.max(0.1F, value));
    }

    private static float scaledRoll(CardSpec spec, RuneStatType type, RandomSource random, int waveNumber, int playerLevel) {
        float base = spec.min() + random.nextFloat() * Math.max(0.01F, spec.max() - spec.min());
        float waveScale = 1.0F + Math.max(0, waveNumber - 1) * 0.11F;
        float levelScale = 1.0F + Math.max(0, playerLevel - 1) * 0.0125F;
        float scaled = base * waveScale * levelScale;
        if (isPercentLike(type)) {
            return scaled * 100.0F;
        }
        return quantizeValue(scaled, 0.1F);
    }

    private static String formatDecimal(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static float quantizePercentRoll(float value) {
        return Math.max(1.0F, quantizeValue(value, 1.0F));
    }

    private static float quantizeValue(float value, float step) {
        return Math.round(value / step) * step;
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
        if (spec.effectId() != null) {
            ResourceLocation effectId = ResourceLocation.tryParse(spec.effectId());
            if (effectId == null) {
                return false;
            }
            if ("minecraft:thorns".equals(spec.effectId())) {
                return !stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                        .keySet()
                        .stream()
                        .anyMatch(holder -> holder.unwrapKey().map(key -> key.location().equals(effectId)).orElse(false));
            }
            return true;
        }
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

    private static List<CardSpec> armorCardsFor(LoadoutDefinition definition, ArmorCardBucket bucket) {
        ArrayList<CardSpec> cards = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (StatRollRange range : definition.armorRunicStatPool()) {
            if (!matchesBucket(range.statId(), bucket) || !seen.add(range.statId())) {
                continue;
            }
            cards.add(cardSpecForArmorStat(range));
        }
        if (bucket == ArmorCardBucket.OFFENCE) {
            cards.add(0, effectCard("minecraft:thorns", "Offence Card", "Thorns", 1));
        }
        return List.copyOf(cards);
    }

    private static boolean matchesBucket(String statId, ArmorCardBucket bucket) {
        return switch (bucket) {
            case EFFECT -> statId.equals("health") || statId.equals("movement_speed") || statId.equals("jump_height") || statId.equals("power");
            case STAT -> statId.equals("resistance") || statId.equals("fire_resistance") || statId.equals("projectile_resistance")
                    || statId.equals("blast_resistance") || statId.equals("knockback_resistance");
            case OFFENCE -> statId.equals("aegis");
        };
    }

    private static CardSpec cardSpecForArmorStat(StatRollRange range) {
        String id = range.statId();
        String title = matchesBucket(id, ArmorCardBucket.STAT) ? "Stat Card" : (matchesBucket(id, ArmorCardBucket.OFFENCE) ? "Offence Card" : "Effect Card");
        String label = switch (id) {
            case "health" -> "Health Boost";
            case "movement_speed" -> "Movement Speed";
            case "jump_height" -> "Leaping";
            case "power" -> "Ability Power";
            case "resistance" -> "Resistance";
            case "fire_resistance" -> "Fire Resistance";
            case "projectile_resistance" -> "Projectile Resistance";
            case "blast_resistance" -> "Blast Resistance";
            case "knockback_resistance" -> "Knockback Resistance";
            case "aegis" -> "Aegis";
            default -> id.replace('_', ' ');
        };
        return "jump_height".equals(id)
                ? bootsOnlyStatCard(id, title, label, range.min(), range.max())
                : statCard(id, title, label, range.min(), range.max());
    }

    private static CardSpec statCard(String statId, String title, String label, float min, float max) {
        return new CardSpec(statId, title, label, min, max, false);
    }

    private static CardSpec bootsOnlyStatCard(String statId, String title, String label, float min, float max) {
        return new CardSpec(statId, title, label, min, max, true);
    }

    private static CardSpec effectCard(String effectId, String title, String label, int effectLevel) {
        return new CardSpec(null, effectId, title, label, 0.0F, 0.0F, false, effectLevel);
    }

    private record CardSpec(String statId, String effectId, String title, String label, float min, float max, boolean bootsOnly, int effectLevel) {
        private CardSpec(String statId, String title, String label, float min, float max, boolean bootsOnly) {
            this(statId, null, title, label, min, max, bootsOnly, 0);
        }

        private String uniqueId() {
            return this.statId != null ? this.statId : "effect:" + this.effectId;
        }
    }

    private enum ArmorCardBucket {
        EFFECT,
        STAT,
        OFFENCE
    }
}
