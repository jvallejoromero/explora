package com.jvallejoromero.explora.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Represents a lightweight snapshot of a player's current in-game state.
 *
 * <p>This class is used to send player location and orientation data to the backend.
 * It captures:
 * <ul>
 *   <li>Player name</li>
 *   <li>World name</li>
 *   <li>Block-level position (X, Y, Z)</li>
 *   <li>Yaw orientation (facing direction)</li>
 * </ul>
 *
 * <p>Intended for serialization and backend reporting — does not include full {@link Player} data.
 *
 * @see com.jvallejoromero.explora.util.HttpUtil#sendPlayerPositionUpdates
 */
public class PlayerStatus {

	private String name;
	private String world;
	
	private int x;
	private int y;
	private int z;
	
	private float yaw;
	
	/**
	 * Creates a new {@code PlayerStatus} object from a given {@link Player} instance.
	 *
	 * @param player the player to extract location and identity data from
	 */
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
