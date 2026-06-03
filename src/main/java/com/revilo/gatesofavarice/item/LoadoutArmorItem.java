package com.revilo.gatesofavarice.item;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

public class LoadoutArmorItem extends ArmorItem {
    private final String setId;

    public LoadoutArmorItem(net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> material, Type type, Properties properties, String setId) {
        super(material, type, properties);
        this.setId = setId;
    }

    public String setId() {
        return this.setId;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(displaySetName(this.setId) + " " + displayPieceName(this.getType()));
    }

    private static String displaySetName(String setId) {
        return switch (setId) {
            case "shadow_set" -> "Assassin";
            case "steel_knight_set" -> "Knight";
            case "rage_set" -> "Berserker";
            case "fortress_set" -> "Vanguard";
            case "windwalker_set" -> "Samurai";
            case "soulbound_set" -> "Reaper";
            case "hunter_set" -> "Ranger";
            case "sharpshooter_set" -> "Marksman";
            case "arena_set" -> "Gladiator";
            case "arcane_set" -> "Spellblade";
            case "tyrant_set" -> "Warlord";
            case "traveler_set" -> "Nomad";
            default -> titleCase(setId.replace("_set", "").replace('_', ' '));
        };
    }

    private static String displayPieceName(Type type) {
        return switch (type) {
            case HELMET -> "Helmet";
            case CHESTPLATE -> "Chestplate";
            case LEGGINGS -> "Leggings";
            case BOOTS -> "Boots";
            default -> "Armor";
        };
    }

    private static String titleCase(String value) {
        String[] parts = value.toLowerCase(Locale.ROOT).split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
