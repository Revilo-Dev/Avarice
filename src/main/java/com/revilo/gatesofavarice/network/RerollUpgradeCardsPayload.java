package com.revilo.gatesofavarice.network;

import com.revilo.gatesofavarice.GatewayExpansion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RerollUpgradeCardsPayload(String sessionId) implements CustomPacketPayload {
    public static final Type<RerollUpgradeCardsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "reroll_upgrade_cards"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RerollUpgradeCardsPayload> STREAM_CODEC =
            StreamCodec.of(RerollUpgradeCardsPayload::write, RerollUpgradeCardsPayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, RerollUpgradeCardsPayload payload) {
        buffer.writeUtf(payload.sessionId);
    }

    private static RerollUpgradeCardsPayload read(RegistryFriendlyByteBuf buffer) {
        return new RerollUpgradeCardsPayload(buffer.readUtf());
    }

    @Override
    public Type<RerollUpgradeCardsPayload> type() {
        return TYPE;
    }
}
