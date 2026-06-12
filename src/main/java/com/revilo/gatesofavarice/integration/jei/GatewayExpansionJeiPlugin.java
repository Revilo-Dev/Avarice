package com.revilo.gatesofavarice.integration.jei;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.augment.AugmentDefinition;
import com.revilo.gatesofavarice.augment.AugmentDefinitionPool;
import com.revilo.gatesofavarice.augment.AugmentStackData;
import com.revilo.gatesofavarice.catalyst.CatalystDefinition;
import com.revilo.gatesofavarice.catalyst.CatalystDefinitionPool;
import com.revilo.gatesofavarice.catalyst.CatalystStackData;
import com.revilo.gatesofavarice.client.screen.DungeonWaveScreen;
import com.revilo.gatesofavarice.client.screen.ShopkeeperScreen;
import com.revilo.gatesofavarice.item.data.AugmentDifficultyTier;
import com.revilo.gatesofavarice.registry.ModItems;
import dev.shadowsoffire.gateways.GatewayObjects;
import java.util.ArrayList;
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
import net.minecraft.world.item.Item;
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
        registration.addRecipes(GatewayWorkbenchJeiCategory.RECIPE_TYPE, workbenchRecipes());
        registration.addIngredientInfo(crystalStacks(), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.gatesofavarice.info.crystals.0"),
                Component.translatable("jei.gatesofavarice.info.crystals.1"));
        registration.addIngredientInfo(augmentBaseStacks(), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.gatesofavarice.info.augments.0"),
                Component.translatable("jei.gatesofavarice.info.augments.1"));
        registration.addIngredientInfo(catalystBaseStacks(), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.gatesofavarice.info.catalysts.0"),
                Component.translatable("jei.gatesofavarice.info.catalysts.1"));
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

    private static List<GatewayWorkbenchJeiRecipe> workbenchRecipes() {
        List<ItemStack> crystals = crystalStacks();
        List<GatewayWorkbenchJeiRecipe> recipes = new ArrayList<>();
        recipes.add(new GatewayWorkbenchJeiRecipe(
                Component.translatable("jei.gatesofavarice.recipe.augments"),
                crystals,
                augmentStacks(AugmentDifficultyTier.EASY, AugmentDifficultyTier.MEDIUM, AugmentDifficultyTier.HARD, AugmentDifficultyTier.EXTREME),
                new ItemStack(GatewayObjects.GATE_PEARL.value())));
        recipes.add(new GatewayWorkbenchJeiRecipe(
                Component.translatable("jei.gatesofavarice.recipe.catalysts"),
                crystals,
                catalystStacks(),
                new ItemStack(GatewayObjects.GATE_PEARL.value())));
        return recipes;
    }

    private static List<ItemStack> crystalStacks() {
        return List.of(
                new ItemStack(ModItems.TIER_1_CRYSTAL.get()),
                new ItemStack(ModItems.TIER_2_CRYSTAL.get()),
                new ItemStack(ModItems.TIER_3_CRYSTAL.get()),
                new ItemStack(ModItems.TIER_4_CRYSTAL.get()),
                new ItemStack(ModItems.TIER_5_CRYSTAL.get()));
    }

    private static List<ItemStack> augmentBaseStacks() {
        return List.of(
                new ItemStack(ModItems.EASY_AUGMENT.get()),
                new ItemStack(ModItems.MEDIUM_AUGMENT.get()),
                new ItemStack(ModItems.HARD_AUGMENT.get()),
                new ItemStack(ModItems.EXTREME_AUGMENT.get()));
    }

    private static List<ItemStack> catalystBaseStacks() {
        return List.of(
                new ItemStack(ModItems.TIME_CATALYST.get()));
    }

    private static List<ItemStack> augmentStacks(AugmentDifficultyTier... tiers) {
        List<ItemStack> stacks = new ArrayList<>();
        for (AugmentDifficultyTier tier : tiers) {
            Item item = switch (tier) {
                case EASY -> ModItems.EASY_AUGMENT.get();
                case MEDIUM -> ModItems.MEDIUM_AUGMENT.get();
                case HARD -> ModItems.HARD_AUGMENT.get();
                case EXTREME -> ModItems.EXTREME_AUGMENT.get();
            };
            for (AugmentDefinition definition : AugmentDefinitionPool.definitionsFor(tier)) {
                ItemStack stack = new ItemStack(item);
                AugmentStackData.setDefinitionId(stack, definition.id());
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private static List<ItemStack> catalystStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        Item item = ModItems.TIME_CATALYST.get();
        for (CatalystDefinition definition : CatalystDefinitionPool.definitionsFor(com.revilo.gatesofavarice.catalyst.CatalystArchetype.TIME)) {
            ItemStack stack = new ItemStack(item);
            CatalystStackData.setDefinitionId(stack, definition.id());
            stacks.add(stack);
        }
        return stacks;
    }
}
