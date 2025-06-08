package com.jvallejoromero.explora.util.mcaselector;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.jvallejoromero.explora.ExploraPlugin;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkRenderer;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.java_1_21.ColorMapping_24w18a;

public final class VersionHandler {

	private VersionHandler() {}

	public static final Map<Class<?>, TreeMap<Integer, Object>> implementations = new HashMap<>();

	public static void init() {
		implementations.computeIfAbsent(ChunkRenderer.class, k -> new TreeMap<>())
	    .put(4325, new ChunkRenderer_1_21());
		
		implementations.computeIfAbsent(ColorMapping.class, k -> new TreeMap<>())
	    .put(4325, new ColorMapping_24w18a());
		
		ExploraPlugin.log("&7MCASelector Implementations Loaded");
	}

	@SuppressWarnings("unchecked")
	public static <T> T getImpl(int dataVersion, Class<T> clazz) {
		TreeMap<Integer, Object> implementation = implementations.get(clazz);
		if (implementation == null) {
			throw new IllegalArgumentException("no implementation for " + clazz);
		}
		Map.Entry<Integer, Object> e = implementation.floorEntry(dataVersion);
		if (e == null) {
			throw new IllegalArgumentException("no implementation for " + clazz + " with version " + dataVersion);
		}
		if (!clazz.isAssignableFrom(e.getValue().getClass())) {
			throw new IllegalArgumentException("wrong implementation for " + clazz + " with version " + dataVersion + ": " + e.getValue().getClass());
		}
		return (T) e.getValue();
	}

	public static <T> T getImpl(ChunkData data, Class<T> clazz) {
		if (data == null) {
			throw new IllegalArgumentException("chunk data is null");
		}
		int dataVersion = 0;
		if (data.region() != null && data.region().getData() != null) {
			dataVersion = Helper.getDataVersion(data.region().getData());
		} else if (data.entities() != null && data.entities().getData() != null) {
			dataVersion = Helper.getDataVersion(data.entities().getData());
		} else if (data.poi() != null && data.poi().getData() != null) {
			dataVersion = Helper.getDataVersion(data.poi().getData());
		}
		return getImpl(dataVersion, clazz);
	}
	
	public static void printAllImplementations() {
		for (Map.Entry<Class<?>, TreeMap<Integer, Object>> entry : implementations.entrySet()) {
			Class<?> interfaceClass = entry.getKey();
			TreeMap<Integer, Object> versionMap = entry.getValue();

			System.out.println("Implementations for: " + interfaceClass.getSimpleName());
			for (Map.Entry<Integer, Object> impl : versionMap.entrySet()) {
				System.out.println("  Version: " + impl.getKey() + " → " + impl.getValue().getClass().getSimpleName());
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static ColorMapping getColorMapping(int dataVersion) {
	    TreeMap<Integer, Object> versions = implementations.get(ColorMapping.class);
	    if (versions == null) return null;

	    Map.Entry<Integer, Object> entry = versions.floorEntry(dataVersion);
	    if (entry != null && entry.getValue() instanceof ColorMapping) {
	        return (ColorMapping) entry.getValue();
	    }
	    return null;
	}
	
	@SuppressWarnings("rawtypes")
	public static ChunkRenderer getChunkRenderer(int dataVersion) {
	    TreeMap<Integer, Object> versions = implementations.get(ChunkRenderer.class);
	    if (versions == null) return null;

	    Map.Entry<Integer, Object> entry = versions.floorEntry(dataVersion);
	    if (entry != null && entry.getValue() instanceof ChunkRenderer) {
	        return (ChunkRenderer) entry.getValue();
	    }
	    return null;
	}

}
