package com.revilo.gatesofavarice.entity;

import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import com.revilo.gatesofavarice.integration.LevelUpIntegration;
import com.revilo.gatesofavarice.party.PartyManager;
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
    private static final String ADVANCE_PORTAL_KEY = "AdvancePortal";
    private static final String SHOP_PORTAL_KEY = "ShopPortal";
    private static final String GUARANTEED_OWNER_LOADOUT_KEY = "GuaranteedOwnerLoadout";
    private static final double DEFAULT_INTERACTION_HORIZONTAL_INFLATE = 0.8D;
    private static final double DEFAULT_INTERACTION_VERTICAL_INFLATE = 0.2D;
    private static final double DUNGEON_WARP_PORTAL_VISUAL_SCALE = 5.0D;
    private static final EntityDataAccessor<Integer> CRYSTAL_TIER = SynchedEntityData.defineId(GatewayCrystalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RETURN_PORTAL = SynchedEntityData.defineId(GatewayCrystalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ADVANCE_PORTAL = SynchedEntityData.defineId(GatewayCrystalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOP_PORTAL = SynchedEntityData.defineId(GatewayCrystalEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID ownerId;
    private int lifeTicks;
    private boolean guaranteedOwnerLoadout;

    public GatewayCrystalEntity(EntityType<? extends GatewayCrystalEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public GatewayCrystalEntity(Level level) {
        this(ModEntities.GATEWAY_CRYSTAL.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CRYSTAL_TIER, 1);
        builder.define(RETURN_PORTAL, false);
        builder.define(ADVANCE_PORTAL, false);
        builder.define(SHOP_PORTAL, false);
    }

    @Override
    public void tick() {
        super.tick();
        this.lifeTicks++;

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!this.isReturnPortal() && !this.isAdvancePortal() && this.lifeTicks >= MAX_LIFETIME_TICKS) {
            this.discard();
            return;
        }

        if ((this.tickCount & 3) != 0) {
            return;
        }

        List<ServerPlayer> players = serverLevel.getEntitiesOfClass(ServerPlayer.class, this.interactionBounds(),
                player -> !player.isSpectator() && !player.isPassenger() && !player.isOnPortalCooldown());
        // A portal transfer has one entrant.  Processing every nearby player let a host
        // standing beside a joining client get transferred first on multiplayer servers.
        ServerPlayer player = players.stream()
                .min(java.util.Comparator.comparingDouble(candidate -> candidate.distanceToSqr(this)))
                .orElse(null);
        if (player == null) return;

        UUID runOwnerId = this.ownerId == null ? player.getUUID() : this.ownerId;
        if (this.isShopPortal()) {
            DungeonRunManager.enterShopThroughGateway(player, runOwnerId, this);
        } else if (this.isAdvancePortal()) {
            DungeonRunManager.advanceThroughFloorGateway(player, runOwnerId, this);
        } else if (this.isReturnPortal()) {
            DungeonRunManager.exitViaBailPortal(player, runOwnerId, this);
        } else {
            if (this.ownerId != null && !PartyManager.canEnterDungeon(player, this.ownerId)) {
                player.displayClientMessage(Component.literal("unable to join this players gate, join their party first"), true);
                player.setPortalCooldown();
                return;
            }
            if (DungeonRunManager.enterFromGateway(player, runOwnerId, this.guaranteedOwnerLoadout)) {
                player.setPortalCooldown();
                if (!PartyManager.isInParty(player)) {
                    this.discard();
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifeTicks = tag.getInt(LIFE_KEY);
        if (tag.hasUUID(OWNER_KEY)) {
            this.ownerId = tag.getUUID(OWNER_KEY);
        }
        this.setReturnPortal(tag.getBoolean(RETURN_PORTAL_KEY));
        this.setAdvancePortal(tag.getBoolean(ADVANCE_PORTAL_KEY));
        this.setShopPortal(tag.getBoolean(SHOP_PORTAL_KEY));
        this.guaranteedOwnerLoadout = tag.getBoolean(GUARANTEED_OWNER_LOADOUT_KEY);
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
        tag.putBoolean(RETURN_PORTAL_KEY, this.isReturnPortal());
        tag.putBoolean(ADVANCE_PORTAL_KEY, this.isAdvancePortal());
        tag.putBoolean(SHOP_PORTAL_KEY, this.isShopPortal());
        tag.putBoolean(GUARANTEED_OWNER_LOADOUT_KEY, this.guaranteedOwnerLoadout);
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
        return this.getEntityData().get(RETURN_PORTAL);
    }

    public void setReturnPortal(boolean returnPortal) {
        this.getEntityData().set(RETURN_PORTAL, returnPortal);
    }

    public boolean isAdvancePortal() {
        return this.getEntityData().get(ADVANCE_PORTAL);
    }

    public void setAdvancePortal(boolean advancePortal) {
        this.getEntityData().set(ADVANCE_PORTAL, advancePortal);
    }

    public boolean isShopPortal() {
        return this.getEntityData().get(SHOP_PORTAL);
    }

    public void setShopPortal(boolean shopPortal) {
        this.getEntityData().set(SHOP_PORTAL, shopPortal);
    }

    public void setGuaranteedOwnerLoadout(boolean guaranteedOwnerLoadout) {
        this.guaranteedOwnerLoadout = guaranteedOwnerLoadout;
    }

    private net.minecraft.world.phys.AABB interactionBounds() {
        if (this.isReturnPortal() || this.isAdvancePortal()) {
            double visualSize = this.getBbHeight() * DUNGEON_WARP_PORTAL_VISUAL_SCALE;
            double horizontalInflate = Math.max(0.0D, (visualSize - this.getBbWidth()) * 0.5D);
            double verticalInflate = Math.max(0.0D, (visualSize - this.getBbHeight()) * 0.5D);
            return this.getBoundingBox().inflate(horizontalInflate, verticalInflate, horizontalInflate);
        }
        return this.getBoundingBox().inflate(DEFAULT_INTERACTION_HORIZONTAL_INFLATE, DEFAULT_INTERACTION_VERTICAL_INFLATE, DEFAULT_INTERACTION_HORIZONTAL_INFLATE);
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
