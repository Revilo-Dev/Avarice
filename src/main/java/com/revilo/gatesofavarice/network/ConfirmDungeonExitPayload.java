package com.revilo.gatesofavarice.network;

import com.revilo.gatesofavarice.GatewayExpansion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ConfirmDungeonExitPayload(int portalEntityId, boolean exitDungeon) implements CustomPacketPayload {
    public static final Type<ConfirmDungeonExitPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "confirm_dungeon_exit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfirmDungeonExitPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeVarInt(payload.portalEntityId);
                buffer.writeBoolean(payload.exitDungeon);
            }, buffer -> new ConfirmDungeonExitPayload(buffer.readVarInt(), buffer.readBoolean()));

    @Override
    public Type<ConfirmDungeonExitPayload> type() {
        return TYPE;
    }
}
