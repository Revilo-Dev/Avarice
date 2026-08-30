package com.revilo.gatesofavarice.knowledge;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.config.GatewayExpansionConfig;
import com.revilo.gatesofavarice.network.KnowledgeLibraryPayload;
import com.revilo.gatesofavarice.registry.ModItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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
import net.neoforged.neoforge.network.PacketDistributor;

/** Player-owned knowledge collection, rewards, and Create/Dark Utilities interaction gate. */
public final class KnowledgeManager {
    private static final String ROOT_KEY = "gatesofavarice_knowledge";
    private static final String UNLOCKED_KEY = "unlocked";
    private static final String UNREAD_KEY = "unread";
    private static final String BOOK_DATA_KEY = "gatesofavarice";
    private static final String BOOK_ENTRY_KEY = "knowledge_id";
    private static final Component LOCKED_MESSAGE = Component.literal("You don't understand how to use this.").withStyle(ChatFormatting.RED);

    private KnowledgeManager() {
    }

    public static List<KnowledgeEntry> entries() {
        ArrayList<KnowledgeEntry> entries = new ArrayList<>();
        for (String raw : GatewayExpansionConfig.KNOWLEDGE_ENTRIES.get()) {
            String[] parts = raw.split("\\|", -1);
            if (parts.length < 5 || parts[0].isBlank()) continue;
            entries.add(new KnowledgeEntry(parts[0].trim(), KnowledgeRarity.parse(parts[1]), parts[2].trim(), parts[3].trim(), parts[4].trim()));
        }
        entries.sort(Comparator.comparing(KnowledgeEntry::id));
        return List.copyOf(entries);
    }

    public static Optional<KnowledgeEntry> entry(String id) {
        return entries().stream().filter(entry -> entry.id.equals(id)).findFirst();
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
        awardNamespaceRecipes(player, entry.get().unlockedNamespace);
        stack.shrink(1);
        player.displayClientMessage(Component.literal("Knowledge gained: " + entry.get().title).withStyle(entry.get().rarity().color()), false);
        sync(player, false);
        return true;
    }

    public static boolean hasKnowledge(Player player, String entryId) {
        return values(player, UNLOCKED_KEY).contains(entryId);
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
        PacketDistributor.sendToPlayer(player, new KnowledgeLibraryPayload(openScreen, List.copyOf(values(player, UNLOCKED_KEY)), List.copyOf(values(player, UNREAD_KEY))));
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
        if (event.getEntity() != null && !canUse(event.getEntity(), net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()))) {
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
        if (!canUse(player, id)) {
            event.setCanceled(true);
            player.displayClientMessage(LOCKED_MESSAGE, true);
        }
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

    public record KnowledgeEntry(String id, KnowledgeRarity rarity, String title, String description, String unlockedNamespace) {
    }
}
