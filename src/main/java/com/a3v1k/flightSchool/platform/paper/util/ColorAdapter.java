package com.a3v1k.flightSchool.platform.paper.util;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;

/**
 * Conversions between Bukkit colour types and the Adventure colour API.
 *
 * <p>Used to render team-coloured display names in chat / titles. {@code Team.color}
 * is currently {@link org.bukkit.Color}; this class hides the channel extraction
 * boilerplate at the call site.</p>
 */
@UtilityClass
public class ColorAdapter {

    /**
     * Convert a Bukkit RGB colour to an Adventure {@link TextColor}.
     */
    public static TextColor toAdventureColor(Color color) {
        return TextColor.color(color.getRed(), color.getGreen(), color.getBlue());
    }
}
