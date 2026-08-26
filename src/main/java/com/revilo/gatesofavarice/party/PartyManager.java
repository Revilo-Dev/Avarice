package com.revilo.gatesofavarice.party;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import com.revilo.gatesofavarice.dungeon.ModDimensions;
import com.revilo.gatesofavarice.dungeon.DungeonRunManager;

/** Lightweight server-side parties. Dungeon ownership is always the party leader. */
public final class PartyManager {
    private static final Map<UUID, Party> PARTIES_BY_LEADER = new HashMap<>();
    private static final Map<UUID, UUID> MEMBER_TO_LEADER = new HashMap<>();
    private static final Map<UUID, UUID> INVITES = new HashMap<>();

    private PartyManager() {}

    public static boolean create(ServerPlayer leader, String name) {
        if (MEMBER_TO_LEADER.containsKey(leader.getUUID()) || name.isBlank()) return false;
        Party party = new Party(name.trim(), leader.getUUID());
        PARTIES_BY_LEADER.put(leader.getUUID(), party);
        MEMBER_TO_LEADER.put(leader.getUUID(), leader.getUUID());
        DungeonRunManager.refreshPartyHud(leader);
        return true;
    }

    public static boolean invite(ServerPlayer leader, ServerPlayer target) {
        Party party = partyOf(leader.getUUID());
        if (party == null || !party.leader.equals(leader.getUUID()) || MEMBER_TO_LEADER.containsKey(target.getUUID())) return false;
        INVITES.put(target.getUUID(), leader.getUUID());
        target.sendSystemMessage(Component.literal(leader.getName().getString() + " invited you to party " + party.name + ". ")
                .append(Component.literal("[ACCEPT]").withStyle(style -> style.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Join " + party.name))))));
        return true;
    }

    public static boolean accept(ServerPlayer player) {
        UUID leaderId = INVITES.remove(player.getUUID());
        Party party = leaderId == null ? null : PARTIES_BY_LEADER.get(leaderId);
        if (party == null || MEMBER_TO_LEADER.containsKey(player.getUUID())) return false;
        party.members.add(player.getUUID());
        MEMBER_TO_LEADER.put(player.getUUID(), leaderId);
        broadcast(player.server, party, Component.literal(player.getName().getString() + " joined the party.").withStyle(ChatFormatting.GREEN));
        refreshHud(player.server, party);
        return true;
    }

    public static boolean decline(ServerPlayer player) { return INVITES.remove(player.getUUID()) != null; }

    public static boolean leave(ServerPlayer player) {
        Party party = partyOf(player.getUUID());
        if (party == null || party.leader.equals(player.getUUID())) return false;
        party.members.remove(player.getUUID());
        MEMBER_TO_LEADER.remove(player.getUUID());
        broadcast(player.server, party, Component.literal(player.getName().getString() + " left the party.").withStyle(ChatFormatting.YELLOW));
        DungeonRunManager.refreshPartyHud(player);
        refreshHud(player.server, party);
        return true;
    }

    public static boolean disband(ServerPlayer leader) {
        Party party = partyOf(leader.getUUID());
        if (party == null || !party.leader.equals(leader.getUUID())) return false;
        PARTIES_BY_LEADER.remove(leader.getUUID());
        for (UUID member : party.members) MEMBER_TO_LEADER.remove(member);
        broadcast(leader.server, party, Component.literal("Party disbanded.").withStyle(ChatFormatting.RED));
        for (UUID member : party.members) {
            ServerPlayer online = leader.server.getPlayerList().getPlayer(member);
            if (online != null) DungeonRunManager.refreshPartyHud(online);
        }
        return true;
    }

    public static UUID dungeonOwner(ServerPlayer player, UUID requestedOwner) {
        UUID leader = MEMBER_TO_LEADER.get(player.getUUID());
        return leader != null && requestedOwner != null && areInSameParty(player.getUUID(), requestedOwner)
                ? leader : player.getUUID();
    }

    /** A generated gate belongs to its owner; only that owner and their current party may enter it. */
    public static boolean canEnterDungeon(ServerPlayer player, UUID ownerId) {
        return player.getUUID().equals(ownerId) || areInSameParty(player.getUUID(), ownerId);
    }

    public static boolean isInParty(ServerPlayer player) {
        return MEMBER_TO_LEADER.containsKey(player.getUUID());
    }

    /** Party membership is checked against the actual gate owner, not merely the leader. */
    public static boolean areInSameParty(UUID firstPlayerId, UUID secondPlayerId) {
        UUID firstLeader = MEMBER_TO_LEADER.get(firstPlayerId);
        return firstLeader != null && firstLeader.equals(MEMBER_TO_LEADER.get(secondPlayerId));
    }

    public static int partySize(UUID ownerId) {
        Party party = PARTIES_BY_LEADER.get(ownerId);
        return party == null ? 1 : party.members.size();
    }

    public static Component status(ServerPlayer player) {
        Party party = partyOf(player.getUUID());
        if (party == null) return Component.literal("You are not in a party.").withStyle(ChatFormatting.GRAY);
        String members = party.members.stream().map(id -> {
            ServerPlayer online = player.server.getPlayerList().getPlayer(id);
            return online == null ? "Offline" : online.getName().getString();
        }).reduce((a, b) -> a + ", " + b).orElse("None");
        return Component.literal("Party " + party.name + ": " + members).withStyle(ChatFormatting.AQUA);
    }

    /** Compact, client-safe information for the dungeon party tab. */
    public static PartyHudData hudData(ServerPlayer viewer) {
        Party party = partyOf(viewer.getUUID());
        if (party == null) return new PartyHudData("", java.util.List.of());
        return new PartyHudData(party.name, party.members.stream().sorted(java.util.Comparator.comparing(UUID::toString))
                .map(id -> memberHudLine(viewer, id)).toList());
    }

    private static String memberHudLine(ServerPlayer viewer, UUID id) {
        ServerPlayer member = viewer.server.getPlayerList().getPlayer(id);
        if (member == null) return "Offline|OFFLINE|0";
        String state = member.level().dimension() == ModDimensions.DUNGEON_LEVEL ? "IN DUNGEON" : "OUTSIDE";
        return member.getName().getString() + "|" + state + "|" + Math.max(0, (int) Math.ceil(member.getHealth()));
    }

    public record PartyHudData(String name, java.util.List<String> members) {}

    private static Party partyOf(UUID member) {
        UUID leader = MEMBER_TO_LEADER.get(member);
        return leader == null ? null : PARTIES_BY_LEADER.get(leader);
    }

    private static void broadcast(net.minecraft.server.MinecraftServer server, Party party, Component message) {
        for (UUID member : party.members) {
            ServerPlayer online = server.getPlayerList().getPlayer(member);
            if (online != null) online.sendSystemMessage(message);
        }
    }

    private static void refreshHud(net.minecraft.server.MinecraftServer server, Party party) {
        for (UUID member : party.members) {
            ServerPlayer online = server.getPlayerList().getPlayer(member);
            if (online != null) DungeonRunManager.refreshPartyHud(online);
        }
    }

    private static final class Party {
        private final String name;
        private final UUID leader;
        private final Set<UUID> members = new HashSet<>();
        private Party(String name, UUID leader) { this.name = name; this.leader = leader; this.members.add(leader); }
    }
}
