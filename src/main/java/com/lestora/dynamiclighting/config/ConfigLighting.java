package com.lestora.dynamiclighting.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigLighting {
    public static final ForgeConfigSpec LIGHTING_CONFIG;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> LIGHT_LEVELS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("light_levels");
        LIGHT_LEVELS = builder.comment("List of item=light level pairs")
                .defineList("items",
                        Arrays.asList("minecraft:torch=14", "minecraft:lava_bucket=10", "minecraft:glowstone=8"),
                        o -> o instanceof String && ((String)o).contains("="));
        builder.pop();
        LIGHTING_CONFIG = builder.build();
    }

    public static Map<ResourceLocation, Integer> getLightLevelsMap() {
        Map<ResourceLocation, Integer> map = new HashMap<>();
        for (String entry : LIGHT_LEVELS.get()) {
            String[] split = entry.split("=");
            if (split.length == 2) {
                String itemId = split[0].trim();
                try {
                    int level = Integer.parseInt(split[1].trim());
                    ResourceLocation loc = ResourceLocation.tryParse(itemId);
                    if (loc != null) { map.put(loc, level); }
                } catch (NumberFormatException ignored) { }
            }
        }
        return map;
    }
}