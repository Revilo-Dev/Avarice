package com.revilo.gatesofavarice.network;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.dungeon.DungeonDeck;
import com.revilo.gatesofavarice.dungeon.DungeonDeck.CardState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DungeonWaveHudPayload(boolean active, boolean inRun, boolean upgradePhase, boolean gatewayOpen, int floorNumber, int waveInFloor, int mobsRemaining, int totalMobs, int nextWaveCountdownTicks, long playTimeTicks, int mobsKilled, ResourceLocation ammoItem, int ammoCount, List<CardState> deck, String partyName, List<String> partyMembers) implements CustomPacketPayload {

    public static final Type<DungeonWaveHudPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "dungeon_wave_hud"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DungeonWaveHudPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeBoolean(payload.active);
                        buffer.writeBoolean(payload.inRun);
                        buffer.writeBoolean(payload.upgradePhase);
                        buffer.writeBoolean(payload.gatewayOpen);
                        buffer.writeVarInt(payload.floorNumber);
                        buffer.writeVarInt(payload.waveInFloor);
                        buffer.writeVarInt(payload.mobsRemaining);
                        buffer.writeVarInt(payload.totalMobs);
                        buffer.writeVarInt(payload.nextWaveCountdownTicks);
                        buffer.writeVarLong(payload.playTimeTicks);
                        buffer.writeVarInt(payload.mobsKilled);
                        buffer.writeResourceLocation(payload.ammoItem);
                        buffer.writeVarInt(payload.ammoCount);
                        List<CardState> deck = DungeonDeck.normalize(payload.deck);
                        buffer.writeVarInt(deck.size());
                        for (CardState card : deck) {
                            buffer.writeVarInt(card.count());
                            buffer.writeDouble(card.appliedPercent());
                        }
                        buffer.writeUtf(payload.partyName);
                        buffer.writeVarInt(payload.partyMembers.size());
                        for (String member : payload.partyMembers) buffer.writeUtf(member);
                    },
                    buffer -> {
                        boolean active = buffer.readBoolean();
                        boolean inRun = buffer.readBoolean();
                        boolean upgradePhase = buffer.readBoolean();
                        boolean gatewayOpen = buffer.readBoolean();
                        int floorNumber = buffer.readVarInt();
                        int waveInFloor = buffer.readVarInt();
                        int mobsRemaining = buffer.readVarInt();
                        int totalMobs = buffer.readVarInt();
                        int nextWaveCountdownTicks = buffer.readVarInt();
                        long playTimeTicks = buffer.readVarLong();
                        int mobsKilled = buffer.readVarInt();
                        ResourceLocation ammoItem = buffer.readResourceLocation();
                        int ammoCount = buffer.readVarInt();
                        int deckSize = buffer.readVarInt();
                        int cardTypeCount = DungeonDeck.CardType.values().length;
                        ArrayList<CardState> deck = new ArrayList<>(Math.min(deckSize, cardTypeCount));
                        for (int index = 0; index < deckSize; index++) {
                            CardState card = new CardState(buffer.readVarInt(), buffer.readDouble());
                            if (index < cardTypeCount) {
                                deck.add(card);
                            }
                        }
                        String partyName = buffer.readUtf();
                        int partyCount = buffer.readVarInt();
                        ArrayList<String> partyMembers = new ArrayList<>(partyCount);
                        for (int i = 0; i < partyCount; i++) partyMembers.add(buffer.readUtf());
                        return new DungeonWaveHudPayload(active, inRun, upgradePhase, gatewayOpen, floorNumber, waveInFloor, mobsRemaining, totalMobs, nextWaveCountdownTicks, playTimeTicks, mobsKilled, ammoItem, ammoCount, DungeonDeck.normalize(deck), partyName, List.copyOf(partyMembers));
                    }
            );

    public DungeonWaveHudPayload {
        deck = DungeonDeck.normalize(deck);
        partyName = partyName == null ? "" : partyName;
        partyMembers = partyMembers == null ? List.of() : List.copyOf(partyMembers);
    }

    @Override
    public Type<DungeonWaveHudPayload> type() {
        return TYPE;
    }
}
