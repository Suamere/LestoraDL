package com.lestora.dynamiclighting.events;

import com.lestora.dynamiclighting.DynamicBlockLighting;
import com.lestora.dynamiclighting.DynamicLighting;
import com.lestora.dynamiclighting.LestoraDLMod;
import com.lestora.dynamiclighting.LightingUpdateManager;
import com.lestora.dynamiclighting.config.RealConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class DLEvents {
    private static final Map<Item, ResourceLocation> itemKeyCache = new ConcurrentHashMap<>();
    private static final Map<UUID, ResourceLocation> previousTorchState = new ConcurrentHashMap<>();
    private static int tick_delay = 5;
    private static int tickCounter = 0;

    public static Set<ChunkPos> previousChunks = new HashSet<>();
//    private static int clientTicks = 0;
//    private static boolean initialDelayDone = false;

    private static final Map<Block, ResourceLocation> blockKeyCache = new ConcurrentHashMap<>();

    public static ResourceLocation getCachedBlockKey(Block block) {
        return blockKeyCache.computeIfAbsent(block, ForgeRegistries.BLOCKS::getKey);
    }

    public static void setTickDelay(int delay) {
        tick_delay = delay;
        DynamicLighting.setEnabled(delay > 0);
    }

    public static void enableBlocks(boolean enable) {
        DynamicBlockLighting.setEnabled(enable);
    }

    public static ResourceLocation getCachedItemKey(Item item) {
        return itemKeyCache.computeIfAbsent(item, ForgeRegistries.ITEMS::getKey);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (++tickCounter % tick_delay != 0) return;

        var localPlayer = Minecraft.getInstance().player;
        var level = Minecraft.getInstance().level;
        if (localPlayer == null || level == null) return;

        if (DynamicLighting.getEnabled()) {
            for (Player player : level.players()) {
                var mainStack = player.getMainHandItem();
                ResourceLocation mhi = getCachedItemKey(mainStack.getItem());
                Integer mhr = (mhi != null) ? LestoraDLMod.getMinLightLevel.apply(mhi, false) : null;

                var offStack = player.getOffhandItem();
                ResourceLocation ohi = getCachedItemKey(offStack.getItem());
                Integer ohr = (ohi != null) ? LestoraDLMod.getMinLightLevel.apply(ohi, false) : null;

                ResourceLocation resourceLocation = null;
                if (mhr != null && ohr != null)  resourceLocation = (mhr > ohr) ? mhi : ohi;
                 else if (mhr != null) resourceLocation = mhi;
                 else if (ohr != null) resourceLocation = ohi;

                UUID uuid = player.getUUID();
                ResourceLocation previous = previousTorchState.get(uuid);
                boolean changed = (previous == null && resourceLocation != null) || (previous != null && !previous.equals(resourceLocation));

                if (changed) {
                    if (resourceLocation != null) {
                        previousTorchState.put(uuid, resourceLocation);
                        DynamicLighting.tryAddEntity(player, resourceLocation);
                    } else {
                        previousTorchState.remove(uuid);
                        DynamicLighting.tryRemoveEntity(player);
                    }
                }
            }
            DynamicLighting.tryUpdateEntityPositions();
        }


//        if (!initialDelayDone) {
//            clientTicks++;
//            if (clientTicks < 100) return;
//            initialDelayDone = true;
//        }


        if (DynamicBlockLighting.getEnabled() && LestoraDLMod.configLoaded) {
            if (tickCounter % 100 == 0){
                RealConfigHandler.updateBlockMap();
                DLEvents.previousChunks.clear();
                LightingUpdateManager.pendingSubChunkScans.clear();
            }
            LightingUpdateManager.processPendingScans(localPlayer);
            // Cache player position
            var playerPos = localPlayer.blockPosition();
            int playerChunkX = playerPos.getX() >> 4;
            int playerChunkZ = playerPos.getZ() >> 4;
            int playerY = playerPos.getY();

            // Retrieve the player's render distance (in chunks) and compute squared distance
            int renderDistance = 5; //Minecraft.getInstance().options.renderDistance().get();
            int renderDistanceSq = renderDistance * renderDistance;

            // Determine new chunks based on chunk_distance.
            Set<ChunkPos> currentChunks = new HashSet<>();
            for (int dx = -renderDistance; dx <= renderDistance; dx++) {
                for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                    if ((dx * dx + dz * dz) <= renderDistanceSq) {
                        currentChunks.add(new ChunkPos(playerChunkX + dx, playerChunkZ + dz));
                    }
                }
            }

            // Remove blocks for chunks that are beyond the player's render distance.
            for (ChunkPos oldChunk : previousChunks) {
                int dx = oldChunk.x - playerChunkX;
                int dz = oldChunk.z - playerChunkZ;
                if ((dx * dx + dz * dz) > renderDistanceSq) {
                    removeBlocksInChunk(oldChunk.x, oldChunk.z);
                }
            }

            var yChange = false;
            if (playerY - previousPlayerY > 16) {
                yChange = true;
                previousPlayerY = playerY;
            }
            // Queue up new chunks that weren't previously scanned.
            for (ChunkPos cp : currentChunks) {
                if (yChange || !previousChunks.contains(cp)) {
                    LightingUpdateManager.queueChunkScan(cp, playerY);
                }
            }

            previousChunks = currentChunks;
        }
    }
    private static int previousPlayerY = Integer.MIN_VALUE;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            Minecraft.getInstance().execute(() -> {
                var resourceLocation = ForgeRegistries.ITEMS.getKey(itemEntity.getItem().getItem());
                var lightLevel = LestoraDLMod.getMinLightLevel.apply(resourceLocation, false);
                if (lightLevel != null) {
                    DynamicLighting.tryAddEntity(itemEntity, resourceLocation);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) return;
        DynamicLighting.tryRemoveEntity(event.getEntity());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide() || !LestoraDLMod.configLoaded) return;
        ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock());
        Integer lightLevel = (rl != null) ? LestoraDLMod.getMaxLightLevel.apply(rl, true) : null;
        if (lightLevel != null) {
            // Add block to dynamic lighting.
            DynamicBlockLighting.tryAddBlock(event.getPos(), rl, lightLevel);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide() || !LestoraDLMod.configLoaded) return;
        // Remove block from dynamic lighting.
        DynamicBlockLighting.tryRemoveBlock(event.getPos());
        // Immediately remove it from the cache.
        DynamicBlockLighting.removeBlockCache(event.getPos());
    }

    // Helper method to remove blocks in a given chunk
    private static void removeBlocksInChunk(int chunkX, int chunkZ) {
        int startX = chunkX << 4, endX = startX + 15;
        int startZ = chunkZ << 4, endZ = startZ + 15;
        for (BlockPos pos : DynamicBlockLighting.getRegisteredBlockPositions()) {
            if (pos.getX() >= startX && pos.getX() <= endX && pos.getZ() >= startZ && pos.getZ() <= endZ) {
                DynamicBlockLighting.tryRemoveBlock(pos);
            }
        }
    }

    public static void scanChunk(int chunkX, int chunkZ, int startY) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        int startX = chunkX << 4, startZ = chunkZ << 4;

        try {
            for (int x = startX; x < startX + 16; x++) {
                for (int z = startZ; z < startZ + 16; z++) {
                    for (int y = startY; y <= startY + 16; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        var state = level.getBlockState(pos);
                        ResourceLocation rl = getCachedBlockKey(state.getBlock());
                        var lightLevel = LestoraDLMod.getMaxLightLevel.apply(rl, true);
                        if (rl != null && lightLevel != null) {
                            DynamicBlockLighting.tryAddBlock(pos, rl, lightLevel);
                        }
                    }
                }
            }
        } catch (Exception ignored) { }
    }
}