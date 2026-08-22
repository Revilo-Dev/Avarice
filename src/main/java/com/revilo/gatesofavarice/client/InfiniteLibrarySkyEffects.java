package com.revilo.gatesofavarice.client;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Client-only visual effects for the Infinite Library dungeon dimension. */
public final class InfiniteLibrarySkyEffects extends DimensionSpecialEffects {
    private static final RenderType INFINITE_LIBRARY_PORTAL = RenderType.create(
            "infinite_library_portal_sky",
            DefaultVertexFormat.POSITION,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_END_PORTAL_SHADER)
                    .setTextureState(RenderStateShard.MultiTextureStateShard.builder()
                            .add(texture("back"), false, false)
                            .add(texture("front"), false, false)
                            .build())
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    public InfiniteLibrarySkyEffects() {
        super(Float.NaN, false, SkyType.NONE, true, true);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor.scale(0.18D);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public boolean renderSky(
            ClientLevel level,
            int ticks,
            float partialTick,
            Matrix4f modelViewMatrix,
            Camera camera,
            Matrix4f projectionMatrix,
            boolean isFoggy,
            Runnable setupFog) {
        // The End Portal shader is a screen-projected effect. Rendering it on a cube creates
        // joins between faces, so draw one clip-space background quad instead.
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.DISTANCE_TO_ORIGIN);
        var modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(false);
        INFINITE_LIBRARY_PORTAL.setupState.run();
        try {
            // The vanilla portal uses 15 layers. The shader supports 16, which adds a finer
            // foreground layer without indexing beyond its built-in colour palette.
            RenderSystem.getShader().safeGetUniform("EndPortalLayers").set(16);
            renderFullscreenQuad();
        } finally {
            INFINITE_LIBRARY_PORTAL.clearState.run();
            RenderSystem.depthMask(true);
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
        return true;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/environment/" + name + ".png");
    }

    private static void renderFullscreenQuad() {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.addVertex(-1.0F, -1.0F, 0.0F);
        buffer.addVertex(1.0F, -1.0F, 0.0F);
        buffer.addVertex(1.0F, 1.0F, 0.0F);
        buffer.addVertex(-1.0F, 1.0F, 0.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
}
