package com.jvallejoromero.explora.util;

import java.nio.file.Path;

import com.jvallejoromero.explora.ExploraPlugin;
import com.jvallejoromero.explora.yaml.CustomConfigurationFile;

public class Constants {
	
    public static final String PLUGIN_NAME = "Explora";
    
    public static Path SAVE_PATH;
    public static Path RENDER_DATA_PATH;
    public static boolean SHOULD_SCAN_FOLDERS;
    public static boolean DEBUG_MODE;
    
    public static long CHUNK_UPDATE_TICKS;
    public static long PLAYER_UPDATE_TICKS;
    public static long SERVER_STATUS_UPDATE_TICKS;
    
    public static int BACKEND_PORT;
    public static String BACKEND_API_KEY;
    public static int BACKEND_CHUNK_BATCH_POST_DELAY_TICKS;
    public static int BACKEND_CHUNK_BATCH_SIZE;
    public static String BACKEND_CHUNK_POST_URL;
    public static String BACKEND_CHUNK_BATCH_POST_URL;
    public static String BACKEND_PLAYER_POST_URL;
    public static String BACKEND_SERVER_STATUS_POST_URL;
    public static String BACKEND_DELETE_CHUNKS_URL;
    public static String BACKEND_UPLOAD_TILE_ZIP_URL;

    public static void init(ExploraPlugin plugin) {
        CustomConfigurationFile config = plugin.getConfiguration();
        SAVE_PATH = plugin.getDataFolder().toPath().resolve(config.yml().getString("chunk-data-folder"));
        RENDER_DATA_PATH = plugin.getDataFolder().toPath().resolve(config.yml().getString("render-data-folder"));
        SHOULD_SCAN_FOLDERS = config.yml().getBoolean("scan-region-files");
        DEBUG_MODE = config.yml().getBoolean("debug-mode");
        CHUNK_UPDATE_TICKS = config.yml().getLong("chunk-update-ticks");
        PLAYER_UPDATE_TICKS = config.yml().getLong("player-update-ticks");
        SERVER_STATUS_UPDATE_TICKS = config.yml().getLong("server-status-update-ticks");
        BACKEND_PORT = config.yml().getInt("backend-port");
        BACKEND_API_KEY = config.yml().getString("backend-api-key");
        BACKEND_CHUNK_BATCH_POST_DELAY_TICKS = config.yml().getInt("backend-chunk-batch-post-delay-ticks");
        BACKEND_CHUNK_BATCH_SIZE = config.yml().getInt("backend-chunk-batch-size");
        BACKEND_CHUNK_POST_URL = config.yml().getString("backend-chunk-post-url");
        BACKEND_CHUNK_BATCH_POST_URL = config.yml().getString("backend-chunk-batch-post-url");
        BACKEND_PLAYER_POST_URL = config.yml().getString("backend-player-post-url");
        BACKEND_SERVER_STATUS_POST_URL = config.yml().getString("backend-server-status-update-url");
        BACKEND_DELETE_CHUNKS_URL = config.yml().getString("backend-delete-chunks-url");
        BACKEND_UPLOAD_TILE_ZIP_URL = config.yml().getString("backend-upload-tile-zip-url");
    }
    
}
