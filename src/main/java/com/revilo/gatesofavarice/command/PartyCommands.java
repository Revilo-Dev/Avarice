package com.revilo.gatesofavarice.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.revilo.gatesofavarice.party.PartyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class PartyCommands {
    private PartyCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("party")
                .executes(context -> status(context.getSource().getPlayerOrException()))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(context -> create(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name"))))
                .then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(context -> create(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("invite").then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> invite(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("accept").executes(context -> accept(context.getSource().getPlayerOrException())))
                .then(Commands.literal("decline").executes(context -> decline(context.getSource().getPlayerOrException())))
                .then(Commands.literal("leave").executes(context -> leave(context.getSource().getPlayerOrException())))
                .then(Commands.literal("disband").executes(context -> disband(context.getSource().getPlayerOrException()))));
    }

    private static int create(ServerPlayer player, String name) { return result(player, PartyManager.create(player, name), "Created party " + name + ".", "You are already in a party."); }
    private static int invite(ServerPlayer player, ServerPlayer target) { return result(player, PartyManager.invite(player, target), "Invited " + target.getName().getString() + ".", "Unable to invite that player."); }
    private static int accept(ServerPlayer player) { return result(player, PartyManager.accept(player), "Joined the party.", "You have no valid party invite."); }
    private static int decline(ServerPlayer player) { return result(player, PartyManager.decline(player), "Invite declined.", "You have no party invite."); }
    private static int leave(ServerPlayer player) { return result(player, PartyManager.leave(player), "Left the party.", "Party leaders must use /party disband."); }
    private static int disband(ServerPlayer player) { return result(player, PartyManager.disband(player), "Party disbanded.", "Only the party leader can disband it."); }
    private static int status(ServerPlayer player) { player.sendSystemMessage(PartyManager.status(player)); return 1; }
    private static int result(ServerPlayer player, boolean success, String message, String error) {
        player.sendSystemMessage(Component.literal(success ? message : error).withStyle(success ? ChatFormatting.GREEN : ChatFormatting.RED));
        return success ? 1 : 0;
    }
}
