package com.revilo.gatesofavarice.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.revilo.gatesofavarice.block.entity.GatewayWorkbenchBlockEntity;
import com.revilo.gatesofavarice.currency.GoldCoinWallet;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutPresetRegistry;
import com.revilo.gatesofavarice.dungeon.loadout.RunicLoadoutService;
import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import com.revilo.gatesofavarice.dungeon.DungeonUpgradeManager;
import com.revilo.gatesofavarice.item.data.CrystalForgeData;
import com.revilo.gatesofavarice.item.data.GatewayCardData;
import com.revilo.gatesofavarice.registry.ModItems;
import com.revilo.gatesofavarice.workbench.GatewayWorkbenchForgeLogic;
import com.revilo.gatesofavarice.workbench.GatewayWorkbenchSlots;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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

        dispatcher.register(avariceRoot("avarice"));
        dispatcher.register(avariceRoot("avrice"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> avariceRoot(String name) {
        return Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("dungeon")
                        .then(Commands.literal("end")
                                .executes(context -> endDungeon(context.getSource()))))
                .then(Commands.literal("best_wave")
                        .then(Commands.literal("reset")
                                .executes(context -> resetBestWave(context.getSource()))))
                .then(Commands.literal("test")
                        .then(Commands.literal("cards")
                                .then(Commands.literal("give")
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                        .executes(context -> testCardsGive(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "type"),
                                                                IntegerArgumentType.getInteger(context, "count")))))))
                        .then(Commands.literal("crystal")
                                .then(Commands.literal("forgeable")
                                        .executes(context -> testCrystalForgeable(context.getSource())))
                                .then(Commands.literal("cards")
                                        .then(Commands.literal("clear")
                                                .executes(context -> testCrystalCardsClear(context.getSource())))
                                        .then(Commands.literal("fill")
                                                .then(Commands.argument("type", StringArgumentType.word())
                                                        .executes(context -> testCrystalCardsFill(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "type")))))))
                        .then(Commands.literal("gui")
                                .then(Commands.literal("survived")
                                        .executes(context -> testGui(context.getSource(), true)))
                                .then(Commands.literal("died")
                                        .executes(context -> testGui(context.getSource(), false))))
                        .then(Commands.literal("wave")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("wave", IntegerArgumentType.integer(0))
                                                .executes(context -> testWaveSet(context.getSource(), IntegerArgumentType.getInteger(context, "wave")))))
                                .then(Commands.literal("clear")
                                        .executes(context -> testWaveClear(context.getSource())))
                                .then(Commands.literal("spawn_remaining")
                                        .then(Commands.argument("count", IntegerArgumentType.integer(0))
                                                .executes(context -> testWaveSpawnRemaining(context.getSource(), IntegerArgumentType.getInteger(context, "count"))))))
                        .then(Commands.literal("rune")
                                .then(Commands.literal("slots")
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(context -> testRuneSlotsSet(context.getSource(), IntegerArgumentType.getInteger(context, "amount")))))))
                        .then(Commands.literal("magnet")
                                .then(Commands.literal("upgrade")
                                        .then(Commands.argument("count", IntegerArgumentType.integer(0))
                                                .executes(context -> testMagnetUpgrade(context.getSource(), IntegerArgumentType.getInteger(context, "count"))))))
                        .then(Commands.literal("rewards")
                                .then(Commands.literal("preview")
                                        .then(Commands.argument("wave", IntegerArgumentType.integer(0))
                                                .executes(context -> testRewardsPreview(context.getSource(), IntegerArgumentType.getInteger(context, "wave"))))))
                        .then(Commands.literal("shop")
                                .then(Commands.literal("restock")
                                        .executes(context -> testShopRestock(context.getSource()))))
                        .then(Commands.literal("teleport")
                                .then(Commands.literal("dungeon")
                                        .executes(context -> testTeleportDungeon(context.getSource()))))
                        .then(Commands.literal("cleanup")
                                .then(Commands.literal("dungeon_items")
                                        .executes(context -> testCleanupDungeonItems(context.getSource()))))
                        .then(Commands.literal("shop_phase")
                                .executes(context -> testShopPhase(context.getSource())))
                        .then(Commands.literal("bail")
                                .then(Commands.literal("level")
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                                .executes(context -> testBailLevel(context.getSource(), IntegerArgumentType.getInteger(context, "level"))))))
                        .then(Commands.literal("spawn_mob")
                                .then(Commands.argument("pool", StringArgumentType.word())
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                .executes(context -> testSpawnMob(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "pool"),
                                                        IntegerArgumentType.getInteger(context, "count"))))))
                        .then(Commands.literal("loot")
                                .then(Commands.literal("wave")
                                        .then(Commands.argument("wave", IntegerArgumentType.integer(1))
                                                .then(Commands.literal("rolls")
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                                .executes(context -> testLootRolls(
                                                                        context.getSource(),
                                                                        IntegerArgumentType.getInteger(context, "wave"),
                                                                        IntegerArgumentType.getInteger(context, "count"))))))))
                        .then(Commands.literal("loadout")
                                .then(Commands.literal("open")
                                        .executes(context -> testLoadoutOpen(context.getSource()))))
                        .then(Commands.literal("modifiers")
                                .then(Commands.literal("list")
                                        .executes(context -> testModifiersList(context.getSource()))))
                        .then(Commands.literal("coins")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(context -> testCoinsSet(context.getSource(), IntegerArgumentType.getInteger(context, "amount"))))))
                        .then(Commands.literal("droprates")
                                .executes(context -> testDropRates(context.getSource())))
                        .then(Commands.literal("difficulty-scale")
                                .executes(context -> testDifficultyScale(context.getSource()))));
    }

    private static int recover(CommandSourceStack source) {
        int recovered = DungeonRunManager.recoverStalledShops(source.getServer());
        source.sendSuccess(() -> Component.literal("Recovered dungeon runs: " + recovered), true);
        return recovered;
    }

    private static int testDifficultyScale(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        for (Component line : DungeonRunManager.buildDifficultyScaleReport(player)) {
            source.sendSuccess(() -> line, false);
        }
        return 1;
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

    private static ServerPlayer requirePlayer(CommandSourceStack source) {
        return source.getPlayer();
    }

    private static int testGui(CommandSourceStack source, boolean survived) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        DungeonRunManager.debugSendCompletionScreen(player, survived);
        source.sendSuccess(() -> Component.literal("Opened test " + (survived ? "survived" : "died") + " screen"), false);
        return 1;
    }

    private static int testCardsGive(CommandSourceStack source, String typeName, int count) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        GatewayCardData.CardType type = parseCardType(typeName);
        if (type == null) {
            source.sendFailure(Component.literal("Unknown card type: " + typeName + ". Valid: stat, damage, effect, ability, rarity, challenge"));
            return 0;
        }
        int given = 0;
        for (int i = 0; i < Math.min(count, 256); i++) {
            ItemStack card = GatewayCardData.create(ModItems.GATEWAY_CARD.get(), type, Math.max(1, player.experienceLevel), player.getRandom());
            if (!player.getInventory().add(card)) {
                player.drop(card, false);
            }
            given++;
        }
        int totalGiven = given;
        source.sendSuccess(() -> Component.literal("Gave " + totalGiven + " " + type.name().toLowerCase(java.util.Locale.ROOT) + " gateway cards"), true);
        return given;
    }

    private static int testCrystalForgeable(CommandSourceStack source) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        GatewayWorkbenchBlockEntity workbench = lookedAtWorkbench(player);
        if (workbench == null) {
            source.sendFailure(Component.literal("Look at a Gateway Workbench within 8 blocks"));
            return 0;
        }
        boolean forgeable = GatewayWorkbenchForgeLogic.canForge(player, workbench);
        source.sendSuccess(() -> Component.literal("Crystal forgeable: " + forgeable), false);
        return forgeable ? 1 : 0;
    }

    private static int testCrystalCardsClear(CommandSourceStack source) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        GatewayWorkbenchBlockEntity workbench = lookedAtWorkbench(player);
        if (workbench == null) {
            source.sendFailure(Component.literal("Look at a Gateway Workbench within 8 blocks"));
            return 0;
        }
        ItemStack crystal = workbench.getItem(GatewayWorkbenchSlots.CRYSTAL_SLOT);
        if (crystal.isEmpty()) {
            source.sendFailure(Component.literal("Gateway Workbench has no crystal"));
            return 0;
        }
        CrystalForgeData.clearCards(crystal);
        workbench.setChanged();
        player.containerMenu.broadcastChanges();
        source.sendSuccess(() -> Component.literal("Cleared crystal cards"), true);
        return 1;
    }

    private static int testCrystalCardsFill(CommandSourceStack source, String typeName) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        GatewayCardData.CardType type = parseCardType(typeName);
        if (type == null) {
            source.sendFailure(Component.literal("Unknown card type: " + typeName + ". Valid: stat, damage, effect, ability, rarity, challenge"));
            return 0;
        }
        GatewayWorkbenchBlockEntity workbench = lookedAtWorkbench(player);
        if (workbench == null) {
            source.sendFailure(Component.literal("Look at a Gateway Workbench within 8 blocks"));
            return 0;
        }
        ItemStack crystal = workbench.getItem(GatewayWorkbenchSlots.CRYSTAL_SLOT);
        if (crystal.isEmpty()) {
            source.sendFailure(Component.literal("Gateway Workbench has no crystal"));
            return 0;
        }
        int filled = CrystalForgeData.fillCards(crystal, type, Math.max(1, player.experienceLevel));
        workbench.setChanged();
        player.containerMenu.broadcastChanges();
        source.sendSuccess(() -> Component.literal("Filled crystal with " + filled + " " + type.displayName() + " modifiers"), true);
        return filled;
    }

    private static int testWaveSet(CommandSourceStack source, int wave) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        if (!DungeonRunManager.debugSetWave(player, wave)) {
            source.sendFailure(Component.literal("No active dungeon run"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Set active dungeon wave to " + wave), true);
        return 1;
    }

    private static int testWaveClear(CommandSourceStack source) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        if (!DungeonRunManager.debugClearWave(player)) {
            source.sendFailure(Component.literal("No active dungeon run to clear"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Cleared active dungeon wave"), true);
        return 1;
    }

    private static int testWaveSpawnRemaining(CommandSourceStack source, int count) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        int remaining = DungeonRunManager.debugSetSpawnRemaining(player, count);
        if (remaining < 0) {
            source.sendFailure(Component.literal("No active dungeon run"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Set wave spawn remaining to " + remaining), true);
        return remaining;
    }

    private static int testRuneSlotsSet(CommandSourceStack source, int amount) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        if (amount <= 0) {
            source.sendFailure(Component.literal("Rune slot amount must be greater than 0"));
            return 0;
        }
        ItemStack target = heldItem(player);
        if (target.isEmpty()) {
            source.sendFailure(Component.literal("Hold the item to edit in either hand"));
            return 0;
        }
        RunicLoadoutService.applyRuneSlotCapacity(target, amount);
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
        source.sendSuccess(() -> Component.literal("Set rune slots on held item to " + amount), true);
        return amount;
    }

    private static int testMagnetUpgrade(CommandSourceStack source, int count) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        int applied = DungeonRunManager.debugUpgradeMagnet(player, count);
        source.sendSuccess(() -> Component.literal("Applied " + applied + " dungeon magnet upgrades"), true);
        return applied;
    }

    private static int testRewardsPreview(CommandSourceStack source, int wave) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        List<Component> lines = DungeonRunManager.debugRewardPreviewLines(player, wave);
        for (Component line : lines) {
            source.sendSuccess(() -> line, false);
        }
        return lines.size();
    }

    private static int testShopRestock(CommandSourceStack source) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        if (!DungeonRunManager.debugRestockShop(player)) {
            source.sendFailure(Component.literal("No active dungeon run"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Restocked dungeon shopkeeper"), true);
        return 1;
    }

    private static int testTeleportDungeon(CommandSourceStack source) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        if (!DungeonRunManager.debugTeleportDungeon(player)) {
            source.sendFailure(Component.literal("Failed to teleport to dungeon"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Teleported to dungeon"), true);
        return 1;
    }

    private static int testCleanupDungeonItems(CommandSourceStack source) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        int removed = DungeonRunManager.debugCleanupDungeonItems(player);
        if (removed < 0) {
            source.sendFailure(Component.literal("Dungeon dimension is not available"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Removed " + removed + " dungeon item entities"), true);
        return removed;
    }

    private static int testShopPhase(CommandSourceStack source) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        if (!DungeonRunManager.debugForceShopPhase(player)) {
            source.sendFailure(Component.literal("No active dungeon run"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Forced active dungeon into shop phase"), true);
        return 1;
    }

    private static int testBailLevel(CommandSourceStack source, int level) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        if (!DungeonRunManager.debugBailAtLevel(player, level)) {
            source.sendFailure(Component.literal("No active dungeon run to bail from"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Completed dungeon as if bailed at level " + level), true);
        return 1;
    }

    private static int testDropRates(CommandSourceStack source) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        List<Component> lines = DungeonRunManager.debugDropRateLines(player);
        for (Component line : lines) {
            source.sendSuccess(() -> line, false);
        }
        return lines.size();
    }

    private static int testSpawnMob(CommandSourceStack source, String pool, int count) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        int spawned = DungeonRunManager.debugSpawnMobs(player, pool, Math.min(count, 100));
        if (spawned == -1) {
            source.sendFailure(Component.literal("No active dungeon run"));
            return 0;
        }
        if (spawned == -2) {
            source.sendFailure(Component.literal("Unknown pool: " + pool + ". Valid: random, undead, horde, assassin, archer, tank, nether"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Spawned " + spawned + " mobs from " + pool + " pool"), true);
        return spawned;
    }

    private static int testLootRolls(CommandSourceStack source, int wave, int count) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        List<Component> lines = DungeonRunManager.debugRollLootLines(player, wave, Math.min(count, 10000));
        for (Component line : lines) {
            source.sendSuccess(() -> line, false);
        }
        return lines.size();
    }

    private static int testLoadoutOpen(CommandSourceStack source) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        if (!DungeonRunManager.debugOpenLoadout(player)) {
            source.sendFailure(Component.literal("No active dungeon run"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Opened loadout selection"), false);
        return 1;
    }

    private static int testModifiersList(CommandSourceStack source) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        List<Component> lines = DungeonRunManager.debugModifierLines(player);
        for (Component line : lines) {
            source.sendSuccess(() -> line, false);
        }
        return lines.size();
    }

    private static int testCoinsSet(CommandSourceStack source, int amount) {
        ServerPlayer player = requirePlayer(source);
        if (player == null) {
            source.sendFailure(Component.literal("Player required"));
            return 0;
        }
        GoldCoinWallet.set(player, amount);
        source.sendSuccess(() -> Component.literal("Set Gold Coins to " + amount), true);
        return amount;
    }

    private static GatewayCardData.CardType parseCardType(String typeName) {
        try {
            return GatewayCardData.CardType.valueOf(typeName.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static ItemStack heldItem(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        return main.isEmpty() ? player.getOffhandItem() : main;
    }

    private static GatewayWorkbenchBlockEntity lookedAtWorkbench(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(8.0D));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        return player.level().getBlockEntity(hit.getBlockPos()) instanceof GatewayWorkbenchBlockEntity workbench ? workbench : null;
    }
}
