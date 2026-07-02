package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.GatewayExpansion;
import java.util.Locale;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.revilodev.runic.runes.RuneSlots;

public class DungeonArmorItem extends ArmorItem {
    public static final String ARMOR_SET_NAME_KEY = "loadout_set_name";
    public static final String ARMOR_TYPE_KEY = "armor_type";

    private final int defaultRuneSlots;

    public DungeonArmorItem(Type type, Properties properties) {
        super(ArmorMaterials.NETHERITE, type, properties);
        this.defaultRuneSlots = switch (type) {
            case HELMET, BOOTS -> 2;
            case CHESTPLATE, LEGGINGS -> 3;
            default -> 2;
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        String setName = dungeonData(stack).getString(ARMOR_SET_NAME_KEY);
        if (setName.isBlank()) {
            setName = "Dungeon";
        }
        return Component.literal(setName.replace(" Set", "") + " " + displayPieceName(this.getType()));
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        RunicItemSupport.ensureRunicData(stack, Math.max(this.defaultRuneSlots, RuneSlots.capacity(stack)));
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    public static String armorType(ItemStack stack) {
        return dungeonData(stack).getString(ARMOR_TYPE_KEY);
    }

    private static CompoundTag dungeonData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(GatewayExpansion.MOD_ID);
    }

    private static String displayPieceName(Type type) {
        String raw = type.getName().replace('_', ' ');
        String[] parts = raw.toLowerCase(Locale.ROOT).split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
