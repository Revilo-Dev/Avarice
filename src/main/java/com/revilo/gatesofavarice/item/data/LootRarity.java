package com.revilo.gatesofavarice.item.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum LootRarity {
    COMMON("common", ChatFormatting.WHITE, 55),
    UNCOMMON("uncommon", ChatFormatting.GREEN, 28),
    RARE("rare", ChatFormatting.AQUA, 12),
    EPIC("epic", ChatFormatting.DARK_PURPLE, 4),
    LEGENDARY("legendary", ChatFormatting.GOLD, 1);

    private final String id;
    private final ChatFormatting color;
    private final int weight;

    LootRarity(String id, ChatFormatting color, int weight) {
        this.id = id;
        this.color = color;
        this.weight = weight;
    }

    public ChatFormatting color() {
        return this.color;
    }

    public int weight() {
        return this.weight;
    }

    public Component displayName() {
        return Component.translatable("rarity.gatesofavarice." + this.id).withStyle(this.color);
    }
}
