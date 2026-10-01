package com.revilo.gatesofavarice.client;

import com.revilo.gatesofavarice.client.screen.KnowledgeLibraryScreen;
import com.revilo.gatesofavarice.network.KnowledgeLibraryPayload;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.client.Minecraft;

/** Client mirror used by the HUD notification and personal-library screen. */
public final class KnowledgeLibraryClientState {
    private static Set<String> unlocked = Set.of();
    private static Set<String> unread = Set.of();
    private static Map<String, Integer> collectedDays = Map.of();
    private KnowledgeLibraryClientState() { }

    public static void apply(KnowledgeLibraryPayload payload) {
        unlocked = Set.copyOf(payload.unlocked());
        unread = Set.copyOf(payload.unread());
        HashMap<String, Integer> days = new HashMap<>();
        for (String value : payload.collectedDays()) {
            int separator = value.lastIndexOf('=');
            if (separator <= 0) continue;
            try { days.put(value.substring(0, separator), Integer.parseInt(value.substring(separator + 1))); }
            catch (NumberFormatException ignored) { }
        }
        collectedDays = Map.copyOf(days);
        if (payload.openScreen()) Minecraft.getInstance().setScreen(new KnowledgeLibraryScreen());
    }
    public static boolean hasUnread() { return !unread.isEmpty(); }
    public static boolean isUnlocked(String id) { return unlocked.contains(id); }
    public static int collectedDay(String id) { return collectedDays.getOrDefault(id, 0); }
}
