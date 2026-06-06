package com.revilo.gatesofavarice.network;

import com.revilo.gatesofavarice.GatewayExpansion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record DungeonCompletePayload(
        boolean survived,
        int wavesComplete,
        int bestWave,
        long timeSpentTicks,
        int levelPointsEarned,
        int coinsEarned,
        int mobsKilled,
        int damageDealt,
        int damageReceived,
        int experienceEarned,
        int rarityLevel,
        int quantityLevel,
        int mobHealth,
        int mobDamage,
        List<ItemStack> rewards
) implements CustomPacketPayload {

    public static final Type<DungeonCompletePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "dungeon_complete"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DungeonCompletePayload> STREAM_CODEC =
            StreamCodec.of(DungeonCompletePayload::write, DungeonCompletePayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, DungeonCompletePayload payload) {
        buffer.writeBoolean(payload.survived);
        buffer.writeVarInt(payload.wavesComplete);
        buffer.writeVarInt(payload.bestWave);
        buffer.writeVarLong(payload.timeSpentTicks);
        buffer.writeVarInt(payload.levelPointsEarned);
        buffer.writeVarInt(payload.coinsEarned);
        buffer.writeVarInt(payload.mobsKilled);
        buffer.writeVarInt(payload.damageDealt);
        buffer.writeVarInt(payload.damageReceived);
        buffer.writeVarInt(payload.experienceEarned);
        buffer.writeVarInt(payload.rarityLevel);
        buffer.writeVarInt(payload.quantityLevel);
        buffer.writeVarInt(payload.mobHealth);
        buffer.writeVarInt(payload.mobDamage);
        buffer.writeVarInt(payload.rewards.size());
        for (ItemStack reward : payload.rewards) {
            writeStack(buffer, reward);
        }
    }

    private static DungeonCompletePayload read(RegistryFriendlyByteBuf buffer) {
        boolean survived = buffer.readBoolean();
        int wavesComplete = buffer.readVarInt();
        int bestWave = buffer.readVarInt();
        long timeSpentTicks = buffer.readVarLong();
        int levelPointsEarned = buffer.readVarInt();
        int coinsEarned = buffer.readVarInt();
        int mobsKilled = buffer.readVarInt();
        int damageDealt = buffer.readVarInt();
        int damageReceived = buffer.readVarInt();
        int experienceEarned = buffer.readVarInt();
        int rarityLevel = buffer.readVarInt();
        int quantityLevel = buffer.readVarInt();
        int mobHealth = buffer.readVarInt();
        int mobDamage = buffer.readVarInt();
        int rewardCount = buffer.readVarInt();
        ArrayList<ItemStack> rewards = new ArrayList<>(rewardCount);
        for (int i = 0; i < rewardCount; i++) {
            rewards.add(readStack(buffer));
        }
        return new DungeonCompletePayload(survived, wavesComplete, bestWave, timeSpentTicks, levelPointsEarned, coinsEarned, mobsKilled, damageDealt,
                damageReceived, experienceEarned, rarityLevel, quantityLevel, mobHealth, mobDamage, List.copyOf(rewards));
    }

    private static void writeStack(RegistryFriendlyByteBuf buffer, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            buffer.writeResourceLocation(ResourceLocation.withDefaultNamespace("air"));
            buffer.writeVarInt(0);
            return;
        }
        buffer.writeResourceLocation(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        buffer.writeVarInt(stack.getCount());
    }

    private static ItemStack readStack(RegistryFriendlyByteBuf buffer) {
        ResourceLocation id = buffer.readResourceLocation();
        int count = buffer.readVarInt();
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR || count <= 0) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }

    @Override
    public Type<DungeonCompletePayload> type() {
        return TYPE;
    }
}
