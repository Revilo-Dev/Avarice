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
            statCard("attack_range", "Stat Card", "Attack Range", 0.20F, 0.80F),
            statCard("attack_speed", "Stat Card", "Attack Speed", 0.01F, 0.06F),
            statCard("sweeping_range", "Stat Card", "Sweeping Range", 0.10F, 0.60F)
    );
    private static final List<CardSpec> ARMOR_EFFECT_CARDS = List.of(
            statCard("health", "Effect Card", "Health Boost", 1.0F, 5.0F),
            statCard("toughness", "Effect Card", "Toughness", 1.0F, 4.0F),
            bootsOnlyStatCard("jump_height", "Effect Card", "Leaping", 0.03F, 0.12F),
            statCard("ability_power", "Effect Card", "Ability Power", 0.50F, 2.50F),
            statCard("movement_speed", "Effect Card", "Movement Speed", 0.01F, 0.06F)
    );
    private static final List<CardSpec> ARMOR_STAT_CARDS = List.of(
            statCard("resistance", "Stat Card", "Resistance", 0.02F, 0.10F),
            statCard("fire_resistance", "Stat Card", "Fire Resistance", 0.02F, 0.10F),
            statCard("projectile_resistance", "Stat Card", "Projectile Resistance", 0.02F, 0.10F),
            statCard("blast_resistance", "Stat Card", "Blast Resistance", 0.02F, 0.10F)
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
        RandomSource random = RandomSource.create(loadout.seed() ^ category.ordinal() ^ target.getItem().hashCode());
        if (category == UpgradeCategory.ITEM) {
            return generateItemCards(definition, category, random);
        }

        ArrayList<UpgradeCard> cards = new ArrayList<>(CARD_COUNT);
        String targetLabel = target.isEmpty() ? definition.displayName() : target.getHoverName().getString();
        RuneStats current = RuneStats.get(target);
        Set<String> usedIds = new HashSet<>();

        if (category == UpgradeCategory.ARMOR) {
            appendCardSpecs(cards, ARMOR_EFFECT_CARDS, current, category, targetLabel, target, usedIds, random, 2);
            appendCardSpecs(cards, ARMOR_STAT_CARDS, current, category, targetLabel, target, usedIds, random, 2);
            appendCardSpecs(cards, ARMOR_OFFENCE_CARDS, current, category, targetLabel, target, usedIds, random, 1);
        } else {
            appendCardSpecs(cards, WEAPON_EFFECT_CARDS, current, category, targetLabel, target, usedIds, random, 3);
            appendCardSpecs(cards, WEAPON_DAMAGE_CARDS, current, category, targetLabel, target, usedIds, random, 1);
            appendCardSpecs(cards, WEAPON_STAT_CARDS, current, category, targetLabel, target, usedIds, random, 1);
        }

        while (cards.size() < CARD_COUNT) {
            cards.add(generateFallbackCard(category, targetLabel, current, usedIds, random));
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
            int maxToAdd
    ) {
        if (maxToAdd <= 0) return;
        for (CardSpec spec : specs) {
            if (cards.size() >= CARD_COUNT || maxToAdd <= 0) {
                break;
            }
            if (!isCardSpecAllowed(spec, target) || !usedIds.add(spec.statId())) {
                continue;
            }
            cards.add(buildStatCard(spec, current, category, targetLabel, random));
            maxToAdd--;
        }
    }

    private static UpgradeCard generateFallbackCard(
            UpgradeCategory category,
            String targetLabel,
            RuneStats current,
            Set<String> usedIds,
            RandomSource random
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
        return buildStatCard(fallback, current, category, targetLabel, random);
    }

    private static UpgradeCard buildStatCard(CardSpec spec, RuneStats current, UpgradeCategory category, String targetLabel, RandomSource random) {
        RuneStatType type = RuneStatType.byId(spec.statId());
        if (type == null) {
            type = category == UpgradeCategory.ARMOR ? RuneStatType.RESISTANCE : RuneStatType.ATTACK_DAMAGE;
            spec = statCard(type.id(), category == UpgradeCategory.ARMOR ? "Stat Card" : "Damage Card", displayStat(type), 0.05F, 0.20F);
        }
        float roll = spec.min() + random.nextFloat() * Math.max(0.01F, spec.max() - spec.min());
        boolean hasStat = current.has(type);
        UpgradeCardType cardType;
        String currentValue;
        String newValue;
        int tier;

        if (hasStat && random.nextBoolean()) {
            cardType = UpgradeCardType.INCREASE_EXISTING_STAT_FLAT;
            currentValue = String.format(Locale.ROOT, "%.2f", current.get(type));
            newValue = String.format(Locale.ROOT, "+%.2f", Math.max(0.01F, roll));
            tier = 1;
        } else if (hasStat) {
            float percent = 0.08F + random.nextFloat() * 0.12F;
            cardType = UpgradeCardType.INCREASE_EXISTING_STAT_PERCENT;
            currentValue = String.format(Locale.ROOT, "%.2f", current.get(type));
            newValue = String.format(Locale.ROOT, "+%.0f%%", percent * 100.0F);
            tier = 1;
        } else {
            cardType = UpgradeCardType.ADD_NEW_RUNE_STAT;
            currentValue = "-";
            newValue = String.format(Locale.ROOT, "+%.2f", Math.max(0.01F, roll));
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
