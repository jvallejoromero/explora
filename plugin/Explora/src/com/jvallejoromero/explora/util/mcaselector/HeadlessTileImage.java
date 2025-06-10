package com.jvallejoromero.explora.util.mcaselector;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
	    try (InputStream in = HeadlessTileImage.class.getResourceAsStream("/corrupted.png")) {
	        if (in == null) {
	            ExploraPlugin.warn("corrupted.png not found in JAR.");
	        } else {
	            BufferedImage overlay = ImageIO.read(in);
	            if (overlay != null && overlay.getWidth() == 16 && overlay.getHeight() == 16) {
	                overlay.getRGB(0, 0, 16, 16, corruptedChunkOverlay, 0, 16);
	            } else {
	                ExploraPlugin.warn("corrupted.png has incorrect dimensions.");
	            }
	        }
	    } catch (IOException e) {
	        ExploraPlugin.warn("Failed to load corrupted.png: " + e.getMessage());
	    }
	}
	
	private HeadlessTileImage() {}

	public static BufferedImage generateBufferedImageOptimized(RegionMCAFile mcaFile, boolean nether, int scale) {
	    int size = Tile.SIZE / scale;
	    int chunkSize = Tile.CHUNK_SIZE / scale;
	    int pixels = Tile.PIXELS / (scale * scale);
	    
	    try {
	        int[] pixelBuffer = new int[pixels];
	        int[] waterPixels = ConfigProvider.WORLD.getShade() && ConfigProvider.WORLD.getShadeWater() && !ConfigProvider.WORLD.getRenderCaves() ? new int[pixels] : null;
	        short[] terrainHeights = new short[pixels];
	        short[] waterHeights = ConfigProvider.WORLD.getShade() && ConfigProvider.WORLD.getShadeWater() && !ConfigProvider.WORLD.getRenderCaves() ? new short[pixels] : null;

	        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	        List<Callable<Void>> tasks = new ArrayList<>();

	        for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
	            for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
	                final int xPos = cx * chunkSize;
	                final int zPos = cz * chunkSize;
	                final int index = cz * Tile.SIZE_IN_CHUNKS + cx;
	                tasks.add(() -> {
	                    Chunk data = mcaFile.getChunk(index);
	                    if (data != null) {
	                        drawChunkImage(data, nether, xPos, zPos, scale, pixelBuffer, waterPixels, terrainHeights, waterHeights);
	                    }
	                    return null;
	                });
	            }
	        }

	        // Ensure all tasks complete
	        for (Future<Void> future : pool.invokeAll(tasks)) {
	            future.get();
	        }
	        pool.shutdown();

	        if (nether) {
	            flatShade(pixelBuffer, terrainHeights, scale);
	        } else if (ConfigProvider.WORLD.getShade() && !ConfigProvider.WORLD.getRenderLayerOnly()) {
	            shade(pixelBuffer, waterPixels, terrainHeights, waterHeights, scale);
	        }

	        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
	        img.setRGB(0, 0, size, size, pixelBuffer, 0, size);
	        return img;
	    } catch (Exception ex) {
	        ExploraPlugin.warn("failed to create image for MCAFile: " +  mcaFile.getFile().getName() + ": " + ex.getMessage());
	        return null;
	    }
	}
	
	public static BufferedImage generateZoomedBufferedImageOptimized(RegionMCAFile mcaFile, boolean nether, int scale, int zoomFactor) {
	    BufferedImage base = generateBufferedImageOptimized(mcaFile, nether, scale);
	    if (base == null || zoomFactor <= 1) return base;

	    int w = base.getWidth();
	    int h = base.getHeight();
	    int[] srcPixels = base.getRGB(0, 0, w, h, null, 0, w);
	    BufferedImage zoomed = new BufferedImage(w * zoomFactor, h * zoomFactor, BufferedImage.TYPE_INT_ARGB);
	    int[] destPixels = new int[w * h * zoomFactor * zoomFactor];

	    for (int y = 0; y < h; y++) {
	        for (int x = 0; x < w; x++) {
	            int color = srcPixels[y * w + x];
	            int baseIndex = (y * zoomFactor) * (w * zoomFactor) + (x * zoomFactor);
	            for (int dy = 0; dy < zoomFactor; dy++) {
	                int rowStart = baseIndex + dy * (w * zoomFactor);
	                for (int dx = 0; dx < zoomFactor; dx++) {
	                    destPixels[rowStart + dx] = color;
	                }
	            }
	        }
	    }

	    zoomed.setRGB(0, 0, w * zoomFactor, h * zoomFactor, destPixels, 0, w * zoomFactor);
	    return zoomed;
	}

	@SuppressWarnings("unchecked")
	private static void drawChunkImage(Chunk chunkData, boolean nether, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights) {
		if (chunkData.getData() == null) return;
		
		try {
			if (nether) {
				VersionHandler.getChunkRenderer(4325).drawCaves(chunkData.getData(), VersionHandler.getColorMapping(4325), x, z, scale, pixelBuffer, terrainHeights, ConfigProvider.WORLD.getRenderHeight());
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
