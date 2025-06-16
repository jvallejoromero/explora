package com.jvallejoromero.explora.util;

import java.util.Objects;


/**
 * Represents the coordinates of a single block in a 3D Minecraft world (x, y, z).
 *
 * <p>This utility class is used to encapsulate block-level positions and provides
 * helper methods for equality, hashing, and conversion to chunk-level coordinates.
 *
 */
public class BlockCoord {
    public int x;
    public int y;
    public int z;

    public BlockCoord(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
    	return y;
    }

    public int getZ() {
        return z;
    }
    
    /**
     * Converts this block coordinate to a chunk coordinate.
     *
     * <p>Only the X and Z values are used; Y is discarded since chunks
     * dont need it in Minecraft
     *
     * @return a new {@link ChunkCoord} representing the chunk containing this block
     */
    public ChunkCoord toChunkCoord() {
    	return new ChunkCoord(x >> 4, z >> 4);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockCoord)) return false;
        BlockCoord other = (BlockCoord) o;
        return x == other.x && y == other.y && z == other.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }
}
