package com.revilo.gatesofavarice.gateway.builder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.gateway.GatewayThemeProfile;
import com.revilo.gatesofavarice.gateway.pool.EnemyPoolRegistry;
import com.revilo.gatesofavarice.gateway.pool.EnemyPoolRole;
import com.revilo.gatesofavarice.gateway.pool.EnemyPoolSet;
import com.revilo.gatesofavarice.gateway.roll.ForgeEffect;
import com.revilo.gatesofavarice.gateway.roll.ForgeEffectType;
import com.revilo.gatesofavarice.integration.LevelUpIntegration;
import com.revilo.gatesofavarice.integration.ModCompat;
import com.revilo.gatesofavarice.item.CrystalItem;
import com.revilo.gatesofavarice.item.data.CrystalForgeData;
import com.revilo.gatesofavarice.item.data.GatewayCardData;
import com.revilo.gatesofavarice.item.data.CrystalTheme;
import com.revilo.gatesofavarice.registry.ModItems;
import com.revilo.gatesofavarice.workbench.GatewayWorkbenchSlots;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.gate.Failure;
import dev.shadowsoffire.gateways.gate.BossEventSettings;
import dev.shadowsoffire.gateways.gate.GateRules;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.gate.Reward;
import dev.shadowsoffire.gateways.gate.StandardWaveEntity;
import dev.shadowsoffire.gateways.gate.Wave;
import dev.shadowsoffire.gateways.gate.WaveModifier;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import dev.shadowsoffire.gateways.item.GatePearlItem;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import dev.shadowsoffire.placebo.reload.ReloadListenerPayloads;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GatewayForgeService {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String ROOT_KEY = GatewayExpansion.MOD_ID;
    private static final String GATEWAY_ID_KEY = "gateway_id";
    private static final String GATEWAY_JSON_KEY = "gateway_json";
    private static final String GATEWAY_JSON_COMPRESSED_KEY = "gateway_json_gzip";
    private static final String DIFFICULTY_KEY = "difficulty";
    private static final String THEME_KEY = "theme";
    private static final String LEVEL_KEY = "level";
    private static final String PLAYER_LEVEL_KEY = "player_level";
    private static final String OVERLEVELED_KEY = "overleveled";
    private static final String SUMMARY_KEY = "summary";
    private static final String DISPLAY_NAME_KEY = "display_name";
    private static final String TIER_KEY = "tier";
    private static final String FLAT_COIN_BONUS_KEY = "flat_coin_bonus";
    private static final String COIN_REWARD_MULTIPLIER_KEY = "coin_reward_multiplier";
    private static final String LEVEL_XP_MULTIPLIER_KEY = "level_xp_multiplier";
    private static final String EXPERIENCE_REWARD_MULTIPLIER_KEY = "experience_reward_multiplier";
    private static final String THORNS_DAMAGE_KEY = "thorns_damage";
    private static final Map<ResourceLocation, String> GENERATED_GATEWAY_NAMES = new HashMap<>();
    private static final Map<ResourceLocation, Integer> GENERATED_GATEWAY_TIERS = new HashMap<>();
    private static final Map<ResourceLocation, Integer> GENERATED_GATEWAY_LEVELS = new HashMap<>();
    private static final Map<ResourceLocation, Integer> GENERATED_GATEWAY_FLAT_COIN_BONUSES = new HashMap<>();
    private static final Map<ResourceLocation, Double> GENERATED_GATEWAY_COIN_MULTIPLIERS = new HashMap<>();
    private static final Map<ResourceLocation, Double> GENERATED_GATEWAY_LEVEL_XP_MULTIPLIERS = new HashMap<>();
    private static final Map<ResourceLocation, Double> GENERATED_GATEWAY_EXPERIENCE_MULTIPLIERS = new HashMap<>();
    private static final Map<ResourceLocation, Double> GENERATED_GATEWAY_THORNS_DAMAGE = new HashMap<>();
    private static final ItemStack[] LEATHER_ARMOR_SET = createArmorSet(Items.LEATHER_BOOTS, Items.LEATHER_LEGGINGS, Items.LEATHER_CHESTPLATE, Items.LEATHER_HELMET);
    private static final ItemStack[] CHAINMAIL_ARMOR_SET = createArmorSet(Items.CHAINMAIL_BOOTS, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_HELMET);
    private static final ItemStack[] IRON_ARMOR_SET = createArmorSet(Items.IRON_BOOTS, Items.IRON_LEGGINGS, Items.IRON_CHESTPLATE, Items.IRON_HELMET);
    private static final ItemStack[] DIAMOND_ARMOR_SET = createArmorSet(Items.DIAMOND_BOOTS, Items.DIAMOND_LEGGINGS, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_HELMET);
    private static final ItemStack[] NETHERITE_ARMOR_SET = createArmorSet(Items.NETHERITE_BOOTS, Items.NETHERITE_LEGGINGS, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_HELMET);
    private static final ItemStack[] SWORD_SET = createWeaponSet(Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);
    private static final ItemStack[] AXE_SET = createWeaponSet(Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE);

    private GatewayForgeService() {
    }

    public static boolean canForge(net.minecraft.world.entity.player.Player player, Container container) {
        ItemStack crystal = container.getItem(GatewayWorkbenchSlots.CRYSTAL_SLOT);
        if (!(crystal.getItem() instanceof CrystalItem crystalItem)) {
            return false;
        }
        if (!container.getItem(GatewayWorkbenchSlots.OUTPUT_SLOT).isEmpty()) {
            return false;
        }
        if (player == null) {
            return true;
        }

        CrystalForgeData.CrystalProfile profile = CrystalForgeData.getProfile(
                crystal,
                crystalItem.crystalTier().minLevel(),
                crystalItem.crystalTier().maxLevel()
        );
        return !LevelUpIntegration.isCrystalOverleveled(player, profile.level());
    }

    public static ItemStack forge(ServerPlayer player, Container container) {
        ItemStack crystalStack = container.getItem(GatewayWorkbenchSlots.CRYSTAL_SLOT);
        if (!(crystalStack.getItem() instanceof CrystalItem crystalItem)) {
            throw new IllegalStateException("Cannot forge without a crystal.");
        }

        CrystalForgeData.CrystalProfile currentProfile = CrystalForgeData.getProfile(
                crystalStack,
                crystalItem.crystalTier().minLevel(),
                crystalItem.crystalTier().maxLevel()
        );
        if (LevelUpIntegration.isCrystalOverleveled(player, currentProfile.level())) {
            throw new IllegalStateException("Cannot forge an overleveled crystal.");
        }

        int playerLevel = LevelUpIntegration.getPlayerLevel(player);
        CrystalForgeData.CrystalProfile profile = CrystalForgeData.syncLevelToPlayer(
                crystalStack,
                crystalItem.crystalTier().minLevel(),
                crystalItem.crystalTier().maxLevel(),
                playerLevel,
                player.serverLevel().random
        );

        ForgeState state = buildState(container, crystalItem.crystalTier(), profile);
        boolean overleveled = playerLevel >= 0 && profile.level() > playerLevel;

        applyFinalRandomStage(state);
        GatewayBuildResult result = generateGateway(state, playerLevel, overleveled);
        registerGeneratedGateway(result.gatewayId(), result.gateway());
        GENERATED_GATEWAY_NAMES.put(result.gatewayId(), result.name());
        GENERATED_GATEWAY_TIERS.put(result.gatewayId(), result.crystalTier());
        GENERATED_GATEWAY_LEVELS.put(result.gatewayId(), result.crystalLevel());
        GENERATED_GATEWAY_FLAT_COIN_BONUSES.put(result.gatewayId(), result.flatCoinBonus());
        GENERATED_GATEWAY_COIN_MULTIPLIERS.put(result.gatewayId(), result.coinRewardMultiplier());
        GENERATED_GATEWAY_LEVEL_XP_MULTIPLIERS.put(result.gatewayId(), result.levelXpMultiplier());
        GENERATED_GATEWAY_EXPERIENCE_MULTIPLIERS.put(result.gatewayId(), result.experienceRewardMultiplier());
        GENERATED_GATEWAY_THORNS_DAMAGE.put(result.gatewayId(), result.thornsDamage());
        persistGeneratedGateway(player.serverLevel(), result);
        syncGatewayRegistry(player);

        ItemStack pearl = createPearl(result);
        consumeInputs(container);
        container.setItem(GatewayWorkbenchSlots.OUTPUT_SLOT, pearl);
        container.setChanged();
        GatewayExpansion.LOGGER.debug("Forged gateway {} for {}", result.gatewayId(), player.getScoreboardName());
        GatewayExpansion.LOGGER.debug("Gateway JSON:\n{}", result.gatewayJson());
        return pearl;
    }

    public static boolean restoreGatewayFromPearl(ItemStack stack) {
        return restoreGatewayFromPearl(stack, null);
    }

    public static boolean restoreGatewayFromPearl(ItemStack stack, ServerLevel serverLevel) {
        CompoundTag root = getRootTag(stack);
        if (!root.contains(GATEWAY_ID_KEY)) {
            return false;
        }

        ResourceLocation gatewayId = ResourceLocation.parse(root.getString(GATEWAY_ID_KEY));
        if (!containsGatewayJson(root)) {
            if (serverLevel != null) {
                GeneratedGatewayStorage.StoredGateway stored = GeneratedGatewayStorage.get(serverLevel).entries().get(gatewayId);
                if (stored != null && stored.json() != null && !stored.json().isBlank()) {
                    writeGatewayJsonToPearl(stack, stored.json());
                    restoreGeneratedGateway(
                            gatewayId,
                            stored.json(),
                            stored.displayName(),
                            stored.crystalTier(),
                            stored.crystalLevel(),
                            stored.flatCoinBonus(),
                            stored.coinRewardMultiplier(),
                            stored.levelXpMultiplier(),
                            stored.experienceRewardMultiplier(),
                            stored.thornsDamage());
                }
            }
            return bindPearlIfResolved(stack, root, gatewayId);
        }
        int crystalLevel = root.contains(LEVEL_KEY) ? root.getInt(LEVEL_KEY) : 0;
        int crystalTier = root.contains(TIER_KEY) ? root.getInt(TIER_KEY) : inferCrystalTierFromLevel(crystalLevel);
        int flatCoinBonus = root.contains(FLAT_COIN_BONUS_KEY) ? root.getInt(FLAT_COIN_BONUS_KEY) : 0;
        double coinRewardMultiplier = root.contains(COIN_REWARD_MULTIPLIER_KEY) ? root.getDouble(COIN_REWARD_MULTIPLIER_KEY) : 1.0D;
        double levelXpMultiplier = root.contains(LEVEL_XP_MULTIPLIER_KEY) ? root.getDouble(LEVEL_XP_MULTIPLIER_KEY) : 1.0D;
        double experienceRewardMultiplier = root.contains(EXPERIENCE_REWARD_MULTIPLIER_KEY) ? root.getDouble(EXPERIENCE_REWARD_MULTIPLIER_KEY) : 1.0D;
        double thornsDamage = root.contains(THORNS_DAMAGE_KEY) ? root.getDouble(THORNS_DAMAGE_KEY) : 0.0D;
        String displayName = root.contains(DISPLAY_NAME_KEY) ? root.getString(DISPLAY_NAME_KEY) : "Lv " + crystalLevel + " Gateway";
        String gatewayJson = readGatewayJson(root);
        if (gatewayJson == null || gatewayJson.isBlank()) {
            return false;
        }
        if (serverLevel != null) {
            persistGeneratedGateway(serverLevel, gatewayId, gatewayJson, displayName, crystalTier, crystalLevel, flatCoinBonus, coinRewardMultiplier, levelXpMultiplier,
                    experienceRewardMultiplier, thornsDamage);
        }
        restoreGeneratedGateway(gatewayId, gatewayJson, displayName, crystalTier, crystalLevel, flatCoinBonus, coinRewardMultiplier, levelXpMultiplier,
                experienceRewardMultiplier, thornsDamage);
        return bindPearlIfResolved(stack, root, gatewayId);
    }

    private static void writeGatewayJsonToPearl(ItemStack stack, String gatewayJson) {
        if (gatewayJson == null || gatewayJson.isBlank()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag root = tag.getCompound(ROOT_KEY);
            writeGatewayJson(root, gatewayJson);
            tag.put(ROOT_KEY, root);
        });
    }

    private static boolean containsGatewayJson(CompoundTag root) {
        return root.contains(GATEWAY_JSON_COMPRESSED_KEY) || root.contains(GATEWAY_JSON_KEY);
    }

    private static void writeGatewayJson(CompoundTag root, String gatewayJson) {
        root.remove(GATEWAY_JSON_COMPRESSED_KEY);
        root.remove(GATEWAY_JSON_KEY);

        byte[] compressed = compressGatewayJson(gatewayJson);
        if (compressed != null && compressed.length > 0) {
            root.putByteArray(GATEWAY_JSON_COMPRESSED_KEY, compressed);
        } else if (gatewayJson != null) {
            root.putString(GATEWAY_JSON_KEY, gatewayJson);
        }
    }

    private static String readGatewayJson(CompoundTag root) {
        if (root.contains(GATEWAY_JSON_COMPRESSED_KEY)) {
            String decompressed = decompressGatewayJson(root.getByteArray(GATEWAY_JSON_COMPRESSED_KEY));
            if (decompressed != null && !decompressed.isBlank()) {
                return decompressed;
            }
        }
        return root.contains(GATEWAY_JSON_KEY) ? root.getString(GATEWAY_JSON_KEY) : null;
    }

    private static byte[] compressGatewayJson(String gatewayJson) {
        if (gatewayJson == null || gatewayJson.isBlank()) {
            return null;
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
                gzip.write(gatewayJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            return output.toByteArray();
        } catch (IOException ignored) {
            return null;
        }
    }

    private static String decompressGatewayJson(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            gzip.transferTo(output);
            return output.toString(java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return null;
        }
    }

    public static void syncGatewayRegistry(ServerPlayer player) {
        for (ServerPlayer target : player.getServer().getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(target, new ReloadListenerPayloads.Start("gateways"));
            for (ResourceLocation key : GatewayRegistry.INSTANCE.getKeys()) {
                Gateway gateway = GatewayRegistry.INSTANCE.getValue(key);
                if (gateway != null) {
                    PacketDistributor.sendToPlayer(target, new ReloadListenerPayloads.Content<>("gateways", key, gateway));
                }
            }
            PacketDistributor.sendToPlayer(target, new ReloadListenerPayloads.End("gateways"));
        }
    }

    private static ForgeState buildState(Container container, CrystalItem.CrystalTier tier, CrystalForgeData.CrystalProfile profile) {
        ForgeState state = new ForgeState(tier, profile);
        applyThemeIdentity(state);
        applyCardModifiers(state, container.getItem(GatewayWorkbenchSlots.CRYSTAL_SLOT));
        state.finish();
        return state;
    }

    private static void applyCardModifiers(ForgeState state, ItemStack crystal) {
        for (GatewayCardData.CardModifier card : CrystalForgeData.readCards(crystal)) {
            double value = card.value();
            switch (card.type()) {
                case STAT -> {
                    state.damageMultiplier -= value * 0.45D;
                    state.speedMultiplier -= value * 0.35D;
                }
                case DAMAGE -> {
                    state.healthMultiplier -= value * 0.60D;
                    state.damageMultiplier -= value * 0.20D;
                }
                case EFFECT -> {
                    state.damageMultiplier -= value * 0.35D;
                }
                case ABILITY -> {
                    state.healthMultiplier -= value * 0.40D;
                    state.rewardMultiplier += value * 0.25D;
                }
                case RARITY -> {
                    state.rarityRewardMultiplier *= 1.0D + value;
                    state.rewardMultiplier += value;
                }
                case CHALLENGE -> {
                    state.mobSpawnMultiplier += value;
                    state.damageMultiplier += value * 0.55D;
                    state.healthMultiplier += value * 0.35D;
                    state.rewardMultiplier += value * 1.35D;
                    state.rarityRewardMultiplier *= 1.0D + value * 0.75D;
                    state.difficultyEstimate += Math.max(1, (int) Math.round(value * 100.0D));
                }
            }
            state.finalRollSummary.add("Deck: " + card.summary());
        }
    }

    private static void applyThemeIdentity(ForgeState state) {
        switch (state.profile.theme()) {
            case UNDEAD -> {
                state.rewardMultiplier += 0.18D;
                state.waveTimeDelta += 40;
                state.difficultyEstimate -= 10;
            }
            case ARCANE -> {
                state.levelXpMultiplier *= 1.35D;
                state.experienceRewardMultiplier *= 1.35D;
                state.rangedPacks += 1;
                state.assassinPacks += 1;
                state.mixedPackCount += 1;
                state.difficultyEstimate += 16;
            }
            case NETHER -> {
                state.rarityRewardMultiplier *= 1.28D;
                state.damageMultiplier += 0.08D;
                state.difficultyEstimate += 8;
            }
            case RAIDER -> {
                state.coinRewardMultiplier *= 1.40D;
                state.projectileDamage += 0.10D;
                state.difficultyEstimate += 4;
            }
            case WILD -> {
                state.rewardMultiplier += 0.12D;
                state.rarityRewardMultiplier *= 1.15D;
                state.rangedPacks += 1;
                state.tankPacks += 1;
                state.hoardPacks += 1;
                state.assassinPacks += 1;
                state.mixedPackCount += 2;
                state.difficultyEstimate += 18;
            }
        }
    }

    private static void applyFinalRandomStage(ForgeState state) {
        RandomSource random = RandomSource.create(state.profile.seed() ^ 0x51A7L);
        GatewayThemeProfile theme = GatewayThemeProfile.forTheme(state.profile.theme(), state.profile.level());
        int effectRolls = state.profile.level() >= 34 ? 2 : 1;
        for (int i = 0; i < effectRolls; i++) {
            ForgeEffect effect = switch (state.profile.theme()) {
                case UNDEAD -> random.nextBoolean()
                        ? ForgeEffect.of(ForgeEffectType.THEMED_REINFORCEMENTS, 1, "Final roll: undead reinforcements")
                        : ForgeEffect.ref(ForgeEffectType.MOB_EFFECT, ResourceLocation.withDefaultNamespace("strength"), 0, 0.0D, "Final roll: stronger undead");
                case BEAST -> random.nextBoolean()
                        ? ForgeEffect.of(ForgeEffectType.SPEED_MULTIPLIER, 0.08D, "Final roll: feral speed")
                        : ForgeEffect.ref(ForgeEffectType.MOB_EFFECT, ResourceLocation.withDefaultNamespace("jump_boost"), 0, 0.0D, "Final roll: leaping beasts");
                case ARCANE -> random.nextBoolean()
                        ? ForgeEffect.of(ForgeEffectType.RANGED_PACKS, 1, "Final roll: extra ranged pressure")
                        : ForgeEffect.ref(ForgeEffectType.MOB_EFFECT, ResourceLocation.withDefaultNamespace("glowing"), 0, 0.0D, "Final roll: revealing magic");
                case NETHER -> random.nextBoolean()
                        ? ForgeEffect.ref(ForgeEffectType.MOB_EFFECT, ResourceLocation.withDefaultNamespace("fire_resistance"), 0, 0.0D, "Final roll: infernal resilience")
                        : ForgeEffect.of(ForgeEffectType.DAMAGE_MULTIPLIER, 0.08D, "Final roll: hellfire damage");
                case RAIDER -> random.nextBoolean()
                        ? ForgeEffect.of(ForgeEffectType.RANGED_PACKS, 1, "Final roll: coordinated volley")
                        : ForgeEffect.ref(ForgeEffectType.MOB_EFFECT, ResourceLocation.withDefaultNamespace("hero_of_the_village"), 0, 0.0D, "Final roll: raider momentum");
                case WILD -> switch (random.nextInt(4)) {
                    case 0 -> ForgeEffect.of(ForgeEffectType.ARCHER_PACKS, 1, "Final roll: wild archer surge");
                    case 1 -> ForgeEffect.of(ForgeEffectType.HOARD_PACKS, 1, "Final roll: wild horde surge");
                    case 2 -> ForgeEffect.of(ForgeEffectType.TANK_PACKS, 1, "Final roll: wild tank surge");
                    default -> ForgeEffect.of(ForgeEffectType.ASSASSIN_PACKS, 1, "Final roll: wild assassin surge");
                };
            };
            applyEffect(state, effect);
            state.finalRollSummary.add(effect.description());
        }
        ForgeEffect rewardRoll = random.nextBoolean()
                ? ForgeEffect.of(ForgeEffectType.REWARD_MULTIPLIER, 0.08D + Math.min(0.10D, state.profile.level() / 250.0D), "Final roll: enhanced payout")
                : ForgeEffect.of(ForgeEffectType.EXTRA_FINAL_REWARD_ROLLS, 1, "Final roll: extra final reward");
        applyEffect(state, rewardRoll);
        state.finalRollSummary.add(rewardRoll.description());
        if (state.profile.level() >= 30 && random.nextFloat() < 0.45F) {
            ForgeEffect highRoll = ForgeEffect.of(ForgeEffectType.MINIBOSS_CHANCE, 0.15D, "Final roll: themed miniboss chance");
            applyEffect(state, highRoll);
            state.finalRollSummary.add(highRoll.description());
        }
        if (state.profile.level() >= 36 && random.nextFloat() < 0.35F) {
            ForgeEffect highRoll = ForgeEffect.ref(ForgeEffectType.MOB_EFFECT, theme.effects().get(random.nextInt(theme.effects().size())), 0, 0.0D, "Final roll: thematic effect");
            applyEffect(state, highRoll);
            state.finalRollSummary.add(highRoll.description());
        }
    }

    private static GatewayBuildResult generateGateway(ForgeState state, int playerLevel, boolean overleveled) {
        GatewayThemeProfile theme = GatewayThemeProfile.forTheme(state.profile.theme(), state.profile.level());
        GatewayThemeProfile.GateTypeProfile gateType = theme.gateType(state.profile.seed());
        EnemyPoolSet enemyPools = EnemyPoolRegistry.create(state.profile.theme(), state.profile.level());
        RandomSource random = RandomSource.create(state.profile.seed());
        int waveCount = state.waveCount();
        int bossWaveIndex = selectBossWaveIndex(state, enemyPools, random);
        boolean bossWaveEnabled = bossWaveIndex >= 0;
        List<Wave> waves = new ArrayList<>();
        for (int waveIndex = 0; waveIndex < waveCount; waveIndex++) {
            if (waveIndex == bossWaveIndex) {
                waves.add(buildBossWave(state, theme, enemyPools, random, waveIndex, waveCount));
                continue;
            }

            Wave.Builder builder = Wave.builder();
            WaveComposition composition = buildWaveComposition(state, enemyPools, random, waveIndex, waveCount);
            int totalEnemies = 0;
            double totalStrength = 0.0D;
            int raiderIllusionersThisWave = countFriendsAndFoesIllusioners(composition.entities());
            for (PlannedWaveEntity planned : composition.entities()) {
                builder.entity(configureWaveEntity(StandardWaveEntity.builder(planned.type()), planned.type(), state, planned.archetype())
                        .count(planned.count())
                        .addModifiers(adjustModifiersForEntity(planned.type(), state, planned.modifiers()))
                        .finalizeSpawn(true)
                        .build());
                totalEnemies += planned.count();
                totalStrength += planned.threatCost();
            }
            if (state.supportInterval > 0 && ((waveIndex + 1) % state.supportInterval == 0)) {
                for (int i = 0; i < state.supportCount; i++) {
                    EntityType<?> supportType = pickEnemy(enemyPools, random, EnemyPoolRole.SUPPORT, EnemyPoolRole.MELEE);
                    int supportCount = 1 + random.nextInt(2);
                    supportCount = clampRaiderIllusionerCount(state, supportType, waveIndex, waveCount, supportCount, raiderIllusionersThisWave);
                    if (supportCount <= 0) {
                        continue;
                    }
                    builder.entity(configureWaveEntity(StandardWaveEntity.builder(supportType), supportType, state, WaveArchetype.HOARD)
                            .count(supportCount)
                            .addModifiers(adjustModifiersForEntity(supportType, state, waveModifiers(state, WaveArchetype.HOARD, waveIndex)))
                            .finalizeSpawn(true)
                            .build());
                    totalEnemies += supportCount;
                    totalStrength += supportCount * perMobThreatCost(state, waveIndex, WaveArchetype.HOARD);
                    if (isFriendsAndFoesIllusioner(supportType)) {
                        raiderIllusionersThisWave += supportCount;
                    }
                }
            }
            if (state.rangedPacks > 0 && random.nextFloat() < 0.45F + (0.10F * state.rangedPacks)) {
                EntityType<?> rangedType = pickEnemy(enemyPools, random, EnemyPoolRole.RANGED, EnemyPoolRole.MELEE);
                int rangedCount = 1 + state.rangedPacks + random.nextInt(2);
                rangedCount = clampRaiderIllusionerCount(state, rangedType, waveIndex, waveCount, rangedCount, raiderIllusionersThisWave);
                if (rangedCount > 0) {
                builder.entity(configureWaveEntity(StandardWaveEntity.builder(rangedType), rangedType, state, WaveArchetype.ARCHER)
                        .count(rangedCount)
                        .addModifiers(adjustModifiersForEntity(rangedType, state, waveModifiers(state, WaveArchetype.ARCHER, waveIndex)))
                        .finalizeSpawn(true)
                        .build());
                totalEnemies += rangedCount;
                totalStrength += rangedCount * perMobThreatCost(state, waveIndex, WaveArchetype.ARCHER);
                    if (isFriendsAndFoesIllusioner(rangedType)) {
                        raiderIllusionersThisWave += rangedCount;
                    }
                }
            }
            if (state.mixedPackCount > 0 && random.nextFloat() < 0.35F) {
                EntityType<?> mixedType = pickEnemy(enemyPools, random, EnemyPoolRole.FAST, EnemyPoolRole.SUPPORT);
                int mixedCount = Math.max(1, state.mixedPackCount);
                mixedCount = clampRaiderIllusionerCount(state, mixedType, waveIndex, waveCount, mixedCount, raiderIllusionersThisWave);
                if (mixedCount > 0) {
                builder.entity(configureWaveEntity(StandardWaveEntity.builder(mixedType), mixedType, state, WaveArchetype.ASSASSIN)
                        .count(mixedCount)
                        .addModifiers(adjustModifiersForEntity(mixedType, state, waveModifiers(state, WaveArchetype.ASSASSIN, waveIndex)))
                        .finalizeSpawn(true)
                        .build());
                totalEnemies += mixedCount;
                totalStrength += mixedCount * perMobThreatCost(state, waveIndex, WaveArchetype.ASSASSIN);
                    if (isFriendsAndFoesIllusioner(mixedType)) {
                        raiderIllusionersThisWave += mixedCount;
                    }
                }
            }
            if (state.reinforcementRolls > 0 && random.nextFloat() < 0.25F * state.reinforcementRolls) {
                EntityType<?> reinforcementType = pickEnemy(enemyPools, random, EnemyPoolRole.TANK, EnemyPoolRole.MELEE);
                int reinforcementCount = 1 + state.reinforcementRolls;
                reinforcementCount = clampRaiderIllusionerCount(state, reinforcementType, waveIndex, waveCount, reinforcementCount, raiderIllusionersThisWave);
                if (reinforcementCount > 0) {
                builder.entity(configureWaveEntity(StandardWaveEntity.builder(reinforcementType), reinforcementType, state, WaveArchetype.TANK)
                        .count(reinforcementCount)
                        .addModifiers(adjustModifiersForEntity(reinforcementType, state, waveModifiers(state, WaveArchetype.TANK, waveIndex)))
                        .finalizeSpawn(true)
                        .build());
                totalEnemies += reinforcementCount;
                totalStrength += reinforcementCount * perMobThreatCost(state, waveIndex, WaveArchetype.TANK);
                    if (isFriendsAndFoesIllusioner(reinforcementType)) {
                        raiderIllusionersThisWave += reinforcementCount;
                    }
                }
            }
            if (shouldAddElite(state, waveIndex, random)) {
                int eliteCount = 1 + (waveIndex == waveCount - 1 ? state.finalWaveEliteCount : 0);
                for (int i = 0; i < eliteCount; i++) {
                    EntityType<?> eliteType = pickEnemy(enemyPools, random, EnemyPoolRole.ELITE, EnemyPoolRole.MELEE);
                    int allowed = clampRaiderIllusionerCount(state, eliteType, waveIndex, waveCount, 1, raiderIllusionersThisWave);
                    if (allowed <= 0) {
                        continue;
                    }
                    builder.entity(configureWaveEntity(StandardWaveEntity.builder(eliteType), eliteType, state).count(1).addModifiers(adjustModifiersForEntity(eliteType, state, eliteModifiers(state, random))).finalizeSpawn(true).build());
                    totalEnemies += 1;
                    totalStrength += 4.25D;
                    if (isFriendsAndFoesIllusioner(eliteType)) {
                        raiderIllusionersThisWave += 1;
                    }
                }
            }
            if (waveIndex >= waveCount - 2 && random.nextFloat() < state.minibossChance) {
                EntityType<?> minibossType = pickEnemy(enemyPools, random, EnemyPoolRole.ELITE, EnemyPoolRole.TANK);
                int allowed = clampRaiderIllusionerCount(state, minibossType, waveIndex, waveCount, 1, raiderIllusionersThisWave);
                if (allowed > 0) {
                builder.entity(configureWaveEntity(StandardWaveEntity.builder(minibossType), minibossType, state).count(1).addModifiers(adjustModifiersForEntity(minibossType, state, minibossModifiers(state))).finalizeSpawn(true).build());
                totalEnemies += 1;
                totalStrength += 6.0D;
                    if (isFriendsAndFoesIllusioner(minibossType)) {
                        raiderIllusionersThisWave += 1;
                    }
                }
            }
            for (WaveModifier modifier : globalWaveModifiers(state)) {
                builder.modifier(modifier);
            }
            addWaveRewardDrops(builder, state, theme, waveIndex, waveCount, random);
            builder.reward(new Reward.ExperienceReward(waveExperienceReward(state), 5));
            builder.maxWaveTime(computeWaveTimeLimit(state, waveIndex, waveCount, totalEnemies, totalStrength));
            builder.setupTime(computeSetupTime(state));
            waves.add(builder.build());
        }

        List<Reward> rewards = buildRewards(state, theme, gateType, bossWaveEnabled);
        List<Failure> failures = buildFailures(state);
        GateRules rules = GateRules.builder()
                .spawnRange(8.0D + state.crystalTier.tier())
                .leashRange(32.0D + state.crystalTier.tier() * 2.0D)
                .removeOnFailure(true)
                .requiresNearbyPlayer(true)
                .followRangeBoost(24.0D + state.profile.level() / 3.0D)
                .lives(bossWaveEnabled ? 1 : Math.max(1, 4 - state.crystalTier.tier() / 2))
                .build();
        NormalGateway gateway = NormalGateway.builder()
                .size(gatewaySize(state.crystalTier.tier()))
                .color(theme.color())
                .waves(waves)
                .keyRewards(rewards)
                .failures(failures)
                .rules(rules)
                .bossSettings(new BossEventSettings(BossEventSettings.Mode.NAME_PLATE, false))
                .build();
        validate(waves, rewards);
        if (bossWaveEnabled && !state.finalRollSummary.contains("Boss encounter")) {
            state.finalRollSummary.add("Boss encounter");
        }

        ResourceLocation gatewayId = ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "forged_" + UUID.randomUUID().toString().replace('-', '_'));
        String gatewayJson = serializeGateway(gateway);
        List<String> debugLines = List.of(
                "theme=" + state.profile.theme(),
                "gateType=" + gateType.id(),
                "level=" + state.profile.level(),
                "difficulty=" + difficultyLabel(state.difficultyEstimate, state.profile.level()),
                "waves=" + waveCount,
                "bossWave=" + bossWaveEnabled,
                "rewardMultiplier=" + state.rewardMultiplier,
                "coinMultiplier=" + state.coinRewardMultiplier,
                "levelXpMultiplier=" + state.levelXpMultiplier,
                "experienceMultiplier=" + state.experienceRewardMultiplier,
                "finalRolls=" + state.finalRollSummary
        );
        return new GatewayBuildResult(
                gatewayId,
                generateName(state, theme, gateType),
                state.profile.theme(),
                theme.color(),
                state.profile.level(),
                state.crystalTier.tier(),
                playerLevel,
                overleveled,
                state.waveCount(),
                state.difficultyEstimate,
                state.rewardMultiplier,
                state.flatCoinBonus,
                state.coinRewardMultiplier,
                state.levelXpMultiplier,
                state.experienceRewardMultiplier,
                state.thornsDamage,
                state.rarityRewardMultiplier,
                Math.max(0, state.rareRewardRolls),
                Math.max(1, state.finalRewardRolls),
                Math.max(0, state.legendaryRewardRolls),
                finalExperienceReward(state),
                state.finalRollSummary,
                debugLines,
                gateway,
                gatewayJson
        );
    }

    private static List<Reward> buildRewards(ForgeState state, GatewayThemeProfile theme, GatewayThemeProfile.GateTypeProfile gateType, boolean bossWaveEnabled) {
        List<Reward> rewards = new ArrayList<>();
        RandomSource random = RandomSource.create(state.profile.seed() ^ 0xA661L);
        int bossBonus = bossWaveEnabled ? 2 : 0;
        int baseRolls = Math.max(1, 1 + state.finalRewardRolls + bossBonus + (int) Math.floor(Math.max(0.0D, (state.rewardMultiplier - 1.0D) * 3.5D)));
        rewards.add(new Reward.LootTableReward(theme.commonLoot(), baseRolls, theme.finalDescKey()));
        ItemStack themeJunkReward = createThemeJunkRewardStack(state, random, true);
        if (!themeJunkReward.isEmpty()) {
            rewards.add(new Reward.StackReward(themeJunkReward));
        }
        if (state.rewardMultiplier > 1.0D && state.lootTableBonusChance > 0.0D) {
            rewards.add(new Reward.ChancedReward(new Reward.LootTableReward(theme.commonLoot(), 1, theme.finalDescKey()), (float) state.lootTableBonusChance));
        }
        float rareRewardChance = Mth.clamp((0.35F + (state.profile.level() / 180.0F)) * (float) state.rarityRewardMultiplier, 0.10F, 0.95F);
        for (int i = 0; i < Math.max(0, state.rareRewardRolls); i++) {
            rewards.add(new Reward.ChancedReward(new Reward.LootTableReward(theme.rareLoot(), 1, theme.rareDescKey()), rareRewardChance));
        }
        for (int i = 0; i < Math.max(0, state.legendaryRewardRolls); i++) {
            rewards.add(new Reward.LootTableReward(theme.rareLoot(), 1, theme.rareDescKey()));
        }
        rewards.add(new Reward.ExperienceReward(finalExperienceReward(state), 5));
        return rewards;
    }

    private static int selectBossWaveIndex(ForgeState state, EnemyPoolSet enemyPools, RandomSource random) {
        if (state.profile.level() < 35 || state.difficultyEstimate < 80 || !enemyPools.hasBosses()) {
            return -1;
        }
        return random.nextFloat() < 0.28F ? state.waveCount() - 1 : -1;
    }

    private static Wave buildBossWave(ForgeState state, GatewayThemeProfile theme, EnemyPoolSet enemyPools, RandomSource random, int waveIndex, int waveCount) {
        Wave.Builder builder = Wave.builder();
        EntityType<?> boss = enemyPools.pickBoss(random);
        builder.entity(configureWaveEntity(StandardWaveEntity.builder(boss), boss, state).count(1).addModifiers(adjustModifiersForEntity(boss, state, bossModifiers(state))).finalizeSpawn(true).build());
        int totalEnemies = 1;
        double totalStrength = 8.5D;
        int raiderIllusionersThisWave = 0;

        if (random.nextFloat() < 0.65F) {
            EntityType<?> supportType = pickEnemy(enemyPools, random, EnemyPoolRole.SUPPORT, EnemyPoolRole.RANGED);
            int supportCount = 2 + state.crystalTier.tier() / 2;
            supportCount = clampRaiderIllusionerCount(state, supportType, waveIndex, waveCount, supportCount, raiderIllusionersThisWave);
            if (supportCount > 0) {
                builder.entity(configureWaveEntity(StandardWaveEntity.builder(supportType), supportType, state, WaveArchetype.ARCHER).count(supportCount).addModifiers(adjustModifiersForEntity(supportType, state, waveModifiers(state, WaveArchetype.ARCHER, waveCount - 1))).finalizeSpawn(true).build());
                totalEnemies += supportCount;
                totalStrength += supportCount * perMobThreatCost(state, waveCount - 1, WaveArchetype.ARCHER);
                if (isFriendsAndFoesIllusioner(supportType)) {
                    raiderIllusionersThisWave += supportCount;
                }
            }
        }
        if (random.nextFloat() < 0.45F) {
            EntityType<?> meleeType = pickEnemy(enemyPools, random, EnemyPoolRole.MELEE, EnemyPoolRole.THEME);
            int meleeCount = 3 + state.crystalTier.tier();
            meleeCount = clampRaiderIllusionerCount(state, meleeType, waveIndex, waveCount, meleeCount, raiderIllusionersThisWave);
            if (meleeCount > 0) {
                builder.entity(configureWaveEntity(StandardWaveEntity.builder(meleeType), meleeType, state, WaveArchetype.TANK).count(meleeCount).addModifiers(adjustModifiersForEntity(meleeType, state, waveModifiers(state, WaveArchetype.TANK, waveCount - 1))).finalizeSpawn(true).build());
                totalEnemies += meleeCount;
                totalStrength += meleeCount * perMobThreatCost(state, waveCount - 1, WaveArchetype.TANK);
            }
        }

        for (WaveModifier modifier : globalWaveModifiers(state)) {
            builder.modifier(modifier);
        }
        builder.reward(new Reward.LootTableReward(theme.rareLoot(), 1, theme.rareDescKey()));
        builder.reward(new Reward.ExperienceReward(waveExperienceReward(state), 5));
        builder.maxWaveTime(Mth.clamp(computeWaveTimeLimit(state, waveCount - 1, waveCount, totalEnemies, totalStrength) + 500, 900, 12000));
        builder.setupTime(computeSetupTime(state));
        return builder.build();
    }

    private static StandardWaveEntity.Builder configureWaveEntity(StandardWaveEntity.Builder builder, EntityType<?> entityType, ForgeState state) {
        return configureWaveEntity(builder, entityType, state, null);
    }

    private static StandardWaveEntity.Builder configureWaveEntity(StandardWaveEntity.Builder builder, EntityType<?> entityType, ForgeState state, WaveArchetype archetype) {
        return builder.nbt(tag -> applySpawnOverrides(tag, entityType, state, archetype));
    }

    private static CompoundTag applySpawnOverrides(CompoundTag tag, EntityType<?> entityType, ForgeState state, WaveArchetype archetype) {
        tag.putString("DeathLootTable", "minecraft:empty");
        if (entityType == EntityType.PIGLIN || entityType == EntityType.PIGLIN_BRUTE || entityType == EntityType.HOGLIN) {
            tag.putBoolean("IsImmuneToZombification", true);
            tag.putInt("TimeInOverworld", 0);
        }
        if (entityType == EntityType.HOGLIN) {
            tag.putInt("Age", 0);
            tag.putBoolean("IsBaby", false);
        }
        if (entityType == EntityType.PHANTOM) {
            ListTag activeEffects = tag.contains("ActiveEffects", 9) ? tag.getList("ActiveEffects", 10) : new ListTag();
            CompoundTag effect = new CompoundTag();
            effect.putString("id", "minecraft:fire_resistance");
            effect.putInt("amplifier", 0);
            effect.putInt("duration", 12000);
            effect.putBoolean("ambient", false);
            effect.putBoolean("show_particles", false);
            effect.putBoolean("show_icon", false);
            activeEffects.add(effect);
            tag.put("ActiveEffects", activeEffects);
        }
        if (entityType == EntityType.DROWNED) {
            tag.putBoolean("CanBreakDoors", true);
            tag.putBoolean("gatesofavarice.gateway_drowned", true);
            ListTag handItems = ensureHandItems(tag);
            handItems.set(0, encodeItemStack(new ItemStack(Items.TRIDENT)));
            handItems.set(1, new CompoundTag());
            tag.put("HandItems", handItems);
            ListTag handDropChances = ensureHandDropChances(tag);
            handDropChances.set(0, net.minecraft.nbt.FloatTag.valueOf(0.0F));
            handDropChances.set(1, net.minecraft.nbt.FloatTag.valueOf(0.0F));
            tag.put("HandDropChances", handDropChances);
        }
        if (!supportsArmorProgression(entityType)) {
            return tag;
        }

        RandomSource random = RandomSource.create(state.profile.seed() ^ BuiltInRegistries.ENTITY_TYPE.getKey(entityType).hashCode() ^ tag.hashCode());
        float progress = armorProgress(state.profile.level());
        ListTag armorItems = ensureArmorItems(tag);
        boolean sunHelmetUndead = needsSunHelmet(entityType);
        if (sunHelmetUndead) {
            armorItems.set(3, encodeItemStack(selectSunHelmet(state.profile.level())));
        }

        if (progress > 0.0F && random.nextFloat() <= Mth.lerp(progress, 0.12F, 1.0F)) {
            ItemStack[] armorSet = selectArmorSet(state.profile.level(), random);
            for (int slot = 0; slot < 4; slot++) {
                boolean equipSlot = state.profile.level() >= 80 || random.nextFloat() < Mth.lerp(progress, 0.35F, 1.0F);
                armorItems.set(slot, equipSlot ? encodeItemStack(armorSet[slot]) : new CompoundTag());
            }
        }
        if (sunHelmetUndead && armorItems.getCompound(3).isEmpty()) {
            armorItems.set(3, encodeItemStack(selectSunHelmet(state.profile.level())));
        }
        tag.put("ArmorItems", armorItems);

        ListTag armorDropChances = new ListTag();
        for (int i = 0; i < 4; i++) {
            armorDropChances.add(net.minecraft.nbt.FloatTag.valueOf(0.0F));
        }
        tag.put("ArmorDropChances", armorDropChances);
        if (supportsWeaponProgression(entityType)) {
            float weaponProgress = weaponProgress(state.profile.level());
            if (weaponProgress > 0.0F && random.nextFloat() < Mth.lerp(weaponProgress, 0.22F, 0.90F)) {
                ListTag handItems = ensureHandItems(tag);
                if (handItems.getCompound(0).isEmpty()) {
                    handItems.set(0, encodeItemStack(selectWeapon(entityType, state.profile.level(), random)));
                    tag.put("HandItems", handItems);
                }
                ListTag handDropChances = ensureHandDropChances(tag);
                handDropChances.set(0, net.minecraft.nbt.FloatTag.valueOf(0.0F));
                handDropChances.set(1, net.minecraft.nbt.FloatTag.valueOf(0.0F));
                tag.put("HandDropChances", handDropChances);
            }
        }
        if (supportsRangedMeleeVariant(entityType) && shouldUseMeleeVariant(state, archetype, random)) {
            ListTag handItems = ensureHandItems(tag);
            handItems.set(0, encodeItemStack(selectWeapon(entityType, state.profile.level(), random)));
            handItems.set(1, new CompoundTag());
            tag.put("HandItems", handItems);
            ListTag handDropChances = ensureHandDropChances(tag);
            handDropChances.set(0, net.minecraft.nbt.FloatTag.valueOf(0.0F));
            handDropChances.set(1, net.minecraft.nbt.FloatTag.valueOf(0.0F));
            tag.put("HandDropChances", handDropChances);
        }
        return tag;
    }

    private static ListTag ensureArmorItems(CompoundTag tag) {
        ListTag armorItems = tag.getList("ArmorItems", net.minecraft.nbt.Tag.TAG_COMPOUND);
        while (armorItems.size() < 4) {
            armorItems.add(new CompoundTag());
        }
        return armorItems;
    }

    private static ListTag ensureHandItems(CompoundTag tag) {
        ListTag handItems = tag.getList("HandItems", net.minecraft.nbt.Tag.TAG_COMPOUND);
        while (handItems.size() < 2) {
            handItems.add(new CompoundTag());
        }
        return handItems;
    }

    private static ListTag ensureHandDropChances(CompoundTag tag) {
        ListTag handDropChances = tag.getList("HandDropChances", net.minecraft.nbt.Tag.TAG_FLOAT);
        while (handDropChances.size() < 2) {
            handDropChances.add(net.minecraft.nbt.FloatTag.valueOf(0.0F));
        }
        return handDropChances;
    }

    private static List<Failure> buildFailures(ForgeState state) {
        return List.of();
    }

    private static int computeSetupTime(ForgeState state) {
        int baseSetupTime = state.profile.level() < 15 ? 28 : state.profile.level() < 25 ? 36 : 42;
        int adjustedSetupTime = baseSetupTime + state.setupTimeDelta - (state.shorterDenser ? 10 : 0);
        return Math.max(20, adjustedSetupTime);
    }

    private static int computeWaveTimeLimit(ForgeState state, int waveIndex, int waveCount, int totalEnemies, double totalStrength) {
        int level = state.profile.level();
        int baseFloorSeconds;
        if (level < 15) {
            baseFloorSeconds = 24;
        } else if (level < 25) {
            baseFloorSeconds = 28;
        } else if (level < 40) {
            baseFloorSeconds = 34;
        } else if (level < 60) {
            baseFloorSeconds = 40;
        } else if (level < 75) {
            baseFloorSeconds = 48;
        } else if (level < 90) {
            baseFloorSeconds = 54;
        } else {
            baseFloorSeconds = 60;
        }
        double extraThreat = Math.max(0.0D, totalStrength - totalEnemies);
        double healthPressure = Math.max(0.0D, state.healthMultiplier);
        double damagePressure = Math.max(0.0D, state.damageMultiplier);
        int durabilityTime = (int) Math.round(
                healthPressure * (totalEnemies * 10.0D + totalStrength * 7.0D)
                        + damagePressure * (totalEnemies * 3.0D));
        int lateLevelPressure = level >= 75 ? (level - 74) * 4 : 0;
        int baseFloor = baseFloorSeconds * 20;
        int minimumPlayableTime = Math.max(30 * 20, baseFloor
                + totalEnemies * 12
                + (int) Math.round(extraThreat * 28.0D)
                + waveIndex * 40
                + durabilityTime
                + lateLevelPressure);

        int enemyTime = totalEnemies * 6;
        int strengthTime = (int) Math.round(totalStrength * 10.0D);
        int tankPressureTime = (int) Math.round(extraThreat * 18.0D);
        int durabilityScaledTime = durabilityTime / 2;
        int progressionTime = waveIndex * 30 + state.crystalTier.tier() * 20 + Math.max(0, state.minionsPerWave) * 10;
        int lateWaveBonus = waveIndex >= waveCount - 2 ? state.lateWaveTimeBonus : 0;
        int scaledTime = baseFloor + enemyTime + strengthTime + tankPressureTime + progressionTime + lateWaveBonus + durabilityScaledTime + lateLevelPressure;
        int adjustedTime = scaledTime + Math.max(-8 * 20, state.waveTimeDelta);
        return Mth.clamp(Math.max(minimumPlayableTime, adjustedTime), 30 * 20, 2400);
    }

    private static boolean supportsArmorProgression(EntityType<?> entityType) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (key == null) {
            return false;
        }
        if (isVariantsAndVenturesUndead(key)) {
            return true;
        }
        String path = key.getPath();
        return path.contains("zombie")
                || path.contains("drowned")
                || path.contains("skeleton")
                || path.contains("stray")
                || path.contains("piglin")
                || path.contains("pillager")
                || path.contains("vindicator")
                || path.contains("evoker")
                || path.contains("illusioner")
                || path.contains("bogged");
    }

    private static boolean supportsWeaponProgression(EntityType<?> entityType) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (key == null) {
            return false;
        }
        String path = key.getPath();
        return path.contains("zombie")
                || path.contains("husk")
                || path.contains("drowned")
                || path.contains("piglin")
                || path.contains("vindicator");
    }

    private static boolean needsSunHelmet(EntityType<?> entityType) {
        if (entityType == EntityType.ZOMBIE
                || entityType == EntityType.ZOMBIE_VILLAGER
                || entityType == EntityType.DROWNED
                || entityType == EntityType.SKELETON
                || entityType == EntityType.STRAY
                || entityType == EntityType.BOGGED) {
            return true;
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (key == null) {
            return false;
        }
        return isVariantsAndVenturesUndead(key);
    }

    private static boolean supportsRangedMeleeVariant(EntityType<?> entityType) {
        return entityType == EntityType.SKELETON
                || entityType == EntityType.STRAY
                || entityType == EntityType.BOGGED;
    }

    private static boolean shouldUseMeleeVariant(ForgeState state, WaveArchetype archetype, RandomSource random) {
        float chance = 0.08F + Math.min(0.18F, state.profile.level() / 220.0F);
        if (archetype == WaveArchetype.TANK || archetype == WaveArchetype.ASSASSIN || archetype == WaveArchetype.HOARD) {
            chance += 0.22F;
        }
        if (archetype == WaveArchetype.ARCHER) {
            chance -= 0.10F;
        }
        return random.nextFloat() < Mth.clamp(chance, 0.05F, 0.45F);
    }

    private static float armorProgress(int level) {
        return Mth.clamp((level - 15) / 65.0F, 0.0F, 1.0F);
    }

    private static float weaponProgress(int level) {
        return Mth.clamp((level - 25) / 55.0F, 0.0F, 1.0F);
    }

    private static ItemStack[] selectArmorSet(int level, RandomSource random) {
        if (level >= 80) {
            return NETHERITE_ARMOR_SET;
        }

        float tierProgress = armorProgress(level) * 4.0F;
        int baseTier = Mth.floor(tierProgress);
        float upgradeChance = tierProgress - baseTier;
        if (random.nextFloat() < upgradeChance) {
            baseTier++;
        }
        return switch (Mth.clamp(baseTier, 0, 4)) {
            case 0 -> LEATHER_ARMOR_SET;
            case 1 -> CHAINMAIL_ARMOR_SET;
            case 2 -> IRON_ARMOR_SET;
            case 3 -> DIAMOND_ARMOR_SET;
            default -> NETHERITE_ARMOR_SET;
        };
    }

    private static ItemStack[] createArmorSet(net.minecraft.world.item.Item boots, net.minecraft.world.item.Item leggings, net.minecraft.world.item.Item chestplate, net.minecraft.world.item.Item helmet) {
        return new ItemStack[] {
                new ItemStack(boots),
                new ItemStack(leggings),
                new ItemStack(chestplate),
                new ItemStack(helmet)
        };
    }

    private static ItemStack[] createWeaponSet(net.minecraft.world.item.Item low, net.minecraft.world.item.Item mid, net.minecraft.world.item.Item high, net.minecraft.world.item.Item top) {
        return new ItemStack[] {
                new ItemStack(low),
                new ItemStack(mid),
                new ItemStack(high),
                new ItemStack(top)
        };
    }

    private static ItemStack selectWeapon(EntityType<?> entityType, int level, RandomSource random) {
        ItemStack[] set = prefersAxe(entityType) ? AXE_SET : SWORD_SET;
        if (level >= 80) {
            return set[3].copy();
        }

        float tierProgress = weaponProgress(level) * 3.0F;
        int baseTier = Mth.floor(tierProgress);
        float upgradeChance = tierProgress - baseTier;
        if (random.nextFloat() < upgradeChance) {
            baseTier++;
        }
        return set[Mth.clamp(baseTier, 0, 3)].copy();
    }

    private static boolean prefersAxe(EntityType<?> entityType) {
        return entityType == EntityType.VINDICATOR || entityType == EntityType.PIGLIN_BRUTE;
    }

    private static ItemStack selectSunHelmet(int level) {
        if (level >= 80) {
            return new ItemStack(Items.NETHERITE_HELMET);
        }
        if (level >= 60) {
            return new ItemStack(Items.DIAMOND_HELMET);
        }
        if (level >= 40) {
            return new ItemStack(Items.IRON_HELMET);
        }
        if (level >= 25) {
            return new ItemStack(Items.CHAINMAIL_HELMET);
        }
        return new ItemStack(Items.LEATHER_HELMET);
    }

    private static CompoundTag encodeItemStack(ItemStack stack) {
        return ItemStack.OPTIONAL_CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, stack.copy())
                .result()
                .filter(CompoundTag.class::isInstance)
                .map(CompoundTag.class::cast)
                .orElseGet(CompoundTag::new);
    }

    private static int finalExperienceReward(ForgeState state) {
        int baseReward = Math.max(1, state.profile.level() * 18 + state.experienceBonus);
        return Math.max(1, (int) Math.round(baseReward * state.experienceRewardMultiplier));
    }

    private static int waveExperienceReward(ForgeState state) {
        return Math.max(1, (int) Math.round(finalExperienceReward(state) * 0.25D));
    }

    private static void addWaveRewardDrops(Wave.Builder builder, ForgeState state, GatewayThemeProfile theme, int waveIndex, int waveCount, RandomSource random) {
        int tierRolls = Math.max(1, state.crystalTier.tier() >= 4 && waveIndex >= waveCount - 2 ? 2 : 1);
        if (state.profile.level() >= 30) {
            tierRolls++;
        }
        if (state.profile.level() >= 50 && waveIndex >= waveCount - 2) {
            tierRolls++;
        }
        builder.reward(new Reward.LootTableReward(
                ResourceLocation.fromNamespaceAndPath("gatesofavarice", (ModCompat.isRunicLoaded() ? "rewards/waves/tier_" : "rewards/waves_fallback/tier_") + state.crystalTier.tier()),
                tierRolls,
                theme.waveDescKey()
        ));
        ItemStack themeJunkReward = createThemeJunkRewardStack(state, random, false);
        if (!themeJunkReward.isEmpty()) {
            builder.reward(new Reward.StackReward(themeJunkReward));
        }
    }

    private static ItemStack createThemeJunkRewardStack(ForgeState state, RandomSource random, boolean finalReward) {
        Item item;
        int minCount;
        int maxCount;
        switch (state.profile.theme()) {
            case UNDEAD -> {
                item = ModItems.HARDENED_FLESH.get();
                minCount = finalReward ? 10 : 3;
                maxCount = finalReward ? 22 : 8;
            }
            case RAIDER -> {
                item = ModItems.RUSTY_COIN.get();
                minCount = finalReward ? 8 : 2;
                maxCount = finalReward ? 18 : 6;
            }
            case WILD -> {
                Item[] pool = {
                        ModItems.RUSTY_COIN.get(),
                        ModItems.SOLAR_SHARD.get(),
                        ModItems.ARCANE_ESSENCE.get(),
                        ModItems.HARDENED_FLESH.get()
                };
                item = pool[random.nextInt(pool.length)];
                minCount = finalReward ? 8 : 3;
                maxCount = finalReward ? 20 : 9;
            }
            default -> {
                return ItemStack.EMPTY;
            }
        }

        int levelBonus = Math.max(0, state.profile.level() / (finalReward ? 25 : 40));
        ItemStack stack = new ItemStack(item);
        stack.setCount(random.nextInt(minCount + levelBonus, maxCount + levelBonus + 1));
        return stack;
    }

    private static List<WaveModifier> globalWaveModifiers(ForgeState state) {
        List<WaveModifier> modifiers = new ArrayList<>();
        for (ResourceLocation id : state.mobEffects) {
            modifiers.add(WaveModifier.EffectModifier.create(BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(ResourceKey.create(Registries.MOB_EFFECT, id)), 0));
        }
        return modifiers;
    }

    private static List<WaveModifier> baseModifiers(ForgeState state) {
        List<WaveModifier> modifiers = new ArrayList<>();
        addAttribute(modifiers, Attributes.MAX_HEALTH, state.healthMultiplier);
        addAttribute(modifiers, Attributes.ATTACK_DAMAGE, state.damageMultiplier);
        if (state.flatDamageBonus != 0.0D) {
            modifiers.add(WaveModifier.AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_VALUE, (float) state.flatDamageBonus));
        }
        addAttribute(modifiers, Attributes.MOVEMENT_SPEED, state.speedMultiplier);
        if (state.armorBonus != 0.0D) {
            modifiers.add(WaveModifier.AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, (float) state.armorBonus));
        }
        if (state.knockbackResistance != 0.0D) {
            modifiers.add(WaveModifier.AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, (float) state.knockbackResistance));
        }
        if (state.lifeSteal != 0.0D) {
            modifiers.add(WaveModifier.AttributeModifier.create(ALObjects.Attributes.LIFE_STEAL, Operation.ADD_VALUE, (float) state.lifeSteal));
        }
        if (state.armorPierce != 0.0D) {
            modifiers.add(WaveModifier.AttributeModifier.create(ALObjects.Attributes.ARMOR_PIERCE, Operation.ADD_VALUE, (float) state.armorPierce));
        }
        if (state.projectileDamage != 0.0D) {
            modifiers.add(WaveModifier.AttributeModifier.create(ALObjects.Attributes.PROJECTILE_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, (float) state.projectileDamage));
        }
        return modifiers;
    }

    private static List<WaveModifier> eliteModifiers(ForgeState state, RandomSource random) {
        List<WaveModifier> modifiers = new ArrayList<>(baseModifiers(state));
        modifiers.add(WaveModifier.AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.40F));
        modifiers.add(WaveModifier.AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.25F));
        if (random.nextFloat() < 0.5F) {
            modifiers.add(WaveModifier.EffectModifier.create(BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(ResourceKey.create(Registries.MOB_EFFECT, ResourceLocation.withDefaultNamespace("strength"))), 0));
        }
        return modifiers;
    }

    private static List<WaveModifier> minibossModifiers(ForgeState state) {
        List<WaveModifier> modifiers = new ArrayList<>(eliteModifiers(state, RandomSource.create(state.profile.seed() + 19L)));
        modifiers.add(WaveModifier.AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.55F));
        modifiers.add(WaveModifier.AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 6.0F));
        return modifiers;
    }

    private static List<WaveModifier> bossModifiers(ForgeState state) {
        List<WaveModifier> modifiers = new ArrayList<>(minibossModifiers(state));
        modifiers.add(WaveModifier.AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.85F));
        modifiers.add(WaveModifier.AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.45F));
        modifiers.add(WaveModifier.AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 8.0F));
        return modifiers;
    }

    private static void addAttribute(List<WaveModifier> modifiers, Holder<Attribute> attribute, double multiplier) {
        if (multiplier != 0.0D) {
            modifiers.add(WaveModifier.AttributeModifier.create(attribute, Operation.ADD_MULTIPLIED_TOTAL, (float) multiplier));
        }
    }

    private static boolean shouldAddElite(ForgeState state, int waveIndex, RandomSource random) {
        if (state.eliteEvery > 0 && (waveIndex + 1) % state.eliteEvery == 0) {
            return true;
        }
        return random.nextFloat() < eliteSpawnChance(state, waveIndex);
    }

    private static float eliteSpawnChance(ForgeState state, int waveIndex) {
        float chance = state.eliteChance + (waveIndex * 0.02F) + state.eliteUpgradeChance;
        if (state.profile.theme() == CrystalTheme.RAIDER) {
            chance *= state.profile.level() < 50 ? 0.45F : 0.60F;
        } else if (state.profile.theme() == CrystalTheme.ARCANE) {
            chance *= state.profile.level() < 50 ? 0.60F : 0.75F;
        }
        return Mth.clamp(chance, 0.01F, 0.45F);
    }

    private static WaveComposition buildWaveComposition(ForgeState state, EnemyPoolSet enemyPools, RandomSource random, int waveIndex, int waveCount) {
        WaveArchetype primaryArchetype = selectWaveArchetype(state, random, waveIndex, waveCount);
        double budget = computeWaveBudget(state, waveIndex, primaryArchetype);
        int minCount = minimumEnemyTarget(state, waveIndex, primaryArchetype);
        int maxCount = maximumEnemyTarget(state, waveIndex, primaryArchetype);
        List<PlannedWaveEntity> planned = new ArrayList<>();
        double spent = 0.0D;
        int totalCount = 0;
        int raiderIllusionersThisWave = 0;
        int guard = 0;
        while (guard++ < 48 && totalCount < maxCount) {
            WaveArchetype archetype = pickArchetypeForBudget(primaryArchetype, random, totalCount, minCount);
            EnemyPoolRole role = pickRoleForArchetype(archetype, random);
            EntityType<?> type = pickEnemy(enemyPools, random, role, fallbackRoleForArchetype(archetype));
            double perMobCost = perMobThreatCost(state, waveIndex, archetype);
            int remainingSlots = maxCount - totalCount;
            double remainingBudget = budget - spent;
            int count = groupCountForArchetype(archetype, remainingBudget, perMobCost, remainingSlots, random);
            if (count <= 0) {
                if (totalCount < minCount) {
                    count = Math.min(remainingSlots, forcedCountForArchetype(archetype, random));
                } else {
                    break;
                }
            }
            count = clampRaiderIllusionerCount(state, type, waveIndex, waveCount, count, raiderIllusionersThisWave);
            if (count <= 0) {
                continue;
            }
            List<WaveModifier> modifiers = waveModifiers(state, archetype, waveIndex);
            double groupCost = count * perMobCost;
            planned.add(new PlannedWaveEntity(type, count, modifiers, groupCost, archetype));
            spent += groupCost;
            totalCount += count;
            if (isFriendsAndFoesIllusioner(type)) {
                raiderIllusionersThisWave += count;
            }
            if (spent >= budget && totalCount >= minCount) {
                break;
            }
        }
        if (planned.isEmpty()) {
            EntityType<?> fallback = pickEnemy(enemyPools, random, EnemyPoolRole.MELEE, EnemyPoolRole.THEME);
            int count = Math.max(4, minimumEnemyTarget(state, waveIndex, WaveArchetype.HOARD) / 2);
            count = clampRaiderIllusionerCount(state, fallback, waveIndex, waveCount, count, 0);
            if (count <= 0) {
                fallback = EntityType.PILLAGER;
                count = 1;
            }
            double cost = count * perMobThreatCost(state, waveIndex, WaveArchetype.HOARD);
            planned.add(new PlannedWaveEntity(fallback, count, waveModifiers(state, WaveArchetype.HOARD, waveIndex), cost, WaveArchetype.HOARD));
            totalCount = count;
            spent = cost;
            primaryArchetype = WaveArchetype.HOARD;
        }
        return new WaveComposition(planned, totalCount, spent, primaryArchetype);
    }

    private static WaveArchetype selectWaveArchetype(ForgeState state, RandomSource random, int waveIndex, int waveCount) {
        float hoardBias;
        float tankBias;
        float assassinBias;
        float archerBias;
        if (state.profile.theme() == CrystalTheme.WILD) {
            float chaos = 0.18F + Math.min(0.16F, state.profile.level() / 500.0F);
            hoardBias = 0.25F + (state.hoardPacks * 0.10F) + ((random.nextFloat() - 0.5F) * chaos);
            tankBias = 0.25F + (state.tankPacks * 0.10F) + ((random.nextFloat() - 0.5F) * chaos);
            assassinBias = 0.25F + (state.assassinPacks * 0.10F) + ((random.nextFloat() - 0.5F) * chaos);
            archerBias = 0.25F + (state.archerPacks * 0.10F) + ((random.nextFloat() - 0.5F) * chaos);
            hoardBias = Math.max(0.10F, hoardBias);
            tankBias = Math.max(0.10F, tankBias);
            assassinBias = Math.max(0.10F, assassinBias);
            archerBias = Math.max(0.10F, archerBias);
        } else {
            hoardBias = 0.20F + Math.min(0.28F, state.profile.level() / 180.0F) + waveIndex * 0.08F + (state.hoardPacks * 0.12F);
            tankBias = 0.34F - Math.min(0.18F, state.profile.level() / 220.0F) - waveIndex * 0.05F + (state.tankPacks * 0.12F);
            assassinBias = 0.24F + (state.assassinPacks * 0.12F);
            archerBias = 0.22F + Math.min(0.16F, state.rangedPacks * 0.06F) + (state.archerPacks * 0.12F);
        }
        if (waveIndex >= waveCount - 2) {
            hoardBias += 0.10F;
            tankBias -= 0.04F;
        }
        float total = hoardBias + tankBias + assassinBias + archerBias;
        float roll = random.nextFloat() * total;
        if ((roll -= tankBias) < 0.0F) return WaveArchetype.TANK;
        if ((roll -= assassinBias) < 0.0F) return WaveArchetype.ASSASSIN;
        if ((roll -= hoardBias) < 0.0F) return WaveArchetype.HOARD;
        return WaveArchetype.ARCHER;
    }

    private static WaveArchetype pickArchetypeForBudget(WaveArchetype primary, RandomSource random, int totalCount, int minCount) {
        if (totalCount < minCount / 2 && primary != WaveArchetype.HOARD && random.nextFloat() < 0.45F) {
            return WaveArchetype.HOARD;
        }
        if (random.nextFloat() < 0.64F) {
            return primary;
        }
        return switch (primary) {
            case TANK -> random.nextBoolean() ? WaveArchetype.ASSASSIN : WaveArchetype.HOARD;
            case ASSASSIN -> random.nextBoolean() ? WaveArchetype.ARCHER : WaveArchetype.HOARD;
            case HOARD -> random.nextBoolean() ? WaveArchetype.ASSASSIN : WaveArchetype.ARCHER;
            case ARCHER -> random.nextBoolean() ? WaveArchetype.HOARD : WaveArchetype.ASSASSIN;
        };
    }

    private static EnemyPoolRole pickRoleForArchetype(WaveArchetype archetype, RandomSource random) {
        return switch (archetype) {
            case TANK -> random.nextFloat() < 0.7F ? EnemyPoolRole.TANK : EnemyPoolRole.MELEE;
            case ASSASSIN -> random.nextFloat() < 0.7F ? EnemyPoolRole.FAST : EnemyPoolRole.MELEE;
            case HOARD -> random.nextFloat() < 0.6F ? EnemyPoolRole.THEME : EnemyPoolRole.MELEE;
            case ARCHER -> random.nextFloat() < 0.75F ? EnemyPoolRole.RANGED : EnemyPoolRole.SUPPORT;
        };
    }

    private static EnemyPoolRole fallbackRoleForArchetype(WaveArchetype archetype) {
        return switch (archetype) {
            case TANK -> EnemyPoolRole.MELEE;
            case ASSASSIN -> EnemyPoolRole.FAST;
            case HOARD -> EnemyPoolRole.THEME;
            case ARCHER -> EnemyPoolRole.MELEE;
        };
    }

    private static double computeWaveBudget(ForgeState state, int waveIndex, WaveArchetype archetype) {
        double budget = 8.0D
                + state.crystalTier.tier() * 3.0D
                + state.profile.level() * 0.34D
                + waveIndex * (7.5D + state.profile.level() * 0.04D)
                + state.minionsPerWave * 1.6D
                + state.densePacks * 3.0D;
        if (waveIndex >= Math.max(1, state.waveCount() - 2)) {
            budget += 4.0D;
        }
        return budget * archetype.budgetMultiplier() * (1.0D + state.mobSpawnMultiplier * 0.45D);
    }

    private static double perMobThreatCost(ForgeState state, int waveIndex, WaveArchetype archetype) {
        double softness = Mth.clamp(1.22D - state.profile.level() * 0.006D - waveIndex * 0.08D, 0.56D, 1.18D);
        return archetype.baseCost() * softness;
    }

    private static int minimumEnemyTarget(ForgeState state, int waveIndex, WaveArchetype archetype) {
        int base = switch (archetype) {
            case TANK -> 8;
            case ASSASSIN -> 10;
            case HOARD -> 16;
            case ARCHER -> 12;
        };
        int target = base + state.crystalTier.tier() + waveIndex * 4 + state.profile.level() / 18 + Math.max(0, state.minionsPerWave);
        return Math.max(base, (int) Math.round(target * (1.0D + state.mobSpawnMultiplier)));
    }

    private static int maximumEnemyTarget(ForgeState state, int waveIndex, WaveArchetype archetype) {
        int padding = switch (archetype) {
            case TANK -> 8;
            case ASSASSIN -> 12;
            case HOARD -> 24;
            case ARCHER -> 18;
        };
        int target = minimumEnemyTarget(state, waveIndex, archetype) + padding + state.densePacks * 4;
        return Math.max(minimumEnemyTarget(state, waveIndex, archetype), (int) Math.round(target * (1.0D + state.mobSpawnMultiplier * 0.5D)));
    }

    private static int groupCountForArchetype(WaveArchetype archetype, double remainingBudget, double perMobCost, int remainingSlots, RandomSource random) {
        int maxAffordable = (int) Math.floor(remainingBudget / Math.max(0.35D, perMobCost));
        if (maxAffordable <= 0) {
            return 0;
        }
        int lower = archetype.minGroupSize();
        int upper = archetype.maxGroupSize();
        int count = lower + random.nextInt(Math.max(1, upper - lower + 1));
        count = Math.min(count, maxAffordable);
        return Mth.clamp(count, 0, remainingSlots);
    }

    private static int forcedCountForArchetype(WaveArchetype archetype, RandomSource random) {
        return archetype.minGroupSize() + random.nextInt(Math.max(1, archetype.maxGroupSize() - archetype.minGroupSize()));
    }

    private static int countFriendsAndFoesIllusioners(List<PlannedWaveEntity> planned) {
        int count = 0;
        for (PlannedWaveEntity entity : planned) {
            if (isFriendsAndFoesIllusioner(entity.type())) {
                count += entity.count();
            }
        }
        return count;
    }

    private static int clampRaiderIllusionerCount(ForgeState state, EntityType<?> entityType, int waveIndex, int waveCount, int requested, int alreadyPlanned) {
        if (!isFriendsAndFoesIllusioner(entityType) || state.profile.theme() != CrystalTheme.RAIDER) {
            return requested;
        }
        int remaining = raiderIllusionerWaveCap(waveIndex, waveCount) - alreadyPlanned;
        return Math.max(0, Math.min(requested, remaining));
    }

    private static int raiderIllusionerWaveCap(int waveIndex, int waveCount) {
        if (waveCount <= 1) {
            return 1;
        }
        float progress = (float) waveIndex / (float) (waveCount - 1);
        if (progress >= 0.75F) {
            return 3;
        }
        if (progress >= 0.35F) {
            return 2;
        }
        return 1;
    }

    private static boolean isFriendsAndFoesIllusioner(EntityType<?> entityType) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        return key != null && "friendsandfoes".equals(key.getNamespace()) && "illusioner".equals(key.getPath());
    }

    private static List<WaveModifier> waveModifiers(ForgeState state, WaveArchetype archetype, int waveIndex) {
        List<WaveModifier> modifiers = new ArrayList<>(baseModifiers(state));
        double softness = Mth.clamp(1.08D - state.profile.level() * 0.003D - waveIndex * 0.05D, 0.62D, 1.05D);
        switch (archetype) {
            case TANK -> {
                modifiers.add(WaveModifier.AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, (float) (0.35D * softness)));
                modifiers.add(WaveModifier.AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, (float) (3.5D * softness)));
                modifiers.add(WaveModifier.AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, (float) (-0.05D * softness)));
                modifiers.add(WaveModifier.AttributeModifier.create(Attributes.SCALE, Operation.ADD_MULTIPLIED_TOTAL, 0.10F));
            }
            case ASSASSIN -> {
                modifiers.add(WaveModifier.AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, (float) (0.22D * softness)));
                modifiers.add(WaveModifier.AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, (float) (0.12D * softness)));
                modifiers.add(WaveModifier.AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, (float) (-0.08D * softness)));
            }
            case HOARD -> {
                modifiers.add(WaveModifier.AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, (float) (-0.18D * softness)));
                modifiers.add(WaveModifier.AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, (float) (-0.10D * softness)));
                modifiers.add(WaveModifier.AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, (float) (0.04D * softness)));
            }
            case ARCHER -> {
                modifiers.add(WaveModifier.AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, (float) (0.08D * softness)));
                modifiers.add(WaveModifier.AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, (float) (-0.18D * softness)));
                modifiers.add(WaveModifier.AttributeModifier.create(ALObjects.Attributes.PROJECTILE_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, (float) (-0.12D * softness)));
                modifiers.add(WaveModifier.AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, (float) (-0.06D * softness)));
                modifiers.add(WaveModifier.AttributeModifier.create(Attributes.SCALE, Operation.ADD_MULTIPLIED_TOTAL, -0.05F));
            }
        }
        return modifiers;
    }

    private static List<WaveModifier> adjustModifiersForEntity(EntityType<?> entityType, ForgeState state, List<WaveModifier> modifiers) {
        List<WaveModifier> adjusted = new ArrayList<>(modifiers);
        if (state.profile.theme() == CrystalTheme.RAIDER && entityType == EntityType.RAVAGER) {
            adjusted.add(WaveModifier.AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, -0.25F));
        }
        if (state.profile.theme() == CrystalTheme.ARCANE && entityType == EntityType.ENDERMITE) {
            adjusted.add(WaveModifier.AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.40F));
        }
        return adjusted;
    }

    private static boolean isVariantsAndVenturesUndead(ResourceLocation key) {
        return (key.getNamespace().equals("variantsandventures") || key.getNamespace().equals("variants_and_ventures"))
                && (key.getPath().equals("gelid")
                || key.getPath().equals("murk")
                || key.getPath().equals("thicket")
                || key.getPath().equals("verdant"));
    }

    private static EntityType<?> pickEnemy(EnemyPoolSet enemyPools, RandomSource random, EnemyPoolRole primary, EnemyPoolRole fallback) {
        EntityType<?> type = enemyPools.pick(random, primary, fallback);
        if (type == null) {
            throw new IllegalStateException("Unable to pick enemy for " + primary + " with fallback " + fallback);
        }
        return type;
    }

    private static ItemStack createPearl(GatewayBuildResult result) {
        ItemStack pearl = new ItemStack(GatewayObjects.GATE_PEARL.value());
        GatePearlItem.setGate(pearl, GatewayRegistry.INSTANCE.holder(result.gatewayId()));
        pearl.set(DataComponents.CUSTOM_NAME, Component.literal(result.name()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(result.color()))));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("Theme: " + titleCase(result.theme().name()))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(result.color())))
                .append(Component.literal(" [alt]").withStyle(ChatFormatting.GRAY)));
        lore.add(Component.literal("Crystal Level: " + result.crystalLevel()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))));
        lore.add(Component.literal("Difficulty: " + difficultyLabel(result.difficultyEstimate(), result.crystalLevel())).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(difficultyColor(result.difficultyEstimate(), result.crystalLevel())))));
        for (String line : buildRewardSummary(result)) {
            lore.add(Component.literal(line).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xF0C24B))));
        }
        if (!result.finalRollSummary().isEmpty()) {
            lore.add(Component.literal("Modifiers:").withStyle(ChatFormatting.LIGHT_PURPLE));
            for (String line : result.finalRollSummary()) {
                lore.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }
        pearl.set(DataComponents.LORE, new ItemLore(lore));
        CustomData.update(DataComponents.CUSTOM_DATA, pearl, tag -> {
            CompoundTag root = tag.getCompound(ROOT_KEY);
            root.putString(GATEWAY_ID_KEY, result.gatewayId().toString());
            writeGatewayJson(root, result.gatewayJson());
            root.putString(DISPLAY_NAME_KEY, result.name());
            root.putString(DIFFICULTY_KEY, difficultyLabel(result.difficultyEstimate(), result.crystalLevel()));
            root.putString(THEME_KEY, result.theme().name());
            root.putInt(LEVEL_KEY, result.crystalLevel());
            root.putInt(TIER_KEY, result.crystalTier());
            root.putInt(PLAYER_LEVEL_KEY, result.playerLevel());
            root.putBoolean(OVERLEVELED_KEY, result.overleveled());
            root.putInt(FLAT_COIN_BONUS_KEY, result.flatCoinBonus());
            root.putDouble(COIN_REWARD_MULTIPLIER_KEY, result.coinRewardMultiplier());
            root.putDouble(LEVEL_XP_MULTIPLIER_KEY, result.levelXpMultiplier());
            root.putDouble(EXPERIENCE_REWARD_MULTIPLIER_KEY, result.experienceRewardMultiplier());
            root.putDouble(THORNS_DAMAGE_KEY, result.thornsDamage());
            ListTag summary = new ListTag();
            for (String line : result.finalRollSummary()) {
                summary.add(net.minecraft.nbt.StringTag.valueOf(line));
            }
            root.put(SUMMARY_KEY, summary);
            tag.put(ROOT_KEY, root);
        });
        return pearl;
    }

    private static CompoundTag getRootTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(ROOT_KEY);
    }

    public static void restorePersistedGateways(ServerLevel level) {
        for (Map.Entry<ResourceLocation, GeneratedGatewayStorage.StoredGateway> entry : GeneratedGatewayStorage.get(level).entries().entrySet()) {
            restoreGeneratedGateway(
                    entry.getKey(),
                    entry.getValue().json(),
                    entry.getValue().displayName(),
                    entry.getValue().crystalTier(),
                    entry.getValue().crystalLevel(),
                    entry.getValue().flatCoinBonus(),
                    entry.getValue().coinRewardMultiplier(),
                    entry.getValue().levelXpMultiplier(),
                    entry.getValue().experienceRewardMultiplier(),
                    entry.getValue().thornsDamage());
        }
    }

    private static ResourceLocation registerGeneratedGateway(ResourceLocation gatewayId, NormalGateway gateway) {
        DynamicRegistry.DataGenPopulator.runScoped(GatewayRegistry.INSTANCE, populator -> populator.register(gatewayId, gateway));
        GENERATED_GATEWAY_NAMES.putIfAbsent(gatewayId, "Gateway");
        return gatewayId;
    }

    public static String getGatewayDisplayName(Gateway gateway) {
        ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gateway);
        if (id == null) {
            return null;
        }
        return GENERATED_GATEWAY_NAMES.get(id);
    }

    public static int getGatewayCrystalTier(Gateway gateway) {
        ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gateway);
        if (id == null) {
            return 0;
        }
        return GENERATED_GATEWAY_TIERS.getOrDefault(id, 0);
    }

    public static int getGatewayLevel(Gateway gateway) {
        ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gateway);
        if (id == null) {
            return 0;
        }
        return GENERATED_GATEWAY_LEVELS.getOrDefault(id, 0);
    }

    public static int getGatewayFlatCoinBonus(Gateway gateway) {
        ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gateway);
        if (id == null) {
            return 0;
        }
        return GENERATED_GATEWAY_FLAT_COIN_BONUSES.getOrDefault(id, 0);
    }

    public static double getGatewayCoinRewardMultiplier(Gateway gateway) {
        ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gateway);
        if (id == null) {
            return 1.0D;
        }
        return GENERATED_GATEWAY_COIN_MULTIPLIERS.getOrDefault(id, 1.0D);
    }

    public static double getGatewayLevelXpMultiplier(Gateway gateway) {
        ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gateway);
        if (id == null) {
            return 1.0D;
        }
        return GENERATED_GATEWAY_LEVEL_XP_MULTIPLIERS.getOrDefault(id, 1.0D);
    }

    public static double getGatewayExperienceRewardMultiplier(Gateway gateway) {
        ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gateway);
        if (id == null) {
            return 1.0D;
        }
        return GENERATED_GATEWAY_EXPERIENCE_MULTIPLIERS.getOrDefault(id, 1.0D);
    }

    public static double getGatewayThornsDamage(Gateway gateway) {
        ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gateway);
        if (id == null) {
            return 0.0D;
        }
        return GENERATED_GATEWAY_THORNS_DAMAGE.getOrDefault(id, 0.0D);
    }

    private static void consumeInputs(Container container) {
        consumeSlot(container, GatewayWorkbenchSlots.CRYSTAL_SLOT);
    }

    private static void consumeSlot(Container container, int slot) {
        ItemStack stack = container.getItem(slot);
        if (!stack.isEmpty()) {
            stack.shrink(1);
            container.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
    }

    private static void validate(List<Wave> waves, List<Reward> rewards) {
        if (waves.isEmpty()) throw new IllegalStateException("Gateway has no waves.");
        for (Wave wave : waves) {
            if (wave.entities().isEmpty()) throw new IllegalStateException("Gateway has empty wave.");
            if (wave.maxWaveTime() < 20 || wave.setupTime() < 0) throw new IllegalStateException("Gateway has invalid timing.");
        }
        if (rewards.isEmpty()) throw new IllegalStateException("Gateway has no rewards.");
    }

    private static String serializeGateway(NormalGateway gateway) {
        JsonElement element = NormalGateway.CODEC.encodeStart(JsonOps.INSTANCE, gateway).getOrThrow(IllegalStateException::new);
        return GSON.toJson(element);
    }

    private static boolean restoreGeneratedGateway(ResourceLocation gatewayId, String gatewayJson, String displayName, int crystalTier, int crystalLevel, int flatCoinBonus,
            double coinRewardMultiplier, double levelXpMultiplier, double experienceRewardMultiplier, double thornsDamage) {
        if (GatewayRegistry.INSTANCE.getValue(gatewayId) != null) {
            GENERATED_GATEWAY_NAMES.putIfAbsent(gatewayId, displayName);
            GENERATED_GATEWAY_TIERS.putIfAbsent(gatewayId, crystalTier);
            GENERATED_GATEWAY_LEVELS.putIfAbsent(gatewayId, crystalLevel);
            GENERATED_GATEWAY_FLAT_COIN_BONUSES.putIfAbsent(gatewayId, flatCoinBonus);
            GENERATED_GATEWAY_COIN_MULTIPLIERS.putIfAbsent(gatewayId, coinRewardMultiplier);
            GENERATED_GATEWAY_LEVEL_XP_MULTIPLIERS.putIfAbsent(gatewayId, levelXpMultiplier);
            GENERATED_GATEWAY_EXPERIENCE_MULTIPLIERS.putIfAbsent(gatewayId, experienceRewardMultiplier);
            GENERATED_GATEWAY_THORNS_DAMAGE.putIfAbsent(gatewayId, thornsDamage);
            return false;
        }

        if (gatewayJson == null || gatewayJson.isBlank() || "null".equals(gatewayJson.trim())) {
            GatewayExpansion.LOGGER.warn("Skipping persisted generated gateway {} because its saved JSON payload is missing.", gatewayId);
            return false;
        }

        try {
            JsonElement json = JsonParser.parseString(gatewayJson);
            if (!json.isJsonObject()) {
                GatewayExpansion.LOGGER.warn("Skipping persisted generated gateway {} because its saved JSON payload is not an object.", gatewayId);
                return false;
            }

            NormalGateway gateway = NormalGateway.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(IllegalStateException::new);
            registerGeneratedGateway(gatewayId, gateway);
            GENERATED_GATEWAY_NAMES.put(gatewayId, displayName);
            GENERATED_GATEWAY_TIERS.put(gatewayId, crystalTier);
            GENERATED_GATEWAY_LEVELS.put(gatewayId, crystalLevel);
            GENERATED_GATEWAY_FLAT_COIN_BONUSES.put(gatewayId, flatCoinBonus);
            GENERATED_GATEWAY_COIN_MULTIPLIERS.put(gatewayId, coinRewardMultiplier);
            GENERATED_GATEWAY_LEVEL_XP_MULTIPLIERS.put(gatewayId, levelXpMultiplier);
            GENERATED_GATEWAY_EXPERIENCE_MULTIPLIERS.put(gatewayId, experienceRewardMultiplier);
            GENERATED_GATEWAY_THORNS_DAMAGE.put(gatewayId, thornsDamage);
            return true;
        } catch (Exception exception) {
            GatewayExpansion.LOGGER.warn("Skipping persisted generated gateway {} because it could not be restored.", gatewayId, exception);
            return false;
        }
    }

    private static boolean bindPearlIfResolved(ItemStack stack, CompoundTag root, ResourceLocation gatewayId) {
        Gateway gateway = GatewayRegistry.INSTANCE.getValue(gatewayId);
        if (gateway == null) {
            return false;
        }
        hydrateGatewayMetadataFromRoot(gatewayId, root);
        GatePearlItem.setGate(stack, GatewayRegistry.INSTANCE.holder(gatewayId));
        return true;
    }

    private static void hydrateGatewayMetadataFromRoot(ResourceLocation gatewayId, CompoundTag root) {
        int crystalLevel = root.contains(LEVEL_KEY) ? root.getInt(LEVEL_KEY) : GENERATED_GATEWAY_LEVELS.getOrDefault(gatewayId, 0);
        int crystalTier = root.contains(TIER_KEY) ? root.getInt(TIER_KEY) : inferCrystalTierFromLevel(crystalLevel);
        String displayName = root.contains(DISPLAY_NAME_KEY)
                ? root.getString(DISPLAY_NAME_KEY)
                : GENERATED_GATEWAY_NAMES.getOrDefault(gatewayId, "Lv " + crystalLevel + " Gateway");
        int flatCoinBonus = root.contains(FLAT_COIN_BONUS_KEY)
                ? root.getInt(FLAT_COIN_BONUS_KEY)
                : GENERATED_GATEWAY_FLAT_COIN_BONUSES.getOrDefault(gatewayId, 0);
        double coinRewardMultiplier = root.contains(COIN_REWARD_MULTIPLIER_KEY)
                ? root.getDouble(COIN_REWARD_MULTIPLIER_KEY)
                : GENERATED_GATEWAY_COIN_MULTIPLIERS.getOrDefault(gatewayId, 1.0D);
        double levelXpMultiplier = root.contains(LEVEL_XP_MULTIPLIER_KEY)
                ? root.getDouble(LEVEL_XP_MULTIPLIER_KEY)
                : GENERATED_GATEWAY_LEVEL_XP_MULTIPLIERS.getOrDefault(gatewayId, 1.0D);
        double experienceRewardMultiplier = root.contains(EXPERIENCE_REWARD_MULTIPLIER_KEY)
                ? root.getDouble(EXPERIENCE_REWARD_MULTIPLIER_KEY)
                : GENERATED_GATEWAY_EXPERIENCE_MULTIPLIERS.getOrDefault(gatewayId, 1.0D);
        double thornsDamage = root.contains(THORNS_DAMAGE_KEY)
                ? root.getDouble(THORNS_DAMAGE_KEY)
                : GENERATED_GATEWAY_THORNS_DAMAGE.getOrDefault(gatewayId, 0.0D);

        GENERATED_GATEWAY_NAMES.put(gatewayId, displayName);
        GENERATED_GATEWAY_TIERS.put(gatewayId, crystalTier);
        GENERATED_GATEWAY_LEVELS.put(gatewayId, crystalLevel);
        GENERATED_GATEWAY_FLAT_COIN_BONUSES.put(gatewayId, flatCoinBonus);
        GENERATED_GATEWAY_COIN_MULTIPLIERS.put(gatewayId, coinRewardMultiplier);
        GENERATED_GATEWAY_LEVEL_XP_MULTIPLIERS.put(gatewayId, levelXpMultiplier);
        GENERATED_GATEWAY_EXPERIENCE_MULTIPLIERS.put(gatewayId, experienceRewardMultiplier);
        GENERATED_GATEWAY_THORNS_DAMAGE.put(gatewayId, thornsDamage);
    }

    private static void persistGeneratedGateway(ServerLevel level, GatewayBuildResult result) {
        persistGeneratedGateway(
                level,
                result.gatewayId(),
                result.gatewayJson(),
                result.name(),
                result.crystalTier(),
                result.crystalLevel(),
                result.flatCoinBonus(),
                result.coinRewardMultiplier(),
                result.levelXpMultiplier(),
                result.experienceRewardMultiplier(),
                result.thornsDamage());
    }

    private static void persistGeneratedGateway(ServerLevel level, ResourceLocation gatewayId, String gatewayJson, String displayName, int crystalTier, int crystalLevel,
            int flatCoinBonus, double coinRewardMultiplier, double levelXpMultiplier, double experienceRewardMultiplier, double thornsDamage) {
        GeneratedGatewayStorage.get(level).put(gatewayId, gatewayJson, displayName, crystalTier, crystalLevel, flatCoinBonus, coinRewardMultiplier, levelXpMultiplier,
                experienceRewardMultiplier, thornsDamage);
    }

    private static int inferCrystalTierFromLevel(int crystalLevel) {
        if (crystalLevel >= 90) return 5;
        if (crystalLevel >= 70) return 4;
        if (crystalLevel >= 50) return 3;
        if (crystalLevel >= 20) return 2;
        return crystalLevel > 0 ? 1 : 0;
    }

    private static Gateway.Size gatewaySize(int tier) {
        if (tier >= 5) return Gateway.Size.LARGE;
        if (tier >= 3) return Gateway.Size.MEDIUM;
        return Gateway.Size.SMALL;
    }

    private static String generateName(ForgeState state, GatewayThemeProfile theme, GatewayThemeProfile.GateTypeProfile gateType) {
        return "Lv " + state.profile.level() + " Gateway";
    }

    private static void applyEffect(ForgeState state, ForgeEffect effect) {
        switch (effect.type()) {
            case ADD_MINIONS_PER_WAVE -> state.minionsPerWave += (int) Math.round(effect.value());
            case SUPPORT_PACK_EVERY -> {
                state.supportInterval = state.supportInterval == 0 ? (int) Math.round(effect.value()) : Math.min(state.supportInterval, (int) Math.round(effect.value()));
                state.supportCount += Math.max(1, (int) Math.round(effect.secondaryValue()));
            }
            case BONUS_WAVES -> state.bonusWaves += (int) Math.round(effect.value());
            case EXTEND_LATE_WAVES -> state.lateWaveTimeBonus += (int) Math.round(effect.value());
            case SHORTER_DENSER_WAVES -> {
                state.shorterDenser = true;
                state.densePacks += 1;
                state.waveTimeDelta -= 80;
                if (state.bonusWaves > 0) state.bonusWaves -= 1;
            }
            case ELITE_EVERY -> state.eliteEvery = state.eliteEvery == 0 ? (int) Math.round(effect.value()) : Math.min(state.eliteEvery, (int) Math.round(effect.value()));
            case ELITE_CHANCE -> state.eliteChance += (float) effect.value();
            case ELITE_UPGRADE_CHANCE -> state.eliteUpgradeChance += (float) effect.value();
            case FINAL_WAVE_ELITES -> state.finalWaveEliteCount += (int) Math.round(effect.value());
            case HEALTH_MULTIPLIER -> state.healthMultiplier += effect.value();
            case DAMAGE_MULTIPLIER -> state.damageMultiplier += effect.value();
            case SPEED_MULTIPLIER -> state.speedMultiplier += effect.value();
            case ARMOR_BONUS -> state.armorBonus += effect.value();
            case KNOCKBACK_RESISTANCE -> state.knockbackResistance += effect.value();
            case LIFE_STEAL -> state.lifeSteal += effect.value();
            case ARMOR_PIERCE -> state.armorPierce += effect.value();
            case PROJECTILE_DAMAGE -> state.projectileDamage += effect.value();
            case MOB_EFFECT -> {
                if (effect.referenceId() != null) state.mobEffects.add(effect.referenceId());
            }
            case THEMED_REINFORCEMENTS -> state.reinforcementRolls += (int) Math.round(effect.value());
            case MIXED_PACKS -> state.mixedPackCount += (int) Math.round(effect.value());
            case RANGED_PACKS -> state.rangedPacks += (int) Math.round(effect.value());
            case TANK_PACKS -> state.tankPacks += (int) Math.round(effect.value());
            case HOARD_PACKS -> state.hoardPacks += (int) Math.round(effect.value());
            case ASSASSIN_PACKS -> state.assassinPacks += (int) Math.round(effect.value());
            case ARCHER_PACKS -> state.archerPacks += (int) Math.round(effect.value());
            case MINIBOSS_CHANCE -> state.minibossChance += (float) effect.value();
            case NAMED_ELITE_CHANCE -> {
            }
            case DANGEROUS_FINAL_WAVE -> state.dangerousFinalWave = true;
            case WAVE_TIME_DELTA -> state.waveTimeDelta += (int) Math.round(effect.value());
            case SETUP_TIME_DELTA -> state.setupTimeDelta += (int) Math.round(effect.value());
            case MOB_SPAWN_MULTIPLIER -> state.mobSpawnMultiplier += effect.value();
            case BONUS_COINS_FLAT -> state.flatCoinBonus += Math.max(0, (int) Math.round(effect.value()));
            case REWARD_MULTIPLIER -> state.rewardMultiplier += effect.value();
            case COIN_REWARD_MULTIPLIER -> state.coinRewardMultiplier *= Math.max(0.05D, effect.value());
            case LEVEL_XP_MULTIPLIER -> state.levelXpMultiplier *= Math.max(0.05D, effect.value());
            case EXPERIENCE_REWARD_MULTIPLIER -> state.experienceRewardMultiplier *= Math.max(0.05D, effect.value());
            case THORNS_DAMAGE -> state.thornsDamage += Math.max(0.0D, effect.value());
            case RARITY_REWARD_MULTIPLIER -> state.rarityRewardMultiplier *= Math.max(0.05D, effect.value());
            case EXTRA_RARE_REWARD_ROLLS -> {
                int rareBonus = Math.max(1, (int) Math.round(effect.value()));
                state.rareRewardRolls += rareBonus;
                int epicBonus = (int) Math.round(effect.secondaryValue());
                if (epicBonus <= 0) {
                    epicBonus = epicRewardBundleBonus(state.profile.level());
                }
                state.finalRewardRolls += Math.max(1, epicBonus);
                if (state.profile.level() >= 40) {
                    state.legendaryRewardRolls += 1;
                }
            }
            case EXTRA_FINAL_REWARD_ROLLS -> state.finalRewardRolls += Math.max(1, (int) Math.round(effect.value()));
            case EXTRA_LEGENDARY_REWARD_ROLLS -> {
                if (state.profile.level() >= 40) {
                    state.legendaryRewardRolls += Math.max(1, (int) Math.round(effect.value()));
                }
            }
            case EXTRA_ENTITY_LOOT_ROLLS -> state.entityLootRolls += (int) Math.round(effect.value());
            case BONUS_EXPERIENCE -> state.experienceBonus += (int) Math.round(effect.value());
            case BONUS_LOOT_TABLE_CHANCE -> state.lootTableBonusChance += effect.value();
        }
        state.difficultyEstimate += effect.value() >= 0 ? 4 : -2;
    }

    private static String difficultyLabel(int estimate, int level) {
        if (estimate >= 95 && level >= 50) return "Extreme";
        if (estimate >= 70) return "Hard";
        if (estimate >= 40) return "Medium";
        return "Easy";
    }

    private static int difficultyColor(int estimate, int level) {
        if (estimate >= 95 && level >= 50) return 0xFF5555;
        if (estimate >= 70) return 0xFFAA00;
        if (estimate >= 40) return 0xFFFF55;
        return 0x55FF55;
    }

    private static int percent(double value) {
        return (int) Math.round(value * 100.0D);
    }

    private static String signedPercent(double value) {
        int rounded = percent(value);
        return (rounded > 0 ? "+" : "") + rounded + "%";
    }

    private static String titleCase(String value) {
        return java.util.Arrays.stream(value.toLowerCase(Locale.ROOT).split("_"))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static String joinedOrNone(List<String> values) {
        return values.isEmpty() ? "None" : String.join(", ", values);
    }

    private static boolean isNegativePreviewEffect(ForgeEffect effect) {
        return effect.type() != ForgeEffectType.MOB_EFFECT
                || effect.referenceId() == null
                || !ResourceLocation.withDefaultNamespace("glowing").equals(effect.referenceId());
    }

    private static List<String> buildRewardSummary(ForgeState state) {
        return List.of(
                Math.max(0, state.rareRewardRolls) + " Rare item drops",
                Math.max(1, state.finalRewardRolls) + " Epic item drops",
                Math.max(0, state.legendaryRewardRolls) + " Legendary item drops",
                signedPercent(state.rarityRewardMultiplier - 1.0D) + " item rarity",
                signedPercent(state.rewardMultiplier - 1.0D) + " item quantity",
                "x" + formatMultiplier(state.levelXpMultiplier) + " Levels",
                finalExperienceReward(state) + " XP"
        );
    }

    private static List<String> buildRewardSummary(GatewayBuildResult result) {
        return List.of(
                result.rareRewardDrops() + " Rare item drops",
                result.epicRewardDrops() + " Epic item drops",
                result.legendaryRewardDrops() + " Legendary item drops",
                signedPercent(result.rarityRewardMultiplier() - 1.0D) + " item rarity",
                signedPercent(result.rewardMultiplier() - 1.0D) + " item quantity",
                "x" + formatMultiplier(result.levelXpMultiplier()) + " Levels",
                result.finalExperienceReward() + " XP"
        );
    }

    private static String formatMultiplier(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private static int epicRewardBundleBonus(int level) {
        if (level >= 90) {
            return 3;
        }
        if (level >= 50) {
            return 2;
        }
        return 1;
    }

    private static double naturalHealthMultiplier(int level) {
        if (level <= 0) {
            return 0.0D;
        }
        if (level <= 20) {
            return level * 0.025D;
        }
        if (level <= 30) {
            return 0.50D + (level - 20) * 0.045D;
        }
        if (level <= 50) {
            return 0.95D + (level - 30) * 0.0225D;
        }
        if (level <= 75) {
            return 1.40D + (level - 50) * 0.060D;
        }
        if (level <= 100) {
            return 2.90D + (level - 75) * 0.020D;
        }
        return 3.40D;
    }

    private static double naturalDamageMultiplier(int level) {
        if (level <= 0) {
            return 0.0D;
        }
        if (level <= 20) {
            return level * 0.023D;
        }
        if (level <= 30) {
            return 0.46D + (level - 20) * 0.040D;
        }
        if (level <= 50) {
            return 0.86D + (level - 30) * 0.020D;
        }
        if (level <= 75) {
            return 1.26D + (level - 50) * 0.070D;
        }
        if (level <= 100) {
            return 3.01D + (level - 75) * 0.020D;
        }
        return 3.51D;
    }

    private static double naturalFlatDamageBonus(int level) {
        if (level <= 0) {
            return 0.0D;
        }
        if (level <= 20) {
            return level * 0.045D;
        }
        if (level <= 30) {
            return 0.90D + (level - 20) * 0.060D;
        }
        if (level <= 50) {
            return 1.50D + (level - 30) * 0.030D;
        }
        if (level <= 75) {
            return 2.10D + (level - 50) * 0.060D;
        }
        if (level <= 100) {
            return 3.60D + (level - 75) * 0.030D;
        }
        return 4.35D;
    }

    private static double naturalArmorBonus(int level) {
        if (level <= 0) {
            return 0.0D;
        }
        if (level <= 20) {
            return level * 0.050D;
        }
        if (level <= 30) {
            return 1.0D + (level - 20) * 0.070D;
        }
        if (level <= 50) {
            return 1.70D + (level - 30) * 0.040D;
        }
        if (level <= 75) {
            return 2.50D + (level - 50) * 0.080D;
        }
        if (level <= 100) {
            return 4.50D + (level - 75) * 0.040D;
        }
        return 5.50D;
    }

    private enum WaveArchetype {
        TANK(2.8D, 0.95D, 2, 5),
        ASSASSIN(1.9D, 1.0D, 3, 6),
        HOARD(0.90D, 1.1D, 5, 11),
        ARCHER(1.45D, 1.0D, 3, 7);

        private final double baseCost;
        private final double budgetMultiplier;
        private final int minGroupSize;
        private final int maxGroupSize;

        WaveArchetype(double baseCost, double budgetMultiplier, int minGroupSize, int maxGroupSize) {
            this.baseCost = baseCost;
            this.budgetMultiplier = budgetMultiplier;
            this.minGroupSize = minGroupSize;
            this.maxGroupSize = maxGroupSize;
        }

        private double baseCost() {
            return this.baseCost;
        }

        private double budgetMultiplier() {
            return this.budgetMultiplier;
        }

        private int minGroupSize() {
            return this.minGroupSize;
        }

        private int maxGroupSize() {
            return this.maxGroupSize;
        }
    }

    private record PlannedWaveEntity(EntityType<?> type, int count, List<WaveModifier> modifiers, double threatCost, WaveArchetype archetype) {
    }

    private record WaveComposition(List<PlannedWaveEntity> entities, int totalCount, double totalStrength, WaveArchetype primaryArchetype) {
    }

    private static final class ForgeState {
        private final CrystalItem.CrystalTier crystalTier;
        private final CrystalForgeData.CrystalProfile profile;
        private final List<String> negativeSummary = new ArrayList<>();
        private final List<String> finalRollSummary = new ArrayList<>();
        private final Set<ResourceLocation> mobEffects = new LinkedHashSet<>();
        private int minionsPerWave;
        private int supportInterval;
        private int supportCount;
        private int bonusWaves;
        private int lateWaveTimeBonus;
        private boolean shorterDenser;
        private int densePacks;
        private int eliteEvery;
        private float eliteChance = 0.06F;
        private float eliteUpgradeChance;
        private int finalWaveEliteCount;
        private double healthMultiplier;
        private double damageMultiplier;
        private double flatDamageBonus;
        private double speedMultiplier;
        private double armorBonus;
        private double knockbackResistance;
        private double lifeSteal;
        private double armorPierce;
        private double projectileDamage;
        private int reinforcementRolls;
        private int mixedPackCount;
        private int rangedPacks;
        private int tankPacks;
        private int hoardPacks;
        private int assassinPacks;
        private int archerPacks;
        private float minibossChance;
        private boolean dangerousFinalWave;
        private int waveTimeDelta;
        private int setupTimeDelta;
        private double mobSpawnMultiplier;
        private double rewardMultiplier = 1.0D;
        private int flatCoinBonus;
        private double coinRewardMultiplier = 1.0D;
        private double levelXpMultiplier = 1.0D;
        private double experienceRewardMultiplier = 1.0D;
        private double thornsDamage;
        private double rarityRewardMultiplier = 1.0D;
        private int rareRewardRolls = 1;
        private int finalRewardRolls = 1;
        private int legendaryRewardRolls;
        private int entityLootRolls = 1;
        private int experienceBonus;
        private double lootTableBonusChance;
        private int difficultyEstimate;

        private ForgeState(CrystalItem.CrystalTier crystalTier, CrystalForgeData.CrystalProfile profile) {
            this.crystalTier = crystalTier;
            this.profile = profile;
            this.healthMultiplier = naturalHealthMultiplier(profile.level());
            this.damageMultiplier = naturalDamageMultiplier(profile.level());
            this.flatDamageBonus = naturalFlatDamageBonus(profile.level());
            this.armorBonus = naturalArmorBonus(profile.level());
            this.difficultyEstimate = crystalTier.tier() * 8 + profile.level();
        }

        private int waveCount() {
            int base = 3 + (this.profile.level() >= 18 ? 1 : 0) + (this.crystalTier.tier() >= 3 ? 1 : 0) + (this.profile.level() >= 38 ? 1 : 0);
            return Mth.clamp(base + this.bonusWaves, 3, 9);
        }

        private String timePressureLabel() {
            int total = this.waveTimeDelta + this.setupTimeDelta;
            if (total <= -120) return "Severe";
            if (total < -40) return "High";
            if (total > 120) return "Relaxed";
            return "Stable";
        }

        private void finish() {
            this.healthMultiplier = Mth.clamp(this.healthMultiplier, -0.20D, 5.00D);
            this.damageMultiplier = Mth.clamp(this.damageMultiplier, -0.20D, 5.00D);
            this.speedMultiplier = Mth.clamp(this.speedMultiplier, -0.10D, 2.00D);
            this.mobSpawnMultiplier = Mth.clamp(this.mobSpawnMultiplier, 0.0D, 1.5D);
            this.eliteChance = Mth.clamp(this.eliteChance, 0.02F, 0.60F);
            this.rewardMultiplier = Math.max(0.40D, this.rewardMultiplier);
            this.flatCoinBonus = Math.max(0, this.flatCoinBonus);
            this.coinRewardMultiplier = Mth.clamp(this.coinRewardMultiplier, 0.10D, 25.0D);
            this.levelXpMultiplier = Mth.clamp(this.levelXpMultiplier, 0.10D, 25.0D);
            this.experienceRewardMultiplier = Mth.clamp(this.experienceRewardMultiplier, 0.10D, 25.0D);
            this.thornsDamage = Mth.clamp(this.thornsDamage, 0.0D, 20.0D);
            this.rarityRewardMultiplier = Mth.clamp(this.rarityRewardMultiplier, 0.10D, 25.0D);
            this.entityLootRolls = Math.max(0, this.entityLootRolls);
            this.finalRewardRolls = Math.max(0, this.finalRewardRolls);
            this.rareRewardRolls = Math.max(0, this.rareRewardRolls);
            this.legendaryRewardRolls = Math.max(0, this.legendaryRewardRolls);
            this.difficultyEstimate += this.waveCount() * 5;
            this.difficultyEstimate += percent(this.healthMultiplier) / 3;
            this.difficultyEstimate += percent(this.damageMultiplier) / 3;
            this.difficultyEstimate += percent(this.speedMultiplier) / 4;
            this.difficultyEstimate += percent(this.mobSpawnMultiplier) / 4;
            this.difficultyEstimate += this.finalWaveEliteCount * 6;
            this.difficultyEstimate += this.dangerousFinalWave ? 12 : 0;
            this.difficultyEstimate += this.mobEffects.size() * 4;
        }
    }
}
