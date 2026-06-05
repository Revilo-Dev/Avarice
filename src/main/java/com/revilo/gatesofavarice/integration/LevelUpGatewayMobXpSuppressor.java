package com.revilo.gatesofavarice.integration;

import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import com.revilo.gatesofavarice.dungeon.ModDimensions;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class LevelUpGatewayMobXpSuppressor {

    private static final String LEVELUP_XP_ORB_CLASS = "com.revilo.levelup.entity.LevelUpXpOrbEntity";
    private static final String GATEWAY_BREACH_SPAWN_KEY = "gatesofavarice.gateway_breach_spawn";
    private static final long RECENT_DEATH_WINDOW_TICKS = 2L;
    private static final double MATCH_RADIUS = 2.0D;
    private static final ArrayDeque<SuppressedDeath> RECENT_SUPPRESSED_DEATHS = new ArrayDeque<>();

    private LevelUpGatewayMobXpSuppressor() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof Mob mob && isGatewayTrackedMob(mob)) {
            mob.skipDropExperience();
            return;
        }

        if (!LevelUpIntegration.isLoaded() || !isLevelUpXpOrb(entity)) {
            return;
        }

        pruneSuppressedDeaths(serverLevel);
        if (serverLevel.dimension() == ModDimensions.DUNGEON_LEVEL || matchesSuppressedDeath(serverLevel, entity.position())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGatewayMobDeathStart(LivingDeathEvent event) {
        if (!LevelUpIntegration.isLoaded() || !(event.getEntity().level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (!isGatewayTrackedMob(entity)) {
            return;
        }

        RECENT_SUPPRESSED_DEATHS.addLast(new SuppressedDeath(serverLevel.dimension(), entity.position(), serverLevel.getGameTime()));
        pruneSuppressedDeaths(serverLevel);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGatewayMobDeathEnd(LivingDeathEvent event) {
        if (!LevelUpIntegration.isLoaded() || !(event.getEntity().level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (!isGatewayTrackedMob(entity)) {
            return;
        }

        AABB searchBox = entity.getBoundingBox().inflate(MATCH_RADIUS);
        for (Entity nearby : serverLevel.getEntities(entity, searchBox, LevelUpGatewayMobXpSuppressor::isLevelUpXpOrb)) {
            nearby.discard();
        }
    }

    private static boolean isGatewayTrackedMob(LivingEntity entity) {
        return entity instanceof Mob
                && (GatewayEntity.getOwner(entity) != null
                || entity.getPersistentData().getBoolean(GATEWAY_BREACH_SPAWN_KEY)
                || entity.getPersistentData().getBoolean(DungeonRunManager.DUNGEON_WAVE_SPAWN_KEY));
    }

    private static boolean isLevelUpXpOrb(Entity entity) {
        return entity != null && LEVELUP_XP_ORB_CLASS.equals(entity.getClass().getName());
    }

    private static boolean matchesSuppressedDeath(ServerLevel level, Vec3 orbPosition) {
        for (SuppressedDeath death : RECENT_SUPPRESSED_DEATHS) {
            if (!Objects.equals(death.dimension(), level.dimension())) {
                continue;
            }
            if (death.position().distanceToSqr(orbPosition) <= MATCH_RADIUS * MATCH_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private static void pruneSuppressedDeaths(ServerLevel level) {
        long minTick = level.getGameTime() - RECENT_DEATH_WINDOW_TICKS;
        Iterator<SuppressedDeath> iterator = RECENT_SUPPRESSED_DEATHS.iterator();
        while (iterator.hasNext()) {
            SuppressedDeath death = iterator.next();
            if (death.gameTime() < minTick || !Objects.equals(death.dimension(), level.dimension())) {
                iterator.remove();
            }
        }
    }

    private record SuppressedDeath(ResourceKey<Level> dimension, Vec3 position, long gameTime) {
    }
}
