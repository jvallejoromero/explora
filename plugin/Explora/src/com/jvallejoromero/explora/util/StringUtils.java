package com.jvallejoromero.explora.util;

import org.bukkit.ChatColor;

/**
 * Utility class for string manipulation and formatting within the {@code Explora} plugin.
 *
 * <p>Provides convenience methods for:
 * <ul>
 *   <li>Translating color codes using {@code '&'} notation</li>
 *   <li>Stripping color codes from strings</li>
 *   <li>Capitalizing words</li>
 * </ul>
 *
 * <p>All color methods are Spigot-compatible and based on {@link ChatColor}.
 */
public class StringUtils {
	
	/**
	 * Translates all color codes in the given string using {@code '&'} as the color character.
	 *
	 * <p>For example, {@code "&aHello"} becomes a green "Hello" in Minecraft chat.
	 *
	 * @param message the raw string with '&'-style color codes
	 * @return the colorized string using Minecraft formatting
	 */
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Removes all Minecraft color and formatting codes from the given string.
     *
     * @param message the string to strip formatting from
     * @return the plain-text version of the string
     */
    public static String stripColors(String message) {
        return ChatColor.stripColor(message);
    }
    
    /**
     * Capitalizes the first letter of the input string and converts the rest to lowercase.
     *
     * <p>If the input is {@code null} or empty, it is returned as-is.
     *
     * @param input the string to capitalize
     * @return the capitalized string, or the original input if null/empty
     */
    public static String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
    
}
