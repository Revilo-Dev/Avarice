package com.revilo.gatesofavarice.menu;

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
    private final List<Component> runChanges;

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
                readRunChanges(data)
        );
    }

    public DungeonWaveMenu(int containerId, Inventory inventory, UUID ownerId, int waveNumber, boolean ownerCanSelect, int stage, int rerollsLeft, int rerollCost, List<WaveOptionView> options, List<Component> runChanges) {
        super(ModMenus.DUNGEON_WAVE.get(), containerId);
        this.ownerId = ownerId;
        this.waveNumber = waveNumber;
        this.ownerCanSelect = ownerCanSelect;
        this.stage = stage;
        this.rerollsLeft = rerollsLeft;
        this.rerollCost = rerollCost;
        this.options = List.copyOf(options);
        this.runChanges = List.copyOf(runChanges);
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

    public List<Component> runChanges() {
        return this.runChanges;
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
                    data.readInt(),
                    data.readInt(),
                    data.readInt(),
                    data.readInt()
            ));
        }
        return options;
    }

    public static void writePayload(RegistryFriendlyByteBuf buffer, UUID ownerId, int waveNumber, boolean ownerCanSelect, int stage, int rerollsLeft, int rerollCost, List<WaveOptionView> options, List<Component> runChanges) {
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
            buffer.writeInt(option.speedRating());
            buffer.writeInt(option.damageRating());
            buffer.writeInt(option.defenceRating());
            buffer.writeInt(option.attackSpeedRating());
        }
        buffer.writeInt(runChanges.size());
        for (Component change : runChanges) {
            buffer.writeUtf(change.getString());
        }
    }

    private static List<Component> readRunChanges(RegistryFriendlyByteBuf data) {
        int size = data.readInt();
        ArrayList<Component> changes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            changes.add(Component.literal(data.readUtf()));
        }
        return changes;
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
            int speedRating,
            int damageRating,
            int defenceRating,
            int attackSpeedRating
    ) {
    }
}
