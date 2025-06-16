package com.jvallejoromero.explora.manager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.World;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jvallejoromero.explora.ExploraPlugin;
import com.jvallejoromero.explora.util.ChunkCoord;
import com.jvallejoromero.explora.util.Constants;
import com.jvallejoromero.explora.util.FileUtil;
import com.jvallejoromero.explora.util.HttpUtil;
import com.jvallejoromero.explora.util.RegionCoord;
import com.jvallejoromero.explora.util.TileImageGenerator;

/**
 * Handles chunk tracking and management across all worlds in the server.
 * 
 * Responsibilities:
 * - Loading and saving chunk data to JSON files.
 * - Tracking newly explored chunks.
 * - Sending chunk data to a backend service.
 * - Scheduling rerendering of updated regions.
 * 
 * Used by: ExploraPlugin
 * Depends on: Constants, TileImageGenerator, HttpUtil
 */
public class ChunkManager {

	private final ExploraPlugin plugin;
	private final Map<String, Set<ChunkCoord>> worldToChunks = new HashMap<>();
	private final Map<String, Set<ChunkCoord>> newlyExploredChunks = new HashMap<>();
	
	private final AtomicInteger pendingBatchCount = new AtomicInteger(0);
	
	private boolean sentChunksToDatabase = false;
	
	public ChunkManager(ExploraPlugin plugin) {
		this.plugin = plugin;
	}

	/**
	 * Initializes the data required for this class to work and sets up periodic tasks
	 * @param onLoaded if specified, runs after loading has completed
	 */
	public void init(Runnable onLoaded) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			ExploraPlugin.log("&aReading chunk data..");
			
			loadChunksFromAllJSONFiles();
			
	        if (!Constants.SHOULD_SCAN_FOLDERS) {
	            setSentChunksToDatabase(true);
	        }
			
			if (onLoaded != null) Bukkit.getScheduler().runTask(plugin, onLoaded);
		});
		
		// schedule the task to periodically update chunk data every x ticks
		Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			if (hasSentChunksToDatabase()) {
				saveNewlyExploredChunksToDisk();
				
				sendNewChunksToDatabase(() -> {					
					Map<String, Set<RegionCoord>> regions = getRegionsToRerender();
					getNewlyExploredChunks().clear();
					
					TileImageGenerator.rerenderUpdatedRegionsAsync(regions, () -> {
						FileUtil.sendRerenderedTilesToBackendAsync(regions, () -> {
							ExploraPlugin.log("&aUpdated database and saved new chunks to disk.");
						});
					});
				});	
			}
		}, Constants.CHUNK_UPDATE_TICKS, Constants.CHUNK_UPDATE_TICKS);
	}

	/**
	 * Parses and loads all previously explored chunks from JSON files in the configured save directory.
	 * Populates the internal `worldToChunks` map.
	 * 
	 */
	private void loadChunksFromAllJSONFiles() {
		File dataFolder = Constants.SAVE_PATH.toFile();
		File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".json"));
		if (files == null) {
			ExploraPlugin.warn("No valid .json files found in the data folder!");
			return;
		}

		int filesRead = 0;
		
		for (File file : files) {
			try (FileReader reader = new FileReader(file)) {
				JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
				
				JsonElement worldElem = obj.get("world");
				String worldName = (worldElem != null && !worldElem.isJsonNull()) ? worldElem.getAsString() : "unknown";
				
				JsonArray chunkList = obj.getAsJsonArray("exploredChunks");
				
				HashSet<ChunkCoord> chunkSet = new HashSet<>();
				for (JsonElement el : chunkList) {
					JsonObject chunk = el.getAsJsonObject();
					int x = chunk.get("x").getAsInt();
					int z = chunk.get("z").getAsInt();
					
					ChunkCoord chunkCoord = new ChunkCoord(x,z);
					chunkSet.add(chunkCoord);
				}

				worldToChunks.put(worldName, chunkSet);
				ExploraPlugin.log("&aLoaded " + chunkSet.size() + " chunks from " + file.getName());

			} catch (Exception e) {
				ExploraPlugin.warn("Failed to read file: " + file.getName());
			}
			filesRead++;
		}
		
		if (filesRead == 0) {
			ExploraPlugin.warn("Did not read any .json files from the data folder. Is it empty?");
		}
	}
	
	/**
	 * Saves all new chunks to the json files
	 */
	public void saveNewlyExploredChunksToDisk() {
		int newChunkSets = 0;
		
        for (Map.Entry<String, Set<ChunkCoord>> entry : newlyExploredChunks.entrySet()) {
            String worldName = entry.getKey();
            Set<ChunkCoord> newChunks = entry.getValue();

            if (newChunks.isEmpty()) continue;
            newChunkSets++;

            File file = Constants.SAVE_PATH.resolve("explored_chunks_" + worldName + ".json").toFile();
            if (!file.exists()) {
                ExploraPlugin.warn("Chunk JSON file missing for world: " + worldName);
                continue;
            }
            
            try (FileReader reader = new FileReader(file)) {
                JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                
				JsonElement dimensionElem = obj.get("dimension");
				String dimensionName = (dimensionElem != null && !dimensionElem.isJsonNull()) ? dimensionElem.getAsString() : "unknown";
				
                JsonArray chunkArray = obj.getAsJsonArray("exploredChunks");
                
                // DEDUPLICATION STEP
                Set<ChunkCoord> existingKeys = new HashSet<>();
                for (JsonElement el : chunkArray) {
                    JsonObject chunk = el.getAsJsonObject();
                    int x = chunk.get("x").getAsInt();
                    int z = chunk.get("z").getAsInt();
                    
                    ChunkCoord chunkCoord = new ChunkCoord(x,z);
                    existingKeys.add(chunkCoord);
                }
                
                int uniqueChunks = 0;
                
                // ADD ONLY UNIQUE CHUNKS
                for (ChunkCoord chunkKey : newChunks) {
                    if (existingKeys.contains(chunkKey)) continue;
                    int x = chunkKey.getX();
                    int z = chunkKey.getZ();

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
        
        if (newChunkSets == 0) {
        	ExploraPlugin.log("&aNo new chunks found.");
        }
	}


	public boolean isChunkExplored(String world, int x, int z) {
		Set<ChunkCoord> chunks = worldToChunks.get(world);
		return chunks != null && chunks.contains(new ChunkCoord(x,z));
	}

	public void recordChunkIfNew(String world, int x, int z) {
	    ChunkCoord key = new ChunkCoord(x,z);

	    if (!isChunkExplored(world, x, z)) {
	        worldToChunks.computeIfAbsent(world, k -> new HashSet<>()).add(key);
	        newlyExploredChunks.computeIfAbsent(world, k -> new HashSet<>()).add(key);
	    }
	}
	
	/**
	 * Sends all chunks to the node backend
	 * @param onComplete if specified, runs when all data is done being sent
	 */
	public void sendChunksToDatabase(Runnable onComplete) {
	    sentChunksToDatabase = false;
	    
	    // compute total batch count 
	    int totalBatches = 0;
	    Map<World, Set<ChunkCoord>> validChunkSets = new HashMap<>();

	    for (Map.Entry<String, Set<ChunkCoord>> entry : getAllWorldChunks().entrySet()) {
	        String worldName = entry.getKey();
	        Set<ChunkCoord> chunks = entry.getValue();

	        if (chunks.isEmpty()) continue;

	        World world = Bukkit.getWorld(worldName);
	        if (world == null) {
	            ExploraPlugin.warn("⚠ World not found: " + worldName);
	            continue;
	        }

	        int batchSize = Constants.BACKEND_CHUNK_BATCH_SIZE;
	        int batchCount = (int) Math.ceil(chunks.size() / (double) batchSize);

	        if (batchCount == 0) continue;

	        totalBatches += batchCount;
	        validChunkSets.put(world, chunks);
	    }
	    
	    if (totalBatches == 0) {
	        sentChunksToDatabase = true;
	        if (onComplete != null) {
	            Bukkit.getScheduler().runTask(plugin, onComplete);
	        }
	        return;
	    }

	    // set total batch count before sending
	    pendingBatchCount.set(totalBatches);
	    
	    if (Constants.DEBUG_MODE) {
	    	ExploraPlugin.log("&a[HTTP] Sending a total of " + totalBatches + " batches to the backend..");
	    }

	    // stream all batches
	    for (Map.Entry<World, Set<ChunkCoord>> entry : validChunkSets.entrySet()) {
	        World world = entry.getKey();
	    	Set<ChunkCoord> chunks = entry.getValue();
	    	List<ChunkCoord> chunkList = new ArrayList<>(chunks);
	    	
	        HttpUtil.streamChunkBatches(world, chunkList, Constants.BACKEND_CHUNK_BATCH_SIZE, Constants.BACKEND_CHUNK_BATCH_POST_DELAY_TICKS, () -> {
	            int remaining = pendingBatchCount.decrementAndGet();
	            if (remaining <= 0) {
	                sentChunksToDatabase = true;
	                if (onComplete != null) {
	                    Bukkit.getScheduler().runTask(plugin, onComplete);
	                }
	            }
	        });
	    }
	}
	
	/**
	 * Sends only new chunks to node backend
	 * @param onComplete, if specified runs when data is done being sent
	 */
	public void sendNewChunksToDatabase(Runnable onComplete) {
	    sentChunksToDatabase = false;
	    
	    // compute total batch count 
	    int totalBatches = 0;
	    Map<World, Set<ChunkCoord>> validChunkSets = new HashMap<>();

	    for (Map.Entry<String, Set<ChunkCoord>> entry : getNewlyExploredChunks().entrySet()) {
	        String worldName = entry.getKey();
	        Set<ChunkCoord> chunks = entry.getValue();

	        if (chunks.isEmpty()) continue;

	        World world = Bukkit.getWorld(worldName);
	        if (world == null) {
	            ExploraPlugin.warn("⚠ World not found: " + worldName);
	            continue;
	        }

	        int batchSize = Constants.BACKEND_CHUNK_BATCH_SIZE;
	        int batchCount = (int) Math.ceil(chunks.size() / (double) batchSize);

	        if (batchCount == 0) continue;

	        totalBatches += batchCount;
	        validChunkSets.put(world, chunks);
	    }
	    
	    if (totalBatches == 0) {
	        sentChunksToDatabase = true;
	        return;
	    }

	    // set total batch count before sending
	    pendingBatchCount.set(totalBatches);
	    
	    if (Constants.DEBUG_MODE) {
	    	ExploraPlugin.log("&a[HTTP] Sending a total of " + totalBatches + " batches to the backend..");
	    }

	    // stream all batches
	    for (Map.Entry<World, Set<ChunkCoord>> entry : validChunkSets.entrySet()) {
	        World world = entry.getKey();
	    	Set<ChunkCoord> chunks = entry.getValue();
	    	List<ChunkCoord> chunkList = new ArrayList<>(chunks);
	    	
	        HttpUtil.streamChunkBatches(world, chunkList, Constants.BACKEND_CHUNK_BATCH_SIZE, Constants.BACKEND_CHUNK_BATCH_POST_DELAY_TICKS, () -> {
	            int remaining = pendingBatchCount.decrementAndGet();
	            if (remaining <= 0) {
	                sentChunksToDatabase = true;
	                if (onComplete != null) {
	                    Bukkit.getScheduler().runTask(plugin, onComplete);
	                }
	            }
	        });
	    }
	}
	
	/**
	 * Gets regions that need re-rendering, based on newly explored chunks
	 * @return Map containing regions to re-render, with the key being the world and the value being a Set of RegionCoords
	 */
	public Map<String, Set<RegionCoord>> getRegionsToRerender() {
	    Map<String, Set<RegionCoord>> regionsToRerender = new HashMap<>();

	    for (Map.Entry<String, Set<ChunkCoord>> entry : newlyExploredChunks.entrySet()) {
	        String world = entry.getKey();
	        Set<ChunkCoord> chunks = entry.getValue();

	        Set<RegionCoord> regionSet = regionsToRerender.computeIfAbsent(world, k -> new HashSet<>());

	        for (ChunkCoord chunk : chunks) {
	            RegionCoord region = new RegionCoord(chunk.getX(), chunk.getZ());
	            regionSet.add(region); 
	        }
	    }
	    return regionsToRerender;
	}
	
	public boolean hasSentChunksToDatabase() {
		return sentChunksToDatabase;
	}
	
	public Map<String, Set<ChunkCoord>> getAllWorldChunks() {
		return worldToChunks;
	}
	
	public Map<String, Set<ChunkCoord>> getNewlyExploredChunks() {
		return newlyExploredChunks;
	}
	
	public void setSentChunksToDatabase(boolean sent) {
		this.sentChunksToDatabase = sent;
	}

}
