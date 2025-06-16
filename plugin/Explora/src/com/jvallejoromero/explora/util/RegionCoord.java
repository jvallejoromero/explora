package com.jvallejoromero.explora.util;

/**
 * Represents a region's coordinates in Minecraft, derived from chunk coordinates.
 *
 * <p>A region consists of a 32x32 chunk area, so converting chunk coordinates to region coordinates
 * involves right-shifting by 5 (i.e., {@code chunkX >> 5}).
 *
 * @see ChunkCoord
 */
public class RegionCoord {
    private int regionX;
    private int regionZ;

    /**
     * Constructs a {@code RegionCoord} from chunk coordinates.
     *
     * @param chunkX the chunk's X coordinate
     * @param chunkZ the chunk's Z coordinate
     */
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
    
    /**
     * Creates a {@code RegionCoord} directly from region coordinates instead of chunk coordinates.
     *
     * @param regionX the region X coordinate
     * @param regionZ the region Z coordinate
     * @return a new {@code RegionCoord} representing the given region position
     */
    public static RegionCoord fromRegionCoords(int regionX, int regionZ) {
        RegionCoord coord = new RegionCoord(0, 0);
        coord.regionX = regionX;
        coord.regionZ = regionZ;
        return coord;
    }
}

