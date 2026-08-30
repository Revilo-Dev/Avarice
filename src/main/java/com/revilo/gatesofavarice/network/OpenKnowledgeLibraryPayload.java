package com.revilo.gatesofavarice.network;

import com.revilo.gatesofavarice.GatewayExpansion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenKnowledgeLibraryPayload() implements CustomPacketPayload {
    public static final OpenKnowledgeLibraryPayload INSTANCE = new OpenKnowledgeLibraryPayload();
    public static final Type<OpenKnowledgeLibraryPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "open_knowledge_library"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenKnowledgeLibraryPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    @Override public Type<OpenKnowledgeLibraryPayload> type() { return TYPE; }
}
