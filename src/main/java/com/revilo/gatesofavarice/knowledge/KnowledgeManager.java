package com.revilo.gatesofavarice.knowledge;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.config.GatewayExpansionConfig;
import com.revilo.gatesofavarice.network.KnowledgeLibraryPayload;
import com.revilo.gatesofavarice.registry.ModItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.revilodev.runic.screen.custom.ArtisansWorkbenchMenu;
import net.revilodev.runic.item.custom.EtchingItem;
import net.revilodev.runic.item.custom.RuneItem;
import net.revilodev.runic.stat.RuneStatType;
import net.revilodev.runic.stat.RuneStats;

/** Player-owned knowledge collection, rewards, and Create/Dark Utilities interaction gate. */
public final class KnowledgeManager {
    private static final String ROOT_KEY = "gatesofavarice_knowledge";
    private static final String UNLOCKED_KEY = "unlocked";
    private static final String UNREAD_KEY = "unread";
    private static final String COLLECTED_DAY_KEY = "collected_days";
    private static final String BOOK_DATA_KEY = "gatesofavarice";
    private static final String BOOK_ENTRY_KEY = "knowledge_id";
    private static final Component LOCKED_MESSAGE = Component.literal("You arent sure how to use this item").withStyle(ChatFormatting.RED);
    private static final Set<String> BASIC_RUNIC_STATS = Set.of(
            "attack_speed", "attack_damage", "movement_speed", "sweeping_range", "durability", "resistance");
    private static final List<String> INSCRIPTION_IDS = List.of(
            "repair", "expansion", "nullification", "upgrade", "reroll", "cursed", "wild", "extraction");

    private KnowledgeManager() {
    }

    public static List<KnowledgeEntry> entries() {
        ArrayList<KnowledgeEntry> entries = new ArrayList<>();
        for (String raw : GatewayExpansionConfig.KNOWLEDGE_ENTRIES.get()) {
            String[] parts = raw.split("\\|", -1);
            if (parts.length < 5 || parts[0].isBlank()) continue;
            KnowledgeCategory category = parts.length >= 6
                    ? KnowledgeCategory.parse(parts[5])
                    : KnowledgeCategory.TECHNOLOGY;
            entries.add(new KnowledgeEntry(parts[0].trim(), KnowledgeRarity.parse(parts[1]), parts[2].trim(), parts[3].trim(), parts[4].trim(), category));
        }
        addRunicEntries(entries);
        entries.sort(Comparator.comparing(KnowledgeEntry::id));
        return List.copyOf(entries);
    }

    private static void addRunicEntries(List<KnowledgeEntry> entries) {
        Set<String> known = entries.stream().map(KnowledgeEntry::id).collect(java.util.stream.Collectors.toSet());
        for (RuneStatType type : RuneStatType.values()) {
            if (BASIC_RUNIC_STATS.contains(type.id())) continue;
            String id = statKnowledgeId(type.id());
            if (known.add(id)) entries.add(runicEntry(id, titleCase(type.id()), "Allows this Runic stat to be applied."));
        }
        for (String raw : GatewayExpansionConfig.ALLOWED_LOADOUT_EFFECTS.get()) {
            ResourceLocation effectId = ResourceLocation.tryParse(raw);
            if (effectId == null) continue;
            String id = effectKnowledgeId(effectId);
            if (known.add(id)) entries.add(runicEntry(id, titleCase(effectId.getPath()), "Allows this Runic enchantment to be applied."));
        }
        for (String inscription : INSCRIPTION_IDS) {
            String id = inscriptionKnowledgeId(inscription);
            if (known.add(id)) entries.add(runicEntry(id, titleCase(inscription) + " Inscription", "Allows this Runic inscription to be applied."));
        }
    }

    private static KnowledgeEntry runicEntry(String id, String title, String description) {
        return new KnowledgeEntry(id, KnowledgeRarity.RARE, title, description, "", KnowledgeCategory.ENCHANTS);
    }

    public static Optional<KnowledgeEntry> entry(String id) {
        return entries().stream().filter(entry -> entry.id.equals(id)).findFirst();
    }

    public static Optional<KnowledgeEntry> findEntry(String name) {
        return entries().stream()
                .filter(entry -> entry.id.equalsIgnoreCase(name) || entry.title.equalsIgnoreCase(name))
                .findFirst();
    }

    public static ItemStack createGodlyBook(ServerPlayer player, RandomSource random) {
        List<KnowledgeEntry> missing = entries().stream().filter(entry -> !hasKnowledge(player, entry.id)).toList();
        List<KnowledgeEntry> choices = missing.isEmpty() ? entries() : missing;
        if (choices.isEmpty()) return new ItemStack(ModItems.USELESS_KNOWLEDGE_BOOK.get());
        KnowledgeEntry entry = choices.get(random.nextInt(choices.size()));
        ItemStack stack = new ItemStack(ModItems.GODLY_KNOWLEDGE_BOOK.get());
        CompoundTag all = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag data = all.getCompound(BOOK_DATA_KEY);
        data.putString(BOOK_ENTRY_KEY, entry.id);
        all.put(BOOK_DATA_KEY, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(all));
        return stack;
    }

    public static Optional<KnowledgeEntry> getBookEntry(ItemStack stack) {
        if (!stack.is(ModItems.GODLY_KNOWLEDGE_BOOK.get())) return Optional.empty();
        CompoundTag all = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!all.contains(BOOK_DATA_KEY, Tag.TAG_COMPOUND)) return Optional.empty();
        return entry(all.getCompound(BOOK_DATA_KEY).getString(BOOK_ENTRY_KEY));
    }

    public static boolean redeem(ServerPlayer player, ItemStack stack) {
        Optional<KnowledgeEntry> entry = getBookEntry(stack);
        if (entry.isEmpty()) {
            player.displayClientMessage(Component.literal("This knowledge book is incomplete.").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (hasKnowledge(player, entry.get().id)) {
            player.displayClientMessage(Component.literal("You have already learned " + entry.get().title + ".").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        Set<String> unlocked = values(player, UNLOCKED_KEY);
        Set<String> unread = values(player, UNREAD_KEY);
        unlocked.add(entry.get().id);
        unread.add(entry.get().id);
        setValues(player, UNLOCKED_KEY, unlocked);
        setValues(player, UNREAD_KEY, unread);
        setCollectedDay(player, entry.get().id, currentDay(player));
        awardNamespaceRecipes(player, entry.get().unlockedNamespace);
        stack.shrink(1);
        player.displayClientMessage(Component.literal("Knowledge gained: " + entry.get().title).withStyle(entry.get().rarity().color()), false);
        sync(player, false);
        return true;
    }

    public static boolean hasKnowledge(Player player, String entryId) {
        return values(player, UNLOCKED_KEY).contains(entryId);
    }

    public static boolean unlock(ServerPlayer player, KnowledgeEntry entry) {
        if (hasKnowledge(player, entry.id)) return false;
        Set<String> unlocked = values(player, UNLOCKED_KEY);
        Set<String> unread = values(player, UNREAD_KEY);
        unlocked.add(entry.id);
        unread.add(entry.id);
        setValues(player, UNLOCKED_KEY, unlocked);
        setValues(player, UNREAD_KEY, unread);
        setCollectedDay(player, entry.id, currentDay(player));
        awardNamespaceRecipes(player, entry.unlockedNamespace);
        sync(player, false);
        return true;
    }

    public static void reset(ServerPlayer player) {
        setValues(player, UNLOCKED_KEY, Set.of());
        setValues(player, UNREAD_KEY, Set.of());
        player.getPersistentData().getCompound(ROOT_KEY).remove(COLLECTED_DAY_KEY);
        sync(player, false);
    }

    public static int unlockAll(ServerPlayer player) {
        int unlockedCount = 0;
        Set<String> unlocked = values(player, UNLOCKED_KEY);
        Set<String> unread = values(player, UNREAD_KEY);
        for (KnowledgeEntry entry : entries()) {
            if (!unlocked.add(entry.id)) continue;
            unread.add(entry.id);
            setCollectedDay(player, entry.id, currentDay(player));
            awardNamespaceRecipes(player, entry.unlockedNamespace);
            unlockedCount++;
        }
        setValues(player, UNLOCKED_KEY, unlocked);
        setValues(player, UNREAD_KEY, unread);
        sync(player, false);
        return unlockedCount;
    }

    public static boolean canUse(Player player, ResourceLocation contentId) {
        if (contentId == null || (!"create".equals(contentId.getNamespace()) && !"darkutils".equals(contentId.getNamespace()))) {
            return true;
        }
        return entries().stream()
                .filter(entry -> contentId.getNamespace().equals(entry.unlockedNamespace))
                .anyMatch(entry -> hasKnowledge(player, entry.id));
    }

    public static void openLibrary(ServerPlayer player) {
        Set<String> unread = values(player, UNREAD_KEY);
        if (!unread.isEmpty()) {
            setValues(player, UNREAD_KEY, Set.of());
        }
        sync(player, true);
    }

    public static void sync(ServerPlayer player, boolean openScreen) {
        Set<String> unlocked = values(player, UNLOCKED_KEY);
        for (String id : unlocked) {
            if (collectedDay(player, id) <= 0) setCollectedDay(player, id, currentDay(player));
        }
        List<String> collectedDays = collectedDays(player).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()).toList();
        PacketDistributor.sendToPlayer(player, new KnowledgeLibraryPayload(openScreen, List.copyOf(unlocked), List.copyOf(values(player, UNREAD_KEY)), collectedDays));
    }

    public static boolean isUpgradeUnlocked(Player player, String statOrEffectId) {
        if (statOrEffectId == null || statOrEffectId.isBlank()) return true;
        if (statOrEffectId.startsWith("effect:")) {
            ResourceLocation id = ResourceLocation.tryParse(statOrEffectId.substring("effect:".length()));
            return id == null || hasKnowledge(player, effectKnowledgeId(id));
        }
        RuneStatType type = RuneStatType.byId(statOrEffectId);
        return type == null || BASIC_RUNIC_STATS.contains(type.id()) || hasKnowledge(player, statKnowledgeId(type.id()));
    }

    public static boolean isInscriptionUnlocked(Player player, String inscriptionId) {
        return hasKnowledge(player, inscriptionKnowledgeId(inscriptionId));
    }

    public static boolean canReceiveRunic(Player player, ItemStack stack) {
        Optional<String> required = requiredKnowledge(stack);
        return required.isEmpty() || hasKnowledge(player, required.get());
    }

    public static Optional<String> requiredKnowledge(ItemStack stack) {
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null || !"runic".equals(itemId.getNamespace())) return Optional.empty();
        String path = itemId.getPath();
        if (path.endsWith("_inscription") || path.endsWith("_rune") && !(stack.getItem() instanceof RuneItem)) {
            String normalized = path.replace("_inscription", "").replace("_rune", "");
            if (INSCRIPTION_IDS.contains(normalized)) return Optional.of(inscriptionKnowledgeId(normalized));
        }
        if (stack.getItem() instanceof RuneItem || stack.getItem() instanceof EtchingItem) {
            RuneStats stats = stack.getItem() instanceof RuneItem
                    ? RuneItem.getRolledStatsForTooltip(stack)
                    : EtchingItem.getRolledStatsForTooltip(stack);
            for (RuneStatType type : stats.view().keySet()) {
                if (!BASIC_RUNIC_STATS.contains(type.id())) return Optional.of(statKnowledgeId(type.id()));
            }
            var effect = stack.getItem() instanceof RuneItem
                    ? RuneItem.getPrimaryEffectEnchantment(stack)
                    : EtchingItem.getPrimaryEffectEnchantment(stack);
            if (effect != null) return effect.unwrapKey().map(key -> effectKnowledgeId(key.location()));
        }
        return Optional.empty();
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        CompoundTag original = event.getOriginal().getPersistentData();
        if (original.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            event.getEntity().getPersistentData().put(ROOT_KEY, original.getCompound(ROOT_KEY).copy());
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getEntity() != null && (!canUse(event.getEntity(), net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()))
                || !canReceiveRunic(event.getEntity(), event.getItemStack()))) {
            event.getToolTip().add(LOCKED_MESSAGE);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock());
        if (!canUse(player, id)) {
            event.setCanceled(true);
            player.displayClientMessage(LOCKED_MESSAGE, true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(event.getPos()).getBlock());
        if (!canUse(player, id)) {
            event.setCanceled(true);
            player.displayClientMessage(LOCKED_MESSAGE, true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!canUse(player, id) || !canReceiveRunic(player, event.getItemStack())) {
            event.setCanceled(true);
            player.displayClientMessage(LOCKED_MESSAGE, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof ArtisansWorkbenchMenu menu)) return;
        ItemStack enhancement = menu.getEnhancementStack();
        if (enhancement.isEmpty() || canReceiveRunic(player, enhancement)) return;
        ItemStack returned = enhancement.copy();
        enhancement.setCount(0);
        if (!player.getInventory().add(returned)) player.drop(returned, false);
        menu.broadcastChanges();
        player.displayClientMessage(LOCKED_MESSAGE, true);
    }

    public static int collectedDay(Player player, String id) {
        return player.getPersistentData().getCompound(ROOT_KEY).getCompound(COLLECTED_DAY_KEY).getInt(id);
    }

    private static Map<String, Integer> collectedDays(Player player) {
        CompoundTag days = player.getPersistentData().getCompound(ROOT_KEY).getCompound(COLLECTED_DAY_KEY);
        Map<String, Integer> values = new LinkedHashMap<>();
        for (String key : days.getAllKeys()) values.put(key, days.getInt(key));
        return values;
    }

    private static void setCollectedDay(Player player, String id, int day) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_KEY);
        CompoundTag days = root.getCompound(COLLECTED_DAY_KEY);
        days.putInt(id, Math.max(1, day));
        root.put(COLLECTED_DAY_KEY, days);
        player.getPersistentData().put(ROOT_KEY, root);
    }

    private static int currentDay(Player player) {
        return Math.max(1, (int) (player.level().getDayTime() / 24000L) + 1);
    }

    private static String statKnowledgeId(String id) { return "runic_stat_" + id; }
    private static String effectKnowledgeId(ResourceLocation id) { return "runic_effect_" + id.getNamespace() + "_" + id.getPath().replace('/', '_'); }
    private static String inscriptionKnowledgeId(String id) { return "runic_inscription_" + id; }
    private static String titleCase(String value) {
        String[] words = value.replace('_', ' ').split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private static Set<String> values(Player player, String key) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_KEY);
        Set<String> values = new HashSet<>();
        for (Tag tag : root.getList(key, Tag.TAG_STRING)) values.add(tag.getAsString());
        return values;
    }

    private static void setValues(Player player, String key, Set<String> values) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_KEY);
        ListTag list = new ListTag();
        values.stream().sorted().map(StringTag::valueOf).forEach(list::add);
        root.put(key, list);
        player.getPersistentData().put(ROOT_KEY, root);
    }

    private static void awardNamespaceRecipes(ServerPlayer player, String namespace) {
        if (namespace == null || namespace.isBlank()) return;
        player.awardRecipes(player.server.getRecipeManager().getRecipes().stream()
                .filter(recipe -> namespace.equals(recipe.id().getNamespace()))
                .toList());
    }

    public enum KnowledgeRarity {
        COMMON(ChatFormatting.WHITE), UNCOMMON(ChatFormatting.GREEN), RARE(ChatFormatting.AQUA), EPIC(ChatFormatting.LIGHT_PURPLE), LEGENDARY(ChatFormatting.GOLD);
        private final ChatFormatting color;
        KnowledgeRarity(ChatFormatting color) { this.color = color; }
        public ChatFormatting color() { return color; }
        private static KnowledgeRarity parse(String value) {
            try { return valueOf(value.trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ignored) { return COMMON; }
        }
    }

    public enum KnowledgeCategory {
        ENCHANTS, TECHNOLOGY, LORE;
        private static KnowledgeCategory parse(String value) {
            try { return valueOf(value.trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ignored) { return TECHNOLOGY; }
        }
    }

    public record KnowledgeEntry(String id, KnowledgeRarity rarity, String title, String description, String unlockedNamespace, KnowledgeCategory category) {
    }
}
