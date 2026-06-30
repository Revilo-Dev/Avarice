package com.revilo.gatesofavarice.entity;

import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import com.revilo.gatesofavarice.integration.LevelUpIntegration;
import com.revilo.gatesofavarice.registry.ModEntities;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class GatewayCrystalEntity extends Entity {

    private static final int MAX_LIFETIME_TICKS = 20 * 90;
    private static final String OWNER_KEY = "Owner";
    private static final String LIFE_KEY = "Life";
    private static final String RETURN_PORTAL_KEY = "ReturnPortal";
    private static final EntityDataAccessor<Integer> CRYSTAL_TIER = SynchedEntityData.defineId(GatewayCrystalEntity.class, EntityDataSerializers.INT);

    private UUID ownerId;
    private int lifeTicks;
    private boolean returnPortal;

    public GatewayCrystalEntity(EntityType<? extends GatewayCrystalEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setCustomName(Component.translatable("entity.gatesofavarice.gateway_crystal"));
        this.setCustomNameVisible(true);
    }

    public GatewayCrystalEntity(Level level) {
        this(ModEntities.GATEWAY_CRYSTAL.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CRYSTAL_TIER, 1);
    }

    @Override
    public void tick() {
        super.tick();
        this.lifeTicks++;

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.lifeTicks >= MAX_LIFETIME_TICKS) {
            this.discard();
            return;
        }

        if ((this.tickCount & 3) != 0) {
            return;
        }

        List<ServerPlayer> players = serverLevel.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(0.2D),
                player -> !player.isSpectator() && !player.isPassenger() && !player.isOnPortalCooldown());
        for (ServerPlayer player : players) {
            UUID runOwnerId = this.ownerId == null ? player.getUUID() : this.ownerId;
            if (this.returnPortal) {
                DungeonRunManager.exitViaBailPortal(player, runOwnerId, this);
            } else {
                if (!canUseTier(player, this.getCrystalTier())) {
                    denyTierAccess(player, this.getCrystalTier());
                    continue;
                }
                DungeonRunManager.enterFromGateway(player, runOwnerId);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifeTicks = tag.getInt(LIFE_KEY);
        if (tag.hasUUID(OWNER_KEY)) {
            this.ownerId = tag.getUUID(OWNER_KEY);
        }
        this.returnPortal = tag.getBoolean(RETURN_PORTAL_KEY);
        if (tag.contains("CrystalTier")) {
            this.setCrystalTier(tag.getInt("CrystalTier"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt(LIFE_KEY, this.lifeTicks);
        if (this.ownerId != null) {
            tag.putUUID(OWNER_KEY, this.ownerId);
        }
        tag.putBoolean(RETURN_PORTAL_KEY, this.returnPortal);
        tag.putInt("CrystalTier", this.getCrystalTier());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    public int getCrystalTier() {
        return this.getEntityData().get(CRYSTAL_TIER);
    }

    public void setCrystalTier(int crystalTier) {
        this.getEntityData().set(CRYSTAL_TIER, Math.max(1, crystalTier));
    }

    public UUID getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public boolean isReturnPortal() {
        return this.returnPortal;
    }

    public void setReturnPortal(boolean returnPortal) {
        this.returnPortal = returnPortal;
    }

    public static boolean canUseTier(ServerPlayer player, int crystalTier) {
        return LevelUpIntegration.getEffectiveLevel(player) >= minLevelForTier(crystalTier);
    }

    public static int minLevelForTier(int crystalTier) {
        return switch (Math.max(1, crystalTier)) {
            case 1 -> 0;
            case 2 -> 20;
            case 3 -> 50;
            case 4 -> 70;
            default -> 90;
        };
    }

    public static void denyTierAccess(ServerPlayer player, int crystalTier) {
        int requiredLevel = minLevelForTier(crystalTier);
        player.displayClientMessage(Component.literal("Requires level " + requiredLevel + " to enter this gateway."), true);
        player.setPortalCooldown();
    }
}
