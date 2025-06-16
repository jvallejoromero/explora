package com.jvallejoromero.explora;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.jvallejoromero.explora.listener.ChunkTracker;
import com.jvallejoromero.explora.manager.ChunkManager;
import com.jvallejoromero.explora.tasks.PlayerUpdateTask;
import com.jvallejoromero.explora.tasks.ServerStatusUpdateTask;
import com.jvallejoromero.explora.util.ChunkUtils;
import com.jvallejoromero.explora.util.Constants;
import com.jvallejoromero.explora.util.FileUtil;
import com.jvallejoromero.explora.util.HttpUtil;
import com.jvallejoromero.explora.util.RegionCoord;
import com.jvallejoromero.explora.util.StringUtils;
import com.jvallejoromero.explora.util.TileImageGenerator;
import com.jvallejoromero.explora.util.mcaselector.VersionHandler;
import com.jvallejoromero.explora.yaml.CustomConfigurationFile;

/**
 * Main plugin class for {@code Explora}, a Spigot plugin that visualizes terrain data
 * and syncs world exploration to a backend map rendering service.
 *
 * <p>This class handles:
 * <ul>
 *   <li>Initializing configuration and constants from {@code config.yml}</li>
 *   <li>Managing lifecycle hooks ({@link #onEnable()}, {@link #onDisable()})</li>
 *   <li>Scanning world folders for explored chunks and region files</li>
 *   <li>Sending chunk and render metadata to the backend API</li>
 *   <li>Scheduling async tasks for periodic player and server updates</li>
 *   <li>Conditionally triggering re-renders of missing tiles</li>
 * </ul>
 *
 * <p>Data is asynchronously scanned and synced to avoid blocking the main server thread.
 *
 */
public class ExploraPlugin extends JavaPlugin {
	
	private static ExploraPlugin instance;
	private static ChunkManager chunkManager;
	
	private static boolean chunksLoaded = false;
	
	private CustomConfigurationFile config;
	
	/**
	 * Called when the plugin is enabled.
	 *
	 * <p>Initializes configuration, chunk tracking, and tile rendering pipelines.
	 * Depending on {@code config.yml}, this may trigger a full world scan and backend sync.
	 */
	@Override
	public void onEnable() {
		VersionHandler.init();
		
		instance = this;
		config = new CustomConfigurationFile("config.yml", true);
		chunkManager = new ChunkManager(this);
		
		Constants.init(this);
		
		this.registerEvents();
		
		if (Constants.SHOULD_SCAN_FOLDERS) {
			System.out.println(" ");
			System.out.println(" ");
			log("&eWARNING: scan-region-files has been set to true.");
			log("&ePlease wait while world folders are scanned for region files.");
			log("&eThis may take a while. Do NOT shut down the server until this is finished!");
			System.out.println(" ");
			System.out.println(" ");
			
			try {
				long scanStartTime = System.currentTimeMillis();
				
				ChunkUtils.scanWorldsAsync(() -> {
					getConfiguration().set("scan-region-files", false);
					getConfiguration().save();
					
					long scanElapsed = (System.currentTimeMillis() - scanStartTime);
					
					ExploraPlugin.log("&aWorld folder scan completed. Completed in " + (scanElapsed/1000.0) + "s");
					
					chunkManager.init(() -> {
						chunksLoaded = true;
						ExploraPlugin.log("&aLoaded chunk data.");
						System.out.println(" ");
						System.out.println(" ");
						ExploraPlugin.log("&eAttempting to send chunk data to database.. This might take a while ..");
						System.out.println(" ");
						System.out.println(" ");
						long syncStartTime = System.currentTimeMillis();
						
						HttpUtil.sendDeleteChunksRequest(() -> {
							chunkManager.sendChunksToDatabase(() -> {
								long syncElapsed = (System.currentTimeMillis()-syncStartTime);
								ExploraPlugin.log("&aFinished sending chunks to database. Completed in " + (syncElapsed/1000.0) + "s");
								
								System.out.println(" ");
								System.out.println(" ");
								log("&e Generating render files and metadata..");
								log("&e This could take a while. Please do not interrupt the server!");
								System.out.println(" ");
								System.out.println(" ");
								
								File outputDir = Constants.RENDER_DATA_PATH.toFile();
								
								TileImageGenerator.generateTilesAsyncOptimized(2, outputDir, () -> {
									log("&aFinished generating render files and metadata!");
									
									System.out.println(" ");
									System.out.println(" ");
									log("&e Sending render files and metadata to database..");
									System.out.println(" ");
									System.out.println(" ");
									
									FileUtil.sendRegionDataToBackendAsync(() -> {
										log("&aFinished sending render files and metadata to database.");
									});
								});
							});
						});
					});
				});
			} catch (Exception ex) {
				warn("An error ocurred while parsing chunk data.");
			} 
		} else {
			chunkManager.init(() -> {
				chunksLoaded = true;
				ExploraPlugin.log("&aLoaded chunk data.");
				
				Bukkit.getScheduler().runTaskAsynchronously(this, ()-> {
					
					System.out.println(" ");
					log("&eVerifying render files and metadata..");
					System.out.println(" ");
					
					Map<String, Set<RegionCoord>> missingRegions = TileImageGenerator.getMissingRenderRegions();
					if (missingRegions.isEmpty()) {
						log("&aFinished verifying render files. No missing files found.");
						return;
					}
					
					System.out.println(" ");
					log("&eFound missing render files, preparing to render..");
					System.out.println(" ");
					
					TileImageGenerator.rerenderUpdatedRegionsAsync(missingRegions, () -> {
						FileUtil.sendRerenderedTilesToBackendAsync(missingRegions, () -> {
							ExploraPlugin.log("&aRendered missing regions and updated database.");
						});
					});
				});
			});
		}
		
		new PlayerUpdateTask().runTaskTimer(this, Constants.PLAYER_UPDATE_TICKS, Constants.PLAYER_UPDATE_TICKS);
		new ServerStatusUpdateTask().runTaskTimerAsynchronously(this, Constants.SERVER_STATUS_UPDATE_TICKS, Constants.SERVER_STATUS_UPDATE_TICKS);
		log("&a" + Constants.PLUGIN_NAME + " v" + this.getDescription().getVersion() + " enabled!");
	}
	
	/**
	 * Called when the plugin is disabled.
	 *
	 * <p>Flushes all newly explored chunk data to disk before shutting down.
	 */
	@Override
	public void onDisable() {
		log("&aSaving chunk data to files before disabling..");
		
		getChunkManager().saveNewlyExploredChunksToDisk();
		
		log("&a" + Constants.PLUGIN_NAME + " v" + this.getDescription().getVersion() + " disabled!");
	}
	
	/**
	 * @return the singleton plugin instance
	 * @throws IllegalStateException if accessed before plugin initialization
	 */
	public static ExploraPlugin getInstance() {
		if (instance == null) throw new IllegalStateException("Plugin not initialized yet!");
		return instance;
	}
	
	/**
	 * @return whether the initial chunk data has been loaded from disk
	 */
	public static boolean hasLoadedChunks() {
		return chunksLoaded;
	}
	
	/**
	 * Sends a formatted message to the server console with plugin prefix and color support.
	 *
	 * @param message the message to send
	 */
	public static void log(String message) {
		Bukkit.getConsoleSender().sendMessage(StringUtils.colorize("[" + Constants.PLUGIN_NAME + "] " + message));
	}
	
	/**
	 * Sends a formatted warning message to the console in red.
	 *
	 * @param message the warning to display
	 */
	public static void warn(String message) {
		Bukkit.getConsoleSender().sendMessage(StringUtils.colorize("[" + Constants.PLUGIN_NAME + "] " + "&c [WARNING] " + message));
	}
	
	/**
	 * Sends a debug message to the console if {@code debug-mode} is enabled in the config.
	 *
	 * @param message the debug message
	 */
	public static void debug(String message) {
		if (!Constants.DEBUG_MODE) return;
		Bukkit.getConsoleSender().sendMessage(StringUtils.colorize("[" + Constants.PLUGIN_NAME + "]" + "&8 [DEBUG] " + message));
	}
	
	/**
	 * Registers all event listeners used by the plugin.
	 * 
	 * <p>Called once during plugin startup inside {@link #onEnable()}.
	 */
	public void registerEvents() {
		this.getServer().getPluginManager().registerEvents(new ChunkTracker(), this);
	}
	
	/**
	 * @return the plugin's loaded {@link CustomConfigurationFile}
	 */
	public CustomConfigurationFile getConfiguration() {
		return config;
	}
	
	/**
	 * @return the singleton {@link ChunkManager} instance
	 */
	public ChunkManager getChunkManager() {
		return chunkManager;
	}
	

}
