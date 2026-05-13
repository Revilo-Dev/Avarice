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
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
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
        ItemStack target = targetStack(player, category);
        ItemStack preview = target.isEmpty() ? previewStackForCategory(category, player) : target;
        List<UpgradeCard> cards = RunicUpgradeService.generateUpgradeCards(player, target, session.instance, session.definition, category);
        session.activeCategory = category;
        session.cardsByCategory.put(category, cards);
        session.cardsById.clear();
        for (UpgradeCard card : cards) {
            session.cardsById.put(card.id(), card);
        }
        PacketDistributor.sendToPlayer(player, new SyncUpgradeCardsPayload(session.sessionId.toString(), category.name(), preview.copy(), cards));
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
        ItemStack target = targetStack(player, session.activeCategory);
        if (target.isEmpty() && session.activeCategory != UpgradeCategory.ITEM) {
            reject(player, "Missing target item.");
            if (session.waveOwnerId != null) {
                DungeonRunManager.completeWaveUpgradeSelection(player, session.waveOwnerId);
            }
            return;
        }
        try {
            UpgradeContext ctx = new UpgradeContext(player.getUUID(), session.instance.instanceId(), 1.0F, 3);
            applyCard(player, target, card, ctx, session.definition);
            if (!target.isEmpty()) {
                RunicLoadoutService.syncRunicSlots(target);
            }
            player.inventoryMenu.broadcastChanges();
            player.containerMenu.broadcastChanges();
        } finally {
            // Never stall the run on UI/apply errors during between-wave upgrades.
            if (session.waveOwnerId != null) {
                DungeonRunManager.completeWaveUpgradeSelection(player, session.waveOwnerId);
            } else {
                selectCategory(player, sessionIdRaw, session.activeCategory);
            }
        }
    }

    private static void applyCard(ServerPlayer player, ItemStack target, UpgradeCard card, UpgradeContext ctx, LoadoutDefinition definition) {
        try {
            if (card.type() == UpgradeCardType.UPGRADE_ITEM_SUPPLY || card.category() == UpgradeCategory.ITEM) {
                applyItemUpgrade(player, definition);
                return;
            }
            switch (card.type()) {
                case ADD_OR_UPGRADE_EFFECT -> {
                    if (!card.changeLabel().startsWith("effect:")) return;
                    String raw = card.changeLabel().substring("effect:".length());
                    var holder = RunicUpgradeService.resolveEffect(player.serverLevel(), net.minecraft.resources.ResourceLocation.parse(raw));
                    if (holder != null) {
                        int requestedLevel = parseEffectLevel(card.newValue());
                        RunicUpgradeService.addOrUpgradeEffect(target, holder, requestedLevel, ctx);
                    }
                }
                case ADD_NEW_RUNE_STAT -> {
                    RuneStatType type = RuneStatType.byId(card.changeLabel());
                    if (type != null && RunicLoadoutService.isStatAllowedForStack(target, type)) {
                        RunicUpgradeService.addNewStat(target, type, Math.max(0.01F, parseNumber(card.newValue())), ctx);
                    }
                }
                case INCREASE_EXISTING_STAT_PERCENT -> {
                    RuneStatType type = RuneStatType.byId(card.changeLabel());
                    if (type != null && RunicLoadoutService.isStatAllowedForStack(target, type)) {
                        float pct = Math.max(0.01F, parsePercent(card.newValue()));
                        float current = RuneStats.get(target).get(type);
                        float delta = Math.max(0.01F, current * pct);
                        if (RuneStats.get(target).has(type)) {
                            RunicUpgradeService.upgradeExistingStat(target, type, delta, ctx);
                        } else {
                            RunicUpgradeService.addNewStat(target, type, delta, ctx);
                        }
                    }
                }
                default -> {
                    RuneStatType type = RuneStatType.byId(card.changeLabel());
                    if (type == null && card.type() == UpgradeCardType.ADD_IMPLICIT) {
                        type = RuneStatType.ATTACK_DAMAGE;
                    }
                    if (type != null && RunicLoadoutService.isStatAllowedForStack(target, type)) {
                        float delta = Math.max(0.01F, parseNumber(card.newValue()));
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

    private static void applyItemUpgrade(ServerPlayer player, LoadoutDefinition definition) {
        if (definition.id().contains("ranger") || definition.id().contains("marksman") || definition.id().contains("warlord") || definition.id().contains("nomad")) {
            player.getInventory().add(new ItemStack(Items.ARROW, 16));
            return;
        }
        if (definition.id().contains("vanguard") || definition.id().contains("knight") || definition.id().contains("gladiator")) {
            player.getInventory().add(new ItemStack(Items.GOLDEN_APPLE, 1));
            return;
        }
        if (definition.id().contains("spellblade") || definition.id().contains("reaper") || definition.id().contains("samurai")) {
            player.getInventory().add(new ItemStack(com.revilo.gatesofavarice.registry.ModItems.ARCANE_APPLE.get(), 1));
            return;
        }
        player.getInventory().add(new ItemStack(Items.COOKED_BEEF, 4));
    }

    private static ItemStack targetStack(ServerPlayer player, UpgradeCategory category) {
        return switch (category) {
            case PRIMARY_WEAPON -> findWeaponByRole(player, DungeonBoundItems.PRIMARY_WEAPON_ROLE);
            case SECONDARY_WEAPON -> findWeaponByRole(player, DungeonBoundItems.SECONDARY_WEAPON_ROLE);
            case ARMOR -> !player.getInventory().armor.get(2).isEmpty() ? player.getInventory().armor.get(2) : player.getInventory().armor.get(3);
            case ITEM -> findFirstSupplyItem(player);
        };
    }

    private static ItemStack previewStackForCategory(UpgradeCategory category, ServerPlayer player) {
        return switch (category) {
            case PRIMARY_WEAPON, SECONDARY_WEAPON -> !player.getMainHandItem().isEmpty() ? player.getMainHandItem().copy() : new ItemStack(Items.IRON_SWORD);
            case ARMOR -> new ItemStack(Items.IRON_CHESTPLATE);
            case ITEM -> new ItemStack(Items.GOLDEN_APPLE);
        };
    }

    private static ItemStack findWeaponByRole(ServerPlayer player, String role) {
        for (ItemStack stack : player.getInventory().items) {
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
            CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(GatewayExpansion.MOD_ID);
            String id = root.getString("loadout_id");
            if (!id.isBlank()) return id;
        }
        return null;
    }

    private static float parsePercent(String text) {
        String cleaned = text.replace("+", "").replace("%", "").trim();
        return Float.parseFloat(cleaned) / 100.0F;
    }

    private static float parseNumber(String text) {
        String cleaned = text.replace("+", "").replace("%", "").trim();
        return Float.parseFloat(cleaned);
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
        private final Map<UpgradeCategory, List<UpgradeCard>> cardsByCategory = new HashMap<>();
        private final Map<String, UpgradeCard> cardsById = new HashMap<>();

        private UpgradeSession(UUID sessionId, LoadoutInstance instance, LoadoutDefinition definition, UUID waveOwnerId) {
            this.sessionId = sessionId;
            this.instance = instance;
            this.definition = definition;
            this.waveOwnerId = waveOwnerId;
        }
    }
}
