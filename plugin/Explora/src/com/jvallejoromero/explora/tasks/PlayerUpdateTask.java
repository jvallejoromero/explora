package com.jvallejoromero.explora.tasks;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.jvallejoromero.explora.util.HttpUtil;
import com.jvallejoromero.explora.util.PlayerStatus;

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
