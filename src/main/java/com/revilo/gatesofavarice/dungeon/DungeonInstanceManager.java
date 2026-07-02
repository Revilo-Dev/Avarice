package com.revilo.gatesofavarice.dungeon;

import com.revilo.gatesofavarice.GatewayExpansion;
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
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class DungeonInstanceManager {

    private static final int INSTANCE_SPACING = 512;
    private static final int PLATFORM_Y = 64;
    private static final int DUNGEON_HALF_SPAN = 32;
    private static final int DUNGEON_CLEAR_HEIGHT = 80;
    private static final int INSTANCE_CLEANUP_RADIUS = 96;
    private static final long PLATFORM_LIFETIME_TICKS = 20L * 120L;
    private static final Vec3 PLAYER_SPAWN_OFFSET = new Vec3(29.0D, 2.0D, 0.0D);
    private static final Vec3 EXIT_PORTAL_POSITION = new Vec3(4983779.5D, 65.0D, -4828671.5D);
    private static final Vec3 SHOPKEEPER_OFFSET = new Vec3(0.0D, 4.0D, 0.0D);
    private static final double MOB_SPAWN_Y_OFFSET = 2.0D;
    private static final double MOB_SPAWN_RADIUS = 12.0D;
    private static final int CENTRAL_PILLAR_MIN_X_OFFSET = -5;
    private static final int CENTRAL_PILLAR_MAX_X_OFFSET = 4;
    private static final int CENTRAL_PILLAR_MIN_Z_OFFSET = -5;
    private static final int CENTRAL_PILLAR_MAX_Z_OFFSET = 4;
    private static final float PLAYER_SPAWN_YAW = 90.0F;
    private static final ResourceLocation BROKEN_STRUCTURE_DROP = ResourceLocation.fromNamespaceAndPath("chipped", "iron_bowl_soul_lantern");

    private static final DungeonStructurePiece[] DUNGEON_PIECES = {
            new DungeonStructurePiece("dungeon-nw", -32, -32),
            new DungeonStructurePiece("dungeon-ne", 0, -32),
            new DungeonStructurePiece("dungeon-sw", -32, 0),
            new DungeonStructurePiece("dungeon-se", 0, 0)
    };
    private static final Map<BlockPos, Long> ACTIVE_DUNGEONS = new HashMap<>();

    private DungeonInstanceManager() {
    }

    public static boolean teleportToDungeonInstance(ServerPlayer player, UUID instanceOwnerId) {
        ServerLevel dungeonLevel = player.server.getLevel(ModDimensions.DUNGEON_LEVEL);
        if (dungeonLevel == null) {
            GatewayExpansion.LOGGER.error("Dungeon dimension is not available. Check dimension datapack registration.");
            return false;
        }

        BlockPos origin = instanceOrigin(instanceOwnerId);
        ensureDungeon(dungeonLevel, origin);
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

    public static BlockPos instanceCenter(UUID instanceOwnerId) {
        return instanceOrigin(instanceOwnerId);
    }

    public static Vec3 exitPortalPosition(UUID instanceOwnerId) {
        return EXIT_PORTAL_POSITION;
    }

    public static Vec3 shopkeeperPosition(UUID instanceOwnerId) {
        return positionedOffset(instanceOwnerId, SHOPKEEPER_OFFSET);
    }

    public static Vec3 randomMobSpawnPosition(UUID instanceOwnerId, net.minecraft.util.RandomSource random) {
        BlockPos origin = instanceOrigin(instanceOwnerId);
        for (int attempt = 0; attempt < 24; attempt++) {
            double x = origin.getX() + 0.5D + (random.nextDouble() * MOB_SPAWN_RADIUS * 2.0D - MOB_SPAWN_RADIUS);
            double z = origin.getZ() + 0.5D + (random.nextDouble() * MOB_SPAWN_RADIUS * 2.0D - MOB_SPAWN_RADIUS);
            if (!isCentralPillarPosition(origin, x, z)) {
                return new Vec3(x, origin.getY() + MOB_SPAWN_Y_OFFSET, z);
            }
        }
        return new Vec3(origin.getX() + MOB_SPAWN_RADIUS + 0.5D, origin.getY() + MOB_SPAWN_Y_OFFSET, origin.getZ() + 0.5D);
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
        ensureDungeon(dungeonLevel, origin);
        ACTIVE_DUNGEONS.put(origin.immutable(), dungeonLevel.getGameTime() + PLATFORM_LIFETIME_TICKS);
    }

    public static void cleanupInstance(UUID instanceOwnerId, ServerLevel dungeonLevel) {
        if (dungeonLevel == null) {
            return;
        }

        BlockPos origin = instanceOrigin(instanceOwnerId);
        clearDungeonEntities(dungeonLevel, origin);
        clearDungeonVolume(dungeonLevel, origin);
        ACTIVE_DUNGEONS.remove(origin);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel dungeonLevel = event.getServer().getLevel(ModDimensions.DUNGEON_LEVEL);
        if (dungeonLevel == null || ACTIVE_DUNGEONS.isEmpty()) {
            return;
        }

        long gameTime = dungeonLevel.getGameTime();
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

    private static void ensureDungeon(ServerLevel level, BlockPos origin) {
        boolean alreadyActive = ACTIVE_DUNGEONS.containsKey(origin);
        long expiresAt = level.getGameTime() + PLATFORM_LIFETIME_TICKS;
        ACTIVE_DUNGEONS.put(origin.immutable(), expiresAt);

        if (alreadyActive) {
            return;
        }

        clearDungeonEntities(level, origin);
        clearDungeonVolume(level, origin);
        placeDungeonStructure(level, origin);
        clearBrokenStructureDrops(level, origin);
    }

    private static void placeDungeonStructure(ServerLevel level, BlockPos origin) {
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(false);
        for (DungeonStructurePiece piece : DUNGEON_PIECES) {
            Optional<StructureTemplate> template = loadDungeonPiece(level, piece);
            if (template.isEmpty()) {
                GatewayExpansion.LOGGER.error("Missing dungeon structure piece: {}", piece.templateId());
                continue;
            }

            BlockPos placementOrigin = origin.offset(piece.xOffset(), 0, piece.zOffset());
            boolean placed = template.get().placeInWorld(level, placementOrigin, placementOrigin, settings, level.random, 2);
            if (!placed) {
                GatewayExpansion.LOGGER.error("Failed to place dungeon structure piece {} at {}", piece.templateId(), placementOrigin);
            }
        }
    }

    private static Optional<StructureTemplate> loadDungeonPiece(ServerLevel level, DungeonStructurePiece piece) {
        ResourceLocation templateId = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, piece.templateId());
        Optional<StructureTemplate> managedTemplate = level.getStructureManager().get(templateId);
        if (managedTemplate.isPresent()) {
            return managedTemplate;
        }

        ResourceLocation resourcePath = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "structures/" + piece.templateId() + ".nbt");
        try (InputStream stream = level.getServer().getResourceManager().open(resourcePath)) {
            CompoundTag tag = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            return Optional.of(level.getStructureManager().readStructure(tag));
        } catch (IOException exception) {
            GatewayExpansion.LOGGER.error("Could not load dungeon structure piece {}", resourcePath, exception);
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
}
