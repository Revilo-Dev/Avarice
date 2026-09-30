package com.revilo.gatesofavarice.network;

import com.revilo.gatesofavarice.GatewayExpansion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenDungeonExitConfirmationPayload(int portalEntityId) implements CustomPacketPayload {
    public static final Type<OpenDungeonExitConfirmationPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "open_dungeon_exit_confirmation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDungeonExitConfirmationPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeVarInt(payload.portalEntityId),
                    buffer -> new OpenDungeonExitConfirmationPayload(buffer.readVarInt()));

    @Override
    public Type<OpenDungeonExitConfirmationPayload> type() {
        return TYPE;
    }
}
