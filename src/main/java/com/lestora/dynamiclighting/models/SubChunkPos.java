package com.lestora.dynamiclighting.models;

import net.minecraft.world.level.ChunkPos;

public class SubChunkPos {
    public final ChunkPos chunkPos;
    public final int startingY;

    public SubChunkPos(ChunkPos chunkPos, int startingY) {
        this.chunkPos = chunkPos;
        this.startingY = startingY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubChunkPos)) return false;
        SubChunkPos that = (SubChunkPos) o;
        return startingY == that.startingY && chunkPos.equals(that.chunkPos);
    }

    @Override
    public int hashCode() {
        int result = chunkPos.hashCode();
        result = 31 * result + startingY;
        return result;
    }

    @Override
    public String toString() {
        return "SubChunkPos{" +
                "chunkPos=" + chunkPos +
                ", startingY=" + startingY +
                '}';
    }
}
