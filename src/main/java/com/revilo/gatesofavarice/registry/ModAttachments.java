package com.revilo.gatesofavarice.registry;

import com.mojang.serialization.Codec;
import com.revilo.gatesofavarice.GatewayExpansion;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {

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

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
