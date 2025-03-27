package com.lestora.dynamiclighting.config;

import com.lestora.config.LightConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class RealConfigHandler {
    public static Function<ResourceLocation, Integer> getMinLightLevelFunc() {
        return (rl) -> LightConfig.getMinLightLevel(rl);
    }

    public static Function<ResourceLocation, Integer> getMaxLightLevelFunc() {
        return (rl) -> LightConfig.getUniqueMaxLightLevel(rl);
    }
}