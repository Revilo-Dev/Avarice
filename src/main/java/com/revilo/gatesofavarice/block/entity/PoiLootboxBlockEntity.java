package com.revilo.gatesofavarice.block.entity;

import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import com.revilo.gatesofavarice.integration.LevelUpIntegration;
import com.revilo.gatesofavarice.registry.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

public class PoiLootboxBlockEntity extends BlockEntity {
    private static final String RARITY_KEY = "poi_lootbox_rarity";
    private PoiLootboxRarity rarity = PoiLootboxRarity.COMMON;

    public PoiLootboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POI_LOOTBOX.get(), pos, state);
    }

    public void assignRandomRarity(RandomSource random) {
        this.rarity = PoiLootboxRarity.roll(random);
        setChanged();
    }

    public void open(ServerLevel level, BlockPos pos, ServerPlayer player) {
        int floor = DungeonRunManager.getCurrentFloor(player);
        int playerLevel = LevelUpIntegration.getEffectiveLevel(player);
        PoiLootboxRarity effectiveRarity = rarity.upgrade(DungeonRunManager.getLootboxRarityBonus(player), level.random);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(effectiveRarity.lootTable());
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .create(LootContextParamSets.CHEST);
        int rolls = DungeonRunManager.getDungeonPartySize(player) + DungeonRunManager.getLootboxAmountBonus(player)
                + Math.min(2, Math.max(0, floor - 1) / 10) + Math.min(1, Math.max(0, playerLevel) / 50);
        double quantityMultiplier = 1.0D + DungeonRunManager.getLootboxQuantityBonus(player)
                + Math.min(0.75D, Math.max(0, floor - 1) * 0.025D + Math.max(0, playerLevel) * 0.003D);
        for (int i = 0; i < rolls; i++) {
            List<ItemStack> rewards = table.getRandomItems(params, level.random.nextLong());
            for (ItemStack reward : rewards) {
                scaleCount(reward, quantityMultiplier, level.random);
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, reward);
            }
        }
        spawnRarityParticles(level, pos, effectiveRarity);
    }

    private static void spawnRarityParticles(ServerLevel level, BlockPos pos, PoiLootboxRarity rarity) {
        net.minecraft.core.particles.ParticleOptions particle = switch (rarity) {
            case COMMON -> ParticleTypes.HAPPY_VILLAGER;
            case UNCOMMON -> ParticleTypes.ENCHANT;
            case RARE -> ParticleTypes.END_ROD;
            case EPIC -> ParticleTypes.DRAGON_BREATH;
            case LEGENDARY -> ParticleTypes.TOTEM_OF_UNDYING;
        };
        int count = 12 + rarity.ordinal() * 8;
        level.sendParticles(particle, pos.getX() + 0.5D, pos.getY() + 0.65D, pos.getZ() + 0.5D, count, 0.35D, 0.45D, 0.35D, 0.04D);
    }

    private static void scaleCount(ItemStack stack, double multiplier, RandomSource random) {
        double scaled = stack.getCount() * multiplier;
        int count = (int) Math.floor(scaled);
        if (random.nextDouble() < scaled - count) count++;
        stack.setCount(Math.min(stack.getMaxStackSize(), Math.max(1, count)));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(RARITY_KEY, rarity.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        rarity = PoiLootboxRarity.byName(tag.getString(RARITY_KEY));
    }

    public enum PoiLootboxRarity {
        COMMON(55, "common"), UNCOMMON(27, "uncommon"), RARE(12, "rare"), EPIC(5, "epic"), LEGENDARY(1, "legendary");

        private final int weight;
        private final ResourceKey<LootTable> lootTable;

        PoiLootboxRarity(int weight, String tableName) {
            this.weight = weight;
            this.lootTable = ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath("gatesofavarice", "chests/poi/" + tableName));
        }

        private ResourceKey<LootTable> lootTable() {
            return lootTable;
        }

        private static PoiLootboxRarity roll(RandomSource random) {
            int roll = random.nextInt(100);
            int accumulated = 0;
            for (PoiLootboxRarity rarity : values()) {
                accumulated += rarity.weight;
                if (roll < accumulated) return rarity;
            }
            return COMMON;
        }

        private PoiLootboxRarity upgrade(double rarityBonus, RandomSource random) {
            int steps = (int) Math.floor(Math.max(0.0D, rarityBonus) * 4.0D);
            if (random.nextDouble() < Math.max(0.0D, rarityBonus) * 4.0D - steps) steps++;
            return values()[Math.min(values().length - 1, ordinal() + steps)];
        }

        private static PoiLootboxRarity byName(String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException ignored) {
                return COMMON;
            }
        }
    }
}
