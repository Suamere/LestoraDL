package com.lestora.dynamiclighting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.lighting.LevelLightEngine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class DynamicBlockLighting {
    public record PosAndName(BlockPos position, ResourceLocation resource) {}

    private static boolean enabled = true;
    private static final Lock lock = new ReentrantLock();
    private static final ConcurrentHashMap<BlockPos, ResourceLocation> registeredBlocks = new ConcurrentHashMap<>();
    // Add these near your other static fields:
    private static final ConcurrentHashMap<Long, Integer> blockLightCache = new ConcurrentHashMap<>();

    // Call this method to remove a block from the cache.
    public static void removeBlockCache(BlockPos pos) {
        lock.lock();
        try {
            blockLightCache.remove(pos.asLong());
        } finally {
            lock.unlock();
        }
    }

    // Refresh the cache when blocks update (e.g., after tryAddBlock/tryRemoveBlock):
    public static void refreshBlockLightCache() {
        lock.lock();
        try {
            blockLightCache.clear();
            for (var entry : registeredBlocks.entrySet()) {
                long posKey = entry.getKey().asLong();
                Integer lightLevel = LestoraDLMod.getMaxLightLevel.apply(entry.getValue(), true);
                if (lightLevel != null) {
                    blockLightCache.put(posKey, lightLevel);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    // Getter for mixin use:
    public static Integer getBlockLightLevel(long pos) {
        return blockLightCache.get(pos);
    }


    public static boolean getEnabled() { return enabled; }

    public static void setEnabled(boolean newVal) {
        enabled = newVal;
    }

    public static Collection<BlockPos> getRegisteredBlockPositions() {
        lock.lock();
        try {
            return new ArrayList<>(registeredBlocks.keySet());
        } finally {
            lock.unlock();
        }
    }

    public static void tryAddBlock(BlockPos pos, ResourceLocation resource, int lightLevel) {
        lock.lock();
        try {
            var level = Minecraft.getInstance().level;
            if (level != null) {
                boolean allOcclude = true;
                for (var d : net.minecraft.core.Direction.values()) {
                    BlockPos neighborPos = pos.relative(d);
                    if (!level.getBlockState(neighborPos).canOcclude()) {
                        allOcclude = false;
                        break;
                    }
                }
                if (allOcclude) return;
            }
            registeredBlocks.put(pos, resource);
            blockLightCache.put(pos.asLong(), lightLevel);
            if (level != null) {
                ClientChunkCache chunkSource = level.getChunkSource();
                LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                lightingEngine.checkBlock(pos);
            }
        } finally {
            lock.unlock();
        }
    }

    public static void removeAll() {
        lock.lock();
        try {
            var level = Minecraft.getInstance().level;
            for (var oldPos : registeredBlocks.entrySet()) {
                if (level != null) {
                    ClientChunkCache chunkSource = level.getChunkSource();
                    LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                    lightingEngine.checkBlock(oldPos.getKey());
                }
            }
            registeredBlocks.clear();
        } finally {
            lock.unlock();
        }
        refreshBlockLightCache();
    }

    public static void tryRemoveBlock(BlockPos pos) {
        lock.lock();
        try {
            registeredBlocks.remove(pos);
            var level = Minecraft.getInstance().level;
            if (level != null) {
                ClientChunkCache chunkSource = level.getChunkSource();
                LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                lightingEngine.checkBlock(pos);
            }
        } finally {
            lock.unlock();
        }
        refreshBlockLightCache();
    }
}