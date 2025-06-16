package com.jvallejoromero.explora.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import com.jvallejoromero.explora.util.HttpUtil;

/**
 * A repeating task responsible for collecting and sending live server data to the backend server.
 *
 * <p>This task is scheduled at a configurable interval (defined in {@code config.yml}) and is used
 * to keep the backend synchronized with the current state of the server.
 *
 * <p>Currently, each server update includes:
 * <ul>
 *   <li>Online status</li>
 *   <li>Player count</li>
 *   <li>Current world time</li>
 *   <li>MOTD</li>
 * </ul>
 *
 * <p>Note: This class extends {@link BukkitRunnable} and is expected to be scheduled with
 * {@code runTaskTimer(...)} or {@code runTaskTimerAsynchronously(...)}.
 */
public class ServerStatusUpdateTask extends BukkitRunnable {

	@Override
	public void run() {
		HttpUtil.sendServerStatusUpdate(null);
	}
	
}
