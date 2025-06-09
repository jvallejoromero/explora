package com.jvallejoromero.explora;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.jvallejoromero.explora.listener.ChunkTracker;
import com.jvallejoromero.explora.manager.ChunkManager;
import com.jvallejoromero.explora.tasks.PlayerUpdateTask;
import com.jvallejoromero.explora.util.ChunkUtils;
import com.jvallejoromero.explora.util.Constants;
import com.jvallejoromero.explora.util.FileUtil;
import com.jvallejoromero.explora.util.HttpUtil;
import com.jvallejoromero.explora.util.StringUtils;
import com.jvallejoromero.explora.util.TileImageGenerator;
import com.jvallejoromero.explora.util.mcaselector.VersionHandler;
import com.jvallejoromero.explora.yaml.CustomConfigurationFile;

public class ExploraPlugin extends JavaPlugin {
	
	private static ExploraPlugin instance;
	private static ChunkManager chunkManager;
	
	private static boolean chunksLoaded = false;
	
	private CustomConfigurationFile config;
	
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
			log("&6Warning: scan-region-files has been set to true. Please wait while world folders are scanned for region files. "
					+ "This may take a while. Do NOT shut down the server until this is finished!");
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
						ExploraPlugin.log("&6&oAttempting to send chunk data to database.. This might take a while ..");
						System.out.println(" ");
						System.out.println(" ");
						long syncStartTime = System.currentTimeMillis();
						
						HttpUtil.sendDeleteChunksRequest(() -> {
							chunkManager.sendChunksToDatabase(() -> {
								long syncElapsed = (System.currentTimeMillis()-syncStartTime);
								ExploraPlugin.log("&aFinished sending chunks to database. Completed in " + (syncElapsed/1000.0) + "s");
								
								System.out.println(" ");
								System.out.println(" ");
								log("&6 Verifying render files and metadata..");
								log("&6 This could take a while. Please do not interrupt the server!");
								System.out.println(" ");
								System.out.println(" ");
								
								File outputDir = Constants.RENDER_DATA_PATH.toFile();
								
								TileImageGenerator.generateTilesAsyncOptimized(2, outputDir, () -> {
									log("&aFinished verifiying rendered files and metadata!");
									
									System.out.println(" ");
									System.out.println(" ");
									log("&6 Sending rendered files and metadata to database..");
									System.out.println(" ");
									System.out.println(" ");
									
									FileUtil.sendRegionDataToBackendAsync(() -> {
										log("&aFinished sending rendered files and metadata to database.");
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
			});
		}
		
		new PlayerUpdateTask().runTaskTimer(this, Constants.PLAYER_UPDATE_TICKS, Constants.PLAYER_UPDATE_TICKS);
		log("&a" + Constants.PLUGIN_NAME + " v" + this.getDescription().getVersion() + " enabled!");
	}
	
	@Override
	public void onDisable() {
		log("&aSaving chunk data to files before disabling..");
		
		getChunkManager().saveNewlyExploredChunksToDisk();
		
		log("&a" + Constants.PLUGIN_NAME + " v" + this.getDescription().getVersion() + " disabled!");
	}
	
	public static ExploraPlugin getInstance() {
		if (instance == null) throw new IllegalStateException("Plugin not initialized yet!");
		return instance;
	}
	
	public static boolean hasLoadedChunks() {
		return chunksLoaded;
	}
	
	public static void log(String message) {
		Bukkit.getConsoleSender().sendMessage(StringUtils.colorize("[" + Constants.PLUGIN_NAME + "] " + message));
	}
	
	public static void warn(String message) {
		Bukkit.getConsoleSender().sendMessage(StringUtils.colorize("[" + Constants.PLUGIN_NAME + "] " + "&c [WARNING] " + message));
	}
	
	public static void debug(String message) {
		if (!Constants.DEBUG_MODE) return;
		Bukkit.getConsoleSender().sendMessage(StringUtils.colorize("[" + Constants.PLUGIN_NAME + "]" + "&8 [DEBUG] " + message));
	}
	
	public void registerEvents() {
		this.getServer().getPluginManager().registerEvents(new ChunkTracker(), this);
	}
	
	public CustomConfigurationFile getConfiguration() {
		return config;
	}
	
	public ChunkManager getChunkManager() {
		return chunkManager;
	}
	

}
