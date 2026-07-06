package com.revilo.gatesofavarice.item.data;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.item.CrystalItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.CustomData;

public final class CrystalForgeData {

    private static final String ROOT_KEY = GatewayExpansion.MOD_ID;
    private static final String LEVEL_KEY = "level";
    private static final String CARD_DECK_KEY = "card_deck";

    private CrystalForgeData() {
    }

    public static CrystalProfile ensureProfile(ItemStack stack, int minLevel, int maxLevel, RandomSource random) {
        CompoundTag rootTag = getRootTag(stack);
        if (!rootTag.contains(LEVEL_KEY)) {
            int level = minLevel;
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                CompoundTag updatedRoot = tag.getCompound(ROOT_KEY);
                updatedRoot.putInt(LEVEL_KEY, level);
                tag.put(ROOT_KEY, updatedRoot);
            });
            syncModelData(stack, level);
            return new CrystalProfile(level);
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
        if (profile.level() == level) {
            return profile;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag updatedRoot = tag.getCompound(ROOT_KEY);
            updatedRoot.putInt(LEVEL_KEY, level);
            tag.put(ROOT_KEY, updatedRoot);
        });
        syncModelData(stack, level);
        return new CrystalProfile(level);
    }

    public static CrystalProfile getProfile(ItemStack stack, int minLevel, int maxLevel) {
        return readProfile(getRootTag(stack), minLevel, maxLevel);
    }

    public static List<Component> buildCrystalTooltip(ItemStack stack) {
        return buildCrystalTooltip(stack, false);
    }

    public static List<Component> buildCrystalTooltip(ItemStack stack, boolean expanded) {
        List<Component> lines = new ArrayList<>();
        CompoundTag rootTag = getRootTag(stack);
        if (rootTag.contains(LEVEL_KEY)) {
            lines.add(Component.translatable("tooltip.gatesofavarice.crystal.level", levelBand(rootTag.getInt(LEVEL_KEY)))
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
        List<GatewayCardData.CardModifier> cards = readCards(stack);
        int slots = maxCardsForCrystal(stack);
        lines.add(Component.literal("Deck").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal(": " + cards.size() + "/" + slots + " cards").withStyle(ChatFormatting.LIGHT_PURPLE)));
        if (!cards.isEmpty()) {
            int visibleCards = expanded ? cards.size() : Math.min(3, cards.size());
            for (int index = 0; index < visibleCards; index++) {
                GatewayCardData.CardModifier card = cards.get(index);
                lines.add(Component.literal("- " + card.summary()).withStyle(card.type().color()));
            }
            if (cards.size() > visibleCards) {
                lines.add(Component.literal("[alt] read more").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return lines;
    }

    public static boolean canAddCard(ItemStack crystal, ItemStack cardStack, int playerLevel) {
        if (crystal.isEmpty() || cardStack.isEmpty()) {
            return false;
        }
        return readCards(crystal).size() < maxCardsForCrystal(crystal);
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

    public static void clearCards(ItemStack crystal) {
        if (crystal.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, crystal, tag -> {
            CompoundTag root = tag.getCompound(ROOT_KEY);
            root.remove(CARD_DECK_KEY);
            tag.put(ROOT_KEY, root);
        });
    }

    public static int fillCards(ItemStack crystal, GatewayCardData.CardType type, int playerLevel) {
        int slots = maxCardsForCrystal(crystal);
        if (crystal.isEmpty() || type == null || slots <= 0) {
            return 0;
        }
        int safeLevel = Math.max(1, playerLevel);
        CustomData.update(DataComponents.CUSTOM_DATA, crystal, tag -> {
            CompoundTag root = tag.getCompound(ROOT_KEY);
            ListTag list = new ListTag();
            for (int index = 0; index < slots; index++) {
                CompoundTag entry = new CompoundTag();
                entry.putString("type", type.name());
                entry.putDouble("value", type.maxValue());
                entry.putInt("level", safeLevel);
                list.add(entry);
            }
            root.put(CARD_DECK_KEY, list);
            tag.put(ROOT_KEY, root);
        });
        return slots;
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

    public static int maxCardsForCrystal(ItemStack crystal) {
        if (crystal.getItem() instanceof CrystalItem crystalItem) {
            return cardSlotsForTier(crystalItem.crystalTier().tier());
        }
        return 0;
    }

    public static int cardSlotsForTier(int tier) {
        return switch (Mth.clamp(tier, 1, 5)) {
            case 1 -> 3;
            case 2 -> 6;
            case 3 -> 9;
            case 4 -> 12;
            default -> 16;
        };
    }

    private static CompoundTag getRootTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(ROOT_KEY);
    }

    private static CrystalProfile readProfile(CompoundTag rootTag, int minLevel, int maxLevel) {
        int level = rootTag.contains(LEVEL_KEY) ? Mth.clamp(rootTag.getInt(LEVEL_KEY), minLevel, maxLevel) : minLevel;
        return new CrystalProfile(level);
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

    private static void syncModelData(ItemStack stack, int level) {
        int modelData = 1;
        CustomModelData existing = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (existing == null || existing.value() != modelData) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(modelData));
        }
    }

    public record CrystalProfile(int level) {
    }
}
