package com.revilo.gatesofavarice.shop;

import com.revilo.gatesofavarice.currency.MythicCoinWallet;
import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import com.revilo.gatesofavarice.dungeon.DungeonInstanceManager;
import com.revilo.gatesofavarice.entity.GatekeeperEntity;
import com.revilo.gatesofavarice.dungeon.ModDimensions;
import com.revilo.gatesofavarice.integration.LevelUpIntegration;
import com.revilo.gatesofavarice.item.MythicCoinStackData;
import com.revilo.gatesofavarice.menu.ShopkeeperMenu;
import com.revilo.gatesofavarice.registry.ModEntities;
import com.revilo.gatesofavarice.registry.ModItems;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ShopkeeperManager {

    private static final String SHOPKEEPER_KEY = "gatesofavarice.shopkeeper";
    private static final String SHOP_SPECIALIST_ROLE_KEY = "gatesofavarice.shop_specialist_role";
    private static final String TEMP_TRADE_KEY = "gatesofavarice.temp_trades";
    private static final String STOCK_KEY = "gatesofavarice.stock";
    private static final String PRICE_KEY = "gatesofavarice.price";
    private static final String REROLL_COUNT_KEY = "gatesofavarice.reroll_count";
    private static final int MAX_REROLLS = 3;
    private static final int BASE_REROLL_COST = 1000;
    private static final double COIN_ATTRACTION_RANGE = 14.0D;
    private static final double COIN_ATTRACTION_FORCE = 0.105D;
    private static final double COIN_ATTRACTION_MAX_SPEED = 1.15D;

    private ShopkeeperManager() {
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

    /** Keeps the two specialist traders present in the archive shop layout. */
    public static void ensureShopSpecialists(ServerLevel level, java.util.UUID instanceOwnerId, Player summoner) {
        ensureSpecialist(level, DungeonInstanceManager.armorerPosition(instanceOwnerId), "armorer", Component.literal("Armorer").withStyle(ChatFormatting.RED), summoner);
        ensureSpecialist(level, DungeonInstanceManager.enchanterPosition(instanceOwnerId), "enchanter", Component.literal("Enchanter").withStyle(ChatFormatting.LIGHT_PURPLE), summoner);
    }

    private static void ensureSpecialist(ServerLevel level, Vec3 position, String role, Component name, Player summoner) {
        boolean present = !level.getEntitiesOfClass(GatekeeperEntity.class,
                new net.minecraft.world.phys.AABB(position, position).inflate(2.0D),
                entity -> role.equals(entity.getPersistentData().getString(SHOP_SPECIALIST_ROLE_KEY))).isEmpty();
        if (present) return;
        GatekeeperEntity trader = spawnShopkeeper(level, position.x(), position.y(), position.z(), summoner);
        if (trader == null) return;
        trader.getPersistentData().putString(SHOP_SPECIALIST_ROLE_KEY, role);
        trader.setCustomName(name);
    }

    public static boolean isShopkeeper(GatekeeperEntity trader) {
        return trader.getPersistentData().getBoolean(SHOPKEEPER_KEY);
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

            // Shop offers are coin-limited, not stock-limited.  A player can keep buying
            // an offer for as long as their wallet can pay for it.
            stocks[index] = Integer.MAX_VALUE;
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
        java.util.List<Integer> eligible = new java.util.ArrayList<>();
        for (int offerIndex = 0; offerIndex < allOffers.size(); offerIndex++) {
            ShopOfferDefinition offer = allOffers.get(offerIndex);
            if (offer.minLevel() > playerLevel || playerLevel > offer.maxLevel()) {
                continue;
            }
            if (isSuppressedMidgameMaterialOffer(offer, playerLevel) || isRetiredPaxelOffer(offer, activePaxelOfferIds)) {
                continue;
            }
            if (dungeonShop ? isDungeonAidOffer(offer) : isDungeonAidOffer(offer) || isPremiumUtilityOffer(offer)) {
                eligible.add(offerIndex);
            }
        }
        if (eligible.isEmpty()) {
            return new int[0];
        }

        int pickCount = Math.min(tempCount, eligible.size());
        int[] picks = new int[pickCount];
        for (int index = 0; index < pickCount; index++) {
            int totalWeight = 0;
            for (int offerIndex : eligible) {
                totalWeight += getOfferWeight(offerIndex);
            }
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
                || preview.is(ModItems.SILK_SPOOL.get())
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
