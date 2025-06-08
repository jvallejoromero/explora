package com.jvallejoromero.explora.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.jvallejoromero.explora.ExploraPlugin;
import com.jvallejoromero.explora.util.mcaselector.HeadlessTileImage;

import net.querz.mcaselector.io.mca.RegionMCAFile;

public class TileImageGenerator {

	/**
	 * Generates zoom level 0 to 4 images for the specified RegionMCAFile.
	 * Each zoom level halves the scale (doubles resolution).
	 *
	 * Output: region images saved in format r.X.Z.zN.png
	 */
	public static void generateAllZoomLevels(RegionMCAFile mcaFile, File outputDir) {
		int[] zoomScales = {16, 8, 4, 2, 1};

		for (int zoom = 0; zoom < zoomScales.length; zoom++) {
			int scale = zoomScales[zoom];
			BufferedImage img = HeadlessTileImage.generateBufferedImage(mcaFile, scale);
			if (img == null) continue;

			String filename = String.format("r.%d.%d.z%d.png", mcaFile.getLocation().getX(), mcaFile.getLocation().getZ(), zoom);
			File outputFile = new File(outputDir, filename);

			try {
				ImageIO.write(img, "png", outputFile);
				ExploraPlugin.log("Saved zoom level " + zoom + " image: " + outputFile.getPath());
			} catch (IOException e) {
				ExploraPlugin.warn("Failed to save image for " + filename + ": " + e.getMessage());
			}
		}
	}
}