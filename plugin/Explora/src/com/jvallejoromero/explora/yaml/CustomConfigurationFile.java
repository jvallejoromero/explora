package com.jvallejoromero.explora.yaml;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.YamlConfiguration;

import com.jvallejoromero.explora.ExploraPlugin;

public class CustomConfigurationFile {
	
	private final ExploraPlugin instance = ExploraPlugin.getInstance();
	
	private File customFile;
	private YamlConfiguration ymlConfig;

	
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
	
	public YamlConfiguration yml() {
		return ymlConfig;
	}
	
	public void set(String path, Object value) {
		ymlConfig.set(path, value);
	}
	
	public void set(String path, Object value, boolean autoSave) {
		ymlConfig.set(path, value);
		this.save();
	}
	
    public void save() {
        try {
            ymlConfig.save(customFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
