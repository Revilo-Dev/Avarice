package com.revilo.gatesofavarice.dungeon;

import com.revilo.gatesofavarice.currency.MythicCoinWallet;
import com.revilo.gatesofavarice.currency.GoldCoinWallet;
import com.revilo.gatesofavarice.dungeon.DungeonUpgradeManager;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCategory;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutPresetRegistry;
import com.revilo.gatesofavarice.dungeon.loadout.AuraAttributeSupport;
import com.revilo.gatesofavarice.dungeon.loadout.RunicLoadoutService;
import com.revilo.gatesofavarice.entity.GatekeeperEntity;
import com.revilo.gatesofavarice.entity.GatewayCrystalEntity;
import com.revilo.gatesofavarice.gateway.pool.EnemyPoolRegistry;
import com.revilo.gatesofavarice.gateway.pool.EnemyPoolRole;
import com.revilo.gatesofavarice.gateway.pool.EnemyPoolSet;
import com.revilo.gatesofavarice.integration.LevelUpIntegration;
import com.revilo.gatesofavarice.entity.MythicCoinOrbEntity;
import com.revilo.gatesofavarice.item.MythicCoinStackData;
import com.revilo.gatesofavarice.progression.ProgressionSystem;
import com.revilo.gatesofavarice.item.data.CrystalTheme;
import com.revilo.gatesofavarice.item.data.GatewayCardData;
import com.revilo.gatesofavarice.menu.DungeonWaveMenu;
import com.revilo.gatesofavarice.network.DungeonCompletePayload;
import com.revilo.gatesofavarice.network.DungeonWaveHudPayload;
import com.revilo.gatesofavarice.registry.ModEntities;
import com.revilo.gatesofavarice.registry.ModItems;
import com.revilo.gatesofavarice.registry.ModAttachments;
import com.revilo.gatesofavarice.shop.ShopkeeperManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.attachment.AttachmentSync;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DungeonRunManager {
    public static final String DUNGEON_WAVE_SPAWN_KEY = "gatesofavarice.dungeon_wave_spawn";
    private static final Map<UUID, RunState> RUNS_BY_OWNER = new HashMap<>();
    private static final Map<UUID, UUID> PLAYER_TO_OWNER = new HashMap<>();
    private static final Map<UUID, PendingSnapshotRestore> PENDING_SNAPSHOT_RESTORES = new HashMap<>();

    private static final ResourceLocation MOB_HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "dungeon_wave_health");
    private static final ResourceLocation MOB_DAMAGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "dungeon_wave_damage");
    private static final ResourceLocation MOB_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("gatesofavarice", "dungeon_wave_speed");
    private static final int BASE_REROLL_COST = 1000;
    private static final double BASE_ELITE_CHANCE = 0.03D;
    private static final float DUNGEON_MAGNET_DROP_CHANCE = 0.04F;
    private static final int LEVEL_POINTS_PER_LOOT_PICKUP = 1;
    private static final int AUTOSAVE_INTERVAL_TICKS = 20 * 30;
    private static final String RUNS_KEY = "runs";
    private static final String PENDING_RESTORES_KEY = "pending_restores";
    private static final String DUNGEON_SHOPKEEPER_OWNER_KEY = "gatesofavarice.dungeon_shopkeeper_owner";

    private static boolean persistedStateLoaded = false;
    private static boolean persistedStateDirty = false;
    private static long lastAutosaveTick = Long.MIN_VALUE;

    private static final List<Item> REGULAR_DROP_POOL = List.of(
            ModItems.GRIMSTONE.get(), ModItems.MYSTIC_ESSENCE.get(), ModItems.SCRAP_METAL.get(), ModItems.MANA_GEMS.get(),
            ModItems.MANA_STEEL_SCRAP.get(), ModItems.MAGNETITE_SCRAP.get(), ModItems.ARCANE_ESSENCE.get(), ModItems.MANASTONES.get(),
            ModItems.ELIXRITE_SCRAP.get(), ModItems.ASTRITE_SCRAP.get(), ModItems.SOLAR_SHARD.get(), ModItems.DARK_ESSENCE.get(),
            ModItems.RUSTY_COIN.get(), ModItems.HARDENED_FLESH.get(), ModItems.SHATTERED_BONES.get(), ModItems.HEART_FRAGMENT.get(),
            ModItems.PLASMA.get(), ModItems.PETRIFIED_SOUL_SHARD.get(), ModItems.RUBY.get(), ModItems.SAPHIRE.get(), ModItems.OPAL.get()
    );
    private static final List<Item> COMMON_DROP_POOL = List.of(
            ModItems.GRIMSTONE.get(), ModItems.MYSTIC_ESSENCE.get(), ModItems.SCRAP_METAL.get(), ModItems.MANA_GEMS.get(),
            ModItems.MAGNETITE_SCRAP.get(), ModItems.ARCANE_ESSENCE.get(), ModItems.MANASTONES.get(), ModItems.RUSTY_COIN.get(), ModItems.HARDENED_FLESH.get(),
            ModItems.SHATTERED_BONES.get()
    );
    private static final List<Item> UNCOMMON_DROP_POOL = List.of(
            ModItems.MANA_STEEL_SCRAP.get(), ModItems.ELIXRITE_SCRAP.get(), ModItems.ASTRITE_SCRAP.get(), ModItems.SOLAR_SHARD.get(),
            ModItems.HEART_FRAGMENT.get(), ModItems.PLASMA.get()
    );
    private static final List<Item> RARE_DROP_POOL = List.of(
            ModItems.DARK_ESSENCE.get(), ModItems.PETRIFIED_SOUL_SHARD.get(), ModItems.RUBY.get(), ModItems.SAPHIRE.get()
    );
    private static final List<Item> EPIC_DROP_POOL = List.of(
            ModItems.PRISMATIC_SHARD.get(), ModItems.OPAL.get()
    );
    private static final List<Component> TAROT_ENEMY_LINES = List.of(
            Component.literal("+2 Hoard Mobs"), Component.literal("+3 Hoard Mobs"), Component.literal("+4 Hoard Mobs"),
            Component.literal("+2 Assassin Mobs"), Component.literal("+3 Archer Mobs"), Component.literal("+2 Tank Mobs"));
    private static final List<Component> TAROT_EFFECT_LINES = List.of(
            Component.literal("+8% Mob Damage"), Component.literal("+10% Mob Health"), Component.literal("+12% Mob Speed"),
            Component.literal("+15% Mob Resistance"), Component.literal("+8% Elite Chance"), Component.literal("+6% Spawn Rate"));
    private static final List<Component> TAROT_REWARD_LINES = List.of(
            Component.literal("+1 Reward Roll"), Component.literal("+2 Reward Rolls"), Component.literal("+20 Mythic Coins"),
            Component.literal("+1 Unlock Archetype"), Component.literal("+1 Dungeon Loot Burst"));
    private static final List<WeightedItem> WEAPON_POOL = buildWeaponPool();
    private static final List<LoadoutModels.LoadoutDefinition> LOADOUT_DEFINITIONS = LoadoutPresetRegistry.all();

    private DungeonRunManager() {}

    private static void ensureLoaded(MinecraftServer server) {
        if (server == null || persistedStateLoaded) {
            return;
        }
        RUNS_BY_OWNER.clear();
        PLAYER_TO_OWNER.clear();
        PENDING_SNAPSHOT_RESTORES.clear();
        ServerLevel overworld = server.overworld();
        if (overworld != null) {
            loadPersistedState(DungeonRunStorage.get(overworld).state(), server, overworld.registryAccess());
        }
        persistedStateLoaded = true;
        persistedStateDirty = false;
        lastAutosaveTick = Long.MIN_VALUE;
    }

    private static void markStateDirty() {
        persistedStateDirty = true;
    }

    private static void savePersistedState(MinecraftServer server) {
        if (server == null) {
            return;
        }
        ensureLoaded(server);
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        DungeonRunStorage.get(overworld).setState(buildPersistedState(overworld));
        persistedStateDirty = false;
    }

    public static void enterFromGateway(ServerPlayer player, UUID ownerId) {
        ensureLoaded(player.server);
        RunState run = RUNS_BY_OWNER.computeIfAbsent(ownerId, RunState::new);
        run.server = player.server;
        if (run.runStartGameTime < 0L) {
            run.runStartGameTime = player.serverLevel().getGameTime();
        }
        run.participants.add(player.getUUID());
        PLAYER_TO_OWNER.put(player.getUUID(), ownerId);
        run.snapshots.putIfAbsent(player.getUUID(), PlayerSnapshot.capture(player));
        MythicCoinWallet.set(player, 0);
        clearForDungeon(player);
        DungeonInstanceManager.teleportToDungeonInstance(player, ownerId);
        if (player.getUUID().equals(ownerId) && run.phase == RunPhase.SELECTING_TAROT) {
            rollTarotOptions(run, player.serverLevel().random);
            openWaveMenu(player, run);
        }
        markStateDirty();
        forceCriticalSave(player.server);
    }

    public static void exitViaBailPortal(ServerPlayer player, UUID ownerId, GatewayCrystalEntity portal) {
        ensureLoaded(player.server);
        RunState run = RUNS_BY_OWNER.get(ownerId);
        if (run == null || run.exitPortalId != portal.getId()) return;
        PlayerSnapshot snapshot = run.snapshots.get(player.getUUID());
        if (snapshot == null) return;
        clearHudToPlayer(player);
        int levelPoints = awardDungeonExitProgression(player, run);
        List<ItemStack> rewards = collectDungeonRewards(player);
        ItemStack lootbox = createLootboxFromRewards(player, run, rewards, levelPoints);
        restoreSnapshot(player, snapshot);
        if (player.getUUID().equals(run.ownerId)) {
            closeOverworldEntryPortals(player.server, run.ownerId);
        }
        if (!lootbox.isEmpty() && !player.getInventory().add(lootbox)) player.drop(lootbox, false);
        DungeonInstanceManager.teleportToSavedLocation(player, snapshot.dimension, snapshot.returnPos, snapshot.yaw, snapshot.pitch);
        closeNearbyEntryPortals(player.server, snapshot.dimension, snapshot.returnPos, run.ownerId);
        int cashedOutCoins = cashoutRemainingCoinsOnDungeonExit(player);
        sendCompletionScreen(player, run, rewards, levelPoints, cashedOutCoins, true);
        PLAYER_TO_OWNER.remove(player.getUUID());
        run.participants.remove(player.getUUID());
        run.snapshots.remove(player.getUUID());
        if (run.participants.isEmpty()) {
            finishAndCleanup(run);
        } else {
            markStateDirty();
            forceCriticalSave(player.server);
        }
    }

    public static boolean handleWaveMenuClick(Player player, UUID ownerId, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        RunState run = RUNS_BY_OWNER.get(ownerId);
        if (run == null || !serverPlayer.getUUID().equals(run.ownerId)) return false;
        if (run.phase != RunPhase.SELECTING_TAROT && run.phase != RunPhase.SELECTING_LOOT) return false;

        if (run.phase == RunPhase.SELECTING_TAROT) {
            if (buttonId == DungeonWaveMenu.BAIL_BUTTON_ID) {
                if (run.waveNumber <= 0) {
                    return false;
                }
                spawnExitPortal(run, serverPlayer.serverLevel());
                run.phase = RunPhase.WAITING_EXIT;
                serverPlayer.closeContainer();
                return true;
            }
            if (buttonId < 0 || buttonId >= run.tarotOptions.size()) return false;
            applyTarot(run, run.tarotOptions.get(buttonId));
            if (run.waveNumber == 0) {
                rollLoadoutOptions(run, serverPlayer.serverLevel().random);
                run.phase = RunPhase.SELECTING_LOOT;
                openWaveMenu(serverPlayer, run);
                return true;
            }
            startWave(run);
            serverPlayer.closeContainer();
            return true;
        }

        if (buttonId == DungeonWaveMenu.REROLL_BUTTON_ID) {
            if (run.selectingLoadout) return false;
            int maxRerolls = maxUpgradeRerollsForLevel(upgradeRerollLevel(run));
            if (run.rerollsUsed >= maxRerolls) return false;
            int cost = getUpgradeRerollCostForUse(run.rerollsUsed);
            if (!MythicCoinWallet.spend(serverPlayer, cost)) return false;
            run.rerollsUsed++;
            rollLootOptions(run, serverPlayer.serverLevel().random);
            openWaveMenu(serverPlayer, run);
            return true;
        }
        if (buttonId == DungeonWaveMenu.SKIP_BUTTON_ID) {
            if (run.selectingLoadout) return false;
            startWave(run);
            serverPlayer.closeContainer();
            return true;
        }
        if (run.selectingLoadout) {
            int randomLoadoutButtonId = run.loadoutOptions.size();
            if (buttonId == randomLoadoutButtonId) {
                if (LOADOUT_DEFINITIONS.isEmpty()) return false;
                LoadoutModels.LoadoutDefinition randomDefinition = LOADOUT_DEFINITIONS.get(serverPlayer.serverLevel().random.nextInt(LOADOUT_DEFINITIONS.size()));
                grantLoadout(serverPlayer, buildLoadoutOptionFromDefinition(randomDefinition, averageParticipantLevel(run), serverPlayer.serverLevel().random), serverPlayer.serverLevel().random);
            } else {
                if (buttonId < 0 || buttonId >= run.loadoutOptions.size()) return false;
                grantLoadout(serverPlayer, run.loadoutOptions.get(buttonId), serverPlayer.serverLevel().random);
            }
        } else {
            if (buttonId < 0 || buttonId >= run.lootOptions.size()) return false;
            UpgradeCategory category = switch (buttonId) {
                case 0 -> UpgradeCategory.PRIMARY_WEAPON;
                case 1 -> UpgradeCategory.SECONDARY_WEAPON;
                case 2 -> UpgradeCategory.ARMOR;
                default -> UpgradeCategory.ITEM;
            };
            serverPlayer.closeContainer();
            DungeonUpgradeManager.openUpgradeScreen(serverPlayer, run.ownerId, category);
            return true;
        }
        startWave(run);
        serverPlayer.closeContainer();
        return true;
    }

    public static boolean isWaveMenuValid(Player player, UUID ownerId) {
        RunState run = RUNS_BY_OWNER.get(ownerId);
        return run != null && run.participants.contains(player.getUUID());
    }

    public static boolean isPlayerInActiveRun(Player player) {
        return getRunForPlayer(player) != null;
    }

    public static int getUpgradeWaveNumber(UUID ownerId) {
        RunState run = RUNS_BY_OWNER.get(ownerId);
        return run == null ? 1 : Math.max(1, run.waveNumber + 1);
    }

    public static int getShopUpgradeWaveNumber(ServerPlayer player) {
        UUID ownerId = PLAYER_TO_OWNER.get(player.getUUID());
        if (ownerId == null) {
            return 1;
        }
        RunState run = RUNS_BY_OWNER.get(ownerId);
        if (run == null) {
            return 1;
        }
        return Math.max(1, run.waveNumber + 1);
    }

    public static int getUpgradeRerollsLeft(UUID ownerId) {
        RunState run = RUNS_BY_OWNER.get(ownerId);
        return run == null ? 0 : Math.max(0, maxUpgradeRerollsForLevel(upgradeRerollLevel(run)) - run.rerollsUsed);
    }

    public static int getUpgradeRerollCost(UUID ownerId) {
        RunState run = RUNS_BY_OWNER.get(ownerId);
        if (run == null) {
            return 0;
        }
        int rerollsLeft = Math.max(0, maxUpgradeRerollsForLevel(upgradeRerollLevel(run)) - run.rerollsUsed);
        return rerollsLeft <= 0 ? 0 : getUpgradeRerollCostForUse(run.rerollsUsed);
    }

    public static boolean consumeUpgradeReroll(ServerPlayer player, UUID ownerId) {
        RunState run = RUNS_BY_OWNER.get(ownerId);
        if (run == null || run.selectingLoadout || run.phase == RunPhase.IN_WAVE || run.phase == RunPhase.WAITING_EXIT) {
            return false;
        }
        return spendUpgradeReroll(player, run);
    }

    public static boolean consumeUpgradeCardReroll(ServerPlayer player, UUID ownerId) {
        RunState run = RUNS_BY_OWNER.get(ownerId);
        if (run == null || run.selectingLoadout) {
            return false;
        }
        return spendUpgradeReroll(player, run);
    }

    private static boolean spendUpgradeReroll(ServerPlayer player, RunState run) {
        int maxRerolls = maxUpgradeRerollsForLevel(upgradeRerollLevel(run));
        if (run.rerollsUsed >= maxRerolls) {
            return false;
        }
        int cost = getUpgradeRerollCostForUse(run.rerollsUsed);
        if (!MythicCoinWallet.spend(player, cost)) {
            return false;
        }
        run.rerollsUsed++;
        return true;
    }

    public static boolean forceEndRun(ServerPlayer player) {
        UUID ownerId = PLAYER_TO_OWNER.get(player.getUUID());
        if (ownerId == null) {
            return false;
        }
        RunState run = RUNS_BY_OWNER.get(ownerId);
        if (run == null) {
            PLAYER_TO_OWNER.remove(player.getUUID());
            return false;
        }
        for (ServerPlayer participant : run.liveParticipants()) {
            participant.closeContainer();
        }
        finishAndCleanup(run);
        forceCriticalSave(player.server);
        return true;
    }

    public static ItemStack rollUpgradeMagnetReward(RandomSource random, int playerLevel) {
        return pickLeveledMagnet(random, playerLevel);
    }

    private static int maxUpgradeRerollsForLevel(int playerLevel) {
        return Math.max(1, 1 + Math.max(0, playerLevel) / 10);
    }

    private static int getUpgradeRerollCostForUse(int rerollsUsed) {
        return BASE_REROLL_COST * Math.max(1, rerollsUsed + 1);
    }

    private static int upgradeRerollLevel(RunState run) {
        if (run == null) {
            return 1;
        }
        ServerPlayer owner = run.online(run.ownerId);
        return owner != null ? Math.max(1, getEffectivePlayerLevel(owner)) : Math.max(1, averageParticipantLevel(run));
    }

    public static void rollAndBindForActiveRun(ServerPlayer player, ItemStack stack, RandomSource random) {
        RunState run = getRunForPlayer(player);
        int playerLevel = getEffectivePlayerLevel(player);
        long timeInDungeonTicks = 0L;
        if (run != null && run.runStartGameTime >= 0L) {
            timeInDungeonTicks = Math.max(0L, player.serverLevel().getGameTime() - run.runStartGameTime);
        }
        DungeonGearRoller.rollAndBind(stack, random, playerLevel, timeInDungeonTicks, player.registryAccess());
    }

    public static int recoverStalledShops(MinecraftServer server) {
        int recovered = 0;
        for (RunState run : RUNS_BY_OWNER.values()) {
            if (run.phase != RunPhase.SHOP) {
                continue;
            }
            ServerLevel dungeon = getDungeonLevel(run);
            if (dungeon == null) {
                continue;
            }
            Entity shop = run.shopkeeperId >= 0 ? dungeon.getEntity(run.shopkeeperId) : null;
            if (shop != null) {
                continue;
            }
            if (ensureShopkeeper(run, dungeon)) {
                recovered++;
            }
        }
        return recovered;
    }
    public static boolean canOwnerStartNextWaveFromShop(ServerPlayer player, int shopkeeperEntityId) {
        UUID ownerId = PLAYER_TO_OWNER.get(player.getUUID());
        if (ownerId == null) return false;
        RunState run = RUNS_BY_OWNER.get(ownerId);
        return run != null
                && player.getUUID().equals(run.ownerId)
                && isRunShopkeeperInteraction(run, shopkeeperEntityId)
                && (run.phase == RunPhase.SHOP || run.phase == RunPhase.SELECTING_TAROT || run.phase == RunPhase.SELECTING_LOOT);
    }
    public static boolean startNextWaveFromShop(ServerPlayer player, int shopkeeperEntityId) {
        if (!canOwnerStartNextWaveFromShop(player, shopkeeperEntityId)) return false;
        UUID ownerId = PLAYER_TO_OWNER.get(player.getUUID());
        if (ownerId == null) return false;
        RunState run = RUNS_BY_OWNER.get(ownerId);
        if (run == null) return false;
        if (run.phase == RunPhase.SELECTING_TAROT || run.phase == RunPhase.SELECTING_LOOT) {
            for (ServerPlayer participant : run.liveParticipants()) {
                participant.closeContainer();
            }
            openWaveMenu(player, run);
            markStateDirty();
            forceCriticalSave(player.server);
            return true;
        }
        run.rerollsUsed = 0;
        for (ServerPlayer participant : run.liveParticipants()) {
            participant.closeContainer();
        }
        run.phase = RunPhase.SELECTING_TAROT;
        rollTarotOptions(run, player.serverLevel().random);
        openWaveMenu(player, run);
        markStateDirty();
        forceCriticalSave(player.server);
        return true;
    }

    public static boolean tryResumePendingWaveMenu(ServerPlayer player, int shopkeeperEntityId) {
        UUID ownerId = PLAYER_TO_OWNER.get(player.getUUID());
        if (ownerId == null) {
            return false;
        }
        RunState run = RUNS_BY_OWNER.get(ownerId);
        if (run == null || !player.getUUID().equals(run.ownerId) || !isRunShopkeeperInteraction(run, shopkeeperEntityId)) {
            return false;
        }
        if (run.phase != RunPhase.SELECTING_TAROT && run.phase != RunPhase.SELECTING_LOOT) {
            return false;
        }
        openWaveMenu(player, run);
        return true;
    }
    public static boolean handleLoadoutMenuClick(Player player, UUID ownerId, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (!isLoadoutMenuValid(serverPlayer, ownerId) || buttonId < 0 || buttonId > 2) {
            return false;
        }
        applyLoadout(serverPlayer, buttonId);
        serverPlayer.closeContainer();
        return true;
    }

    public static boolean isLoadoutMenuValid(Player player, UUID ownerId) {
        RunState run = RUNS_BY_OWNER.get(ownerId);
        return run != null && run.ownerId.equals(ownerId) && run.participants.contains(player.getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ensureLoaded(event.getServer());
        if (RUNS_BY_OWNER.isEmpty() && PENDING_SNAPSHOT_RESTORES.isEmpty()) {
            return;
        }
        ServerLevel dungeonLevel = event.getServer().getLevel(ModDimensions.DUNGEON_LEVEL);
        if (dungeonLevel != null) {
            dungeonLevel.setDayTime(18000L);
        }
        for (RunState run : RUNS_BY_OWNER.values()) {
            ServerLevel dungeon = dungeonLevel;
            if (dungeon == null) continue;
            DungeonInstanceManager.keepInstanceAlive(run.ownerId, dungeon);

            if (run.phase == RunPhase.SELECTING_TAROT && run.tarotOptions.isEmpty()) {
                rollTarotOptions(run, dungeon.random);
                ServerPlayer owner = run.online(run.ownerId);
                if (owner != null) openWaveMenu(owner, run);
            } else if (run.phase == RunPhase.SELECTING_LOOT) {
                boolean rebuiltOptions = false;
                if (run.selectingLoadout) {
                    if (run.loadoutOptions.isEmpty()) {
                        rollLoadoutOptions(run, dungeon.random);
                        rebuiltOptions = true;
                    }
                } else if (run.lootOptions.isEmpty()) {
                    rollLootOptions(run, dungeon.random);
                    rebuiltOptions = true;
                }
                if (rebuiltOptions) {
                    ServerPlayer owner = run.online(run.ownerId);
                    if (owner != null) openWaveMenu(owner, run);
                }
            } else if (run.phase == RunPhase.IN_WAVE) {
                if (run.currentWavePools.isEmpty()) {
                    run.currentWavePools = buildWavePools(run);
                }
                tickWave(run, dungeon);
            } else if (run.phase == RunPhase.SHOP) {
                ensureShopkeeper(run, dungeon);
            } else if (run.phase == RunPhase.WAITING_EXIT) {
                spawnExitPortal(run, dungeon);
            }
        }
        markStateDirty();
        ServerLevel overworld = event.getServer().overworld();
        maybeAutosave(event.getServer(), dungeonLevel != null ? dungeonLevel.getGameTime() : (overworld != null ? overworld.getGameTime() : 0L));
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        float damage = Math.max(0.0F, event.getNewDamage());
        if (damage <= 0.0F) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer player) {
            RunState run = getRunForPlayer(player);
            if (run != null) {
                run.damageDealt += damage;
            }
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            RunState run = getRunForPlayer(player);
            if (run != null) {
                run.damageReceived += damage;
            }
        }
        if (attacker instanceof LivingEntity mob && event.getEntity() instanceof ServerPlayer player) {
            RunState run = getRunForPlayer(player);
            if (run != null && run.aliveMobs.contains(mob.getUUID()) && run.mobLeechPercent > 0.0D) {
                mob.heal((float) (damage * run.mobLeechPercent));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ensureLoaded(player.server);
            UUID ownerId = PLAYER_TO_OWNER.remove(player.getUUID());
            if (ownerId == null) return;
            RunState run = RUNS_BY_OWNER.get(ownerId);
            if (run == null) return;
            PlayerSnapshot snapshot = run.snapshots.get(player.getUUID());
            if (snapshot != null) {
                PENDING_SNAPSHOT_RESTORES.put(player.getUUID(), new PendingSnapshotRestore(snapshot, createDeathSummary(run, player)));
            }
            run.participants.remove(player.getUUID());
            run.snapshots.remove(player.getUUID());
            if (player.getUUID().equals(run.ownerId)) {
                closeOverworldEntryPortals(player.server, run.ownerId);
            }
            clearForDungeon(player);
            clearHudToPlayer(player);
            if (run.liveParticipants().isEmpty()) {
                finishAndCleanup(run);
            } else {
                markStateDirty();
            }
            forceCriticalSave(player.server);
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity dead) || dead.level().isClientSide) return;
        RunState run = findRunByTrackedMob(dead.getUUID());
        if (run == null) return;
        trackDungeonKillProgress(run, dead);
        run.mobsKilled++;
        run.aliveMobs.remove(dead.getUUID());
        if (dead.level() instanceof ServerLevel level) spawnDungeonMobDrops(level, dead, run);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof GatekeeperEntity) {
            event.getDrops().clear();
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && PENDING_SNAPSHOT_RESTORES.containsKey(player.getUUID())) {
            event.getDrops().clear();
            return;
        }
        if (findRunByTrackedMob(event.getEntity().getUUID()) != null) {
            event.getDrops().clear();
        }
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        RunState run = getRunForPlayer(player);
        if (run == null) {
            return;
        }
        ItemStack stack = event.getItemEntity().getItem();
        if (stack.isEmpty() || stack.is(ModItems.MYTHIC_COIN.get())) {
            return;
        }
        run.levelSourcePoints.merge(player.getUUID(), LEVEL_POINTS_PER_LOOT_PICKUP, Integer::sum);
    }

    @SubscribeEvent
    public static void onDungeonBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == ModDimensions.DUNGEON_LEVEL) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDungeonBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == ModDimensions.DUNGEON_LEVEL) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ensureLoaded(player.server);
        PendingSnapshotRestore pending = PENDING_SNAPSHOT_RESTORES.remove(player.getUUID());
        if (pending == null) {
            return;
        }
        restorePlayerSnapshot(player, pending.snapshot(), true);
        if (pending.completionPayload() != null) {
            PacketDistributor.sendToPlayer(player, pending.completionPayload());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ensureLoaded(player.server);
        if (restorePendingSnapshot(player)) {
            return;
        }
        UUID ownerId = PLAYER_TO_OWNER.get(player.getUUID());
        if (ownerId == null) {
            return;
        }
        RunState run = RUNS_BY_OWNER.get(ownerId);
        if (run == null) {
            PlayerSnapshot snapshot = findSnapshotForPlayer(player.getUUID());
            if (snapshot != null) {
                restorePlayerSnapshot(player, snapshot, true);
            }
            return;
        }
        run.server = player.server;
        if (player.level().dimension() != ModDimensions.DUNGEON_LEVEL) {
            DungeonInstanceManager.teleportToDungeonInstance(player, ownerId);
        }
        syncHudToPlayer(player, run.phase == RunPhase.IN_WAVE, run.waveNumber, Math.max(0, run.toSpawn + run.aliveMobs.size()), Math.max(1, run.waveTotalMobs));
        if (player.getUUID().equals(run.ownerId) && (run.phase == RunPhase.SELECTING_TAROT || run.phase == RunPhase.SELECTING_LOOT)) {
            openWaveMenu(player, run);
        } else if (player.getUUID().equals(run.ownerId) && run.phase == RunPhase.SHOP) {
            ServerLevel dungeon = getDungeonLevel(run);
            if (dungeon != null) {
                ensureShopkeeper(run, dungeon);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ensureLoaded(player.server);
        UUID ownerId = PLAYER_TO_OWNER.get(player.getUUID());
        if (ownerId == null) return;
        RunState run = RUNS_BY_OWNER.get(ownerId);
        if (run != null) {
            run.server = player.server;
        }
        forceCriticalSave(player.server);
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level != level.getServer().overworld()) {
            return;
        }
        persistedStateLoaded = false;
        ensureLoaded(level.getServer());
    }

    @SubscribeEvent
    public static void onLevelSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level != level.getServer().overworld()) {
            return;
        }
        if (persistedStateDirty) {
            savePersistedState(level.getServer());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level != level.getServer().overworld()) {
            return;
        }
        RUNS_BY_OWNER.clear();
        PLAYER_TO_OWNER.clear();
        PENDING_SNAPSHOT_RESTORES.clear();
        persistedStateLoaded = false;
        persistedStateDirty = false;
        lastAutosaveTick = Long.MIN_VALUE;
    }

    private static void startWave(RunState run) {
        discardRunShopkeeper(run);
        run.waveNumber++;
        run.phase = RunPhase.IN_WAVE;
        run.aliveMobs.clear();
        run.spawnCooldown = 0;
        run.tarotOptions = List.of();
        run.lootOptions = List.of();
        run.loadoutOptions = List.of();
        run.selectingLoadout = false;
        int extraPlayers = Math.max(0, run.liveParticipants().size() - 1);
        int avgLevel = averageParticipantLevel(run);
        double progressionDifficulty = ProgressionSystem.dungeonDifficultyScalar(avgLevel, Math.max(1, run.waveNumber));
        int baseCount = 6 + run.waveNumber * 2 + extraPlayers * 3;
        run.toSpawn = Math.max(4, (int) Math.round(baseCount * run.enemyCountMultiplier * progressionDifficulty));
        run.waveTotalMobs = run.toSpawn;
        run.currentWavePools = buildWavePools(run);
        syncHud(run, true);
    }

    private static void tickWave(RunState run, ServerLevel level) {
        run.aliveMobs.removeIf(uuid -> {
            Entity entity = level.getEntity(uuid);
            return !(entity instanceof LivingEntity living) || !living.isAlive();
        });
        if (run.toSpawn > 0) {
            if (run.spawnCooldown > 0) run.spawnCooldown--;
            else {
                int batch = Math.min(run.toSpawn, 2 + level.random.nextInt(2));
                for (int i = 0; i < batch; i++) spawnOneMob(run, level);
                run.toSpawn -= batch;
                run.spawnCooldown = 10;
            }
        }
        if (run.toSpawn <= 0 && run.aliveMobs.isEmpty()) completeWave(run, level); else syncHud(run, true);
    }

    private static void completeWave(RunState run, ServerLevel level) {
        int avgLevel = averageParticipantLevel(run);
        int wave = Math.max(1, run.waveNumber);
        int scaledCoins = ProgressionSystem.dungeonCoinReward(avgLevel, wave, run.quantityBonusModifier, run.coinBonusModifier);
        run.coinsEarned += Math.max(5, scaledCoins);
        for (ServerPlayer player : run.liveParticipants()) {
            MythicCoinWallet.addRaw(player, Math.max(5, scaledCoins));
        }
        int lootRolls = computeWaveLootRolls(run, avgLevel, level.random);
        spawnWaveLootBurst(run, level, lootRolls);
        for (ServerPlayer player : run.liveParticipants()) {
            player.removeAllEffects();
            player.setHealth(player.getMaxHealth());
        }
        ensureShopkeeper(run, level);
        run.phase = RunPhase.SHOP;
        syncHud(run, false);
    }

    private static void spawnExitPortal(RunState run, ServerLevel level) {
        if (run.exitPortalId >= 0) return;
        GatewayCrystalEntity portal = ModEntities.GATEWAY_CRYSTAL.get().create(level);
        if (portal == null) return;
        net.minecraft.world.phys.Vec3 portalPos = DungeonInstanceManager.exitPortalPosition(run.ownerId);
        portal.setOwnerId(run.ownerId);
        portal.setReturnPortal(true);
        portal.moveTo(portalPos.x(), portalPos.y(), portalPos.z(), 0.0F, 0.0F);
        if (level.addFreshEntity(portal)) run.exitPortalId = portal.getId();
    }

    private static boolean ensureShopkeeper(RunState run, ServerLevel level) {
        GatekeeperEntity existingShopkeeper = findRunShopkeeper(run, level);
        if (existingShopkeeper != null) {
            markDungeonShopkeeper(existingShopkeeper, run.ownerId);
            run.shopkeeperId = existingShopkeeper.getId();
            return true;
        }
        net.minecraft.world.phys.Vec3 shopPos = DungeonInstanceManager.shopkeeperPosition(run.ownerId);
        ServerPlayer owner = run.online(run.ownerId);
        Player summoner = owner;
        var shop = ShopkeeperManager.spawnShopkeeper(level, shopPos.x(), shopPos.y(), shopPos.z(), summoner);
        if (shop == null) {
            run.shopkeeperId = -1;
            return false;
        }
        markDungeonShopkeeper(shop, run.ownerId);
        run.shopkeeperId = shop.getId();
        return true;
    }

    private static boolean isRunShopkeeperInteraction(RunState run, int shopkeeperEntityId) {
        ServerLevel dungeon = getDungeonLevel(run);
        if (dungeon == null) {
            return false;
        }
        Entity entity = dungeon.getEntity(shopkeeperEntityId);
        if (entity instanceof GatekeeperEntity trader && trader.isAlive() && isDungeonShopkeeperForRun(run, trader) && isWithinRunShopArea(run, trader)) {
            markDungeonShopkeeper(trader, run.ownerId);
            run.shopkeeperId = trader.getId();
            return true;
        }
        GatekeeperEntity keeper = findRunShopkeeper(run, dungeon);
        return keeper != null && keeper.getId() == shopkeeperEntityId;
    }

    private static boolean isWithinRunShopArea(RunState run, GatekeeperEntity trader) {
        net.minecraft.world.phys.Vec3 shopPos = DungeonInstanceManager.shopkeeperPosition(run.ownerId);
        return Math.abs(trader.getX() - shopPos.x()) <= 8.0D
                && Math.abs(trader.getY() - shopPos.y()) <= 6.0D
                && Math.abs(trader.getZ() - shopPos.z()) <= 8.0D;
    }

    private static GatekeeperEntity findRunShopkeeper(RunState run, ServerLevel level) {
        if (run.shopkeeperId >= 0) {
            Entity existing = level.getEntity(run.shopkeeperId);
            if (existing instanceof GatekeeperEntity trader && existing.isAlive() && isDungeonShopkeeperForRun(run, trader) && isWithinRunShopArea(run, trader)) {
                markDungeonShopkeeper(trader, run.ownerId);
                return trader;
            }
            run.shopkeeperId = -1;
        }

        net.minecraft.world.phys.Vec3 shopPos = DungeonInstanceManager.shopkeeperPosition(run.ownerId);
        AABB searchBox = new AABB(
                shopPos.x() - 8.0D,
                shopPos.y() - 4.0D,
                shopPos.z() - 8.0D,
                shopPos.x() + 8.0D,
                shopPos.y() + 6.0D,
                shopPos.z() + 8.0D);
        List<GatekeeperEntity> matches = level.getEntitiesOfClass(
                GatekeeperEntity.class,
                searchBox,
                trader -> trader.isAlive() && isDungeonShopkeeperForRun(run, trader));
        if (matches.isEmpty()) {
            return null;
        }

        GatekeeperEntity keeper = matches.getFirst();
        markDungeonShopkeeper(keeper, run.ownerId);
        for (int index = 1; index < matches.size(); index++) {
            matches.get(index).discard();
        }
        run.shopkeeperId = keeper.getId();
        return keeper;
    }

    private static void markDungeonShopkeeper(GatekeeperEntity trader, UUID ownerId) {
        trader.setInvulnerable(true);
        trader.getPersistentData().putUUID(DUNGEON_SHOPKEEPER_OWNER_KEY, ownerId);
    }

    private static boolean isDungeonShopkeeperForRun(RunState run, GatekeeperEntity trader) {
        if (!ShopkeeperManager.isShopkeeper(trader)) {
            return false;
        }
        CompoundTag data = trader.getPersistentData();
        return !data.hasUUID(DUNGEON_SHOPKEEPER_OWNER_KEY) || data.getUUID(DUNGEON_SHOPKEEPER_OWNER_KEY).equals(run.ownerId);
    }

    private static void discardRunShopkeeper(RunState run) {
        if (run.shopkeeperId < 0) {
            return;
        }
        ServerLevel dungeon = getDungeonLevel(run);
        if (dungeon != null) {
            Entity shop = dungeon.getEntity(run.shopkeeperId);
            if (shop != null) {
                shop.discard();
            }
        }
        run.shopkeeperId = -1;
    }

    private static void spawnWaveLootBurst(RunState run, ServerLevel level, int rolls) {
        BlockPos center = DungeonInstanceManager.instanceCenter(run.ownerId);
        for (int i = 0; i < rolls; i++) {
            Item item = pickScaledDrop(run, averageParticipantLevel(run), level.random);
            int stackCount = Math.max(1, 1 + level.random.nextInt(2) + Math.max(0, run.waveNumber / 5) + (int) Math.floor(run.quantityBonusModifier * 2.0D));
            ItemStack stack = new ItemStack(item, stackCount);
            ItemEntity entity = new ItemEntity(level, center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D, stack);
            entity.setDeltaMovement(level.random.nextDouble() * 0.3D - 0.15D, 0.25D, level.random.nextDouble() * 0.3D - 0.15D);
            entity.setNoPickUpDelay();
            level.addFreshEntity(entity);
        }
    }

    private static void spawnOneMob(RunState run, ServerLevel level) {
        WaveArchetype archetype = pickWeightedArchetype(run, level.random);
        EnemyPoolSet pools = run.currentWavePools.get(archetype);
        EntityType<?> type = pickEntityType(level.random, pools, archetype);
        if (type == null) return;
        Entity entity = type.create(level);
        if (!(entity instanceof LivingEntity mob)) return;
        net.minecraft.world.phys.Vec3 spawnPos = DungeonInstanceManager.randomMobSpawnPosition(run.ownerId, level.random);
        double x = spawnPos.x();
        double y = spawnPos.y();
        double z = spawnPos.z();
        mob.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
        if (mob instanceof Mob aiMob) aiMob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)), MobSpawnType.EVENT, (SpawnGroupData) null);
        mob.getPersistentData().putBoolean(DUNGEON_WAVE_SPAWN_KEY, true);
        mob.skipDropExperience();
        applyMobScaling(mob, run.healthMultiplier, run.damageMultiplier, run.speedMultiplier);
        if (rollElite(run, level.random)) {
            applyEliteVariant(mob);
        }
        if (level.addFreshEntity(mob)) run.aliveMobs.add(mob.getUUID());
    }

    private static void rollTarotOptions(RunState run, RandomSource random) {
        ArrayList<TarotOption> rolled = new ArrayList<>();
        int avgLevel = averageParticipantLevel(run);
        int displayedWave = run.waveNumber + 1;
        for (int i = 0; i < 4; i++) {
            int difficulty = rollTarotDifficulty(displayedWave, avgLevel, random);
            rolled.add(TarotOption.random(random, difficulty, displayedWave, avgLevel));
        }
        run.tarotOptions = List.copyOf(rolled);
    }

    private static void rollLootOptions(RunState run, RandomSource random) {
        run.selectingLoadout = false;
        ArrayList<LootOption> rolled = new ArrayList<>();
        rolled.add(LootOption.category("Primary Weapon Upgrade", "Upgrade your main weapon", ItemStack.EMPTY));
        rolled.add(LootOption.category("Secondary Weapon Upgrade", "Upgrade your backup weapon", ItemStack.EMPTY));
        rolled.add(LootOption.category("Armor Upgrade", "Upgrade your themed armor set", ItemStack.EMPTY));
        rolled.add(LootOption.category("Item Upgrade", "Upgrade supplies (arrows, arcane apples, etc.)", ItemStack.EMPTY));
        run.lootOptions = List.copyOf(rolled);
        run.loadoutOptions = List.of();
    }

    private static void rollLoadoutOptions(RunState run, RandomSource random) {
        run.selectingLoadout = true;
        ArrayList<LoadoutOption> rolled = new ArrayList<>();
        ArrayList<LoadoutModels.LoadoutDefinition> eligible = new ArrayList<>(LOADOUT_DEFINITIONS);
        int avgLevel = averageParticipantLevel(run);
        for (int i = 0; i < 3 && !eligible.isEmpty(); i++) {
            LoadoutModels.LoadoutDefinition definition = eligible.remove(random.nextInt(eligible.size()));
            rolled.add(buildLoadoutOptionFromDefinition(definition, avgLevel, random));
        }
        run.loadoutOptions = List.copyOf(rolled);
        run.lootOptions = List.of();
    }

    private static LoadoutOption buildLoadoutOptionFromDefinition(LoadoutModels.LoadoutDefinition definition, int avgLevel, RandomSource random) {
        ItemStack primary = new ItemStack(pickLoadoutWeapon(random, avgLevel, definition.primaryWeaponKind()));
        ItemStack secondary = new ItemStack(pickLoadoutWeapon(random, avgLevel, definition.secondaryWeaponKind()));
        return new LoadoutOption(
                definition.id(),
                Component.literal(definition.displayName() + " loadout"),
                Component.literal("Armor: " + definition.armorSet().displayName() + "\nTheme: " + definition.theme().name().toLowerCase(java.util.Locale.ROOT) + "\nRunic-enabled preset"),
                new ItemStack(Items.NETHERITE_HELMET),
                new ItemStack(Items.NETHERITE_CHESTPLATE),
                new ItemStack(Items.NETHERITE_LEGGINGS),
                new ItemStack(Items.NETHERITE_BOOTS),
                primary,
                secondary,
                pickLeveledMagnet(random, avgLevel),
                definition.supplies().stream()
                        .map(spec -> new ItemStack(spec.item(), spec.minCount() + random.nextInt(Math.max(1, spec.maxCount() - spec.minCount() + 1))))
                        .toList());
    }

    private static void applyTarot(RunState run, TarotOption option) {
        run.enemyCountMultiplier += option.enemyCountBonus;
        run.healthMultiplier += option.healthBonus;
        run.damageMultiplier += option.damageBonus;
        run.speedMultiplier += option.speedBonus;
        run.mobLeechPercent += option.mobLeechBonus;
        run.quantityBonusModifier += option.quantityBonus;
        run.rarityBonusModifier += option.rarityBonus;
        run.coinBonusModifier += option.coinBonus;
        run.levelMultiplier += option.levelBonus;
        run.eliteChanceBonus += option.eliteChanceBonus;
        run.extraRewardRolls += option.rewardRollBonus;
        run.hordeWeightBonus += option.hordeMobs;
        run.archerWeightBonus += option.archerMobs;
        run.assassinWeightBonus += option.assassinMobs;
        run.tankWeightBonus += option.tankMobs;
        run.eliteWeightBonus += option.eliteMobs;
        run.totalDifficultySelected += option.difficulty;
    }

    private static void grantLoot(ServerPlayer player, RunState run, LootOption option, RandomSource random) {}

    public static void completeWaveUpgradeSelection(ServerPlayer player, UUID ownerId) {
        RunState run = RUNS_BY_OWNER.get(ownerId);
        if (run == null || run.phase != RunPhase.SELECTING_LOOT || run.selectingLoadout) {
            return;
        }
        startWave(run);
        player.closeContainer();
    }

    private static void grantLoadout(ServerPlayer player, LoadoutOption loadout, RandomSource random) {
        clearForDungeon(player);
        int playerLevel = getEffectivePlayerLevel(player);
        long timeTicks = 0L;
        LoadoutModels.LoadoutDefinition definition = LoadoutPresetRegistry.byId(loadout.loadoutId()).orElse(null);
        ItemStack head = loadout.head().copy();
        ItemStack chest = loadout.chest().copy();
        ItemStack legs = loadout.legs().copy();
        ItemStack feet = loadout.feet().copy();
        ItemStack primary = loadout.primary().copy();
        ItemStack secondary = loadout.secondary().copy();
        ItemStack utility = loadout.utility().copy();
        DungeonGearRoller.rollAndBind(head, random, playerLevel, timeTicks, player.registryAccess());
        DungeonGearRoller.rollAndBind(chest, random, playerLevel, timeTicks, player.registryAccess());
        DungeonGearRoller.rollAndBind(legs, random, playerLevel, timeTicks, player.registryAccess());
        DungeonGearRoller.rollAndBind(feet, random, playerLevel, timeTicks, player.registryAccess());
        DungeonGearRoller.rollAndBind(primary, random, playerLevel, timeTicks, player.registryAccess());
        DungeonGearRoller.rollAndBind(secondary, random, playerLevel, timeTicks, player.registryAccess());
        DungeonGearRoller.rollAndBind(utility, random, playerLevel, timeTicks, player.registryAccess());
        if (definition != null) {
            rollArmorPiece(player, head, definition, EquipmentSlot.HEAD, random);
            rollArmorPiece(player, chest, definition, EquipmentSlot.CHEST, random);
            rollArmorPiece(player, legs, definition, EquipmentSlot.LEGS, random);
            rollArmorPiece(player, feet, definition, EquipmentSlot.FEET, random);
            RunicLoadoutService.applyLoadoutStats(player.serverLevel(), primary, definition.primaryRunicStatPool(), random);
            RunicLoadoutService.applyLoadoutStats(player.serverLevel(), secondary, definition.secondaryRunicStatPool(), random);
            RunicLoadoutService.applyLoadoutEffects(player.serverLevel(), primary, definition.weaponEffectPool(), random);
            RunicLoadoutService.applyLoadoutEffects(player.serverLevel(), secondary, definition.weaponEffectPool(), random);
        }
        DungeonBoundItems.markPrimaryWeapon(primary);
        DungeonBoundItems.markSecondaryWeapon(secondary);
        player.setItemSlot(EquipmentSlot.HEAD, head);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        player.setItemSlot(EquipmentSlot.LEGS, legs);
        player.setItemSlot(EquipmentSlot.FEET, feet);
        addRoleAware(player, primary);
        addRoleAware(player, secondary);
        player.getInventory().add(utility);
        for (ItemStack food : loadout.food()) {
            ItemStack foodCopy = food.copy();
            DungeonBoundItems.forceMarkDungeonBound(foodCopy);
            player.getInventory().add(foodCopy);
        }
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    public static void grantPrimaryWeapon(ServerPlayer player, ItemStack stack) {
        DungeonBoundItems.markPrimaryWeapon(stack);
        addRoleAware(player, stack);
    }

    private static void grantSecondaryWeapon(ServerPlayer player, ItemStack stack) {
        DungeonBoundItems.markSecondaryWeapon(stack);
        addRoleAware(player, stack);
    }

    private static void addRoleAware(ServerPlayer player, ItemStack stack) {
        if (!DungeonBoundItems.replaceRoleWeapon(player, stack) && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void rollArmorPiece(ServerPlayer player, ItemStack stack, LoadoutModels.LoadoutDefinition definition, EquipmentSlot slot, RandomSource random) {
        RunicLoadoutService.tagLoadoutIdentity(stack, definition.id(), definition.armorSet().displayName(), slot.getName());
        RunicLoadoutService.applyLoadoutStats(player.serverLevel(), stack, definition.armorRunicStatPool(), random);
        AuraAttributeSupport.applyLoadoutBonuses(stack, definition.id(), slot, definition.armorRunicStatPool());
        RunicLoadoutService.applyLoadoutEffects(player.serverLevel(), stack, definition.armorEffectPool(), random);
        if (com.revilo.gatesofavarice.config.GatewayExpansionConfig.FORCE_BINDING_ON_LOADOUT_ARMOR.get()) {
            net.minecraft.core.Holder.Reference<net.minecraft.world.item.enchantment.Enchantment> binding =
                    player.serverLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                            .get(net.minecraft.world.item.enchantment.Enchantments.BINDING_CURSE).orElse(null);
            if (binding != null) {
                net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(binding, 1));
            }
        }
        RunicLoadoutService.syncRunicSlots(stack);
    }

    private static List<LoadoutDefinition> buildLoadoutDefinitions() {
        return List.of(
                loadout("Assassin", "dagger", "dagger", "Shadow Leather Set", Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS,
                        List.of("Very high speed", "Low defence", "Small crit bonus", "Low health"), List.of(new ItemStack(Items.GOLDEN_APPLE, 2), new ItemStack(Items.COOKED_PORKCHOP, 16))),
                loadout("Knight", "longsword", "crossbow", "Steel Knight Set", Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
                        List.of("High defence", "Medium speed", "Medium health boost", "Small blocking"), List.of(new ItemStack(Items.COOKED_BEEF, 16), new ItemStack(Items.GOLDEN_CARROT, 8))),
                loadout("Berserker", "axe", "machete", "Rage Plate Set", Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
                        List.of("Medium defence", "Medium speed", "High health boost", "Low ability power"), List.of(new ItemStack(Items.COOKED_BEEF, 24), new ItemStack(Items.GOLDEN_APPLE, 1))),
                loadout("Vanguard", "hammer", "broadsword", "Fortress Set", Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
                        List.of("Very high defence", "Very low speed", "Massive health boost", "Medium shockwaves"), List.of(new ItemStack(Items.RABBIT_STEW, 6), new ItemStack(Items.COOKED_BEEF, 12))),
                loadout("Samurai", "gaundao", "dagger", "Windwalker Set", Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS,
                        List.of("High speed", "Medium defence", "Small ability power", "Medium health"), List.of(new ItemStack(ModItems.ARCANE_APPLE.get(), 2), new ItemStack(Items.COOKED_SALMON, 16), new ItemStack(Items.GOLDEN_CARROT, 12))),
                loadout("Reaper", "glaive", "dagger", "Soulbound Set", Items.CHAINMAIL_HELMET, Items.IRON_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.IRON_BOOTS,
                        List.of("Medium defence", "Medium speed", "High soul drain", "Medium ability power"), List.of(new ItemStack(ModItems.ARCANE_APPLE.get(), 3), new ItemStack(Items.BEETROOT_SOUP, 8))),
                loadout("Ranger", "bow", "machete", "Hunter Set", Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS,
                        List.of("High speed", "Low-medium defence", "Small ability power", "Low health boost"), List.of(new ItemStack(ModItems.ARCANE_APPLE.get(), 1), new ItemStack(Items.COOKED_CHICKEN, 16), new ItemStack(Items.SWEET_BERRIES, 32))),
                loadout("Marksman", "crossbow", "longsword", "Sharpshooter Set", Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS,
                        List.of("Medium defence", "Medium speed", "Medium ability power", "Small health boost"), List.of(new ItemStack(ModItems.ARCANE_APPLE.get(), 2), new ItemStack(Items.GOLDEN_CARROT, 16), new ItemStack(Items.PUMPKIN_PIE, 8))),
                loadout("Gladiator", "broadsword", "dagger", "Arena Set", Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
                        List.of("Medium-high defence", "Medium speed", "Medium blocking", "Medium health boost"), List.of(new ItemStack(Items.COOKED_BEEF, 20), new ItemStack(Items.GOLDEN_APPLE, 1))),
                loadout("Spellblade", "longsword", "glaive", "Arcane Set", Items.GOLDEN_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.CHAINMAIL_BOOTS,
                        List.of("Medium defence", "Medium speed", "High ability power", "Small health boost"), List.of(new ItemStack(ModItems.ARCANE_APPLE.get(), 4), new ItemStack(Items.GOLDEN_CARROT, 16), new ItemStack(Items.HONEY_BOTTLE, 6))),
                loadout("Warlord", "hammer", "crossbow", "Tyrant Set", Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
                        List.of("Very high defence", "Heavy shockwaves", "High health boost", "Very low speed"), List.of(new ItemStack(Items.COOKED_MUTTON, 24), new ItemStack(Items.GOLDEN_APPLE, 3))),
                loadout("Nomad", "machete", "bow", "Traveler Set", Items.LEATHER_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS,
                        List.of("Very high speed", "Low defence", "Small health boost", "Low ability power"), List.of(new ItemStack(Items.BREAD, 24), new ItemStack(Items.COOKED_COD, 12)))
        );
    }

    private static LoadoutDefinition loadout(String name, String primaryKind, String secondaryKind, String armorName, Item head, Item chest, Item legs, Item feet, List<String> traits, List<ItemStack> food) {
        return new LoadoutDefinition(name, primaryKind, secondaryKind, armorName, head, chest, legs, feet, traits, food);
    }

    private static List<WeightedItem> buildWeaponPool() {
        ArrayList<WeightedItem> pool = new ArrayList<>();
        addWeighted(pool, "gatewayexpansion:mana_steel_paxel", 3);
        addWeighted(pool, "gatewayexpansion:elixrite_paxel", 4);
        addWeighted(pool, "gatewayexpansion:astrite_paxel", 5);
        addWeighted(pool, "gatewayexpansion:lunarium_paxel", 6);
        addWeighted(pool, "gatewayexpansion:ignite_paxel", 7);
        addWeighted(pool, "gatewayexpansion:iridium_paxel", 8);
        addWeighted(pool, "gatewayexpansion:mythril_paxel", 9);
        addWeighted(pool, "gatewayexpansion:arcanium_paxel", 10);
        addWeighted(pool, "gatewayexpansion:prismatic_steel_paxel", 11);
        addWeighted(pool, "gatewayexpansion:mana_steel_sword", 7);
        addWeighted(pool, "gatewayexpansion:elixrite_sword", 7);
        addWeighted(pool, "gatewayexpansion:astrite_sword", 8);
        addWeighted(pool, "gatewayexpansion:lunarium_sword", 8);
        addWeighted(pool, "gatewayexpansion:ignite_sword", 9);
        addWeighted(pool, "gatewayexpansion:iridium_sword", 9);
        addWeighted(pool, "gatewayexpansion:mythril_sword", 10);
        addWeighted(pool, "gatewayexpansion:arcanium_sword", 10);
        addWeighted(pool, "gatewayexpansion:prismatic_steel_sword", 12);

        addWeighted(pool, "arsenal:mana_steel_broadsword", 7);
        addWeighted(pool, "arsenal:elixrite_broadsword", 7);
        addWeighted(pool, "arsenal:astrite_broadsword", 8);
        addWeighted(pool, "arsenal:lunarium_broadsword", 8);
        addWeighted(pool, "arsenal:ignite_broadsword", 9);
        addWeighted(pool, "arsenal:iridium_broadsword", 9);
        addWeighted(pool, "arsenal:mythril_broadsword", 10);
        addWeighted(pool, "arsenal:arcanium_broadsword", 10);
        addWeighted(pool, "arsenal:prismatic_steel_broadsword", 11);
        addWeighted(pool, "arsenal:mana_steel_dagger", 7);
        addWeighted(pool, "arsenal:elixrite_dagger", 7);
        addWeighted(pool, "arsenal:astrite_dagger", 8);
        addWeighted(pool, "arsenal:lunarium_dagger", 8);
        addWeighted(pool, "arsenal:ignite_dagger", 9);
        addWeighted(pool, "arsenal:iridium_dagger", 9);
        addWeighted(pool, "arsenal:mythril_dagger", 10);
        addWeighted(pool, "arsenal:arcanium_dagger", 10);
        addWeighted(pool, "arsenal:prismatic_steel_dagger", 11);
        addWeighted(pool, "arsenal:mana_steel_gaundao", 7);
        addWeighted(pool, "arsenal:elixrite_gaundao", 7);
        addWeighted(pool, "arsenal:astrite_gaundao", 8);
        addWeighted(pool, "arsenal:lunarium_gaundao", 8);
        addWeighted(pool, "arsenal:ignite_gaundao", 9);
        addWeighted(pool, "arsenal:iridium_gaundao", 9);
        addWeighted(pool, "arsenal:mythril_gaundao", 10);
        addWeighted(pool, "arsenal:arcanium_gaundao", 10);
        addWeighted(pool, "arsenal:prismatic_steel_gaundao", 11);
        addWeighted(pool, "arsenal:mana_steel_glaive", 7);
        addWeighted(pool, "arsenal:elixrite_glaive", 7);
        addWeighted(pool, "arsenal:astrite_glaive", 8);
        addWeighted(pool, "arsenal:lunarium_glaive", 8);
        addWeighted(pool, "arsenal:ignite_glaive", 9);
        addWeighted(pool, "arsenal:iridium_glaive", 9);
        addWeighted(pool, "arsenal:mythril_glaive", 10);
        addWeighted(pool, "arsenal:arcanium_glaive", 10);
        addWeighted(pool, "arsenal:prismatic_steel_glaive", 11);
        addWeighted(pool, "arsenal:mana_steel_hammer", 7);
        addWeighted(pool, "arsenal:elixrite_hammer", 7);
        addWeighted(pool, "arsenal:astrite_hammer", 8);
        addWeighted(pool, "arsenal:lunarium_hammer", 8);
        addWeighted(pool, "arsenal:ignite_hammer", 9);
        addWeighted(pool, "arsenal:iridium_hammer", 9);
        addWeighted(pool, "arsenal:mythril_hammer", 10);
        addWeighted(pool, "arsenal:arcanium_hammer", 10);
        addWeighted(pool, "arsenal:prismatic_steel_hammer", 11);
        addWeighted(pool, "arsenal:mana_steel_longsword", 7);
        addWeighted(pool, "arsenal:elixrite_longsword", 7);
        addWeighted(pool, "arsenal:astrite_longsword", 8);
        addWeighted(pool, "arsenal:lunarium_longsword", 8);
        addWeighted(pool, "arsenal:ignite_longsword", 9);
        addWeighted(pool, "arsenal:iridium_longsword", 9);
        addWeighted(pool, "arsenal:mythril_longsword", 10);
        addWeighted(pool, "arsenal:arcanium_longsword", 10);
        addWeighted(pool, "arsenal:prismatic_steel_longsword", 11);
        addWeighted(pool, "arsenal:mana_steel_machete", 7);
        addWeighted(pool, "arsenal:elixrite_machete", 7);
        addWeighted(pool, "arsenal:astrite_machete", 8);
        addWeighted(pool, "arsenal:lunarium_machete", 8);
        addWeighted(pool, "arsenal:ignite_machete", 9);
        addWeighted(pool, "arsenal:iridium_machete", 9);
        addWeighted(pool, "arsenal:mythril_machete", 10);
        addWeighted(pool, "arsenal:arcanium_machete", 10);
        addWeighted(pool, "arsenal:prismatic_steel_machete", 11);

        addWeighted(pool, "gatewayexpansion:mana_steel_magnet", 2);
        addWeighted(pool, "gatewayexpansion:elixrite_magnet", 2);
        addWeighted(pool, "gatewayexpansion:astrite_magnet", 2);
        addWeighted(pool, "gatewayexpansion:lunarium_magnet", 2);
        addWeighted(pool, "gatewayexpansion:ignite_magnet", 2);
        addWeighted(pool, "gatewayexpansion:iridium_magnet", 2);
        addWeighted(pool, "gatewayexpansion:mythril_magnet", 2);
        addWeighted(pool, "gatewayexpansion:arcanium_magnet", 2);
        addWeighted(pool, "gatewayexpansion:prismatic_steel_magnet", 3);
        return List.copyOf(pool);
    }

    private static void addWeighted(List<WeightedItem> pool, String id, int weight) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || weight <= 0) {
            return;
        }
        BuiltInRegistries.ITEM.getOptional(key).ifPresent(item -> pool.add(new WeightedItem(item, weight)));
    }

    private static Item pickWeightedWeapon(RandomSource random) {
        return pickWeightedWeapon(random, 100);
    }

    private static Item pickWeightedWeapon(RandomSource random, int playerLevel) {
        if (WEAPON_POOL.isEmpty()) {
            return Items.IRON_SWORD;
        }
        int safeLevel = Math.max(1, playerLevel);
        ArrayList<WeightedItem> eligible = new ArrayList<>();
        for (WeightedItem entry : WEAPON_POOL) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(entry.item());
            int tier = weaponTier(id);
            if (safeLevel < 10) {
                if (tier == 1) {
                    eligible.add(new WeightedItem(entry.item(), Math.max(1, entry.weight() * 10)));
                } else if (tier == 2) {
                    eligible.add(new WeightedItem(entry.item(), Math.max(1, entry.weight() / 6)));
                }
                continue;
            }
            if (tier <= 2 && safeLevel < 20) {
                eligible.add(entry);
                continue;
            }
            if (tier <= 3 && safeLevel < 35) {
                eligible.add(entry);
                continue;
            }
            if (tier <= 4 && safeLevel < 50) {
                eligible.add(entry);
                continue;
            }
            if (tier <= 5 || safeLevel >= 50) {
                eligible.add(entry);
            }
        }
        if (eligible.isEmpty()) {
            eligible.addAll(WEAPON_POOL);
        }
        int totalWeight = 0;
        for (WeightedItem entry : eligible) totalWeight += entry.weight();
        int roll = random.nextInt(totalWeight);
        int cursor = 0;
        for (WeightedItem entry : eligible) {
            cursor += entry.weight();
            if (roll < cursor) {
                return entry.item();
            }
        }
        return eligible.getLast().item();
    }

    private static Item pickLoadoutWeapon(RandomSource random, int playerLevel, String kind) {
        if ("bow".equals(kind)) {
            return Items.BOW;
        }
        if ("crossbow".equals(kind)) {
            return Items.CROSSBOW;
        }
        if ("axe".equals(kind)) {
            return pickWeaponByPath(random, playerLevel, "paxel");
        }
        return pickWeaponByPath(random, playerLevel, kind);
    }

    private static Item pickWeaponByPath(RandomSource random, int playerLevel, String pathNeedle) {
        int safeLevel = Math.max(1, playerLevel);
        ArrayList<WeightedItem> eligible = new ArrayList<>();
        for (WeightedItem entry : WEAPON_POOL) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(entry.item());
            if (id == null || !id.getPath().contains(pathNeedle)) {
                continue;
            }
            int tier = weaponTier(id);
            if ((safeLevel < 10 && tier > 2) || (safeLevel < 20 && tier > 2) || (safeLevel < 35 && tier > 3) || (safeLevel < 50 && tier > 4)) {
                continue;
            }
            int weight = Math.max(1, entry.weight());
            if (safeLevel < 10 && tier == 1) {
                weight *= 10;
            } else if (safeLevel < 10 && tier == 2) {
                weight = Math.max(1, weight / 6);
            }
            eligible.add(new WeightedItem(entry.item(), weight));
        }
        if (eligible.isEmpty()) {
            return pickWeightedWeapon(random, playerLevel);
        }
        int totalWeight = 0;
        for (WeightedItem entry : eligible) totalWeight += entry.weight();
        int roll = random.nextInt(totalWeight);
        int cursor = 0;
        for (WeightedItem entry : eligible) {
            cursor += entry.weight();
            if (roll < cursor) {
                return entry.item();
            }
        }
        return eligible.getLast().item();
    }

    private static int weaponTier(ResourceLocation id) {
        if (id == null) return 1;
        String p = id.getPath();
        if (p.contains("mana_steel")) return 1;
        if (p.contains("elixrite")) return 2;
        if (p.contains("astrite") || p.contains("lunarium")) return 3;
        if (p.contains("ignite") || p.contains("iridium")) return 4;
        return 5;
    }

    private static String shortName(Item item) {
        return new ItemStack(item).getHoverName().getString();
    }

    private static ItemStack pickLeveledMagnet(RandomSource random, int playerLevel) {
        int level = Math.max(1, playerLevel);
        Item item;
        if (level < 15) {
            item = resolveItem("gatewayexpansion:mana_steel_magnet");
        } else if (level < 30) {
            item = random.nextFloat() < 0.8F ? resolveItem("gatewayexpansion:mana_steel_magnet") : resolveItem("gatewayexpansion:elixrite_magnet");
        } else if (level < 45) {
            item = random.nextBoolean() ? resolveItem("gatewayexpansion:elixrite_magnet") : resolveItem("gatewayexpansion:astrite_magnet");
        } else if (level < 60) {
            item = random.nextBoolean() ? resolveItem("gatewayexpansion:lunarium_magnet") : resolveItem("gatewayexpansion:ignite_magnet");
        } else if (level < 75) {
            item = random.nextBoolean() ? resolveItem("gatewayexpansion:iridium_magnet") : resolveItem("gatewayexpansion:mythril_magnet");
        } else if (level < 90) {
            item = resolveItem("gatewayexpansion:arcanium_magnet");
        } else {
            item = resolveItem("gatewayexpansion:prismatic_steel_magnet");
        }
        if (item == null) {
            item = resolveItem("gatewayexpansion:mana_steel_magnet");
        }
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static Item resolveItem(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
            return null;
        }
        return BuiltInRegistries.ITEM.get(key);
    }

    private static int getEffectivePlayerLevel(ServerPlayer player) {
        return LevelUpIntegration.getEffectiveLevel(player);
    }

    private static void applyLoadout(ServerPlayer player, int loadoutId) {
        clearForDungeon(player);
        switch (loadoutId) {
            case 0 -> equipVanguard(player);
            case 1 -> equipRanger(player);
            default -> equipSpellblade(player);
        }
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    private static void equipVanguard(ServerPlayer player) {
        player.getInventory().add(new ItemStack(Items.IRON_SWORD));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        equipArmorSet(player, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
    }

    private static void equipRanger(ServerPlayer player) {
        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(player.registryAccess().holderOrThrow(Enchantments.POWER), 1);
        player.getInventory().add(bow);
        player.getInventory().add(new ItemStack(Items.ARROW, 48));
        player.getInventory().add(new ItemStack(Items.STONE_SWORD));
        equipArmorSet(player, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS);
    }

    private static void equipSpellblade(ServerPlayer player) {
        player.getInventory().add(new ItemStack(Items.TRIDENT));
        player.getInventory().add(new ItemStack(Items.IRON_AXE));
        equipArmorSet(player, Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);
    }

    private static void equipArmorSet(ServerPlayer player, Item helmet, Item chest, Item legs, Item boots) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(helmet));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(boots));
    }

    private static RunState getRunForPlayer(Player player) {
        UUID ownerId = PLAYER_TO_OWNER.get(player.getUUID());
        return ownerId == null ? null : RUNS_BY_OWNER.get(ownerId);
    }

    private static RunState findRunByTrackedMob(UUID mobId) {
        for (RunState run : RUNS_BY_OWNER.values()) if (run.aliveMobs.contains(mobId)) return run;
        return null;
    }

    private static void spawnDungeonMobDrops(ServerLevel level, LivingEntity dead, RunState run) {
        int avgLevel = averageParticipantLevel(run);
        int wave = Math.max(1, run.waveNumber);
        int baseRolls = Math.max(1, 1 + wave / 3 + (int) Math.floor(run.quantityBonusModifier));
        int rolls = Math.max(1, (int) Math.ceil(baseRolls * 0.5D));
        for (int i = 0; i < rolls; i++) {
            Item item = pickScaledDrop(run, avgLevel, level.random);
            ItemStack drop = new ItemStack(item, rollDungeonDropCount(item, wave, level.random));
            if (item == ModItems.HEART_FRAGMENT.get()) {
                DungeonBoundItems.forceMarkDungeonBound(drop);
            }
            dead.spawnAtLocation(drop);
        }
        int coinValue = 2 + wave * 2 + avgLevel / 8 + (int) Math.floor(run.quantityBonusModifier * 3.0D);
        for (int i = 0; i < 2 + wave / 5; i++) {
            MythicCoinOrbEntity.spawn((ServerLevel) dead.level(), dead.getX(), dead.getY() + 0.35D, dead.getZ(), Math.max(1, coinValue / Math.max(1, 2 + wave / 5)));
        }
        if (level.random.nextFloat() < DUNGEON_MAGNET_DROP_CHANCE) {
            ItemStack magnet = pickLeveledMagnet(level.random, avgLevel);
            if (!magnet.isEmpty()) {
                DungeonBoundItems.forceMarkDungeonBound(magnet);
                dead.spawnAtLocation(magnet);
            }
        }
    }

    private static int computeWaveLootRolls(RunState run, int avgLevel, RandomSource random) {
        int base = ProgressionSystem.dungeonLootRolls(avgLevel, Math.max(1, run.waveNumber), run.extraRewardRolls, run.quantityBonusModifier);
        int difficultyBonus = Math.max(0, run.totalDifficultySelected - Math.max(0, run.waveNumber - 1) * 2);
        return base + difficultyBonus / 4 + random.nextInt(2 + Math.max(1, Math.max(1, run.waveNumber) / 6));
    }

    private static Item pickScaledDrop(RunState run, int avgLevel, RandomSource random) {
        double rarityRoll = random.nextDouble();
        double waveFactor = Math.min(0.22D, run.waveNumber * 0.010D);
        double levelFactor = Math.min(0.06D, avgLevel / 1600.0D);
        double difficultyFactor = Math.min(0.12D, run.totalDifficultySelected * 0.007D);
        double rarityChanceBonus = Math.max(0.0D, run.rarityBonusModifier);
        double epicChance = avgLevel >= 45
                ? Math.min(0.035D, 0.002D + waveFactor * 0.18D + levelFactor * 0.30D + difficultyFactor * 0.18D + rarityChanceBonus * 0.10D)
                : 0.0D;
        double rareChance = Math.min(0.16D, 0.012D + waveFactor * 0.55D + levelFactor * 0.60D + difficultyFactor * 0.45D + rarityChanceBonus * 0.35D);
        double uncommonChance = Math.min(0.34D, 0.18D + waveFactor * 0.65D + levelFactor * 0.70D + difficultyFactor * 0.55D + rarityChanceBonus * 0.45D);
        if (rarityRoll < epicChance && !EPIC_DROP_POOL.isEmpty()) {
            return EPIC_DROP_POOL.get(random.nextInt(EPIC_DROP_POOL.size()));
        }
        if (rarityRoll < epicChance + rareChance) {
            return RARE_DROP_POOL.get(random.nextInt(RARE_DROP_POOL.size()));
        }
        if (rarityRoll < epicChance + rareChance + uncommonChance) {
            return UNCOMMON_DROP_POOL.get(random.nextInt(UNCOMMON_DROP_POOL.size()));
        }
        return COMMON_DROP_POOL.get(random.nextInt(COMMON_DROP_POOL.size()));
    }

    private static int rollDungeonDropCount(Item item, int wave, RandomSource random) {
        if (item == ModItems.PRISMATIC_SHARD.get()) {
            return 1;
        }
        if (item == ModItems.DARK_ESSENCE.get()) {
            return random.nextFloat() < 0.75F ? 1 : 2;
        }
        if (UNCOMMON_DROP_POOL.contains(item)) {
            return 1 + random.nextInt(2) + Math.max(0, wave / 20);
        }
        return 1 + random.nextInt(1 + Math.max(1, wave / 20));
    }

    private static int averageParticipantLevel(RunState run) {
        List<ServerPlayer> players = run.liveParticipants();
        if (players.isEmpty()) {
            return 1;
        }
        int total = 0;
        for (ServerPlayer player : players) {
            total += getEffectivePlayerLevel(player);
        }
        return Math.max(1, total / players.size());
    }

    private static void trackDungeonKillProgress(RunState run, LivingEntity dead) {
        // Intentionally no-op.
        // Dungeon completion progression is awarded at exit and sourced from non-kill activity only.
    }

    private static int awardDungeonExitProgression(ServerPlayer player, RunState run) {
        if (!run.awardedExitXp.add(player.getUUID())) {
            return 0;
        }
        int sourcePoints = run.levelSourcePoints.getOrDefault(player.getUUID(), 0);
        int xp = ProgressionSystem.dungeonLevelOrbReward(sourcePoints, run.levelMultiplier);
        run.experienceEarned += xp;
        return xp;
    }

    private static EntityType<?> pickEntityType(RandomSource random, EnemyPoolSet pools, WaveArchetype archetype) {
        return switch (archetype) {
            case UNDEAD, HORDE -> pools.pick(random, EnemyPoolRole.HOARD, EnemyPoolRole.TANK);
            case ASSASSIN -> pools.pick(random, EnemyPoolRole.ASSASSIN, EnemyPoolRole.HOARD);
            case ARCHER -> pools.pick(random, EnemyPoolRole.ARCHER, EnemyPoolRole.HOARD);
            case TANK -> pools.pick(random, EnemyPoolRole.TANK, EnemyPoolRole.HOARD);
            case NETHER -> pools.pick(random, EnemyPoolRole.HOARD, EnemyPoolRole.TANK);
        };
    }

    private static Map<WaveArchetype, EnemyPoolSet> buildWavePools(RunState run) {
        Map<WaveArchetype, EnemyPoolSet> pools = new HashMap<>();
        int level = Math.max(1, Math.max(10 + run.waveNumber * 2, averageParticipantLevel(run)));
        for (WaveArchetype archetype : WaveArchetype.values()) {
            CrystalTheme theme = waveTheme(run, archetype);
            pools.put(archetype, EnemyPoolRegistry.create(theme, level));
        }
        return pools;
    }

    private static WaveArchetype pickWeightedArchetype(RunState run, RandomSource random) {
        if (run.waveNumber <= 10) {
            return WaveArchetype.UNDEAD;
        }
        int averageLevel = averageParticipantLevel(run);
        boolean raidersUnlocked = averageLevel >= 16;
        boolean netherUnlocked = run.waveNumber >= 20 && averageLevel >= 50;
        int undeadWeight = 2;
        int hordeWeight = 1 + run.hordeWeightBonus;
        int assassinWeight = raidersUnlocked ? 1 + run.assassinWeightBonus : 0;
        int archerWeight = raidersUnlocked ? 1 + run.archerWeightBonus : 0;
        int tankWeight = run.waveNumber >= 12 ? 1 + run.tankWeightBonus : 0;
        int netherWeight = netherUnlocked ? 2 : 0;
        int total = undeadWeight + hordeWeight + assassinWeight + archerWeight + tankWeight + netherWeight;
        int roll = random.nextInt(Math.max(1, total));
        if ((roll -= undeadWeight) < 0) return WaveArchetype.UNDEAD;
        if ((roll -= hordeWeight) < 0) return WaveArchetype.HORDE;
        if ((roll -= assassinWeight) < 0) return WaveArchetype.ASSASSIN;
        if ((roll -= archerWeight) < 0) return WaveArchetype.ARCHER;
        if ((roll -= tankWeight) < 0) return WaveArchetype.TANK;
        return WaveArchetype.NETHER;
    }

    private static CrystalTheme waveTheme(RunState run, WaveArchetype archetype) {
        return switch (archetype) {
            case UNDEAD -> CrystalTheme.UNDEAD;
            case HORDE, TANK -> CrystalTheme.BEAST;
            case ASSASSIN, ARCHER -> CrystalTheme.RAIDER;
            case NETHER -> CrystalTheme.NETHER;
        };
    }

    private static void applyMobScaling(LivingEntity mob, double healthMultiplier, double damageMultiplier, double speedMultiplier) {
        removeMobModifiers(mob);
        addMobModifier(mob, Attributes.MAX_HEALTH, MOB_HEALTH_MODIFIER_ID, Math.max(0.0D, healthMultiplier - 1.0D));
        addMobModifier(mob, Attributes.ATTACK_DAMAGE, MOB_DAMAGE_MODIFIER_ID, Math.max(0.0D, damageMultiplier - 1.0D));
        addMobModifier(mob, Attributes.MOVEMENT_SPEED, MOB_SPEED_MODIFIER_ID, Math.max(0.0D, speedMultiplier - 1.0D));
        mob.setHealth(mob.getMaxHealth());
    }

    private static boolean rollElite(RunState run, RandomSource random) {
        if (run.waveNumber <= 1) {
            return false;
        }
        double chance = Math.max(0.0D, Math.min(0.50D, BASE_ELITE_CHANCE + run.eliteChanceBonus + run.eliteWeightBonus * 0.01D));
        return random.nextDouble() < chance;
    }

    private static void applyEliteVariant(LivingEntity mob) {
        if (mob.getAttribute(Attributes.MAX_HEALTH) != null) {
            mob.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(
                    new AttributeModifier(ResourceLocation.fromNamespaceAndPath("gatesofavarice", "elite_health_x2"), 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            mob.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(
                    new AttributeModifier(ResourceLocation.fromNamespaceAndPath("gatesofavarice", "elite_damage_x2"), 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        mob.setHealth(mob.getMaxHealth());
        MutableComponent icon = Component.literal("◆ ELITE ◆").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        mob.setCustomName(icon);
        mob.setCustomNameVisible(true);
    }

    private static void removeMobModifiers(LivingEntity mob) {
        if (mob.getAttribute(Attributes.MAX_HEALTH) != null) mob.getAttribute(Attributes.MAX_HEALTH).removeModifier(MOB_HEALTH_MODIFIER_ID);
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) mob.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(MOB_DAMAGE_MODIFIER_ID);
        if (mob.getAttribute(Attributes.MOVEMENT_SPEED) != null) mob.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(MOB_SPEED_MODIFIER_ID);
    }

    private static void addMobModifier(LivingEntity mob, Holder<Attribute> attribute, ResourceLocation id, double value) {
        if (value <= 0.0D || mob.getAttribute(attribute) == null) return;
        mob.getAttribute(attribute).addPermanentModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void openWaveMenu(ServerPlayer owner, RunState run) {
        List<DungeonWaveMenu.WaveOptionView> views;
        if (run.phase == RunPhase.SELECTING_TAROT) {
            views = run.tarotOptions.stream().map(option -> new DungeonWaveMenu.WaveOptionView(option.title, option.details, 100, 100, option.difficulty, ItemStack.EMPTY, ItemStack.EMPTY)).toList();
        } else if (run.selectingLoadout) {
            ArrayList<DungeonWaveMenu.WaveOptionView> loadoutViews = new ArrayList<>();
            for (LoadoutOption option : run.loadoutOptions) {
                loadoutViews.add(new DungeonWaveMenu.WaveOptionView(option.title, option.details, 100, 100, 0, option.primary().copy(), option.secondary().copy()));
            }
            loadoutViews.add(new DungeonWaveMenu.WaveOptionView(
                    Component.literal("Random Loadout"),
                    Component.literal("Randomly picks any loadout from the full pool"),
                    100,
                    100,
                    0,
                    ItemStack.EMPTY,
                    ItemStack.EMPTY
            ));
            views = List.copyOf(loadoutViews);
        } else {
            views = run.lootOptions.stream().map(option -> new DungeonWaveMenu.WaveOptionView(option.title, option.details, 100, 100, 0, option.stack().copy(), ItemStack.EMPTY)).toList();
        }
        int rerollsLeft = getUpgradeRerollsLeft(run.ownerId);
        int rerollCost = getUpgradeRerollCost(run.ownerId);
        int stage = run.phase == RunPhase.SELECTING_TAROT ? 0 : (run.selectingLoadout ? 2 : 1);
        List<Component> changes = runChangeSummary(run);
        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new DungeonWaveMenu(containerId, inventory, run.ownerId, run.waveNumber + 1, true, stage, rerollsLeft, rerollCost, views, changes),
                Component.translatable("screen.gatesofavarice.dungeon_wave.title", run.waveNumber + 1)
        );
        owner.openMenu(provider, buffer -> DungeonWaveMenu.writePayload(buffer, run.ownerId, run.waveNumber + 1, true, stage, rerollsLeft, rerollCost, views, changes));
    }

    private static List<Component> runChangeSummary(RunState run) {
        ArrayList<Component> changes = new ArrayList<>();
        addRunChange(changes, "spawn chance", Math.max(0.0D, (run.enemyCountMultiplier - 1.0D) * 100.0D));
        addRunChange(changes, "mob health", Math.max(0.0D, (run.healthMultiplier - 1.0D) * 100.0D));
        addRunChange(changes, "mob damage", Math.max(0.0D, (run.damageMultiplier - 1.0D) * 100.0D));
        addRunChange(changes, "mob speed", Math.max(0.0D, (run.speedMultiplier - 1.0D) * 100.0D));
        addRunChange(changes, "mob leech", run.mobLeechPercent * 100.0D);
        addRunChange(changes, "elite spawns", (run.eliteChanceBonus + run.eliteWeightBonus * 0.01D) * 100.0D);
        addRunChange(changes, "quantity", run.quantityBonusModifier * 100.0D);
        addRunChange(changes, "rarity", run.rarityBonusModifier * 100.0D);
        addRunChange(changes, "coins", run.coinBonusModifier * 100.0D);
        addRunChange(changes, "levels", Math.max(0.0D, (run.levelMultiplier - 1.0D) * 100.0D));
        return List.copyOf(changes);
    }

    private static List<String> modifiedStatSummary(RunState run) {
        ArrayList<String> changes = new ArrayList<>();
        addModifiedStat(changes, "Mob Quantity", Math.max(0.0D, (run.enemyCountMultiplier - 1.0D) * 100.0D));
        addModifiedStat(changes, "Mob Health", Math.max(0.0D, (run.healthMultiplier - 1.0D) * 100.0D));
        addModifiedStat(changes, "Mob Damage", Math.max(0.0D, (run.damageMultiplier - 1.0D) * 100.0D));
        addModifiedStat(changes, "Mob Speed", Math.max(0.0D, (run.speedMultiplier - 1.0D) * 100.0D));
        addModifiedStat(changes, "Mob Leech", run.mobLeechPercent * 100.0D);
        addModifiedStat(changes, "Elite Chance", (run.eliteChanceBonus + run.eliteWeightBonus * 0.01D) * 100.0D);
        addModifiedStat(changes, "Quantity", run.quantityBonusModifier * 100.0D);
        addModifiedStat(changes, "Rarity", run.rarityBonusModifier * 100.0D);
        addModifiedStat(changes, "Coins", run.coinBonusModifier * 100.0D);
        addModifiedStat(changes, "Levels", Math.max(0.0D, (run.levelMultiplier - 1.0D) * 100.0D));
        return List.copyOf(changes);
    }

    private static void addRunChange(List<Component> changes, String label, double value) {
        if (value <= 0.0D) {
            return;
        }
        changes.add(Component.literal(String.format(java.util.Locale.ROOT, "+%s %.1f%%", label, value)));
    }

    private static void addModifiedStat(List<String> changes, String label, double value) {
        if (value <= 0.0D) {
            return;
        }
        changes.add(String.format(java.util.Locale.ROOT, "%s +%.1f%%", label, value));
    }

    private static int rollTarotDifficulty(int displayedWave, int avgLevel, RandomSource random) {
        int levelPressure = Math.min(2, Math.max(0, (avgLevel - 70) / 20));
        int roll = random.nextInt(100);
        if (displayedWave <= 2) {
            if (levelPressure >= 2 && roll < 10) return 4;
            if (roll < 60) return 1;
            if (roll < 92) return 2;
            return 3;
        }
        if (displayedWave <= 5) {
            if (levelPressure >= 2 && roll < 8) return 4;
            if (roll < 35) return 1;
            if (roll < 78) return 2;
            if (roll < 97) return 3;
            return 4;
        }
        if (displayedWave <= 9) {
            if (levelPressure >= 2 && roll < 10) return 5;
            if (roll < 18) return 1;
            if (roll < 55) return 2;
            if (roll < 86) return 3;
            if (roll < 98) return 4;
            return 5;
        }
        if (roll < 8) return 1;
        if (roll < 30) return 2;
        if (roll < 62) return 3;
        if (roll < 88) return 4;
        return 5;
    }

    private static void syncHudToPlayer(ServerPlayer player, boolean active, int wave, int remaining, int total) {
        RunState run = getRunForPlayer(player);
        if (run == null) {
            clearHudToPlayer(player);
            return;
        }
        PacketDistributor.sendToPlayer(player, createHudPayload(run, active, wave, remaining, total));
    }

    private static void clearHudToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new DungeonWaveHudPayload(false, false, 0, 0, 1, 0L, 0, List.of()));
    }

    private static DungeonWaveHudPayload createHudPayload(RunState run, boolean active, int wave, int remaining, int total) {
        boolean upgradePhase = run != null && run.phase == RunPhase.SHOP;
        boolean displayActive = active || upgradePhase;
        int displayRemaining = upgradePhase ? 1 : remaining;
        int displayTotal = upgradePhase ? 1 : total;
        return new DungeonWaveHudPayload(displayActive, upgradePhase, wave, displayRemaining, displayTotal, elapsedRunTicks(run), run.mobsKilled, modifiedStatSummary(run));
    }

    private static long elapsedRunTicks(RunState run) {
        if (run == null || run.runStartGameTime < 0L || run.server == null) {
            return 0L;
        }
        ServerLevel dungeon = getDungeonLevel(run);
        if (dungeon == null) {
            return 0L;
        }
        return Math.max(0L, dungeon.getGameTime() - run.runStartGameTime);
    }

    private static void finishAndCleanup(RunState run) {
        syncHud(run, false);
        closeOverworldEntryPortals(run.server, run.ownerId);
        for (Map.Entry<UUID, PlayerSnapshot> entry : run.snapshots.entrySet()) {
            UUID playerId = entry.getKey();
            PLAYER_TO_OWNER.remove(playerId);
            ServerPlayer player = run.online(playerId);
            if (player != null) {
                restorePlayerSnapshot(player, entry.getValue(), true);
            } else {
                PENDING_SNAPSHOT_RESTORES.put(playerId, new PendingSnapshotRestore(entry.getValue()));
            }
        }
        ServerLevel dungeon = getDungeonLevel(run);
        if (run.shopkeeperId >= 0) {
            if (dungeon != null) {
                Entity shop = dungeon.getEntity(run.shopkeeperId);
                if (shop != null) shop.discard();
            }
        }
        if (run.exitPortalId >= 0) {
            if (dungeon != null) {
                Entity portal = dungeon.getEntity(run.exitPortalId);
                if (portal != null) portal.discard();
            }
        }
        DungeonInstanceManager.cleanupInstance(run.ownerId, dungeon);
        RUNS_BY_OWNER.remove(run.ownerId);
        markStateDirty();
    }

    private static void closeOverworldEntryPortals(MinecraftServer server, UUID ownerId) {
        if (server == null || ownerId == null) {
            return;
        }
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        for (GatewayCrystalEntity gateway : overworld.getEntitiesOfClass(
                GatewayCrystalEntity.class,
                new net.minecraft.world.phys.AABB(-30000000, -1024, -30000000, 30000000, 2048, 30000000),
                entity -> !entity.isReturnPortal() && ownerId.equals(entity.getOwnerId()))) {
            gateway.discard();
        }
    }

    private static void closeNearbyEntryPortals(MinecraftServer server, ResourceKey<Level> dimension, BlockPos center, UUID ownerId) {
        if (server == null || center == null || dimension == null) {
            return;
        }
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            return;
        }
        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(
                center.getX() - 24, center.getY() - 12, center.getZ() - 24,
                center.getX() + 24, center.getY() + 24, center.getZ() + 24);
        for (GatewayCrystalEntity gateway : level.getEntitiesOfClass(
                GatewayCrystalEntity.class,
                area,
                entity -> !entity.isReturnPortal() && (ownerId.equals(entity.getOwnerId()) || entity.getOwnerId() == null))) {
            gateway.discard();
        }
    }

    private static ServerLevel getDungeonLevel(RunState run) {
        return run.server == null ? null : run.server.getLevel(ModDimensions.DUNGEON_LEVEL);
    }

    private static void clearForDungeon(ServerPlayer player) {
        player.getInventory().clearContent();
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    private static List<ItemStack> collectDungeonRewards(ServerPlayer player) {
        ArrayList<ItemStack> rewards = new ArrayList<>();
        collectDungeonRewards(player.getInventory().items, rewards);
        collectDungeonRewards(player.getInventory().armor, rewards);
        collectDungeonRewards(player.getInventory().offhand, rewards);
        return List.copyOf(rewards);
    }

    private static void collectDungeonRewards(List<ItemStack> source, List<ItemStack> rewards) {
        for (ItemStack stack : source) {
            if (!stack.isEmpty() && !DungeonBoundItems.isDungeonBound(stack)) {
                rewards.add(stack.copy());
            }
        }
    }

    private static ItemStack createLootboxFromRewards(ServerPlayer player, RunState run, List<ItemStack> rewards, int levelOrbs) {
        ItemStack lootbox = new ItemStack(ModItems.LOOTBOX.get());
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (ItemStack stack : rewards) {
            if (!stack.isEmpty()) {
                list.add(stack.saveOptional(player.registryAccess()));
            }
        }
        addCompletionCards(player, run, list);
        if (list.isEmpty() && levelOrbs <= 0) return ItemStack.EMPTY;
        net.minecraft.nbt.CompoundTag all = lootbox.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        net.minecraft.nbt.CompoundTag root = all.getCompound("gatesofavarice");
        root.put("lootbox_loot", list);
        root.putInt("lootbox_level_orbs", Math.max(0, levelOrbs));
        all.put("gatesofavarice", root);
        lootbox.set(DataComponents.CUSTOM_DATA, CustomData.of(all));
        return lootbox;
    }

    private static void addCompletionCards(ServerPlayer player, RunState run, net.minecraft.nbt.ListTag list) {
        RandomSource random = player.serverLevel().random;
        int playerLevel = Math.max(1, getEffectivePlayerLevel(player));
        int guaranteed = Math.max(0, run.waveNumber / 5);
        int extra = random.nextFloat() < Mth.clamp(playerLevel / 140.0F + run.waveNumber * 0.01F, 0.05F, 0.65F) ? 1 : 0;
        for (int i = 0; i < guaranteed + extra; i++) {
            ItemStack card = rollGatewayCard(random, playerLevel);
            if (!card.isEmpty()) {
                list.add(card.saveOptional(player.registryAccess()));
            }
        }
    }

    public static ItemStack rollGatewayCard(RandomSource random, int playerLevel) {
        GatewayCardData.CardType[] values = GatewayCardData.CardType.values();
        GatewayCardData.CardType type = values[random.nextInt(values.length)];
        return GatewayCardData.create(ModItems.GATEWAY_CARD.get(), type, playerLevel, random);
    }

    private static void restoreSnapshot(ServerPlayer player, PlayerSnapshot snapshot) {
        for (int i = 0; i < player.getInventory().items.size(); i++) player.getInventory().items.set(i, snapshot.items.get(i).copy());
        for (int i = 0; i < player.getInventory().armor.size(); i++) player.getInventory().armor.set(i, snapshot.armor.get(i).copy());
        for (int i = 0; i < player.getInventory().offhand.size(); i++) player.getInventory().offhand.set(i, snapshot.offhand.get(i).copy());
        player.getInventory().selected = snapshot.selectedSlot;
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    private static void restorePlayerSnapshot(ServerPlayer player, PlayerSnapshot snapshot, boolean returnToSavedLocation) {
        MythicCoinWallet.set(player, 0);
        restoreSnapshot(player, snapshot);
        syncHudToPlayer(player, false, 0, 0, 1);
        if (returnToSavedLocation) {
            DungeonInstanceManager.teleportToSavedLocation(player, snapshot.dimension, snapshot.returnPos, snapshot.yaw, snapshot.pitch);
        }
    }

    private static boolean restorePendingSnapshot(ServerPlayer player) {
        PendingSnapshotRestore pending = PENDING_SNAPSHOT_RESTORES.remove(player.getUUID());
        if (pending == null) {
            return false;
        }
        restorePlayerSnapshot(player, pending.snapshot(), true);
        if (pending.completionPayload() != null) {
            PacketDistributor.sendToPlayer(player, pending.completionPayload());
        }
        markStateDirty();
        return true;
    }

    private static PlayerSnapshot findSnapshotForPlayer(UUID playerId) {
        for (RunState run : RUNS_BY_OWNER.values()) {
            PlayerSnapshot snapshot = run.snapshots.get(playerId);
            if (snapshot != null) {
                return snapshot;
            }
        }
        return null;
    }

    private static void maybeAutosave(MinecraftServer server, long gameTime) {
        if (!persistedStateDirty || gameTime - lastAutosaveTick < AUTOSAVE_INTERVAL_TICKS) {
            return;
        }
        savePersistedState(server);
        server.saveEverything(true, false, true);
        lastAutosaveTick = gameTime;
    }

    private static void forceCriticalSave(MinecraftServer server) {
        if (server == null) {
            return;
        }
        savePersistedState(server);
        server.saveEverything(true, false, true);
        ServerLevel overworld = server.overworld();
        lastAutosaveTick = overworld != null ? overworld.getGameTime() : lastAutosaveTick;
    }

    private static CompoundTag buildPersistedState(ServerLevel dataLevel) {
        CompoundTag root = new CompoundTag();
        ListTag runs = new ListTag();
        for (RunState run : RUNS_BY_OWNER.values()) {
            runs.add(saveRun(run, dataLevel.registryAccess()));
        }
        root.put(RUNS_KEY, runs);
        ListTag pendingRestores = new ListTag();
        for (Map.Entry<UUID, PendingSnapshotRestore> entry : PENDING_SNAPSHOT_RESTORES.entrySet()) {
            CompoundTag pending = new CompoundTag();
            pending.putUUID("player", entry.getKey());
            pending.put("snapshot", saveSnapshot(entry.getValue().snapshot(), dataLevel.registryAccess()));
            pendingRestores.add(pending);
        }
        root.put(PENDING_RESTORES_KEY, pendingRestores);
        return root;
    }

    private static void loadPersistedState(CompoundTag root, MinecraftServer server, HolderLookup.Provider registries) {
        ListTag runs = root.getList(RUNS_KEY, Tag.TAG_COMPOUND);
        for (Tag value : runs) {
            RunState run = loadRun((CompoundTag) value, server, registries);
            if (run == null) {
                continue;
            }
            RUNS_BY_OWNER.put(run.ownerId, run);
            for (UUID participantId : run.participants) {
                PLAYER_TO_OWNER.put(participantId, run.ownerId);
            }
        }
        ListTag pendingRestores = root.getList(PENDING_RESTORES_KEY, Tag.TAG_COMPOUND);
        for (Tag value : pendingRestores) {
            CompoundTag pending = (CompoundTag) value;
            if (!pending.hasUUID("player") || !pending.contains("snapshot", Tag.TAG_COMPOUND)) {
                continue;
            }
            PlayerSnapshot snapshot = loadSnapshot(pending.getCompound("snapshot"), registries);
            if (snapshot != null) {
                PENDING_SNAPSHOT_RESTORES.put(pending.getUUID("player"), new PendingSnapshotRestore(snapshot));
            }
        }
    }

    private static CompoundTag saveRun(RunState run, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("owner", run.ownerId);
        tag.putString("phase", run.phase.name());
        tag.putInt("wave_number", run.waveNumber);
        tag.putInt("to_spawn", run.toSpawn);
        tag.putInt("wave_total_mobs", run.waveTotalMobs);
        tag.putInt("spawn_cooldown", run.spawnCooldown);
        tag.putInt("rerolls_used", run.rerollsUsed);
        tag.putBoolean("selecting_loadout", run.selectingLoadout);
        tag.putDouble("enemy_count_multiplier", run.enemyCountMultiplier);
        tag.putDouble("health_multiplier", run.healthMultiplier);
        tag.putDouble("damage_multiplier", run.damageMultiplier);
        tag.putDouble("speed_multiplier", run.speedMultiplier);
        tag.putDouble("mob_leech_percent", run.mobLeechPercent);
        tag.putDouble("elite_chance_bonus", run.eliteChanceBonus);
        tag.putInt("total_difficulty_selected", run.totalDifficultySelected);
        tag.putDouble("quantity_bonus_modifier", run.quantityBonusModifier);
        tag.putDouble("rarity_bonus_modifier", run.rarityBonusModifier);
        tag.putDouble("coin_bonus_modifier", run.coinBonusModifier);
        tag.putDouble("level_multiplier", run.levelMultiplier);
        tag.putInt("extra_reward_rolls", run.extraRewardRolls);
        tag.putInt("horde_weight_bonus", run.hordeWeightBonus);
        tag.putInt("archer_weight_bonus", run.archerWeightBonus);
        tag.putInt("assassin_weight_bonus", run.assassinWeightBonus);
        tag.putInt("tank_weight_bonus", run.tankWeightBonus);
        tag.putInt("elite_weight_bonus", run.eliteWeightBonus);
        tag.putLong("run_start_game_time", run.runStartGameTime);
        tag.putInt("mobs_killed", run.mobsKilled);
        tag.putInt("coins_earned", run.coinsEarned);
        tag.putInt("experience_earned", run.experienceEarned);
        tag.putFloat("damage_dealt", run.damageDealt);
        tag.putFloat("damage_received", run.damageReceived);
        tag.put("participants", saveUuidList(run.participants));
        tag.put("alive_mobs", saveUuidList(run.aliveMobs));
        ListTag snapshots = new ListTag();
        for (Map.Entry<UUID, PlayerSnapshot> entry : run.snapshots.entrySet()) {
            CompoundTag snapshot = new CompoundTag();
            snapshot.putUUID("player", entry.getKey());
            snapshot.put("data", saveSnapshot(entry.getValue(), registries));
            snapshots.add(snapshot);
        }
        tag.put("snapshots", snapshots);
        tag.put("level_source_points", saveIntegerMap(run.levelSourcePoints));
        tag.put("awarded_exit_xp", saveUuidList(run.awardedExitXp));
        return tag;
    }

    private static RunState loadRun(CompoundTag tag, MinecraftServer server, HolderLookup.Provider registries) {
        if (!tag.hasUUID("owner")) {
            return null;
        }
        UUID ownerId = tag.getUUID("owner");
        RunState run = new RunState(ownerId);
        run.server = server;
        run.participants.clear();
        run.participants.addAll(loadUuidSet(tag.getList("participants", Tag.TAG_COMPOUND)));
        if (run.participants.isEmpty()) {
            run.participants.add(ownerId);
        }
        run.snapshots.clear();
        ListTag snapshots = tag.getList("snapshots", Tag.TAG_COMPOUND);
        for (Tag value : snapshots) {
            CompoundTag snapshot = (CompoundTag) value;
            if (!snapshot.hasUUID("player") || !snapshot.contains("data", Tag.TAG_COMPOUND)) {
                continue;
            }
            PlayerSnapshot loadedSnapshot = loadSnapshot(snapshot.getCompound("data"), registries);
            if (loadedSnapshot != null) {
                run.snapshots.put(snapshot.getUUID("player"), loadedSnapshot);
            }
        }
        run.phase = parseRunPhase(tag.getString("phase"));
        run.waveNumber = tag.getInt("wave_number");
        run.aliveMobs = loadUuidSet(tag.getList("alive_mobs", Tag.TAG_COMPOUND));
        run.toSpawn = tag.getInt("to_spawn");
        run.waveTotalMobs = tag.getInt("wave_total_mobs");
        run.spawnCooldown = tag.getInt("spawn_cooldown");
        run.exitPortalId = -1;
        run.shopkeeperId = -1;
        run.currentWavePools = new HashMap<>();
        run.tarotOptions = List.of();
        run.lootOptions = List.of();
        run.loadoutOptions = List.of();
        run.selectingLoadout = tag.getBoolean("selecting_loadout");
        run.rerollsUsed = tag.getInt("rerolls_used");
        run.enemyCountMultiplier = tag.contains("enemy_count_multiplier") ? tag.getDouble("enemy_count_multiplier") : 1.0D;
        run.healthMultiplier = tag.contains("health_multiplier") ? tag.getDouble("health_multiplier") : 1.0D;
        run.damageMultiplier = tag.contains("damage_multiplier") ? tag.getDouble("damage_multiplier") : 1.0D;
        run.speedMultiplier = tag.contains("speed_multiplier") ? tag.getDouble("speed_multiplier") : 1.0D;
        run.mobLeechPercent = tag.getDouble("mob_leech_percent");
        run.eliteChanceBonus = tag.getDouble("elite_chance_bonus");
        run.totalDifficultySelected = tag.getInt("total_difficulty_selected");
        run.quantityBonusModifier = tag.getDouble("quantity_bonus_modifier");
        run.rarityBonusModifier = tag.getDouble("rarity_bonus_modifier");
        run.coinBonusModifier = tag.getDouble("coin_bonus_modifier");
        run.levelMultiplier = tag.contains("level_multiplier") ? tag.getDouble("level_multiplier") : 1.0D;
        run.extraRewardRolls = tag.getInt("extra_reward_rolls");
        run.hordeWeightBonus = tag.getInt("horde_weight_bonus");
        run.archerWeightBonus = tag.getInt("archer_weight_bonus");
        run.assassinWeightBonus = tag.getInt("assassin_weight_bonus");
        run.tankWeightBonus = tag.getInt("tank_weight_bonus");
        run.eliteWeightBonus = tag.getInt("elite_weight_bonus");
        run.runStartGameTime = tag.contains("run_start_game_time") ? tag.getLong("run_start_game_time") : -1L;
        run.levelSourcePoints.clear();
        run.levelSourcePoints.putAll(loadIntegerMap(tag.getList("level_source_points", Tag.TAG_COMPOUND)));
        run.awardedExitXp.clear();
        run.awardedExitXp.addAll(loadUuidSet(tag.getList("awarded_exit_xp", Tag.TAG_COMPOUND)));
        run.mobsKilled = tag.getInt("mobs_killed");
        run.coinsEarned = tag.getInt("coins_earned");
        run.experienceEarned = tag.getInt("experience_earned");
        run.damageDealt = tag.getFloat("damage_dealt");
        run.damageReceived = tag.getFloat("damage_received");
        return run;
    }

    private static CompoundTag saveSnapshot(PlayerSnapshot snapshot, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", snapshot.dimension.location().toString());
        tag.putInt("return_x", snapshot.returnPos.getX());
        tag.putInt("return_y", snapshot.returnPos.getY());
        tag.putInt("return_z", snapshot.returnPos.getZ());
        tag.putFloat("yaw", snapshot.yaw);
        tag.putFloat("pitch", snapshot.pitch);
        tag.putInt("selected_slot", snapshot.selectedSlot);
        tag.put("items", saveItemStacks(snapshot.items, registries));
        tag.put("armor", saveItemStacks(snapshot.armor, registries));
        tag.put("offhand", saveItemStacks(snapshot.offhand, registries));
        return tag;
    }

    private static PlayerSnapshot loadSnapshot(CompoundTag tag, HolderLookup.Provider registries) {
        if (!tag.contains("dimension", Tag.TAG_STRING)) {
            return null;
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("dimension"));
        if (dimensionId == null) {
            return null;
        }
        return new PlayerSnapshot(
                ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionId),
                new BlockPos(tag.getInt("return_x"), tag.getInt("return_y"), tag.getInt("return_z")),
                tag.getFloat("yaw"),
                tag.getFloat("pitch"),
                loadItemStacks(tag.getList("items", Tag.TAG_COMPOUND), registries, 36),
                loadItemStacks(tag.getList("armor", Tag.TAG_COMPOUND), registries, 4),
                loadItemStacks(tag.getList("offhand", Tag.TAG_COMPOUND), registries, 1),
                tag.getInt("selected_slot"));
    }

    private static ListTag saveUuidList(Iterable<UUID> uuids) {
        ListTag list = new ListTag();
        for (UUID uuid : uuids) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", uuid);
            list.add(entry);
        }
        return list;
    }

    private static Set<UUID> loadUuidSet(ListTag list) {
        Set<UUID> uuids = new HashSet<>();
        for (Tag value : list) {
            CompoundTag entry = (CompoundTag) value;
            if (entry.hasUUID("id")) {
                uuids.add(entry.getUUID("id"));
            }
        }
        return uuids;
    }

    private static ListTag saveIntegerMap(Map<UUID, Integer> values) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Integer> entry : values.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("player", entry.getKey());
            tag.putInt("value", entry.getValue());
            list.add(tag);
        }
        return list;
    }

    private static Map<UUID, Integer> loadIntegerMap(ListTag list) {
        Map<UUID, Integer> values = new HashMap<>();
        for (Tag value : list) {
            CompoundTag entry = (CompoundTag) value;
            if (entry.hasUUID("player")) {
                values.put(entry.getUUID("player"), entry.getInt("value"));
            }
        }
        return values;
    }

    private static ListTag saveItemStacks(List<ItemStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            list.add(stack.saveOptional(registries));
        }
        return list;
    }

    private static List<ItemStack> loadItemStacks(ListTag list, HolderLookup.Provider registries, int expectedSize) {
        ArrayList<ItemStack> stacks = new ArrayList<>(expectedSize);
        for (int i = 0; i < list.size(); i++) {
            stacks.add(ItemStack.parseOptional(registries, list.getCompound(i)));
        }
        while (stacks.size() < expectedSize) {
            stacks.add(ItemStack.EMPTY);
        }
        return List.copyOf(stacks);
    }

    private static RunPhase parseRunPhase(String name) {
        try {
            return RunPhase.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return RunPhase.SELECTING_TAROT;
        }
    }

    private static int cashoutRemainingCoinsOnDungeonExit(ServerPlayer player) {
        int coins = MythicCoinWallet.get(player);
        int goldCoins = coins / 1000;
        GoldCoinWallet.add(player, goldCoins);
        if (goldCoins > 0) {
            player.sendSystemMessage(Component.translatable("message.gatesofavarice.gold_coins_earned", coins, goldCoins));
        }
        MythicCoinWallet.set(player, 0);
        return coins;
    }

    public static int getBestWave(ServerPlayer player) {
        return Math.max(0, player.getData(ModAttachments.BEST_WAVE));
    }

    public static void setBestWave(ServerPlayer player, int wave) {
        player.setData(ModAttachments.BEST_WAVE, Math.max(0, wave));
        AttachmentSync.syncEntityUpdate(player, ModAttachments.BEST_WAVE.get());
    }

    public static void resetBestWave(ServerPlayer player) {
        setBestWave(player, 0);
    }

    private static int recordBestWave(ServerPlayer player, int wave) {
        int current = getBestWave(player);
        if (wave > current) {
            setBestWave(player, wave);
            return wave;
        }
        return current;
    }

    private static void sendCompletionScreen(ServerPlayer player, RunState run, List<ItemStack> rewards, int levelPoints, int cashedOutCoins, boolean survived) {
        PacketDistributor.sendToPlayer(player, buildCompletionPayload(run, player, player.serverLevel().getGameTime(), rewards, levelPoints, cashedOutCoins, survived));
    }

    private static DungeonCompletePayload createDeathSummary(RunState run, ServerPlayer player) {
        return buildCompletionPayload(run, player, player.serverLevel().getGameTime(), List.of(), 0, 0, false);
    }

    private static DungeonCompletePayload buildCompletionPayload(RunState run, ServerPlayer player, long now, List<ItemStack> rewards, int levelPoints, int cashedOutCoins, boolean survived) {
        long elapsedTicks = run.runStartGameTime < 0L ? 0L : Math.max(0L, now - run.runStartGameTime);
        if (survived) {
            run.experienceEarned += cashedOutCoins;
        }
        int bestWave = recordBestWave(player, Math.max(0, run.waveNumber));
        return new DungeonCompletePayload(
                survived,
                Math.max(0, run.waveNumber),
                bestWave,
                elapsedTicks,
                levelPoints,
                cashedOutCoins,
                run.mobsKilled,
                Math.round(run.damageDealt),
                Math.round(run.damageReceived),
                survived ? levelPoints + cashedOutCoins : 0,
                (int) Math.round(run.rarityBonusModifier * 100.0D),
                (int) Math.round(run.quantityBonusModifier * 100.0D),
                (int) Math.round(Math.max(0.0D, (run.enemyCountMultiplier - 1.0D) * 60.0D)),
                (int) Math.round(Math.max(0.0D, (run.damageMultiplier - 1.0D) * 100.0D)),
                rewards
        );
    }

    private static void syncHud(RunState run, boolean active) {
        int remaining = Math.max(0, run.toSpawn + run.aliveMobs.size());
        int total = Math.max(1, run.waveTotalMobs);
        DungeonWaveHudPayload payload = createHudPayload(run, active, run.waveNumber, remaining, total);
        for (ServerPlayer participant : run.liveParticipants()) PacketDistributor.sendToPlayer(participant, payload);
    }

    private enum RunPhase { SELECTING_TAROT, SELECTING_LOOT, IN_WAVE, SHOP, WAITING_EXIT }

    private enum WaveArchetype { UNDEAD, HORDE, ASSASSIN, ARCHER, TANK, NETHER }

    private static final class RunState {
        private final UUID ownerId;
        private MinecraftServer server;
        private final Set<UUID> participants = new HashSet<>();
        private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
        private RunPhase phase = RunPhase.SELECTING_TAROT;
        private int waveNumber = 0;
        private Set<UUID> aliveMobs = new HashSet<>();
        private int toSpawn = 0;
        private int waveTotalMobs = 0;
        private int spawnCooldown = 0;
        private int exitPortalId = -1;
        private int shopkeeperId = -1;
        private Map<WaveArchetype, EnemyPoolSet> currentWavePools = new HashMap<>();
        private List<TarotOption> tarotOptions = List.of();
        private List<LootOption> lootOptions = List.of();
        private List<LoadoutOption> loadoutOptions = List.of();
        private boolean selectingLoadout = false;
        private int rerollsUsed = 0;
        private double enemyCountMultiplier = 1.0D;
        private double healthMultiplier = 1.0D;
        private double damageMultiplier = 1.0D;
        private double speedMultiplier = 1.0D;
        private double mobLeechPercent = 0.0D;
        private double eliteChanceBonus = 0.0D;
        private int totalDifficultySelected = 0;
        private double quantityBonusModifier = 0.0D;
        private double rarityBonusModifier = 0.0D;
        private double coinBonusModifier = 0.0D;
        private double levelMultiplier = 1.0D;
        private int extraRewardRolls = 0;
        private int hordeWeightBonus = 0;
        private int archerWeightBonus = 0;
        private int assassinWeightBonus = 0;
        private int tankWeightBonus = 0;
        private int eliteWeightBonus = 0;
        private long runStartGameTime = -1L;
        private final Map<UUID, Integer> levelSourcePoints = new HashMap<>();
        private final Set<UUID> awardedExitXp = new HashSet<>();
        private int mobsKilled = 0;
        private int coinsEarned = 0;
        private int experienceEarned = 0;
        private float damageDealt = 0.0F;
        private float damageReceived = 0.0F;

        private RunState(UUID ownerId) {
            this.ownerId = ownerId;
            this.participants.add(ownerId);
        }

        private ServerPlayer online(UUID playerId) {
            return this.server == null ? null : this.server.getPlayerList().getPlayer(playerId);
        }

        private List<ServerPlayer> liveParticipants() {
            ArrayList<ServerPlayer> players = new ArrayList<>();
            for (UUID uuid : this.participants) {
                ServerPlayer player = this.online(uuid);
                if (player != null && player.isAlive() && player.level().dimension() == ModDimensions.DUNGEON_LEVEL) players.add(player);
            }
            return players;
        }
    }

    private static final class PlayerSnapshot {
        private final ResourceKey<Level> dimension;
        private final BlockPos returnPos;
        private final float yaw;
        private final float pitch;
        private final List<ItemStack> items;
        private final List<ItemStack> armor;
        private final List<ItemStack> offhand;
        private final int selectedSlot;

        private PlayerSnapshot(ResourceKey<Level> dimension, BlockPos returnPos, float yaw, float pitch, List<ItemStack> items, List<ItemStack> armor, List<ItemStack> offhand, int selectedSlot) {
            this.dimension = dimension;
            this.returnPos = returnPos;
            this.yaw = yaw;
            this.pitch = pitch;
            this.items = items;
            this.armor = armor;
            this.offhand = offhand;
            this.selectedSlot = selectedSlot;
        }

        private static PlayerSnapshot capture(ServerPlayer player) {
            return new PlayerSnapshot(
                    player.level().dimension(),
                    player.blockPosition(),
                    player.getYRot(),
                    player.getXRot(),
                    copyStacks(player.getInventory().items),
                    copyStacks(player.getInventory().armor),
                    copyStacks(player.getInventory().offhand),
                    player.getInventory().selected
            );
        }

        private static List<ItemStack> copyStacks(List<ItemStack> source) {
            ArrayList<ItemStack> copied = new ArrayList<>(source.size());
            for (ItemStack stack : source) copied.add(stack.copy());
            return copied;
        }
    }

    private record PendingSnapshotRestore(PlayerSnapshot snapshot, DungeonCompletePayload completionPayload) {
        private PendingSnapshotRestore(PlayerSnapshot snapshot) {
            this(snapshot, null);
        }
    }

    private static final class TarotOption {
        private final Component title;
        private final Component details;
        private final double enemyCountBonus;
        private final double healthBonus;
        private final double damageBonus;
        private final double speedBonus;
        private final double mobLeechBonus;
        private final double eliteChanceBonus;
        private final double quantityBonus;
        private final double rarityBonus;
        private final double coinBonus;
        private final double levelBonus;
        private final int rewardRollBonus;
        private final int hordeMobs;
        private final int archerMobs;
        private final int assassinMobs;
        private final int tankMobs;
        private final int eliteMobs;
        private final int difficulty;

        private TarotOption(Component title, Component details, double enemyCountBonus, double healthBonus, double damageBonus, double speedBonus, double mobLeechBonus,
                double eliteChanceBonus, double quantityBonus, double rarityBonus, double coinBonus, double levelBonus, int rewardRollBonus,
                int hordeMobs, int archerMobs, int assassinMobs, int tankMobs, int eliteMobs, int difficulty) {
            this.title = title;
            this.details = details;
            this.enemyCountBonus = enemyCountBonus;
            this.healthBonus = healthBonus;
            this.damageBonus = damageBonus;
            this.speedBonus = speedBonus;
            this.mobLeechBonus = mobLeechBonus;
            this.eliteChanceBonus = eliteChanceBonus;
            this.quantityBonus = quantityBonus;
            this.rarityBonus = rarityBonus;
            this.coinBonus = coinBonus;
            this.levelBonus = levelBonus;
            this.rewardRollBonus = rewardRollBonus;
            this.hordeMobs = hordeMobs;
            this.archerMobs = archerMobs;
            this.assassinMobs = assassinMobs;
            this.tankMobs = tankMobs;
            this.eliteMobs = eliteMobs;
            this.difficulty = difficulty;
        }

        private static TarotOption random(RandomSource random, int difficulty, int displayedWave, int avgLevel) {
            int wavePressure = Math.max(0, displayedWave - 1);
            double levelPressure = Math.max(0.0D, Math.min(0.10D, Math.max(0, avgLevel - 80) * 0.001D));
            int mobLineCount = Math.max(1, Math.min(3, 1 + (difficulty - 1) / 2 + (random.nextBoolean() ? 1 : 0)));
            int negativeCount = Math.max(2, Math.min(5, 1 + difficulty + (random.nextBoolean() ? 1 : 0)));
            int positiveCount = Math.max(2, Math.min(4, 1 + difficulty / 2 + (random.nextBoolean() ? 1 : 0)));
            int hordeMobs = 0;
            int archerMobs = 0;
            int assassinMobs = 0;
            int tankMobs = 0;
            int eliteMobs = 0;
            double enemyBonus = 0.0D;
            double health = 0.0D;
            double damage = 0.0D;
            double speed = 0.0D;
            double leech = 0.0D;
            double eliteChance = 0.0D;
            double quantity = 0.0D;
            double rarity = 0.0D;
            double coins = 0.0D;
            double levels = 0.0D;
            ArrayList<String> lines = new ArrayList<>();
            ArrayList<String> mobPool = new ArrayList<>(List.of("hoard", "archer", "assassin", "tank"));
            if (displayedWave > 1) {
                mobPool.add("elite");
            }
            for (int i = 0; i < mobLineCount && !mobPool.isEmpty(); i++) {
                String type = mobPool.remove(random.nextInt(mobPool.size()));
                switch (type) {
                    case "hoard" -> {
                        hordeMobs = 2 + random.nextInt(2 + Math.max(1, difficulty / 2));
                        enemyBonus += 0.04D * hordeMobs;
                        lines.add("+" + hordeMobs + " hoard mobs");
                    }
                    case "archer" -> {
                        archerMobs = 1 + random.nextInt(Math.min(3, 1 + difficulty / 2));
                        enemyBonus += 0.03D * archerMobs;
                        lines.add("+" + archerMobs + " archer mobs");
                    }
                    case "assassin" -> {
                        assassinMobs = 1 + random.nextInt(Math.min(3, 1 + difficulty / 2));
                        enemyBonus += 0.03D * assassinMobs;
                        lines.add("+" + assassinMobs + " assassin mobs");
                    }
                    case "tank" -> {
                        tankMobs = 1 + random.nextInt(Math.min(3, 1 + difficulty / 2));
                        enemyBonus += 0.025D * tankMobs;
                        lines.add("+" + tankMobs + " tank mobs");
                    }
                    case "elite" -> {
                        eliteMobs = 1 + random.nextInt(Math.min(2, Math.max(1, difficulty - 1)));
                        eliteChance += eliteMobs * 0.0075D;
                        lines.add("+" + eliteMobs + " elite mobs");
                    }
                }
            }
            lines.add("-------------------");
            ArrayList<String> negativePool = new ArrayList<>(List.of("spawn", "damage", "health", "speed", "leech"));
            for (int i = 0; i < negativeCount && !negativePool.isEmpty(); i++) {
                String type = negativePool.remove(random.nextInt(negativePool.size()));
                switch (type) {
                    case "spawn" -> {
                        double amount = 0.06D + difficulty * 0.025D + wavePressure * 0.004D + levelPressure;
                        enemyBonus += amount;
                        lines.add("+" + Math.round(amount * 100.0D) + "% spawn chance");
                    }
                    case "damage" -> {
                        double amount = 0.06D + difficulty * 0.03D + wavePressure * 0.005D + levelPressure;
                        damage += amount;
                        lines.add("+" + Math.round(amount * 100.0D) + "% mob damage");
                    }
                    case "health" -> {
                        double amount = 0.08D + difficulty * 0.035D + wavePressure * 0.006D + levelPressure;
                        health += amount;
                        lines.add("+" + Math.round(amount * 100.0D) + "% mob health");
                    }
                    case "speed" -> {
                        double amount = 0.04D + difficulty * 0.02D + wavePressure * 0.003D;
                        speed += amount;
                        lines.add("+" + Math.round(amount * 100.0D) + "% mob speed");
                    }
                    case "leech" -> {
                        leech += 0.005D + random.nextDouble() * (0.004D * difficulty + 0.003D);
                        lines.add("+" + String.format(java.util.Locale.ROOT, "%.1f", leech * 100.0D) + "% mob leech");
                    }
                }
            }
            lines.add("-------------------");
            ArrayList<String> positivePool = new ArrayList<>(List.of("quantity", "rarity", "coins", "levels"));
            for (int i = 0; i < positiveCount && !positivePool.isEmpty(); i++) {
                String type = positivePool.remove(random.nextInt(positivePool.size()));
                switch (type) {
                    case "quantity" -> {
                        quantity += 0.025D + difficulty * 0.015D + wavePressure * 0.003D;
                        lines.add("+" + Math.round(quantity * 100.0D) + "% quantity");
                    }
                    case "rarity" -> {
                        rarity += 0.02D + difficulty * 0.0125D + wavePressure * 0.0025D;
                        lines.add("+" + Math.round(rarity * 100.0D) + "% rarity");
                    }
                    case "coins" -> {
                        coins += 0.06D + difficulty * 0.03D + wavePressure * 0.006D;
                        lines.add("+" + Math.round(coins * 100.0D) + "% coins");
                    }
                    case "levels" -> {
                        double amount = 0.06D + difficulty * 0.03D + wavePressure * 0.006D;
                        levels += amount;
                        lines.add("+" + Math.round(amount * 100.0D) + "% levels");
                    }
                }
            }
            Component title = Component.literal(difficultyName(difficulty));
            return new TarotOption(
                    title,
                    Component.literal(String.join("\n", lines)),
                    enemyBonus,
                    health,
                    damage,
                    speed,
                    leech,
                    eliteChance,
                    quantity,
                    rarity,
                    coins,
                    levels,
                    0,
                    hordeMobs,
                    archerMobs,
                    assassinMobs,
                    tankMobs,
                    eliteMobs,
                    difficulty
            );
        }

        private static String difficultyName(int difficulty) {
            return switch (difficulty) {
                case 1 -> "Easy";
                case 2 -> "Normal";
                case 3 -> "Medium";
                case 4 -> "Hard";
                default -> "Extreme";
            };
        }
    }

    private static final class LootOption {
        private final Component title;
        private final Component details;
        private final ItemStack stack;

        private LootOption(Component title, Component details, ItemStack stack) {
            this.title = title;
            this.details = details;
            this.stack = stack;
        }

        private ItemStack stack() { return this.stack; }

        private static LootOption category(String title, String details, ItemStack preview) {
            return new LootOption(Component.literal(title), Component.literal(details), preview);
        }

        private static LootOption weapon(RandomSource random, int playerLevel) {
            ItemStack stack = new ItemStack(pickWeightedWeapon(random, playerLevel));
            String name = stack.getHoverName().getString();
            return new LootOption(Component.literal("Weapon"), Component.literal(name), stack);
        }

        private static LootOption armor(RandomSource random) {
            ItemStack stack = switch (random.nextInt(4)) {
                case 0 -> new ItemStack(Items.IRON_HELMET);
                case 1 -> new ItemStack(Items.IRON_CHESTPLATE);
                case 2 -> new ItemStack(Items.IRON_LEGGINGS);
                default -> new ItemStack(Items.IRON_BOOTS);
            };
            return new LootOption(Component.literal("Armor"), Component.literal(stack.getHoverName().getString()), stack);
        }

        private static LootOption buff(RandomSource random) {
            ItemStack stack = random.nextBoolean() ? new ItemStack(Items.GOLDEN_APPLE, 16) : new ItemStack(ModItems.ARCANE_APPLE.get(), 8);
            String name = stack.getHoverName().getString() + " x" + stack.getCount();
            return new LootOption(Component.literal("Item"), Component.literal(name), stack);
        }

        private static LootOption coins(RandomSource random, int playerLevel) {
            int base = 100 + Math.max(1, playerLevel) * 8;
            int variance = 20 + random.nextInt(81);
            int value = base + variance;
            ItemStack stack = MythicCoinStackData.createStack(value);
            return new LootOption(Component.literal("Item Upgrade"), Component.literal("Mythic Coins x" + value), stack);
        }

        private static LootOption magnet(RandomSource random, int playerLevel) {
            ItemStack stack = pickLeveledMagnet(random, playerLevel);
            if (stack.isEmpty()) {
                return buff(random);
            }
            String name = stack.getHoverName().getString();
            return new LootOption(Component.literal("Item Upgrade"), Component.literal(name), stack);
        }

        private static LootOption mystery(RandomSource random, int playerLevel) {
            return switch (random.nextInt(5)) {
                case 0 -> weapon(random, playerLevel);
                case 1 -> armor(random);
                case 2 -> buff(random);
                case 3 -> magnet(random, playerLevel);
                default -> coins(random, playerLevel);
            };
        }
    }

    private record WeightedItem(Item item, int weight) {
    }

    private record LoadoutOption(
            String loadoutId,
            Component title,
            Component details,
            ItemStack head,
            ItemStack chest,
            ItemStack legs,
            ItemStack feet,
            ItemStack primary,
            ItemStack secondary,
            ItemStack utility,
            List<ItemStack> food
    ) {
    }

    private record LoadoutDefinition(
            String name,
            String primaryKind,
            String secondaryKind,
            String armorName,
            Item head,
            Item chest,
            Item legs,
            Item feet,
            List<String> traits,
            List<ItemStack> food
    ) {
        private String foodSummary() {
            ArrayList<String> parts = new ArrayList<>();
            for (ItemStack stack : this.food) {
                parts.add(stack.getHoverName().getString() + " x" + stack.getCount());
            }
            return String.join(", ", parts);
        }
    }
}

