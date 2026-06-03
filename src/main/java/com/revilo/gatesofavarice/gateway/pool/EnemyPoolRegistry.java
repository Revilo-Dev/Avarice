package com.revilo.gatesofavarice.gateway.pool;

import com.revilo.gatesofavarice.GatewayExpansion;
import com.revilo.gatesofavarice.integration.ModCompat;
import java.util.HashSet;
import com.revilo.gatesofavarice.item.data.CrystalTheme;
import java.util.List;
import java.util.Set;
import net.minecraft.world.entity.EntityType;

public final class EnemyPoolRegistry {
    private static final List<CrystalTheme> WILD_THEME_BLEND = List.of(
            CrystalTheme.UNDEAD,
            CrystalTheme.RAIDER,
            CrystalTheme.NETHER,
            CrystalTheme.ARCANE,
            CrystalTheme.BEAST
    );

    private static final String[] BOSS_MOD_IDS = {"bosses_of_mass_destruction", "bossesrise", "bosses_rise"};
    private static final String[] ALEXS_MOBS_IDS = {"alexsmobs"};
    private static final String[] DEEPER_DARKER_IDS = {"deeperdarker"};
    private static final String[] ENDERMAN_OVERHAUL_IDS = {"endermanoverhaul"};
    private static final String[] LUMINOUS_IDS = {"luminous_monsters"};
    private static final String[] FRIENDS_AND_FOES_IDS = {"friendsandfoes"};
    private static final String[] TAKES_A_PILLAGE_IDS = {"takesapillage"};
    private static final String[] VARIANTS_IDS = {"variantsandventures", "variants_and_ventures"};
    private static final String[] VILLAGERS_IDS = {"villagersandpillagers"};
    private static final String[] UNDEAD_VARIANTS_IDS = {
            "variantsandventures:gelid",
            "variantsandventures:murk",
            "variantsandventures:thicket",
            "variantsandventures:verdant"
    };
    private static final String[] ARCANE_ENDERMAN_OVERHAUL_IDS = {
            "endermanoverhaul:badlands_endman",
            "endermanoverhaul:badlands_enderman",
            "endermanoverhaul:cave_enderman",
            "endermanoverhaul:crimson_forest_enderman",
            "endermanoverhaul:dark_oak_enderman",
            "endermanoverhaul:desert",
            "endermanoverhaul:desert_enderman",
            "endermanoverhaul:ender_enderman",
            "endermanoverhaul:end_enderman",
            "endermanoverhaul:end_islands_enderman",
            "endermanoverhaul:flower_fields_enderman",
            "endermanoverhaul:ice_spikes_enderman",
            "endermanoverhaul:mushroom_fields_underman",
            "endermanoverhaul:mushroom_fields_enderman",
            "endermanoverhaul:nether_wastes_enderman",
            "endermanoverhaul:coral_enderman",
            "endermanoverhaul:savanna_enderman",
            "endermanoverhaul:snowy_enderman",
            "endermanoverhaul:soulsand_valley_enderman",
            "endermanoverhaul:swamp_enderman",
            "endermanoverhaul:warped_forest_enderman",
            "endermanoverhaul:windswept_hills_enderman",
            "endermanoverhaul:scarab",
            "endermanoverhaul:spirit"
    };
    private static final String[] RAIDER_TAKES_A_PILLAGE_IDS = {
            "takesapillage:archer",
            "takesapillage:skirmisher",
            "takesapillage:legioner"
    };
    private static final String[] RAIDER_FRIENDS_AND_FOES_IDS = {
            "friendsandfoes:iceologer"
    };
    private static final String[] RAIDER_FRIENDS_AND_FOES_ILLUSIONER_IDS = {
            "friendsandfoes:illusioner"
    };
    private static final String[] NETHER_FRIENDS_AND_FOES_IDS = {
            "friendsandfoes:wildfire"
    };

    private EnemyPoolRegistry() {
    }

    public static EnemyPoolSet create(CrystalTheme theme, int level) {
        EnemyPoolSet pools = new EnemyPoolSet();
        if (theme == CrystalTheme.WILD) {
            for (CrystalTheme mixedTheme : WILD_THEME_BLEND) {
                addVanilla(mixedTheme, level, pools);
                addBossCompat(mixedTheme, level, pools);
                addAlexsMobsCompat(mixedTheme, level, pools);
                addDeeperDarkerCompat(mixedTheme, level, pools);
                addEndermanOverhaulCompat(mixedTheme, level, pools);
                addFriendsAndFoesCompat(mixedTheme, level, pools);
                addLuminousCompat(mixedTheme, level, pools);
                addTakesAPillageCompat(mixedTheme, level, pools);
                addVariantsCompat(mixedTheme, level, pools);
                addVillagersCompat(mixedTheme, level, pools);
            }
            normalizePools(pools);
            GatewayExpansion.LOGGER.debug("Enemy pools for {} -> hoard={}, archer={}, assassin={}, tank={}",
                    theme, pools.pool(EnemyPoolRole.HOARD).size(), pools.pool(EnemyPoolRole.ARCHER).size(), pools.pool(EnemyPoolRole.ASSASSIN).size(),
                    pools.pool(EnemyPoolRole.TANK).size());
            return pools;
        }

        addVanilla(theme, level, pools);
        addBossCompat(theme, level, pools);
        addAlexsMobsCompat(theme, level, pools);
        addDeeperDarkerCompat(theme, level, pools);
        addEndermanOverhaulCompat(theme, level, pools);
        addFriendsAndFoesCompat(theme, level, pools);
        addLuminousCompat(theme, level, pools);
        addTakesAPillageCompat(theme, level, pools);
        addVariantsCompat(theme, level, pools);
        addVillagersCompat(theme, level, pools);
        normalizePools(pools);
        GatewayExpansion.LOGGER.debug("Enemy pools for {} -> hoard={}, archer={}, assassin={}, tank={}",
                theme, pools.pool(EnemyPoolRole.HOARD).size(), pools.pool(EnemyPoolRole.ARCHER).size(), pools.pool(EnemyPoolRole.ASSASSIN).size(),
                pools.pool(EnemyPoolRole.TANK).size());
        return pools;
    }

    private static void normalizePools(EnemyPoolSet pools) {
        Set<EntityType<?>> banned = Set.of(
                EntityType.ENDERMITE,
                EntityType.HOGLIN,
                EntityType.PHANTOM,
                EntityType.BEE,
                EntityType.BREEZE,
                EntityType.ZOGLIN,
                EntityType.RAVAGER
        );

        for (EnemyPoolRole role : List.of(EnemyPoolRole.ASSASSIN, EnemyPoolRole.HOARD, EnemyPoolRole.ARCHER, EnemyPoolRole.TANK)) {
            pools.pool(role).removeIf(entry -> banned.contains(entry.type()));
        }

        // One category per mob: priority = tank, archer, assassin, hoard.
        Set<EntityType<?>> claimed = new HashSet<>();
        for (EnemyPoolRole role : List.of(EnemyPoolRole.TANK, EnemyPoolRole.ARCHER, EnemyPoolRole.ASSASSIN, EnemyPoolRole.HOARD)) {
            pools.pool(role).removeIf(entry -> {
                if (claimed.contains(entry.type())) {
                    return true;
                }
                claimed.add(entry.type());
                return false;
            });
        }
    }

    private static void addVanilla(CrystalTheme theme, int level, EnemyPoolSet pools) {
        switch (theme) {
            case UNDEAD -> {
                pools.pool(EnemyPoolRole.MELEE).add(EntityType.ZOMBIE, 8, "vanilla").add(EntityType.HUSK, 5, "vanilla").add(EntityType.SKELETON, 4, "vanilla");
                pools.pool(EnemyPoolRole.RANGED).add(EntityType.SKELETON, 8, "vanilla").add(EntityType.STRAY, 4, "vanilla").add(EntityType.BOGGED, 4, "vanilla").add(EntityType.DROWNED, 4, "vanilla");
                pools.pool(EnemyPoolRole.TANK).add(EntityType.ZOMBIE, 6, "vanilla").add(EntityType.WITHER_SKELETON, 2, "vanilla");
                pools.pool(EnemyPoolRole.FAST).add(EntityType.HUSK, 5, "vanilla").add(EntityType.SILVERFISH, 3, "vanilla").add(EntityType.ZOMBIE_VILLAGER, 3, "vanilla");
                pools.pool(EnemyPoolRole.ELITE).add(EntityType.WITHER_SKELETON, 5, "vanilla").add(EntityType.WITCH, 4, "vanilla").add(EntityType.STRAY, 3, "vanilla");
                pools.pool(EnemyPoolRole.SUPPORT).add(EntityType.WITCH, 8, "vanilla").add(EntityType.BOGGED, 3, "vanilla").add(EntityType.CAVE_SPIDER, 2, "vanilla");
                pools.pool(EnemyPoolRole.THEME).add(EntityType.ZOMBIE, 7, "vanilla").add(EntityType.SKELETON, 6, "vanilla").add(EntityType.HUSK, 4, "vanilla");
                if (level >= 20) {
                    pools.pool(EnemyPoolRole.MELEE).add(EntityType.ZOMBIE_VILLAGER, 4, "vanilla");
                    pools.pool(EnemyPoolRole.FAST).add(EntityType.DROWNED, 3, "vanilla");
                    pools.pool(EnemyPoolRole.RANGED).add(EntityType.DROWNED, 4, "vanilla");
                    pools.pool(EnemyPoolRole.THEME).add(EntityType.ZOMBIE_VILLAGER, 4, "vanilla");
                }
                if (level >= 35) {
                    pools.pool(EnemyPoolRole.RANGED).add(EntityType.PHANTOM, 3, "vanilla");
                    pools.pool(EnemyPoolRole.SUPPORT).add(EntityType.PHANTOM, 2, "vanilla");
                    pools.pool(EnemyPoolRole.ELITE).add(EntityType.PHANTOM, 2, "vanilla");
                }
                if (level >= 50) {
                    pools.pool(EnemyPoolRole.ELITE).add(EntityType.BREEZE, 2, "vanilla");
                    pools.pool(EnemyPoolRole.TANK).add(EntityType.ZOGLIN, 2, "vanilla");
                }
            }
            case BEAST -> {
                pools.pool(EnemyPoolRole.MELEE).add(EntityType.SPIDER, 8, "vanilla").add(EntityType.SLIME, 4, "vanilla");
                pools.pool(EnemyPoolRole.RANGED).add(EntityType.BEE, 6, "vanilla");
                pools.pool(EnemyPoolRole.TANK).add(EntityType.RAVAGER, 5, "vanilla").add(EntityType.SLIME, 4, "vanilla").add(EntityType.HOGLIN, 2, "vanilla");
                pools.pool(EnemyPoolRole.FAST).add(EntityType.CAVE_SPIDER, 8, "vanilla").add(EntityType.SILVERFISH, 4, "vanilla").add(EntityType.ENDERMITE, 3, "vanilla");
                pools.pool(EnemyPoolRole.ELITE).add(EntityType.RAVAGER, 7, "vanilla").add(EntityType.HOGLIN, 2, "vanilla");
                pools.pool(EnemyPoolRole.SUPPORT).add(EntityType.BEE, 6, "vanilla").add(EntityType.CAVE_SPIDER, 4, "vanilla").add(EntityType.SLIME, 2, "vanilla");
                pools.pool(EnemyPoolRole.THEME).add(EntityType.SPIDER, 8, "vanilla").add(EntityType.CAVE_SPIDER, 5, "vanilla").add(EntityType.SLIME, 3, "vanilla");
                if (level >= 20) {
                    pools.pool(EnemyPoolRole.MELEE).add(EntityType.SLIME, 4, "vanilla");
                    pools.pool(EnemyPoolRole.SUPPORT).add(EntityType.SLIME, 3, "vanilla");
                    pools.pool(EnemyPoolRole.THEME).add(EntityType.SLIME, 3, "vanilla");
                }
                if (level >= 35) {
                    pools.pool(EnemyPoolRole.FAST).add(EntityType.ENDERMITE, 3, "vanilla");
                    pools.pool(EnemyPoolRole.TANK).add(EntityType.SLIME, 4, "vanilla");
                }
                if (level >= 50) {
                    pools.pool(EnemyPoolRole.RANGED).add(EntityType.BREEZE, 3, "vanilla");
                    pools.pool(EnemyPoolRole.ELITE).add(EntityType.BREEZE, 2, "vanilla");
                }
            }
            case ARCANE -> {
                pools.pool(EnemyPoolRole.MELEE).add(EntityType.ENDERMAN, 6, "vanilla").add(EntityType.VEX, 6, "vanilla");
                pools.pool(EnemyPoolRole.RANGED).add(EntityType.PILLAGER, 4, "vanilla").add(EntityType.BLAZE, 7, "vanilla").add(EntityType.WITCH, 5, "vanilla");
                pools.pool(EnemyPoolRole.TANK).add(EntityType.ENDERMAN, 5, "vanilla");
                pools.pool(EnemyPoolRole.FAST).add(EntityType.VEX, 8, "vanilla").add(EntityType.ENDERMITE, 4, "vanilla");
                pools.pool(EnemyPoolRole.ELITE).add(EntityType.ENDERMAN, 8, "vanilla").add(EntityType.WITCH, 4, "vanilla").add(EntityType.EVOKER, 1, "vanilla").add(EntityType.ILLUSIONER, 1, "vanilla");
                pools.pool(EnemyPoolRole.SUPPORT).add(EntityType.WITCH, 8, "vanilla").add(EntityType.BREEZE, 3, "vanilla");
                pools.pool(EnemyPoolRole.THEME).add(EntityType.VEX, 7, "vanilla").add(EntityType.ENDERMAN, 6, "vanilla").add(EntityType.ENDERMITE, 3, "vanilla");
                if (level >= 20) {
                    pools.pool(EnemyPoolRole.RANGED).add(EntityType.BREEZE, 4, "vanilla");
                    pools.pool(EnemyPoolRole.FAST).add(EntityType.BREEZE, 4, "vanilla");
                    pools.pool(EnemyPoolRole.THEME).add(EntityType.BREEZE, 3, "vanilla");
                }
                if (level >= 35) {
                    pools.pool(EnemyPoolRole.SUPPORT).add(EntityType.PHANTOM, 2, "vanilla");
                    pools.pool(EnemyPoolRole.ELITE).add(EntityType.WITCH, 2, "vanilla").add(EntityType.ENDERMAN, 2, "vanilla");
                }
                if (level >= 50) {
                    pools.pool(EnemyPoolRole.MELEE).add(EntityType.SHULKER, 2, "vanilla");
                    pools.pool(EnemyPoolRole.TANK).add(EntityType.SHULKER, 3, "vanilla");
                    pools.pool(EnemyPoolRole.ELITE).add(EntityType.SHULKER, 2, "vanilla").add(EntityType.ILLUSIONER, 1, "vanilla");
                }
            }
            case NETHER -> {
                pools.pool(EnemyPoolRole.MELEE).add(EntityType.HOGLIN, 5, "vanilla").add(EntityType.WITHER_SKELETON, 4, "vanilla").add(EntityType.SKELETON, 3, "vanilla");
                pools.pool(EnemyPoolRole.RANGED).add(EntityType.BLAZE, 7, "vanilla").add(EntityType.SKELETON, 5, "vanilla");
                pools.pool(EnemyPoolRole.TANK).add(EntityType.HOGLIN, 7, "vanilla").add(EntityType.PIGLIN_BRUTE, 5, "vanilla").add(EntityType.MAGMA_CUBE, 3, "vanilla");
                pools.pool(EnemyPoolRole.FAST).add(EntityType.MAGMA_CUBE, 6, "vanilla");
                pools.pool(EnemyPoolRole.ELITE).add(EntityType.PIGLIN_BRUTE, 7, "vanilla").add(EntityType.WITHER_SKELETON, 6, "vanilla").add(EntityType.BLAZE, 3, "vanilla");
                pools.pool(EnemyPoolRole.SUPPORT).add(EntityType.BLAZE, 6, "vanilla").add(EntityType.SKELETON, 4, "vanilla");
                pools.pool(EnemyPoolRole.THEME).add(EntityType.HOGLIN, 5, "vanilla").add(EntityType.WITHER_SKELETON, 4, "vanilla").add(EntityType.BLAZE, 4, "vanilla");
                if (level >= 20) {
                    pools.pool(EnemyPoolRole.MELEE).add(EntityType.SKELETON, 2, "vanilla");
                    pools.pool(EnemyPoolRole.SUPPORT).add(EntityType.WITHER_SKELETON, 2, "vanilla");
                }
                if (level >= 35) {
                    pools.pool(EnemyPoolRole.TANK).add(EntityType.PIGLIN_BRUTE, 3, "vanilla");
                    pools.pool(EnemyPoolRole.ELITE).add(EntityType.PIGLIN_BRUTE, 2, "vanilla").add(EntityType.WITHER_SKELETON, 2, "vanilla");
                    pools.pool(EnemyPoolRole.THEME).add(EntityType.SKELETON, 2, "vanilla");
                }
                if (level >= 50) {
                    pools.pool(EnemyPoolRole.MELEE).add(EntityType.ZOGLIN, 2, "vanilla");
                    pools.pool(EnemyPoolRole.TANK).add(EntityType.ZOGLIN, 3, "vanilla").add(EntityType.PIGLIN_BRUTE, 2, "vanilla");
                    pools.pool(EnemyPoolRole.ELITE).add(EntityType.ZOGLIN, 2, "vanilla");
                }
            }
            case RAIDER -> {
                pools.pool(EnemyPoolRole.MELEE).add(EntityType.PILLAGER, 12, "vanilla").add(EntityType.VINDICATOR, 2, "vanilla");
                pools.pool(EnemyPoolRole.RANGED).add(EntityType.PILLAGER, 15, "vanilla");
                pools.pool(EnemyPoolRole.TANK).add(EntityType.RAVAGER, 8, "vanilla");
                pools.pool(EnemyPoolRole.FAST).add(EntityType.PILLAGER, 10, "vanilla").add(EntityType.VINDICATOR, 1, "vanilla");
                pools.pool(EnemyPoolRole.ELITE).add(EntityType.VINDICATOR, 5, "vanilla").add(EntityType.ILLUSIONER, 2, "vanilla");
                pools.pool(EnemyPoolRole.SUPPORT).add(EntityType.PILLAGER, 10, "vanilla").add(EntityType.VINDICATOR, 1, "vanilla");
                pools.pool(EnemyPoolRole.THEME).add(EntityType.PILLAGER, 13, "vanilla").add(EntityType.VINDICATOR, 3, "vanilla");
                if (level >= 20) {
                    pools.pool(EnemyPoolRole.MELEE).add(EntityType.PILLAGER, 4, "vanilla");
                    pools.pool(EnemyPoolRole.THEME).add(EntityType.PILLAGER, 3, "vanilla");
                }
                if (level >= 35) {
                    pools.pool(EnemyPoolRole.ELITE).add(EntityType.VINDICATOR, 2, "vanilla").add(EntityType.PILLAGER, 2, "vanilla");
                    pools.pool(EnemyPoolRole.FAST).add(EntityType.PILLAGER, 2, "vanilla");
                }
                if (level >= 50) {
                    pools.pool(EnemyPoolRole.TANK).add(EntityType.RAVAGER, 4, "vanilla");
                    pools.pool(EnemyPoolRole.ELITE).add(EntityType.ILLUSIONER, 1, "vanilla");
                }
            }
        }
    }

    private static void addBossCompat(CrystalTheme theme, int level, EnemyPoolSet pools) {
        ModCompat.debugDetected("Boss mod", BOSS_MOD_IDS);
        if (level < 35 || !ModCompat.isAnyLoaded(BOSS_MOD_IDS)) {
            return;
        }

        EntityType<?> obsidilith = ModCompat.findFirstEntity("bosses_of_mass_destruction:obsidilith");
        EntityType<?> gauntlet = ModCompat.findFirstEntity("bosses_of_mass_destruction:gauntlet");
        EntityType<?> voidBlossom = ModCompat.findFirstEntity("bosses_of_mass_destruction:void_blossom");
        EntityType<?> lich = ModCompat.findFirstEntity("bosses_of_mass_destruction:lich");
        if (theme == CrystalTheme.NETHER) {
            pools.pool(EnemyPoolRole.BOSS).add(gauntlet, 4, "bosses_of_mass_destruction");
        }
        if (theme == CrystalTheme.ARCANE) {
            pools.pool(EnemyPoolRole.BOSS).add(voidBlossom, 3, "bosses_of_mass_destruction").add(lich, 3, "bosses_of_mass_destruction");
        }
        if (theme == CrystalTheme.UNDEAD || theme == CrystalTheme.ARCANE) {
            pools.pool(EnemyPoolRole.BOSS).add(lich, 4, "bosses_of_mass_destruction");
        }
        pools.pool(EnemyPoolRole.BOSS).add(obsidilith, theme == CrystalTheme.NETHER ? 5 : 2, "bosses_of_mass_destruction");

        for (EntityType<?> type : ModCompat.hostileEntitiesForNamespaces(BOSS_MOD_IDS)) {
            pools.pool(EnemyPoolRole.BOSS).add(type, 1, "boss namespace scan");
        }
    }

    private static void addAlexsMobsCompat(CrystalTheme theme, int level, EnemyPoolSet pools) {
        ModCompat.debugDetected("Alex's Mobs", ALEXS_MOBS_IDS);
        if (!ModCompat.isAnyLoaded(ALEXS_MOBS_IDS)) {
            return;
        }
        switch (theme) {
            case UNDEAD -> {
                if (level >= 35) {
                    pools.pool(EnemyPoolRole.THEME).addAll(ModCompat.findEntitiesMatching(ALEXS_MOBS_IDS, "soul_vulture", "bone_serpent"), 2, "alexsmobs");
                    pools.pool(EnemyPoolRole.ELITE).addAll(ModCompat.findEntitiesMatching(ALEXS_MOBS_IDS, "soul_vulture", "bone_serpent"), 2, "alexsmobs");
                }
            }
            case BEAST -> {
                if (level >= 20) {
                    pools.pool(EnemyPoolRole.MELEE).addAll(ModCompat.findEntitiesMatching(ALEXS_MOBS_IDS, "centipede", "dropbear"), 2, "alexsmobs");
                    pools.pool(EnemyPoolRole.FAST).addAll(ModCompat.findEntitiesMatching(ALEXS_MOBS_IDS, "centipede"), 2, "alexsmobs");
                    pools.pool(EnemyPoolRole.THEME).addAll(ModCompat.findEntitiesMatching(ALEXS_MOBS_IDS, "centipede", "dropbear"), 2, "alexsmobs");
                }
                if (level >= 35) {
                    pools.pool(EnemyPoolRole.ELITE).addAll(ModCompat.findEntitiesMatching(ALEXS_MOBS_IDS, "dropbear"), 2, "alexsmobs");
                }
            }
            case ARCANE -> {
                if (level >= 20) {
                    pools.pool(EnemyPoolRole.MELEE).addAll(ModCompat.findEntitiesMatching(ALEXS_MOBS_IDS, "enderiophage", "mimicube"), 2, "alexsmobs");
                    pools.pool(EnemyPoolRole.THEME).addAll(ModCompat.findEntitiesMatching(ALEXS_MOBS_IDS, "enderiophage", "mimicube"), 3, "alexsmobs");
                }
                if (level >= 35) {
                    pools.pool(EnemyPoolRole.SUPPORT).addAll(ModCompat.findEntitiesMatching(ALEXS_MOBS_IDS, "enderiophage"), 2, "alexsmobs");
                    pools.pool(EnemyPoolRole.ELITE).addAll(ModCompat.findEntitiesMatching(ALEXS_MOBS_IDS, "mimicube"), 2, "alexsmobs");
                }
            }
            case NETHER -> {
                if (level >= 20) {
                    pools.pool(EnemyPoolRole.FAST).addAll(ModCompat.findEntitiesMatching(ALEXS_MOBS_IDS, "crimson_mosquito"), 2, "alexsmobs");
                    pools.pool(EnemyPoolRole.SUPPORT).addAll(ModCompat.findEntitiesMatching(ALEXS_MOBS_IDS, "crimson_mosquito"), 2, "alexsmobs");
                }
                if (level >= 35) {
                    pools.pool(EnemyPoolRole.THEME).addAll(ModCompat.findEntitiesMatching(ALEXS_MOBS_IDS, "bone_serpent", "soul_vulture", "warped_mosco"), 2, "alexsmobs");
                    pools.pool(EnemyPoolRole.MELEE).addAll(ModCompat.findEntitiesMatching(ALEXS_MOBS_IDS, "bone_serpent", "warped_mosco"), 2, "alexsmobs");
                }
                if (level >= 50) {
                    pools.pool(EnemyPoolRole.ELITE).addAll(ModCompat.findEntitiesMatching(ALEXS_MOBS_IDS, "warped_mosco", "bone_serpent"), 2, "alexsmobs");
                    pools.pool(EnemyPoolRole.TANK).addAll(ModCompat.findEntitiesMatching(ALEXS_MOBS_IDS, "warped_mosco"), 2, "alexsmobs");
                }
            }
        }
    }

    private static void addDeeperDarkerCompat(CrystalTheme theme, int level, EnemyPoolSet pools) {
        ModCompat.debugDetected("Deeper and Darker", DEEPER_DARKER_IDS);
        if (!ModCompat.isAnyLoaded(DEEPER_DARKER_IDS)) {
            return;
        }
        if (theme == CrystalTheme.ARCANE || theme == CrystalTheme.UNDEAD) {
            if (level >= 20) {
                List<EntityType<?>> darkMobs = ModCompat.hostileEntitiesForNamespaces(DEEPER_DARKER_IDS);
                pools.pool(EnemyPoolRole.THEME).addAll(darkMobs, 3, "deeperdarker");
                pools.pool(EnemyPoolRole.ELITE).addAll(ModCompat.findEntitiesMatching(DEEPER_DARKER_IDS, "stalker", "shattered", "sculk"), 2, "deeperdarker");
            }
            if (level >= 50) {
                pools.pool(EnemyPoolRole.SUPPORT).addAll(ModCompat.findEntitiesMatching(DEEPER_DARKER_IDS, "shriek", "worm"), 2, "deeperdarker");
            }
        }
    }

    private static void addEndermanOverhaulCompat(CrystalTheme theme, int level, EnemyPoolSet pools) {
        ModCompat.debugDetected("Enderman Overhaul", ENDERMAN_OVERHAUL_IDS);
        if (!ModCompat.isAnyLoaded(ENDERMAN_OVERHAUL_IDS)) {
            return;
        }
        if (theme == CrystalTheme.ARCANE) {
            List<EntityType<?>> endermen = ModCompat.findEntitiesByIds(ARCANE_ENDERMAN_OVERHAUL_IDS);
            pools.pool(EnemyPoolRole.MELEE).addAll(endermen, 2, "endermanoverhaul");
            pools.pool(EnemyPoolRole.THEME).addAll(endermen, 3, "endermanoverhaul");
            if (level >= 35) {
                pools.pool(EnemyPoolRole.FAST).addAll(endermen, 2, "endermanoverhaul");
                pools.pool(EnemyPoolRole.ELITE).addAll(endermen, 2, "endermanoverhaul");
            }
        }
        if (theme == CrystalTheme.NETHER) {
            pools.pool(EnemyPoolRole.THEME).addAll(ModCompat.findEntitiesMatching(ENDERMAN_OVERHAUL_IDS, "nether", "warped", "crimson", "soulsand"), 2, "endermanoverhaul");
            if (level >= 20) {
                pools.pool(EnemyPoolRole.FAST).addAll(ModCompat.findEntitiesMatching(ENDERMAN_OVERHAUL_IDS, "nether", "warped", "crimson"), 2, "endermanoverhaul");
            }
        }
    }

    private static void addLuminousCompat(CrystalTheme theme, int level, EnemyPoolSet pools) {
        ModCompat.debugDetected("Luminous Monsters", LUMINOUS_IDS);
        if (level < 20 || !ModCompat.isAnyLoaded(LUMINOUS_IDS)) {
            return;
        }
        List<EntityType<?>> luminousMobs = ModCompat.hostileEntitiesForNamespaces(LUMINOUS_IDS);
        if (theme == CrystalTheme.ARCANE) {
            pools.pool(EnemyPoolRole.THEME).addAll(luminousMobs, 3, "luminous_monsters");
            pools.pool(EnemyPoolRole.RANGED).addAll(ModCompat.findEntitiesMatching(LUMINOUS_IDS, "mage", "caster", "seer"), 2, "luminous_monsters");
            if (level >= 35) {
                pools.pool(EnemyPoolRole.SUPPORT).addAll(ModCompat.findEntitiesMatching(LUMINOUS_IDS, "summon", "wisp", "shade"), 2, "luminous_monsters");
            }
        }
    }

    private static void addFriendsAndFoesCompat(CrystalTheme theme, int level, EnemyPoolSet pools) {
        ModCompat.debugDetected("Friends and Foes", FRIENDS_AND_FOES_IDS);
        if (!ModCompat.isAnyLoaded(FRIENDS_AND_FOES_IDS)) {
            return;
        }
        if (theme == CrystalTheme.NETHER && level >= 20) {
            List<EntityType<?>> wildfire = ModCompat.findEntitiesByIds(NETHER_FRIENDS_AND_FOES_IDS);
            pools.pool(EnemyPoolRole.ELITE).addAll(wildfire, 2, "friendsandfoes");
            pools.pool(EnemyPoolRole.TANK).addAll(wildfire, 2, "friendsandfoes");
        }
        if (theme == CrystalTheme.RAIDER) {
            List<EntityType<?>> assassins = ModCompat.findEntitiesByIds(RAIDER_FRIENDS_AND_FOES_IDS);
            List<EntityType<?>> illusioners = level >= 30 ? ModCompat.findEntitiesByIds(RAIDER_FRIENDS_AND_FOES_ILLUSIONER_IDS) : List.of();
            pools.pool(EnemyPoolRole.THEME).addAll(assassins, 2, "friendsandfoes");
            if (!illusioners.isEmpty()) {
                pools.pool(EnemyPoolRole.THEME).addAll(illusioners, 2, "friendsandfoes");
            }
            if (level >= 20) {
                pools.pool(EnemyPoolRole.FAST).addAll(assassins, 2, "friendsandfoes");
                if (!illusioners.isEmpty()) {
                    pools.pool(EnemyPoolRole.FAST).addAll(illusioners, 2, "friendsandfoes");
                }
            }
            if (level >= 35) {
                pools.pool(EnemyPoolRole.ELITE).addAll(assassins, 2, "friendsandfoes");
                if (!illusioners.isEmpty()) {
                    pools.pool(EnemyPoolRole.ELITE).addAll(illusioners, 2, "friendsandfoes");
                }
            }
        }
    }

    private static void addTakesAPillageCompat(CrystalTheme theme, int level, EnemyPoolSet pools) {
        ModCompat.debugDetected("It Takes a Pillage", TAKES_A_PILLAGE_IDS);
        if (theme != CrystalTheme.RAIDER || !ModCompat.isAnyLoaded(TAKES_A_PILLAGE_IDS)) {
            return;
        }
        pools.pool(EnemyPoolRole.RANGED).addAll(ModCompat.findEntitiesByIds("takesapillage:archer"), 3, "takesapillage");
        pools.pool(EnemyPoolRole.MELEE).addAll(ModCompat.findEntitiesByIds("takesapillage:skirmisher"), 2, "takesapillage");
        pools.pool(EnemyPoolRole.TANK).addAll(ModCompat.findEntitiesByIds("takesapillage:legioner"), 2, "takesapillage");
        pools.pool(EnemyPoolRole.THEME).addAll(ModCompat.findEntitiesByIds(RAIDER_TAKES_A_PILLAGE_IDS), 3, "takesapillage");
        if (level >= 20) {
            pools.pool(EnemyPoolRole.FAST).addAll(ModCompat.findEntitiesByIds("takesapillage:skirmisher"), 2, "takesapillage");
        }
        if (level >= 35) {
            pools.pool(EnemyPoolRole.ELITE).addAll(ModCompat.findEntitiesByIds("takesapillage:legioner"), 2, "takesapillage");
        }
    }

    private static void addVariantsCompat(CrystalTheme theme, int level, EnemyPoolSet pools) {
        ModCompat.debugDetected("Variants and Ventures", VARIANTS_IDS);
        if (!ModCompat.isAnyLoaded(VARIANTS_IDS)) {
            return;
        }
        List<EntityType<?>> variants = ModCompat.hostileEntitiesForNamespaces(VARIANTS_IDS);
        if (theme == CrystalTheme.UNDEAD) {
            List<EntityType<?>> undeadVariants = ModCompat.findEntitiesByIds(UNDEAD_VARIANTS_IDS);
            pools.pool(EnemyPoolRole.MELEE).addAll(ModCompat.findEntitiesByIds("variantsandventures:thicket"), 2, "variants_and_ventures");
            pools.pool(EnemyPoolRole.RANGED).addAll(ModCompat.findEntitiesByIds("variantsandventures:gelid", "variantsandventures:verdant"), 2, "variants_and_ventures");
            pools.pool(EnemyPoolRole.TANK).addAll(ModCompat.findEntitiesByIds("variantsandventures:murk"), 2, "variants_and_ventures");
            pools.pool(EnemyPoolRole.SUPPORT).addAll(ModCompat.findEntitiesByIds("variantsandventures:verdant", "variantsandventures:murk"), 2, "variants_and_ventures");
            pools.pool(EnemyPoolRole.THEME).addAll(undeadVariants, 3, "variants_and_ventures");
            if (level >= 35) {
                pools.pool(EnemyPoolRole.ELITE).addAll(undeadVariants, 2, "variants_and_ventures");
                pools.pool(EnemyPoolRole.FAST).addAll(ModCompat.findEntitiesByIds("variantsandventures:gelid", "variantsandventures:thicket"), 2, "variants_and_ventures");
            }
        }
        pools.pool(EnemyPoolRole.MELEE).addAll(ModCompat.findEntitiesMatching(VARIANTS_IDS, "zombie", "spider", "piglin"), 2, "variants_and_ventures");
        pools.pool(EnemyPoolRole.RANGED).addAll(ModCompat.findEntitiesMatching(VARIANTS_IDS, "skeleton", "archer", "pillager"), 2, "variants_and_ventures");
        if (level >= 20) {
            pools.pool(EnemyPoolRole.THEME).addAll(variants, theme == CrystalTheme.ARCANE ? 2 : 1, "variants_and_ventures");
        }
    }

    private static void addVillagersCompat(CrystalTheme theme, int level, EnemyPoolSet pools) {
        ModCompat.debugDetected("Villagers and Pillagers", VILLAGERS_IDS);
        if (level < 20 || !ModCompat.isAnyLoaded(VILLAGERS_IDS)) {
            return;
        }
        // Disabled to keep raider/arcane pools from overloading on illager casters.
    }
}
