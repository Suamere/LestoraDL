package com.lestora.dynamiclighting.config;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class AlternateConfigHandler {

    // Create a static, unmodifiable map with ResourceLocation keys.
    private static final Map<ResourceLocation, Integer> PRIORITY_MAP;
    static {
        Map<ResourceLocation, Integer> map = new HashMap<>();
        map.put(ResourceLocation.fromNamespaceAndPath("minecraft", "torch"), 14);
        map.put(ResourceLocation.fromNamespaceAndPath("minecraft", "lantern"), 15);
        map.put(ResourceLocation.fromNamespaceAndPath("minecraft", "sea_lantern"), 15);
        map.put(ResourceLocation.fromNamespaceAndPath("minecraft", "shroomlight"), 15);
        map.put(ResourceLocation.fromNamespaceAndPath("minecraft", "soul_lantern"), 10);
        map.put(ResourceLocation.fromNamespaceAndPath("minecraft", "ochre_froglight"), 15);
        map.put(ResourceLocation.fromNamespaceAndPath("minecraft", "pearlescent_froglight"), 15);
        map.put(ResourceLocation.fromNamespaceAndPath("minecraft", "verdant_froglight"), 15);
        map.put(ResourceLocation.fromNamespaceAndPath("minecraft", "campfire"), 15);
        map.put(ResourceLocation.fromNamespaceAndPath("minecraft", "candle"), 3);
        map.put(ResourceLocation.fromNamespaceAndPath("minecraft", "end_rod"), 14);
        map.put(ResourceLocation.fromNamespaceAndPath("minecraft", "fire"), 15);
        map.put(ResourceLocation.fromNamespaceAndPath("minecraft", "soul_torch"), 10);
        map.put(ResourceLocation.fromNamespaceAndPath("minecraft", "soul_fire"), 10);
        map.put(ResourceLocation.fromNamespaceAndPath("minecraft", "redstone_torch"), 7);
        PRIORITY_MAP = Collections.unmodifiableMap(map);
    }

    // Returns a BiFunction that looks up a ResourceLocation directly in the PRIORITY_MAP.
    public static BiFunction<ResourceLocation, Integer, Integer> getLightLevelWithQtyFunc() {
        return (rl, amount) -> {
            if (rl == null) { return 0; }
            return PRIORITY_MAP.getOrDefault(rl, 0);
        };
    }

    // Returns a BiFunction that looks up a ResourceLocation directly in the PRIORITY_MAP.
    public static Function<ResourceLocation, Integer> getMinLightLevelFunc() {
        return (rl) -> (rl == null) ? 0 : PRIORITY_MAP.getOrDefault(rl, 0);
    }

    // Returns a BiFunction that looks up a ResourceLocation directly in the PRIORITY_MAP.
    public static Function<ResourceLocation, Integer> getMaxLightLevelFunc() {
        return (rl) -> (rl == null) ? 0 : PRIORITY_MAP.getOrDefault(rl, 0);
    }
}
