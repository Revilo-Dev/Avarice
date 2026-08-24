package com.revilo.gatesofavarice.network;

import com.revilo.gatesofavarice.GatewayExpansion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DungeonWaveHudPayload(boolean active, boolean upgradePhase, int floorNumber, int waveInFloor, int mobsRemaining, int totalMobs, int nextWaveCountdownTicks, long playTimeTicks, int mobsKilled, List<String> statLines) implements CustomPacketPayload {

    public static final Type<DungeonWaveHudPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "dungeon_wave_hud"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DungeonWaveHudPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeBoolean(payload.active);
                        buffer.writeBoolean(payload.upgradePhase);
                        buffer.writeVarInt(payload.floorNumber);
                        buffer.writeVarInt(payload.waveInFloor);
                        buffer.writeVarInt(payload.mobsRemaining);
                        buffer.writeVarInt(payload.totalMobs);
                        buffer.writeVarInt(payload.nextWaveCountdownTicks);
                        buffer.writeVarLong(payload.playTimeTicks);
                        buffer.writeVarInt(payload.mobsKilled);
                        buffer.writeVarInt(payload.statLines.size());
                        for (String statLine : payload.statLines) {
                            buffer.writeUtf(statLine);
                        }
                    },
                    buffer -> {
                        boolean active = buffer.readBoolean();
                        boolean upgradePhase = buffer.readBoolean();
                        int floorNumber = buffer.readVarInt();
                        int waveInFloor = buffer.readVarInt();
                        int mobsRemaining = buffer.readVarInt();
                        int totalMobs = buffer.readVarInt();
                        int nextWaveCountdownTicks = buffer.readVarInt();
                        long playTimeTicks = buffer.readVarLong();
                        int mobsKilled = buffer.readVarInt();
                        int statCount = buffer.readVarInt();
                        ArrayList<String> statLines = new ArrayList<>(statCount);
                        for (int i = 0; i < statCount; i++) {
                            statLines.add(buffer.readUtf());
                        }
                        return new DungeonWaveHudPayload(active, upgradePhase, floorNumber, waveInFloor, mobsRemaining, totalMobs, nextWaveCountdownTicks, playTimeTicks, mobsKilled, List.copyOf(statLines));
                    }
            );

    @Override
    public Type<DungeonWaveHudPayload> type() {
        return TYPE;
    }
}
