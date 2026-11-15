package com.revilo.gatesofavarice.client;

import com.revilo.gatesofavarice.dungeon.DungeonWeaponEquipment;
import com.revilo.gatesofavarice.dungeon.ModDimensions;
import com.revilo.gatesofavarice.network.UseDungeonWeaponPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DungeonWeaponInputHandler {
    private DungeonWeaponInputHandler() { }

    @SubscribeEvent
    public static void onWeaponInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack() && !event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || minecraft.player.level().dimension() != ModDimensions.DUNGEON_LEVEL) return;
        boolean secondary = event.isUseItem();
        if (DungeonWeaponEquipment.forRole(minecraft.player, secondary ? "secondary" : "primary").isEmpty()) return;
        event.setCanceled(true);
        if (minecraft.hitResult instanceof EntityHitResult hit) {
            PacketDistributor.sendToServer(new UseDungeonWeaponPayload(secondary, hit.getEntity().getId()));
        }
    }
}
