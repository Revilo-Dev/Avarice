package com.revilo.gatesofavarice.dungeon;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.block.entity.PoiLootboxBlockEntity;
import com.revilo.gatesofavarice.registry.ModBlocks;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class DungeonInstanceManager {

    private static final int INSTANCE_SPACING = 512;
    private static final int PLATFORM_Y = 64;
    // Clear the whole reserved instance area. This also removes remnants from older template layouts.
    private static final int DUNGEON_HALF_SPAN = 96;
    private static final int DUNGEON_CLEAR_HEIGHT = 80;
    private static final int INSTANCE_CLEANUP_RADIUS = 96;
    private static final long PLATFORM_LIFETIME_TICKS = 20L * 120L;
    private static final Vec3 PLAYER_SPAWN_OFFSET = new Vec3(29.0D, 2.0D, 0.0D);
    private static final Vec3 SHOP_PLAYER_SPAWN = new Vec3(4983831.0D, 65.0D, -4828645.0D);
    private static final Vec3 SHOP_ARCHIVIST_POSITION = new Vec3(4983831.0D, 65.0D, -4828631.0D);
    private static final Vec3 SHOP_ARMORER_POSITION = new Vec3(4983814.0D, 65.0D, -4828648.0D);
    private static final Vec3 SHOP_ENCHANTER_POSITION = new Vec3(4983850.0D, 65.0D, -4828648.0D);
    private static final Vec3 SHOP_ADVANCE_PORTAL_POSITION = new Vec3(4983832.0D, 65.0D, -4828664.0D);
    private static final BlockPos SHOP_STRUCTURE_ORIGIN = new BlockPos(4983831, 64, -4828645);
    private static final Vec3 EXIT_PORTAL_OFFSET = new Vec3(0.0D, 5.0D, 8.0D);
    private static final Vec3 SHOPKEEPER_OFFSET = new Vec3(0.0D, 4.0D, 0.0D);
    private static final Vec3 ADVANCE_PORTAL_OFFSET = new Vec3(0.0D, 1.0D, 0.0D);
    // These are the walkable floor blocks; the former values targeted the raised structure layer above them.
    private static final int[] DUNGEON_SPAWN_SURFACE_Y = {64, 75, 85};
    // The assembled combat room is 96 blocks across; leave a narrow edge buffer but use its full interior.
    private static final double MOB_SPAWN_RADIUS = 46.0D;
    private static final int CENTRAL_PILLAR_MIN_X_OFFSET = -5;
    private static final int CENTRAL_PILLAR_MAX_X_OFFSET = 4;
    private static final int CENTRAL_PILLAR_MIN_Z_OFFSET = -5;
    private static final int CENTRAL_PILLAR_MAX_Z_OFFSET = 4;
    private static final float PLAYER_SPAWN_YAW = 90.0F;
    private static final ResourceLocation BROKEN_STRUCTURE_DROP = ResourceLocation.fromNamespaceAndPath("chipped", "iron_bowl_soul_lantern");

    private static final DungeonStructurePiece[] DUNGEON_PIECES = {
            new DungeonStructurePiece("t1-nw1", -48, -48),
            new DungeonStructurePiece("t1-ne1", 0, -48),
            new DungeonStructurePiece("t1-sw1", -48, 0),
            new DungeonStructurePiece("t1-se1", 0, 0)
    };
    private static final Map<BlockPos, Long> ACTIVE_DUNGEONS = new HashMap<>();
    private static final Map<BlockPos, InstanceLayout> INSTANCE_LAYOUTS = new HashMap<>();

    private DungeonInstanceManager() {
    }

    public static boolean teleportToDungeonInstance(ServerPlayer player, UUID instanceOwnerId) {
        ServerLevel dungeonLevel = player.server.getLevel(ModDimensions.DUNGEON_LEVEL);
        if (dungeonLevel == null) {
            GatewayExpansion.LOGGER.error("Dungeon dimension is not available. Check dimension datapack registration.");
            return false;
        }

        BlockPos origin = instanceOrigin(instanceOwnerId);
        ensureInstance(dungeonLevel, origin, InstanceLayout.DUNGEON);
        clearDungeonItems(dungeonLevel, origin);
        clearBrokenStructureDrops(dungeonLevel, origin);

        Vec3 spawnPos = playerSpawnPosition(instanceOwnerId);
        player.teleportTo(dungeonLevel, spawnPos.x(), spawnPos.y(), spawnPos.z(), PLAYER_SPAWN_YAW, 0.0F);
        player.setPortalCooldown();
        return true;
    }

    public static boolean teleportToSavedLocation(ServerPlayer player, ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos pos, float yaw, float pitch) {
        return teleportToSavedLocation(player, dimension, Vec3.atBottomCenterOf(pos), yaw, pitch);
    }

    public static boolean teleportToSavedLocation(ServerPlayer player, ResourceKey<net.minecraft.world.level.Level> dimension, Vec3 pos, float yaw, float pitch) {
        ServerLevel destination = player.server.getLevel(dimension);
        if (destination == null) {
            return false;
        }
        player.teleportTo(destination, pos.x(), pos.y(), pos.z(), yaw, pitch);
        player.setPortalCooldown();
        return true;
    }

    /** Moves a player to the mob-free shop layout for a completed five-floor segment. */
    public static boolean teleportToShopInstance(ServerPlayer player, UUID instanceOwnerId) {
        ServerLevel dungeonLevel = player.server.getLevel(ModDimensions.DUNGEON_LEVEL);
        if (dungeonLevel == null) {
            GatewayExpansion.LOGGER.error("Dungeon dimension is not available. Check dimension datapack registration.");
            return false;
        }

        ensureInstance(dungeonLevel, SHOP_STRUCTURE_ORIGIN, InstanceLayout.SHOP);
        clearDungeonItems(dungeonLevel, SHOP_STRUCTURE_ORIGIN);

        Vec3 spawnPos = SHOP_PLAYER_SPAWN;
        player.teleportTo(dungeonLevel, spawnPos.x(), spawnPos.y(), spawnPos.z(), PLAYER_SPAWN_YAW, 0.0F);
        player.setPortalCooldown();
        return true;
    }

    public static BlockPos instanceCenter(UUID instanceOwnerId) {
        return instanceOrigin(instanceOwnerId);
    }

    public static Vec3 exitPortalPosition(UUID instanceOwnerId) {
        return positionedOffset(instanceOwnerId, EXIT_PORTAL_OFFSET);
    }

    public static Vec3 shopkeeperPosition(UUID instanceOwnerId) {
        return SHOP_ARCHIVIST_POSITION;
    }

    public static Vec3 armorerPosition() { return SHOP_ARMORER_POSITION; }

    public static Vec3 enchanterPosition() { return SHOP_ENCHANTER_POSITION; }

    public static Vec3 shopAdvancePortalPosition() { return SHOP_ADVANCE_PORTAL_POSITION; }

    public static Vec3 advancePortalPosition(UUID instanceOwnerId) {
        return positionedOffset(instanceOwnerId, ADVANCE_PORTAL_OFFSET);
    }

    /** Adds fresh point-of-interest crates without rebuilding the current floor. */
    public static void spawnWaveLootboxes(ServerLevel level, UUID instanceOwnerId, int floor, double quantityBonus, int count) {
        if (count <= 0) {
            return;
        }
        spawnPoiLootboxes(level, instanceOrigin(instanceOwnerId), floor, quantityBonus, count);
    }

    public static Vec3 randomMobSpawnPosition(ServerLevel level, UUID instanceOwnerId, net.minecraft.util.RandomSource random, int activeMobCount) {
        BlockPos origin = instanceOrigin(instanceOwnerId);
        double spawnRadius = 16.0D + Math.min(30.0D, Math.max(0, activeMobCount) * 0.6D);
        for (int attempt = 0; attempt < 96; attempt++) {
            int x = origin.getX() + random.nextInt((int) (spawnRadius * 2.0D + 1.0D)) - (int) spawnRadius;
            int z = origin.getZ() + random.nextInt((int) (spawnRadius * 2.0D + 1.0D)) - (int) spawnRadius;
            int y = DUNGEON_SPAWN_SURFACE_Y[random.nextInt(DUNGEON_SPAWN_SURFACE_Y.length)];
            BlockPos surface = new BlockPos(x, y, z);
            if (!isCentralPillarPosition(origin, x + 0.5D, z + 0.5D)
                    && level.getBlockState(surface).isFaceSturdy(level, surface, net.minecraft.core.Direction.UP)
                    && level.getBlockState(surface.above()).isAir()
                    && level.getBlockState(surface.above(2)).isAir()) {
                return Vec3.atBottomCenterOf(surface.above());
            }
        }
        return null;
    }

    private static Vec3 playerSpawnPosition(UUID instanceOwnerId) {
        return positionedOffset(instanceOwnerId, PLAYER_SPAWN_OFFSET);
    }

    private static Vec3 positionedOffset(UUID instanceOwnerId, Vec3 offset) {
        BlockPos origin = instanceOrigin(instanceOwnerId);
        return new Vec3(origin.getX() + offset.x(), origin.getY() + offset.y(), origin.getZ() + offset.z());
    }

    private static boolean isCentralPillarPosition(BlockPos origin, double x, double z) {
        int blockXOffset = BlockPos.containing(x, origin.getY(), z).getX() - origin.getX();
        int blockZOffset = BlockPos.containing(x, origin.getY(), z).getZ() - origin.getZ();
        return blockXOffset >= CENTRAL_PILLAR_MIN_X_OFFSET
                && blockXOffset <= CENTRAL_PILLAR_MAX_X_OFFSET
                && blockZOffset >= CENTRAL_PILLAR_MIN_Z_OFFSET
                && blockZOffset <= CENTRAL_PILLAR_MAX_Z_OFFSET;
    }

    public static void keepInstanceAlive(UUID instanceOwnerId, ServerLevel dungeonLevel) {
        if (dungeonLevel == null) {
            return;
        }
        BlockPos origin = instanceOrigin(instanceOwnerId);
        ensureInstance(dungeonLevel, origin, INSTANCE_LAYOUTS.getOrDefault(origin, InstanceLayout.DUNGEON));
        ACTIVE_DUNGEONS.put(origin.immutable(), dungeonLevel.getGameTime() + PLATFORM_LIFETIME_TICKS);
    }

    /** Rebuilds the combat room so every floor starts with a fresh structure and POIs. */
    public static void reloadDungeonFloor(ServerLevel level, UUID instanceOwnerId, int floor, double quantityBonus) {
        BlockPos origin = instanceOrigin(instanceOwnerId);
        showRebuildStatus(level, origin, "Rebuilding dungeon floor...");
        clearDungeonEntities(level, origin);
        clearDungeonVolume(level, origin);
        placeDungeonStructure(level, origin);
        clearBrokenStructureDrops(level, origin);
        spawnPoiLootboxes(level, origin, floor, quantityBonus);
        ACTIVE_DUNGEONS.put(origin.immutable(), level.getGameTime() + PLATFORM_LIFETIME_TICKS);
        INSTANCE_LAYOUTS.put(origin.immutable(), InstanceLayout.DUNGEON);
        showRebuildStatus(level, origin, "Dungeon floor ready.");
    }

    public static void cleanupInstance(UUID instanceOwnerId, ServerLevel dungeonLevel) {
        if (dungeonLevel == null) {
            return;
        }

        BlockPos origin = instanceOrigin(instanceOwnerId);
        clearDungeonEntities(dungeonLevel, origin);
        clearDungeonVolume(dungeonLevel, origin);
        clearDungeonItems(dungeonLevel, origin);
        ACTIVE_DUNGEONS.remove(origin);
        INSTANCE_LAYOUTS.remove(origin);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel dungeonLevel = event.getServer().getLevel(ModDimensions.DUNGEON_LEVEL);
        if (dungeonLevel == null || ACTIVE_DUNGEONS.isEmpty()) {
            return;
        }

        long gameTime = dungeonLevel.getGameTime();
        extinguishFireNearDungeonPlayers(dungeonLevel);
        ArrayList<BlockPos> expiredDungeons = new ArrayList<>();
        for (Map.Entry<BlockPos, Long> entry : ACTIVE_DUNGEONS.entrySet()) {
            if (gameTime >= entry.getValue()) {
                clearDungeonEntities(dungeonLevel, entry.getKey());
                clearDungeonVolume(dungeonLevel, entry.getKey());
                expiredDungeons.add(entry.getKey());
            }
        }

        for (BlockPos dungeonOrigin : expiredDungeons) {
            ACTIVE_DUNGEONS.remove(dungeonOrigin);
            INSTANCE_LAYOUTS.remove(dungeonOrigin);
        }
    }

    private static BlockPos instanceOrigin(UUID playerId) {
        long hash = playerId.getMostSignificantBits() ^ playerId.getLeastSignificantBits();
        int xIndex = (int) (hash & 0xFFFFL) - 32768;
        int zIndex = (int) ((hash >>> 16) & 0xFFFFL) - 32768;

        int x = Mth.clamp(xIndex, -30000, 30000) * INSTANCE_SPACING;
        int z = Mth.clamp(zIndex, -30000, 30000) * INSTANCE_SPACING;
        return new BlockPos(x, PLATFORM_Y, z);
    }

    private static void ensureInstance(ServerLevel level, BlockPos origin, InstanceLayout layout) {
        boolean alreadyActive = ACTIVE_DUNGEONS.containsKey(origin);
        InstanceLayout activeLayout = INSTANCE_LAYOUTS.get(origin);
        long expiresAt = level.getGameTime() + PLATFORM_LIFETIME_TICKS;
        ACTIVE_DUNGEONS.put(origin.immutable(), expiresAt);

        if (alreadyActive && activeLayout == layout) {
            return;
        }

        clearDungeonEntities(level, origin);
        clearDungeonVolume(level, origin);
        if (layout == InstanceLayout.DUNGEON) {
            placeDungeonStructure(level, origin);
            clearBrokenStructureDrops(level, origin);
            spawnPoiLootboxes(level, origin, 1, 0.0D);
        } else {
            placeShopStructure(level, origin);
        }
        INSTANCE_LAYOUTS.put(origin.immutable(), layout);
    }

    private static void placeShopStructure(ServerLevel level, BlockPos origin) {
        Optional<StructureTemplate> template = loadStructure(level, "t1-archive1");
        if (template.isEmpty()) {
            GatewayExpansion.LOGGER.error("Missing shop structure t1-archive1");
            return;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(false);
        if (!template.get().placeInWorld(level, origin, origin, settings, level.random, 2)) {
            GatewayExpansion.LOGGER.error("Failed to place shop structure t1-archive1 at {}", origin);
        }
    }

    /**
     * Dungeon structures are immutable during a run.  Extinguish both normal and soul fire
     * around players before a fire tick can spread it into the template.
     */
    private static void extinguishFireNearDungeonPlayers(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            BlockPos playerPos = player.blockPosition();
            boolean inActiveDungeon = ACTIVE_DUNGEONS.keySet().stream()
                    .anyMatch(origin -> Math.abs(playerPos.getX() - origin.getX()) <= DUNGEON_HALF_SPAN
                            && Math.abs(playerPos.getZ() - origin.getZ()) <= DUNGEON_HALF_SPAN);
            if (!inActiveDungeon) continue;

            for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-12, -6, -12), playerPos.offset(12, 6, 12))) {
                if (level.getBlockState(pos).is(Blocks.FIRE) || level.getBlockState(pos).is(Blocks.SOUL_FIRE)) {
                    level.removeBlock(pos, false);
                }
            }
        }
    }

    private static void placeDungeonStructure(ServerLevel level, BlockPos origin) {
        // The exported tier-one pieces are 48x48 and are authored facing out from their local origin.
        // Rotate them in-place around the 48x48 piece centre so every entrance faces the room centre.
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(false)
                .setRotation(Rotation.CLOCKWISE_180)
                .setRotationPivot(new BlockPos(24, 0, 24));
        for (DungeonStructurePiece piece : DUNGEON_PIECES) {
            Optional<StructureTemplate> template = loadDungeonPiece(level, piece);
            if (template.isEmpty()) {
                GatewayExpansion.LOGGER.error("Missing dungeon structure piece: {}", piece.templateId());
                continue;
            }

            // Rotation around (24, 24) is one block wider on the positive axes; offset back to retain the 96x96 layout.
            BlockPos placementOrigin = origin.offset(piece.xOffset() - 1, 0, piece.zOffset() - 1);
            boolean placed = template.get().placeInWorld(level, placementOrigin, placementOrigin, settings, level.random, 2);
            if (!placed) {
                GatewayExpansion.LOGGER.error("Failed to place dungeon structure piece {} at {}", piece.templateId(), placementOrigin);
            }
        }
    }

    private static void spawnPoiLootboxes(ServerLevel level, BlockPos origin, int floor, double quantityBonus) {
        spawnPoiLootboxes(level, origin, floor, quantityBonus, -1);
    }

    private static void spawnPoiLootboxes(ServerLevel level, BlockPos origin, int floor, double quantityBonus, int requestedBoxes) {
        int quantitySteps = Math.min(4, (int) Math.floor(Math.max(0.0D, quantityBonus) * 4.0D));
        int clusterCount = requestedBoxes > 0
                ? Math.max(1, (int) Math.ceil(requestedBoxes / (double) Math.max(1, 3 + quantitySteps)))
                : Math.min(6, 2 + Math.max(0, floor - 1) / 5 + quantitySteps);
        int boxesPerCluster = 3 + quantitySteps;
        int spawned = 0;
        int clusterRadius = 3 + quantitySteps * 2;
        for (int cluster = 0; cluster < clusterCount; cluster++) {
            int centerX = origin.getX() + level.random.nextInt((int) (MOB_SPAWN_RADIUS * 2.0D + 1.0D)) - (int) MOB_SPAWN_RADIUS;
            int centerZ = origin.getZ() + level.random.nextInt((int) (MOB_SPAWN_RADIUS * 2.0D + 1.0D)) - (int) MOB_SPAWN_RADIUS;
            for (int box = 0; box < boxesPerCluster; box++) {
                BlockPos surface = null;
                // Cluster members remain on the same valid floor regions used by wave mobs.
                for (int attempt = 0; attempt < 48 && surface == null; attempt++) {
                    int x = centerX + level.random.nextInt(clusterRadius * 2 + 1) - clusterRadius;
                    int z = centerZ + level.random.nextInt(clusterRadius * 2 + 1) - clusterRadius;
                    surface = findPoiSurface(level, origin, x, z);
                }
                if (surface == null) continue;
                BlockPos lootboxPos = surface.above();
                if (!level.getBlockState(lootboxPos).isAir()) continue;
                level.setBlock(lootboxPos, ModBlocks.POI_LOOTBOX.get().defaultBlockState(), 3);
                if (level.getBlockEntity(lootboxPos) instanceof PoiLootboxBlockEntity lootbox) {
                    lootbox.assignRandomRarity(level.random);
                    spawned++;
                    if (requestedBoxes > 0 && spawned >= requestedBoxes) {
                        return;
                    }
                }
            }
        }
    }

    private static BlockPos findPoiSurface(ServerLevel level, BlockPos origin, int x, int z) {
        for (int y : DUNGEON_SPAWN_SURFACE_Y) {
            BlockPos candidate = new BlockPos(x, y, z);
            if (!isCentralPillarPosition(origin, x + 0.5D, z + 0.5D)
                    && level.getBlockState(candidate).isFaceSturdy(level, candidate, net.minecraft.core.Direction.UP)
                    && level.getBlockState(candidate.above()).isAir()
                    && level.getBlockState(candidate.above(2)).isAir()) {
                return candidate;
            }
        }
        return null;
    }

    private static Optional<StructureTemplate> loadDungeonPiece(ServerLevel level, DungeonStructurePiece piece) {
        return loadStructure(level, piece.templateId());
    }

    private static Optional<StructureTemplate> loadStructure(ServerLevel level, String structureId) {
        ResourceLocation templateId = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, structureId);
        Optional<StructureTemplate> managedTemplate = level.getStructureManager().get(templateId);
        if (managedTemplate.isPresent()) {
            return managedTemplate;
        }

        ResourceLocation resourcePath = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "structures/" + structureId + ".nbt");
        try (InputStream stream = level.getServer().getResourceManager().open(resourcePath)) {
            CompoundTag tag = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            return Optional.of(level.getStructureManager().readStructure(tag));
        } catch (IOException exception) {
            GatewayExpansion.LOGGER.error("Could not load structure {}", resourcePath, exception);
            return Optional.empty();
        }
    }

    private static void clearDungeonVolume(ServerLevel level, BlockPos origin) {
        int minX = origin.getX() - DUNGEON_HALF_SPAN;
        int maxX = origin.getX() + DUNGEON_HALF_SPAN - 1;
        int minZ = origin.getZ() - DUNGEON_HALF_SPAN;
        int maxZ = origin.getZ() + DUNGEON_HALF_SPAN - 1;
        int minY = origin.getY();
        int maxY = origin.getY() + DUNGEON_CLEAR_HEIGHT - 1;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos clearPos = new BlockPos(x, y, z);
                    if (!level.getBlockState(clearPos).isAir()) {
                        level.setBlock(clearPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void clearDungeonEntities(ServerLevel level, BlockPos origin) {
        AABB bounds = new AABB(
                origin.getX() - INSTANCE_CLEANUP_RADIUS,
                origin.getY() - 32,
                origin.getZ() - INSTANCE_CLEANUP_RADIUS,
                origin.getX() + INSTANCE_CLEANUP_RADIUS,
                origin.getY() + 96,
                origin.getZ() + INSTANCE_CLEANUP_RADIUS);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, bounds, DungeonInstanceManager::isDungeonCleanupEntity)) {
            entity.discard();
        }
    }

    private static void showRebuildStatus(ServerLevel level, BlockPos origin, String message) {
        double maxDistance = (double) INSTANCE_CLEANUP_RADIUS * INSTANCE_CLEANUP_RADIUS;
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(origin) <= maxDistance) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), true);
            }
        }
    }

    private static void clearBrokenStructureDrops(ServerLevel level, BlockPos origin) {
        AABB bounds = new AABB(
                origin.getX() - INSTANCE_CLEANUP_RADIUS,
                origin.getY() - 32,
                origin.getZ() - INSTANCE_CLEANUP_RADIUS,
                origin.getX() + INSTANCE_CLEANUP_RADIUS,
                origin.getY() + 96,
                origin.getZ() + INSTANCE_CLEANUP_RADIUS);
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, bounds, DungeonInstanceManager::isBrokenStructureDrop)) {
            itemEntity.discard();
        }
    }

    private static int clearDungeonItems(ServerLevel level, BlockPos origin) {
        AABB bounds = new AABB(
                origin.getX() - INSTANCE_CLEANUP_RADIUS,
                origin.getY() - 32,
                origin.getZ() - INSTANCE_CLEANUP_RADIUS,
                origin.getX() + INSTANCE_CLEANUP_RADIUS,
                origin.getY() + 96,
                origin.getZ() + INSTANCE_CLEANUP_RADIUS);
        ArrayList<ItemEntity> items = new ArrayList<>(level.getEntitiesOfClass(ItemEntity.class, bounds, ItemEntity::isAlive));
        for (ItemEntity itemEntity : items) {
            itemEntity.discard();
        }
        return items.size();
    }

    private static boolean isBrokenStructureDrop(ItemEntity itemEntity) {
        return BROKEN_STRUCTURE_DROP.equals(BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem()));
    }

    private static boolean isDungeonCleanupEntity(Entity entity) {
        if (entity instanceof Player) {
            return false;
        }
        return true;
    }

    private record DungeonStructurePiece(String templateId, int xOffset, int zOffset) {
    }

    private enum InstanceLayout { DUNGEON, SHOP }
}
