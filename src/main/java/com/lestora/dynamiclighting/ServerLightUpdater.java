package com.lestora.dynamiclighting;

import com.lestora.dynamiclighting.models.PendingCheck;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Iterator;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerLightUpdater {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Iterator<PendingCheck> it = LestoraDLMod.pendingCheckPositions.iterator();
        while (it.hasNext()) {
            PendingCheck pending = it.next();
            ServerLevel level = server.getLevel(pending.dimension());
            if (level != null)
                level.getChunkSource().getLightEngine().checkBlock(pending.pos());

            //Integer levelValue = DynamicBlockLighting.getBlockLightLevel(pos.asLong());
            //if (levelValue == null || levelValue == 0) {
                it.remove();
            //}
        }
    }
}