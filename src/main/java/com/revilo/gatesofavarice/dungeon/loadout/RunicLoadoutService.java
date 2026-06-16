package com.revilo.gatesofavarice.dungeon.loadout;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.config.GatewayExpansionConfig;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.EffectSpec;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.StatRollRange;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.component.CustomData;
import net.revilodev.runic.gear.GearAttributes;
import net.revilodev.runic.item.custom.RuneItem;
import net.revilodev.runic.runes.RuneSlots;
import net.revilodev.runic.stat.RuneStatType;
import net.revilodev.runic.stat.RuneStats;

public final class RunicLoadoutService {
    private static final Set<String> ARMOR_ALLOWED_STATS = Set.of(
            "movement_speed", "resistance", "fire_resistance", "blast_resistance", "projectile_resistance",
            "knockback_resistance", "health", "aegis", "jump_height", "power", "ability_power",
            "attack_damage", "attack_speed", "poison_chance", "flame_chance", "freezing_chance",
            "shocking_chance", "withering_chance"
    );
    private static final Set<String> WEAPON_ALLOWED_STATS = Set.of(
            "attack_speed", "attack_damage", "attack_range", "movement_speed", "sweeping_range", "durability",
            "mining_speed", "undead_damage", "nether_damage", "stun_chance", "flame_chance", "bleeding_chance",
            "shocking_chance", "poison_chance", "withering_chance", "weakening_chance", "draw_speed", "freezing_chance",
            "leeching_chance", "bonus_chance", "fangs", "jump_height", "power"
    );

    private RunicLoadoutService() {}

    public static ItemStack createRunicPreset(ServerLevel level, ItemStack base, List<StatRollRange> stats, List<EffectSpec> effects, RandomSource random) {
        ItemStack stack = base.copy();
        applyLoadoutStats(level, stack, stats, random);
        applyLoadoutEffects(level, stack, effects, random);
        RuneSlots.syncUsedToContents(stack);
        return stack;
    }

    public static ItemStack applyLoadoutStats(ServerLevel level, ItemStack stack, List<StatRollRange> stats, RandomSource random) {
        RuneStats merged = RuneStats.get(stack);
        for (StatRollRange spec : stats) {
            if (AuraAttributeSupport.isAuraAttributeStatId(spec.statId())) {
                continue;
            }
            RuneStatType type = RuneStatType.byId(spec.statId());
            if (type == null) {
                GatewayExpansion.LOGGER.warn("Unknown rune stat id in loadout preset: {}", spec.statId());
                continue;
            }
            if (!isStatAllowedForStack(stack, type)) {
                continue;
            }
            float raw = spec.min() + random.nextFloat() * (spec.max() - spec.min());
            float value = raw * GatewayExpansionConfig.LOADOUT_STAT_ROLL_MULTIPLIER.get().floatValue();
            if (RunicUpgradeService.isPercentLike(type)) {
                value = Math.max(1.0F, (float) Math.ceil(value * 100.0F));
            }
            merged = RuneStats.combine(merged, RuneStats.single(type, value));
        }
        RuneStats.set(stack, merged);
        RuneSlots.syncUsedToContents(stack);
        return stack;
    }

    public static ItemStack applyLoadoutEffects(ServerLevel level, ItemStack stack, List<EffectSpec> effects, RandomSource random) {
        ItemEnchantments current = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
        for (EffectSpec effect : effects) {
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, effect.enchantmentId());
            Holder.Reference<Enchantment> holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key).orElse(null);
            if (holder == null) {
                GatewayExpansion.LOGGER.warn("Unknown effect enchant id in loadout preset: {}", effect.enchantmentId());
                continue;
            }
            if (!RuneItem.isEffectEnchantment(holder)) {
                GatewayExpansion.LOGGER.warn("Rejected non-effect enchantment in loadout preset: {}", effect.enchantmentId());
                continue;
            }
            if (!isEffectAllowedForStack(stack, effect.enchantmentId())) {
                continue;
            }
            int levelRoll = effect.minLevel() + random.nextInt(Math.max(1, effect.maxLevel() - effect.minLevel() + 1));
            int clamped = RuneItem.clampEffectLevel(holder, levelRoll);
            mutable.set(holder, Math.max(mutable.getLevel(holder), clamped));
        }
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
        RuneSlots.syncUsedToContents(stack);
        return stack;
    }

    public static void tagLoadoutIdentity(ItemStack stack, String loadoutId, String setName, String pieceId) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag root = tag.getCompound(GatewayExpansion.MOD_ID);
            root.putString("loadout_id", loadoutId);
            root.putString("loadout_set_name", setName);
            root.putString("armor_piece", pieceId);
            tag.put(GatewayExpansion.MOD_ID, root);
        });
    }

    public static boolean isAllowedEffect(ResourceLocation enchantmentId) {
        return RuneItem.allowedEffectIds().contains(enchantmentId);
    }

    public static boolean isEffectAllowedForStack(ItemStack stack, ResourceLocation enchantmentId) {
        if (!isAllowedEffect(enchantmentId)) {
            return false;
        }
        if (enchantmentId.equals(ResourceLocation.withDefaultNamespace("multishot"))) {
            return stack.getItem() instanceof CrossbowItem;
        }
        return true;
    }

    public static int clampEffectLevel(Holder<Enchantment> holder, int requestedLevel) {
        return RuneItem.clampEffectLevel(holder, requestedLevel);
    }

    public static void syncRunicSlots(ItemStack stack) {
        RuneSlots.syncUsedToContents(stack);
    }

    public static float cursedMultiplier(ItemStack stack) {
        return GearAttributes.cursedMultiplier(stack);
    }

    public static List<RuneStatType> parseTypes(List<String> ids) {
        ArrayList<RuneStatType> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            RuneStatType type = RuneStatType.byId(id);
            if (type != null) out.add(type);
        }
        return out;
    }

    public static boolean isStatAllowedForStack(ItemStack stack, RuneStatType type) {
        if (stack.isEmpty() || type == null) return false;
        String id = type.id();
        if (stack.getItem() instanceof ArmorItem) {
            return ARMOR_ALLOWED_STATS.contains(id) && isStatAllowedForArmorPiece(stack, id);
        }
        return WEAPON_ALLOWED_STATS.contains(id);
    }

    private static boolean isStatAllowedForArmorPiece(ItemStack stack, String statId) {
        if (!(stack.getItem() instanceof ArmorItem armorItem)) {
            return false;
        }
        if ("movement_speed".equals(statId)) {
            return armorItem.getEquipmentSlot() == net.minecraft.world.entity.EquipmentSlot.FEET;
        }
        if ("jump_height".equals(statId)) {
            return armorItem.getEquipmentSlot() == net.minecraft.world.entity.EquipmentSlot.LEGS;
        }
        return true;
    }

    public static List<StatRollRange> filterStatsForStack(ItemStack stack, List<StatRollRange> source) {
        ArrayList<StatRollRange> out = new ArrayList<>(source.size());
        Set<String> seen = new HashSet<>();
        for (StatRollRange range : source) {
            if (AuraAttributeSupport.isAuraAttributeStatId(range.statId())) {
                out.add(range);
                continue;
            }
            RuneStatType type = RuneStatType.byId(range.statId());
            if (type == null || !isStatAllowedForStack(stack, type)) continue;
            if (seen.add(range.statId())) {
                out.add(range);
            }
        }
        return out;
    }
}
