package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.item.data.LootRarity;
import com.revilo.gatesofavarice.shop.GatewaySellValues;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class LootMaterialItem extends Item implements RarityTintedItemName {

    private final LootRarity rarity;

    public LootMaterialItem(LootRarity rarity, Properties properties) {
        super(properties.stacksTo(99));
        this.rarity = rarity;
    }

    public LootRarity rarity() {
        return this.rarity;
    }

    @Override
    public net.minecraft.ChatFormatting nameColor() {
        return this.rarity.color();
    }

    @Override
    public Component getName(ItemStack stack) {
        if (this.rarity == LootRarity.LEGENDARY || this.rarity == LootRarity.EPIC) {
            return RarityTintedItemName.super.tintedName(stack, super.getName(stack));
        }
        return super.getName(stack).copy().withStyle(this.rarity.color());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(this.rarity.displayName());
        GatewaySellValues.appendSellValueTooltip(stack, tooltipComponents);
    }
}
