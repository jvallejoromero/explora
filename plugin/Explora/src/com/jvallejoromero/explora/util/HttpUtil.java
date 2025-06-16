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
import org.bukkit.scheduler.BukkitRunnable;

import com.google.gson.Gson;
import com.jvallejoromero.explora.ExploraPlugin;

/**
 * Utility class for sending asynchronous HTTP requests to the backend server used by the {@code Explora} plugin.
 *
 * <p>Supports:
 * <ul>
 *   <li>Sending JSON-based {@code POST} and {@code DELETE} requests</li>
 *   <li>Uploading ZIP files as multipart form data</li>
 *   <li>Streaming chunk updates in batches</li>
 *   <li>Sending player location and server status updates</li>
 * </ul>
 *
 * <p>All methods use Bukkit's async task scheduler to avoid blocking the main server thread.
 * Endpoints, keys, and configurations are injected from the {@link Constants} class.
 */
public class HttpUtil {

	private static final Gson GSON = new Gson();
	
	/**
	 * Sends a {@code DELETE} request asynchronously to the given backend URL.
	 *
	 * @param targetUrl the endpoint to delete from
	 * @param onSuccess optional callback to run on the main thread if the response is successful (HTTP 200)
	 */
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
	
	/**
	 * Sends a JSON {@code POST} request asynchronously to the given backend URL.
	 *
	 * @param targetUrl the endpoint to send the request to
	 * @param json the JSON-encoded request body
	 * @param onComplete an optional callback to run on the main thread after the request completes
	 */
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
	
	/**
	 * Sends ZIP file data as a multipart/form-data {@code POST} request to the backend.
	 *
	 * @param zipBytes the ZIP archive bytes to send
	 * @param onSuccess callback to run on the main thread if the upload succeeds
	 * @param deleteExisting if {@code true}, tells the backend to delete previously uploaded data before saving
	 */
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
	
    /**
     * Asynchronously uploads a ZIP file to the backend using multipart/form-data encoding.
     *
     * <p>This is an alternative to {@link #postZipBytes(byte[], Runnable, boolean)} and works with pre-zipped files on disk.
     *
     * @param zipFile the ZIP file to upload
     */
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
    
	/**
	 * Sends a {@code DELETE} request to the backend to remove all previously stored chunk data.
	 *
	 * @param onSuccess optional callback to run after successful deletion
	 */
	public static void sendDeleteChunksRequest(Runnable onSuccess) {
		String targetUrl = buildUrl(Constants.BACKEND_DELETE_CHUNKS_URL);
		deleteRequest(targetUrl, onSuccess);
	}
	
	/**
	 * Sends a single chunk coordinate update to the backend.
	 *
	 * <p>Use this for updating one chunk's explored status.
	 *
	 * @param world the world the chunk belongs to
	 * @param x the chunk's X coordinate
	 * @param z the chunk's Z coordinate
	 * @param onComplete callback to run after request completion
	 */
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
	
	/**
	 * Sends a batch of chunk coordinates for a single world to the backend.
	 *
	 * @param world the world these chunks belong to
	 * @param chunks the list of {@link ChunkCoord} objects to send
	 * @param onComplete callback to run after request completion
	 */
	public static void sendBatchChunkUpdate(World world, List<ChunkCoord> chunks, Runnable onComplete) {
		String worldName = world.getName();
		Map<String, Object> jsonMap = new HashMap<>();
		
		jsonMap.put("world", worldName);
		jsonMap.put("chunks", chunks);
		
		String json = GSON.toJson(jsonMap);
		String url = buildUrl(Constants.BACKEND_CHUNK_BATCH_POST_URL);
		
		postJson(url, json, onComplete);
	}
	
	/**
	 * Streams chunk updates to the backend in timed batches to reduce load.
	 *
	 * <p>This method splits a large chunk set into batches of a given size, then schedules each batch
	 * to be sent at the specified interval.
	 *
	 * @param world the world the chunks belong to
	 * @param chunkSet the list of chunk coordinates to send
	 * @param batchSize how many chunks per batch
	 * @param delayTicks how many ticks between batch sends
	 * @param onBatchSent optional callback after each batch is sent
	 */
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
	 * Sends a batched update of all currently online player locations to the backend.
	 *
	 * @param players a set of {@link PlayerStatus} objects representing player states
	 * @param onComplete callback to run after request completion
	 */
	public static void sendPlayerPositionUpdates(Set<PlayerStatus> players, Runnable onComplete) {

	    Map<String, Object> jsonMap = new HashMap<>();
	    jsonMap.put("online-players", players);
	    
	    String json = GSON.toJson(jsonMap);
		String url = buildUrl(Constants.BACKEND_PLAYER_POST_URL);
		
		postJson(url, json, onComplete);
	}
	
	/**
	 * Sends a snapshot of the current server status (online status, player count, time, MOTD)
	 * to the backend server for monitoring or display purposes.
	 *
	 * @param onComplete callback to run after request completion
	 */
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
	
	/**
	 * Replaces the placeholder {@code %port%} in a backend URL template with the actual backend port.
	 *
	 * @param template the URL template from {@link Constants}
	 * @return the resolved URL string
	 */
	private static String buildUrl(String template) {
	    return template.replace("%port%", String.valueOf(Constants.BACKEND_PORT));
	}

}
