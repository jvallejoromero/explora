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
import org.bukkit.World;

import com.jvallejoromero.explora.ExploraPlugin;

public class FileUtil {
	
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
	

    public static File getPrimaryRegionFolderForWorld(World world) {
        File baseDir = world.getWorldFolder();
        String name = world.getName().toLowerCase();

        if (name.contains("nether")) {
            // world_nether -> world_nether/DIM-1/region
            return new File(baseDir, "DIM-1/region");
        } else if (name.contains("end")) {
            // world_the_end -> world_the_end/DIM1/region
            return new File(baseDir, "DIM1/region");
        } else {
            // Overworld -> world/region
            return new File(baseDir, "region");
        }
    }
}
