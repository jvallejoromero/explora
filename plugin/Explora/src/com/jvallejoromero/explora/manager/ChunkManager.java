package com.jvallejoromero.explora.manager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jvallejoromero.explora.ExploraPlugin;
import com.jvallejoromero.explora.util.Constants;

public class ChunkManager {

	private final ExploraPlugin plugin;
	private final Map<String, Set<String>> worldToChunks = new HashMap<>();
	private final Map<String, Set<String>> newlyExploredChunks = new HashMap<>();
	
	public ChunkManager(ExploraPlugin plugin) {
		this.plugin = plugin;
	}

	public void init() {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			ExploraPlugin.log("&aReading chunk data..");
			
			loadChunksFromAllJSONFiles();
			
			Bukkit.getScheduler().runTask(plugin, () -> {
				ExploraPlugin.log("&aLoaded chunk data.");
			});
		});
		
		// schedule the task to periodically update chunk data
		Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			ExploraPlugin.log("&aUpdating chunk data..");
			saveNewlyExploredChunksToDisk();
		}, Constants.CHUNK_UPDATE_TICKS, Constants.CHUNK_UPDATE_TICKS);
	}

	private void loadChunksFromAllJSONFiles() {
		File dataFolder = Constants.SAVE_PATH.toFile();
		File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".json"));
		if (files == null) return;

		for (File file : files) {
			try (FileReader reader = new FileReader(file)) {
				JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
				
				JsonElement worldElem = obj.get("world");
				String worldName = (worldElem != null && !worldElem.isJsonNull()) ? worldElem.getAsString() : "unknown";
				
				JsonArray chunkList = obj.getAsJsonArray("exploredChunks");
				
				Set<String> chunkSet = new HashSet<>();
				for (JsonElement el : chunkList) {
					JsonObject chunk = el.getAsJsonObject();
					int x = chunk.get("x").getAsInt();
					int z = chunk.get("z").getAsInt();
					chunkSet.add(x + "," + z);
				}

				worldToChunks.put(worldName, chunkSet);
				ExploraPlugin.log("&aLoaded " + chunkSet.size() + " chunks from " + file.getName());

			} catch (Exception e) {
				ExploraPlugin.warn("Failed to read file: " + file.getName());
			}
		}
	}
	
	public void saveNewlyExploredChunksToDisk() {
        for (Map.Entry<String, Set<String>> entry : newlyExploredChunks.entrySet()) {
            String worldName = entry.getKey();
            Set<String> newChunks = entry.getValue();

            if (newChunks.isEmpty()) continue;

            File file = Constants.SAVE_PATH.resolve("explored_chunks_" + worldName + ".json").toFile();

            try (FileReader reader = new FileReader(file)) {
                JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                
				JsonElement dimensionElem = obj.get("dimension");
				String dimensionName = (dimensionElem != null && !dimensionElem.isJsonNull()) ? dimensionElem.getAsString() : "unknown";
				
                JsonArray chunkArray = obj.getAsJsonArray("exploredChunks");
                
                // DEDUPLICATION STEP
                Set<String> existingKeys = new HashSet<>();
                for (JsonElement el : chunkArray) {
                    JsonObject chunk = el.getAsJsonObject();
                    int x = chunk.get("x").getAsInt();
                    int z = chunk.get("z").getAsInt();
                    existingKeys.add(x + "," + z);
                }
                
                int uniqueChunks = 0;
                
                // ADD ONLY UNIQUE CHUNKS
                for (String chunkKey : newChunks) {
                    if (existingKeys.contains(chunkKey)) continue;

                    String[] parts = chunkKey.split(",");
                    int x = Integer.parseInt(parts[0]);
                    int z = Integer.parseInt(parts[1]);

                    JsonObject chunkObj = new JsonObject();
                    chunkObj.addProperty("x", x);
                    chunkObj.addProperty("z", z);
                    chunkArray.add(chunkObj);
                    
                    uniqueChunks++;
                }
                
    			Map<String, Object> data = new LinkedHashMap<>();
    			data.put("world", worldName);
    			data.put("dimension", dimensionName);
    			data.put("exploredChunks", chunkArray);

                try (FileWriter writer = new FileWriter(file)) {
                    new GsonBuilder().setPrettyPrinting().create().toJson(data, writer);
                }
                
                if (uniqueChunks > 0) {
                	ExploraPlugin.log("&aUpdated chunks for " + worldName + " (" + uniqueChunks + " chunks).");
                } else {
                	ExploraPlugin.log("&aNo new chunks found.");
                }
            } catch (Exception ex) {
                ExploraPlugin.warn("Failed to save new chunks for world: " + worldName);
            }
        }
        
        newlyExploredChunks.clear();
	}


	public boolean isChunkExplored(String world, int x, int z) {
		Set<String> chunks = worldToChunks.get(world);
		return chunks != null && chunks.contains(x + "," + z);
	}

	public void markChunkAsExplored(String world, int x, int z) {
	    String key = x + "," + z;

	    if (!isChunkExplored(world, x, z)) {
	        worldToChunks.computeIfAbsent(world, k -> new HashSet<>()).add(key);
	        newlyExploredChunks.computeIfAbsent(world, k -> new HashSet<>()).add(key);
	    }
	}

}
