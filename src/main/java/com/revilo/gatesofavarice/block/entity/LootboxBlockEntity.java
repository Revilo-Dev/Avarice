package com.revilo.gatesofavarice.block.entity;

import com.revilo.gatesofavarice.integration.LevelUpIntegration;
import com.revilo.gatesofavarice.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class LootboxBlockEntity extends BlockEntity {

    private static final String ROOT_KEY = "gatesofavarice";
    private static final String LOOT_KEY = "lootbox_loot";
    private static final String LEVEL_ORBS_KEY = "lootbox_level_orbs";
    private static final ResourceLocation DUNGEON_LOOTBOX_XP_SOURCE = ResourceLocation.fromNamespaceAndPath("levelup", "dungeon_lootbox_orbs");
    private NonNullList<ItemStack> loot = NonNullList.create();
    private int storedLevelOrbs;

    public LootboxBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.LOOTBOX.get(), pos, blockState);
    }

    public void readFromItemStack(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(ROOT_KEY);
        this.loot = NonNullList.create();
        this.storedLevelOrbs = root.getInt(LEVEL_ORBS_KEY);
        if (this.level != null && root.contains(LOOT_KEY, 9)) {
            net.minecraft.nbt.ListTag list = root.getList(LOOT_KEY, 10);
            for (int i = 0; i < list.size(); i++) {
                ItemStack parsed = ItemStack.parseOptional(this.level.registryAccess(), list.getCompound(i));
                if (!parsed.isEmpty()) this.loot.add(parsed);
            }
        }
        setChanged();
    }

    public void writeToItemStack(ItemStack stack) {
        CompoundTag all = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag root = all.getCompound(ROOT_KEY);
        if (this.level != null) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (ItemStack entry : this.loot) {
                if (!entry.isEmpty()) list.add(entry.saveOptional(this.level.registryAccess()));
            }
            root.put(LOOT_KEY, list);
            root.putInt(LEVEL_ORBS_KEY, this.storedLevelOrbs);
            all.put(ROOT_KEY, root);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(all));
        }
    }

    public void setLoot(NonNullList<ItemStack> loot) {
        this.loot = loot;
        setChanged();
    }

    public void setStoredLevelOrbs(int storedLevelOrbs) {
        this.storedLevelOrbs = Math.max(0, storedLevelOrbs);
        setChanged();
    }

    public void burstLoot(ServerLevel level, BlockPos pos, ServerPlayer player) {
        spawnOpenParticles(level, pos);
        for (ItemStack stack : this.loot) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, stack.copy());
            }
        }
        if (this.storedLevelOrbs > 0) {
            ExperienceOrb.award(level, Vec3.atCenterOf(pos).add(0.0D, 1.0D, 0.0D), this.storedLevelOrbs);
            LevelUpIntegration.awardXp(player, this.storedLevelOrbs, DUNGEON_LOOTBOX_XP_SOURCE);
        }
        this.loot.clear();
        this.storedLevelOrbs = 0;
        setChanged();
    }

    private static void spawnOpenParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_PLANKS.defaultBlockState()).setPos(pos),
                pos.getX() + 0.5D,
                pos.getY() + 0.7D,
                pos.getZ() + 0.5D,
                48,
                0.45D,
                0.35D,
                0.45D,
                0.18D
        );
    }
}
