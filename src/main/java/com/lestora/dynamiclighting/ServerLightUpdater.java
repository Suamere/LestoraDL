package com.lestora.dynamiclighting;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Iterator;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerLightUpdater {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        ServerLevel overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld == null) return;

        Iterator<BlockPos> it = LestoraDLMod.pendingCheckPositions.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            overworld.getChunkSource().getLightEngine().checkBlock(pos);

            //Integer levelValue = DynamicBlockLighting.getBlockLightLevel(pos.asLong());
            //if (levelValue == null || levelValue == 0) {
                it.remove();
            //}
        }
    }
}