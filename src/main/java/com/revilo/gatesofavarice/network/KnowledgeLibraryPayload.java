package com.revilo.gatesofavarice.network;

import com.revilo.gatesofavarice.GatewayExpansion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KnowledgeLibraryPayload(boolean openScreen, List<String> unlocked, List<String> unread) implements CustomPacketPayload {
    public static final Type<KnowledgeLibraryPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "knowledge_library"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KnowledgeLibraryPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBoolean(payload.openScreen);
                writeStrings(buffer, payload.unlocked);
                writeStrings(buffer, payload.unread);
            },
            buffer -> new KnowledgeLibraryPayload(buffer.readBoolean(), readStrings(buffer), readStrings(buffer))
    );
    private static void writeStrings(RegistryFriendlyByteBuf buffer, List<String> values) {
        buffer.writeVarInt(values.size());
        for (String value : values) buffer.writeUtf(value);
    }
    private static List<String> readStrings(RegistryFriendlyByteBuf buffer) {
        int size = Math.min(256, Math.max(0, buffer.readVarInt()));
        ArrayList<String> values = new ArrayList<>(size);
        for (int index = 0; index < size; index++) values.add(buffer.readUtf());
        return List.copyOf(values);
    }
    @Override public Type<KnowledgeLibraryPayload> type() { return TYPE; }
}
