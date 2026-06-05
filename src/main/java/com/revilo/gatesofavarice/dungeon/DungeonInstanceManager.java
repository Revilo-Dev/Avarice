package com.revilo.gatesofavarice.dungeon;

import com.revilo.gatesofavarice.GatewayExpansion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class DungeonInstanceManager {

    private static final int INSTANCE_SPACING = 512;
    private static final int PLATFORM_SIZE = 50;
    private static final int PLATFORM_HALF_SPAN = PLATFORM_SIZE / 2;
    private static final int PLATFORM_Y = 64;
    private static final int INSTANCE_CLEANUP_RADIUS = 96;
    private static final long PLATFORM_LIFETIME_TICKS = 20L * 120L;

    private static final BlockState PLATFORM_BLOCK = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final Map<BlockPos, Long> ACTIVE_PLATFORMS = new HashMap<>();

    private DungeonInstanceManager() {
    }

    public static boolean teleportToDungeonInstance(ServerPlayer player, UUID instanceOwnerId) {
        ServerLevel dungeonLevel = player.server.getLevel(ModDimensions.DUNGEON_LEVEL);
        if (dungeonLevel == null) {
            GatewayExpansion.LOGGER.error("Dungeon dimension is not available. Check dimension datapack registration.");
            return false;
        }

        BlockPos origin = instanceOrigin(instanceOwnerId);
        ensurePlatform(dungeonLevel, origin);

        player.teleportTo(dungeonLevel, origin.getX() + 0.5D, origin.getY() + 1.1D, origin.getZ() + 0.5D, player.getYRot(), player.getXRot());
        player.setPortalCooldown();
        return true;
    }

    public static boolean teleportToSavedLocation(ServerPlayer player, ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos pos, float yaw, float pitch) {
        ServerLevel destination = player.server.getLevel(dimension);
        if (destination == null) {
            return false;
        }
        player.teleportTo(destination, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, pitch);
        player.setPortalCooldown();
        return true;
    }

    public static BlockPos instanceCenter(UUID instanceOwnerId) {
        return instanceOrigin(instanceOwnerId);
    }

    public static void keepInstanceAlive(UUID instanceOwnerId, ServerLevel dungeonLevel) {
        if (dungeonLevel == null) {
            return;
        }
        BlockPos origin = instanceOrigin(instanceOwnerId);
        ensurePlatform(dungeonLevel, origin);
        ACTIVE_PLATFORMS.put(origin.immutable(), dungeonLevel.getGameTime() + PLATFORM_LIFETIME_TICKS);
    }

    public static void cleanupInstance(UUID instanceOwnerId, ServerLevel dungeonLevel) {
        if (dungeonLevel == null) {
            return;
        }

        BlockPos origin = instanceOrigin(instanceOwnerId);
        closeGatewaysInInstance(dungeonLevel, origin);
        clearPlatform(dungeonLevel, origin);
        ACTIVE_PLATFORMS.remove(origin);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel dungeonLevel = event.getServer().getLevel(ModDimensions.DUNGEON_LEVEL);
        if (dungeonLevel == null || ACTIVE_PLATFORMS.isEmpty()) {
            return;
        }

        long gameTime = dungeonLevel.getGameTime();
        ArrayList<BlockPos> expiredPlatforms = new ArrayList<>();
        for (Map.Entry<BlockPos, Long> entry : ACTIVE_PLATFORMS.entrySet()) {
            if (gameTime >= entry.getValue()) {
                closeGatewaysInInstance(dungeonLevel, entry.getKey());
                clearPlatform(dungeonLevel, entry.getKey());
                expiredPlatforms.add(entry.getKey());
            }
        }

        for (BlockPos platformOrigin : expiredPlatforms) {
            ACTIVE_PLATFORMS.remove(platformOrigin);
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

    private static void ensurePlatform(ServerLevel level, BlockPos origin) {
        long expiresAt = level.getGameTime() + PLATFORM_LIFETIME_TICKS;
        ACTIVE_PLATFORMS.put(origin.immutable(), expiresAt);

        int minX = origin.getX() - PLATFORM_HALF_SPAN;
        int maxX = minX + PLATFORM_SIZE - 1;
        int minZ = origin.getZ() - PLATFORM_HALF_SPAN;
        int maxZ = minZ + PLATFORM_SIZE - 1;
        int y = origin.getY();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos placePos = new BlockPos(x, y, z);
                if (!level.getBlockState(placePos).is(PLATFORM_BLOCK.getBlock())) {
                    level.setBlock(placePos, PLATFORM_BLOCK, 3);
                }
            }
        }
    }

    private static void clearPlatform(ServerLevel level, BlockPos origin) {
        int minX = origin.getX() - PLATFORM_HALF_SPAN;
        int maxX = minX + PLATFORM_SIZE - 1;
        int minZ = origin.getZ() - PLATFORM_HALF_SPAN;
        int maxZ = minZ + PLATFORM_SIZE - 1;
        int y = origin.getY();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos clearPos = new BlockPos(x, y, z);
                if (!level.getBlockState(clearPos).isAir()) {
                    level.setBlock(clearPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void closeGatewaysInInstance(ServerLevel level, BlockPos origin) {
        AABB bounds = new AABB(
                origin.getX() - INSTANCE_CLEANUP_RADIUS,
                origin.getY() - 32,
                origin.getZ() - INSTANCE_CLEANUP_RADIUS,
                origin.getX() + INSTANCE_CLEANUP_RADIUS,
                origin.getY() + 96,
                origin.getZ() + INSTANCE_CLEANUP_RADIUS);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, bounds, DungeonInstanceManager::isGatewayEntity)) {
            entity.discard();
        }
    }

    private static boolean isGatewayEntity(Entity entity) {
        net.minecraft.resources.ResourceLocation typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (typeId == null) {
            return false;
        }
        return "gateways".equals(typeId.getNamespace()) && typeId.getPath().contains("gateway");
    }
}
