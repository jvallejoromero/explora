package com.jvallejoromero.explora.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.bukkit.Bukkit;
import org.bukkit.World;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jvallejoromero.explora.ExploraPlugin;
import com.jvallejoromero.explora.util.mcaselector.HeadlessTileImage;

import net.querz.mcaselector.io.mca.RegionChunk;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.StringTag;
import net.querz.nbt.Tag;

public class TileImageGenerator {
	
	public static final Pattern REGION_PATTERN = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
	private static final Gson gson = new GsonBuilder().create();	
	
	private static ExploraPlugin plugin = ExploraPlugin.getInstance();
	
	public static boolean generateRegionData(String worldName, int regionX, int regionZ, File outputFile) {
	    long start = System.currentTimeMillis();

	    World world = Bukkit.getWorld(worldName);
	    if (world == null) {
	        ExploraPlugin.warn("[Render] World not found: " + worldName);
	        return false;
	    }

	    File worldFolder = world.getWorldFolder();
	    Map<String, File> regionFolders = ChunkUtils.getAllRegionFolders(worldFolder);
	    
		for (Map.Entry<String, File> entry : regionFolders.entrySet()) {
			File regionDir = entry.getValue();
			
		    File regionFile = new File(regionDir, "r." + regionX + "." + regionZ + ".mca");
		    if (!regionFile.exists()) continue;
		    
		    try {
		        RegionMCAFile mcaFile = new RegionMCAFile(regionFile);
		        mcaFile.load(false);

		        long afterLoad = System.currentTimeMillis();
		        ExploraPlugin.debug("&8[Render] Loaded " + regionFile.getName() + " in " + (afterLoad - start) + "ms");
		        
		        if (mcaFile.isEmpty()) {
		        	ExploraPlugin.warn("[Render] Region: " + regionFile.getName() + " is empty! Rendering anyways..");
		        }
		        
		        boolean isNether = world.getName().toLowerCase().contains("nether");
		        BufferedImage image = HeadlessTileImage.generateZoomedBufferedImageOptimized(mcaFile, isNether, 1, 2);

		        if (image == null) {
		            ExploraPlugin.warn("[Render] Failed to render image for region " + regionX + ", " + regionZ);
		            return false;
		        }

		        if (outputFile.getParentFile() != null) {
		            outputFile.getParentFile().mkdirs(); 
		        }

		        ImageIO.write(image, "png", outputFile);
		        
                // detect biomes and chunks
                JsonObject regionInfo = new JsonObject();
                JsonArray chunks = new JsonArray();
                
                for (int i = 0; i < 1024; i++) {
                    RegionChunk regionChunk = mcaFile.getChunk(i);
                    if (regionChunk == null || regionChunk.getData() == null) continue;

                    CompoundTag level = regionChunk.getData();
                    if (level == null) continue;

                    ListTag sectionTags = level.getListTag("sections");
                    if (sectionTags == null) continue;

                    Set<String> chunkBiomes = new HashSet<>();

                    for (CompoundTag section : sectionTags.iterateType(CompoundTag.class)) {
                        CompoundTag biomesTag = section.getCompoundTag("biomes");
                        if (biomesTag == null) continue;

                        ListTag palette = biomesTag.getListTag("palette");
                        if (palette == null) continue;

                        for (Tag t : palette) {
                            if (t instanceof StringTag tag) {
                                chunkBiomes.add(tag.getValue());
                            }
                        }
                    }

                    JsonObject chunkInfo = new JsonObject();
                    chunkInfo.addProperty("x", regionChunk.getAbsoluteLocation().getX());
                    chunkInfo.addProperty("z", regionChunk.getAbsoluteLocation().getZ());

                    JsonArray biomeArray = new JsonArray();
                    for (String biome : chunkBiomes) {
                        biomeArray.add(biome);
                    }
                    chunkInfo.add("biomes", biomeArray);
                    chunks.add(chunkInfo);
                }

                regionInfo.add("chunks", chunks);
                
                File jsonFile = new File(outputFile.getParent(), outputFile.getName().replace(".png", ".json"));
                try (FileWriter writer = new FileWriter(jsonFile)) {
                    gson.toJson(regionInfo, writer);
                } catch (IOException e) {
                    ExploraPlugin.warn("[Render] Failed to write JSON for region: " + outputFile.getName() + " - " + e.getMessage());
                }

		        long totalTime = System.currentTimeMillis() - start;
		        ExploraPlugin.debug("&8[Render] Wrote data " + outputFile.getName() + " in " + totalTime + "ms");

		        return true;
		    } catch (Exception e) {
		        ExploraPlugin.warn("[Render] Error rendering region (" + regionX + ", " + regionZ + "): " + e.getMessage());
		        e.printStackTrace();
		        return false;
		    }
		}
		return false;
	}
    
	public static void rerenderUpdatedRegionsAsync(Map<String, Set<RegionCoord>> regionsToRender, Runnable onComplete) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			int cores = Runtime.getRuntime().availableProcessors();
			int maxThreads = Math.max(2, (int) Math.ceil(cores * 0.5));

			ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
			AtomicInteger remaining = new AtomicInteger();
			
			ExploraPlugin.debug("Queueing re-render for regions:");
			for (Map.Entry<String, Set<RegionCoord>> entry : regionsToRender.entrySet()) {
			    for (RegionCoord region : entry.getValue()) {
			        ExploraPlugin.debug(" - " + entry.getKey() + " r." + region.getX() + "." + region.getZ());
			    }
			}
			
			for (Map.Entry<String, Set<RegionCoord>> entry : regionsToRender.entrySet()) {
				String worldName = entry.getKey();

				for (RegionCoord region : entry.getValue()) {
					remaining.incrementAndGet();

					executor.submit(() -> {
						try {
							File outputFile = Constants.RENDER_DATA_PATH.resolve(worldName)
									.resolve("r." + region.getX() + "." + region.getZ() + ".png").toFile();

							boolean success = generateRegionData(worldName, region.getX(), region.getZ(), outputFile);

							if (!success) {
								ExploraPlugin.warn("Failed to render data for " + worldName + " r." + region.getX()
										+ "." + region.getZ());
							}
						} finally {
							if (remaining.decrementAndGet() == 0) {
								executor.shutdown();
								if (onComplete != null) {
									Bukkit.getScheduler().runTask(plugin, onComplete);
								}
							}
						}
					});
				}
			}

			// If nothing was submitted, run callback immediately
			if (remaining.get() == 0 && onComplete != null) {
				Bukkit.getScheduler().runTask(plugin, onComplete);
			}
		});
	}
    
    public static Map<String, Set<RegionCoord>> getMissingRenderRegions() {
    	Map<String, Set<RegionCoord>> regions = new HashMap<>();
    	
        File serverRoot = new File(".");
        File[] candidates = serverRoot.listFiles(File::isDirectory);
        if (candidates == null) return regions;


        for (File folder : candidates) {
            Map<String, File> regionFolders = ChunkUtils.getAllRegionFolders(folder);

            for (Entry<String, File> entry : regionFolders.entrySet()) {
                File regionDir = entry.getValue();

                if (!regionDir.exists() || !regionDir.isDirectory()) continue;

                File[] mcaFiles = regionDir.listFiles((dir, name) -> name.endsWith(".mca"));
                if (mcaFiles == null || mcaFiles.length == 0) continue;
                
                File outputDir = new File(Constants.RENDER_DATA_PATH.toFile(), entry.getKey().toLowerCase());

                for (File mcaFile : mcaFiles) {
                    Matcher matcher = REGION_PATTERN.matcher(mcaFile.getName());
                    if (!matcher.matches()) continue;

                    int regionX = Integer.parseInt(matcher.group(1));
                    int regionZ = Integer.parseInt(matcher.group(2));

                    String fileBaseName = "r." + regionX + "." + regionZ;
                    File outputPng = new File(outputDir, fileBaseName + ".png");
                    File outputJson = new File(outputDir, fileBaseName + ".json");
                    
                    if (!outputPng.exists() || !outputJson.exists()) {
                        ExploraPlugin.debug("Missing png or json for region: " + fileBaseName);
                        Set<RegionCoord> regionSet = regions.computeIfAbsent(entry.getKey().toLowerCase(), k -> new HashSet<>());
                    	RegionCoord coord = RegionCoord.fromRegionCoords(regionX, regionZ);
                        regionSet.add(coord);
                    }
                }
            }
        }
        return regions;
    }
    
    public static void generateTilesAsyncOptimized(int zoomLevel, File outputBaseDir, Runnable onComplete) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Instant start = Instant.now();
            int cores = Runtime.getRuntime().availableProcessors();
            int maxThreads = Math.max(2, (int) Math.ceil(cores * 0.5));
            
            ExecutorService pool = Executors.newFixedThreadPool(maxThreads);
            
            AtomicInteger renderedCount = new AtomicInteger();
            AtomicInteger skippedCount = new AtomicInteger();
            AtomicInteger submittedCount = new AtomicInteger();

            File serverRoot = new File(".");
            File[] candidates = serverRoot.listFiles(File::isDirectory);
            if (candidates == null) return;

            for (File folder : candidates) {
            	
                Map<String, File> regionFolders = ChunkUtils.getAllRegionFolders(folder);

                for (Entry<String, File> entry : regionFolders.entrySet()) {
                    File regionDir = entry.getValue();

                    if (!regionDir.exists() || !regionDir.isDirectory()) continue;

                    File[] mcaFiles = regionDir.listFiles((dir, name) -> name.endsWith(".mca"));
                    if (mcaFiles == null || mcaFiles.length == 0) continue;

                    boolean isNether = regionDir.getName().equalsIgnoreCase("DIM-1") || regionDir.getAbsolutePath().toLowerCase().contains("nether");
                    File outputDir = new File(outputBaseDir, entry.getKey().toLowerCase());

                    for (File mcaFile : mcaFiles) {
                        Matcher matcher = REGION_PATTERN.matcher(mcaFile.getName());
                        if (!matcher.matches()) continue;

                        int regionX = Integer.parseInt(matcher.group(1));
                        int regionZ = Integer.parseInt(matcher.group(2));

                        String fileName = "r." + regionX + "." + regionZ + ".png";
                        File outputFile = new File(outputDir, fileName);
                        if (outputFile.exists()) {
                            skippedCount.incrementAndGet();
                            continue;
                        }

                        submittedCount.incrementAndGet();
                        pool.submit(() -> {
                            try {
                                RegionMCAFile mca = new RegionMCAFile(mcaFile);
                                mca.load(false);

                                if (mca.isEmpty()) {
                                	ExploraPlugin.warn("Skipped rendering for empty region: r." + regionX + "." + regionZ);
                                	return;
                                }
                             
                                BufferedImage image = HeadlessTileImage.generateZoomedBufferedImageOptimized(mca, isNether, 1, zoomLevel);
                                if (image == null) {
                                    ExploraPlugin.warn("Failed to generate image for region: " + mcaFile.getName());
                                    return;
                                }

                                File parent = outputFile.getParentFile();
                                if (parent != null && !parent.exists()) parent.mkdirs();

                                ImageIO.write(image, "png", outputFile);
                                
                                
                                // detect biomes and chunks
                                JsonObject regionInfo = new JsonObject();
                                JsonArray chunks = new JsonArray();
                                
                                for (int i = 0; i < 1024; i++) {
                                    RegionChunk regionChunk = mca.getChunk(i);
                                    if (regionChunk == null || regionChunk.getData() == null) continue;

                                    CompoundTag level = regionChunk.getData();
                                    if (level == null) continue;

                                    ListTag sectionTags = level.getListTag("sections");
                                    if (sectionTags == null) continue;

                                    Set<String> chunkBiomes = new HashSet<>();

                                    for (CompoundTag section : sectionTags.iterateType(CompoundTag.class)) {
                                        CompoundTag biomesTag = section.getCompoundTag("biomes");
                                        if (biomesTag == null) continue;

                                        ListTag palette = biomesTag.getListTag("palette");
                                        if (palette == null) continue;

                                        for (Tag t : palette) {
                                            if (t instanceof StringTag tag) {
                                                chunkBiomes.add(tag.getValue());
                                            }
                                        }
                                    }

                                    JsonObject chunkInfo = new JsonObject();
                                    chunkInfo.addProperty("x", regionChunk.getAbsoluteLocation().getX());
                                    chunkInfo.addProperty("z", regionChunk.getAbsoluteLocation().getZ());

                                    JsonArray biomeArray = new JsonArray();
                                    for (String biome : chunkBiomes) {
                                        biomeArray.add(biome);
                                    }
                                    chunkInfo.add("biomes", biomeArray);
                                    chunks.add(chunkInfo);
                                }

                                regionInfo.add("chunks", chunks);
                                
                                File jsonFile = new File(outputFile.getParent(), outputFile.getName().replace(".png", ".json"));
                                try (FileWriter writer = new FileWriter(jsonFile)) {
                                    gson.toJson(regionInfo, writer);
                                } catch (IOException e) {
                                    ExploraPlugin.warn("Failed to write JSON for region: " + outputFile.getName() + " - " + e.getMessage());
                                }

                                int count = renderedCount.incrementAndGet();
                                if (count % 10 == 0) {
                                    int total = submittedCount.get();
                                    double percent = (count / (double) total) * 100.0;
                                    Duration elapsed = Duration.between(start, Instant.now());
                                    long estimatedTotal = (long) (elapsed.toMillis() / (count / (double) total));
                                    Duration eta = Duration.ofMillis(estimatedTotal - elapsed.toMillis());
                                    System.out.printf("Rendered %d/%d (%.1f%%) - ETA: %s\n", count, total, percent, formatDuration(eta));
                                }
                            } catch (Exception e) {
                                ExploraPlugin.warn("Error rendering region: " + mcaFile.getName());
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }

            pool.shutdown();
            try {
                pool.awaitTermination(30, TimeUnit.MINUTES);
                Duration totalTime = Duration.between(start, Instant.now());
                System.out.println("Finished rendering.");
                System.out.println("Rendered: " + renderedCount.get());
                System.out.println("Skipped: " + skippedCount.get());
                System.out.println("Total time: " + formatDuration(totalTime));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Rendering interrupted.");
            }

            if (onComplete != null) {
                Bukkit.getScheduler().runTask(plugin, onComplete);
            }
        });
    }

    private static String formatDuration(Duration d) {
        long mins = d.toMinutes();
        long secs = d.minusMinutes(mins).getSeconds();
        return String.format("%dm %ds", mins, secs);
    }
}