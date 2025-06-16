package com.jvallejoromero.explora.util;

import java.util.Objects;

/**
 * Represents the 2D coordinates of a Minecraft chunk (x, z).
 *
 * <p>This utility class is used to uniquely identify a chunk within a world.
 * It implements {@code equals} and {@code hashCode} to support usage in hash-based collections
 * such as {@link java.util.HashMap} or {@link java.util.HashSet}.
 *
 * <p>Each chunk spans a 16x16 area in the XZ plane. The Y (vertical) axis is not tracked here.
 *
 */
public class ChunkCoord {
    public int x;
    public int z;

    public ChunkCoord(int x, int z) {
        this.x = x;
        this.z = z;
    }
    
    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkCoord)) return false;
        ChunkCoord other = (ChunkCoord) o;
        return x == other.x && z == other.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public String toString() {
        return x + "," + z;
    }
}