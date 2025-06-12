package com.jvallejoromero.explora.util;

import java.util.Objects;

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
