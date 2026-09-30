package com.revilo.gatesofavarice.client;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Client-only visual effects for the Infinite Library dungeon dimension. */
public final class InfiniteLibrarySkyEffects extends DimensionSpecialEffects {
    private static final float SKYBOX_RADIUS = 100.0F;
    private static final RenderType[] SKYBOX_FACES = {
            skyboxFace("back"), skyboxFace("front"), skyboxFace("top"),
            skyboxFace("bottom"), skyboxFace("left"), skyboxFace("right")
    };

    private static RenderType skyboxFace(String name) {
        return RenderType.create(
            "infinite_library_sky_" + name,
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_TEX_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture(name), false, false))
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    }

    public InfiniteLibrarySkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
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
        RenderSystem.depthMask(false);
        try {
            Matrix4f inverseView = new Matrix4f(modelViewMatrix).invert();
            for (int face = 0; face < SKYBOX_FACES.length; face++) {
                SKYBOX_FACES[face].setupState.run();
                renderSkyboxFace(face, inverseView);
                SKYBOX_FACES[face].clearState.run();
            }
        } finally {
            RenderSystem.depthMask(true);
        }
        return true;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/environment/" + name + ".png");
    }

    private static void renderSkyboxFace(int face, Matrix4f inverseView) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float radius = SKYBOX_RADIUS;
        switch (face) {
            case 0 -> addFace(buffer, inverseView, -radius, -radius, -radius, radius, -radius, -radius, radius, radius, -radius, -radius, radius, -radius);
            case 1 -> addFace(buffer, inverseView, radius, -radius, radius, -radius, -radius, radius, -radius, radius, radius, radius, radius, radius);
            case 2 -> addFace(buffer, inverseView, -radius, -radius, radius, radius, -radius, radius, radius, -radius, -radius, -radius, -radius, -radius);
            case 3 -> addFace(buffer, inverseView, -radius, radius, -radius, radius, radius, -radius, radius, radius, radius, -radius, radius, radius);
            case 4 -> addFace(buffer, inverseView, -radius, -radius, radius, -radius, -radius, -radius, -radius, radius, -radius, -radius, radius, radius);
            case 5 -> addFace(buffer, inverseView, radius, -radius, -radius, radius, -radius, radius, radius, radius, radius, radius, radius, -radius);
            default -> throw new IllegalArgumentException("Unknown skybox face: " + face);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void addFace(BufferBuilder buffer, Matrix4f inverseView, float x1, float y1, float z1, float x2, float y2, float z2,
            float x3, float y3, float z3, float x4, float y4, float z4) {
        addVertex(buffer, inverseView, x1, y1, z1, 0.0F, 0.0F);
        addVertex(buffer, inverseView, x2, y2, z2, 1.0F, 0.0F);
        addVertex(buffer, inverseView, x3, y3, z3, 1.0F, 1.0F);
        addVertex(buffer, inverseView, x4, y4, z4, 0.0F, 1.0F);
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f inverseView, float x, float y, float z, float u, float v) {
        Vector3f position = inverseView.transformPosition(x, y, z, new Vector3f());
        buffer.addVertex(position.x, position.y, position.z).setUv(u, v);
    }
}
