package com.lestora.dynamiclighting;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

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
                    // Save the new position in our tracking map.
                    currentPositions.put(e.first().getUUID(), new PosAndName(newPos.immutable(), e.second()));
                    // Update the new position normally.
                    LestoraDLMod.checkBlock(Minecraft.getInstance().level, newPos);
                    // And update the old position via our removal method,
                    // so that it gets added to the pending-check cache.
                    if (!oldPos.position.equals(BlockPos.ZERO)) {
                        LestoraDLMod.checkBlockRemoval(Minecraft.getInstance().level, oldPos.position);
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
            UUID uuid = e.getUUID();
            boolean isNew = !registeredEntities.containsKey(uuid);
            // Always record the current block position when adding the entity.
            PosAndName initial = new PosAndName(e.blockPosition().immutable(), resource);
            if (isNew) {
                currentPositions.put(uuid, initial);
                registeredEntities.put(uuid, new EntityPair(e, resource));
                // Immediately force a light update for the entity's current position.
                LestoraDLMod.checkBlock(Minecraft.getInstance().level, e.blockPosition());
            } else {
                // If already present, update resource if needed.
                registeredEntities.compute(uuid, (id, existingPair) -> {
                    if (existingPair == null || !existingPair.second().equals(resource)) {
                        return new EntityPair(e, resource);
                    }
                    return existingPair;
                });
            }
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
            if (oldPos != null) {
                var level = Minecraft.getInstance().level;
                if (level != null) {
                    // Update the position where the entity was removed...
                    LestoraDLMod.checkBlockRemoval(level, oldPos.position);
                    // ...and update its neighbors to force full propagation.
                    for (var d : net.minecraft.core.Direction.values()) {
                        LestoraDLMod.checkBlockRemoval(level, oldPos.position.relative(d));
                    }
                }
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
                    LestoraDLMod.checkBlockRemoval(Minecraft.getInstance().level, oldPos.position);
                }
            }
            else {
                for (EntityPair e : registeredEntities.values()) {
                    BlockPos newPos = e.first().blockPosition();
                    LestoraDLMod.checkBlock(Minecraft.getInstance().level, newPos);
                }
            }
        } finally {
            lock.unlock();
        }
    }
}