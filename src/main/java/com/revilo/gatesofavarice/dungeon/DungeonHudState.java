package com.revilo.gatesofavarice.dungeon;

import com.revilo.gatesofavarice.network.DungeonWaveHudPayload;
import com.revilo.gatesofavarice.dungeon.DungeonDeck.CardState;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class DungeonHudState {

    private static volatile boolean active;
    private static volatile boolean inRun;
    private static volatile boolean upgradePhase;
    private static volatile boolean gatewayOpen;
    private static volatile int floorNumber;
    private static volatile int waveInFloor;
    private static volatile int mobsRemaining;
    private static volatile int totalMobs;
    private static volatile int nextWaveCountdownTicks;
    private static volatile long playTimeTicks;
    private static volatile long playTimeReceivedAtMillis;
    private static volatile int mobsKilled;
    private static volatile ResourceLocation ammoItem = ResourceLocation.withDefaultNamespace("air");
    private static volatile int ammoCount;
    private static volatile List<CardState> deck = DungeonDeck.empty();
    private static volatile String partyName = "";
    private static volatile List<String> partyMembers = List.of();

    private DungeonHudState() {
    }

    public static void apply(DungeonWaveHudPayload payload) {
        active = payload.active();
        inRun = payload.inRun();
        upgradePhase = payload.upgradePhase();
        gatewayOpen = payload.gatewayOpen();
        floorNumber = payload.floorNumber();
        waveInFloor = payload.waveInFloor();
        mobsRemaining = payload.mobsRemaining();
        totalMobs = payload.totalMobs();
        nextWaveCountdownTicks = payload.nextWaveCountdownTicks();
        playTimeTicks = payload.playTimeTicks();
        playTimeReceivedAtMillis = System.currentTimeMillis();
        mobsKilled = payload.mobsKilled();
        ammoItem = payload.ammoItem();
        ammoCount = payload.ammoCount();
        deck = DungeonDeck.normalize(payload.deck());
        partyName = payload.partyName();
        partyMembers = List.copyOf(payload.partyMembers());
    }

    public static void clear() {
        active = false;
        inRun = false;
        upgradePhase = false;
        gatewayOpen = false;
        floorNumber = 0;
        waveInFloor = 0;
        mobsRemaining = 0;
        totalMobs = 0;
        nextWaveCountdownTicks = 0;
        playTimeTicks = 0L;
        playTimeReceivedAtMillis = 0L;
        mobsKilled = 0;
        ammoItem = ResourceLocation.withDefaultNamespace("air");
        ammoCount = 0;
        deck = DungeonDeck.empty();
        partyName = "";
        partyMembers = List.of();
    }

    public static boolean active() {
        return active;
    }

    public static boolean inRun() {
        return inRun;
    }

    public static boolean upgradePhase() {
        return upgradePhase;
    }

    public static boolean gatewayOpen() { return gatewayOpen; }

    public static int floorNumber() {
        return floorNumber;
    }

    public static int waveInFloor() {
        return waveInFloor;
    }

    public static int mobsRemaining() {
        return mobsRemaining;
    }

    public static int totalMobs() {
        return totalMobs;
    }

    public static int nextWaveCountdownTicks() {
        return nextWaveCountdownTicks;
    }

    public static boolean hasRunStats() {
        return inRun || playTimeTicks > 0L || mobsKilled > 0;
    }

    public static long playTimeTicks() {
        if (!hasRunStats() || playTimeReceivedAtMillis <= 0L) {
            return playTimeTicks;
        }
        long elapsedMillis = Math.max(0L, System.currentTimeMillis() - playTimeReceivedAtMillis);
        return playTimeTicks + elapsedMillis / 50L;
    }

    public static int mobsKilled() {
        return mobsKilled;
    }

    public static ResourceLocation ammoItem() { return ammoItem; }

    public static int ammoCount() { return ammoCount; }

    public static List<CardState> deck() {
        return deck;
    }

    public static String partyName() { return partyName; }

    public static List<String> partyMembers() { return partyMembers; }
}
