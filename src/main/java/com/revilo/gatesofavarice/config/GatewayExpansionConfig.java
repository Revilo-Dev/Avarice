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
    public static final ModConfigSpec.IntValue NEXT_WAVE_DURATION_SECONDS;
    public static final ModConfigSpec.IntValue INITIAL_WAVE_MOB_PERCENT;
    public static final ModConfigSpec.IntValue INITIAL_WAVE_MOB_MAX;
    public static final ModConfigSpec.IntValue MAX_ACTIVE_WAVE_MOBS;
    public static final ModConfigSpec.IntValue LOOT_CRATES_PER_WAVE;
    public static final ModConfigSpec.DoubleValue RUNE_LOOT_CRATE_DROP_CHANCE;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> KNOWLEDGE_ENTRIES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("gateway");
        ANNOUNCE_GATE_OPEN_IN_CHAT = builder.comment("Broadcast a chat message when a player opens a generated gate.")
                .define("announceGateOpenInChat", true);
        ANNOUNCE_GATE_PARTY_JOIN_IN_CHAT = builder.comment("Broadcast a chat message when another player joins an active generated gate.")
                .define("announceGatePartyJoinInChat", true);
        builder.pop();
        builder.push("dungeon");
        NEXT_WAVE_DURATION_SECONDS = builder.comment("Default delay between waves, in seconds.")
                .defineInRange("nextWaveDurationSeconds", 10, 0, 300);
        INITIAL_WAVE_MOB_PERCENT = builder.comment("Percentage of a wave spawned immediately.")
                .defineInRange("initialWaveMobPercent", 50, 1, 100);
        INITIAL_WAVE_MOB_MAX = builder.comment("Maximum mobs spawned immediately at the start of a wave.")
                .defineInRange("initialWaveMobMax", 150, 1, 500);
        MAX_ACTIVE_WAVE_MOBS = builder.comment("Maximum concurrently alive dungeon mobs for one wave.")
                .defineInRange("maxActiveWaveMobs", 250, 1, 1000);
        LOOT_CRATES_PER_WAVE = builder.comment("Extra dungeon loot crates created after each completed wave.")
                .defineInRange("lootCratesPerWave", 3, 0, 32);
        RUNE_LOOT_CRATE_DROP_CHANCE = builder.comment("Chance for an otherwise rolled Runic item to remain in a loot crate.")
                .defineInRange("runeLootCrateDropChance", 0.10D, 0.0D, 1.0D);
        builder.pop();
        builder.push("knowledge");
        KNOWLEDGE_ENTRIES = builder.comment("Knowledge entries: id|rarity|title|description|unlocked-item namespace. The first matching entry unlocks a gated namespace.")
                .defineListAllowEmpty("entries", () -> java.util.List.of(
                        "create_foundations|common|Knowledge of Kinetics|The Create mod's basic machinery and components.|create",
                        "dark_utilities_foundations|common|Knowledge of Dark Utilities|The Dark Utilities mod's blocks and tools.|darkutils",
                        "create_mixing|uncommon|Knowledge of Mixing|Mechanical mixers and their craft.|",
                        "create_trains|legendary|Knowledge of Trains|Railways, stations, and schedules.|",
                        "create_blaze_brewing|rare|Knowledge of Blaze Brewing|Heated mixing and blaze burners.|",
                        "create_crushing|common|Knowledge of Crushing|Crushing wheels and ore processing.|",
                        "create_belts|common|Knowledge of Belts|Belts, funnels, and item movement.|",
                        "create_filters|uncommon|Knowledge of Filters|Precise item filtering and routing.|",
                        "create_logistics|rare|Knowledge of Logistics|Stock links, requests, and vaults.|",
                        "create_fluids|uncommon|Knowledge of Fluids|Pipes, pumps, and fluid handling.|",
                        "create_contraptions|epic|Knowledge of Contraptions|Moving machines and mechanical bearings.|",
                        "create_deployers|rare|Knowledge of Deployment|Deployers that work with a mechanical hand.|",
                        "create_encased_fans|uncommon|Knowledge of Airflow|Encased fans and bulk processing.|",
                        "darkutils_plates|common|Knowledge of Movement Plates|Movement, speed, and launch plates.|",
                        "darkutils_filters|uncommon|Knowledge of Dark Filters|Advanced filtering and item control.|",
                        "darkutils_charms|rare|Knowledge of Charms|Charms and personal utility.|",
                        "darkutils_angel|epic|Knowledge of the Angel Block|Building where no platform exists.|",
                        "darkutils_traps|uncommon|Knowledge of Traps|Dark traps and mob control.|",
                        "darkutils_wither|legendary|Knowledge of Withering|Wither tools and dangerous utilities.|",
                        "darkutils_portals|epic|Knowledge of Dark Passage|Teleportation and dimensional utility.|"
                ), value -> value instanceof String text && text.split("\\|", -1).length >= 5);
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
