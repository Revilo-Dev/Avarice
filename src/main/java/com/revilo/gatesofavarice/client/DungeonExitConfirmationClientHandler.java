package com.revilo.gatesofavarice.client;

import com.revilo.gatesofavarice.client.screen.DungeonExitConfirmationScreen;
import com.revilo.gatesofavarice.network.OpenDungeonExitConfirmationPayload;
import net.minecraft.client.Minecraft;

public final class DungeonExitConfirmationClientHandler {
    private DungeonExitConfirmationClientHandler() {
    }

    public static void open(OpenDungeonExitConfirmationPayload payload) {
        Minecraft.getInstance().setScreen(new DungeonExitConfirmationScreen(payload.portalEntityId()));
    }
}
