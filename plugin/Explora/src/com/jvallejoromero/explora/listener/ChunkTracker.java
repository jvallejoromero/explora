package com.jvallejoromero.explora.listener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.jvallejoromero.explora.ExploraPlugin;
import com.jvallejoromero.explora.manager.ChunkManager;
import com.jvallejoromero.explora.util.BlockCoord;
import com.jvallejoromero.explora.util.ChunkCoord;
import com.jvallejoromero.explora.util.Constants;

public class ChunkTracker implements Listener {

	private static Map<ChunkCoord, Set<BlockCoord>> changedBlocks = new HashMap<>();
	
	@EventHandler
	public void onChunkExplore(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		
		int fromChunkX = event.getFrom().getBlockX() >> 4;
		int fromChunkZ = event.getFrom().getBlockZ() >> 4;

		int toChunkX = event.getTo().getBlockX() >> 4;
		int toChunkZ = event.getTo().getBlockZ() >> 4;

		if (fromChunkX == toChunkX && fromChunkZ == toChunkZ) return;
		
		ChunkManager chunkManager = ExploraPlugin.getInstance().getChunkManager();
		Chunk chunk = event.getTo().getChunk();
		
		int chunkX = chunk.getX();
		int chunkZ = chunk.getZ();
		
		boolean explored = chunkManager.isChunkExplored(player.getWorld().getName(), chunkX, chunkZ);
		
		if (!explored) {
		    chunkManager.recordChunkIfNew(player.getWorld().getName(), chunkX, chunkZ);
		}
	}
	
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Block placed = event.getBlock();
		
		if (!isNearTop(placed)) return;
		
		int blockX = placed.getLocation().getBlockX();
		int blockY = placed.getLocation().getBlockY();
		int blockZ = placed.getLocation().getBlockZ();
		
		BlockCoord blockCoord = new BlockCoord(blockX, blockY, blockZ);
		ChunkCoord chunkCoord = blockCoord.toChunkCoord();
		
		Set<BlockCoord> blocksChangedInChunk = changedBlocks.computeIfAbsent(chunkCoord, key -> new HashSet<>());
		if (blocksChangedInChunk.size() >= Constants.BLOCKS_CHANGED_PER_CHUNK_THRESHOLD) {
			// trigger re-render
			String world = placed.getLocation().getWorld().getName();
			ExploraPlugin.getInstance().getChunkManager().getNewlyExploredChunks().computeIfAbsent(world, key -> new HashSet<>()).add(chunkCoord);
			blocksChangedInChunk.clear();
			return;
		}
		
		blocksChangedInChunk.add(blockCoord);
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Block broken = event.getBlock();

		if (!isNearTop(broken)) return;
		
		int blockX = broken.getLocation().getBlockX();
		int blockY = broken.getLocation().getBlockY();
		int blockZ = broken.getLocation().getBlockZ();
		
		BlockCoord blockCoord = new BlockCoord(blockX, blockY, blockZ);
		ChunkCoord chunkCoord = blockCoord.toChunkCoord();
		
		Set<BlockCoord> blocksChangedInChunk = changedBlocks.computeIfAbsent(chunkCoord, key -> new HashSet<>());
		if (blocksChangedInChunk.size() >= Constants.BLOCKS_CHANGED_PER_CHUNK_THRESHOLD) {
			// trigger re-render
			String world = broken.getLocation().getWorld().getName();
			ExploraPlugin.getInstance().getChunkManager().getNewlyExploredChunks().computeIfAbsent(world, key -> new HashSet<>()).add(chunkCoord);
			blocksChangedInChunk.clear();
			return;
		}
		
		if (blocksChangedInChunk.contains(blockCoord)) {
			blocksChangedInChunk.remove(blockCoord);
			return;
		}
		
		blocksChangedInChunk.add(blockCoord);
	}
	
	private boolean isNearTop(Block block) {
		Location loc = block.getLocation();
		
		int blockY = loc.getBlockY();
		int highestY = loc.getWorld().getHighestBlockYAt(loc);
		
		return ((blockY >= highestY) || (blockY - highestY >= -1));
	}
	
}
