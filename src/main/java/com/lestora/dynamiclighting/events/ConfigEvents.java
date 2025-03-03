package com.lestora.dynamiclighting.events;

import com.lestora.dynamiclighting.config.ConfigLighting;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigEvents {
    public static Map<ResourceLocation, Integer> lightLevelsMap = new HashMap<>();

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ConfigLighting.LIGHTING_CONFIG) {
            lightLevelsMap = ConfigLighting.getLightLevelsMap();
        }
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ConfigLighting.LIGHTING_CONFIG) {
            lightLevelsMap = ConfigLighting.getLightLevelsMap();
        }
    }

    public static Integer getLightLevel(ResourceLocation rl) {

        return lightLevelsMap.get(rl);
    }
}