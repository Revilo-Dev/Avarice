package com.revilo.gatesofavarice.integration.jei;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.client.screen.DungeonWaveScreen;
import com.revilo.gatesofavarice.client.screen.ShopkeeperScreen;
import com.revilo.gatesofavarice.registry.ModItems;
import java.util.List;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class GatewayExpansionJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new GatewayWorkbenchJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.GATEWAY_WORKBENCH.get()), GatewayWorkbenchJeiCategory.RECIPE_TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addIngredientInfo(crystalStacks(), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.gatesofavarice.info.crystals.0"),
                Component.translatable("jei.gatesofavarice.info.crystals.1"));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(DungeonWaveScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(DungeonWaveScreen containerScreen) {
                return List.of(new Rect2i(-10000, -10000, 20000, 20000));
            }
        });
        registration.addGuiContainerHandler(ShopkeeperScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(ShopkeeperScreen containerScreen) {
                return List.of(new Rect2i(-10000, -10000, 20000, 20000));
            }
        });
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
    }

    private static List<ItemStack> crystalStacks() {
        return List.of(
                new ItemStack(ModItems.TIER_1_CRYSTAL.get()),
                new ItemStack(ModItems.TIER_2_CRYSTAL.get()),
                new ItemStack(ModItems.TIER_3_CRYSTAL.get()),
                new ItemStack(ModItems.TIER_4_CRYSTAL.get()),
                new ItemStack(ModItems.TIER_5_CRYSTAL.get()));
    }

}
