package com.lestora.dynamiclighting;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class DynamicBlockLighting {
    private static boolean enabled = true;
    private static final Lock lock = new ReentrantLock();
    private static final ConcurrentHashMap<BlockPos, ResourceLocation> registeredBlocks = new ConcurrentHashMap<>();
    // Add these near your other static fields:
    private static final ConcurrentHashMap<Long, Integer> blockLightCache = new ConcurrentHashMap<>();

    // Refresh the cache when blocks update (e.g., after tryAddBlock/tryRemoveBlock):
    public static void refreshBlockLightCache() {
        lock.lock();
        try {
            blockLightCache.clear();
            for (var entry : registeredBlocks.entrySet()) {
                long posKey = entry.getKey().asLong();
                Integer lightLevel = LestoraDLMod.getBlockLightLevel.apply(entry.getValue());
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
            if (level == null) return;

            boolean allOcclude = true;
            for (var d : net.minecraft.core.Direction.values()) {
                BlockPos neighborPos = pos.relative(d);
                if (!level.getBlockState(neighborPos).canOcclude()) {
                    allOcclude = false;
                    break;
                }
            }
            if (allOcclude) return;

            registeredBlocks.put(pos, resource);
            blockLightCache.put(pos.asLong(), lightLevel);
            LestoraDLMod.checkBlock(level, pos);
        } finally {
            lock.unlock();
        }
    }

    public static void tryRemoveBlock(BlockPos pos) {
        lock.lock();
        try {
            registeredBlocks.remove(pos);
            blockLightCache.remove(pos.asLong());
        } finally {
            lock.unlock();
        }

        refreshBlockLightCache();
        LestoraDLMod.checkBlock(Minecraft.getInstance().level, pos);
    }
}