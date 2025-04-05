package com.lestora.dynamiclighting.models;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record PendingCheck(BlockPos pos, ResourceKey<Level> dimension) {}