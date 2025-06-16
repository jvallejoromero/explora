package com.jvallejoromero.explora.yaml;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.YamlConfiguration;

import com.jvallejoromero.explora.ExploraPlugin;

/**
 * Utility class for creating and managing custom YAML configuration files within the {@code Explora} plugin.
 *
 * <p>This class wraps Spigot's {@link YamlConfiguration} and provides a simplified interface for:
 * <ul>
 *   <li>Loading YAML files from disk or the plugin JAR</li>
 *   <li>Setting and saving configuration values</li>
 *   <li>Auto-creating missing configuration files and directories</li>
 * </ul>
 *
 * <p>All configuration files are stored relative to the plugin's {@code dataFolder}.
 */
public class CustomConfigurationFile {
	
	private final ExploraPlugin instance = ExploraPlugin.getInstance();
	
	private File customFile;
	private YamlConfiguration ymlConfig;


	/**
	 * Loads (or creates) a custom YAML configuration file from the given relative path.
	 *
	 * @param path the relative file path inside the plugin's data folder (e.g., {@code "settings/config.yml"})
	 */
	public CustomConfigurationFile(String path) {
		customFile = new File(instance.getDataFolder() + File.separator + path);
		
		if (!customFile.exists()) {
			customFile.getParentFile().mkdirs();
			try {
				customFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		ymlConfig = YamlConfiguration.loadConfiguration(customFile);
	}
	
	/**
	 * Loads (or creates) a YAML configuration file from the given relative path,
	 * optionally copying it from the plugin JAR on first creation.
	 *
	 * @param path the relative file path inside the plugin's data folder
	 * @param fromJar whether to copy the file from the plugin JAR
	 */
	public CustomConfigurationFile(String path, boolean fromJar) {
		customFile = new File(instance.getDataFolder() + File.separator + path);
		
		if (!customFile.exists()) {
		    customFile.getParentFile().mkdirs(); 
		    if (fromJar) {
		        instance.saveResource(path, false);
		    } else {
		        try {
		            customFile.createNewFile();
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
		    }
		}
		ymlConfig = YamlConfiguration.loadConfiguration(customFile);
	}
	
	/**
	 * @return the loaded {@link YamlConfiguration} object for direct access to configuration data
	 */
	public YamlConfiguration yml() {
		return ymlConfig;
	}
	
	/**
	 * Sets a value in the configuration without saving it to disk immediately.
	 *
	 * @param path the path to the config key
	 * @param value the value to set
	 */
	public void set(String path, Object value) {
		ymlConfig.set(path, value);
	}
	
	/**
	 * Sets a value in the configuration and optionally saves it immediately.
	 *
	 * @param path the path to the config key
	 * @param value the value to set
	 * @param autoSave if {@code true}, immediately saves the config file after setting the value
	 */
	public void set(String path, Object value, boolean autoSave) {
		ymlConfig.set(path, value);
		if (autoSave) {
			this.save();
		}
	}
	
	/**
	 * Saves the current configuration to disk.
	 *
	 * <p>If the file cannot be written, logs and prints the stack trace.
	 */
    public void save() {
        try {
            ymlConfig.save(customFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
