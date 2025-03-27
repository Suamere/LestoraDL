package com.lestora.dynamiclighting;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.lestora.dynamiclighting.models.SubChunkPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.player.Player;
import com.lestora.dynamiclighting.events.DLEvents;
import org.jetbrains.annotations.Nullable;

public class LightingUpdateManager {
    // Use a concurrent set for pending sub-chunk scans.
    public static final Set<SubChunkPos> pendingSubChunkScans = ConcurrentHashMap.newKeySet();

    public static void queueChunkScan(ChunkPos cp, int playerY) {
        int baseY = Math.floorDiv(playerY, 16) * 16;

        int minY = Math.max(-60, baseY - 3 * 16);
        int maxY = Math.min(320, baseY + 3 * 16);

        for (int y = minY; y <= maxY; y += 16)
            pendingSubChunkScans.add(new SubChunkPos(cp, y));
    }

    public static void processPendingScans(Player player) {
        int playerChunkX = player.blockPosition().getX() >> 4;
        int playerChunkZ = player.blockPosition().getZ() >> 4;
        int playerSubChunk = player.blockPosition().getY() >> 4; // player's sub-chunk index

        SubChunkPos closest = getClosest(playerChunkX, playerChunkZ, playerSubChunk);
        if (closest != null) {
            pendingSubChunkScans.remove(closest);
            DLEvents.scanChunk(closest);
        }
    }

    private static @Nullable SubChunkPos getClosest(int playerChunkX, int playerChunkZ, int playerSubChunk) {
        SubChunkPos closest = null;
        double bestDistance = Double.MAX_VALUE;

        for (SubChunkPos scp : pendingSubChunkScans) {
            int dx = scp.chunkPos.x - playerChunkX;
            int dz = scp.chunkPos.z - playerChunkZ;
            int verticalDiff = Math.abs((scp.startingY >> 4) - playerSubChunk);
            double horizontalDistSq = dx * dx + dz * dz;
            double effectiveDistance = horizontalDistSq + 3 * (verticalDiff * verticalDiff);

            if (effectiveDistance < bestDistance) {
                bestDistance = effectiveDistance;
                closest = scp;
            }
        }
        return closest;
    }
}