package com.lestora;

import com.lestora.dynamiclighting.config.ConfigLighting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("lestora_dynamic_lights")
public class LestoraMod {
    public LestoraMod(FMLJavaModLoadingContext constructContext) {
        constructContext.registerConfig(ModConfig.Type.COMMON, ConfigLighting.LIGHTING_CONFIG, "lestora-lighting.toml");
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }
}