package com.revilo.gatesofavarice.item.data;

import com.revilo.gatesofavarice.GatewayExpansion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.CustomData;

public final class CrystalForgeData {

    private static final String ROOT_KEY = GatewayExpansion.MOD_ID;
    private static final String THEME_KEY = "theme";
    private static final String LEVEL_KEY = "level";
    private static final String SEED_KEY = "seed";
    private static final String ATTUNED_KEY = "attuned";
    private static final String CARD_DECK_KEY = "card_deck";

    private CrystalForgeData() {
    }

    public static CrystalProfile ensureProfile(ItemStack stack, int minLevel, int maxLevel, RandomSource random) {
        CompoundTag rootTag = getRootTag(stack);
        if (!rootTag.contains(THEME_KEY) || !rootTag.contains(LEVEL_KEY) || !rootTag.contains(SEED_KEY)) {
            int level = minLevel;
            long seed = random.nextLong();
            CrystalTheme theme = randomThemeForLevel(level, seed);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                CompoundTag updatedRoot = tag.getCompound(ROOT_KEY);
                updatedRoot.putString(THEME_KEY, theme.name());
                updatedRoot.putInt(LEVEL_KEY, level);
                updatedRoot.putLong(SEED_KEY, seed);
                updatedRoot.putBoolean(ATTUNED_KEY, false);
                tag.put(ROOT_KEY, updatedRoot);
            });
            syncModelData(stack, level);
            return new CrystalProfile(theme, level, seed);
        }
        CrystalProfile profile = readProfile(rootTag, minLevel, maxLevel);
        syncModelData(stack, profile.level());
        return profile;
    }

    public static CrystalProfile syncLevelToPlayer(ItemStack stack, int minLevel, int maxLevel, int playerLevel, RandomSource random) {
        CrystalProfile profile = ensureProfile(stack, minLevel, maxLevel, random);
        if (playerLevel < 0) {
            return profile;
        }

        int level = Mth.clamp(playerLevel, minLevel, maxLevel);
        CompoundTag rootTag = getRootTag(stack);
        boolean attuned = rootTag.contains(ATTUNED_KEY) && rootTag.getBoolean(ATTUNED_KEY);
        CrystalTheme theme = attuned ? profile.theme() : randomThemeForLevel(level, profile.seed());
        if (profile.level() == level && profile.theme() == theme) {
            return profile;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag updatedRoot = tag.getCompound(ROOT_KEY);
            updatedRoot.putString(THEME_KEY, theme.name());
            updatedRoot.putInt(LEVEL_KEY, level);
            updatedRoot.putLong(SEED_KEY, profile.seed());
            updatedRoot.putBoolean(ATTUNED_KEY, attuned);
            tag.put(ROOT_KEY, updatedRoot);
        });
        syncModelData(stack, level);
        return new CrystalProfile(theme, level, profile.seed());
    }

    public static CrystalProfile getProfile(ItemStack stack, int minLevel, int maxLevel) {
        return readProfile(getRootTag(stack), minLevel, maxLevel);
    }

    public static void attuneTheme(ItemStack stack, CrystalTheme theme) {
        CompoundTag rootTag = getRootTag(stack);
        int level = rootTag.contains(LEVEL_KEY) ? rootTag.getInt(LEVEL_KEY) : 0;
        final int attunedLevel = normalizeToTierBand(level);
        long seed = stableThemeSeed(attunedLevel, theme);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag updatedRoot = tag.getCompound(ROOT_KEY);
            updatedRoot.putString(THEME_KEY, theme.name());
            updatedRoot.putInt(LEVEL_KEY, attunedLevel);
            updatedRoot.putLong(SEED_KEY, seed);
            updatedRoot.putBoolean(ATTUNED_KEY, true);
            tag.put(ROOT_KEY, updatedRoot);
        });
        syncModelData(stack, attunedLevel);
    }

    public static List<Component> buildCrystalTooltip(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        CompoundTag rootTag = getRootTag(stack);
        if (rootTag.contains(THEME_KEY) && rootTag.contains(ATTUNED_KEY) && rootTag.getBoolean(ATTUNED_KEY)) {
            CrystalTheme theme = CrystalTheme.valueOf(rootTag.getString(THEME_KEY));
            lines.add(Component.translatable("tooltip.gatesofavarice.crystal.theme", themedLabel(theme)));
            if (isAltHeld()) {
                lines.addAll(themeSummary(theme));
            }
        } else {
            lines.add(Component.translatable("tooltip.gatesofavarice.crystal.random_theme")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xA45CFF))));
        }
        if (rootTag.contains(LEVEL_KEY)) {
            lines.add(Component.translatable("tooltip.gatesofavarice.crystal.level", levelBand(rootTag.getInt(LEVEL_KEY)))
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
        List<GatewayCardData.CardModifier> cards = readCards(stack);
        int slots = GatewayCardData.unlockedSlots(rootTag.contains(LEVEL_KEY) ? rootTag.getInt(LEVEL_KEY) : 1);
        lines.add(Component.literal("Deck: " + cards.size() + "/" + slots + " cards").withStyle(ChatFormatting.AQUA));
        if (!cards.isEmpty()) {
            lines.add(Component.literal("Card modifiers next run:").withStyle(ChatFormatting.LIGHT_PURPLE));
            for (GatewayCardData.CardModifier card : cards) {
                lines.add(Component.literal("- " + card.summary()).withStyle(card.type().color()));
            }
        }
        return lines;
    }

    public static boolean canAddCard(ItemStack crystal, ItemStack cardStack, int playerLevel) {
        if (crystal.isEmpty() || cardStack.isEmpty()) {
            return false;
        }
        CompoundTag rootTag = getRootTag(crystal);
        int crystalLevel = rootTag.contains(LEVEL_KEY) ? rootTag.getInt(LEVEL_KEY) : Math.max(1, playerLevel);
        return readCards(crystal).size() < GatewayCardData.unlockedSlots(Math.max(crystalLevel, playerLevel));
    }

    public static boolean addCard(ItemStack crystal, ItemStack cardStack, int playerLevel) {
        if (!canAddCard(crystal, cardStack, playerLevel)) {
            return false;
        }
        GatewayCardData.CardModifier modifier = GatewayCardData.read(cardStack);
        CustomData.update(DataComponents.CUSTOM_DATA, crystal, tag -> {
            CompoundTag root = tag.getCompound(ROOT_KEY);
            ListTag list = root.getList(CARD_DECK_KEY, Tag.TAG_COMPOUND);
            CompoundTag entry = new CompoundTag();
            entry.putString("type", modifier.type().name());
            entry.putDouble("value", modifier.value());
            entry.putInt("level", modifier.playerLevel());
            list.add(entry);
            root.put(CARD_DECK_KEY, list);
            tag.put(ROOT_KEY, root);
        });
        return true;
    }

    public static List<GatewayCardData.CardModifier> readCards(ItemStack crystal) {
        CompoundTag rootTag = getRootTag(crystal);
        if (!rootTag.contains(CARD_DECK_KEY, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag list = rootTag.getList(CARD_DECK_KEY, Tag.TAG_COMPOUND);
        ArrayList<GatewayCardData.CardModifier> cards = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            try {
                GatewayCardData.CardType type = GatewayCardData.CardType.valueOf(entry.getString("type"));
                double value = entry.contains("value") ? entry.getDouble("value") : type.minValue();
                int level = entry.contains("level") ? entry.getInt("level") : 1;
                cards.add(new GatewayCardData.CardModifier(type, Mth.clamp(value, type.minValue(), type.maxValue()), Math.max(1, level)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return List.copyOf(cards);
    }

    private static CompoundTag getRootTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(ROOT_KEY);
    }

    private static CrystalProfile readProfile(CompoundTag rootTag, int minLevel, int maxLevel) {
        int level = rootTag.contains(LEVEL_KEY) ? Mth.clamp(rootTag.getInt(LEVEL_KEY), minLevel, maxLevel) : minLevel;
        long seed = rootTag.contains(SEED_KEY) ? rootTag.getLong(SEED_KEY) : level * 31L;
        CrystalTheme theme = rootTag.contains(THEME_KEY) ? CrystalTheme.valueOf(rootTag.getString(THEME_KEY)) : randomThemeForLevel(level, seed);
        return new CrystalProfile(theme, level, seed);
    }

    private static Component themedLabel(CrystalTheme theme) {
        return theme.displayName().copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(themeColor(theme))))
                .append(Component.literal(" "))
                .append(Component.literal("[alt]").withStyle(ChatFormatting.GRAY));
    }

    private static List<Component> themeSummary(CrystalTheme theme) {
        List<Component> lines = new ArrayList<>();
        switch (theme) {
            case UNDEAD -> {
                lines.add(themeStat("More mobs"));
                lines.add(themeStat("High item quantity"));
                lines.add(themeStat("Low rarity"));
            }
            case RAIDER -> {
                lines.add(themeStat("More assassins"));
                lines.add(themeStat("High rarity"));
                lines.add(themeStat("High coins"));
                lines.add(themeStat("Low levels"));
            }
            case NETHER -> {
                lines.add(themeStat("More tanks"));
                lines.add(themeStat("High xp"));
                lines.add(themeStat("High quantity"));
                lines.add(themeStat("Low coins"));
            }
            case ARCANE -> {
                lines.add(themeStat("More Chaos"));
                lines.add(themeStat("High levels"));
                lines.add(themeStat("High coins"));
                lines.add(themeStat("High rarity"));
                lines.add(themeStat("Low xp"));
            }
            case BEAST -> {
            }
            case WILD -> {
                lines.add(themeStat("Unpredictable waves"));
                lines.add(themeStat("Balanced archetypes"));
                lines.add(themeStat("All enemy factions"));
            }
        }
        return lines;
    }

    private static Component themeStat(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }

    private static int themeColor(CrystalTheme theme) {
        return switch (theme) {
            case UNDEAD -> 0x2E6B2E;
            case ARCANE -> 0x66CCFF;
            case NETHER -> 0xD63B2A;
            case RAIDER -> 0x2A4FA8;
            case BEAST -> 0xFFFFFF;
            case WILD -> 0x6C4CD8;
        };
    }

    private static String levelBand(int level) {
        if (level >= 90) {
            return "90+";
        }
        if (level >= 70) {
            return "70-89";
        }
        if (level >= 50) {
            return "50-69";
        }
        if (level >= 20) {
            return "20-49";
        }
        return "0-19";
    }

    private static int normalizeToTierBand(int level) {
        if (level >= 90) return 90;
        if (level >= 70) return 70;
        if (level >= 50) return 50;
        if (level >= 20) return 20;
        return 0;
    }

    private static long stableThemeSeed(int levelBandStart, CrystalTheme theme) {
        return (long) levelBandStart * 31L + theme.ordinal() * 131L + 0x5F3759D5L;
    }

    private static boolean isAltHeld() {
        try {
            Class<?> screenClass = Class.forName("net.minecraft.client.gui.screens.Screen");
            Object value = screenClass.getMethod("hasAltDown").invoke(null);
            return value instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static CrystalTheme randomThemeForLevel(int level, long seed) {
        RandomSource random = RandomSource.create(seed ^ ((long) level << 32) ^ 0x5F3759D5L);
        if (level >= 50) {
            return switch (random.nextInt(5)) {
                case 0 -> CrystalTheme.UNDEAD;
                case 1 -> CrystalTheme.RAIDER;
                case 2 -> CrystalTheme.NETHER;
                case 3 -> CrystalTheme.ARCANE;
                default -> CrystalTheme.WILD;
            };
        }
        if (level >= 30) {
            return switch (random.nextInt(4)) {
                case 0 -> CrystalTheme.UNDEAD;
                case 1 -> CrystalTheme.RAIDER;
                case 2 -> CrystalTheme.NETHER;
                default -> CrystalTheme.ARCANE;
            };
        }
        if (level >= 25) {
            return switch (random.nextInt(3)) {
                case 0 -> CrystalTheme.UNDEAD;
                case 1 -> CrystalTheme.RAIDER;
                default -> CrystalTheme.NETHER;
            };
        }
        if (level >= 20) {
            return random.nextBoolean() ? CrystalTheme.UNDEAD : CrystalTheme.RAIDER;
        }
        return CrystalTheme.UNDEAD;
    }

    private static void syncModelData(ItemStack stack, int level) {
        int modelData = modelDataForLevel(level);
        CustomModelData existing = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (existing == null || existing.value() != modelData) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(modelData));
        }
    }

    private static int modelDataForLevel(int level) {
        if (level >= 81) {
            return 3;
        }
        if (level >= 51) {
            return 2;
        }
        return 1;
    }

    public record CrystalProfile(CrystalTheme theme, int level, long seed) {
    }
}
