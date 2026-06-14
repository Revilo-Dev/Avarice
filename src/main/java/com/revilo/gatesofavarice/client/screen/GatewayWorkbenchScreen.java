package com.revilo.gatesofavarice.client.screen;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.menu.GatewayWorkbenchMenu;
import com.revilo.gatesofavarice.workbench.GatewayWorkbenchSlots;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GatewayWorkbenchScreen extends AbstractContainerScreen<GatewayWorkbenchMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/workbench.png");
    private static final int FORGE_ANIMATION_TICKS = 22;
    private static final Random PARTICLE_RANDOM = new Random();

    private float crystalHoverScale = 1.0F;
    private int forgeAnimationTicks;
    private boolean pendingForgeSend;
    private final List<ScreenParticle> particles = new ArrayList<>();

    public GatewayWorkbenchScreen(GatewayWorkbenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        boolean crystalHovered = this.isHoveringCrystal(mouseX, mouseY);
        boolean forgeAnimating = this.isForgeAnimating();
        this.crystalHoverScale = Mth.lerp(0.25F, this.crystalHoverScale, 1.0F);

        this.renderCenterCrystal(guiGraphics, crystalHovered);
        this.renderParticles(guiGraphics, partialTick);

        if (!forgeAnimating && crystalHovered && !this.menu.getCrystalStack().isEmpty()) {
            this.renderCrystalTooltip(guiGraphics, mouseX, mouseY);
        } else {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Keep the workbench face clean; progression feedback is handled through slot tooltips.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && this.isHoveringCrystal(mouseX, mouseY)
                && !this.menu.getCrystalStack().isEmpty()
                && this.menu.getCarried().isEmpty()
                && !hasShiftDown()
                && this.minecraft != null
                && this.minecraft.gameMode != null
                && !this.isForgeAnimating()) {
            this.forgeAnimationTicks = FORGE_ANIMATION_TICKS;
            this.pendingForgeSend = true;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if (this.forgeAnimationTicks > 0) {
            this.forgeAnimationTicks--;
            if (this.forgeAnimationTicks == 0 && this.pendingForgeSend && this.minecraft != null && this.minecraft.gameMode != null) {
                if (this.menu.canForge()) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, GatewayWorkbenchMenu.FORGE_BUTTON_ID);
                }
                this.pendingForgeSend = false;
                this.spawnParticleBurst();
            }
        }

        this.tickParticles();
    }

    private void renderCenterCrystal(GuiGraphics guiGraphics, boolean hovered) {
        ItemStack crystal = this.menu.getCrystalStack();
        if (crystal.isEmpty()) {
            return;
        }

        int centerX = this.leftPos + GatewayWorkbenchSlots.DISPLAY_CENTER_X;
        int centerY = this.topPos + GatewayWorkbenchSlots.DISPLAY_CENTER_Y;
        float partialTick = this.minecraft == null ? 0.0F : this.minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        float time = (this.minecraft != null && this.minecraft.level != null)
                ? this.minecraft.level.getGameTime() + partialTick
                : (float) (Util.getMillis() / 16.6667);
        float forgeProgress = this.getForgeProgress(partialTick);
        float hoverBoost = hovered ? 0.12F : 0.0F;
        float bob = Mth.sin(time * 0.11F) * 2.5F;
        float scaleBoost = this.crystalHoverScale + hoverBoost + (forgeProgress * 0.2F);
        float spinSpeed = Mth.lerp(forgeProgress, WorkbenchCrystalRenderer.BASE_SPIN_SPEED, 4.5F);
        WorkbenchCrystalRenderer.render(guiGraphics, crystal, centerX, Math.round(centerY + bob), partialTick, scaleBoost, spinSpeed);

    }

    private void renderCrystalTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack crystal = this.menu.getCrystalStack();
        List<Component> tooltip = new ArrayList<>(Screen.getTooltipFromItem(this.minecraft, crystal));
        guiGraphics.renderTooltip(this.font, tooltip, crystal.getTooltipImage(), crystal, mouseX, mouseY);
    }


    private void renderParticles(GuiGraphics guiGraphics, float partialTick) {
        for (ScreenParticle particle : this.particles) {
            float age = particle.age + partialTick;
            float progress = age / particle.lifetime;
            if (progress >= 1.0F) {
                continue;
            }

            float x = particle.x + particle.velocityX * age;
            float y = particle.y + particle.velocityY * age;
            int alpha = (int) (255 * (1.0F - progress));
            int color = (alpha << 24) | particle.color;
            guiGraphics.fill(Mth.floor(x), Mth.floor(y), Mth.floor(x) + particle.size, Mth.floor(y) + particle.size, color);
        }
    }

    private void tickParticles() {
        this.particles.removeIf(particle -> ++particle.age >= particle.lifetime);
    }

    private void spawnParticleBurst() {
        int centerX = this.leftPos + GatewayWorkbenchSlots.DISPLAY_CENTER_X;
        int centerY = this.topPos + GatewayWorkbenchSlots.DISPLAY_CENTER_Y;
        for (int index = 0; index < 30; index++) {
            double angle = (Math.PI * 2D / 30.0D) * index + (PARTICLE_RANDOM.nextDouble() * 0.16D);
            float speed = 0.8F + PARTICLE_RANDOM.nextFloat() * 1.8F;
            int color = PARTICLE_RANDOM.nextBoolean() ? 0xA24BFF : 0xD58DFF;
            this.particles.add(new ScreenParticle(
                    centerX,
                    centerY,
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed,
                    2 + PARTICLE_RANDOM.nextInt(2),
                    color,
                    12 + PARTICLE_RANDOM.nextInt(8)
            ));
        }
    }

    private boolean isForgeAnimating() {
        return this.forgeAnimationTicks > 0 || this.pendingForgeSend;
    }

    private float getForgeProgress(float partialTick) {
        if (!this.isForgeAnimating()) {
            return 0.0F;
        }

        return 1.0F - ((this.forgeAnimationTicks + partialTick) / FORGE_ANIMATION_TICKS);
    }

    private boolean isHoveringCrystal(double mouseX, double mouseY) {
        int left = this.leftPos + GatewayWorkbenchSlots.DISPLAY_CENTER_X - 26;
        int top = this.topPos + GatewayWorkbenchSlots.DISPLAY_CENTER_Y - 26;
        return mouseX >= left && mouseX <= left + 52 && mouseY >= top && mouseY <= top + 52;
    }

    private static final class ScreenParticle {
        private final float x;
        private final float y;
        private final float velocityX;
        private final float velocityY;
        private final int size;
        private final int color;
        private final int lifetime;
        private int age;

        private ScreenParticle(float x, float y, float velocityX, float velocityY, int size, int color, int lifetime) {
            this.x = x;
            this.y = y;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.size = size;
            this.color = color;
            this.lifetime = lifetime;
        }
    }
}
