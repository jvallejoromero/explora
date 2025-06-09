package com.jvallejoromero.explora.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.bukkit.Bukkit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jvallejoromero.explora.ExploraPlugin;

import net.querz.mcaselector.io.mca.RegionChunk;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.util.point.Point2i;

public class ChunkUtils {

	private static final Gson gson = new GsonBuilder().create();	
	private static ExploraPlugin plugin = ExploraPlugin.getInstance();
	
	
	/**
	 * Scans all world folders in the server root asynchronously, extracts explored chunk data 
	 * from region files, and saves the results as JSON files under the plugin's data folder.
	 * 
	 * This method is non-blocking and runs the scan on a separate thread to avoid freezing
	 * the main server thread. Once the scan is complete, the provided {@code onComplete} 
	 * callback (if not null) is executed on the main server thread.
	 * 
	 * @param onComplete a Runnable to run on the main thread after scanning is complete; may be null
	 */
	public static void scanWorldsAsync(Runnable onComplete) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			File serverRoot = new File(".");
			File[] candidates = serverRoot.listFiles(File::isDirectory);

			if (candidates == null) return;

			for (File folder : candidates) {
				Map<String, File> regionFolders = getAllRegionFolders(folder); 
				
				for (Entry<String, File> entry : regionFolders.entrySet()) {
					String dimension = entry.getKey();
					File regionDir = entry.getValue();
					
					ExploraPlugin.log("&6Scanning world: " + folder.getName() + " [" + dimension + "]");
					Set<ChunkCoord> exploredChunks = getExploredChunksFromRegionFolder(regionDir);
					
					saveAsJson(folder.getName(), dimension, exploredChunks);
				}
			}
			
			if (onComplete != null) {
				Bukkit.getScheduler().runTask(plugin, onComplete); 
			}
		});
	}

	/**
	 * Scans the given world folder and finds all "region" directories within it.
	 *
	 * @param worldFolder The root world folder to scan (e.g., "world", "world_nether").
	 * @return A map where the key is the readable dimension name (e.g., "overworld", "nether") 
	 *         and the value is the corresponding region folder.
	 */
	public static Map<String, File> getAllRegionFolders(File worldFolder) {
	    Map<String, File> regionFolders = new HashMap<>();

	    Queue<File> toSearch = new LinkedList<>();
	    toSearch.add(worldFolder);

	    while (!toSearch.isEmpty()) {
	        File current = toSearch.poll();
	        File[] subdirs = current.listFiles(File::isDirectory);
	        if (subdirs == null) continue;

	        for (File subdir : subdirs) {
	            if (subdir.getName().equals("region")) {
	                String dimensionName = worldFolder.getName();
	                regionFolders.put(dimensionName, subdir);
	            } else {
	                toSearch.add(subdir);
	            }
	        }
	    }

	    return regionFolders;
	}
	
	/**
	 * Returns a readable dimension name (e.g., "overworld", "nether", "the_end") 
	 * based on the relative path between the world folder and its region folder.
	 *
	 * @param worldFolder The base world folder (e.g., "world", "world_nether").
	 * @param regionFolder The region directory inside the world folder structure.
	 * @return A simplified name for the dimension such as "overworld", "nether", "the_end", or a custom relative path.
	 */
	public static String getRelativeDimensionName(File worldFolder, File regionFolder) {
	    try {
	        Path worldPath = worldFolder.getCanonicalFile().toPath();
	        Path regionPath = regionFolder.getCanonicalFile().toPath();
	        Path relativePath = worldPath.relativize(regionPath.getParent());

	        String key = relativePath.toString().replace(File.separator, "/");

	        if (key.isEmpty() || key.equals(".")) {
	            return "overworld";
	        } else if (key.equalsIgnoreCase("DIM-1")) {
	            return "nether";
	        } else if (key.equalsIgnoreCase("DIM1")) {
	            return "the_end";
	        }

	        return key;
	    } catch (IOException e) {
	        return regionFolder.getName(); // fallback
	    }
	}
	
	/**
	 * Returns a set of strings representing the explored chunks in ChunkCoord format
	 * from the provided region directory.
	 *
	 * @param regionDir The region folder containing .mca files to scan.
	 * @return A set of explored chunk coordinates as ChunkCoord objects
	 */
	public static Set<ChunkCoord> getExploredChunksFromRegionFolder(File regionDir) {
	    Set<ChunkCoord> exploredChunks = new HashSet<>();

	    File[] regionFiles = regionDir.listFiles((dir, name) -> name.endsWith(".mca"));
	    if (regionFiles == null) return exploredChunks;

	    for (File regionFile : regionFiles) {
	        try {
	            RegionMCAFile region = new RegionMCAFile(regionFile);
	            region.load(false); // load raw = false
	            
	            for (int i = 0; i < 1024; i++) {
	                RegionChunk chunk = region.getChunk(i);
	            	
	                if (chunk != null && !chunk.isEmpty()) {
	                    Point2i coord = chunk.getAbsoluteLocation();	                    
	                    ChunkCoord chunkCoord = new ChunkCoord(coord.getX(), coord.getZ());
	                    exploredChunks.add(chunkCoord);
	                }
	            }

	        } catch (Exception e) {
	            ExploraPlugin.warn("Failed to load region: " + regionFile.getName() + " - " + e.getMessage());
	        }
	    }

	    return exploredChunks;
	}


	/**
	 * Saves the explored chunks to a JSON file inside the plugin's data folder.
	 *
	 * @param worldName The name of the world or dimension (e.g., "world", "world_nether").
	 * @param chunks    A set of strings representing explored chunks in "x,z" format.
	 */
	public static void saveAsJson(String worldName, String dimension, Set<ChunkCoord> chunks) {
		try {
			Files.createDirectories(Constants.SAVE_PATH);
			File output = new File(Constants.SAVE_PATH.toFile(), "explored_chunks_" + worldName + ".json");

			List<Map<String, Integer>> chunkList = new ArrayList<>();

			for (ChunkCoord chunk : chunks) {
				int x = chunk.getX();
				int z = chunk.getZ();
				
				Map<String, Integer> chunkObj = new HashMap<>();
				chunkObj.put("x", x);
				chunkObj.put("z", z);
				chunkList.add(chunkObj);
			}

			Map<String, Object> data = new LinkedHashMap<>();
			data.put("world", worldName);
			data.put("dimension", dimension);
			data.put("exploredChunks", chunkList);

			try (FileWriter writer = new FileWriter(output)) {
				gson.toJson(data, writer);
			}

			ExploraPlugin.log("&aSaved explored chunks for " + worldName + " (" + chunkList.size() + " chunks)");
		} catch (Exception e) {
			ExploraPlugin.warn("Failed to write chunk JSON for " + worldName + ": " + e.getMessage());
		}
	}
	
	public static BufferedImage scaleImage(BufferedImage original, int zoomFactor) {
		int width = original.getWidth() * zoomFactor;
		int height = original.getHeight() * zoomFactor;

		BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = scaled.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR); // preserve pixel look
		g.drawImage(original, 0, 0, width, height, null);
		g.dispose();

		return scaled;
	}
}
