package com.revilo.gatesofavarice.dungeon;

import java.util.List;
import java.util.Map;

public final class DungeonDeck {

    public static final int NEGATIVE_CARD_COUNT = 5;

    private DungeonDeck() {
    }

    public static List<CardState> empty() {
        return List.of(CardType.values()).stream()
                .map(type -> new CardState(0, 0.0D))
                .toList();
    }

    public static List<CardState> normalize(List<CardState> cards) {
        CardState[] normalized = new CardState[CardType.values().length];
        for (int index = 0; index < normalized.length; index++) {
            CardState card = cards != null && index < cards.size() ? cards.get(index) : null;
            normalized[index] = card == null ? new CardState(0, 0.0D) : card;
        }
        return List.of(normalized);
    }

    public static List<CardState> merge(List<CardState> current, List<CardState> additions) {
        List<CardState> normalizedCurrent = normalize(current);
        List<CardState> normalizedAdditions = normalize(additions);
        CardState[] merged = new CardState[CardType.values().length];
        for (int index = 0; index < merged.length; index++) {
            CardState left = normalizedCurrent.get(index);
            CardState right = normalizedAdditions.get(index);
            merged[index] = new CardState(
                    left.count() + right.count(),
                    left.appliedPercent() + right.appliedPercent());
        }
        return List.of(merged);
    }

    public static List<CardState> withPulls(List<CardState> current, List<Integer> pulledCounts) {
        List<CardState> normalizedCurrent = normalize(current);
        CardState[] preview = new CardState[CardType.values().length];
        for (int index = 0; index < preview.length; index++) {
            CardState card = normalizedCurrent.get(index);
            Integer pulled = pulledCounts != null && index < pulledCounts.size() ? pulledCounts.get(index) : null;
            int pulledCount = Math.max(0, pulled == null ? 0 : pulled);
            preview[index] = new CardState(
                    card.count() + pulledCount,
                    card.appliedPercent() + pulledCount * CardType.values()[index].effectPercent());
        }
        return List.of(preview);
    }

    public static List<CardState> fromCounts(Map<CardType, Integer> counts) {
        return List.of(CardType.values()).stream()
                .map(type -> {
                    int count = counts == null ? 0 : Math.max(0, counts.getOrDefault(type, 0));
                    return new CardState(count, count * type.percentPerCopy());
                })
                .toList();
    }

    public enum CardType {
        ENEMY_HEALTH(true, 5, "Enemy Health", "Increases enemy maximum health"),
        ENEMY_DAMAGE(true, 5, "Enemy Damage", "Increases damage dealt by enemies"),
        ENEMY_QUANTITY(true, 10, "Enemy Quantity", "Adds more enemies to each wave"),
        ENEMY_SPEED(true, 3, "Enemy Speed", "Increases enemy movement speed"),
        ENEMY_LEECH(true, 1, "Enemy Leech", "Enemies heal from damage dealt"),
        COINS(false, 5, "Coins", "Increases Mythic Coins earned"),
        LOOT_QUANTITY(false, 5, "Loot Quantity", "Increases dungeon loot quantity"),
        LOOT_QUALITY(false, 5, "Loot Quality", "Improves dungeon loot quality"),
        XP(false, 3, "XP", "Increases dungeon XP earned");

        private final boolean negative;
        private final int percentPerCopy;
        private final String title;
        private final String description;

        CardType(boolean negative, int percentPerCopy, String title, String description) {
            this.negative = negative;
            this.percentPerCopy = percentPerCopy;
            this.title = title;
            this.description = description;
        }

        public boolean negative() {
            return this.negative;
        }

        public int percentPerCopy() {
            return this.percentPerCopy;
        }

        public int effectPercent() {
            return this.percentPerCopy;
        }

        public String title() {
            return this.title;
        }

        public String description() {
            return this.description;
        }
    }

    public record CardState(int count, double appliedPercent) {
        public CardState {
            count = Math.max(0, count);
            appliedPercent = Double.isFinite(appliedPercent) ? Math.max(0.0D, appliedPercent) : 0.0D;
        }

        public static CardState fromCount(CardType type, int count) {
            int normalizedCount = Math.max(0, count);
            return new CardState(normalizedCount, normalizedCount * type.percentPerCopy());
        }
    }
}
