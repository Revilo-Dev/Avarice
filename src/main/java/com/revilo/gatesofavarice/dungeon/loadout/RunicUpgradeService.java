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

        RuneStats current = RuneStats.get(target);
        Set<RuneStatType> used = new HashSet<>();
        for (int i = 0; i < count; i++) {
            UpgradeCardType type = switch (i) {
                case 0 -> UpgradeCardType.INCREASE_EXISTING_STAT_PERCENT;
                case 1 -> UpgradeCardType.INCREASE_EXISTING_STAT_FLAT;
                case 2 -> UpgradeCardType.ADD_OR_UPGRADE_EFFECT;
                case 3 -> UpgradeCardType.ADD_NEW_RUNE_STAT;
                default -> UpgradeCardType.ADD_IMPLICIT;
            };
            cards.add(generateCard(type, category, definition, pool, current, used, random));
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

    private static UpgradeCard generateCard(
            UpgradeCardType type,
            UpgradeCategory category,
            LoadoutDefinition definition,
            List<StatRollRange> pool,
            RuneStats current,
            Set<RuneStatType> used,
            RandomSource random
    ) {
        if (type == UpgradeCardType.ADD_OR_UPGRADE_EFFECT && !definition.allowedEffectPool().isEmpty()) {
            EffectSpec spec = definition.allowedEffectPool().get(random.nextInt(definition.allowedEffectPool().size()));
            int level = spec.minLevel() + random.nextInt(Math.max(1, spec.maxLevel() - spec.minLevel() + 1));
            return new UpgradeCard(
                    UUID.randomUUID().toString(),
                    type,
                    category,
                    "Enhance Effect",
                    definition.displayName(),
                    "effect:" + spec.enchantmentId(),
                    "Lv?",
                    "Lv" + level,
                    2,
                    0
            );
        }

        RuneStatType chosenType = pickRuneStat(pool, current, used, random, type == UpgradeCardType.ADD_NEW_RUNE_STAT);
        if (chosenType == null) {
            chosenType = RuneStatType.ABILITY_POWER;
        }
        used.add(chosenType);
        float currentValue = current.get(chosenType);
        String chosenId = chosenType.id();
        StatRollRange range = pool.stream().filter(r -> r.statId().equals(chosenId)).findFirst().orElse(new StatRollRange(chosenId, 0.05F, 0.15F));
        float roll = range.min() + random.nextFloat() * (range.max() - range.min());

        return switch (type) {
            case INCREASE_EXISTING_STAT_PERCENT -> {
                float percent = 0.08F + random.nextFloat() * 0.10F;
                yield new UpgradeCard(UUID.randomUUID().toString(), type, category, "Boost " + displayStat(chosenType), definition.displayName(), chosenType.id(),
                        String.format(Locale.ROOT, "%.2f", currentValue),
                        String.format(Locale.ROOT, "+%.0f%%", percent * 100.0F), 1, 0);
            }
            case INCREASE_EXISTING_STAT_FLAT -> new UpgradeCard(UUID.randomUUID().toString(), type, category, "Raise " + displayStat(chosenType), definition.displayName(), chosenType.id(),
                    String.format(Locale.ROOT, "%.2f", currentValue),
                    String.format(Locale.ROOT, "+%.2f", Math.max(0.01F, roll)), 1, 0);
            case ADD_NEW_RUNE_STAT -> new UpgradeCard(UUID.randomUUID().toString(), type, category, "Add " + displayStat(chosenType), definition.displayName(), chosenType.id(),
                    current.has(chosenType) ? String.format(Locale.ROOT, "%.2f", currentValue) : "-", String.format(Locale.ROOT, "+%.2f", Math.max(0.01F, roll)), 2, 0);
            case ADD_IMPLICIT -> new UpgradeCard(UUID.randomUUID().toString(), type, category, "Damage Tuning", definition.displayName(), RuneStatType.ATTACK_DAMAGE.id(),
                    String.format(Locale.ROOT, "%.2f", current.get(RuneStatType.ATTACK_DAMAGE)),
                    String.format(Locale.ROOT, "+%.2f", 0.4F + random.nextFloat() * 1.4F), 2, 0);
            default -> new UpgradeCard(UUID.randomUUID().toString(), UpgradeCardType.INCREASE_EXISTING_STAT_FLAT, category, "Raise " + displayStat(chosenType), definition.displayName(), chosenType.id(),
                    String.format(Locale.ROOT, "%.2f", currentValue), String.format(Locale.ROOT, "+%.2f", Math.max(0.01F, roll)), 1, 0);
        };
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

    private static RuneStatType pickRuneStat(List<StatRollRange> pool, RuneStats current, Set<RuneStatType> used, RandomSource random, boolean preferMissing) {
        List<RuneStatType> valid = new ArrayList<>();
        for (StatRollRange range : pool) {
            RuneStatType type = RuneStatType.byId(range.statId());
            if (type == null || used.contains(type)) continue;
            if (preferMissing && current.has(type)) continue;
            valid.add(type);
        }
        if (valid.isEmpty()) {
            for (StatRollRange range : pool) {
                RuneStatType type = RuneStatType.byId(range.statId());
                if (type != null && !used.contains(type)) {
                    valid.add(type);
                }
            }
        }
        return valid.isEmpty() ? null : valid.get(random.nextInt(valid.size()));
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
