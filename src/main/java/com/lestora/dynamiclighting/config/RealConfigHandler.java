package com.lestora.dynamiclighting.config;

import com.lestora.config.LestoraConfig;
import com.lestora.config.RLAmount;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

public class RealConfigHandler {
    private static final ConcurrentMap<RLAmount, Integer> ignore = new ConcurrentHashMap<>();
    private static final Map<RLAmount, Integer> blockLightLevelsMap = new HashMap<>();

    private static final Map<ResourceLocation, Integer> blockMinCache = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Integer> blockMaxCache = new ConcurrentHashMap<>();

    static {
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "light"), 15), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "light"), 14), 14);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "light"), 13), 13);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "light"), 12), 12);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "light"), 11), 11);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "light"), 10), 10);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "light"), 9), 9);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "light"), 7), 7);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "light"), 6), 6);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "light"), 5), 5);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "light"), 4), 4);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "light"), 3), 3);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "light"), 2), 2);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "light"), 1), 1);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "sea_pickle"), 4), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "sea_pickle"), 3), 12);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "sea_pickle"), 2), 9);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "sea_pickle"), 1), 6);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "respawn_anchor"), 4), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "respawn_anchor"), 3), 11);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "respawn_anchor"), 2), 7);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "respawn_anchor"), 1), 3);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "candle"), 4), 12);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "candle"), 3), 9);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "candle"), 2), 6);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "candle"), 1), 3);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "campfire"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "redstone_lamp"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "soul_campfire"), 1), 10);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "blast_furnace"), 1), 13);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "furnace"), 1), 13);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "smoker"), 1), 13);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "deepslate_redstone_ore"), 1), 9);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "redstone_ore"), 1), 9);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "redstone_torch"), 1), 7);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "beacon"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "conduit"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "end_gateway"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "end_portal"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "fire"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "ochre_froglight"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "pearlescent_froglight"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "verdant_froglight"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "glowstone"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "jack_o_lantern"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "lantern"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "lava"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "lava_cauldron"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "sea_lantern"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "shroomlight"), 1), 15);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "cave_vines"), 1), 14);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "end_rod"), 1), 14);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "torch"), 1), 14);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "nether_portal"), 1), 11);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "crying_obsidian"), 1), 10);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "soul_fire"), 1), 10);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "soul_lantern"), 1), 10);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "soul_torch"), 1), 10);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "enchanting_table"), 1), 7);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "ender_chest"), 1), 7);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "glow_lichen"), 1), 7);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "sculk_catalyst"), 1), 6);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "amethyst_cluster"), 1), 5);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "large_amethyst_bud"), 1), 4);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "magma_block"), 1), 3);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "medium_amethyst_bud"), 1), 2);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "brewing_stand"), 1), 1);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "brown_mushroom"), 1), 1);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "dragon_egg"), 1), 1);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "end_portal_frame"), 1), 1);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "sculk_sensor"), 1), 1);
        ignore.put(new RLAmount(ResourceLocation.fromNamespaceAndPath("minecraft", "small_amethyst_bud"), 1), 1);

        updateBlockMap();
    }

    public static void updateBlockMap() {
        blockLightLevelsMap.clear();
        blockMinCache.clear();
        blockMaxCache.clear();
        Map<RLAmount, Integer> allLevels = LestoraConfig.getLightLevels();
        for (Map.Entry<RLAmount, Integer> entry : allLevels.entrySet()) {
            RLAmount key = entry.getKey();
            Integer value = entry.getValue();
            if (!(ignore.containsKey(key) && ignore.get(key).equals(value)))
                blockLightLevelsMap.put(key, value);
        }
    }

    // BiFunction that returns the minimum light level for a given ResourceLocation.
    // If forBlocks is true, use the local cache; otherwise, fallback to LestoraConfig.
    public static BiFunction<ResourceLocation, Boolean, Integer> getMinLightLevelFunc() {
        return (rl, forBlocks) -> {
            if (forBlocks) {
                // Try to get from local cache first.
                if (blockMinCache.containsKey(rl)) {
                    return blockMinCache.get(rl);
                }
                Integer min = null;
                for (Map.Entry<RLAmount, Integer> entry : blockLightLevelsMap.entrySet()) {
                    if (entry.getKey().getResource().equals(rl)) {
                        int val = entry.getValue();
                        if (min == null || val < min) {
                            min = val;
                        }
                    }
                }
                if (min != null) {
                    blockMinCache.put(rl, min);
                }
                return min;
            } else {
                return LestoraConfig.getMinLightLevel(rl);
            }
        };
    }

    // BiFunction that returns the maximum light level for a given ResourceLocation.
    // If forBlocks is true, use the local cache; otherwise, fallback to LestoraConfig.
    public static BiFunction<ResourceLocation, Boolean, Integer> getMaxLightLevelFunc() {
        return (rl, forBlocks) -> {
            if (forBlocks) {
                if (blockMaxCache.containsKey(rl))
                    return blockMaxCache.get(rl);

                Integer max = null;
                for (var entry : blockLightLevelsMap.entrySet()) {
                    if (entry.getKey().getResource().equals(rl)) {
                        int val = entry.getValue();
                        if (max == null || val > max) {
                            max = val;
                        }
                    }
                }
                if (max != null) {
                    blockMaxCache.put(rl, max);
                }
                return max;
            } else {
                return LestoraConfig.getMaxLightLevel(rl);
            }
        };
    }
}
