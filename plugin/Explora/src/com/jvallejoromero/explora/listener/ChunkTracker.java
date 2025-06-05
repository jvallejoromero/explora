package com.jvallejoromero.explora.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class ChunkTracker implements Listener {

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		//TODO: track loaded chunks as players move
	}
	
}
