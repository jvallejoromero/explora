package com.jvallejoromero.explora.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import com.jvallejoromero.explora.util.HttpUtil;

public class ServerStatusUpdateTask extends BukkitRunnable {

	@Override
	public void run() {
		HttpUtil.sendServerStatusUpdate(null);
	}
	
}
