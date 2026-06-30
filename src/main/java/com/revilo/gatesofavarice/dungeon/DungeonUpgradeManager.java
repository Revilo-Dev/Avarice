package com.revilo.gatesofavarice.dungeon;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.LoadoutDefinition;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.LoadoutInstance;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCard;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCardType;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCategory;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeContext;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutPresetRegistry;
import com.revilo.gatesofavarice.dungeon.loadout.AuraAttributeSupport;
import com.revilo.gatesofavarice.dungeon.loadout.RunicLoadoutService;
import com.revilo.gatesofavarice.dungeon.loadout.RunicUpgradeService;
import com.revilo.gatesofavarice.item.GatewayCardItem;
import com.revilo.gatesofavarice.item.MagnetItem;
import com.revilo.gatesofavarice.integration.CuriosCompat;
import com.revilo.gatesofavarice.integration.ModCompat;
import com.revilo.gatesofavarice.network.OpenUpgradeCategoryPayload;
import com.revilo.gatesofavarice.network.SyncUpgradeCardsPayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.revilodev.runic.stat.RuneStatType;
import net.revilodev.runic.stat.RuneStats;

public final class DungeonUpgradeManager {
    private static final Map<UUID, UpgradeSession> SESSIONS = new HashMap<>();

    private DungeonUpgradeManager() {}

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    public static boolean openUpgradeScreen(ServerPlayer player) {
        return openUpgradeScreen(player, null, null);
    }

    public static boolean openShopUpgradeScreen(ServerPlayer player) {
        UpgradeSession session = getOrCreateShopSession(player);
        if (session == null) {
            return false;
        }
        PacketDistributor.sendToPlayer(player, new OpenUpgradeCategoryPayload(session.sessionId.toString(), session.definition.displayName(), session.definition.theme().name()));
        return true;
    }

    public static boolean openUpgradeScreen(ServerPlayer player, UUID ownerId, UpgradeCategory preselectedCategory) {
        UpgradeSession session = createSession(player, ownerId);
        if (session == null) {
            return false;
        }
        if (preselectedCategory != null) {
            selectCategory(player, session.sessionId.toString(), preselectedCategory);
        } else {
            PacketDistributor.sendToPlayer(player, new OpenUpgradeCategoryPayload(session.sessionId.toString(), session.definition.displayName(), session.definition.theme().name()));
        }
        return true;
    }

    public static boolean selectShopCategory(ServerPlayer player, UpgradeCategory category, int rerollsLeft, int rerollCost) {
        UpgradeSession session = SESSIONS.get(player.getUUID());
        if (session == null && !openShopUpgradeScreen(player)) {
            return false;
        }
        session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return false;
        }
        syncCategoryCards(player, session, category, rerollsLeft, rerollCost);
        return true;
    }

    public static boolean rerollShopCategory(ServerPlayer player, int rerollsLeft, int rerollCost) {
        UpgradeSession session = SESSIONS.get(player.getUUID());
        if (session == null || session.activeCategory == null) {
            reject(player, "No active category.");
            return false;
        }
        rerollExistingShopCards(player, session, rerollsLeft, rerollCost);
        return true;
    }

    public static boolean selectShopCard(ServerPlayer player, int cardIndex) {
        UpgradeSession session = SESSIONS.get(player.getUUID());
        if (session == null || session.activeCategory == null) {
            reject(player, "No active category.");
            return false;
        }
        List<UpgradeCard> cards = session.cardsByCategory.getOrDefault(session.activeCategory, List.of());
        if (cardIndex < 0 || cardIndex >= cards.size()) {
            reject(player, "Invalid card selection.");
            return false;
        }
        UpgradeCard card = cards.get(cardIndex);
        if (session.purchasedShopCards >= maxShopSelections(player)) {
            reject(player, "You have selected all available cards.");
            return false;
        }
        ItemStack target = representativeTargetStack(player, session.activeCategory, session.activeArmorPiece);
        if (!canApplyCardToTarget(player, target, card)) {
            reject(player, "No rune slots available.");
            return false;
        }
        if (!trySpendForCard(player, session, card)) {
            reject(player, "You do not have enough Mythic Coins.");
            return false;
        }
        applySelectedCard(player, session, card, false);
        removePurchasedShopCard(player, session, card, session.activeCategory);
        return true;
    }

    public static String getActiveSessionId(ServerPlayer player) {
        UpgradeSession session = SESSIONS.get(player.getUUID());
        return session == null ? "" : session.sessionId.toString();
    }

    private static UpgradeSession createSession(ServerPlayer player, UUID ownerId) {
        String loadoutId = resolveLoadoutId(player);
        if (loadoutId == null || loadoutId.isBlank()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("No active loadout gear found.").withStyle(ChatFormatting.RED), true);
            return null;
        }
        LoadoutDefinition definition = LoadoutPresetRegistry.byId(loadoutId).orElse(null);
        if (definition == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Unknown loadout id: " + loadoutId).withStyle(ChatFormatting.RED), true);
            return null;
        }
        UpgradeSession session = new UpgradeSession(
                UUID.randomUUID(),
                new LoadoutInstance(UUID.randomUUID(), definition.id(), player.level().getGameTime() ^ player.getUUID().getMostSignificantBits()),
                definition,
                ownerId
        );
        SESSIONS.put(player.getUUID(), session);
        return session;
    }

    private static UpgradeSession getOrCreateShopSession(ServerPlayer player) {
        int shopWaveNumber = DungeonRunManager.getShopUpgradeWaveNumber(player);
        UpgradeSession existing = SESSIONS.get(player.getUUID());
        if (existing != null && existing.waveOwnerId == null) {
            String loadoutId = resolveLoadoutId(player);
            if (loadoutId != null && !loadoutId.isBlank() && existing.definition.id().equals(loadoutId) && existing.shopWaveNumber == shopWaveNumber) {
                return existing;
            }
        }
        UpgradeSession session = createSession(player, null);
        if (session != null) {
            session.shopWaveNumber = shopWaveNumber;
        }
        return session;
    }

    public static void selectCategory(ServerPlayer player, String sessionIdRaw, UpgradeCategory category) {
        UpgradeSession session = SESSIONS.get(player.getUUID());
        if (session == null || !session.sessionId.toString().equals(sessionIdRaw)) {
            reject(player, "Invalid upgrade session.");
            return;
        }
        int rerollsLeft = session.waveOwnerId == null ? 0 : DungeonRunManager.getUpgradeRerollsLeft(session.waveOwnerId);
        int rerollCost = session.waveOwnerId == null ? 0 : DungeonRunManager.getUpgradeRerollCost(session.waveOwnerId);
        syncCategoryCards(player, session, category, rerollsLeft, rerollCost);
    }

    public static void rerollCards(ServerPlayer player, String sessionIdRaw) {
        UpgradeSession session = SESSIONS.get(player.getUUID());
        if (session == null || !session.sessionId.toString().equals(sessionIdRaw)) {
            reject(player, "Invalid upgrade session.");
            return;
        }
        if (session.activeCategory == null) {
            reject(player, "No active category.");
            return;
        }
        if (session.waveOwnerId == null) {
            reject(player, "This upgrade screen cannot reroll.");
            return;
        }
        if (!DungeonRunManager.consumeUpgradeCardReroll(player, session.waveOwnerId)) {
            reject(player, "Unable to reroll upgrade cards.");
            return;
        }
        session.cardGenerationNonce++;
        session.cardsByCategory.remove(session.activeCategory);
        if (session.activeCategory == UpgradeCategory.ARMOR) {
            session.activeArmorPiece = chooseArmorPieceForCards(player, session.activeArmorPiece);
        }
        selectCategory(player, sessionIdRaw, session.activeCategory);
    }

    public static void selectCard(ServerPlayer player, String sessionIdRaw, String cardId) {
        UpgradeSession session = SESSIONS.get(player.getUUID());
        if (session == null || !session.sessionId.toString().equals(sessionIdRaw)) {
            reject(player, "Invalid upgrade session.");
            return;
        }
        if (session.activeCategory == null) {
            reject(player, "No active category.");
            return;
        }
        UpgradeCard card = session.cardsById.get(cardId);
        if (card == null) {
            reject(player, "Invalid card selection.");
            return;
        }
        ItemStack target = representativeTargetStack(player, session.activeCategory, session.activeArmorPiece);
        if (!canApplyCardToTarget(player, target, card)) {
            reject(player, "No rune slots available.");
            syncCategoryCards(player, session, session.activeCategory, 0, 0);
            return;
        }
        try {
            applySelectedCard(player, session, card, true);
        } finally {
            if (session.waveOwnerId != null) {
                DungeonRunManager.completeWaveUpgradeSelection(player, session.waveOwnerId);
            } else {
                syncCategoryCards(player, session, session.activeCategory, 0, 0);
            }
        }
    }

    private static void applySelectedCard(ServerPlayer player, UpgradeSession session, UpgradeCard card, boolean requireRepresentativeTarget) {
        List<ItemStack> targets = targetStacks(player, session.activeCategory, session.activeArmorPiece);
        ItemStack representativeTarget = targets.isEmpty() ? ItemStack.EMPTY : targets.getFirst();
        if (representativeTarget.isEmpty() && session.activeCategory != UpgradeCategory.ITEM && requireRepresentativeTarget) {
            reject(player, "Missing target item.");
            return;
        }
        UpgradeContext ctx = new UpgradeContext(player.getUUID(), session.instance.instanceId(), 1.0F, 3);
        applyCard(player, representativeTarget, card, ctx, session.definition);
        if (!representativeTarget.isEmpty()) {
            RunicLoadoutService.syncRunicSlots(representativeTarget);
        }
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    private static boolean trySpendForCard(ServerPlayer player, UpgradeSession session, UpgradeCard card) {
        if (card.cost() <= 0) {
            return true;
        }
        return com.revilo.gatesofavarice.currency.MythicCoinWallet.spend(player, card.cost());
    }

    private static void syncCategoryCards(ServerPlayer player, UpgradeSession session, UpgradeCategory category, int rerollsLeft, int rerollCost) {
        if (category == UpgradeCategory.ARMOR && !isArmorPieceTargetValid(player, session.activeArmorPiece)) {
            session.activeArmorPiece = chooseArmorPieceForCards(player, null);
            session.cardsByCategory.remove(category);
        }
        ItemStack target = representativeTargetStack(player, category, session.activeArmorPiece);
        ItemStack preview = target.isEmpty() ? previewStackForCategory(category, player) : target;
        int waveNumber = getSessionWaveNumber(session);
        session.activeCategory = category;
        List<UpgradeCard> cards = session.cardsByCategory.get(category);
        if (cards == null) {
            cards = generateShopCards(player, session, category, target, waveNumber, RunicUpgradeService.CARD_COUNT);
            session.cardsByCategory.put(category, cards);
        }
        rebuildShopCardIndex(session);
        PacketDistributor.sendToPlayer(player, new SyncUpgradeCardsPayload(
                session.sessionId.toString(),
                category.name(),
                preview.copy(),
                rerollsLeft,
                rerollCost,
                session.purchasedShopCards,
                maxShopSelections(player),
                slotCount(target, true),
                slotCount(target, false),
                cards));
    }

    private static void rerollExistingShopCards(ServerPlayer player, UpgradeSession session, int rerollsLeft, int rerollCost) {
        UpgradeCategory category = session.activeCategory;
        if (category == null) {
            reject(player, "No active category.");
            return;
        }
        List<UpgradeCard> existing = session.cardsByCategory.getOrDefault(category, List.of());
        int remainingSlots = existing.size();
        if (category == UpgradeCategory.ARMOR) {
            session.activeArmorPiece = chooseArmorPieceForCards(player, session.activeArmorPiece);
        }
        ItemStack target = representativeTargetStack(player, category, session.activeArmorPiece);
        int waveNumber = getSessionWaveNumber(session);
        session.cardGenerationNonce++;
        List<UpgradeCard> cards = generateShopCards(player, session, category, target, waveNumber, remainingSlots);
        session.cardsByCategory.put(category, cards);
        rebuildShopCardIndex(session);
        ItemStack preview = target.isEmpty() ? previewStackForCategory(category, player) : target;
        PacketDistributor.sendToPlayer(player, new SyncUpgradeCardsPayload(
                session.sessionId.toString(),
                category.name(),
                preview.copy(),
                rerollsLeft,
                rerollCost,
                session.purchasedShopCards,
                maxShopSelections(player),
                slotCount(target, true),
                slotCount(target, false),
                cards));
    }

    private static void removePurchasedShopCard(ServerPlayer player, UpgradeSession session, UpgradeCard card, UpgradeCategory category) {
        List<UpgradeCard> existing = session.cardsByCategory.getOrDefault(category, List.of());
        java.util.ArrayList<UpgradeCard> remaining = new java.util.ArrayList<>(existing.size());
        boolean removed = false;
        for (UpgradeCard candidate : existing) {
            if (!removed && candidate.id().equals(card.id())) {
                removed = true;
                continue;
            }
            remaining.add(candidate);
        }
        if (!removed) {
            return;
        }
        session.purchasedShopCards++;
        ItemStack target = representativeTargetStack(player, category, session.activeArmorPiece);
        remaining.removeIf(candidate -> !canApplyCardToTarget(player, target, candidate));
        session.cardsByCategory.put(category, List.copyOf(remaining));
        rebuildShopCardIndex(session);
        int rerollsLeft = session.waveOwnerId == null ? 0 : DungeonRunManager.getUpgradeRerollsLeft(session.waveOwnerId);
        int rerollCost = session.waveOwnerId == null ? 0 : DungeonRunManager.getUpgradeRerollCost(session.waveOwnerId);
        syncCategoryCards(player, session, category, rerollsLeft, rerollCost);
    }

    private static List<UpgradeCard> generateShopCards(ServerPlayer player, UpgradeSession session, UpgradeCategory category, ItemStack target, int waveNumber, int count) {
        if (count <= 0) {
            return List.of();
        }
        List<UpgradeCard> generated = pricedCards(RunicUpgradeService.generateUpgradeCards(player, target, session.instance, session.definition, category, waveNumber, session.cardGenerationNonce), waveNumber);
        if (generated.size() <= count) {
            return generated;
        }
        return List.copyOf(generated.subList(0, count));
    }

    private static int maxShopSelections(ServerPlayer player) {
        int playerLevel = Math.max(0, com.revilo.gatesofavarice.integration.LevelUpIntegration.getEffectiveLevel(player));
        if (playerLevel <= 5) return 2;
        if (playerLevel <= 10) return 3;
        if (playerLevel <= 15) return 4;
        if (playerLevel <= 20) return 5;
        if (playerLevel <= 30) return 6;
        if (playerLevel <= 40) return 7;
        if (playerLevel <= 50) return 8;
        if (playerLevel <= 75) return 9;
        return 10;
    }

    private static int getSessionWaveNumber(UpgradeSession session) {
        return session.waveOwnerId == null ? Math.max(1, session.shopWaveNumber) : DungeonRunManager.getUpgradeWaveNumber(session.waveOwnerId);
    }

    private static void rebuildShopCardIndex(UpgradeSession session) {
        session.cardsById.clear();
        if (session.activeCategory == null) {
            return;
        }
        for (UpgradeCard generated : session.cardsByCategory.getOrDefault(session.activeCategory, List.of())) {
            session.cardsById.put(generated.id(), generated);
        }
    }

    private static List<UpgradeCard> pricedCards(List<UpgradeCard> cards, int waveNumber) {
        return cards.stream().map(card -> new UpgradeCard(
                card.id(),
                card.type(),
                card.category(),
                card.title(),
                card.targetLabel(),
                card.changeLabel(),
                card.currentValue(),
                card.newValue(),
                card.tier(),
                upgradeCost(card, waveNumber)
        )).toList();
    }

    private static int upgradeCost(UpgradeCard card, int waveNumber) {
        int base = 100;
        int waveScale = Math.max(0, waveNumber - 1) * 35;
        int statLevel = parsedUpgradeLevel(card.currentValue());
        int tierScale = Math.max(0, card.tier() - 1) * 45;
        int typeScale = switch (card.type()) {
            case ADD_OR_UPGRADE_EFFECT -> 90;
            case ITEM_REROLL_PRIMARY_WEAPON, ITEM_REROLL_SECONDARY_WEAPON -> 120;
            case UPGRADE_DUNGEON_MAGNET -> 95;
            case ITEM_REWARD_ABILITY -> 80;
            case ITEM_REWARD_GATEWAY_CARD -> 260;
            case ITEM_REWARD_FOOD, ITEM_REWARD_RESTOCK, UPGRADE_ITEM_SUPPLY -> 30;
            default -> 0;
        };
        return base + waveScale + tierScale + (statLevel * 60) + typeScale;
    }

    private static int parsedUpgradeLevel(String currentValue) {
        if (currentValue == null || currentValue.isBlank() || "-".equals(currentValue.trim())) {
            return 0;
        }
        String cleaned = currentValue.toLowerCase(java.util.Locale.ROOT)
                .replace("lv", "")
                .replace("+", "")
                .replace("%", "")
                .trim();
        try {
            return Math.max(0, Math.round(Float.parseFloat(cleaned)));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean canApplyCardToTarget(ServerPlayer player, ItemStack target, UpgradeCard card) {
        if (!wouldAddRuneEntry(player, target, card)) {
            return true;
        }
        return !target.isEmpty() && RunicLoadoutService.runeSlotsRemaining(target) > 0;
    }

    private static boolean wouldAddRuneEntry(ServerPlayer player, ItemStack target, UpgradeCard card) {
        if (target.isEmpty()) {
            return false;
        }
        return switch (card.type()) {
            case ADD_NEW_RUNE_STAT -> {
                RuneStatType type = resolveCardStatType(card);
                yield type != null && !RuneStats.get(target).has(type);
            }
            case ADD_OR_UPGRADE_EFFECT -> {
                String raw = switch (card.changeLabel()) {
                    case "Thorns" -> "minecraft:thorns";
                    case "Power" -> "minecraft:power";
                    case "Punch" -> "minecraft:punch";
                    case "Flame" -> "minecraft:flame";
                    default -> card.changeLabel().startsWith("effect:") ? card.changeLabel().substring("effect:".length()) : null;
                };
                if (raw == null) {
                    yield false;
                }
                var holder = RunicUpgradeService.resolveEffect(player.serverLevel(), net.minecraft.resources.ResourceLocation.parse(raw));
                if (holder == null) {
                    yield false;
                }
                yield target.getOrDefault(DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).getLevel(holder) <= 0;
            }
            default -> false;
        };
    }

    private static int slotCount(ItemStack target, boolean used) {
        if (target.isEmpty()) {
            return 0;
        }
        return used ? RunicLoadoutService.runeSlotsUsed(target) : RunicLoadoutService.runeSlotsCapacity(target);
    }

    private static void applyCard(ServerPlayer player, ItemStack target, UpgradeCard card, UpgradeContext ctx, LoadoutDefinition definition) {
        try {
            switch (card.type()) {
                case ITEM_REWARD_FOOD, ITEM_REWARD_RESTOCK, ITEM_REWARD_ABILITY, ITEM_REWARD_GATEWAY_CARD, ITEM_REROLL_PRIMARY_WEAPON, ITEM_REROLL_SECONDARY_WEAPON, UPGRADE_ITEM_SUPPLY, UPGRADE_DUNGEON_MAGNET -> {
                    applyItemUpgrade(player, definition, card);
                    return;
                }
                case ADD_OR_UPGRADE_EFFECT -> {
                    String raw = switch (card.changeLabel()) {
                        case "Thorns" -> "minecraft:thorns";
                        case "Power" -> "minecraft:power";
                        case "Punch" -> "minecraft:punch";
                        case "Flame" -> "minecraft:flame";
                        default -> card.changeLabel().startsWith("effect:") ? card.changeLabel().substring("effect:".length()) : null;
                    };
                    if (raw == null) return;
                    var holder = RunicUpgradeService.resolveEffect(player.serverLevel(), net.minecraft.resources.ResourceLocation.parse(raw));
                    if (holder != null) {
                        int requestedLevel = parseEffectLevel(card.newValue());
                        RunicUpgradeService.addOrUpgradeEffect(target, holder, requestedLevel, ctx);
                    }
                }
                case ADD_NEW_RUNE_STAT, INCREASE_EXISTING_STAT_PERCENT, INCREASE_EXISTING_STAT_FLAT, ADD_IMPLICIT, UPGRADE_ARMOR_BASE_STAT -> {
                    if (AuraAttributeSupport.addCardBonus(target, card.title(), card.changeLabel(), parseNumber(card.newValue()))) {
                        return;
                    }
                    if ("Ability Card".equals(card.title()) || "Skill Card".equals(card.title())) {
                        return;
                    }
                    RuneStatType type = resolveCardStatType(card);
                    if (type == null && card.type() == UpgradeCardType.ADD_IMPLICIT) {
                        type = RuneStatType.ATTACK_DAMAGE;
                    }
                    if (type != null && "flame_chance".equals(type.id()) && target.getItem() instanceof SwordItem) {
                        return;
                    }
                    if (type != null && RunicLoadoutService.isStatAllowedForStack(target, type)) {
                        float delta = clampDisplayedStatDelta(type, card.newValue());
                        if (RuneStats.get(target).has(type)) {
                            RunicUpgradeService.upgradeExistingStat(target, type, delta, ctx);
                        } else {
                            RunicUpgradeService.addNewStat(target, type, delta, ctx);
                        }
                    }
                }
            }
        } catch (Exception e) {
            GatewayExpansion.LOGGER.warn("Failed to apply upgrade card {} for {}: {}", card.id(), player.getScoreboardName(), e.toString());
        }
    }

    private static void applyItemUpgrade(ServerPlayer player, LoadoutDefinition definition, UpgradeCard card) {
        RandomSource random = player.getRandom();
        switch (card.type()) {
            case ITEM_REWARD_FOOD -> giveBoundStack(player, new ItemStack(com.revilo.gatesofavarice.registry.ModItems.HEART_FRAGMENT.get(), 8 + random.nextInt(9)));
            case ITEM_REWARD_RESTOCK, UPGRADE_ITEM_SUPPLY -> giveBoundStack(player, rollRestockReward(random, definition, card.changeLabel()));
            case ITEM_REWARD_ABILITY -> {
                giveBoundStack(player, new ItemStack(com.revilo.gatesofavarice.registry.ModItems.ARCANE_APPLE.get(), 1 + random.nextInt(3)));
            }
            case ITEM_REWARD_GATEWAY_CARD -> giveBoundStack(player, new ItemStack(com.revilo.gatesofavarice.registry.ModItems.RARE_BOOSTER_PACK.get()));
            case UPGRADE_DUNGEON_MAGNET -> upgradeDungeonMagnet(player, random);
            case ITEM_REROLL_PRIMARY_WEAPON -> rerollWeapon(player, definition, true, random);
            case ITEM_REROLL_SECONDARY_WEAPON -> rerollWeapon(player, definition, false, random);
            default -> {
            }
        }
    }

    private static void upgradeDungeonMagnet(ServerPlayer player, RandomSource random) {
        ItemStack current = findFirstDungeonMagnet(player);
        if (current.isEmpty()) {
            current = new ItemStack(com.revilo.gatesofavarice.registry.ModItems.DUNGEON_MAGNET.get());
            DungeonGearRoller.rollAndBind(current, random, Math.max(1, com.revilo.gatesofavarice.integration.LevelUpIntegration.getEffectiveLevel(player)), 0L, player.registryAccess());
            DungeonBoundItems.forceMarkDungeonBound(current);
            MagnetItem.upgradeDungeonMagnet(current);
            if (!equipBeltMagnet(player, current)) {
                player.getInventory().add(current);
            }
        } else {
            MagnetItem.upgradeDungeonMagnet(current);
        }
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    private static boolean equipBeltMagnet(ServerPlayer player, ItemStack stack) {
        if (!stack.is(com.revilo.gatesofavarice.registry.ModItems.DUNGEON_MAGNET.get()) || !ModCompat.isAnyLoaded("curios")) {
            return false;
        }
        return CuriosCompat.equipBeltMagnet(player, stack);
    }

    private static Item defaultFoodItem(LoadoutDefinition definition) {
        return definition.supplies().stream().findFirst().map(spec -> spec.item()).orElse(Items.COOKED_BEEF);
    }

    private static ItemStack rollRestockReward(RandomSource random, LoadoutDefinition definition, String bundleType) {
        if (bundleType.endsWith("Arrows") || "Arrow Bundle".equals(bundleType)) {
            return rollArrowBundle(random, bundleType);
        }
        if ("Food Bundle".equals(bundleType)) {
            return new ItemStack(defaultFoodItem(definition), 16 + random.nextInt(17));
        }
        if ("Apple Bundle".equals(bundleType)) {
            return rollAppleBundle(random);
        }
        float roll = random.nextFloat();
        if (roll < 0.18F) {
            return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1);
        }
        return new ItemStack(Items.GOLDEN_APPLE, 5 + random.nextInt(12));
    }

    private static ItemStack rollAppleBundle(RandomSource random) {
        float roll = random.nextFloat();
        if (roll < 0.08F) {
            return new ItemStack(com.revilo.gatesofavarice.registry.ModItems.ENCHANTED_ARCANE_APPLE.get(), 1);
        }
        if (roll < 0.18F) {
            return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1);
        }
        if (roll < 0.52F) {
            return new ItemStack(com.revilo.gatesofavarice.registry.ModItems.ARCANE_APPLE.get(), 2 + random.nextInt(4));
        }
        return new ItemStack(Items.GOLDEN_APPLE, 5 + random.nextInt(12));
    }

    private static ItemStack rollArrowBundle(RandomSource random, String bundleType) {
        String id = switch (bundleType) {
            case "Copper Arrows" -> "arsenal:copper_arrow";
            case "Iron Arrows" -> "arsenal:iron_arrow";
            case "Amethyst Arrows" -> "arsenal:amethyst_arrow";
            case "Golden Arrows" -> "arsenal:golden_arrow";
            case "Diamond Arrows" -> "arsenal:diamond_arrow";
            case "Netherite Arrows" -> "arsenal:netherite_arrow";
            default -> "minecraft:arrow";
        };
        Item item = resolveItem(id);
        return new ItemStack(item == null ? Items.ARROW : item, 32);
    }

    private static Item resolveItem(String id) {
        net.minecraft.resources.ResourceLocation key = net.minecraft.resources.ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
            return null;
        }
        return BuiltInRegistries.ITEM.get(key);
    }

    private static void rerollWeapon(ServerPlayer player, LoadoutDefinition definition, boolean primary, RandomSource random) {
        int playerLevel = Math.max(1, com.revilo.gatesofavarice.integration.LevelUpIntegration.getEffectiveLevel(player));
        ItemStack current = representativeTargetStack(player, primary ? UpgradeCategory.PRIMARY_WEAPON : UpgradeCategory.SECONDARY_WEAPON);
        Item item = primary
                ? DungeonUpgradeWeaponRolls.rollPrimaryUpgradeWeapon(random, playerLevel, current)
                : DungeonUpgradeWeaponRolls.rollSecondaryUpgradeWeapon(random, playerLevel, current);
        ItemStack replacement = new ItemStack(item);
        DungeonGearRoller.rollAndBind(replacement, random, playerLevel, 0L, player.registryAccess());
        RunicLoadoutService.applyRuneSlotCapacity(replacement, RunicLoadoutService.runeSlotsForPlayerLevel(playerLevel));
        if (primary) {
            DungeonBoundItems.markPrimaryWeapon(replacement);
        } else {
            DungeonBoundItems.markSecondaryWeapon(replacement);
        }
        if (!DungeonBoundItems.replaceRoleWeapon(player, replacement)) {
            player.getInventory().add(replacement);
        }
    }

    private static void giveBoundStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!(stack.getItem() instanceof GatewayCardItem)) {
            DungeonBoundItems.forceMarkDungeonBound(stack);
        }
        player.getInventory().add(stack);
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    private static ItemStack representativeTargetStack(ServerPlayer player, UpgradeCategory category) {
        return representativeTargetStack(player, category, null);
    }

    private static ItemStack representativeTargetStack(ServerPlayer player, UpgradeCategory category, String armorPiece) {
        List<ItemStack> targets = targetStacks(player, category, armorPiece);
        return targets.isEmpty() ? ItemStack.EMPTY : targets.getFirst();
    }

    private static List<ItemStack> targetStacks(ServerPlayer player, UpgradeCategory category) {
        return targetStacks(player, category, null);
    }

    private static List<ItemStack> targetStacks(ServerPlayer player, UpgradeCategory category, String armorPiece) {
        return switch (category) {
            case PRIMARY_WEAPON -> List.of(findWeaponByRole(player, DungeonBoundItems.PRIMARY_WEAPON_ROLE));
            case SECONDARY_WEAPON -> List.of(findWeaponByRole(player, DungeonBoundItems.SECONDARY_WEAPON_ROLE));
            case ARMOR -> findArmorPieceTarget(player, armorPiece);
            case ITEM -> List.of(findFirstDungeonMagnet(player));
        };
    }

    private static List<ItemStack> findArmorPieceTarget(ServerPlayer player, String armorPiece) {
        List<ItemStack> armorTargets = findArmorSetTargets(player);
        if (armorTargets.isEmpty()) {
            return List.of();
        }
        if (armorPiece == null || armorPiece.isBlank()) {
            return List.of(armorTargets.getFirst());
        }
        for (ItemStack target : armorTargets) {
            if (armorPiece.equals(armorPieceOf(target))) {
                return List.of(target);
            }
        }
        return List.of(armorTargets.getFirst());
    }

    private static List<ItemStack> findArmorSetTargets(ServerPlayer player) {
        java.util.ArrayList<ItemStack> targets = new java.util.ArrayList<>(4);
        String loadoutId = resolveLoadoutId(player);
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isEmpty()) {
                continue;
            }
            if (loadoutId == null || loadoutId.isBlank() || loadoutId.equals(loadoutIdOf(stack))) {
                targets.add(stack);
            }
        }
        return List.copyOf(targets);
    }

    private static boolean isArmorPieceTargetValid(ServerPlayer player, String armorPiece) {
        if (armorPiece == null || armorPiece.isBlank()) {
            return false;
        }
        for (ItemStack target : findArmorSetTargets(player)) {
            if (armorPiece.equals(armorPieceOf(target))) {
                return true;
            }
        }
        return false;
    }

    private static String chooseArmorPieceForCards(ServerPlayer player, String previousPiece) {
        List<ItemStack> armorTargets = findArmorSetTargets(player);
        if (armorTargets.isEmpty()) {
            return "";
        }
        java.util.ArrayList<String> pieces = new java.util.ArrayList<>(armorTargets.size());
        for (ItemStack target : armorTargets) {
            String piece = armorPieceOf(target);
            if (!piece.isBlank() && (previousPiece == null || !piece.equals(previousPiece))) {
                pieces.add(piece);
            }
        }
        if (pieces.isEmpty()) {
            for (ItemStack target : armorTargets) {
                String piece = armorPieceOf(target);
                if (!piece.isBlank()) {
                    pieces.add(piece);
                }
            }
        }
        return pieces.isEmpty() ? "" : pieces.get(player.getRandom().nextInt(pieces.size()));
    }

    private static ItemStack previewStackForCategory(UpgradeCategory category, ServerPlayer player) {
        return switch (category) {
            case PRIMARY_WEAPON -> {
                ItemStack stack = findWeaponByRole(player, DungeonBoundItems.PRIMARY_WEAPON_ROLE);
                yield stack.isEmpty() ? (!player.getMainHandItem().isEmpty() ? player.getMainHandItem().copy() : new ItemStack(Items.IRON_SWORD)) : stack.copy();
            }
            case SECONDARY_WEAPON -> {
                ItemStack stack = findWeaponByRole(player, DungeonBoundItems.SECONDARY_WEAPON_ROLE);
                yield stack.isEmpty() ? (!player.getOffhandItem().isEmpty() ? player.getOffhandItem().copy() : new ItemStack(Items.IRON_SWORD)) : stack.copy();
            }
            case ARMOR -> {
                List<ItemStack> armorTargets = findArmorSetTargets(player);
                yield armorTargets.isEmpty() ? new ItemStack(Items.IRON_CHESTPLATE) : armorTargets.getFirst().copy();
            }
            case ITEM -> {
                ItemStack magnet = findFirstDungeonMagnet(player);
                yield magnet.isEmpty() ? ItemStack.EMPTY : magnet.copy();
            }
        };
    }

    private static ItemStack findWeaponByRole(ServerPlayer player, String role) {
        if (role.equals(DungeonBoundItems.getWeaponRole(player.getMainHandItem()))) {
            return player.getMainHandItem();
        }
        if (role.equals(DungeonBoundItems.getWeaponRole(player.getOffhandItem()))) {
            return player.getOffhandItem();
        }
        for (ItemStack stack : allInventoryStacks(player)) {
            if (role.equals(DungeonBoundItems.getWeaponRole(stack))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static List<ItemStack> allInventoryStacks(ServerPlayer player) {
        java.util.ArrayList<ItemStack> stacks = new java.util.ArrayList<>(
                player.getInventory().items.size()
                        + player.getInventory().offhand.size()
                        + player.getInventory().armor.size());
        stacks.addAll(player.getInventory().items);
        stacks.addAll(player.getInventory().offhand);
        stacks.addAll(player.getInventory().armor);
        return stacks;
    }

    private static ItemStack findFirstSupplyItem(ServerPlayer player) {
        for (ItemStack stack : allInventoryStacks(player)) {
            if (stack.isEmpty()) continue;
            if (stack.is(Items.ARROW) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.COOKED_BEEF) || stack.is(com.revilo.gatesofavarice.registry.ModItems.ARCANE_APPLE.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack findFirstDungeonMagnet(ServerPlayer player) {
        if (ModCompat.isAnyLoaded("curios")) {
            ItemStack beltMagnet = CuriosCompat.findBeltMagnet(player);
            if (!beltMagnet.isEmpty()) {
                return beltMagnet;
            }
        }
        for (ItemStack stack : allInventoryStacks(player)) {
            if (stack.isEmpty()) continue;
            if (stack.is(com.revilo.gatesofavarice.registry.ModItems.DUNGEON_MAGNET.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static String resolveLoadoutId(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isEmpty()) continue;
            String id = loadoutIdOf(stack);
            if (!id.isBlank()) return id;
        }
        return null;
    }

    private static String loadoutIdOf(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(GatewayExpansion.MOD_ID);
        return root.getString("loadout_id");
    }

    private static String armorPieceOf(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(GatewayExpansion.MOD_ID);
        return root.getString("armor_piece");
    }

    private static float parseNumber(String text) {
        String cleaned = text.replace("+", "").replace("%", "").trim();
        return Float.parseFloat(cleaned);
    }

    private static float clampDisplayedStatDelta(RuneStatType type, String text) {
        float parsed = parseNumber(text);
        return Math.max(type == RuneStatType.ATTACK_DAMAGE ? 0.1F : 0.01F, parsed);
    }

    private static RuneStatType resolveCardStatType(UpgradeCard card) {
        RuneStatType direct = RuneStatType.byId(card.changeLabel());
        if (direct != null) {
            return direct;
        }
        return switch (card.changeLabel()) {
            case "Toxic" -> RuneStatType.byId("poison_chance");
            case "Fire Aspect" -> RuneStatType.byId("flame_chance");
            case "Withering" -> RuneStatType.byId("withering_chance");
            case "Bleeding" -> RuneStatType.byId("bleeding_chance");
            case "Stunning" -> RuneStatType.byId("stun_chance");
            case "Shocking" -> RuneStatType.byId("shocking_chance");
            case "Leeching" -> RuneStatType.byId("leeching_chance");
            case "Freezing" -> RuneStatType.byId("freezing_chance");
            case "Fangs" -> RuneStatType.byId("fangs");
            case "Health Boost" -> RuneStatType.byId("health");
            case "Toughness" -> RuneStatType.byId("toughness");
            case "Leaping" -> RuneStatType.byId("jump_height");
            case "Ability Power" -> RuneStatType.byId("ability_power");
            case "Power" -> RuneStatType.byId("power");
            case "Strength" -> RuneStatType.byId("attack_damage");
            case "Rampage" -> RuneStatType.byId("attack_speed");
            case "Poison" -> RuneStatType.byId("poison_chance");
            case "Fire" -> RuneStatType.byId("flame_chance");
            case "Ice" -> RuneStatType.byId("freezing_chance");
            case "Lightning" -> RuneStatType.byId("shocking_chance");
            case "Wind" -> RuneStatType.byId("withering_chance");
            case "Movement Speed" -> RuneStatType.byId("movement_speed");
            case "Resistance" -> RuneStatType.byId("resistance");
            case "Fire Resistance" -> RuneStatType.byId("fire_resistance");
            case "Projectile Resistance" -> RuneStatType.byId("projectile_resistance");
            case "Blast Resistance" -> RuneStatType.byId("blast_resistance");
            case "Attack Damage" -> RuneStatType.byId("attack_damage");
            case "Undead Damage" -> RuneStatType.byId("undead_damage");
            case "Attack Range" -> RuneStatType.byId("attack_range");
            case "Attack Speed" -> RuneStatType.byId("attack_speed");
            case "Draw Speed" -> RuneStatType.byId("draw_speed");
            case "Sweeping Range" -> RuneStatType.byId("sweeping_range");
            case "Aegis" -> RuneStatType.byId("aegis");
            case "Stone Skin" -> RuneStatType.byId("stone");
            default -> null;
        };
    }

    private static int parseEffectLevel(String text) {
        String cleaned = text.replace("Lv", "").replace("lv", "").replace("+", "").trim();
        return Math.max(1, Integer.parseInt(cleaned));
    }

    private static void reject(ServerPlayer player, String reason) {
        GatewayExpansion.LOGGER.warn("Rejected upgrade request from {}: {}", player.getScoreboardName(), reason);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(reason).withStyle(ChatFormatting.RED), true);
    }

    private static final class UpgradeSession {
        private final UUID sessionId;
        private final LoadoutInstance instance;
        private final LoadoutDefinition definition;
        private final UUID waveOwnerId;
        private UpgradeCategory activeCategory;
        private String activeArmorPiece = "";
        private int shopWaveNumber = 1;
        private long cardGenerationNonce = 0L;
        private int purchasedShopCards = 0;
        private final Map<UpgradeCategory, List<UpgradeCard>> cardsByCategory = new HashMap<>();
        private final Map<String, UpgradeCard> cardsById = new HashMap<>();

        private UpgradeSession(UUID sessionId, LoadoutInstance instance, LoadoutDefinition definition, UUID waveOwnerId) {
            this.sessionId = sessionId;
            this.instance = instance;
            this.definition = definition;
            this.waveOwnerId = waveOwnerId;
        }
    }

    private static final class DungeonUpgradeWeaponRolls {
        private DungeonUpgradeWeaponRolls() {
        }

        private static Item rollPrimaryUpgradeWeapon(RandomSource random, int playerLevel, ItemStack current) {
            return rollSecondaryUpgradeWeapon(random, playerLevel, current);
        }

        private static Item rollSecondaryUpgradeWeapon(RandomSource random, int playerLevel, ItemStack current) {
            int currentTier = weaponTier(current);
            if (currentTier <= 0) {
                return rollPrimaryUpgradeWeapon(random, playerLevel, current);
            }
            Item item = pickWeapon(random, playerLevel, currentTier, true, current.getItem());
            return item == null ? com.revilo.gatesofavarice.registry.ModItems.MANA_STEEL_SWORD.get() : item;
        }

        private static Item pickWeapon(RandomSource random, int playerLevel, int targetTier, boolean allowRareTierUp, Item exclude) {
            int safeLevel = Math.max(1, playerLevel);
            java.util.ArrayList<Item> exactPool = new java.util.ArrayList<>();
            java.util.ArrayList<Item> tierUpPool = new java.util.ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                net.minecraft.resources.ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id == null || !isUpgradeableWeapon(id, item)) {
                    continue;
                }
                int tier = weaponTier(id);
                if ((safeLevel < 10 && tier > 2) || (safeLevel < 35 && tier > 3) || (safeLevel < 50 && tier > 4)) {
                    continue;
                }
                if (exclude != null && item == exclude) {
                    continue;
                }
                if (targetTier <= 0) {
                    exactPool.add(item);
                } else if (tier == targetTier) {
                    exactPool.add(item);
                } else if (allowRareTierUp && tier == targetTier + 1) {
                    tierUpPool.add(item);
                }
            }
            if (allowRareTierUp && !tierUpPool.isEmpty() && random.nextFloat() < 0.12F) {
                return tierUpPool.get(random.nextInt(tierUpPool.size()));
            }
            if (!exactPool.isEmpty()) {
                return exactPool.get(random.nextInt(exactPool.size()));
            }
            if (!tierUpPool.isEmpty()) {
                return tierUpPool.get(random.nextInt(tierUpPool.size()));
            }
            return null;
        }

        private static boolean isUpgradeableWeapon(net.minecraft.resources.ResourceLocation id, Item item) {
            String path = id.getPath();
            if ("gatesofavarice".equals(id.getNamespace())) {
                return path.contains("paxel")
                    || path.contains("sword")
                    || path.contains("broadsword")
                    || path.contains("dagger")
                    || path.contains("gaundao")
                    || path.contains("glaive")
                    || path.contains("hammer")
                    || path.contains("longsword")
                    || path.contains("machete");
            }
            return path.contains("paxel")
                    || path.contains("sword")
                    || path.contains("broadsword")
                    || path.contains("dagger")
                    || path.contains("gaundao")
                    || path.contains("glaive")
                    || path.contains("hammer")
                    || path.contains("longsword")
                    || path.contains("machete");
        }

        private static int weaponTier(ItemStack stack) {
            return weaponTier(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }

        private static int weaponTier(net.minecraft.resources.ResourceLocation id) {
            if (id == null) return 1;
            String path = id.getPath();
            if (path.contains("mana_steel")) return 1;
            if (path.contains("elixrite")) return 2;
            if (path.contains("astrite") || path.contains("lunarium")) return 3;
            if (path.contains("ignite") || path.contains("iridium")) return 4;
            return 5;
        }
    }
}
