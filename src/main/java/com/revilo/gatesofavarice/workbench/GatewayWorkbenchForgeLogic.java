package com.revilo.gatesofavarice.workbench;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

public final class GatewayWorkbenchForgeLogic {

    private GatewayWorkbenchForgeLogic() {
    }

    public static boolean canForge(Player player, Container container) {
        return false;
    }

    public static boolean forge(Player player, Container container) {
        return false;
    }
}
