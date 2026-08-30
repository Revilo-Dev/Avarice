package com.revilo.gatesofavarice.client;

import com.revilo.gatesofavarice.client.screen.KnowledgeLibraryScreen;
import com.revilo.gatesofavarice.network.KnowledgeLibraryPayload;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;

/** Client mirror used by the HUD notification and personal-library screen. */
public final class KnowledgeLibraryClientState {
    private static Set<String> unlocked = Set.of();
    private static Set<String> unread = Set.of();
    private KnowledgeLibraryClientState() { }

    public static void apply(KnowledgeLibraryPayload payload) {
        unlocked = Set.copyOf(payload.unlocked());
        unread = Set.copyOf(payload.unread());
        if (payload.openScreen()) Minecraft.getInstance().setScreen(new KnowledgeLibraryScreen());
    }
    public static boolean hasUnread() { return !unread.isEmpty(); }
    public static boolean isUnlocked(String id) { return unlocked.contains(id); }
}
