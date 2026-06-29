package com.revilo.gatesofavarice.registry;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.item.DungeonArmorItem;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LoadoutArmorRegistry {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GatewayExpansion.MOD_ID);
    private static final Map<String, DeferredHolder<Item, DungeonArmorItem>> BY_KEY = new HashMap<>();

    private LoadoutArmorRegistry() {
    }

    static {
        registerPiece(ArmorItem.Type.HELMET, "helmet");
        registerPiece(ArmorItem.Type.CHESTPLATE, "chestplate");
        registerPiece(ArmorItem.Type.LEGGINGS, "leggings");
        registerPiece(ArmorItem.Type.BOOTS, "boots");
    }

    private static void registerPiece(ArmorItem.Type type, String suffix) {
        DeferredHolder<Item, DungeonArmorItem> holder = ITEMS.register(
                "dungeon_" + suffix,
                () -> new DungeonArmorItem(type, new Item.Properties().stacksTo(1))
        );
        BY_KEY.put(suffix, holder);
    }

    public static Item get(String setId, ArmorItem.Type type) {
        String suffix = switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            case BOOTS -> "boots";
            default -> "body";
        };
        DeferredHolder<Item, DungeonArmorItem> holder = BY_KEY.get(suffix);
        return holder == null ? net.minecraft.world.item.Items.AIR : holder.get();
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}

