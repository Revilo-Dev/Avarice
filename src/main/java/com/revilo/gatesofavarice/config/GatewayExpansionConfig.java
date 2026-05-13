package com.revilo.gatesofavarice.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class GatewayExpansionConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ANNOUNCE_GATE_OPEN_IN_CHAT;
    public static final ModConfigSpec.BooleanValue ANNOUNCE_GATE_PARTY_JOIN_IN_CHAT;
    public static final ModConfigSpec.DoubleValue LOADOUT_STAT_ROLL_MULTIPLIER;
    public static final ModConfigSpec.IntValue LOADOUT_EFFECT_LEVEL_CAP;
    public static final ModConfigSpec.BooleanValue FORCE_BINDING_ON_LOADOUT_ARMOR;
    public static final ModConfigSpec.BooleanValue LOADOUT_ITEM_UPGRADES_ENABLED;
    public static final ModConfigSpec.IntValue LOADOUT_UPGRADE_CARD_COUNT;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> ALLOWED_LOADOUT_EFFECTS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("gateway");
        ANNOUNCE_GATE_OPEN_IN_CHAT = builder.comment("Broadcast a chat message when a player opens a generated gate.")
                .define("announceGateOpenInChat", true);
        ANNOUNCE_GATE_PARTY_JOIN_IN_CHAT = builder.comment("Broadcast a chat message when another player joins an active generated gate.")
                .define("announceGatePartyJoinInChat", true);
        builder.pop();
        builder.push("loadout");
        LOADOUT_STAT_ROLL_MULTIPLIER = builder.defineInRange("statRollMultiplier", 1.0D, 0.1D, 10.0D);
        LOADOUT_EFFECT_LEVEL_CAP = builder.defineInRange("effectLevelCap", 3, 1, 10);
        FORCE_BINDING_ON_LOADOUT_ARMOR = builder.define("forceCurseOfBinding", true);
        LOADOUT_ITEM_UPGRADES_ENABLED = builder.define("itemUpgradesEnabled", true);
        LOADOUT_UPGRADE_CARD_COUNT = builder.defineInRange("upgradeCardCount", 5, 1, 10);
        ALLOWED_LOADOUT_EFFECTS = builder.defineListAllowEmpty(
                java.util.List.of("allowedEffects"),
                () -> java.util.List.of(
                        "aether:renewal", "combat_roll:acrobat", "combat_roll:longfooted", "combat_roll:multi_roll",
                        "create:capacity", "create:potato_recovery", "deeperdarker:catalysis", "deeperdarker:discharge",
                        "deeperdarker:sculk_smite", "dungeons_arise:discharge", "dungeons_arise:ensnaring", "dungeons_arise:lolths_curse",
                        "dungeons_arise:purification", "dungeons_arise:voltaic_shot", "expanded_combat:blocking", "expanded_combat:ground_slam",
                        "farmersdelight:backstabbing", "mysticalagriculture:mystical_enlightenment", "mysticalagriculture:soul_siphoner",
                        "simplyswords:catalysis", "simplyswords:fire_react", "simplyswords:soul_siphoner", "supplementaries:stasis",
                        "twilightforest:chill_aura", "twilightforest:destruction", "twilightforest:fire_react",
                        "minecraft:aqua_affinity", "minecraft:depth_strider", "minecraft:feather_falling", "minecraft:binding_curse",
                        "minecraft:channeling", "minecraft:density", "minecraft:flame", "minecraft:impaling",
                        "minecraft:infinity", "minecraft:looting", "minecraft:luck_of_the_sea", "minecraft:multishot",
                        "minecraft:respiration", "minecraft:riptide", "minecraft:fortune", "minecraft:frost_walker",
                        "minecraft:loyalty", "minecraft:lure", "minecraft:mending", "minecraft:piercing", "minecraft:punch",
                        "minecraft:silk_touch", "minecraft:soul_speed", "minecraft:swift_sneak", "minecraft:thorns",
                        "minecraft:vanishing_curse", "minecraft:wind_burst"
                ),
                o -> o instanceof String s && !s.isBlank()
        );
        builder.pop();
        SPEC = builder.build();
    }

    private GatewayExpansionConfig() {
    }
}
