package com.revilo.gatesofavarice.item;

import com.revilo.gatesofavarice.item.data.CrystalForgeData;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class CrystalItem extends Item {

    private final CrystalTier crystalTier;

    public CrystalItem(CrystalTier crystalTier, Properties properties) {
        super(properties.stacksTo(99));
        this.crystalTier = crystalTier;
    }

    public CrystalTier crystalTier() {
        return this.crystalTier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.gatesofavarice.crystal.description").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.gatesofavarice.crystal.tier", this.crystalTier.tier()).withStyle(ChatFormatting.AQUA));
        tooltipComponents.addAll(CrystalForgeData.buildCrystalTooltip(stack, isAltDown()));
    }

    private static boolean isAltDown() {
        try {
            Class<?> screenClass = Class.forName("net.minecraft.client.gui.screens.Screen");
            Method method = screenClass.getMethod("hasAltDown");
            return Boolean.TRUE.equals(method.invoke(null));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    /** Crystal tier now controls deck capacity only. */
    public record CrystalTier(int tier) {
    }
}
