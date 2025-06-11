package com.jvallejoromero.explora.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PlayerStatus {

	private String name;
	private String world;
	
	private int x;
	private int y;
	private int z;
	
	private float yaw;
	
	public PlayerStatus(Player player) {
		Location loc = player.getLocation();
		this.name = player.getName();
		this.world = player.getWorld().getName();
		this.x = loc.getBlockX();
		this.y = loc.getBlockY();
		this.z = loc.getBlockZ();
		this.yaw = loc.getYaw();
	}
	
	public String getName() {
		return name;
	}
	
	public String getWorld() {
		return world;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getZ() {
		return z;
	}
	
	public float getYaw() {
		return yaw;
	}
}
