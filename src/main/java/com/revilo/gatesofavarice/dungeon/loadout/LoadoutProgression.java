package com.revilo.gatesofavarice.dungeon.loadout;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class LoadoutProgression {
    private static final String ROOT_KEY = "gatesofavarice.loadout_progression";

    private LoadoutProgression() {}

    public static Stats get(ServerPlayer player, String loadoutId) {
        CompoundTag tag = entry(player, loadoutId);
        long xp = Math.max(0L, tag.getLong("xp"));
        return new Stats(levelForXp(xp), xp, Math.max(0, tag.getInt("plays")), Math.max(0, tag.getInt("kills")), Math.max(0L, tag.getLong("play_time")));
    }

    public static void recordPlay(ServerPlayer player, String loadoutId) {
        mutate(player, loadoutId, tag -> tag.putInt("plays", tag.getInt("plays") + 1));
    }

    public static void addKill(ServerPlayer player, String loadoutId) {
        mutate(player, loadoutId, tag -> tag.putInt("kills", tag.getInt("kills") + 1));
    }

    public static void addPlayTime(ServerPlayer player, String loadoutId, long ticks) {
        if (ticks > 0L) mutate(player, loadoutId, tag -> tag.putLong("play_time", tag.getLong("play_time") + ticks));
    }

    public static void addXp(ServerPlayer player, String loadoutId, long amount) {
        if (amount > 0L) mutate(player, loadoutId, tag -> tag.putLong("xp", tag.getLong("xp") + amount));
    }

    public static long xpForNextLevel(int level) {
        return 100L * (Math.max(0, level) + 1L) * (Math.max(0, level) + 1L);
    }

    private static int levelForXp(long xp) {
        int level = 0;
        while (level < 100 && xp >= xpForNextLevel(level)) level++;
        return level;
    }

    private static CompoundTag entry(ServerPlayer player, String loadoutId) {
        return player.getPersistentData().getCompound(ROOT_KEY).getCompound(loadoutId);
    }

    private static void mutate(ServerPlayer player, String loadoutId, java.util.function.Consumer<CompoundTag> mutation) {
        if (loadoutId == null || loadoutId.isBlank()) return;
        CompoundTag persistent = player.getPersistentData();
        CompoundTag root = persistent.getCompound(ROOT_KEY);
        CompoundTag tag = root.getCompound(loadoutId);
        mutation.accept(tag);
        root.put(loadoutId, tag);
        persistent.put(ROOT_KEY, root);
    }

    public record Stats(int level, long xp, int plays, int kills, long playTimeTicks) {}
}
