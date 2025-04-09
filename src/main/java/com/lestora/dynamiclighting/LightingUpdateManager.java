package com.lestora.dynamiclighting;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.lestora.dynamiclighting.models.SubChunkPos;
import net.minecraft.client.Minecraft;
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
            //Minecraft.getInstance().getConnection().send(new ServerboundChatCommandPacket("setblock " + (closest.chunkPos.x * 16 + 8) + " 100 " + (closest.chunkPos.z * 16 + 8) + " minecraft:sea_lantern"));
            pendingSubChunkScans.remove(closest);
            DLEvents.scanChunk(closest);
        }
    }

    private static @Nullable SubChunkPos getClosest(int playerChunkX, int playerChunkZ, int playerSubChunk) {
        SubChunkPos closest = null;
        double bestDistance = Double.MAX_VALUE;
        var level = Minecraft.getInstance().level;
        if (level == null) return null;

        final int groundThreshold = 4;
        for (SubChunkPos scp : pendingSubChunkScans) {
            if (!level.hasChunk(scp.chunkPos.x, scp.chunkPos.z)) {
                continue;
            }
            int dx = scp.chunkPos.x - playerChunkX;
            int dz = scp.chunkPos.z - playerChunkZ;
            double horizontalDistSq = dx * dx + dz * dz;

            // Compute vertical difference using sub-chunk indices.
            int scpSubChunk = scp.startingY >> 4; // scp's sub-chunk index.
            int verticalDiff = Math.abs(scpSubChunk - playerSubChunk);

            // Base effective distance calculation.
            double effectiveDistance = horizontalDistSq + 3 * (verticalDiff * verticalDiff);

            // Apply additional penalty for sub-chunks that are in a different vertical zone.
            // For example, if the player is above ground and the sub-chunk is below ground, add a high penalty.
            if (playerSubChunk > groundThreshold && scpSubChunk < groundThreshold) {
                effectiveDistance += 1000;  // Large penalty.
            } else if (playerSubChunk < groundThreshold && scpSubChunk >= groundThreshold) {
                effectiveDistance += 1000;  // Likewise, if the player is underground and the chunk is above.
            }

            if (effectiveDistance < bestDistance) {
                bestDistance = effectiveDistance;
                closest = scp;
            }
        }
        return closest;
    }
}