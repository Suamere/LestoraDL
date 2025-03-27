package com.lestora.dynamiclighting;

import com.lestora.dynamiclighting.config.AlternateConfigHandler;
import com.lestora.dynamiclighting.config.RealConfigHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.function.BiFunction;

@Mod("lestora_dynamic_lights")
public class LestoraDLMod {
    public static boolean configLoaded = false;
    //public static BiFunction<ResourceLocation, Integer, Integer> getLightLevelQTY;
    public static BiFunction<ResourceLocation, Boolean, Integer> getMinLightLevel;
    public static BiFunction<ResourceLocation, Boolean, Integer> getMaxLightLevel;

    public LestoraDLMod(FMLJavaModLoadingContext constructContext) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("lestora_config")) {
            //getLightLevelQTY = OptionalConfigHandler.getLightLevelWithQtyFunc();
            getMinLightLevel = RealConfigHandler.getMinLightLevelFunc();
            getMaxLightLevel = RealConfigHandler.getMaxLightLevelFunc();
            configLoaded = true;
        }
        else {
            //getLightLevelQTY = AlternateConfigHandler.getLightLevelWithQtyFunc();
            getMinLightLevel = AlternateConfigHandler.getMinLightLevelFunc();
            getMaxLightLevel = AlternateConfigHandler.getMaxLightLevelFunc();
        }
    }
}