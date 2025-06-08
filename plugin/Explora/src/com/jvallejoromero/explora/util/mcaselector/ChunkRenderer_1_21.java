package com.jvallejoromero.explora.util.mcaselector;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import net.querz.mcaselector.version.ChunkRenderer;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.nbt.ByteArrayTag;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.LongArrayTag;
import net.querz.nbt.Tag;

@MCVersionImplementation(4325) // 1.21.5 data version
public class ChunkRenderer_1_21 implements ChunkRenderer<CompoundTag, String> {

	private static final CompoundTag waterDummy = new CompoundTag();

	static {
		waterDummy.putString("Name", "minecraft:water");
	}

	@Override
	public void drawChunk(CompoundTag root, ColorMapping<CompoundTag, String> colorMapping, int x, int z, int scale,
			int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean water, int height) {
		ListTag sections = Helper.tagFromCompound(root, "sections");
		if (sections == null || sections.getElementType() != Tag.Type.COMPOUND) return;

		int yMin = Helper.intFromCompound(root, "yPos", -4);
		int scaleBits = 31 - Integer.numberOfLeadingZeros(scale);
		int absHeight = height - yMin * 16;
		int yMax = 1 + (height >> 4);
		int sMax = yMax - yMin;

		CompoundTag[] indexedSections = new CompoundTag[sMax];
		for (CompoundTag s : sections.iterateType(CompoundTag.class)) {
			int y = Helper.numberFromCompound(s, "Y", yMin - 1).intValue();
			if (y >= yMin && y < yMax) {
				indexedSections[y - yMin] = s;
			}
		}

		for (int cx = 0; cx < 16; cx += scale) {
			zLoop:
			for (int cz = 0; cz < 16; cz += scale) {
				int pixelIndex = (z + (cz >> scaleBits)) * (512 >> scaleBits) + (x + (cx >> scaleBits));
				boolean waterDepth = false;

				for (int i = sMax - (sMax - (absHeight >> 4)); i >= 0; i--) {
					CompoundTag section = indexedSections[i];
					if (section == null) continue;

					ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(section, "block_states"), "palette");
					Tag dataTag = Helper.tagFromCompound(Helper.tagFromCompound(section, "block_states"), "data");
					LongBuffer blockStates = getLongBufferFromTag(dataTag);

					ListTag biomePalette = Helper.tagFromCompound(Helper.tagFromCompound(section, "biomes"), "palette");
					Tag biomeTag = Helper.tagFromCompound(Helper.tagFromCompound(section, "biomes"), "data");
					LongBuffer biomes = getLongBufferFromTag(biomeTag);

					int paletteSize = palette == null ? 0 : palette.size();
					int biomePaletteSize = biomePalette == null ? 0 : biomePalette.size();

					int bits = paletteSize > 1 ? Math.max(4, 32 - Integer.numberOfLeadingZeros(paletteSize - 1)) : 0;
					int cleanBits = (1 << bits) - 1;
					int indexesPerLong = bits == 0 ? 1 : 64 / bits;

					int biomeBits = biomePaletteSize > 1 ? Math.max(1, 32 - Integer.numberOfLeadingZeros(biomePaletteSize - 1)) : 0;
					int biomeCleanBits = (1 << biomeBits) - 1;
					int biomeIndexesPerLong = biomeBits == 0 ? 1 : 64 / biomeBits;

					int sectionHeight = (i + yMin) * 16;
					int startHeight = absHeight >> 4 == i ? absHeight & 0xF : 15;

					for (int cy = startHeight; cy >= 0; cy--) {
						CompoundTag blockData = getBlock(cx, cy, cz, blockStates, bits, cleanBits, indexesPerLong, palette);
						if (blockData == null || colorMapping.isTransparent(blockData)) continue;

						String biome = getBiome(cx, cy, cz, biomes, biomeBits, biomeCleanBits, biomeIndexesPerLong, biomePalette);

						if (water) {
							if (!waterDepth) {
								pixelBuffer[pixelIndex] = colorMapping.getRGB(blockData, biome);
								waterHeights[pixelIndex] = (short) (sectionHeight + cy);
							}
							if (colorMapping.isWater(blockData)) {
								waterDepth = true;
								continue;
							} else if (colorMapping.isWaterlogged(blockData)) {
								pixelBuffer[pixelIndex] = colorMapping.getRGB(waterDummy, biome);
								waterPixels[pixelIndex] = colorMapping.getRGB(blockData, biome);
								waterHeights[pixelIndex] = (short) (sectionHeight + cy);
								terrainHeights[pixelIndex] = (short) (sectionHeight + cy - 1);
								continue zLoop;
							} else {
								waterPixels[pixelIndex] = colorMapping.getRGB(blockData, biome);
							}
						} else {
							pixelBuffer[pixelIndex] = colorMapping.getRGB(blockData, biome);
						}
						terrainHeights[pixelIndex] = (short) (sectionHeight + cy);
						continue zLoop;
					}
				}
			}
		}
	}
	
	public BufferedImage drawAndZoomChunk(CompoundTag root, ColorMapping<CompoundTag, String> colorMapping, int x, int z, int scale, boolean water, int height, int zoomFactor) {
	    int size = 512 / scale;
	    int[] pixelBuffer = new int[size * size];
	    int[] waterPixels = new int[size * size];
	    short[] terrainHeights = new short[size * size];
	    short[] waterHeights = new short[size * size];

	    // Use existing drawChunk logic
	    drawChunk(root, colorMapping, x, z, scale, pixelBuffer, waterPixels, terrainHeights, waterHeights, water, height);

	    // Generate zoomed image
	    return generateZoomedImage(pixelBuffer, zoomFactor, size, size);
	}


	@Override
	public void drawLayer(CompoundTag root, ColorMapping<CompoundTag, String> colorMapping, int x, int z, int scale, int[] pixelBuffer, int height) {}

	@Override
	public void drawCaves(CompoundTag root, ColorMapping<CompoundTag, String> colorMapping, int x, int z, int scale, int[] pixelBuffer, short[] terrainHeights, int height) {}

	@Override
	public CompoundTag minimizeChunk(CompoundTag root) {
		CompoundTag minData = new CompoundTag();
		minData.put("DataVersion", root.get("DataVersion").copy());
		minData.put("sections", root.get("sections").copy());
		minData.put("Status", root.get("Status").copy());
		return minData;
	}

	private CompoundTag getBlock(int x, int y, int z, LongBuffer blockStates, int bits, int clean, int indexesPerLong, ListTag palette) {
		if (blockStates == null || palette == null || palette.isEmpty()) return null;
		if (bits == 0) return palette.getCompound(0);
		int index = y * 256 + z * 16 + x;
		int longIndex = index / indexesPerLong;
		int bitIndex = (index % indexesPerLong) * bits;
		long value = blockStates.get(longIndex) >> bitIndex;
		int paletteIndex = (int) value & clean;
		if (paletteIndex >= palette.size()) return palette.getCompound(0);
		return palette.getCompound(paletteIndex);
	}

	private String getBiome(int x, int y, int z, LongBuffer biomes, int bits, int clean, int indexesPerLong, ListTag palette) {
		if (biomes == null || palette == null || palette.isEmpty()) return "";
		if (bits == 0) return palette.getString(0);
		int index = (y >> 2 & 0xF) * 16 + (z >> 2 & 0xF) * 4 + (x >> 2 & 0xF);
		int longIndex = index / indexesPerLong;
		int bitIndex = (index % indexesPerLong) * bits;
		long value = biomes.get(longIndex) >> bitIndex;
		int paletteIndex = (int) value & clean;
		if (paletteIndex >= palette.size()) return "";
		return palette.getString(paletteIndex);
	}

	private LongBuffer getLongBufferFromTag(Tag tag) {
		if (tag instanceof ByteArrayTag byteTag) {
			return ByteBuffer.wrap(byteTag.getValue()).asLongBuffer();
		} else if (tag instanceof LongArrayTag longTag) {
			return LongBuffer.wrap(longTag.getValue());
		}
		return null;
	}
	
	public static BufferedImage generateZoomedImage(int[] pixelBuffer, int scaleFactor, int width, int height) {
		BufferedImage zoomed = new BufferedImage(width * scaleFactor, height * scaleFactor, BufferedImage.TYPE_INT_ARGB);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int rgb = pixelBuffer[y * width + x];
				for (int dy = 0; dy < scaleFactor; dy++) {
					for (int dx = 0; dx < scaleFactor; dx++) {
						zoomed.setRGB(x * scaleFactor + dx, y * scaleFactor + dy, rgb);
					}
				}
			}
		}

		return zoomed;
	}

}
