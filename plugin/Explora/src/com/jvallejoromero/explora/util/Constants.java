package com.jvallejoromero.explora.util;

import java.nio.file.Path;

import com.jvallejoromero.explora.ExploraPlugin;
import com.jvallejoromero.explora.yaml.CustomConfigurationFile;

public class Constants {
	
    public static final String PLUGIN_NAME = "Explora";
    public static Path SAVE_PATH;
    public static long CHUNK_UPDATE_TICKS;

    public static void init(ExploraPlugin plugin) {
        CustomConfigurationFile config = plugin.getConfiguration();
        SAVE_PATH = plugin.getDataFolder().toPath().resolve(config.yml().getString("chunk-data-folder"));
        CHUNK_UPDATE_TICKS = config.yml().getLong("chunk-update-ticks");
    }
    
}
