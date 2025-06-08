package com.jvallejoromero.explora.util.mcaselector;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.imageio.ImageIO;

import com.jvallejoromero.explora.ExploraPlugin;

import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.mca.Chunk;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.ui.Color;
import net.querz.mcaselector.util.math.MathUtil;

public final class HeadlessTileImage {
	
	private static final int[] corruptedChunkOverlay = new int[256];

	static {
		try {
			File corruptedFile = Paths.get("plugins/Explora/img/corrupted.png").toFile();
			BufferedImage corrupted =  ImageIO.read(corruptedFile);
			corrupted.getRGB(0, 0, 16, 16, corruptedChunkOverlay, 0, 16);
		} catch (IOException e) {
			ExploraPlugin.warn("Failed to load corrupted chunk overlay: " + e.getMessage());
		}
	}

	private HeadlessTileImage() {}

	public static BufferedImage generateBufferedImage(RegionMCAFile mcaFile, int scale) {
		int size = Tile.SIZE / scale;
		int chunkSize = Tile.CHUNK_SIZE / scale;
		int pixels = Tile.PIXELS / (scale * scale);

		try {
			int[] pixelBuffer = new int[pixels];
			int[] waterPixels = ConfigProvider.WORLD.getShade() && ConfigProvider.WORLD.getShadeWater() && !ConfigProvider.WORLD.getRenderCaves() ? new int[pixels] : null;
			short[] terrainHeights = new short[pixels];
			short[] waterHeights = ConfigProvider.WORLD.getShade() && ConfigProvider.WORLD.getShadeWater() && !ConfigProvider.WORLD.getRenderCaves() ? new short[pixels] : null;
			
			for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
				for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
					int index = cz * Tile.SIZE_IN_CHUNKS + cx;
					Chunk data = mcaFile.getChunk(index);
					if (data == null) continue;
					
					ExploraPlugin.log("Drawing chunk at index: " + index + " -> " + data.getAbsoluteLocation());
					drawChunkImage(data, cx * chunkSize, cz * chunkSize, scale, pixelBuffer, waterPixels, terrainHeights, waterHeights);
				}
			}

			if (ConfigProvider.WORLD.getRenderCaves()) {
				flatShade(pixelBuffer, terrainHeights, scale);
			} else if (ConfigProvider.WORLD.getShade() && !ConfigProvider.WORLD.getRenderLayerOnly()) {
				shade(pixelBuffer, waterPixels, terrainHeights, waterHeights, scale);
			}

			BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			img.setRGB(0, 0, size, size, pixelBuffer, 0, size);
			
			boolean drewAnything = Arrays.stream(pixelBuffer).anyMatch(p -> p != 0);
			ExploraPlugin.log("Did we draw anything? " + drewAnything);
			
			return img;
		} catch (Exception ex) {
			ExploraPlugin.warn("failed to create image for MCAFile: " +  mcaFile.getFile().getName() + ": " + ex.getMessage());
			return null;
		}
	}
	
	public static BufferedImage generateZoomedBufferedImage(RegionMCAFile mcaFile, int scale, int zoomFactor) {
	    BufferedImage base = generateBufferedImage(mcaFile, scale);
	    if (base == null || zoomFactor <= 1) return base;

	    int w = base.getWidth();
	    int h = base.getHeight();
	    BufferedImage zoomed = new BufferedImage(w * zoomFactor, h * zoomFactor, BufferedImage.TYPE_INT_ARGB);

	    for (int y = 0; y < h; y++) {
	        for (int x = 0; x < w; x++) {
	            int rgb = base.getRGB(x, y);
	            for (int dy = 0; dy < zoomFactor; dy++) {
	                for (int dx = 0; dx < zoomFactor; dx++) {
	                    zoomed.setRGB(x * zoomFactor + dx, y * zoomFactor + dy, rgb);
	                }
	            }
	        }
	    }
	    return zoomed;
	}

	@SuppressWarnings("unchecked")
	private static void drawChunkImage(Chunk chunkData, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights) {
		if (chunkData.getData() == null) return;
		try {
			if (ConfigProvider.WORLD.getRenderCaves()) {
				VersionHandler.getChunkRenderer(4325).drawCaves(chunkData.getData(), VersionHandler.getColorMapping(4325), x, z, scale, pixelBuffer, terrainHeights, ConfigProvider.WORLD.getRenderHeight());
			} else if (ConfigProvider.WORLD.getRenderLayerOnly()) {
				VersionHandler.getChunkRenderer(4325).drawLayer(chunkData.getData(), VersionHandler.getColorMapping(4325), x, z, scale, pixelBuffer, ConfigProvider.WORLD.getRenderHeight());
			} else {
				VersionHandler.getChunkRenderer(4325).drawChunk(chunkData.getData(), VersionHandler.getColorMapping(4325), x, z, scale, pixelBuffer, waterPixels, terrainHeights, waterHeights, ConfigProvider.WORLD.getShade() && ConfigProvider.WORLD.getShadeWater(), ConfigProvider.WORLD.getRenderHeight());
			}
		} catch (Exception ex) {
			ExploraPlugin.warn("failed to draw chunk: " + chunkData.getAbsoluteLocation() + " " +  ex.getMessage());
			for (int cx = 0; cx < Tile.CHUNK_SIZE; cx += scale) {
				for (int cz = 0; cz < Tile.CHUNK_SIZE; cz += scale) {
					int srcIndex = cz * Tile.CHUNK_SIZE + cx;
					int dstIndex = (z + cz / scale) * Tile.SIZE / scale + (x + cx / scale);
					pixelBuffer[dstIndex] = corruptedChunkOverlay[srcIndex];
					terrainHeights[dstIndex] = 64;
					if (waterHeights != null) waterHeights[dstIndex] = 64;
				}
			}
		}
	}

	private static void flatShade(int[] pixelBuffer, short[] terrainHeights, int scale) {
		int size = Tile.SIZE / scale;
		int index = 0;
		for (int z = 0; z < size; z++) {
			for (int x = 0; x < size; x++, index++) {
				int altitudeShade = MathUtil.clamp(terrainHeights[index] / 4, -50, 50);
				pixelBuffer[index] = Color.shade(pixelBuffer[index], altitudeShade * 4);
			}
		}
	}

	private static void shade(int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, int scale) {
		if (!ConfigProvider.WORLD.getShadeWater() || !ConfigProvider.WORLD.getShade()) {
			waterHeights = terrainHeights;
		}

		int size = Tile.SIZE / scale;
		float altitudeShadeMultiplier = ConfigProvider.WORLD.getShadeAltitude() ? 12f / 256f : 0f;
		int index = 0;
		for (int z = 0; z < size; z++) {
			for (int x = 0; x < size; x++, index++) {
				if (pixelBuffer[index] == 0) continue;
				float xShade, zShade;
				if (terrainHeights[index] != waterHeights[index]) {
					float ratio = 0.5f - 0.5f / 40f * (waterHeights[index] - terrainHeights[index]);
					pixelBuffer[index] = Color.blend(pixelBuffer[index], waterPixels[index], ratio);
				} else {
					zShade = (z == 0) ? waterHeights[index + size] - waterHeights[index]
							: (z == size - 1) ? waterHeights[index] - waterHeights[index - size]
							: (waterHeights[index + size] - waterHeights[index - size]) * 2;
					xShade = (x == 0) ? waterHeights[index + 1] - waterHeights[index]
							: (x == size - 1) ? waterHeights[index] - waterHeights[index - 1]
							: (waterHeights[index + 1] - waterHeights[index - 1]) * 2;
					float shade = MathUtil.clamp(xShade + zShade, -8f, 8f);
					float altitudeShade = MathUtil.clamp((waterHeights[index] - 64f) * altitudeShadeMultiplier, -4f, 12f);
					pixelBuffer[index] = Color.shade(pixelBuffer[index], (int) ((shade + altitudeShade) * 8f));
				}
			}
		}
	}
}
