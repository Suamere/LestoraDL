package com.lestora.dynamiclighting;

import com.lestora.config.LightConfig;
import com.lestora.dynamiclighting.config.AlternateConfigHandler;
import com.lestora.dynamiclighting.config.RealConfigHandler;
import com.lestora.dynamiclighting.events.DLEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
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

    public static void checkBlock(ClientLevel level, BlockPos... positions) {
        if (level != null){
            for (var pos : positions) {
                if (pos != null) {
                    level.getChunkSource().getLightEngine().checkBlock(pos);
                }
            }
        }
    }
}