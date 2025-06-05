package com.jvallejoromero.explora;

import org.bukkit.plugin.java.JavaPlugin;

import com.jvallejoromero.explora.listener.ChunkTracker;

public class ExploraPlugin extends JavaPlugin {
	
	public static final String PLUGIN_NAME = "Explora";
	
	private static ExploraPlugin instance;
	
	@Override
	public void onEnable() {
		instance = this;
		this.registerEvents();
		
		log(PLUGIN_NAME + " v" + this.getDescription().getVersion() + " enabled!");
	}
	
	@Override
	public void onDisable() {
		log(PLUGIN_NAME + " v" + this.getDescription().getVersion() + " disabled!");
	}
	
	public void registerEvents() {
		this.getServer().getPluginManager().registerEvents(new ChunkTracker(), this);
	}
	
	public static ExploraPlugin getInstance() {
		if (instance == null) throw new IllegalStateException("Plugin not initialized yet!");
		return instance;
	}
	
	public static void log(String message) {
		getInstance().getLogger().info(message);
	}
	

}
