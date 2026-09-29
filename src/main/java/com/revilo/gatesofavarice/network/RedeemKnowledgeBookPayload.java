package com.revilo.gatesofavarice.network;

import com.revilo.gatesofavarice.GatewayExpansion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RedeemKnowledgeBookPayload(int inventorySlot) implements CustomPacketPayload {
    public static final Type<RedeemKnowledgeBookPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "redeem_knowledge_book"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RedeemKnowledgeBookPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeVarInt(payload.inventorySlot),
                    buffer -> new RedeemKnowledgeBookPayload(buffer.readVarInt()));

    @Override
    public Type<RedeemKnowledgeBookPayload> type() {
        return TYPE;
    }
}
