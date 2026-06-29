package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.shop.GatewaySellValues;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class MagnetItem extends Item implements RarityTintedItemName {
    private static final String BONUS_RANGE_UPGRADES_KEY = "magnet_range_upgrades";
    private static final String PULL_SPEED_UPGRADES_KEY = "magnet_speed_upgrades";

    private final ChatFormatting nameColor;
    private final int bonusRange;
    private final int pullSpeed;
    private final int runeSlots;

    public MagnetItem(ChatFormatting nameColor, int bonusRange, int pullSpeed, int runeSlots, Properties properties) {
        super(properties);
        this.nameColor = nameColor;
        this.bonusRange = bonusRange;
        this.pullSpeed = pullSpeed;
        this.runeSlots = runeSlots;
    }

    @Override
    public ChatFormatting nameColor() {
        return this.nameColor;
    }

    @Override
    public Component getName(ItemStack stack) {
        return this.tintedName(stack, super.getName(stack));
    }

    public int bonusRange() {
        return this.bonusRange;
    }

    public int bonusRange(ItemStack stack) {
        return this.bonusRange + upgradeValue(stack, BONUS_RANGE_UPGRADES_KEY);
    }

    public int pullSpeed() {
        return this.pullSpeed;
    }

    public int pullSpeed(ItemStack stack) {
        return this.pullSpeed + upgradeValue(stack, PULL_SPEED_UPGRADES_KEY);
    }

    public double attractionRange(ItemStack stack) {
        return 3.0D + this.bonusRange(stack);
    }

    public double attractionForce(ItemStack stack) {
        return 0.025D + (this.pullSpeed(stack) * 0.03D);
    }

    public int runeSlots() {
        return this.runeSlots;
    }

    public static void upgradeDungeonMagnet(ItemStack stack) {
        if (!(stack.getItem() instanceof MagnetItem)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag root = tag.getCompound(GatewayExpansion.MOD_ID);
            root.putInt(BONUS_RANGE_UPGRADES_KEY, root.getInt(BONUS_RANGE_UPGRADES_KEY) + 1);
            root.putInt(PULL_SPEED_UPGRADES_KEY, root.getInt(PULL_SPEED_UPGRADES_KEY) + 1);
            tag.put(GatewayExpansion.MOD_ID, root);
        });
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        RunicItemSupport.ensureRunicData(stack, this.runeSlots());
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("+" + (this.bonusRange(stack) + 2) + " block magnet range").withStyle(ChatFormatting.AQUA));
        if (this.pullSpeed(stack) > 0) {
            tooltipComponents.add(Component.literal("+" + this.pullSpeed(stack) + " pull speed").withStyle(ChatFormatting.GREEN));
        }
        GatewaySellValues.appendSellValueTooltip(stack, tooltipComponents);
    }

    private static int upgradeValue(ItemStack stack, String key) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(GatewayExpansion.MOD_ID);
        return Math.max(0, root.getInt(key));
    }
}
