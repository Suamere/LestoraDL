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

    private static boolean enabled = false;
    private static final Lock lock = new ReentrantLock();
    private static final ConcurrentHashMap<BlockPos, PosAndName> currentPositions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<BlockPos, ResourceLocation> registeredBlocks = new ConcurrentHashMap<>();

    public static boolean getEnabled() { return enabled; }

    public static void setEnabled(boolean newVal) {
        lock.lock();
        try {
            if (newVal == enabled) return;
            enabled = newVal;
            var level = Minecraft.getInstance().level;
            if (level != null) {
                ClientChunkCache chunkSource = level.getChunkSource();
                LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                for (BlockPos pos : registeredBlocks.keySet()) {
                    lightingEngine.checkBlock(pos);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public static Collection<PosAndName> getCurrentPositions() {
        lock.lock();
        try {
            return new ArrayList<>(currentPositions.values());
        } finally {
            lock.unlock();
        }
    }

    public static Collection<BlockPos> getRegisteredBlockPositions() {
        lock.lock();
        try {
            return new ArrayList<>(registeredBlocks.keySet());
        } finally {
            lock.unlock();
        }
    }

    public static void tryAddBlock(BlockPos pos, ResourceLocation resource) {
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
            currentPositions.put(pos, new PosAndName(pos, resource));
            if (level != null) {
                ClientChunkCache chunkSource = level.getChunkSource();
                LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                lightingEngine.checkBlock(pos);
            }
        } finally {
            lock.unlock();
        }
    }

    public static void tryRemoveBlock(BlockPos pos) {
        lock.lock();
        try {
            registeredBlocks.remove(pos);
            currentPositions.remove(pos);
            var level = Minecraft.getInstance().level;
            if (level != null) {
                ClientChunkCache chunkSource = level.getChunkSource();
                LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                lightingEngine.checkBlock(pos);
            }
        } finally {
            lock.unlock();
        }
    }
}