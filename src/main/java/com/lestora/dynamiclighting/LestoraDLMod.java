package com.lestora.dynamiclighting;

import com.lestora.config.LightConfig;
import com.lestora.dynamiclighting.config.AlternateConfigHandler;
import com.lestora.dynamiclighting.config.RealConfigHandler;
import com.lestora.dynamiclighting.events.DLEvents;
import com.lestora.dynamiclighting.models.PendingCheck;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Mod("lestora_dynamic_lights")
public class LestoraDLMod {
    public static boolean configLoaded = false;
    public static Function<ResourceLocation, Integer> getEntityLightLevel;
    public static Function<ResourceLocation, Integer> getBlockLightLevel;

    public static final Set<PendingCheck> pendingCheckPositions = ConcurrentHashMap.newKeySet();

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

    // Modified checkBlock: calls the LightEngine check and stores the pos in the pending set.
    public static void checkBlock(ClientLevel level, BlockPos... positions) {
        if (level != null) {
            for (var pos : positions) {
                if (pos != null) {
                    level.getChunkSource().getLightEngine().checkBlock(pos);
                }
            }
        }
    }

    public static void checkBlockRemoval(ClientLevel level, BlockPos... positions) {
        if (level != null) {
            // Get the dimension key from the client-level.
            ResourceKey<Level> dimKey = level.dimension();
            for (var pos : positions) {
                if (pos != null) {
                    level.getChunkSource().getLightEngine().checkBlock(pos);
                    // Store both the pos and the dimension key.
                    pendingCheckPositions.add(new PendingCheck(pos, dimKey));
                }
            }
        }
    }
}