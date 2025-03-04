package com.lestora.dynamiclighting.events;

import com.lestora.dynamiclighting.DynamicBlockLighting;
import com.lestora.dynamiclighting.DynamicLighting;
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class DLEvents {
    private static final Map<Item, ResourceLocation> itemKeyCache = new ConcurrentHashMap<>();
    private static final Map<UUID, ResourceLocation> previousTorchState = new ConcurrentHashMap<>();
    private static int tick_delay = 5;
    private static int chunk_distance = 0;
    private static int tickCounter = 0;

    public static void setTickDelay(int delay) {
        tick_delay = delay;
        DynamicLighting.setEnabled(delay > 0);
    }


    public static void setChunk_distance(int distance) {
        chunk_distance = distance;
        DynamicBlockLighting.setEnabled(distance > 0);
    }

    public static ResourceLocation getCachedItemKey(Item item) {
        return itemKeyCache.computeIfAbsent(item, ForgeRegistries.ITEMS::getKey);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (++tickCounter % tick_delay != 0) return;

        var level = Minecraft.getInstance().level;
        if (level == null) return;

        if (DynamicLighting.getEnabled()) {
            for (Player player : level.players()) {
                var mainStack = player.getMainHandItem();
                ResourceLocation mhi = getCachedItemKey(mainStack.getItem());
                Integer mhr = (mhi != null) ? ConfigEvents.getLightLevel(mhi) : null;

                var offStack = player.getOffhandItem();
                ResourceLocation ohi = getCachedItemKey(offStack.getItem());
                Integer ohr = (ohi != null) ? ConfigEvents.getLightLevel(ohi) : null;

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
    }


    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            Minecraft.getInstance().execute(() -> {
                var resourceLocation = ForgeRegistries.ITEMS.getKey(itemEntity.getItem().getItem());
                var lightLevel = ConfigEvents.getLightLevel(resourceLocation);
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
        if (event.getLevel().isClientSide()) return;
        ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock());
        Integer lightLevel = (rl != null) ? ConfigEvents.getLightLevel(rl) : null;
        if (lightLevel != null)
            DynamicBlockLighting.tryAddBlock(event.getPos(), rl);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        DynamicBlockLighting.tryRemoveBlock(event.getPos());
    }

    private static final Map<Block, ResourceLocation> blockKeyCache = new ConcurrentHashMap<>();
    public static ResourceLocation getCachedBlockKey(Block block) {
        return blockKeyCache.computeIfAbsent(block, ForgeRegistries.BLOCKS::getKey);
    }

    private static Set<ChunkPos> previousChunks = new HashSet<>();
    private static int lastPlayerChunkX = Integer.MIN_VALUE;
    private static int lastPlayerChunkZ = Integer.MIN_VALUE;
    private static int lastPlayerY = Integer.MIN_VALUE;
    private static int clientTicks = 0;
    private static boolean initialDelayDone = false;

    @SubscribeEvent
    public static void onClientTickBlocks(TickEvent.ClientTickEvent event) {
        if (!initialDelayDone) {
            clientTicks++;
            if (clientTicks < 100) return;
            initialDelayDone = true;
        }
        var player = Minecraft.getInstance().player;
        var level = Minecraft.getInstance().level;
        if (player == null || level == null) return;
        int playerChunkX = player.blockPosition().getX() >> 4;
        int playerChunkZ = player.blockPosition().getZ() >> 4;

        // Only update if the player's chunk has changed
        if (playerChunkX == lastPlayerChunkX && playerChunkZ == lastPlayerChunkZ &&
                Math.abs(player.blockPosition().getY() - lastPlayerY) < 3) return;
        lastPlayerChunkX = playerChunkX;
        lastPlayerChunkZ = playerChunkZ;
        lastPlayerY = player.blockPosition().getY();

        // Build the complete set of chunks in a circular radius
        Set<ChunkPos> newChunks = new HashSet<>();
        for (int dx = -chunk_distance; dx <= chunk_distance; dx++) {
            for (int dz = -chunk_distance; dz <= chunk_distance; dz++) {
                if (Math.sqrt(dx * dx + dz * dz) <= chunk_distance) {
                    newChunks.add(new ChunkPos(playerChunkX + dx, playerChunkZ + dz));
                }
            }
        }

        // Remove blocks for any chunk that was in the previous set but is no longer in the new set.
        for (ChunkPos oldChunk : previousChunks) {
            if (!newChunks.contains(oldChunk)) {
                removeBlocksInChunk(oldChunk.x, oldChunk.z);
            }
        }

        // Scan every chunk in the new set (this includes inner chunks).
        for (ChunkPos cp : newChunks) {
            scanChunk(cp.x, cp.z, player.blockPosition().getY());
        }

        previousChunks = newChunks;
    }

    // Helper method to remove blocks in a given chunk
    private static void removeBlocksInChunk(int chunkX, int chunkZ) {
        int startX = chunkX << 4, endX = startX + 15;
        int startZ = chunkZ << 4, endZ = startZ + 15;
        // Assumes DynamicBlockLighting provides a getter for its registered block positions.
        for (BlockPos pos : DynamicBlockLighting.getRegisteredBlockPositions()) {
            if (pos.getX() >= startX && pos.getX() <= endX &&
                    pos.getZ() >= startZ && pos.getZ() <= endZ) {
                DynamicBlockLighting.tryRemoveBlock(pos);
            }
        }
    }

    private static void scanChunk(int chunkX, int chunkZ, int playerY) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        int startX = chunkX << 4, startZ = chunkZ << 4;
        int minY = playerY - 16, maxY = playerY + 16;
        for (int x = startX; x < startX + 16; x++)
            for (int z = startZ; z < startZ + 16; z++)
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    var state = level.getBlockState(pos);
                    ResourceLocation rl = getCachedBlockKey(state.getBlock());
                    if (rl != null && ConfigEvents.getLightLevel(rl) != null)
                        DynamicBlockLighting.tryAddBlock(pos, rl);
                }
    }
}