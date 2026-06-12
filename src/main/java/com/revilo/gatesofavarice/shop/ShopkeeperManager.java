package com.revilo.gatesofavarice.shop;

import com.revilo.gatesofavarice.currency.MythicCoinWallet;
import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import com.revilo.gatesofavarice.entity.GatekeeperEntity;
import com.revilo.gatesofavarice.dungeon.ModDimensions;
import com.revilo.gatesofavarice.gateway.GatewayPartyScaling;
import com.revilo.gatesofavarice.gateway.builder.GatewayForgeService;
import com.revilo.gatesofavarice.integration.LevelUpIntegration;
import com.revilo.gatesofavarice.integration.LevelUpGatewayXpRewards;
import com.revilo.gatesofavarice.integration.ModCompat;
import com.revilo.gatesofavarice.item.LootMaterialItem;
import com.revilo.gatesofavarice.item.MythicCoinStackData;
import com.revilo.gatesofavarice.item.data.LootRarity;
import com.revilo.gatesofavarice.menu.ShopkeeperMenu;
import com.revilo.gatesofavarice.registry.ModEntities;
import com.revilo.gatesofavarice.registry.ModItems;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.event.GateEvent;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ShopkeeperManager {

    private static final String SHOPKEEPER_KEY = "gatesofavarice.shopkeeper";
    private static final String TEMP_TRADE_KEY = "gatesofavarice.temp_trades";
    private static final String STOCK_KEY = "gatesofavarice.stock";
    private static final String PRICE_KEY = "gatesofavarice.price";
    private static final String REROLL_COUNT_KEY = "gatesofavarice.reroll_count";
    private static final String SHOP_GATEWAY_ANIMATION_KEY = "gatesofavarice.shop_gateway_animation";
    private static final String SHOP_GATEWAY_TRADER_ID = "gatesofavarice.shop_gateway_trader";
    private static final String SHOP_GATEWAY_TRADER_SPAWNED = "gatesofavarice.shop_gateway_trader_spawned";
    private static final String SHOP_GATEWAY_ENTITY_ID = "gatesofavarice.shop_gateway_entity";
    private static final int MAX_REROLLS = 3;
    private static final int BASE_REROLL_COST = 100;
    private static final int SHOP_GATEWAY_ANIMATION_TICKS = 50;
    private static final double COIN_ATTRACTION_RANGE = 14.0D;
    private static final double COIN_ATTRACTION_FORCE = 0.105D;
    private static final double COIN_ATTRACTION_MAX_SPEED = 1.15D;
    private static final List<DeferredHolder<net.minecraft.world.item.Item, ? extends LootMaterialItem>> GATEWAY_DROP_ITEMS = List.of(
            ModItems.GRIMSTONE,
            ModItems.MYSTIC_ESSENCE,
            ModItems.SCRAP_METAL,
            ModItems.UPGRADE_BASE,
            ModItems.MANA_GEMS,
            ModItems.MANA_STEEL_SCRAP,
            ModItems.MAGNETITE_SCRAP,
            ModItems.ARCANE_ESSENCE,
            ModItems.MANASTONES,
            ModItems.ELIXRITE_SCRAP,
            ModItems.MAGNETITE_INGOT,
            ModItems.ASTRITE_SCRAP,
            ModItems.SOLAR_SHARD,
            ModItems.ARCANE_APPLE,
            ModItems.ENCHANTED_ARCANE_APPLE,
            ModItems.PRISMATIC_SHARD,
            ModItems.PRISMATIC_DIAMOND,
            ModItems.LUNARIUM_SCRAP,
            ModItems.DARK_ESSENCE,
            ModItems.PRISMATIC_CORE
    );
    private static final float STABILITY_PEARL_DROP_CHANCE = 0.04F;

    private ShopkeeperManager() {
    }

    @SubscribeEvent
    public static void onGateCompleted(GateEvent.Completed event) {
        GatewayEntity gate = event.getEntity();
        if (!(gate.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (serverLevel.dimension() == ModDimensions.DUNGEON_LEVEL) {
            int completionBurst = computeCompletionCoinBurst(gate, serverLevel.random);
            int awardedCoins = applyGatewayCoinAdjustments(gate, completionBurst);
            awardedCoins = Math.max(awardedCoins, minimumCompletionCoinReward(gate));
            creditCoins(gate, awardedCoins);
        }
        spawnGatewayLoot(gate, computeCompletionLootRolls(gate, serverLevel.random));
    }

    @SubscribeEvent
    public static void onWaveCompleted(GateEvent.WaveEnd event) {
        GatewayEntity gate = event.getEntity();
        if (gate.level() instanceof ServerLevel serverLevel) {
            int awardedCoins = 0;
            if (serverLevel.dimension() == ModDimensions.DUNGEON_LEVEL) {
                int waveBurst = computeWaveCoinBurst(gate, serverLevel.random);
                awardedCoins = applyGatewayCoinAdjustments(gate, waveBurst);
                awardedCoins = Math.max(awardedCoins, minimumWaveCoinReward(gate));
                creditCoins(gate, awardedCoins);
            }
            sendWaveSummary(gate, awardedCoins);
        }
    }

    @SubscribeEvent
    public static void onCoinPickup(ItemEntityPickupEvent.Pre event) {
        ItemEntity itemEntity = event.getItemEntity();
        ItemStack stack = itemEntity.getItem();
        if (!stack.is(ModItems.MYTHIC_COIN.get())) {
            return;
        }

        event.setCanPickup(TriState.FALSE);
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        MythicCoinWallet.add(player, MythicCoinStackData.getValue(stack));
        player.take(itemEntity, stack.getCount());
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 1.1F);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 0.6D, player.getZ(), 6, 0.2D, 0.2D, 0.2D, 0.0D);
        }
        itemEntity.discard();
    }

    @SubscribeEvent
    public static void onCoinTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof GatewayEntity gatewayEntity && isGatewayAnimation(gatewayEntity)) {
            if (gatewayEntity.level() instanceof ServerLevel serverLevel) {
                if (!gatewayEntity.getPersistentData().getBoolean(SHOP_GATEWAY_TRADER_SPAWNED) && gatewayEntity.tickCount >= SHOP_GATEWAY_ANIMATION_TICKS) {
                    Player summoner = gatewayEntity.summonerOrClosest();
                    GatekeeperEntity trader = spawnShopkeeper(serverLevel, gatewayEntity.getX(), gatewayEntity.getY() + 0.5D, gatewayEntity.getZ(), summoner);
                    if (trader != null) {
                        gatewayEntity.getPersistentData().putBoolean(SHOP_GATEWAY_TRADER_SPAWNED, true);
                        gatewayEntity.getPersistentData().putUUID(SHOP_GATEWAY_TRADER_ID, trader.getUUID());
                        trader.getPersistentData().putUUID(SHOP_GATEWAY_ENTITY_ID, gatewayEntity.getUUID());
                        gatewayEntity.remove(Entity.RemovalReason.DISCARDED);
                    }
                }
            }
            return;
        }

        if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }

        ItemStack stack = itemEntity.getItem();
        if (!stack.is(ModItems.MYTHIC_COIN.get())) {
            return;
        }

        Vec3 motion = itemEntity.getDeltaMovement();
        itemEntity.setDeltaMovement(motion.x, motion.y + 0.02D, motion.z);

        Player target = itemEntity.level().getNearestPlayer(itemEntity, COIN_ATTRACTION_RANGE);
        if (target != null) {
            Vec3 direction = new Vec3(
                    target.getX() - itemEntity.getX(),
                    target.getY() + target.getEyeHeight() * 0.45D - itemEntity.getY(),
                    target.getZ() - itemEntity.getZ());
            double distance = direction.length();
            if (distance <= 1.35D && target instanceof ServerPlayer serverPlayer) {
                MythicCoinWallet.add(serverPlayer, MythicCoinStackData.getValue(stack));
                serverPlayer.take(itemEntity, stack.getCount());
                target.level().playSound(null, target.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 1.15F);
                itemEntity.discard();
                return;
            }
            if (distance > 0.001D) {
                double normalizedDistance = Math.min(distance / COIN_ATTRACTION_RANGE, 1.0D);
                double pull = COIN_ATTRACTION_FORCE * (0.6D + (1.0D - normalizedDistance) * 2.8D);
                Vec3 boostedMotion = itemEntity.getDeltaMovement().scale(0.72D).add(direction.normalize().scale(pull));
                if (boostedMotion.lengthSqr() > COIN_ATTRACTION_MAX_SPEED * COIN_ATTRACTION_MAX_SPEED) {
                    boostedMotion = boostedMotion.normalize().scale(COIN_ATTRACTION_MAX_SPEED);
                }
                itemEntity.setDeltaMovement(boostedMotion);
                itemEntity.hasImpulse = true;
                itemEntity.hurtMarked = true;
            }
        }

        if (itemEntity.level() instanceof ServerLevel serverLevel && itemEntity.tickCount % 4 == 0) {
            serverLevel.sendParticles(
                    ParticleTypes.PORTAL,
                    itemEntity.getX(),
                    itemEntity.getY() + 0.08D,
                    itemEntity.getZ(),
                    3,
                    0.12D,
                    0.04D,
                    0.12D,
                    0.01D);
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Entity target = event.getTarget();
        if (!(target instanceof GatekeeperEntity trader) || !isShopkeeper(trader) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        if (DungeonRunManager.tryResumePendingWaveMenu(player, trader.getId())) {
            return;
        }
        MenuProvider provider = new net.minecraft.world.SimpleMenuProvider(
                (containerId, inventory, ignored) -> new ShopkeeperMenu(containerId, inventory, trader.getId()),
                Component.translatable("entity.gatesofavarice.shopkeeper"));
        player.openMenu(provider, buffer -> buffer.writeInt(trader.getId()));
    }

    @SubscribeEvent
    public static void onShopkeeperDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof GatekeeperEntity trader) || trader.level().isClientSide || !isShopkeeper(trader)) {
            return;
        }

        if (trader.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.PORTAL,
                    trader.getX(),
                    trader.getY() + 0.9D,
                    trader.getZ(),
                    28,
                    0.35D,
                    0.45D,
                    0.35D,
                    0.15D);
        }

        CompoundTag traderData = trader.getPersistentData();
        if (!traderData.hasUUID(SHOP_GATEWAY_ENTITY_ID) || !(trader.level() instanceof ServerLevel serverLevel)) {
            trader.remove(Entity.RemovalReason.DISCARDED);
            return;
        }

        Entity linkedEntity = serverLevel.getEntity(traderData.getUUID(SHOP_GATEWAY_ENTITY_ID));
        if (linkedEntity instanceof GatewayEntity gatewayEntity && isGatewayAnimation(gatewayEntity) && !gatewayEntity.isRemoved()) {
            gatewayEntity.remove(Entity.RemovalReason.DISCARDED);
        }

        trader.remove(Entity.RemovalReason.DISCARDED);
    }

    public static GatekeeperEntity spawnShopkeeper(Level level, double x, double y, double z, Player summoner) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        GatekeeperEntity trader = ModEntities.GATEKEEPER.get().create(serverLevel);
        if (trader == null) {
            return null;
        }

        trader.moveTo(x, y, z, summoner == null ? 0.0F : summoner.getYRot(), 0.0F);
        trader.setCustomName(Component.translatable("entity.gatesofavarice.shopkeeper").withStyle(ChatFormatting.GOLD));
        trader.setCustomNameVisible(true);
        trader.setPersistenceRequired();
        trader.setHealth(1.0F);
        trader.getPersistentData().putBoolean(SHOPKEEPER_KEY, true);
        rollVisibleOffers(trader, serverLevel.random, getPlayerLevel(summoner));
        return serverLevel.addFreshEntity(trader) ? trader : null;
    }

    public static boolean isShopkeeper(GatekeeperEntity trader) {
        return trader.getPersistentData().getBoolean(SHOPKEEPER_KEY);
    }

    public static void markGatewayAnimation(GatewayEntity entity) {
        entity.getPersistentData().putBoolean(SHOP_GATEWAY_ANIMATION_KEY, true);
    }

    public static void markGatewayAnimation(Entity entity) {
        entity.getPersistentData().putBoolean(SHOP_GATEWAY_ANIMATION_KEY, true);
    }

    public static boolean isGatewayAnimation(GatewayEntity entity) {
        return entity.getPersistentData().getBoolean(SHOP_GATEWAY_ANIMATION_KEY);
    }

    public static boolean isGatewayAnimation(Entity entity) {
        return entity.getPersistentData().getBoolean(SHOP_GATEWAY_ANIMATION_KEY);
    }

    public static java.util.List<ShopOfferDefinition> getOffers(GatekeeperEntity trader) {
        java.util.List<ShopOfferDefinition> offers = buildOffers(trader.getPersistentData().getIntArray(TEMP_TRADE_KEY));
        return offers.size() > ShopkeeperMenu.GRID_SLOT_COUNT ? offers.subList(0, ShopkeeperMenu.GRID_SLOT_COUNT) : offers;
    }

    public static int getMaxRerolls() {
        return MAX_REROLLS;
    }

    public static int getRerollCost(GatekeeperEntity trader) {
        int rerollCount = getRerollCount(trader);
        if (rerollCount >= MAX_REROLLS) {
            return 0;
        }
        return BASE_REROLL_COST << rerollCount;
    }

    public static int getRerollCount(GatekeeperEntity trader) {
        return Math.max(0, trader.getPersistentData().getInt(REROLL_COUNT_KEY));
    }

    public static void incrementRerollCount(GatekeeperEntity trader) {
        int rerollCount = getRerollCount(trader);
        trader.getPersistentData().putInt(REROLL_COUNT_KEY, Math.min(MAX_REROLLS, rerollCount + 1));
    }

    public static int[] getTempOfferIndexes(GatekeeperEntity trader) {
        return trader.getPersistentData().getIntArray(TEMP_TRADE_KEY);
    }

    public static int[] getOfferStocks(GatekeeperEntity trader) {
        int[] stocks = trader.getPersistentData().getIntArray(STOCK_KEY);
        if (stocks.length == ShopkeeperMenu.GRID_SLOT_COUNT) {
            return stocks;
        }

        int[] normalized = new int[ShopkeeperMenu.GRID_SLOT_COUNT];
        System.arraycopy(stocks, 0, normalized, 0, Math.min(stocks.length, normalized.length));
        return normalized;
    }

    public static int[] getOfferPrices(GatekeeperEntity trader) {
        int[] prices = trader.getPersistentData().getIntArray(PRICE_KEY);
        if (prices.length == ShopkeeperMenu.GRID_SLOT_COUNT) {
            return prices;
        }

        int[] normalized = new int[ShopkeeperMenu.GRID_SLOT_COUNT];
        System.arraycopy(prices, 0, normalized, 0, Math.min(prices.length, normalized.length));
        return normalized;
    }

    public static boolean rerollOffers(ServerPlayer player, GatekeeperEntity trader) {
        return rerollOffersWithWallet(player, trader, false);
    }

    public static boolean rerollOffersWithWallet(ServerPlayer player, GatekeeperEntity trader, boolean useDungeonTokens) {
        int rerollCount = Math.max(0, trader.getPersistentData().getInt(REROLL_COUNT_KEY));
        if (rerollCount >= MAX_REROLLS) {
            return false;
        }

        int rerollCost = getRerollCost(trader);
        boolean paid = MythicCoinWallet.spend(player, rerollCost);
        if (!paid) {
            return false;
        }

        rollVisibleOffers(trader, player.getRandom(), getPlayerLevel(player));
        CompoundTag tag = trader.getPersistentData();
        tag.putInt(REROLL_COUNT_KEY, rerollCount + 1);
        return true;
    }

    public static boolean consumeStock(GatekeeperEntity trader, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= ShopkeeperMenu.GRID_SLOT_COUNT) {
            return false;
        }

        int[] stocks = getOfferStocks(trader);
        if (stocks[slotIndex] <= 0) {
            return false;
        }

        stocks[slotIndex]--;
        trader.getPersistentData().putIntArray(STOCK_KEY, stocks);
        return true;
    }

    public static void restoreStock(GatekeeperEntity trader, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= ShopkeeperMenu.GRID_SLOT_COUNT) {
            return;
        }

        int[] stocks = getOfferStocks(trader);
        stocks[slotIndex]++;
        trader.getPersistentData().putIntArray(STOCK_KEY, stocks);
    }

    private static void creditCoins(GatewayEntity gate, int amount) {
        if (amount <= 0) {
            return;
        }
        ServerPlayer player = LevelUpGatewayXpRewards.findRewardPlayer(gate);
        if (player != null) {
            MythicCoinWallet.add(player, amount);
        }
    }

    private static void spawnGatewayLoot(GatewayEntity gate, int rolls) {
        RandomSource random = gate.level().random;
        for (int index = 0; index < rolls; index++) {
            if (random.nextFloat() < STABILITY_PEARL_DROP_CHANCE) {
                spawnGatewayDrop(gate, new ItemStack(ModItems.STABILITY_PEARL.get()));
                continue;
            }

            LootMaterialItem item = rollGatewayLoot(gate, random);
            if (item == null) {
                continue;
            }

            int stackSize = switch (item.rarity()) {
                case COMMON -> 4 + random.nextInt(5);
                case UNCOMMON -> 2 + random.nextInt(4);
                case RARE -> 1 + random.nextInt(2);
                case EPIC, LEGENDARY, UNIQUE -> 1;
            };

            spawnGatewayDrop(gate, new ItemStack(item, stackSize));
        }
    }

    private static void spawnGatewayDrop(GatewayEntity gate, ItemStack stack) {
        RandomSource random = gate.level().random;
        ItemEntity itemEntity = new ItemEntity(gate.level(), gate.getX(), gate.getY() + 1.0D, gate.getZ(), stack);
            itemEntity.setDeltaMovement(
                    random.nextDouble() * 0.24D - 0.12D,
                    0.22D + random.nextDouble() * 0.10D,
                    random.nextDouble() * 0.24D - 0.12D);
            itemEntity.setNoPickUpDelay();
            gate.level().addFreshEntity(itemEntity);
    }

    private static LootMaterialItem rollGatewayLoot(GatewayEntity gate, RandomSource random) {
        LootMaterialItem[] pool = getDropPoolForGate(gate);
        int totalWeight = java.util.Arrays.stream(pool).mapToInt(item -> getGatewayDropWeight(item, gate)).sum();
        if (totalWeight <= 0) {
            return null;
        }

        int roll = random.nextInt(totalWeight);
        for (LootMaterialItem item : pool) {
            roll -= getGatewayDropWeight(item, gate);
            if (roll < 0) {
                return item;
            }
        }

        return pool[0];
    }

    private static void rollVisibleOffers(GatekeeperEntity trader, RandomSource random, int playerLevel) {
        List<ShopOfferDefinition> allOffers = ShopOfferDefinition.allOffers();
        int tempCount = Math.min(ShopkeeperMenu.GRID_SLOT_COUNT, allOffers.size());
        boolean dungeonShop = trader.level().dimension() == ModDimensions.DUNGEON_LEVEL;
        int[] picks = pickTempOfferIndexes(random, tempCount, playerLevel, dungeonShop);
        List<ShopOfferDefinition> offers = buildOffers(picks);
        trader.getPersistentData().putIntArray(TEMP_TRADE_KEY, picks);
        trader.getPersistentData().putIntArray(STOCK_KEY, rollStocks(offers, random));
        trader.getPersistentData().putIntArray(PRICE_KEY, rollPrices(offers, random));
    }

    private static int[] rollStocks(java.util.List<ShopOfferDefinition> offers, RandomSource random) {
        int[] stocks = new int[ShopkeeperMenu.GRID_SLOT_COUNT];
        for (int index = 0; index < stocks.length; index++) {
            if (index >= offers.size()) {
                stocks[index] = 0;
                continue;
            }

            stocks[index] = offers.get(index).rollStock(random);
        }
        return stocks;
    }

    private static int[] rollPrices(java.util.List<ShopOfferDefinition> offers, RandomSource random) {
        int[] prices = new int[ShopkeeperMenu.GRID_SLOT_COUNT];
        for (int index = 0; index < prices.length; index++) {
            if (index >= offers.size()) {
                prices[index] = 0;
                continue;
            }

            prices[index] = offers.get(index).rollPrice(random);
        }
        return prices;
    }

    private static java.util.List<ShopOfferDefinition> buildOffers(int[] tempIndexes) {
        java.util.List<ShopOfferDefinition> offers = new java.util.ArrayList<>(tempIndexes.length);
        List<ShopOfferDefinition> allOffers = ShopOfferDefinition.allOffers();
        for (int tempIndex : tempIndexes) {
            if (tempIndex >= 0 && tempIndex < allOffers.size()) {
                offers.add(allOffers.get(tempIndex));
            }
        }
        return offers;
    }

    private static int[] pickTempOfferIndexes(RandomSource random, int tempCount, int playerLevel, boolean dungeonShop) {
        List<ShopOfferDefinition> allOffers = ShopOfferDefinition.allOffers();
        Set<String> activePaxelOfferIds = activePaxelOfferIds(allOffers, playerLevel);
        java.util.List<Integer> eligible = allOffers.stream()
                .filter(offer -> offer.minLevel() <= playerLevel && playerLevel <= offer.maxLevel())
                .filter(offer -> !isSuppressedMidgameMaterialOffer(offer, playerLevel))
                .filter(offer -> !isRetiredPaxelOffer(offer, activePaxelOfferIds))
                .filter(offer -> dungeonShop ? isDungeonAidOffer(offer) : isDungeonAidOffer(offer) || isPremiumUtilityOffer(offer))
                .map(allOffers::indexOf)
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        if (eligible.isEmpty()) {
            return new int[0];
        }

        int pickCount = Math.min(tempCount, eligible.size());
        int[] picks = new int[pickCount];
        for (int index = 0; index < pickCount; index++) {
            int totalWeight = eligible.stream().mapToInt(ShopkeeperManager::getOfferWeight).sum();
            if (totalWeight <= 0) {
                picks[index] = eligible.remove(random.nextInt(eligible.size()));
                continue;
            }

            int roll = random.nextInt(totalWeight);
            int selectedPos = 0;
            for (int pos = 0; pos < eligible.size(); pos++) {
                roll -= getOfferWeight(eligible.get(pos));
                if (roll < 0) {
                    selectedPos = pos;
                    break;
                }
            }
            picks[index] = eligible.remove(selectedPos);
        }
        return picks;
    }

    private static boolean isSuppressedMidgameMaterialOffer(ShopOfferDefinition offer, int playerLevel) {
        if (playerLevel < 20) {
            return false;
        }
        return offer.id().equals("iron_ingot")
                || offer.id().equals("gold_ingot")
                || offer.id().equals("diamond");
    }

    private static boolean isDungeonAidOffer(ShopOfferDefinition offer) {
        ItemStack preview = offer.previewStack();
        if (preview.isEmpty()) {
            return false;
        }
        if (preview.is(Items.GOLDEN_APPLE) || preview.is(Items.ENCHANTED_GOLDEN_APPLE)
                || preview.is(ModItems.ARCANE_APPLE.get()) || preview.is(ModItems.ENCHANTED_ARCANE_APPLE.get())) {
            return true;
        }
        if (isHealingPotion(preview)) {
            return true;
        }
        return isSupplyOffer(preview);
    }

    private static boolean isHealingPotion(ItemStack stack) {
        if (!(stack.getItem() instanceof PotionItem)) {
            return false;
        }
        var contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        var potion = contents.potion().orElse(null);
        return potion != null && (potion.is(Potions.HEALING)
                || potion.is(Potions.STRONG_HEALING)
                || potion.is(Potions.REGENERATION)
                || potion.is(Potions.STRONG_REGENERATION));
    }

    private static boolean isSupplyOffer(ItemStack preview) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(preview.getItem());
        if (itemId != null && "gatewayexpansion".equals(itemId.getNamespace()) && itemId.getPath().endsWith("_magnet")) {
            return true;
        }
        return preview.is(ModItems.GRIMSTONE.get())
                || preview.is(ModItems.MYSTIC_ESSENCE.get())
                || preview.is(ModItems.SCRAP_METAL.get())
                || preview.is(ModItems.MANA_GEMS.get())
                || preview.is(ModItems.MANA_STEEL_SCRAP.get())
                || preview.is(ModItems.MAGNETITE_SCRAP.get())
                || preview.is(ModItems.ARCANE_ESSENCE.get())
                || preview.is(ModItems.MANASTONES.get())
                || preview.is(ModItems.ELIXRITE_SCRAP.get())
                || preview.is(ModItems.ASTRITE_SCRAP.get())
                || preview.is(ModItems.SOLAR_SHARD.get())
                || preview.is(ModItems.DARK_ESSENCE.get())
                || preview.is(ModItems.RUSTY_COIN.get())
                || preview.is(ModItems.HARDENED_FLESH.get());
    }

    private static boolean isPremiumUtilityOffer(ShopOfferDefinition offer) {
        return offer.id().startsWith("premium_");
    }

    private static Set<String> activePaxelOfferIds(List<ShopOfferDefinition> allOffers, int playerLevel) {
        List<ShopOfferDefinition> unlockedPaxels = new ArrayList<>();
        for (int index = allOffers.size() - 1; index >= 0; index--) {
            ShopOfferDefinition offer = allOffers.get(index);
            if (!isPaxelOffer(offer) || offer.minLevel() > playerLevel || playerLevel > offer.maxLevel()) {
                continue;
            }
            unlockedPaxels.add(offer);
            if (unlockedPaxels.size() >= 2) {
                break;
            }
        }
        return unlockedPaxels.stream().map(ShopOfferDefinition::id).collect(java.util.stream.Collectors.toSet());
    }

    private static boolean isRetiredPaxelOffer(ShopOfferDefinition offer, Set<String> activePaxelOfferIds) {
        return isPaxelOffer(offer) && !activePaxelOfferIds.contains(offer.id());
    }

    private static boolean isPaxelOffer(ShopOfferDefinition offer) {
        return offer.id().endsWith("_paxel");
    }

    private static int getOfferWeight(int offerIndex) {
        ShopOfferDefinition offer = ShopOfferDefinition.allOffers().get(offerIndex);
        ItemStack preview = offer.previewStack();
        if (isHealingPotion(preview)) {
            return 4;
        }
        if (preview.is(Items.ENCHANTED_GOLDEN_APPLE) || preview.is(ModItems.ENCHANTED_ARCANE_APPLE.get())) {
            return 1;
        }
        if (preview.is(Items.GOLDEN_APPLE) || preview.is(ModItems.ARCANE_APPLE.get())) {
            return 3;
        }
        if (preview.is(ModItems.SOLAR_SHARD.get()) || preview.is(ModItems.DARK_ESSENCE.get()) || preview.is(ModItems.ASTRITE_SCRAP.get())) {
            return 2;
        }
        if (isPremiumUtilityOffer(offer)) {
            return 1;
        }
        return 9;
    }

    private static LootMaterialItem[] getDropPoolForGate(GatewayEntity gate) {
        LootMaterialItem[] gatewayDropPool = GATEWAY_DROP_ITEMS.stream().map(DeferredHolder::get).toArray(LootMaterialItem[]::new);
        if (gate == null || GatewayForgeService.getGatewayCrystalTier(gate.getGateway()) >= 4) {
            return gatewayDropPool;
        }

        return java.util.Arrays.stream(gatewayDropPool)
                .filter(item -> item != ModItems.PRISMATIC_CORE.get())
                .filter(item -> item != ModItems.LUNARIUM_SCRAP.get())
                .toArray(LootMaterialItem[]::new);
    }

    private static int getGatewayDropWeight(LootMaterialItem item, GatewayEntity gate) {
        if (item == ModItems.MANA_STEEL_SCRAP.get()) {
            return LootRarity.COMMON.weight();
        }
        if (item == ModItems.UPGRADE_BASE.get()) {
            return getGateLevel(gate) >= 5 ? LootRarity.UNCOMMON.weight() : 0;
        }
        if (item == ModItems.MAGNETITE_SCRAP.get()) {
            return isLevel20PlusGate(gate) ? LootRarity.UNCOMMON.weight() : LootRarity.COMMON.weight();
        }
        if (item == ModItems.MAGNETITE_INGOT.get()) {
            return isLevel20PlusGate(gate) ? LootRarity.UNCOMMON.weight() : 0;
        }
        if (item == ModItems.ELIXRITE_SCRAP.get()) {
            return isLevel20PlusGate(gate) ? LootRarity.UNCOMMON.weight() : 0;
        }
        if (item == ModItems.ASTRITE_SCRAP.get()) {
            return isLevel20PlusGate(gate) ? LootRarity.UNCOMMON.weight() : 0;
        }
        if (item == ModItems.LUNARIUM_SCRAP.get()) {
            return isLevel20PlusGate(gate) ? LootRarity.RARE.weight() : 0;
        }
        if (item == ModItems.PRISMATIC_SHARD.get()) {
            return getGateLevel(gate) >= 50 ? LootRarity.EPIC.weight() : 0;
        }
        if (item == ModItems.PRISMATIC_DIAMOND.get()) {
            return getGateLevel(gate) >= 50 ? LootRarity.LEGENDARY.weight() : 0;
        }
        return item.rarity().weight();
    }

    private static boolean isLevel20PlusGate(GatewayEntity gate) {
        return gate != null && GatewayForgeService.getGatewayCrystalTier(gate.getGateway()) >= 2;
    }

    private static int getGateLevel(GatewayEntity gate) {
        return gate == null ? 0 : Math.max(0, GatewayForgeService.getGatewayLevel(gate.getGateway()));
    }

    private static int computeWaveCoinBurst(GatewayEntity gate, RandomSource random) {
        int level = Math.max(1, GatewayForgeService.getGatewayLevel(gate.getGateway()));
        int tier = Math.max(1, GatewayForgeService.getGatewayCrystalTier(gate.getGateway()));
        int wave = Math.max(1, gate.getWave());
        int base = 4 + level + tier * 3 + wave * 2;
        int scaled = (int) Math.round((base + random.nextInt(7)) * GatewayPartyScaling.getRewardMultiplier(gate));
        return Math.max(1, scaled);
    }

    private static int computeCompletionCoinBurst(GatewayEntity gate, RandomSource random) {
        int level = Math.max(1, GatewayForgeService.getGatewayLevel(gate.getGateway()));
        int tier = Math.max(1, GatewayForgeService.getGatewayCrystalTier(gate.getGateway()));
        int wave = Math.max(1, gate.getWave());
        int base = 18 + (level * 2) + tier * 8 + wave * 4;
        int scaled = (int) Math.round((base + random.nextInt(13)) * GatewayPartyScaling.getRewardMultiplier(gate));
        return Math.max(1, scaled);
    }

    private static void sendWaveSummary(GatewayEntity gate, int awardedCoins) {
        ServerPlayer player = LevelUpGatewayXpRewards.findRewardPlayer(gate);
        if (player == null) {
            return;
        }

        int displayedCoins = awardedCoins;
        if (MythicCoinWallet.getTotalMultiplier(player) != 1.0D) {
            displayedCoins = Mth.floor((float) (awardedCoins * MythicCoinWallet.getTotalMultiplier(player)) + 0.5F);
        }

        int levelXp = LevelUpGatewayXpRewards.computeWaveXp(gate);
        String survived = formatElapsedTime(gate.tickCount);
        Component summary = Component.empty()
                .append(Component.literal("◆ ").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal("Coins: " + displayedCoins).withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("Levels: " + levelXp).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("Survived: " + survived).withStyle(ChatFormatting.GOLD));
        player.displayClientMessage(summary, true);
    }

    private static int applyGatewayCoinAdjustments(GatewayEntity gate, int amount) {
        int scaled = (int) Math.round(amount * GatewayForgeService.getGatewayCoinRewardMultiplier(gate.getGateway()));
        int withFlatBonus = scaled + GatewayForgeService.getGatewayFlatCoinBonus(gate.getGateway());
        return Math.max(0, withFlatBonus);
    }

    private static int minimumWaveCoinReward(GatewayEntity gate) {
        int minimumTotal = minimumGateCoinTotal(gate);
        int totalWaves = totalGateWaves(gate);
        int waveBudget = (int) Math.floor(minimumTotal * 0.70D);
        return Math.max(1, waveBudget / Math.max(1, totalWaves));
    }

    private static int minimumCompletionCoinReward(GatewayEntity gate) {
        int minimumTotal = minimumGateCoinTotal(gate);
        int totalWaves = totalGateWaves(gate);
        int waveBudget = (int) Math.floor(minimumTotal * 0.70D);
        int completionBudget = minimumTotal - (waveBudget / Math.max(1, totalWaves)) * Math.max(1, totalWaves);
        return Math.max(1, completionBudget);
    }

    private static int minimumGateCoinTotal(GatewayEntity gate) {
        int level = Math.max(1, GatewayForgeService.getGatewayLevel(gate.getGateway()));
        int scaled = (int) Math.round(level * 22.5D - 125.0D);
        return Math.max(100, scaled);
    }

    private static int totalGateWaves(GatewayEntity gate) {
        if (gate.getGateway() instanceof NormalGateway normalGateway) {
            return Math.max(1, normalGateway.getNumWaves());
        }
        return Math.max(1, gate.getWave() + 1);
    }

    private static String formatElapsedTime(int ticks) {
        int totalSeconds = Math.max(0, ticks / 20);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(java.util.Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    private static int computeWaveLootRolls(GatewayEntity gate, RandomSource random) {
        int level = Math.max(1, GatewayForgeService.getGatewayLevel(gate.getGateway()));
        int tier = Math.max(1, GatewayForgeService.getGatewayCrystalTier(gate.getGateway()));
        int extraPlayers = GatewayPartyScaling.getExtraPlayers(gate);
        int base = 2 + tier + Math.max(0, level / 12) + extraPlayers * (1 + tier);
        return base + random.nextInt(2 + tier);
    }

    private static int computeCompletionLootRolls(GatewayEntity gate, RandomSource random) {
        int level = Math.max(1, GatewayForgeService.getGatewayLevel(gate.getGateway()));
        int tier = Math.max(1, GatewayForgeService.getGatewayCrystalTier(gate.getGateway()));
        int extraPlayers = GatewayPartyScaling.getExtraPlayers(gate);
        int base = 6 + tier * 2 + Math.max(0, level / 8) + extraPlayers * (3 + tier * 2);
        return base + random.nextInt(3 + tier);
    }

    private static boolean isRareOptionalModOffer(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        return id.equals(ResourceLocation.fromNamespaceAndPath("friendsandfoes", "totem_of_illusion"))
                || id.equals(ResourceLocation.fromNamespaceAndPath("friendsandfoes", "totem_of_freezing"))
                || id.equals(ResourceLocation.fromNamespaceAndPath("endermanoverhaul", "enderman_tooth"));
    }

    private static int getPlayerLevel(Player player) {
        if (player == null) {
            return 0;
        }
        return LevelUpIntegration.getEffectiveLevel(player);
    }
}
