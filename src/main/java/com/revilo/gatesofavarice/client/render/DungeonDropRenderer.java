package com.revilo.gatesofavarice.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.revilo.gatesofavarice.entity.DungeonDropEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.EntityHitResult;

public final class DungeonDropRenderer extends EntityRenderer<DungeonDropEntity> {
    private final net.minecraft.client.renderer.entity.ItemRenderer itemRenderer;

    public DungeonDropRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(DungeonDropEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.stack().isEmpty()) return;
        poseStack.pushPose();
        poseStack.translate(0.0D, .22D + Math.sin((entity.tickCount + partialTick) * .16D) * .08D, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(.8F, .8F, .8F);
        this.itemRenderer.renderStatic(entity.stack(), ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
        if (Minecraft.getInstance().hitResult instanceof EntityHitResult hit && hit.getEntity() == entity) {
            this.renderNameTag(entity, tooltip(entity), poseStack, buffer, packedLight, partialTick);
        }
    }

    private static net.minecraft.network.chat.Component tooltip(DungeonDropEntity entity) {
        if (entity.isArrowBundle()) return net.minecraft.network.chat.Component.literal(entity.stack().getHoverName().getString() + " x" + entity.stack().getCount());
        StringBuilder text = new StringBuilder(entity.stack().getHoverName().getString());
        for (net.minecraft.network.chat.Component line : entity.stack().getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, Minecraft.getInstance().player, TooltipFlag.Default.NORMAL)) {
            String value = line.getString();
            if (!value.equals(entity.stack().getHoverName().getString()) && !value.isBlank()) text.append('\n').append(value);
        }
        text.append("\nHold Shift to equip");
        return net.minecraft.network.chat.Component.literal(text.toString());
    }

    @Override public ResourceLocation getTextureLocation(DungeonDropEntity entity) { return InventoryMenu.BLOCK_ATLAS; }
}
