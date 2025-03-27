package com.lestora.dynamiclighting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.lighting.LevelLightEngine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class DynamicLighting {
    public record EntityPair(Entity first, ResourceLocation second) {}
    public record PosAndName(BlockPos position, ResourceLocation resource) {}

    private static boolean enabled = true;
    private static final Lock lock = new ReentrantLock();
    private static final ConcurrentHashMap<UUID, PosAndName> currentPositions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, EntityPair> registeredEntities = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Long, Integer> dynamicLightCache = new ConcurrentHashMap<>();

    // Call this method periodically (e.g., at the end of tryUpdateEntityPositions or on a timer) to refresh the cache.
    public static void refreshDynamicLightCache() {
        lock.lock();
        try {
            dynamicLightCache.clear();
            for (var entry : registeredEntities.entrySet()) {
                Entity e = entry.getValue().first();
                ResourceLocation resource = entry.getValue().second();
                Integer lightLevel = LestoraDLMod.getEntityLightLevel.apply(resource);
                if (lightLevel != null) {
                    dynamicLightCache.put(e.blockPosition().asLong(), lightLevel);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public static Integer getDynamicLightLevel(long pos) {
        return dynamicLightCache.get(pos);
    }

    public static Collection<PosAndName> getCurrentPositions() {
        lock.lock();
        try {
            return new ArrayList<>(currentPositions.values());
        } finally {
            lock.unlock();
        }
    }

    public static void tryUpdateEntityPositions() {
        lock.lock();
        try {
            for (Map.Entry<UUID, EntityPair> entry : registeredEntities.entrySet()) {
                EntityPair e = entry.getValue();
                BlockPos newPos = e.first().blockPosition();
                PosAndName oldPos = currentPositions.getOrDefault(e.first().getUUID(), new PosAndName(BlockPos.ZERO, null));

                if (!newPos.equals(oldPos.position)) {
                    currentPositions.put(e.first().getUUID(), new PosAndName(newPos.immutable(), e.second()));

                    var level = Minecraft.getInstance().level;
                    if (level != null) {
                        ClientChunkCache chunkSource = level.getChunkSource();
                        LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                        lightingEngine.checkBlock(newPos);
                        lightingEngine.checkBlock(oldPos.position);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
        refreshDynamicLightCache();
    }

    public static void tryAddEntity(Entity e, ResourceLocation resource) {
        lock.lock();
        try {
            registeredEntities.compute(e.getUUID(), (uuid, existingPair) -> {
                if (existingPair == null) {
                    return new EntityPair(e, resource);
                } else if (!existingPair.second().equals(resource)) {
                    return new EntityPair(e, resource);
                }
                return existingPair;
            });
        } finally {
            lock.unlock();
        }
        refreshDynamicLightCache();
    }

    public static void removeAll() {
        lock.lock();
        try {
            var level = Minecraft.getInstance().level;
            registeredEntities.clear();
            for (var oldPos : currentPositions.entrySet()) {
                if (level != null) {
                    ClientChunkCache chunkSource = level.getChunkSource();
                    LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                    lightingEngine.checkBlock(oldPos.getValue().position);
                }
            }
            currentPositions.clear();
        } finally {
            lock.unlock();
        }
        refreshDynamicLightCache();
    }

    public static void tryRemoveEntity(Entity e) {
        lock.lock();
        try {
            registeredEntities.remove(e.getUUID());
            var oldPos = currentPositions.remove(e.getUUID());
            var level = Minecraft.getInstance().level;
            if (level != null && oldPos != null) {
                ClientChunkCache chunkSource = level.getChunkSource();
                LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                lightingEngine.checkBlock(oldPos.position);
            }
        } finally {
            lock.unlock();
        }
        refreshDynamicLightCache();
    }

    public static boolean getEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean newVal) {
        lock.lock();
        try {
            if (newVal == enabled) return;

            enabled = newVal;
            if (!newVal) {
                for (EntityPair e : registeredEntities.values()) {
                    var oldPos = currentPositions.getOrDefault(e.first().getUUID(), new PosAndName(BlockPos.ZERO, null));

                    var level = Minecraft.getInstance().level;
                    if (level != null) {
                        ClientChunkCache chunkSource = level.getChunkSource();
                        LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                        lightingEngine.checkBlock(oldPos.position);
                    }
                }
            }
            else {
                for (EntityPair e : registeredEntities.values()) {
                    BlockPos newPos = e.first().blockPosition();

                    var level = Minecraft.getInstance().level;
                    if (level != null) {
                        ClientChunkCache chunkSource = level.getChunkSource();
                        LevelLightEngine lightingEngine = chunkSource.getLightEngine();
                        lightingEngine.checkBlock(newPos);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }
}