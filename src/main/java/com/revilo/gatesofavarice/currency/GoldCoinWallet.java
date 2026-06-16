package com.revilo.gatesofavarice.currency;

import com.revilo.gatesofavarice.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentSync;

public final class GoldCoinWallet {

    private GoldCoinWallet() {
    }

    public static int get(Player player) {
        return player.getData(ModAttachments.GOLD_COINS);
    }

    public static void add(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        player.setData(ModAttachments.GOLD_COINS, get(player) + amount);
        AttachmentSync.syncEntityUpdate(player, ModAttachments.GOLD_COINS.get());
    }

    public static void set(ServerPlayer player, int amount) {
        player.setData(ModAttachments.GOLD_COINS, Math.max(0, amount));
        AttachmentSync.syncEntityUpdate(player, ModAttachments.GOLD_COINS.get());
    }

    public static boolean spend(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return true;
        }
        int current = get(player);
        if (current < amount) {
            return false;
        }
        set(player, current - amount);
        return true;
    }
}
