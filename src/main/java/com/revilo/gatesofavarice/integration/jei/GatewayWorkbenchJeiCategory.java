package com.revilo.gatesofavarice.integration.jei;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class GatewayWorkbenchJeiCategory implements IRecipeCategory<GatewayWorkbenchJeiRecipe> {

    public static final RecipeType<GatewayWorkbenchJeiRecipe> RECIPE_TYPE = RecipeType.create(
            GatewayExpansion.MOD_ID,
            "gateway_workbench",
            GatewayWorkbenchJeiRecipe.class);

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "textures/gui/workbench.png");

    private final IDrawable icon;

    public GatewayWorkbenchJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.GATEWAY_WORKBENCH.get()));
    }

    @Override
    public RecipeType<GatewayWorkbenchJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.gatesofavarice.gateway_workbench");
    }

    @Override
    public int getWidth() {
        return 144;
    }

    @Override
    public int getHeight() {
        return 54;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GatewayWorkbenchJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 18)
                .addItemStacks(recipe.crystals());
        builder.addSlot(RecipeIngredientRole.INPUT, 44, 18)
                .addItemStacks(recipe.components());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 18)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(GatewayWorkbenchJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.blit(TEXTURE, 0, 10, 0, 0, 144, 34, 256, 256);
        guiGraphics.drawString(Minecraft.getInstance().font, recipe.title(), 4, 0, 0x404040, false);
    }
}
