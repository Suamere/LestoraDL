package com.lestora.dynamiclighting;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.lestora.dynamiclighting.models.SubChunkPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.player.Player;
import com.lestora.dynamiclighting.events.DLEvents;

public class LightingUpdateManager {
    // Use a concurrent set for pending sub-chunk scans.
    public static final Set<SubChunkPos> pendingSubChunkScans = ConcurrentHashMap.newKeySet();

    /**
     * Queues up all sub-chunks within the given chunk.
     * Each sub-chunk covers a vertical slice of 16 blocks from worldHeightMin (inclusive)
     * up to worldHeightMax (exclusive).
     */
    public static void queueChunkScan(ChunkPos cp, int playerY) {
        // Determine the player's sub-chunk starting Y using floor division.
        int baseY = Math.floorDiv(playerY, 16) * 16;

        // Define the bounds: up to 5 sub-chunks (80 blocks) below and 3 sub-chunks (48 blocks) above.
        // Clamp the values to -60 (min) and 320 (max).
        int minY = Math.max(-60, baseY - 5 * 16);
        int maxY = Math.min(320, baseY + 3 * 16);

        // Add each sub-chunk within the calculated range.
        for (int y = minY; y <= maxY; y += 16) {
            pendingSubChunkScans.add(new SubChunkPos(cp, y));
        }
    }

    /**
     * Processes one pending sub-chunk scan.
     * It selects the SubChunkPos with the lowest effective distance to the player.
     * Effective distance is calculated as:
     *    (dx^2 + dz^2) + 9 * (verticalDiff^2)
     * where verticalDiff is the difference (in sub-chunk indices) between the candidate and the player.
     */
    public static void processPendingScans(Player player) {
        if (!pendingSubChunkScans.isEmpty()) {
            int playerChunkX = player.blockPosition().getX() >> 4;
            int playerChunkZ = player.blockPosition().getZ() >> 4;
            int playerSubChunk = player.blockPosition().getY() >> 4; // player's sub-chunk index

            SubChunkPos closest = null;
            double bestDistance = Double.MAX_VALUE;

            // Iterate over all pending sub-chunks to determine the one closest to the player.
            for (SubChunkPos scp : pendingSubChunkScans) {
                int dx = scp.chunkPos.x - playerChunkX;
                int dz = scp.chunkPos.z - playerChunkZ;
                int verticalDiff = Math.abs((scp.startingY >> 4) - playerSubChunk);
                double horizontalDistSq = dx * dx + dz * dz;
                double effectiveDistance = horizontalDistSq + 9 * (verticalDiff * verticalDiff);

                if (effectiveDistance < bestDistance) {
                    bestDistance = effectiveDistance;
                    closest = scp;
                }
            }

            if (closest != null) {
                pendingSubChunkScans.remove(closest);
                // Pass in the startingY so scanChunk can work from there (e.g. scan startingY to startingY+16)
                DLEvents.scanChunk(closest.chunkPos.x, closest.chunkPos.z, closest.startingY);
            }
        }
    }
}
