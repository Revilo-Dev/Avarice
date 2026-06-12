package com.revilo.gatesofavarice.network;

import com.revilo.gatesofavarice.registry.ModAttachments;
import com.revilo.gatesofavarice.dungeon.DungeonUpgradeManager;
import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCategory;
import com.revilo.gatesofavarice.dungeon.DungeonHudState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class GatewayExpansionNetwork {

    private GatewayExpansionNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GatewayExpansionNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ToggleMagnetPayload.TYPE, ToggleMagnetPayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            boolean enabled = !player.getData(ModAttachments.MAGNET_ENABLED);
            player.setData(ModAttachments.MAGNET_ENABLED, enabled);
            player.displayClientMessage(
                    Component.translatable(enabled
                                    ? "message.gatesofavarice.magnet_enabled"
                                    : "message.gatesofavarice.magnet_disabled")
                            .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED),
                    true);
        });
        registrar.playToClient(DungeonWaveHudPayload.TYPE, DungeonWaveHudPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> DungeonHudState.apply(payload)));
        registrar.playToClient(DungeonCompletePayload.TYPE, DungeonCompletePayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (!FMLEnvironment.dist.isClient()) {
                        return;
                    }
                    try {
                        Class<?> handler = Class.forName("com.revilo.gatesofavarice.client.DungeonCompleteClientHandler");
                        handler.getMethod("open", DungeonCompletePayload.class).invoke(null, payload);
                    } catch (ReflectiveOperationException ignored) {
                    }
                }));
        registrar.playToServer(SelectUpgradeCategoryPayload.TYPE, SelectUpgradeCategoryPayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            UpgradeCategory[] values = UpgradeCategory.values();
            if (payload.categoryOrdinal() < 0 || payload.categoryOrdinal() >= values.length) return;
            DungeonUpgradeManager.selectCategory(player, payload.sessionId(), values[payload.categoryOrdinal()]);
        });
        registrar.playToServer(SelectUpgradeCardPayload.TYPE, SelectUpgradeCardPayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            DungeonUpgradeManager.selectCard(player, payload.sessionId(), payload.cardId());
        });
        registrar.playToServer(RerollUpgradeCardsPayload.TYPE, RerollUpgradeCardsPayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            DungeonUpgradeManager.rerollCards(player, payload.sessionId());
        });
        registrar.playToClient(OpenUpgradeCategoryPayload.TYPE, OpenUpgradeCategoryPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (!FMLEnvironment.dist.isClient()) return;
                    try {
                        Class<?> state = Class.forName("com.revilo.gatesofavarice.client.DungeonUpgradeClientState");
                        state.getField("sessionId").set(null, payload.sessionId());
                        state.getField("loadoutName").set(null, payload.loadoutName());
                        state.getField("theme").set(null, payload.theme());
                        state.getField("categorySelection").setBoolean(null, true);
                        state.getField("categoryName").set(null, "");
                        state.getField("cards").set(null, java.util.List.of());
                        Class<?> mc = Class.forName("net.minecraft.client.Minecraft");
                        Object instance = mc.getMethod("getInstance").invoke(null);
                        Object currentScreen = mc.getField("screen").get(instance);
                        Class<?> shopScreenClass = Class.forName("com.revilo.gatesofavarice.client.screen.ShopkeeperScreen");
                        if (currentScreen != null && shopScreenClass.isInstance(currentScreen)) {
                            shopScreenClass.getMethod("applyUpgradeCategoryState").invoke(currentScreen);
                            return;
                        }
                        Class<?> screenClass = Class.forName("com.revilo.gatesofavarice.client.screen.DungeonUpgradeCategoryScreen");
                        Object screen = screenClass.getConstructor().newInstance();
                        mc.getMethod("setScreen", Class.forName("net.minecraft.client.gui.screens.Screen")).invoke(instance, screen);
                    } catch (ReflectiveOperationException ignored) {
                    }
                }));
        registrar.playToClient(SyncUpgradeCardsPayload.TYPE, SyncUpgradeCardsPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (!FMLEnvironment.dist.isClient()) return;
                    try {
                        Class<?> state = Class.forName("com.revilo.gatesofavarice.client.DungeonUpgradeClientState");
                        state.getField("sessionId").set(null, payload.sessionId());
                        state.getField("categorySelection").setBoolean(null, false);
                        state.getField("categoryName").set(null, payload.categoryName());
                        state.getField("previewStack").set(null, payload.previewStack());
                        state.getField("rerollsLeft").setInt(null, payload.rerollsLeft());
                        state.getField("rerollCost").setInt(null, payload.rerollCost());
                        state.getField("cards").set(null, payload.cards());
                        Class<?> mc = Class.forName("net.minecraft.client.Minecraft");
                        Object instance = mc.getMethod("getInstance").invoke(null);
                        Object currentScreen = mc.getField("screen").get(instance);
                        Class<?> shopScreenClass = Class.forName("com.revilo.gatesofavarice.client.screen.ShopkeeperScreen");
                        if (currentScreen != null && shopScreenClass.isInstance(currentScreen)) {
                            shopScreenClass.getMethod("applyUpgradeCardsState", SyncUpgradeCardsPayload.class).invoke(currentScreen, payload);
                            return;
                        }
                        Class<?> screenClass = Class.forName("com.revilo.gatesofavarice.client.screen.DungeonUpgradeCardsScreen");
                        if (currentScreen != null && screenClass.isInstance(currentScreen)) {
                            screenClass.getMethod("applySyncPayload", SyncUpgradeCardsPayload.class).invoke(currentScreen, payload);
                        } else {
                            Object screen = screenClass.getConstructor().newInstance();
                            mc.getMethod("setScreen", Class.forName("net.minecraft.client.gui.screens.Screen")).invoke(instance, screen);
                        }
                    } catch (ReflectiveOperationException ignored) {
                    }
                }));
    }
}
