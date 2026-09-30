package com.revilo.gatesofavarice.integration.jei;

import com.revilo.gatesofavarice.GatewayExpansion;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = GatewayExpansion.MOD_ID, value = Dist.CLIENT)
final class JeiOverlayVisibility {
    private static Object toggleState;
    private static Method isOverlayEnabled;
    private static Method toggleOverlayEnabled;
    private static Method isBookmarkOverlayEnabled;
    private static Method toggleBookmarkEnabled;
    private static boolean restoreOverlay;
    private static boolean restoreBookmarks;
    private static boolean hiddenByGateway;

    private JeiOverlayVisibility() {
    }

    static void initialize(IJeiRuntime runtime) {
        try {
            Object ingredientOverlay = runtime.getIngredientListOverlay();
            Field field = ingredientOverlay.getClass().getDeclaredField("toggleState");
            field.setAccessible(true);
            toggleState = field.get(ingredientOverlay);
            Class<?> type = toggleState.getClass();
            isOverlayEnabled = type.getMethod("isOverlayEnabled");
            toggleOverlayEnabled = type.getMethod("toggleOverlayEnabled");
            isBookmarkOverlayEnabled = type.getMethod("isBookmarkOverlayEnabled");
            toggleBookmarkEnabled = type.getMethod("toggleBookmarkEnabled");
        } catch (ReflectiveOperationException ignored) {
            toggleState = null;
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        setVisible(!isGatewayScreen(event.getNewScreen()));
    }

    private static boolean isGatewayScreen(Screen screen) {
        return screen != null && screen.getClass().getPackageName().equals("com.revilo.gatesofavarice.client.screen");
    }

    private static void setVisible(boolean visible) {
        if (toggleState == null) {
            return;
        }
        try {
            if (!visible && !hiddenByGateway) {
                restoreOverlay = (boolean) isOverlayEnabled.invoke(toggleState);
                restoreBookmarks = (boolean) isBookmarkOverlayEnabled.invoke(toggleState);
                if (restoreOverlay) {
                    toggleOverlayEnabled.invoke(toggleState);
                }
                if (restoreBookmarks) {
                    toggleBookmarkEnabled.invoke(toggleState);
                }
                hiddenByGateway = true;
            } else if (visible && hiddenByGateway) {
                if (restoreOverlay && !(boolean) isOverlayEnabled.invoke(toggleState)) {
                    toggleOverlayEnabled.invoke(toggleState);
                }
                if (restoreBookmarks && !(boolean) isBookmarkOverlayEnabled.invoke(toggleState)) {
                    toggleBookmarkEnabled.invoke(toggleState);
                }
                restoreOverlay = false;
                restoreBookmarks = false;
                hiddenByGateway = false;
            }
        } catch (ReflectiveOperationException ignored) {
            toggleState = null;
        }
    }
}
