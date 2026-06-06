package com.revilo.gatesofavarice.dungeon;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.LoadoutDefinition;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.LoadoutInstance;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCard;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCardType;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCategory;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeContext;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutPresetRegistry;
import com.revilo.gatesofavarice.dungeon.loadout.RunicLoadoutService;
import com.revilo.gatesofavarice.dungeon.loadout.RunicUpgradeService;
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

    public static boolean openUpgradeScreen(ServerPlayer player, UUID ownerId, UpgradeCategory preselectedCategory) {
        String loadoutId = resolveLoadoutId(player);
        if (loadoutId == null || loadoutId.isBlank()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("No active loadout gear found.").withStyle(ChatFormatting.RED), true);
            return false;
        }
        LoadoutDefinition definition = LoadoutPresetRegistry.byId(loadoutId).orElse(null);
        if (definition == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Unknown loadout id: " + loadoutId).withStyle(ChatFormatting.RED), true);
            return false;
        }
        UpgradeSession session = new UpgradeSession(
                UUID.randomUUID(),
                new LoadoutInstance(UUID.randomUUID(), definition.id(), player.level().getGameTime() ^ player.getUUID().getMostSignificantBits()),
                definition,
                ownerId
        );
        SESSIONS.put(player.getUUID(), session);
        if (preselectedCategory != null) {
            selectCategory(player, session.sessionId.toString(), preselectedCategory);
        } else {
            PacketDistributor.sendToPlayer(player, new OpenUpgradeCategoryPayload(session.sessionId.toString(), definition.displayName(), definition.theme().name()));
        }
        return true;
    }

    public static void selectCategory(ServerPlayer player, String sessionIdRaw, UpgradeCategory category) {
        UpgradeSession session = SESSIONS.get(player.getUUID());
        if (session == null || !session.sessionId.toString().equals(sessionIdRaw)) {
            reject(player, "Invalid upgrade session.");
            return;
        }
        ItemStack target = representativeTargetStack(player, category);
        ItemStack preview = category == UpgradeCategory.ITEM
                ? ItemStack.EMPTY
                : (target.isEmpty() ? previewStackForCategory(category, player) : target);
        int waveNumber = session.waveOwnerId == null ? 1 : DungeonRunManager.getUpgradeWaveNumber(session.waveOwnerId);
        List<UpgradeCard> cards = RunicUpgradeService.generateUpgradeCards(player, target, session.instance, session.definition, category, waveNumber, session.cardGenerationNonce);
        session.activeCategory = category;
        session.cardsByCategory.put(category, cards);
        session.cardsById.clear();
        for (UpgradeCard card : cards) {
            session.cardsById.put(card.id(), card);
        }
        int rerollsLeft = session.waveOwnerId == null ? 0 : DungeonRunManager.getUpgradeRerollsLeft(session.waveOwnerId);
        int rerollCost = session.waveOwnerId == null ? 0 : DungeonRunManager.getUpgradeRerollCost(session.waveOwnerId);
        PacketDistributor.sendToPlayer(player, new SyncUpgradeCardsPayload(session.sessionId.toString(), category.name(), preview.copy(), rerollsLeft, rerollCost, cards));
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
        List<ItemStack> targets = targetStacks(player, session.activeCategory);
        ItemStack representativeTarget = targets.isEmpty() ? ItemStack.EMPTY : targets.getFirst();
        if (representativeTarget.isEmpty() && session.activeCategory != UpgradeCategory.ITEM) {
            reject(player, "Missing target item.");
            if (session.waveOwnerId != null) {
                DungeonRunManager.completeWaveUpgradeSelection(player, session.waveOwnerId);
            }
            return;
        }
        try {
            UpgradeContext ctx = new UpgradeContext(player.getUUID(), session.instance.instanceId(), 1.0F, 3);
            if (session.activeCategory == UpgradeCategory.ARMOR) {
                for (ItemStack target : targets) {
                    applyCard(player, target, card, ctx, session.definition);
                    if (!target.isEmpty()) {
                        RunicLoadoutService.syncRunicSlots(target);
                    }
                }
            } else {
                applyCard(player, representativeTarget, card, ctx, session.definition);
                if (!representativeTarget.isEmpty()) {
                    RunicLoadoutService.syncRunicSlots(representativeTarget);
                }
            }
            player.inventoryMenu.broadcastChanges();
            player.containerMenu.broadcastChanges();
        } finally {
            if (session.waveOwnerId != null) {
                DungeonRunManager.completeWaveUpgradeSelection(player, session.waveOwnerId);
            } else {
                selectCategory(player, sessionIdRaw, session.activeCategory);
            }
        }
    }

    private static void applyCard(ServerPlayer player, ItemStack target, UpgradeCard card, UpgradeContext ctx, LoadoutDefinition definition) {
        try {
            switch (card.type()) {
                case ITEM_REWARD_FOOD, ITEM_REWARD_RESTOCK, ITEM_REWARD_ABILITY, ITEM_REROLL_PRIMARY_WEAPON, ITEM_REROLL_SECONDARY_WEAPON, UPGRADE_ITEM_SUPPLY -> {
                    applyItemUpgrade(player, definition, card.type());
                    return;
                }
                case ADD_OR_UPGRADE_EFFECT -> {
                    String raw = switch (card.changeLabel()) {
                        case "Thorns" -> "minecraft:thorns";
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
                    RuneStatType type = resolveCardStatType(card);
                    if (type == null && card.type() == UpgradeCardType.ADD_IMPLICIT) {
                        type = RuneStatType.ATTACK_DAMAGE;
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

    private static void applyItemUpgrade(ServerPlayer player, LoadoutDefinition definition, UpgradeCardType type) {
        RandomSource random = player.getRandom();
        switch (type) {
            case ITEM_REWARD_FOOD -> giveBoundStack(player, new ItemStack(com.revilo.gatesofavarice.registry.ModItems.HEART_FRAGMENT.get(), 8 + random.nextInt(9)));
            case ITEM_REWARD_RESTOCK, UPGRADE_ITEM_SUPPLY -> giveBoundStack(player, rollRestockReward(random));
            case ITEM_REWARD_ABILITY -> {
                giveBoundStack(player, new ItemStack(com.revilo.gatesofavarice.registry.ModItems.ARCANE_APPLE.get(), 1 + random.nextInt(3)));
                giveBoundStack(player, DungeonRunManager.rollUpgradeMagnetReward(random, Math.max(1, com.revilo.gatesofavarice.integration.LevelUpIntegration.getEffectiveLevel(player))));
            }
            case ITEM_REROLL_PRIMARY_WEAPON -> rerollWeapon(player, definition, true, random);
            case ITEM_REROLL_SECONDARY_WEAPON -> rerollWeapon(player, definition, false, random);
            default -> {
            }
        }
    }

    private static Item defaultFoodItem(LoadoutDefinition definition) {
        return definition.supplies().stream().findFirst().map(spec -> spec.item()).orElse(Items.COOKED_BEEF);
    }

    private static ItemStack rollRestockReward(RandomSource random) {
        float roll = random.nextFloat();
        if (roll < 0.18F) {
            return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1);
        }
        if (roll < 0.52F) {
            return new ItemStack(Items.ARROW, 32 + random.nextInt(33));
        }
        return new ItemStack(Items.GOLDEN_APPLE, 5 + random.nextInt(12));
    }

    private static void rerollWeapon(ServerPlayer player, LoadoutDefinition definition, boolean primary, RandomSource random) {
        int playerLevel = Math.max(1, com.revilo.gatesofavarice.integration.LevelUpIntegration.getEffectiveLevel(player));
        ItemStack current = representativeTargetStack(player, primary ? UpgradeCategory.PRIMARY_WEAPON : UpgradeCategory.SECONDARY_WEAPON);
        Item item = primary
                ? DungeonUpgradeWeaponRolls.rollPrimaryUpgradeWeapon(random, playerLevel, current)
                : DungeonUpgradeWeaponRolls.rollSecondaryUpgradeWeapon(random, playerLevel, current);
        ItemStack replacement = new ItemStack(item);
        DungeonGearRoller.rollAndBind(replacement, random, playerLevel, 0L, player.registryAccess());
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
        DungeonBoundItems.forceMarkDungeonBound(stack);
        player.getInventory().add(stack);
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    private static ItemStack representativeTargetStack(ServerPlayer player, UpgradeCategory category) {
        List<ItemStack> targets = targetStacks(player, category);
        return targets.isEmpty() ? ItemStack.EMPTY : targets.getFirst();
    }

    private static List<ItemStack> targetStacks(ServerPlayer player, UpgradeCategory category) {
        return switch (category) {
            case PRIMARY_WEAPON -> List.of(findWeaponByRole(player, DungeonBoundItems.PRIMARY_WEAPON_ROLE));
            case SECONDARY_WEAPON -> List.of(findWeaponByRole(player, DungeonBoundItems.SECONDARY_WEAPON_ROLE));
            case ARMOR -> findArmorSetTargets(player);
            case ITEM -> List.of(findFirstSupplyItem(player));
        };
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

    private static ItemStack previewStackForCategory(UpgradeCategory category, ServerPlayer player) {
        return switch (category) {
            case PRIMARY_WEAPON, SECONDARY_WEAPON -> !player.getMainHandItem().isEmpty() ? player.getMainHandItem().copy() : new ItemStack(Items.IRON_SWORD);
            case ARMOR -> new ItemStack(Items.IRON_CHESTPLATE);
            case ITEM -> ItemStack.EMPTY;
        };
    }

    private static ItemStack findWeaponByRole(ServerPlayer player, String role) {
        for (ItemStack stack : player.getInventory().items) {
            if (role.equals(DungeonBoundItems.getWeaponRole(stack))) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (role.equals(DungeonBoundItems.getWeaponRole(stack))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack findFirstSupplyItem(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;
            if (stack.is(Items.ARROW) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.COOKED_BEEF) || stack.is(com.revilo.gatesofavarice.registry.ModItems.ARCANE_APPLE.get())) {
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
            case "Ability Power" -> RuneStatType.byId("power");
            case "Movement Speed" -> RuneStatType.byId("movement_speed");
            case "Resistance" -> RuneStatType.byId("resistance");
            case "Fire Resistance" -> RuneStatType.byId("fire_resistance");
            case "Projectile Resistance" -> RuneStatType.byId("projectile_resistance");
            case "Blast Resistance" -> RuneStatType.byId("blast_resistance");
            case "Attack Damage" -> RuneStatType.byId("attack_damage");
            case "Undead Damage" -> RuneStatType.byId("undead_damage");
            case "Attack Range" -> RuneStatType.byId("attack_range");
            case "Attack Speed" -> RuneStatType.byId("attack_speed");
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
        private long cardGenerationNonce = 0L;
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
