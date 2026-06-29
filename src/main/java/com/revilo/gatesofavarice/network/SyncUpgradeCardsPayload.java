package com.revilo.gatesofavarice.network;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCard;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCardType;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCategory;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record SyncUpgradeCardsPayload(String sessionId, String categoryName, ItemStack previewStack, int rerollsLeft, int rerollCost, int selectedCardCount, int maxCardSelections, int runeSlotsUsed, int runeSlotsCapacity, List<UpgradeCard> cards) implements CustomPacketPayload {
    public static final Type<SyncUpgradeCardsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "sync_upgrade_cards"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncUpgradeCardsPayload> STREAM_CODEC =
            StreamCodec.of(SyncUpgradeCardsPayload::write, SyncUpgradeCardsPayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, SyncUpgradeCardsPayload payload) {
        buffer.writeUtf(payload.sessionId);
        buffer.writeUtf(payload.categoryName);
        buffer.writeNbt(payload.previewStack.saveOptional(buffer.registryAccess()));
        buffer.writeVarInt(payload.rerollsLeft);
        buffer.writeVarInt(payload.rerollCost);
        buffer.writeVarInt(payload.selectedCardCount);
        buffer.writeVarInt(payload.maxCardSelections);
        buffer.writeVarInt(payload.runeSlotsUsed);
        buffer.writeVarInt(payload.runeSlotsCapacity);
        buffer.writeVarInt(payload.cards.size());
        for (UpgradeCard card : payload.cards) {
            buffer.writeUtf(card.id());
            buffer.writeUtf(card.type().name());
            buffer.writeUtf(card.category().name());
            buffer.writeUtf(card.title());
            buffer.writeUtf(card.targetLabel());
            buffer.writeUtf(card.changeLabel());
            buffer.writeUtf(card.currentValue());
            buffer.writeUtf(card.newValue());
            buffer.writeVarInt(card.tier());
            buffer.writeVarInt(card.cost());
        }
    }

    private static SyncUpgradeCardsPayload read(RegistryFriendlyByteBuf buffer) {
        String sessionId = buffer.readUtf();
        String category = buffer.readUtf();
        ItemStack stack = ItemStack.parseOptional(buffer.registryAccess(), buffer.readNbt());
        int rerollsLeft = buffer.readVarInt();
        int rerollCost = buffer.readVarInt();
        int selectedCardCount = buffer.readVarInt();
        int maxCardSelections = buffer.readVarInt();
        int runeSlotsUsed = buffer.readVarInt();
        int runeSlotsCapacity = buffer.readVarInt();
        int size = buffer.readVarInt();
        ArrayList<UpgradeCard> cards = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            cards.add(new UpgradeCard(
                    buffer.readUtf(),
                    UpgradeCardType.valueOf(buffer.readUtf()),
                    UpgradeCategory.valueOf(buffer.readUtf()),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readVarInt(),
                    buffer.readVarInt()
            ));
        }
        return new SyncUpgradeCardsPayload(sessionId, category, stack, rerollsLeft, rerollCost, selectedCardCount, maxCardSelections, runeSlotsUsed, runeSlotsCapacity, List.copyOf(cards));
    }

    @Override
    public Type<SyncUpgradeCardsPayload> type() {
        return TYPE;
    }
}
