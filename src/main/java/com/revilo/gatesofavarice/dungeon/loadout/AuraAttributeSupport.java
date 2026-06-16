package com.revilo.gatesofavarice.dungeon.loadout;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.StatRollRange;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class AuraAttributeSupport {
    private static final String AURA_NAMESPACE = "aura";
    private static final String AURA_PREFIX = AURA_NAMESPACE + ":";

    private AuraAttributeSupport() {}

    public static boolean isAuraAttributeStatId(String statId) {
        return statId != null && statId.startsWith(AURA_PREFIX);
    }

    public static void applyLoadoutBonuses(ItemStack stack, String loadoutId, EquipmentSlot slot, List<StatRollRange> stats) {
        if (stack.isEmpty() || slot != EquipmentSlot.CHEST) {
            return;
        }
        for (StatRollRange range : stats) {
            if (!isAuraAttributeStatId(range.statId())) {
                continue;
            }
            double amount = range.min() == range.max() ? range.min() : (range.min() + range.max()) * 0.5D;
            addOrIncreaseModifier(stack, range.statId(), amount, modifierId("loadout", loadoutId, slot, range.statId()), EquipmentSlotGroup.bySlot(slot));
        }
    }

    public static boolean addCardBonus(ItemStack stack, String title, String label, double delta) {
        if (stack.isEmpty()) {
            return false;
        }
        String attributeId = resolveCardAttributeId(title, label);
        if (attributeId == null) {
            return false;
        }
        EquipmentSlot equipmentSlot = equipmentSlot(stack);
        EquipmentSlotGroup slot = equipmentSlot == null ? EquipmentSlotGroup.ARMOR : EquipmentSlotGroup.bySlot(equipmentSlot);
        return addOrIncreaseModifier(stack, attributeId, delta, modifierId("card", "upgrade", equipmentSlot, attributeId), slot);
    }

    public static String currentCardValue(ItemStack stack, String attributeId) {
        double value = totalAmount(stack, attributeId);
        if (value == 0.0D) {
            return "-";
        }
        return formatSigned(value);
    }

    public static String titleFor(String attributeId) {
        if ("aura:ability_power".equals(attributeId) || attributeId.startsWith("aura:ability_")) {
            return "Ability Card";
        }
        return "Skill Card";
    }

    public static String labelFor(String attributeId) {
        return switch (attributeId) {
            case "aura:ability_power" -> "Ability Power";
            case "aura:ability_force_rampage_bonus" -> "Rampage";
            case "aura:ability_lightning_bonus" -> "Lightning";
            case "aura:ability_poison_bonus" -> "Poison";
            case "aura:ability_force_bonus" -> "Force";
            case "aura:ability_wind_bonus" -> "Wind";
            case "aura:ability_magic_bonus" -> "Magic";
            case "aura:ability_ice_bonus" -> "Ice";
            case "aura:ability_fire_bonus" -> "Fire";
            case "aura:skill_strength_bonus" -> "Strength";
            case "aura:skill_power_bonus" -> "Power";
            case "aura:skill_agility_bonus" -> "Agility";
            case "aura:skill_resistance_bonus" -> "Resistance";
            default -> attributeId.substring(AURA_PREFIX.length()).replace("_bonus", "").replace('_', ' ');
        };
    }

    public static String formatDelta(double value) {
        return formatSigned(value);
    }

    private static boolean addOrIncreaseModifier(ItemStack stack, String attributeId, double delta, ResourceLocation modifierId, EquipmentSlotGroup slot) {
        Optional<Holder.Reference<Attribute>> attribute = resolveAttribute(attributeId);
        if (attribute.isEmpty()) {
            return false;
        }
        ItemAttributeModifiers current = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        if (current.modifiers().isEmpty()) {
            current = stack.getItem().getDefaultAttributeModifiers();
        }
        double existing = current.modifiers().stream()
                .filter(entry -> entry.attribute().equals(attribute.get()) && entry.modifier().is(modifierId))
                .mapToDouble(entry -> entry.modifier().amount())
                .sum();
        ItemAttributeModifiers updated = current.withModifierAdded(
                attribute.get(),
                new AttributeModifier(modifierId, existing + delta, AttributeModifier.Operation.ADD_VALUE),
                slot);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, updated);
        return true;
    }

    private static double totalAmount(ItemStack stack, String attributeId) {
        Optional<Holder.Reference<Attribute>> attribute = resolveAttribute(attributeId);
        if (attribute.isEmpty()) {
            return 0.0D;
        }
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers().stream()
                .filter(entry -> entry.attribute().equals(attribute.get()))
                .mapToDouble(entry -> entry.modifier().amount())
                .sum();
    }

    private static Optional<Holder.Reference<Attribute>> resolveAttribute(String attributeId) {
        ResourceLocation id = ResourceLocation.tryParse(attributeId);
        if (id == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ATTRIBUTE.getHolder(id);
    }

    private static String resolveCardAttributeId(String title, String label) {
        if (!"Ability Card".equals(title) && !"Skill Card".equals(title)) {
            return null;
        }
        return switch (label) {
            case "Ability Power" -> "aura:ability_power";
            case "Rampage" -> "aura:ability_force_rampage_bonus";
            case "Lightning" -> "aura:ability_lightning_bonus";
            case "Poison" -> "aura:ability_poison_bonus";
            case "Force" -> "aura:ability_force_bonus";
            case "Wind" -> "aura:ability_wind_bonus";
            case "Magic" -> "aura:ability_magic_bonus";
            case "Ice" -> "aura:ability_ice_bonus";
            case "Fire" -> "aura:ability_fire_bonus";
            case "Strength" -> "aura:skill_strength_bonus";
            case "Power" -> "aura:skill_power_bonus";
            case "Agility" -> "aura:skill_agility_bonus";
            case "Resistance" -> "aura:skill_resistance_bonus";
            default -> null;
        };
    }

    private static ResourceLocation modifierId(String source, String owner, EquipmentSlot slot, String attributeId) {
        String slotName = slot == null ? "armor" : slot.getName();
        String path = (source + "_" + owner + "_" + slotName + "_" + attributeId.substring(AURA_PREFIX.length()))
                .toLowerCase(Locale.ROOT)
                .replace(':', '_')
                .replace('/', '_');
        return ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, path);
    }

    private static EquipmentSlot equipmentSlot(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armorItem ? armorItem.getEquipmentSlot() : null;
    }

    private static String formatSigned(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001D) {
            return String.format(Locale.ROOT, "%+.0f", value);
        }
        return String.format(Locale.ROOT, "%+.2f", value);
    }
}
