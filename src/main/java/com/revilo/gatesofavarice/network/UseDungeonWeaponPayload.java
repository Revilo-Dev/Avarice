package com.revilo.gatesofavarice.network;

import com.revilo.gatesofavarice.GatewayExpansion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UseDungeonWeaponPayload(boolean secondary, int targetEntityId) implements CustomPacketPayload {
    public static final Type<UseDungeonWeaponPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(GatewayExpansion.MOD_ID, "use_dungeon_weapon"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UseDungeonWeaponPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, UseDungeonWeaponPayload::secondary,
            ByteBufCodecs.VAR_INT, UseDungeonWeaponPayload::targetEntityId,
            UseDungeonWeaponPayload::new);
    @Override public Type<UseDungeonWeaponPayload> type() { return TYPE; }
}
