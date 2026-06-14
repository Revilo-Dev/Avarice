package com.revilo.gatesofavarice.dungeon;

import com.revilo.gatesofavarice.network.DungeonWaveHudPayload;
import java.util.List;

public final class DungeonHudState {

    private static volatile boolean active;
    private static volatile int waveNumber;
    private static volatile int mobsRemaining;
    private static volatile int totalMobs;
    private static volatile long playTimeTicks;
    private static volatile long playTimeReceivedAtMillis;
    private static volatile int mobsKilled;
    private static volatile List<String> statLines = List.of();

    private DungeonHudState() {
    }

    public static void apply(DungeonWaveHudPayload payload) {
        active = payload.active();
        waveNumber = payload.waveNumber();
        mobsRemaining = payload.mobsRemaining();
        totalMobs = payload.totalMobs();
        playTimeTicks = payload.playTimeTicks();
        playTimeReceivedAtMillis = System.currentTimeMillis();
        mobsKilled = payload.mobsKilled();
        statLines = List.copyOf(payload.statLines());
    }

    public static void clear() {
        active = false;
        waveNumber = 0;
        mobsRemaining = 0;
        totalMobs = 0;
        playTimeTicks = 0L;
        playTimeReceivedAtMillis = 0L;
        mobsKilled = 0;
        statLines = List.of();
    }

    public static boolean active() {
        return active;
    }

    public static int waveNumber() {
        return waveNumber;
    }

    public static int mobsRemaining() {
        return mobsRemaining;
    }

    public static int totalMobs() {
        return totalMobs;
    }

    public static boolean hasRunStats() {
        return playTimeTicks > 0L || mobsKilled > 0 || !statLines.isEmpty();
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

    public static List<String> statLines() {
        return statLines;
    }
}
