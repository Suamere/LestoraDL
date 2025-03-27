package com.lestora.dynamiclighting;

import com.lestora.config.LightConfig;
import com.lestora.dynamiclighting.config.AlternateConfigHandler;
import com.lestora.dynamiclighting.config.RealConfigHandler;
import com.lestora.dynamiclighting.events.DLEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.function.Function;

@Mod("lestora_dynamic_lights")
public class LestoraDLMod {
    public static boolean configLoaded = false;
    public static Function<ResourceLocation, Integer> getEntityLightLevel;
    public static Function<ResourceLocation, Integer> getBlockLightLevel;

    public LestoraDLMod(FMLJavaModLoadingContext constructContext) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("lestora_config")) {
            getEntityLightLevel = RealConfigHandler.getMinLightLevelFunc();
            getBlockLightLevel = RealConfigHandler.getMaxLightLevelFunc();
            configLoaded = true;
            LightConfig.subscribe(DLEvents::resetBlockChunkScans);
        }
        else {
            getEntityLightLevel = AlternateConfigHandler.getMinLightLevelFunc();
            getBlockLightLevel = AlternateConfigHandler.getMaxLightLevelFunc();
        }
    }
}