package com.jvallejoromero.explora.tasks;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.jvallejoromero.explora.util.HttpUtil;
import com.jvallejoromero.explora.util.PlayerStatus;

/**
 * A repeating task responsible for collecting and sending live player data to the backend server.
 *
 * <p>This task is scheduled at a configurable interval (defined in {@code config.yml}) and is used
 * to keep the backend synchronized with the current state of all online players.
 *
 * <p>Currently, each player update includes:
 * <ul>
 *   <li>Player name</li>
 *   <li>Current world</li>
 *   <li>Coordinates (x, y, z)</li>
 *   <li>Yaw orientation</li>
 *   <li>Online status</li>
 * </ul>
 *
 * <p>All updates are serialized into {@link PlayerStatus} objects and streamed to the backend
 * using {@link HttpUtil#sendPlayerPositionUpdates(Set, Runnable)}.
 *
 * <p>Note: This class extends {@link BukkitRunnable} and is expected to be scheduled with
 * {@code runTaskTimer(...)} or {@code runTaskTimerAsynchronously(...)}.
 */
public class PlayerUpdateTask extends BukkitRunnable {
	
	@Override
	public void run() {
		Set<PlayerStatus> playerUpdates = new HashSet<PlayerStatus>();
	    
		for (Player player : Bukkit.getOnlinePlayers()) {
	     playerUpdates.add(new PlayerStatus(player));
	    }
	    
	    HttpUtil.sendPlayerPositionUpdates(playerUpdates, null);
	}

}
