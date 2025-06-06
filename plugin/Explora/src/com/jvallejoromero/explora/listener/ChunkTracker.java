package com.jvallejoromero.explora.listener;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import com.jvallejoromero.explora.ExploraPlugin;
import com.jvallejoromero.explora.manager.ChunkManager;
import com.jvallejoromero.explora.util.StringUtils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ChunkTracker implements Listener {

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		
		int fromChunkX = event.getFrom().getBlockX() >> 4;
		int fromChunkZ = event.getFrom().getBlockZ() >> 4;

		int toChunkX = event.getTo().getBlockX() >> 4;
		int toChunkZ = event.getTo().getBlockZ() >> 4;

		if (fromChunkX == toChunkX && fromChunkZ == toChunkZ) return;
		
		ChunkManager chunkManager = ExploraPlugin.getInstance().getChunkManager();
		Chunk chunk = player.getLocation().getChunk();
		
		int chunkX = chunk.getX();
		int chunkZ = chunk.getZ();
		
		boolean explored = chunkManager.isChunkExplored(player.getWorld().getName(), chunkX, chunkZ);
		sendActionBarMessage(player, "Chunk Explored: " + explored);
		
		if (!explored) {
		    chunkManager.markChunkAsExplored(player.getWorld().getName(), chunkX, chunkZ);

		    if (ExploraPlugin.hasLoadedChunks()) {
		        player.sendMessage(StringUtils.colorize("&aYou explored a new chunk!"));
		    }
		}
	}
	
    private void sendActionBarMessage(Player player, String message) {
   	 player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(StringUtils.colorize(message)));
   }
	
}
