package com.revilo.gatesofavarice.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.revilo.gatesofavarice.knowledge.KnowledgeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class KnowledgeCommands {
    private static final DynamicCommandExceptionType UNKNOWN_ENTRY = new DynamicCommandExceptionType(name -> Component.literal("Unknown knowledge entry: " + name));

    private KnowledgeCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("knowledge")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("unlock")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> unlock(context.getSource(), StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("reset")
                        .executes(context -> reset(context.getSource()))));
    }

    private static int unlock(CommandSourceStack source, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        KnowledgeManager.KnowledgeEntry entry = KnowledgeManager.findEntry(name)
                .orElseThrow(() -> UNKNOWN_ENTRY.create(name));
        if (KnowledgeManager.unlock(player, entry)) {
            source.sendSuccess(() -> Component.literal("Unlocked " + entry.title() + "."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("You already know " + entry.title() + "."), false);
        return 0;
    }

    private static int reset(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        KnowledgeManager.reset(source.getPlayerOrException());
        source.sendSuccess(() -> Component.literal("Knowledge reset."), false);
        return 1;
    }
}
