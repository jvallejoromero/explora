package com.jvallejoromero.explora;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.jvallejoromero.explora.listener.ChunkTracker;
import com.jvallejoromero.explora.manager.ChunkManager;
import com.jvallejoromero.explora.util.ChunkUtils;
import com.jvallejoromero.explora.util.Constants;
import com.jvallejoromero.explora.util.StringUtils;
import com.jvallejoromero.explora.yaml.CustomConfigurationFile;

public class ExploraPlugin extends JavaPlugin {
	
	private static ExploraPlugin instance;
	private static ChunkManager chunkManager;
	
	private static boolean chunksLoaded = false;
	private static boolean scanRegionFiles;
	
	private CustomConfigurationFile config;
	
	@Override
	public void onEnable() {
		
		instance = this;
		config = new CustomConfigurationFile("config.yml", true);
		scanRegionFiles = getConfiguration().yml().getBoolean("scan-region-files");
		chunkManager = new ChunkManager(this);
		
		Constants.init(this);
		
		this.registerEvents();
		
		if (scanRegionFiles) {
			log("&6&oWarning: scan-region-files has been set to true. Please wait while world folders are scanned for region files. This may take a while.");
			try {
				ChunkUtils.scanWorldsAsync(() -> {
					getConfiguration().set("scan-region-files", false);
					getConfiguration().save();
					
					ExploraPlugin.log("&a World folder scan completed.");
					
					chunkManager.init();
					chunksLoaded = true;
				});
			} catch (Exception ex) {
				warn("An error ocurred while parsing chunk data.");
			} 
		} else {
			chunkManager.init();
			chunksLoaded = true;
		}
		
		log("&a" + Constants.PLUGIN_NAME + " v" + this.getDescription().getVersion() + " enabled!");
	}
	
	@Override
	public void onDisable() {
		log("&asaving chunk data to files before disabling..");
		
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
		Bukkit.getConsoleSender().sendMessage(StringUtils.colorize("[" + Constants.PLUGIN_NAME + "] " + "&c" + message));
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
