package com.revilo.gatesofavarice.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.entity.GatewayCrystalEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class GatewayCrystalRenderer extends EntityRenderer<GatewayCrystalEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/entity/gateway.png");
    private static final int FRAME_COUNT = 9;
    private static final int TIER_1_COLOR = 0x406A82;
    private static final int TIER_2_COLOR = 0x405082;
    private static final int TIER_3_COLOR = 0x5F4082;
    private static final int TIER_4_COLOR = 0x824079;
    private static final int TIER_5_COLOR = 0x824050;

    public GatewayCrystalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GatewayCrystalEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        float width = entity.getBbWidth();
        float height = entity.getBbHeight();
        float halfWidth = width * 0.5F;

        int frame = (entity.tickCount / 4) % FRAME_COUNT;
        float frameV = 1.0F / FRAME_COUNT;
        float minV = frame * frameV;
        float maxV = minV + frameV;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(this.getTextureLocation(entity)));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        int tintColor = tintColorForTier(entity.getCrystalTier());

        addVertex(consumer, pose, poseMatrix, -halfWidth, 0.0F, 0.0F, 0.0F, maxV, packedLight, tintColor);
        addVertex(consumer, pose, poseMatrix, halfWidth, 0.0F, 0.0F, 1.0F, maxV, packedLight, tintColor);
        addVertex(consumer, pose, poseMatrix, halfWidth, height, 0.0F, 1.0F, minV, packedLight, tintColor);
        addVertex(consumer, pose, poseMatrix, -halfWidth, height, 0.0F, 0.0F, minV, packedLight, tintColor);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GatewayCrystalEntity entity) {
        return TEXTURE;
    }

    private static void addVertex(VertexConsumer consumer, PoseStack.Pose pose, Matrix4f poseMatrix,
                                  float x, float y, float z, float u, float v, int packedLight, int tintColor) {
        consumer.addVertex(poseMatrix, x, y, z)
                .setColor((tintColor >> 16) & 0xFF, (tintColor >> 8) & 0xFF, tintColor & 0xFF, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static int tintColorForTier(int tier) {
        return switch (tier) {
            case 2 -> TIER_2_COLOR;
            case 3 -> TIER_3_COLOR;
            case 4 -> TIER_4_COLOR;
            case 5 -> TIER_5_COLOR;
            default -> TIER_1_COLOR;
        };
    }
}
