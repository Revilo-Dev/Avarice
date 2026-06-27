package com.revilo.gatesofavarice.entity;

import com.revilo.gatesofavarice.menu.ShopkeeperMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GatekeeperEntity extends PathfinderMob {

    public GatekeeperEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setNoGravity(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public void push(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public void travel(Vec3 travelVector) {
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new ShopkeeperMenu(containerId, inventory, this.getId()),
                Component.translatable("entity.gatesofavarice.shopkeeper"));
        serverPlayer.openMenu(provider, buffer -> buffer.writeInt(this.getId()));
        return InteractionResult.CONSUME;
    }
}
