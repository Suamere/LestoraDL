package com.lestora.dynamiclighting.events;

import com.lestora.dynamiclighting.DynamicBlockLighting;
import com.lestora.dynamiclighting.DynamicLighting;
import com.lestora.dynamiclighting.LestoraDLMod;
import com.lestora.dynamiclighting.LightingUpdateManager;
import com.lestora.dynamiclighting.models.SubChunkPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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
    private static final Map<FallingBlockEntity, BlockPos> fallingBlockCache = new ConcurrentHashMap<>();
    private static int tick_delay = 5;
    private static int tickCounter = 0;

    public static Set<ChunkPos> previousChunks = new HashSet<>();

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
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        var level = Minecraft.getInstance().level;
        var localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null || level == null) return;
        LightingUpdateManager.processPendingScans(localPlayer);

        if (DynamicLighting.getEnabled()) {
            TickDynamicEntities(level);
        }

        if (DynamicBlockLighting.getEnabled() && LestoraDLMod.configLoaded) {
            TickDynamicBlocks(localPlayer);
        }
    }

    @SubscribeEvent
    public static void onClientTick2(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!LestoraDLMod.configLoaded) return;

        for (Iterator<Map.Entry<FallingBlockEntity, BlockPos>> it = fallingBlockCache.entrySet().iterator(); it.hasNext();) {
            Map.Entry<FallingBlockEntity, BlockPos> entry = it.next();
            FallingBlockEntity fallingBlock = entry.getKey();

            BlockPos currentPos = fallingBlock.blockPosition();

            if (fallingBlock.isRemoved() || !fallingBlock.isAlive()) {
                ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(fallingBlock.getBlockState().getBlock());

                Integer lightLevel = (rl != null) ? LestoraDLMod.getEntityLightLevel.apply(rl) : null;
                if (lightLevel != null)
                    DynamicBlockLighting.tryAddBlock(currentPos, rl, lightLevel);

                it.remove();
            } else {
                // Entity still falling, update the cached position.
                entry.setValue(currentPos);
            }
        }
    }

    private static void TickDynamicEntities(ClientLevel level) {
        if (tickCounter % tick_delay != 0) return;
        for (Player player : level.players()) {
            var mainStack = player.getMainHandItem();
            ResourceLocation mhi = getCachedItemKey(mainStack.getItem());
            Integer mhr = (mhi != null) ? LestoraDLMod.getEntityLightLevel.apply(mhi) : null;

            var offStack = player.getOffhandItem();
            ResourceLocation ohi = getCachedItemKey(offStack.getItem());
            Integer ohr = (ohi != null) ? LestoraDLMod.getEntityLightLevel.apply(ohi) : null;

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

    public static void resetBlockChunkScans() {
        for (ChunkPos oldChunk : previousChunks) {
            removeBlocksInChunk(oldChunk.x, oldChunk.z);
        }
        previousChunks.clear();
        LightingUpdateManager.pendingSubChunkScans.clear();
    }

    private static int lastPlayerChunkX = Integer.MIN_VALUE;
    private static int lastPlayerChunkZ = Integer.MIN_VALUE;
    private static void TickDynamicBlocks(LocalPlayer localPlayer) {
        if (tickCounter % 20 != 0) return;

        var playerPos = localPlayer.blockPosition();
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;

        // Early return if the player's chunk hasn't changed.
        if (playerChunkX == lastPlayerChunkX && playerChunkZ == lastPlayerChunkZ) {
            return;
        }

        // Update last known chunk coordinates.
        lastPlayerChunkX = playerChunkX;
        lastPlayerChunkZ = playerChunkZ;
        int playerY = playerPos.getY();

        var yChange = false;
        if (Math.abs(playerY - previousPlayerY) > 8) {
            yChange = true;
            previousPlayerY = playerY;
        }

        // Retrieve the player's render distance (in chunks) and compute squared distance
        int renderDistance = Minecraft.getInstance().options.renderDistance().get();
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

        // Queue up new chunks that weren't previously scanned.
        for (ChunkPos cp : currentChunks) {
            if (yChange || !previousChunks.contains(cp)) {
                LightingUpdateManager.queueChunkScan(cp, playerY);
            }
        }

        previousChunks = currentChunks;
    }

    private static int previousPlayerY = Integer.MIN_VALUE;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!LestoraDLMod.configLoaded) return;
        if (!event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof ItemEntity itemEntity) {
            Minecraft.getInstance().execute(() -> {
                var resourceLocation = ForgeRegistries.ITEMS.getKey(itemEntity.getItem().getItem());
                var lightLevel = LestoraDLMod.getEntityLightLevel.apply(resourceLocation);
                if (lightLevel != null) {
                    DynamicLighting.tryAddEntity(itemEntity, resourceLocation);
                }
            });
        }
        if (event.getEntity() instanceof FallingBlockEntity fallingBlock) {
            Minecraft.getInstance().execute(() -> {
                ResourceLocation resourceLocation = ForgeRegistries.BLOCKS.getKey(fallingBlock.getBlockState().getBlock());
                var lightLevel = LestoraDLMod.getEntityLightLevel.apply(resourceLocation);
                if (lightLevel != null) {
                    DynamicLighting.tryAddEntity(fallingBlock, resourceLocation);
                }
            });
            BlockPos originalPos = fallingBlock.blockPosition();
            DynamicBlockLighting.tryRemoveBlock(originalPos);

            fallingBlockCache.put(fallingBlock, originalPos);
        }
    }


    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!LestoraDLMod.configLoaded) return;
        if (!event.getLevel().isClientSide()) return;
        DynamicLighting.tryRemoveEntity(event.getEntity());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        if (!LestoraDLMod.configLoaded) return;
        ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock());
        Integer lightLevel = (rl != null) ? LestoraDLMod.getBlockLightLevel.apply(rl) : null;
        if (lightLevel != null) {
            DynamicBlockLighting.tryAddBlock(event.getPos(), rl, lightLevel);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        if (!LestoraDLMod.configLoaded) return;
        BlockPos pos = event.getPos();
        DynamicBlockLighting.tryRemoveBlock(pos);
        var level = event.getLevel();
        var state = level.getBlockState(pos);

        // Handle Doors
        if (state.getBlock() instanceof DoorBlock) {
            boolean isLower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
            BlockPos otherHalfPos = isLower ? pos.above() : pos.below();
            DynamicBlockLighting.tryRemoveBlock(otherHalfPos);
        }
        // Handle Beds
        else if (state.getBlock() instanceof BedBlock) {
            // BedBlock has a PART property (HEAD/FOOT)
            boolean isHead = state.getValue(BedBlock.PART) == BedPart.HEAD;
            BlockPos otherHalfPos = isHead ? pos.below() : pos.above();
            DynamicBlockLighting.tryRemoveBlock(otherHalfPos);
        }
        // Handle Double-Tall Plants (Sunflower, Lilac, Large Fern, Rose Bush, Peony, Tall-Grass)
        else if (state.getBlock() instanceof DoublePlantBlock) {
            boolean isLower = state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER;
            BlockPos otherHalfPos = isLower ? pos.above() : pos.below();
            DynamicBlockLighting.tryRemoveBlock(otherHalfPos);
        }
        // Handle Extended Pistons
        else if (state.getBlock() instanceof PistonHeadBlock) {
            // Remove the piston base (behind the head)
            Direction facing = state.getValue(PistonHeadBlock.FACING);
            BlockPos pistonBasePos = pos.relative(facing.getOpposite());
            DynamicBlockLighting.tryRemoveBlock(pistonBasePos);
        }
        else if (state.getBlock() instanceof PistonBaseBlock) {
            // If the piston is extended, remove its head.
            if (state.getValue(PistonBaseBlock.EXTENDED)) {
                Direction facing = state.getValue(PistonBaseBlock.FACING);
                BlockPos headPos = pos.relative(facing);
                DynamicBlockLighting.tryRemoveBlock(headPos);
            }
        }
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

    static int scanCount = 0;
    static Map<SubChunkPos, Integer> sky = new HashMap<>();
    public static void scanChunk(SubChunkPos scp) {
        var existing = sky.get(scp);
        if (existing != null && existing >= scp.startingY) return;

        var chunkX = scp.chunkPos.x;
        var chunkZ = scp.chunkPos.z;
        var startY = scp.startingY;

        var level = Minecraft.getInstance().level;
        if (level == null) return;
        int startX = chunkX << 4, startZ = chunkZ << 4;

        try {
            for (int y = startY; y <= startY + 16; y++) {
                var allAir = true;
                for (int x = startX; x < startX + 16; x++) {
                    for (int z = startZ; z < startZ + 16; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        var state = level.getBlockState(pos);
                        if (state.isAir()) continue;
                        allAir = false;
                        ResourceLocation rl = getCachedBlockKey(state.getBlock());
                        var lightLevel = LestoraDLMod.getBlockLightLevel.apply(rl);
                        if (rl != null && lightLevel != null) {
                            DynamicBlockLighting.tryAddBlock(pos, rl, lightLevel);
                        }
                    }
                }
                if (allAir){
                    sky.put(scp, scp.startingY);
                    break;
                }
            }
            scanCount++;
        } catch (Exception ignored) { }
    }
}