package com.revilo.gatesofavarice.registry;

import com.mojang.serialization.Codec;
import com.revilo.gatesofavarice.GatewayExpansion;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {

    private static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_ITEM_STACK_STREAM_CODEC = StreamCodec.of(
            (buffer, stack) -> ByteBufCodecs.optional(ItemStack.STREAM_CODEC).encode(buffer,
                    stack.isEmpty() ? Optional.empty() : Optional.of(stack)),
            buffer -> ByteBufCodecs.optional(ItemStack.STREAM_CODEC).decode(buffer).orElse(ItemStack.EMPTY));

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, GatewayExpansion.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> MYTHIC_COINS =
            ATTACHMENTS.register("mythic_coins", () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .copyOnDeath()
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> GOLD_COINS =
            ATTACHMENTS.register("gold_coins", () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .copyOnDeath()
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> TOTAL_GOLD_COINS_EARNED =
            ATTACHMENTS.register("total_gold_coins_earned", () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .copyOnDeath()
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> DUNGEON_TOKENS =
            ATTACHMENTS.register("dungeon_tokens", () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .copyOnDeath()
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> COIN_MULTIPLIER =
            ATTACHMENTS.register("coin_multiplier", () -> AttachmentType.builder(() -> 1.0F)
                    .serialize(Codec.FLOAT)
                    .copyOnDeath()
                    .sync(ByteBufCodecs.FLOAT)
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> MAGNET_ENABLED =
            ATTACHMENTS.register("magnet_enabled", () -> AttachmentType.builder(() -> true)
                    .serialize(Codec.BOOL)
                    .copyOnDeath()
                    .sync(ByteBufCodecs.BOOL)
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> BEST_WAVE =
            ATTACHMENTS.register("best_wave", () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .copyOnDeath()
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> PRIMARY_DUNGEON_WEAPON =
            ATTACHMENTS.register("primary_dungeon_weapon", () -> AttachmentType.builder(() -> ItemStack.EMPTY)
                    .serialize(ItemStack.CODEC)
                    .copyOnDeath()
                    .sync(OPTIONAL_ITEM_STACK_STREAM_CODEC)
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> SECONDARY_DUNGEON_WEAPON =
            ATTACHMENTS.register("secondary_dungeon_weapon", () -> AttachmentType.builder(() -> ItemStack.EMPTY)
                    .serialize(ItemStack.CODEC)
                    .copyOnDeath()
                    .sync(OPTIONAL_ITEM_STACK_STREAM_CODEC)
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> ARCHIVE_LOADED =
            ATTACHMENTS.register("archive_loaded", () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
