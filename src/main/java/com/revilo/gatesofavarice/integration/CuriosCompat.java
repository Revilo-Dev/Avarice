package com.revilo.gatesofavarice.integration;

import com.revilo.gatesofavarice.item.MagnetItem;
import com.revilo.gatesofavarice.item.RunicItemSupport;
import com.revilo.gatesofavarice.registry.ModItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class CuriosCompat {

    private CuriosCompat() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CuriosCompat::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> magnetItems().forEach(item -> CuriosApi.registerCurio(item, new MagnetCurio())));
    }

    private static List<Item> magnetItems() {
        return List.of(
                ModItems.MANA_STEEL_MAGNET.get(),
                ModItems.ELIXRITE_MAGNET.get(),
                ModItems.ASTRITE_MAGNET.get(),
                ModItems.LUNARIUM_MAGNET.get(),
                ModItems.IGNITE_MAGNET.get(),
                ModItems.IRIDIUM_MAGNET.get(),
                ModItems.MYTHRIL_MAGNET.get(),
                ModItems.ARCANIUM_MAGNET.get(),
                ModItems.PRISMATIC_STEEL_MAGNET.get(),
                ModItems.DUNGEON_MAGNET.get());
    }

    public static boolean equipBeltMagnet(ServerPlayer player, ItemStack stack) {
        return CuriosApi.getCuriosInventory(player).map(handler -> {
            Optional<ItemStack> current = handler.findCurio("belt", 0).map(result -> result.stack());
            if (current.isPresent() && !current.get().isEmpty() && !current.get().is(ModItems.DUNGEON_MAGNET.get())) {
                return false;
            }
            handler.setEquippedCurio("belt", 0, stack);
            return true;
        }).orElse(false);
    }

    public static ItemStack findBeltMagnet(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findCurio("belt", 0))
                .map(result -> result.stack())
                .filter(stack -> stack.getItem() instanceof MagnetItem)
                .orElse(ItemStack.EMPTY);
    }

    public static void clearDungeonBeltMagnet(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.findCurio("belt", 0)
                .filter(result -> result.stack().is(ModItems.DUNGEON_MAGNET.get()))
                .ifPresent(result -> handler.setEquippedCurio("belt", 0, ItemStack.EMPTY)));
    }

    private static final class MagnetCurio implements ICurioItem {

        @Override
        public void curioTick(SlotContext slotContext, ItemStack stack) {
            if (!(stack.getItem() instanceof MagnetItem magnet) || !isBeltSlot(slotContext)) {
                return;
            }
            RunicItemSupport.ensureRunicData(stack, magnet.runeSlots());
            MagnetHandler.pullNearbyItems(slotContext.entity(), stack, magnet);
        }

        private boolean isBeltSlot(SlotContext slotContext) {
            return "belt".equals(slotContext.identifier());
        }
    }
}
