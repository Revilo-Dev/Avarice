package com.revilo.gatesofavarice.client.render;

import com.mojang.math.Axis;
import com.revilo.gatesofavarice.entity.MythicCoinOrbEntity;
import com.revilo.gatesofavarice.registry.ModItems;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;

public final class MythicCoinOrbRenderer extends EntityRenderer<MythicCoinOrbEntity> {
    private final net.minecraft.client.renderer.entity.ItemRenderer itemRenderer;

    public MythicCoinOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(MythicCoinOrbEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.12D + Math.sin((entity.tickCount + partialTick) * 0.2D) * 0.06D, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.55F, 0.55F, 0.55F);
        this.itemRenderer.renderStatic(new ItemStack(ModItems.MYTHIC_COIN.get()), ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MythicCoinOrbEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
