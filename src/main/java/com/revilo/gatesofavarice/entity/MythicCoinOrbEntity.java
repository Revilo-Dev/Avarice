package com.revilo.gatesofavarice.entity;

import com.revilo.gatesofavarice.currency.MythicCoinWallet;
import com.revilo.gatesofavarice.registry.ModEntities;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MythicCoinOrbEntity extends Entity {
    private static final String VALUE_KEY = "Value";
    private static final String AGE_KEY = "Age";
    private static final int MAX_AGE = 20 * 45;
    private static final double PICKUP_RANGE = 0.75D;
    private static final double ATTRACT_RANGE = 5.5D;
    private static final double MAX_SPEED = 0.28D;
    private static final EntityDataAccessor<Integer> VALUE = SynchedEntityData.defineId(MythicCoinOrbEntity.class, EntityDataSerializers.INT);

    private int age;

    public MythicCoinOrbEntity(EntityType<? extends MythicCoinOrbEntity> entityType, Level level) {
        super(entityType, level);
    }

    public MythicCoinOrbEntity(Level level, double x, double y, double z, int value) {
        this(ModEntities.MYTHIC_COIN_ORB.get(), level);
        this.setPos(x, y, z);
        this.setValue(value);
        this.setDeltaMovement(
                (this.random.nextDouble() - 0.5D) * 0.18D,
                0.18D + this.random.nextDouble() * 0.08D,
                (this.random.nextDouble() - 0.5D) * 0.18D);
    }

    public static void spawn(ServerLevel level, double x, double y, double z, int value) {
        if (value <= 0) {
            return;
        }
        level.addFreshEntity(new MythicCoinOrbEntity(level, x, y, z, value));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(VALUE, 1);
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;
        if (this.age >= MAX_AGE) {
            this.discard();
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        if (!this.isNoGravity()) {
            motion = motion.add(0.0D, -0.018D, 0.0D);
        }

        Player nearest = this.level().getNearestPlayer(this, ATTRACT_RANGE);
        if (nearest instanceof ServerPlayer serverPlayer && serverPlayer.isAlive() && !serverPlayer.isSpectator()) {
            Vec3 target = new Vec3(
                    serverPlayer.getX() - this.getX(),
                    serverPlayer.getY() + serverPlayer.getEyeHeight() * 0.45D - this.getY(),
                    serverPlayer.getZ() - this.getZ());
            double distance = target.length();
            if (distance <= PICKUP_RANGE) {
                collect(serverPlayer);
                return;
            }
            if (distance > 0.001D) {
                double distanceFactor = 1.0D - Math.min(distance / ATTRACT_RANGE, 1.0D);
                Vec3 pull = target.normalize().scale(0.018D + distanceFactor * 0.045D);
                motion = motion.scale(0.92D).add(pull);
                if (motion.lengthSqr() > MAX_SPEED * MAX_SPEED) {
                    motion = motion.normalize().scale(MAX_SPEED);
                }
            }
        }

        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.75D, -0.35D, 0.75D));
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.98D, 0.98D, 0.98D));
        }
    }

    @Override
    public void playerTouch(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            collect(serverPlayer);
        }
    }

    private void collect(ServerPlayer player) {
        if (this.level().isClientSide) {
            return;
        }
        int value = this.getValue();
        if (value <= 0) {
            this.discard();
            return;
        }
        MythicCoinWallet.add(player, value);
        player.take(this, 1);
        this.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 1.0F + this.random.nextFloat() * 0.2F);
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt(AGE_KEY);
        this.setValue(tag.getInt(VALUE_KEY));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt(AGE_KEY, this.age);
        tag.putInt(VALUE_KEY, this.getValue());
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

    @Override
    public boolean isPickable() {
        return false;
    }

    public int getValue() {
        return Math.max(1, this.getEntityData().get(VALUE));
    }

    public void setValue(int value) {
        this.getEntityData().set(VALUE, Mth.clamp(value, 1, Integer.MAX_VALUE));
    }
}
