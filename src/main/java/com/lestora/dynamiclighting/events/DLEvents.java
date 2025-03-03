package com.lestora.dynamiclighting.events;

import com.lestora.dynamiclighting.DynamicLighting;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class DLEvents {
    private static final Map<Item, ResourceLocation> itemKeyCache = new ConcurrentHashMap<>();
    private static final Map<UUID, ResourceLocation> previousTorchState = new ConcurrentHashMap<>();
    private static int tick_delay = 5;
    private static int tickCounter = 0;

    public static void setTickDelay(int delay) {
        tick_delay = delay;
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
                    System.out.println("Block Position: " + itemEntity.blockPosition());
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
}