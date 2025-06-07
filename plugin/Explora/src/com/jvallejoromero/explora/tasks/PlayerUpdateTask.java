package com.jvallejoromero.explora.tasks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.jvallejoromero.explora.util.HttpUtil;

public class PlayerUpdateTask extends BukkitRunnable {
	
	@Override
	public void run() {
	    for (Player player : Bukkit.getOnlinePlayers()) {
	        HttpUtil.sendPlayerPositionUpdate(player, null);
	    }
	}

}
