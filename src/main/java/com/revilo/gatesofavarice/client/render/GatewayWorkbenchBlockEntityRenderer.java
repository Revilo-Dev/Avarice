package com.revilo.gatesofavarice.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.revilo.gatesofavarice.block.entity.GatewayWorkbenchBlockEntity;
import com.revilo.gatesofavarice.workbench.GatewayWorkbenchSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class GatewayWorkbenchBlockEntityRenderer implements BlockEntityRenderer<GatewayWorkbenchBlockEntity> {

    private static final float CRYSTAL_HEIGHT = 1.03F;
    private static final float CRYSTAL_SCALE = 0.9F;

    public GatewayWorkbenchBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GatewayWorkbenchBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) {
            return;
        }

        ItemStack crystal = blockEntity.getItem(GatewayWorkbenchSlots.CRYSTAL_SLOT);
        if (crystal.isEmpty()) {
            return;
        }

        float time = blockEntity.getLevel().getGameTime() + partialTick;
        renderCrystal(blockEntity, crystal, time, poseStack, buffer, packedLight);
    }

    private void renderCrystal(GatewayWorkbenchBlockEntity blockEntity, ItemStack crystal, float time, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();
        poseStack.pushPose();
        poseStack.translate(0.5D, CRYSTAL_HEIGHT + (Mth.sin(time * 0.09F) * 0.01F), 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees((time * 1.8F) % 360.0F));
        poseStack.scale(CRYSTAL_SCALE, CRYSTAL_SCALE, CRYSTAL_SCALE);
        minecraft.getItemRenderer().renderStatic(
                crystal,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                blockEntity.getLevel(),
                0
        );
        poseStack.popPose();
    }

}
