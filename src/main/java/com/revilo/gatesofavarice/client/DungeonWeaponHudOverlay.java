package com.revilo.gatesofavarice.client;

import com.revilo.gatesofavarice.dungeon.ModDimensions;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class DungeonWeaponHudOverlay {
    private DungeonWeaponHudOverlay() {
    }

    @SubscribeEvent
    public static void hideDungeonHunger(RenderGuiLayerEvent.Pre event) {
        if (!VanillaGuiLayers.FOOD_LEVEL.equals(event.getName())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.level().dimension() == ModDimensions.DUNGEON_LEVEL) {
            event.setCanceled(true);
        }
    }

}
