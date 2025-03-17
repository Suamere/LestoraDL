package com.lestora.dynamiclighting.commands;

import com.lestora.dynamiclighting.events.DLEvents;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DLCommands {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        var root = Commands.literal("lestora");

        registerFixNearby(root);
        registerEfficiency(root);
        registerChunkDistance(root);

        event.getDispatcher().register(root);
    }

    private static void registerEfficiency(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("dynamicLighting")
            .then(Commands.literal("efficiency")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> {
                            int value = IntegerArgumentType.getInteger(ctx, "value");
                            DLEvents.setTickDelay(value);
                            return 1;
                        })
                )
        ));
    }

    private static void registerChunkDistance(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("dynamicLighting")
                .then(Commands.literal("chunkDistance")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 10))
                                .executes(ctx -> {
                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                    DLEvents.setChunk_distance(value);
                                    return 1;
                                })
                        )
                ));
    }

    private static void registerFixNearby(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("dynamicLighting")
            .then(Commands.literal("fixNearby")
                .executes(ctx -> {
                    var player = Minecraft.getInstance().player;
                    var level = player.level();
                    var chunkSource = level.getChunkSource();
                    LevelLightEngine lightingEngine = chunkSource.getLightEngine();

                    BlockPos playerPos = player.blockPosition();
                    for (int dx = -20; dx <= 20; dx++) {
                        for (int dy = -20; dy <= 20; dy++) {
                            for (int dz = -20; dz <= 20; dz++) {
                                BlockPos pos = playerPos.offset(dx, dy, dz);
                                lightingEngine.checkBlock(pos);
                            }
                        }
                    }

                    return 1;
                })
        ));
    }
}