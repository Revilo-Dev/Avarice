package com.revilo.gatesofavarice.item.data;

import com.revilo.gatesofavarice.GatewayExpansion;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

public final class GatewayCardData {
    private static final String ROOT_KEY = GatewayExpansion.MOD_ID;
    private static final String CARD_TYPE_KEY = "card_type";
    private static final String CARD_VALUE_KEY = "card_value";
    private static final String CARD_LEVEL_KEY = "card_level";

    private GatewayCardData() {
    }

    public static ItemStack create(net.minecraft.world.item.Item item, CardType type, int playerLevel, RandomSource random) {
        ItemStack stack = new ItemStack(item);
        write(stack, type, rollValue(type, playerLevel, random), Math.max(1, playerLevel));
        return stack;
    }

    public static ItemStack createFromBooster(net.minecraft.world.item.Item item, BoosterRarity rarity, int playerLevel, RandomSource random) {
        CardType type = rollTypeForBooster(rarity, random);
        ItemStack stack = new ItemStack(item);
        write(stack, type, rollBoosterValue(type, rarity, random), Math.max(1, playerLevel));
        return stack;
    }

    public static void ensure(ItemStack stack, RandomSource random) {
        CompoundTag root = root(stack);
        if (root.contains(CARD_TYPE_KEY) && root.contains(CARD_VALUE_KEY)) {
            syncModel(stack, readType(stack));
            return;
        }
        write(stack, CardType.STAT, rollValue(CardType.STAT, 1, random), 1);
    }

    public static void write(ItemStack stack, CardType type, double value, int playerLevel) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag root = tag.getCompound(ROOT_KEY);
            root.putString(CARD_TYPE_KEY, type.name());
            root.putDouble(CARD_VALUE_KEY, value);
            root.putInt(CARD_LEVEL_KEY, Math.max(1, playerLevel));
            tag.put(ROOT_KEY, root);
        });
        syncModel(stack, type);
    }

    public static CardModifier read(ItemStack stack) {
        CardType type = readType(stack);
        CompoundTag root = root(stack);
        double value = root.contains(CARD_VALUE_KEY) ? root.getDouble(CARD_VALUE_KEY) : type.minValue();
        int level = root.contains(CARD_LEVEL_KEY) ? root.getInt(CARD_LEVEL_KEY) : 1;
        return new CardModifier(type, Mth.clamp(value, type.minValue(), type.maxValue()), Math.max(1, level));
    }

    public static CardType readType(ItemStack stack) {
        CompoundTag root = root(stack);
        if (root.contains(CARD_TYPE_KEY)) {
            try {
                return CardType.valueOf(root.getString(CARD_TYPE_KEY));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return CardType.STAT;
    }

    public static List<Component> tooltip(ItemStack stack) {
        CardModifier modifier = read(stack);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(modifier.type().displayName()).withStyle(modifier.type().color()));
        lines.add(Component.literal(modifier.summary()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Apply in the Gateway Workbench.").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    public static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0D);
    }

    public static int unlockedSlots(int playerLevel) {
        return Mth.clamp(3 + Math.max(0, playerLevel) / 20, 3, 8);
    }

    private static double rollValue(CardType type, int playerLevel, RandomSource random) {
        double levelScale = Mth.clamp(Math.max(1, playerLevel) / 100.0D, 0.0D, 1.0D);
        double max = Mth.lerp(levelScale, type.minValue(), type.maxValue());
        double min = Math.min(type.minValue(), max);
        return min + random.nextDouble() * Math.max(0.001D, max - min);
    }

    private static CardType rollTypeForBooster(BoosterRarity rarity, RandomSource random) {
        if (rarity.allowsLoadout() && random.nextFloat() < rarity.loadoutChance()) {
            return CardType.LOADOUT;
        }
        if (rarity == BoosterRarity.LEGENDARY) {
            return CardType.MULTIPLIERS[random.nextInt(CardType.MULTIPLIERS.length)];
        }
        if (rarity.allowsMultiplier() && random.nextFloat() < rarity.multiplierChance()) {
            return CardType.MULTIPLIERS[random.nextInt(CardType.MULTIPLIERS.length)];
        }
        return CardType.BASE[random.nextInt(CardType.BASE.length)];
    }

    private static double rollBoosterValue(CardType type, BoosterRarity rarity, RandomSource random) {
        if (type == CardType.LOADOUT) {
            return 1.0D;
        }
        if (type.multiplier()) {
            double roll = random.nextDouble();
            double biased = roll * roll * roll;
            return 1.2D + biased * (rarity.maxMultiplier() - 1.2D);
        }
        return rarity.minPercent() + random.nextDouble() * Math.max(0.001D, rarity.maxPercent() - rarity.minPercent());
    }

    private static CompoundTag root(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(ROOT_KEY);
    }

    private static void syncModel(ItemStack stack, CardType type) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.modelData()));
    }

    public enum CardType {
        STAT("Stat Card", ChatFormatting.AQUA, 1, 0.02D, 0.18D),
        DAMAGE("Damage Card", ChatFormatting.RED, 2, 0.02D, 0.18D),
        EFFECT("Effect Card", ChatFormatting.GREEN, 3, 0.02D, 0.18D),
        ABILITY("Ability Card", ChatFormatting.LIGHT_PURPLE, 4, 0.05D, 0.20D),
        RARITY("Rarity Card", ChatFormatting.GOLD, 5, 0.02D, 0.18D),
        CHALLENGE("Challenge Card", ChatFormatting.DARK_RED, 6, 0.02D, 0.18D),
        STAT_MULTIPLIER("Stat Multiplier", ChatFormatting.GOLD, 7, 1.10D, 3.00D, true),
        DAMAGE_MULTIPLIER("Damage Multiplier", ChatFormatting.GOLD, 8, 1.10D, 3.00D, true),
        EFFECT_MULTIPLIER("Effect Multiplier", ChatFormatting.GOLD, 9, 1.10D, 3.00D, true),
        ABILITY_MULTIPLIER("Ability Multiplier", ChatFormatting.GOLD, 10, 1.10D, 3.00D, true),
        RARITY_MULTIPLIER("Rarity Multiplier", ChatFormatting.GOLD, 11, 1.10D, 3.00D, true),
        CHALLENGE_MULTIPLIER("Challenge Multiplier", ChatFormatting.GOLD, 12, 1.10D, 3.00D, true),
        LOADOUT("Loadout Card", ChatFormatting.LIGHT_PURPLE, 13, 1.00D, 1.00D);

        private static final CardType[] BASE = { STAT, DAMAGE, EFFECT, ABILITY, RARITY, CHALLENGE };
        private static final CardType[] MULTIPLIERS = { STAT_MULTIPLIER, DAMAGE_MULTIPLIER, EFFECT_MULTIPLIER, ABILITY_MULTIPLIER, RARITY_MULTIPLIER, CHALLENGE_MULTIPLIER };

        private final String displayName;
        private final ChatFormatting color;
        private final int modelData;
        private final double minValue;
        private final double maxValue;
        private final boolean multiplier;

        CardType(String displayName, ChatFormatting color, int modelData, double minValue, double maxValue) {
            this(displayName, color, modelData, minValue, maxValue, false);
        }

        CardType(String displayName, ChatFormatting color, int modelData, double minValue, double maxValue, boolean multiplier) {
            this.displayName = displayName;
            this.color = color;
            this.modelData = modelData;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.multiplier = multiplier;
        }

        public String displayName() {
            return this.displayName;
        }

        public ChatFormatting color() {
            return this.color;
        }

        public int modelData() {
            return this.modelData;
        }

        public double minValue() {
            return this.minValue;
        }

        public double maxValue() {
            return this.maxValue;
        }

        public boolean multiplier() {
            return this.multiplier;
        }
    }

    public enum BoosterRarity {
        COMMON(0.01D, 0.03D, 0.0F, 1.2D),
        UNCOMMON(0.03D, 0.05D, 0.0F, 1.2D),
        RARE(0.05D, 0.08D, 0.12F, 1.8D),
        EPIC(0.08D, 0.12D, 0.35F, 0.15F, 2.3D),
        LEGENDARY(0.12D, 0.18D, 1.0F, 0.35F, 3.0D);

        private final double minPercent;
        private final double maxPercent;
        private final float multiplierChance;
        private final float loadoutChance;
        private final double maxMultiplier;

        BoosterRarity(double minPercent, double maxPercent, float multiplierChance, double maxMultiplier) {
            this(minPercent, maxPercent, multiplierChance, 0.0F, maxMultiplier);
        }

        BoosterRarity(double minPercent, double maxPercent, float multiplierChance, float loadoutChance, double maxMultiplier) {
            this.minPercent = minPercent;
            this.maxPercent = maxPercent;
            this.multiplierChance = multiplierChance;
            this.loadoutChance = loadoutChance;
            this.maxMultiplier = maxMultiplier;
        }

        public double minPercent() {
            return this.minPercent;
        }

        public double maxPercent() {
            return this.maxPercent;
        }

        public boolean allowsMultiplier() {
            return this.multiplierChance > 0.0F;
        }

        public float multiplierChance() {
            return this.multiplierChance;
        }

        public boolean allowsLoadout() {
            return this.loadoutChance > 0.0F;
        }

        public float loadoutChance() {
            return this.loadoutChance;
        }

        public double maxMultiplier() {
            return this.maxMultiplier;
        }
    }

    public record CardModifier(CardType type, double value, int playerLevel) {
        public String summary() {
            String amount = formatPercent(this.value);
            return switch (this.type) {
                case STAT -> "+" + amount + " player resistance and speed";
                case DAMAGE -> "+" + amount + " damage, strength, crit power";
                case EFFECT -> "+" + amount + " defensive effects";
                case ABILITY -> "+" + amount + " ability power";
                case RARITY -> "+" + amount + " item rarity and quantity";
                case CHALLENGE -> "+" + amount + " loot rewards, harder mobs";
                case STAT_MULTIPLIER -> formatMultiplier(this.value) + " stats";
                case DAMAGE_MULTIPLIER -> formatMultiplier(this.value) + " damage";
                case EFFECT_MULTIPLIER -> formatMultiplier(this.value) + " effects";
                case ABILITY_MULTIPLIER -> formatMultiplier(this.value) + " ability";
                case RARITY_MULTIPLIER -> formatMultiplier(this.value) + " rarity";
                case CHALLENGE_MULTIPLIER -> formatMultiplier(this.value) + " challenge";
                case LOADOUT -> "Automatically grants the crystal owner a random dungeon loadout";
            };
        }

        private static String formatMultiplier(double value) {
            return String.format(Locale.ROOT, "x%.1f", value);
        }
    }
}
