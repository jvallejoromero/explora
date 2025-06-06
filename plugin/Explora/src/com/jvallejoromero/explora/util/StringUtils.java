package com.jvallejoromero.explora.util;

import org.bukkit.ChatColor;

public class StringUtils {
	
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static String stripColors(String message) {
        return ChatColor.stripColor(message);
    }
    
    public static String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
    
}
