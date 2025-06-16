package com.jvallejoromero.explora.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bukkit.Bukkit;

import com.jvallejoromero.explora.ExploraPlugin;

/**
 * Utility class for zipping files/folders and sending tile or region data to the backend.
 *
 * <p>This class provides:
 * <ul>
 *   <li>Methods for zipping individual files or entire folder trees into a byte array</li>
 *   <li>Asynchronous methods for uploading zipped tile or render data to a configured backend endpoint</li>
 *   <li>Multi-threaded batching for performance on large rerendered tile uploads</li>
 * </ul>
 *
 * <p>All I/O and network operations are run off the main thread using Bukkit's async scheduler
 * to avoid blocking the Minecraft server.
 *
 * @see HttpUtil
 * @see RegionCoord
 */
public class FileUtil {
	
	/**
	 * Compresses a list of files into a ZIP archive and returns the result as a byte array.
	 *
	 * @param files the list of files to zip
	 * @return a byte array containing the zipped file contents
	 * @throws IOException if any I/O error occurs during zipping
	 */
	public static byte[] zipFilesToBytes(List<File> files) throws IOException {
	    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
	    try (ZipOutputStream zipOut = new ZipOutputStream(byteOut)) {
	        for (File file : files) {
	            try (FileInputStream fis = new FileInputStream(file)) {
	                ZipEntry entry = new ZipEntry(file.getParentFile().getName() + "/" + file.getName());
	                zipOut.putNextEntry(entry);

	                byte[] buffer = new byte[8192];
	                int len;
	                while ((len = fis.read(buffer)) > 0) {
	                    zipOut.write(buffer, 0, len);
	                }

	                zipOut.closeEntry();
	            }
	        }
	    }
	    return byteOut.toByteArray();
	}
	
	/**
	 * Compresses multiple folder trees into a ZIP archive and returns it as a byte array.
	 *
	 * <p>All non-directory files under the given folders (recursively) are added to the archive
	 * using their relative paths.
	 *
	 * @param worldDirs a list of top-level directories 
	 * @return a byte array of the zipped folder contents, or {@code null} if zipping fails
	 */
	public static byte[] zipFoldersToBytes(List<File> worldDirs) {
		try {
			ByteArrayOutputStream byteOut = null;
			OutputStream zipStream;

			byteOut = new ByteArrayOutputStream();
			zipStream = new BufferedOutputStream(byteOut);

			try (ZipOutputStream zipOut = new ZipOutputStream(zipStream)) {
				for (File sourceDir : worldDirs) {
					if (!sourceDir.isDirectory())
						continue;

					Path basePath = sourceDir.getParentFile().toPath();
					Files.walk(sourceDir.toPath()).filter(path -> !Files.isDirectory(path)).forEach(path -> {
						try {
							String relativePath = basePath.relativize(path).toString().replace(File.separatorChar, '/');
							ZipEntry zipEntry = new ZipEntry(relativePath);
							zipOut.putNextEntry(zipEntry);
							Files.copy(path, zipOut);
							zipOut.closeEntry();
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					});
				}
			}

			return byteOut.toByteArray();
		} catch (IOException e) {
			ExploraPlugin.warn("Failed to zip and upload: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Asynchronously compresses the entire contents of the render data folder
	 * (typically all world region folders) and sends the resulting ZIP to the backend.
	 *
	 * <p>After sending, the optional {@code onComplete} callback will be run on the main thread.
	 *
	 * @param onComplete a Runnable to run on the main thread after sending is complete; may be {@code null}
	 */
	public static void sendRegionDataToBackendAsync(Runnable onComplete) {
		Bukkit.getScheduler().runTaskAsynchronously(ExploraPlugin.getInstance(), () -> {
			File renderDataFolder = Constants.RENDER_DATA_PATH.toFile();
			File[] subdirs = renderDataFolder.listFiles(File::isDirectory);
			
			if (subdirs == null || subdirs.length == 0) {
			    ExploraPlugin.warn("No world folders found to zip.");
			    return;
			}
			
			byte[] zipBytes = zipFoldersToBytes(Arrays.asList(subdirs));
			HttpUtil.postZipBytes(zipBytes, onComplete, true);
		});
	}
    
	/**
	 * Asynchronously collects and zips updated PNG and JSON tile files for re-rendered regions,
	 * then sends them to the backend in a single ZIP archive.
	 *
	 * <p>Uses a thread pool to build the file list and stream the ZIP in the background. After the
	 * operation completes or fails, the provided {@code onComplete} callback will be run on the main thread.
	 *
	 * @param regions a map of world names to sets of {@link RegionCoord} objects that were updated
	 * @param onComplete an optional Runnable to invoke after the upload completes
	 */
	public static void sendRerenderedTilesToBackendAsync(Map<String, Set<RegionCoord>> regions, Runnable onComplete) {
		Bukkit.getScheduler().runTaskAsynchronously(ExploraPlugin.getInstance(), () -> {
	        int cores = Runtime.getRuntime().availableProcessors();
	        int maxThreads = Math.max(2, (int) Math.ceil(cores * 0.5));
	        
		    ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
		    List<File> filesToSend = new ArrayList<>();

		    for (Map.Entry<String, Set<RegionCoord>> entry : regions.entrySet()) {
		        String world = entry.getKey();
		        for (RegionCoord region : entry.getValue()) {
		            String baseName = "r." + region.getX() + "." + region.getZ();
		            File pngFile = Constants.RENDER_DATA_PATH.resolve(world).resolve(baseName + ".png").toFile();
		            File jsonFile = Constants.RENDER_DATA_PATH.resolve(world).resolve(baseName + ".json").toFile();

		            if (pngFile.exists()) filesToSend.add(pngFile);
		            if (jsonFile.exists()) filesToSend.add(jsonFile);
		        }
		    }

		    if (filesToSend.isEmpty()) {
		        ExploraPlugin.log("&6No updated PNG or JSON tiles to send to backend.");
		        if (onComplete != null) Bukkit.getScheduler().runTask(ExploraPlugin.getInstance(), onComplete);
		        return;
		    }

		    executor.submit(() -> {
		        try {
		            byte[] zipBytes = FileUtil.zipFilesToBytes(filesToSend);
		            HttpUtil.postZipBytes(zipBytes, null, false);
		        } catch (IOException e) {
		            ExploraPlugin.warn("Failed to zip/send rendered tiles: " + e.getMessage());
		            e.printStackTrace();
		        } finally {
		            executor.shutdown();
				    if (onComplete != null) {
				    	Bukkit.getScheduler().runTask(ExploraPlugin.getInstance(), onComplete);
				    }
		        }
		    });
		});
	}
}
