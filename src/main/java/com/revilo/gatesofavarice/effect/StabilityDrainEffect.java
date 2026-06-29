package com.revilo.gatesofavarice.effect;

import com.revilo.gatesofavarice.GatewayExpansion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class StabilityDrainEffect extends MobEffect {

    private static final ResourceLocation HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "stability_drain_health");

    public StabilityDrainEffect() {
        super(MobEffectCategory.HARMFUL, 0x3A203F);
        this.addAttributeModifier(Attributes.MAX_HEALTH, HEALTH_MODIFIER_ID, -0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
