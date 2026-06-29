package com.revilo.gatesofavarice.client;

import com.revilo.gatesofavarice.dungeon.loadout.LoadoutModels.UpgradeCard;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public final class DungeonUpgradeClientState {
    public static String sessionId = "";
    public static String loadoutName = "";
    public static String theme = "";
    public static boolean categorySelection = true;
    public static String categoryName = "";
    public static ItemStack previewStack = ItemStack.EMPTY;
    public static List<UpgradeCard> cards = List.of();
    public static int rerollsLeft = 0;
    public static int rerollCost = 0;
    public static int selectedCardCount = 0;
    public static int maxCardSelections = 5;
    public static int runeSlotsUsed = 0;
    public static int runeSlotsCapacity = 0;

    private DungeonUpgradeClientState() {}
}
