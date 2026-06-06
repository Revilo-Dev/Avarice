package com.revilo.gatesofavarice.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutPresetRegistry;
import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import com.revilo.gatesofavarice.dungeon.DungeonUpgradeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class DungeonCommands {

    private DungeonCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dungeon")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("recover")
                        .executes(context -> recover(context.getSource())))
                .then(Commands.literal("loadout")
                        .then(Commands.literal("list")
                                .executes(context -> listLoadouts(context.getSource())))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .executes(context -> grantLoadout(context.getSource(), StringArgumentType.getString(context, "id"))))))
                .then(Commands.literal("upgrade")
                        .then(Commands.literal("open")
                                .executes(context -> openUpgrade(context.getSource())))));

        dispatcher.register(Commands.literal("avarice")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("dungeon")
                        .then(Commands.literal("end")
                                .executes(context -> endDungeon(context.getSource()))))
                .then(Commands.literal("best_wave")
                        .then(Commands.literal("reset")
                                .executes(context -> resetBestWave(context.getSource())))));
    }

    private static int recover(CommandSourceStack source) {
        int recovered = DungeonRunManager.recoverStalledShops(source.getServer());
        source.sendSuccess(() -> Component.literal("Recovered dungeon runs: " + recovered), true);
        return recovered;
    }

    private static int listLoadouts(CommandSourceStack source) {
        String joined = String.join(", ", LoadoutPresetRegistry.all().stream().map(def -> def.id()).toList());
        source.sendSuccess(() -> Component.literal("Loadouts: " + joined), false);
        return 1;
    }

    private static int grantLoadout(CommandSourceStack source, String id) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        boolean exists = LoadoutPresetRegistry.byId(id).isPresent();
        if (!exists) {
            source.sendFailure(Component.literal("Unknown loadout id: " + id));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Loadout preset exists: " + id + ". Start a dungeon wave to roll/select it."), false);
        return 1;
    }

    private static int openUpgrade(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        boolean ok = DungeonUpgradeManager.openUpgradeScreen(player);
        if (!ok) {
            source.sendFailure(Component.literal("Failed to open upgrade screen"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Opened upgrade screen"), false);
        return 1;
    }

    private static int resetBestWave(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        DungeonRunManager.resetBestWave(player);
        source.sendSuccess(() -> Component.literal("Reset best wave for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int endDungeon(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        if (!DungeonRunManager.forceEndRun(player)) {
            source.sendFailure(Component.literal("No active dungeon run to end"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Ended active dungeon run for " + player.getGameProfile().getName()), true);
        return 1;
    }
}
