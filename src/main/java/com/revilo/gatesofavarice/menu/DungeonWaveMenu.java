package com.revilo.gatesofavarice.menu;

import com.revilo.gatesofavarice.dungeon.DungeonDeck;
import com.revilo.gatesofavarice.dungeon.DungeonRunManager;
import com.revilo.gatesofavarice.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class DungeonWaveMenu extends AbstractContainerMenu {

    public static final int STAGE_BOOSTER = 0;
    public static final int STAGE_UPGRADE = 1;
    public static final int STAGE_LOADOUT = 2;
    public static final int OPTION_COUNT = 4;
    public static final int BAIL_BUTTON_ID = 100;
    public static final int REROLL_BUTTON_ID = 101;
    public static final int SKIP_BUTTON_ID = 102;

    private final UUID ownerId;
    private final int waveNumber;
    private final boolean ownerCanSelect;
    private final int stage;
    private final int rerollsLeft;
    private final int rerollCost;
    private final List<WaveOptionView> options;
    private final List<DungeonDeck.CardState> deck;

    public DungeonWaveMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(
                containerId,
                inventory,
                data.readUUID(),
                data.readInt(),
                data.readBoolean(),
                data.readInt(),
                data.readInt(),
                data.readInt(),
                readOptions(data),
                readDeck(data)
        );
    }

    public DungeonWaveMenu(int containerId, Inventory inventory, UUID ownerId, int waveNumber, boolean ownerCanSelect, int stage, int rerollsLeft, int rerollCost, List<WaveOptionView> options, List<DungeonDeck.CardState> deck) {
        super(ModMenus.DUNGEON_WAVE.get(), containerId);
        this.ownerId = ownerId;
        this.waveNumber = waveNumber;
        this.ownerCanSelect = ownerCanSelect;
        this.stage = stage;
        this.rerollsLeft = rerollsLeft;
        this.rerollCost = rerollCost;
        this.options = List.copyOf(options);
        this.deck = normalizeDeck(deck);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return DungeonRunManager.handleWaveMenuClick(player, this.ownerId, id);
    }

    @Override
    public boolean stillValid(Player player) {
        return DungeonRunManager.isWaveMenuValid(player, this.ownerId);
    }

    public int waveNumber() {
        return this.waveNumber;
    }

    public boolean ownerCanSelect() {
        return this.ownerCanSelect;
    }

    public List<WaveOptionView> options() {
        return this.options;
    }

    public int stage() {
        return this.stage;
    }

    public int rerollsLeft() {
        return this.rerollsLeft;
    }

    public int rerollCost() {
        return this.rerollCost;
    }

    public List<DungeonDeck.CardState> deck() {
        return this.deck;
    }

    private static List<WaveOptionView> readOptions(RegistryFriendlyByteBuf data) {
        int size = data.readInt();
        ArrayList<WaveOptionView> options = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            options.add(new WaveOptionView(
                    Component.literal(data.readUtf()),
                    Component.literal(data.readUtf()),
                    data.readInt(),
                    data.readInt(),
                    data.readInt(),
                    readStack(data),
                    readStack(data),
                    readStack(data),
                    readStack(data),
                    readStack(data),
                    readStack(data),
                    readStack(data),
                    data.readInt(),
                    data.readInt(),
                    data.readInt(),
                    data.readInt(),
                    data.readInt(),
                    readCardCounts(data)
            ));
        }
        return options;
    }

    public static void writePayload(RegistryFriendlyByteBuf buffer, UUID ownerId, int waveNumber, boolean ownerCanSelect, int stage, int rerollsLeft, int rerollCost, List<WaveOptionView> options, List<DungeonDeck.CardState> deck) {
        buffer.writeUUID(ownerId);
        buffer.writeInt(waveNumber);
        buffer.writeBoolean(ownerCanSelect);
        buffer.writeInt(stage);
        buffer.writeInt(rerollsLeft);
        buffer.writeInt(rerollCost);
        buffer.writeInt(options.size());
        for (WaveOptionView option : options) {
            buffer.writeUtf(option.title().getString());
            buffer.writeUtf(option.details().getString());
            buffer.writeInt(option.inDungeonRewardPercent());
            buffer.writeInt(option.externalRewardPercent());
            buffer.writeInt(option.difficultyRating());
            writeStack(buffer, option.displayStack());
            writeStack(buffer, option.secondaryDisplayStack());
            writeStack(buffer, option.helmetStack());
            writeStack(buffer, option.chestStack());
            writeStack(buffer, option.legsStack());
            writeStack(buffer, option.feetStack());
            writeStack(buffer, option.ammoStack());
            buffer.writeInt(option.ammoCount());
            buffer.writeInt(option.speedRating());
            buffer.writeInt(option.damageRating());
            buffer.writeInt(option.defenceRating());
            buffer.writeInt(option.attackSpeedRating());
            writeCardCounts(buffer, option.pulledCardCounts());
        }
        writeDeck(buffer, deck);
    }

    private static List<Integer> readCardCounts(RegistryFriendlyByteBuf data) {
        int size = data.readInt();
        ArrayList<Integer> counts = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            counts.add(data.readInt());
        }
        return normalizeCardCounts(counts);
    }

    private static void writeCardCounts(RegistryFriendlyByteBuf buffer, List<Integer> cardCounts) {
        List<Integer> normalized = normalizeCardCounts(cardCounts);
        buffer.writeInt(normalized.size());
        for (int count : normalized) {
            buffer.writeInt(count);
        }
    }

    private static List<DungeonDeck.CardState> readDeck(RegistryFriendlyByteBuf data) {
        int cardTypeCount = DungeonDeck.CardType.values().length;
        int size = data.readInt();
        ArrayList<DungeonDeck.CardState> deck = new ArrayList<>(Math.min(size, cardTypeCount));
        for (int index = 0; index < size; index++) {
            int count = data.readInt();
            double appliedPercent = data.readDouble();
            if (index < cardTypeCount) {
                deck.add(new DungeonDeck.CardState(count, appliedPercent));
            }
        }
        return normalizeDeck(deck);
    }

    private static void writeDeck(RegistryFriendlyByteBuf buffer, List<DungeonDeck.CardState> deck) {
        List<DungeonDeck.CardState> normalized = normalizeDeck(deck);
        buffer.writeInt(normalized.size());
        for (DungeonDeck.CardState state : normalized) {
            buffer.writeInt(state.count());
            buffer.writeDouble(state.appliedPercent());
        }
    }

    private static List<Integer> normalizeCardCounts(List<Integer> cardCounts) {
        int cardTypeCount = DungeonDeck.CardType.values().length;
        ArrayList<Integer> normalized = new ArrayList<>(cardTypeCount);
        for (int index = 0; index < cardTypeCount; index++) {
            Integer count = cardCounts != null && index < cardCounts.size() ? cardCounts.get(index) : null;
            normalized.add(Math.max(0, count == null ? 0 : count));
        }
        return List.copyOf(normalized);
    }

    private static List<DungeonDeck.CardState> normalizeDeck(List<DungeonDeck.CardState> deck) {
        int cardTypeCount = DungeonDeck.CardType.values().length;
        ArrayList<DungeonDeck.CardState> states = new ArrayList<>(cardTypeCount);
        for (int index = 0; index < cardTypeCount; index++) {
            DungeonDeck.CardState state = deck != null && index < deck.size() ? deck.get(index) : null;
            states.add(state == null
                    ? new DungeonDeck.CardState(0, 0.0D)
                    : new DungeonDeck.CardState(Math.max(0, state.count()), state.appliedPercent()));
        }
        return List.copyOf(states);
    }

    private static void writeStack(RegistryFriendlyByteBuf buffer, ItemStack stack) {
        buffer.writeNbt(stack.saveOptional(buffer.registryAccess()));
    }

    private static ItemStack readStack(RegistryFriendlyByteBuf buffer) {
        return ItemStack.parseOptional(buffer.registryAccess(), buffer.readNbt());
    }

    public record WaveOptionView(
            Component title,
            Component details,
            int inDungeonRewardPercent,
            int externalRewardPercent,
            int difficultyRating,
            ItemStack displayStack,
            ItemStack secondaryDisplayStack,
            ItemStack helmetStack,
            ItemStack chestStack,
            ItemStack legsStack,
            ItemStack feetStack,
            ItemStack ammoStack,
            int ammoCount,
            int speedRating,
            int damageRating,
            int defenceRating,
            int attackSpeedRating,
            List<Integer> pulledCardCounts
    ) {
        public WaveOptionView {
            pulledCardCounts = normalizeCardCounts(pulledCardCounts);
        }
    }
}
