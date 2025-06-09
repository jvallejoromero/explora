package com.jvallejoromero.explora.util;

public class RegionCoord {
    private final int regionX;
    private final int regionZ;

    public RegionCoord(int chunkX, int chunkZ) {
        this.regionX = chunkX >> 5; // 1 region = 32 chunks
        this.regionZ = chunkZ >> 5;
    }

    public int getX() { return regionX; }
    public int getZ() { return regionZ; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RegionCoord)) return false;
        RegionCoord other = (RegionCoord) o;
        return this.regionX == other.regionX && this.regionZ == other.regionZ;
    }

    @Override
    public int hashCode() {
        return 31 * regionX + regionZ;
    }

    @Override
    public String toString() {
        return "r." + regionX + "." + regionZ;
    }
}

