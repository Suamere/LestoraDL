package com.lestora.dynamiclighting.commands;

import com.lestora.dynamiclighting.LestoraDLMod;
import com.lestora.dynamiclighting.events.DLEvents;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DLCommands {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        var root = Commands.literal("lestora");

        registerFixNearby(root);
        registerEfficiency(root);
        registerBlocksEnabled(root);

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

    private static void registerBlocksEnabled(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("dynamicLighting")
                .then(Commands.literal("blocksEnabled")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean value = BoolArgumentType.getBool(ctx, "enabled");
                                    DLEvents.enableBlocks(value);
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
                            if (player == null) return 0;

                            BlockPos playerPos = player.blockPosition();
                            int range = 20;
                            int sideLength = range * 2 + 1;
                            int size = sideLength * sideLength * sideLength;
                            BlockPos[] positions = new BlockPos[size];
                            int index = 0;

                            for (int dx = -range; dx <= range; dx++)
                                for (int dy = -range; dy <= range; dy++)
                                    for (int dz = -range; dz <= range; dz++)
                                        positions[index++] = playerPos.offset(dx, dy, dz);

                            LestoraDLMod.checkBlockRemoval((ClientLevel) player.level(), positions);
                            return 1;
                        })
                )
        );
    }
}