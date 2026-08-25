package com.revilo.gatesofavarice.block.entity;

import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import com.revilo.gatesofavarice.config.GatewayExpansionConfig;
import com.revilo.gatesofavarice.integration.LevelUpIntegration;
import com.revilo.gatesofavarice.registry.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;
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
        // A crate can be placed at any tier, but its rewards must still respect player progression.
        // In particular, Lunarium is in the RARE table and cannot be obtained at level 1.
        PoiLootboxRarity effectiveRarity = rarity.upgrade(DungeonRunManager.getLootboxRarityBonus(player), level.random)
                .capForPlayerLevel(playerLevel);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(effectiveRarity.lootTable());
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .create(LootContextParamSets.CHEST);
        int rolls = effectiveRarity.baseRolls() + Math.max(0, DungeonRunManager.getDungeonPartySize(player) - 1)
                + DungeonRunManager.getLootboxAmountBonus(player)
                + Math.min(2, Math.max(0, floor - 1) / 10) + Math.min(1, Math.max(0, playerLevel) / 50);
        double quantityMultiplier = effectiveRarity.quantityMultiplier() + DungeonRunManager.getLootboxQuantityBonus(player)
                + Math.min(0.75D, Math.max(0, floor - 1) * 0.025D + Math.max(0, playerLevel) * 0.003D);
        for (int i = 0; i < rolls; i++) {
            List<ItemStack> rewards = table.getRandomItems(params, level.random.nextLong());
            for (ItemStack reward : rewards) {
                if (BuiltInRegistries.ITEM.getKey(reward.getItem()).getNamespace().equals("runic")
                        && level.random.nextDouble() > GatewayExpansionConfig.RUNE_LOOT_CRATE_DROP_CHANCE.get()) {
                    continue;
                }
                scaleCount(reward, quantityMultiplier, level.random);
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, reward);
            }
        }
        spawnRarityParticles(level, pos, effectiveRarity);
    }

    private static void spawnRarityParticles(ServerLevel level, BlockPos pos, PoiLootboxRarity rarity) {
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_PLANKS.defaultBlockState()).setPos(pos),
                pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D,
                64, 0.5D, 0.4D, 0.5D, 0.2D
        );
        level.playSound(null, pos, switch (rarity) {
            case RARE -> net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME;
            case EPIC -> net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP;
            case LEGENDARY -> net.minecraft.sounds.SoundEvents.TOTEM_USE;
            default -> net.minecraft.sounds.SoundEvents.CHEST_OPEN;
        }, net.minecraft.sounds.SoundSource.BLOCKS, rarity == PoiLootboxRarity.LEGENDARY ? 0.9F : 0.65F,
                rarity == PoiLootboxRarity.EPIC ? 1.25F : 1.0F);
        DustParticleOptions particle = switch (rarity) {
            case COMMON -> null;
            case UNCOMMON -> dust(0x57D65D);
            case RARE -> dust(0x55E8E8);
            case EPIC -> dust(0xB05CFF);
            case LEGENDARY -> dust(0xF6C64A);
        };
        if (particle == null) return;
        int count = 42 + rarity.ordinal() * 12;
        level.sendParticles(particle, pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D, count, 0.45D, 0.55D, 0.45D, 0.06D);
    }

    private static DustParticleOptions dust(int rgb) {
        return new DustParticleOptions(Vec3.fromRGB24(rgb).toVector3f(), 1.2F);
    }

    private static void scaleCount(ItemStack stack, double multiplier, RandomSource random) {
        // Runes are single, rare rewards; quantity bonuses must not duplicate them.
        if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("runic")) return;
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
        COMMON(55, "common", 4, 1.25D, 0),
        UNCOMMON(27, "uncommon", 4, 1.15D, 8),
        RARE(12, "rare", 3, 1.0D, 20),
        EPIC(5, "epic", 3, 0.9D, 35),
        LEGENDARY(1, "legendary", 2, 0.8D, 50);

        private final int weight;
        private final ResourceKey<LootTable> lootTable;
        private final int baseRolls;
        private final double quantityMultiplier;
        private final int requiredPlayerLevel;

        PoiLootboxRarity(int weight, String tableName, int baseRolls, double quantityMultiplier, int requiredPlayerLevel) {
            this.weight = weight;
            this.baseRolls = baseRolls;
            this.quantityMultiplier = quantityMultiplier;
            this.requiredPlayerLevel = requiredPlayerLevel;
            this.lootTable = ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath("gatesofavarice", "chests/poi/" + tableName));
        }

        private ResourceKey<LootTable> lootTable() {
            return lootTable;
        }

        private int baseRolls() {
            return baseRolls;
        }

        private double quantityMultiplier() {
            return quantityMultiplier;
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

        private PoiLootboxRarity capForPlayerLevel(int playerLevel) {
            PoiLootboxRarity allowed = COMMON;
            for (PoiLootboxRarity candidate : values()) {
                if (playerLevel >= candidate.requiredPlayerLevel) allowed = candidate;
            }
            return ordinal() > allowed.ordinal() ? allowed : this;
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
