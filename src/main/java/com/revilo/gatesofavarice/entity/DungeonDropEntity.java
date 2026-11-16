package com.revilo.gatesofavarice.entity;

import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import com.revilo.gatesofavarice.dungeon.DungeonWeaponEquipment;
import com.revilo.gatesofavarice.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import java.util.UUID;

/** Floating dungeon-only loot. It intentionally is not an ItemEntity. */
public final class DungeonDropEntity extends Entity {
    private static final String STACK_KEY = "Stack";
    private static final String ARROW_KEY = "ArrowBundle";
    private static final EntityDataAccessor<ItemStack> STACK = SynchedEntityData.defineId(DungeonDropEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> ARROW_BUNDLE = SynchedEntityData.defineId(DungeonDropEntity.class, EntityDataSerializers.BOOLEAN);
    private int age;
    private UUID swappingPlayerId;
    private int sneakTicks;

    public DungeonDropEntity(EntityType<? extends DungeonDropEntity> type, Level level) {
        super(type, level);
    }

    private DungeonDropEntity(Level level, double x, double y, double z, ItemStack stack, boolean arrowBundle) {
        this(ModEntities.DUNGEON_DROP.get(), level);
        this.setPos(x, y, z);
        this.getEntityData().set(STACK, stack.copy());
        this.getEntityData().set(ARROW_BUNDLE, arrowBundle);
        this.setDeltaMovement((this.random.nextDouble() - .5D) * .14D, .16D, (this.random.nextDouble() - .5D) * .14D);
    }

    public static void spawnWeapon(ServerLevel level, double x, double y, double z, ItemStack stack) {
        if (!stack.isEmpty()) level.addFreshEntity(new DungeonDropEntity(level, x, y, z, stack, false));
    }

    public static void spawnArrows(ServerLevel level, double x, double y, double z, ItemStack stack) {
        if (!stack.isEmpty()) level.addFreshEntity(new DungeonDropEntity(level, x, y, z, stack, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STACK, ItemStack.EMPTY);
        builder.define(ARROW_BUNDLE, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (++this.age > 20 * 90) { discard(); return; }
        this.setDeltaMovement(this.getDeltaMovement().add(0, -0.02D, 0).scale(.96D));
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) this.setDeltaMovement(this.getDeltaMovement().multiply(.7D, -.3D, .7D));
        if (this.level().isClientSide() && !this.isArrowBundle() && this.tickCount % 6 == 0) {
            for (int index = 0; index < 12; index++) {
                double angle = Math.PI * 2.0D * index / 12.0D;
                this.level().addParticle(ParticleTypes.END_ROD,
                        this.getX() + Math.cos(angle) * .55D, this.getY() + .03D, this.getZ() + Math.sin(angle) * .55D,
                        0.0D, .003D, 0.0D);
            }
        }
        if (!this.level().isClientSide()) {
            if (swappingPlayerId != null && this.level() instanceof ServerLevel level) {
                ServerPlayer swappingPlayer = level.getServer().getPlayerList().getPlayer(swappingPlayerId);
                if (swappingPlayer == null || !swappingPlayer.isShiftKeyDown() || swappingPlayer.distanceToSqr(this) > 1.0D) {
                    swappingPlayerId = null;
                    sneakTicks = 0;
                }
            }
            Player player = this.level().getNearestPlayer(this, 1.0D);
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.isAlive()) collect(serverPlayer);
        }
    }

    @Override public void playerTouch(Player player) { if (player instanceof ServerPlayer serverPlayer) collect(serverPlayer); }

    private void collect(ServerPlayer player) {
        ItemStack stack = stack();
        if (stack.isEmpty()) { discard(); return; }
        if (isArrowBundle()) {
            DungeonRunManager.addArrowBundle(player, stack);
        } else {
            if (!player.isShiftKeyDown()) {
                swappingPlayerId = null;
                sneakTicks = 0;
                return;
            }
            if (!player.getUUID().equals(swappingPlayerId)) {
                swappingPlayerId = player.getUUID();
                sneakTicks = 0;
            }
            if (++sneakTicks < 60) return;
            DungeonWeaponEquipment.equip(player, stack);
        }
        player.take(this, 1);
        this.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .3F, 1.0F);
        discard();
    }

    public ItemStack stack() { return this.getEntityData().get(STACK); }
    public boolean isArrowBundle() { return this.getEntityData().get(ARROW_BUNDLE); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { this.age = tag.getInt("Age"); this.getEntityData().set(STACK, ItemStack.parse(this.registryAccess(), tag.getCompound(STACK_KEY)).orElse(ItemStack.EMPTY)); this.getEntityData().set(ARROW_BUNDLE, tag.getBoolean(ARROW_KEY)); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { tag.putInt("Age", age); tag.put(STACK_KEY, stack().save(this.registryAccess())); tag.putBoolean(ARROW_KEY, isArrowBundle()); }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) { return false; }
    @Override public boolean isPickable() { return false; }
}
