package com.jvallejoromero.explora.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.gson.Gson;
import com.jvallejoromero.explora.ExploraPlugin;

public class HttpUtil {

	private static final Gson GSON = new Gson();
	
	public static void deleteRequest(String targetUrl, Runnable onSuccess) {
	    Bukkit.getScheduler().runTaskAsynchronously(ExploraPlugin.getInstance(), () -> {
	        try {
	            URL url = new URL(targetUrl);
	            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	            
	            conn.setRequestMethod("DELETE");
	            conn.setRequestProperty("Content-Type", "application/json");
	            conn.setRequestProperty("x-api-key", Constants.BACKEND_API_KEY);
	            
	            int responseCode = conn.getResponseCode();

	            ExploraPlugin.debug("&6[HTTP] DELETE " + targetUrl + " => " + responseCode);

	            if (responseCode != 200) {
	                ExploraPlugin.warn("[HTTP] DELETE failed with code: " + responseCode);
	            }

	            conn.disconnect();

	            if (responseCode == 200 && onSuccess != null) {
	                Bukkit.getScheduler().runTask(ExploraPlugin.getInstance(), onSuccess);
	            }
	        } catch (Exception ex) {
	            ExploraPlugin.warn("[HTTP] Failed to DELETE: " + ex.getMessage());
	        }
	    });
	}
	
	public static void postJson(String targetUrl, String json, Runnable onComplete) {
		Bukkit.getScheduler().runTaskAsynchronously(ExploraPlugin.getInstance(), () -> {
			try {
				
				if (targetUrl.contains(buildUrl(Constants.BACKEND_CHUNK_BATCH_POST_URL))) {
					ExploraPlugin.debug("[HTTP] Sending chunks to " + targetUrl);
				} else if (!targetUrl.contains(buildUrl(Constants.BACKEND_PLAYER_POST_URL)) && !targetUrl.contains(buildUrl(Constants.BACKEND_SERVER_STATUS_POST_URL))){
					ExploraPlugin.debug("[HTTP] Sending to " + targetUrl + ": " + json);
				}
				
				URL url = new URL(targetUrl);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "application/json");
				conn.setRequestProperty("x-api-key", Constants.BACKEND_API_KEY);
				conn.setDoOutput(true);
				
				try (OutputStream os = conn.getOutputStream()) {
					os.write(json.getBytes(StandardCharsets.UTF_8));
				}

				int responseCode = conn.getResponseCode();
				if (responseCode != 200 && Constants.DEBUG_MODE) {
					ExploraPlugin.warn("[HTTP] POST failed with code: " + responseCode);
				}

				conn.disconnect();
				
				if (onComplete != null) {
					Bukkit.getScheduler().runTask(ExploraPlugin.getInstance(), onComplete); 
				}
			} catch (Exception ex) {
				ExploraPlugin.warn("[HTTP] Failed to POST: " + ex.getMessage());
				
			    if (onComplete != null) {
			        Bukkit.getScheduler().runTask(ExploraPlugin.getInstance(), onComplete);
			    }
			}
		});
	}
	
    public static void postZipBytes(byte[] zipBytes, Runnable onSuccess, boolean deleteExisting) {
    	try {
            String boundary = "----ExploraBoundary" + System.currentTimeMillis();
            String LINE_FEED = "\r\n";
            String backendUrl = buildUrl(Constants.BACKEND_UPLOAD_TILE_ZIP_URL);
            
            backendUrl += backendUrl.contains("?") ? "&" : "?";
            backendUrl += "deleteExisting=" + deleteExisting;

            HttpURLConnection conn = (HttpURLConnection) new URL(backendUrl).openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setRequestProperty("x-api-key", Constants.BACKEND_API_KEY);

            try (OutputStream output = conn.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true)) {

                writer.append("--").append(boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"tiles.zip\"").append(LINE_FEED);
                writer.append("Content-Type: application/zip").append(LINE_FEED);
                writer.append(LINE_FEED).flush();

                output.write(zipBytes);
                output.flush();

                writer.append(LINE_FEED).flush();
                writer.append("--").append(boundary).append("--").append(LINE_FEED).flush();
            }

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    ExploraPlugin.debug("&8[HTTP] Upload successful: " + reader.lines().reduce("", String::concat));
                    
                    if (onSuccess != null) {
                    	Bukkit.getScheduler().runTask(ExploraPlugin.getInstance(), onSuccess);
                    }
                }
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    ExploraPlugin.warn("[HTTP] Upload failed (" + status + "): " + reader.lines().reduce("", String::concat));
                }
            }
            conn.disconnect();
    	} catch (Exception ex) {
    		ExploraPlugin.warn("[HTTP] Failed to upload zip bytes: " + ex.getMessage());
    		ex.printStackTrace();
    	}
    }
	
	public static void uploadZipToBackendAsync(File zipFile) {
		Bukkit.getScheduler().runTaskAsynchronously(ExploraPlugin.getInstance(), () -> {
			try {
				String boundary = "----ExploraBoundary" + System.currentTimeMillis();
				String LINE_FEED = "\r\n";

				String targetUrl = buildUrl(Constants.BACKEND_UPLOAD_TILE_ZIP_URL);
				URL url = new URL(targetUrl);

				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setUseCaches(false);
				conn.setDoOutput(true);
				conn.setDoInput(true);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
				conn.setRequestProperty("x-api-key", Constants.BACKEND_API_KEY);

				try (OutputStream outputStream = conn.getOutputStream();
						PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {

					// Start multipart file part
					writer.append("--").append(boundary).append(LINE_FEED);
					writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"tiles.zip\"")
							.append(LINE_FEED);
					writer.append("Content-Type: application/zip").append(LINE_FEED);
					writer.append(LINE_FEED).flush();

					// Write file bytes
					Files.copy(zipFile.toPath(), outputStream);
					outputStream.flush();

					writer.append(LINE_FEED).flush();
					writer.append("--").append(boundary).append("--").append(LINE_FEED).flush();
				}

				int status = conn.getResponseCode();

				if (status == HttpURLConnection.HTTP_OK) {
					try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
						String responseLine;
						StringBuilder response = new StringBuilder();
						while ((responseLine = in.readLine()) != null) {
							response.append(responseLine);
						}
						ExploraPlugin.debug("&8[HTTP] Upload successful: " + response);
					}
				} else {
					try (InputStream errorStream = conn.getErrorStream()) {
						String errorMsg = errorStream != null ? new String(errorStream.readAllBytes()) : "(no error message)";
						ExploraPlugin.warn("[HTTP] Upload failed with status " + status + ": " + errorMsg);
					}
				}
				conn.disconnect();

			} catch (IOException e) {
				ExploraPlugin.warn("[HTTP] Upload failed: " + e.getMessage());
				e.printStackTrace();
			}
		});
	}
    
	public static void sendDeleteChunksRequest(Runnable onSuccess) {
		String targetUrl = buildUrl(Constants.BACKEND_DELETE_CHUNKS_URL);
		deleteRequest(targetUrl, onSuccess);
	}
	
	public static void sendChunkUpdate(World world, int x, int z, Runnable onComplete) {
		String worldName = world.getName();
		
		Map<String, Object> jsonMap = new HashMap<>();
		jsonMap.put("world", worldName);
		jsonMap.put("x", x);
		jsonMap.put("z", z);
		
		String json = GSON.toJson(jsonMap);
		String url = buildUrl(Constants.BACKEND_CHUNK_POST_URL);
		
		postJson(url, json, onComplete);
	}
	
	public static void sendBatchChunkUpdate(World world, List<ChunkCoord> chunks, Runnable onComplete) {
		String worldName = world.getName();
		Map<String, Object> jsonMap = new HashMap<>();
		
		jsonMap.put("world", worldName);
		jsonMap.put("chunks", chunks);
		
		String json = GSON.toJson(jsonMap);
		String url = buildUrl(Constants.BACKEND_CHUNK_BATCH_POST_URL);
		
		postJson(url, json, onComplete);
	}
	
	public static void streamChunkBatches(World world, List<ChunkCoord> chunkSet, int batchSize, long delayTicks, Runnable onBatchSent) {
	    List<ChunkCoord> allChunks = new ArrayList<>(chunkSet); 
	    int total = allChunks.size();
	    
	    new BukkitRunnable() {
	        int index = 0;
	        int batchCount = 0;
	        
	        @Override
	        public void run() {
	            if (index >= total) {
	            	this.cancel();
	            	ExploraPlugin.log("&aFinished streaming all " + total + " chunks to backend (" + batchCount + " batches) for world: " + world.getName());
	                return;
	            }

	            int end = Math.min(index + batchSize, total);
	            List<ChunkCoord> batch = allChunks.subList(index, end);
	            
	            sendBatchChunkUpdate(world, batch, onBatchSent);
	            index = end;
	            batchCount++;
	        }
	    }.runTaskTimer(ExploraPlugin.getInstance(), 0L, delayTicks);
	}

	/**
	 * @deprecated Use {@link #sendPlayerPositionUpdates(Set, Runnable)} to batch all players instead.
	 */
	@Deprecated
	public static void sendPlayerPositionUpdate(Player player, Runnable onComplete) {
		String name = player.getName();
		String world = player.getWorld().getName();
		int x = player.getLocation().getBlockX();
		int y = player.getLocation().getBlockY();
		int z = player.getLocation().getBlockZ();

	    Map<String, Object> jsonMap = new HashMap<>();
	    
	    jsonMap.put("name", name);
	    jsonMap.put("world", world);
	    jsonMap.put("x", x);
	    jsonMap.put("y", y);
	    jsonMap.put("z", z);
	    
	    String json = GSON.toJson(jsonMap);
		String url = buildUrl(Constants.BACKEND_PLAYER_POST_URL);
		
		postJson(url, json, onComplete);
	}
	
	public static void sendPlayerPositionUpdates(Set<PlayerStatus> players, Runnable onComplete) {

	    Map<String, Object> jsonMap = new HashMap<>();
	    jsonMap.put("online-players", players);
	    
	    String json = GSON.toJson(jsonMap);
		String url = buildUrl(Constants.BACKEND_PLAYER_POST_URL);
		
		postJson(url, json, onComplete);
	}
	
	
	public static void sendServerStatusUpdate(Runnable onComplete) {
		boolean isOnline = true;
		int playerCount = Bukkit.getOnlinePlayers().size();
		long worldTime = Bukkit.getWorlds().get(0).getTime();
		String motd = Bukkit.getServer().getMotd();
		
		Map<String, Object> jsonMap = new HashMap<>();
		
		jsonMap.put("isOnline", isOnline);
		jsonMap.put("playerCount", playerCount);
		jsonMap.put("worldTime", worldTime);
		jsonMap.put("motd", motd);
		
		String json = GSON.toJson(jsonMap);
		String url = buildUrl(Constants.BACKEND_SERVER_STATUS_POST_URL);
		postJson(url, json, onComplete);
	}
	
	private static String buildUrl(String template) {
	    return template.replace("%port%", String.valueOf(Constants.BACKEND_PORT));
	}

}
