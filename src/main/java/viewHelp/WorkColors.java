package viewHelp;

import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import model.logger.ErrorLogger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class WorkColors {
    private WorkColors() {
        /* This utility class should not be instantiated */
    }

    /**
     * Opening the graphical interface (View) button selects the color.
     */
    public static void updateColorView(java.awt.Color awtColor, Button btnColorPicker) {
        if(isNull(awtColor)) {
            ErrorLogger.error("java.awt.Color is null");
            return;
        }

        String hexColor = rgbToHex(awtColor);
        boolean darkBackground = isDark(awtColor);

        btnColorPicker.setStyle("""
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-min-width: 100;
                -fx-min-height: 30;
                -fx-border-color: black;
                -fx-border-radius: 5;
                -fx-background-radius: 5;
                """.formatted(hexColor, darkBackground ? "white" : "black"));
    }

    public static boolean isDark(java.awt.Color color) {
        if (isNull(color)) return false;
        double luma = 0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue();
        return luma < 128;
    }

    public static java.awt.Color toAwtColor(Color fxColor) {
        if (isNull(fxColor)) return null;
        return new java.awt.Color(
                (float) fxColor.getRed(),
                (float) fxColor.getGreen(),
                (float) fxColor.getBlue(),
                (float) fxColor.getOpacity()
        );
    }

    public static Color toFxColor(java.awt.Color awtColor) {
        if (isNull(awtColor)) return null;
        return Color.rgb(
                awtColor.getRed(),
                awtColor.getGreen(),
                awtColor.getBlue(),
                awtColor.getAlpha() / 255.0
        );
    }

    /**
     * Parse RGB components from HEX color string (#RRGGBB or RRGGBB)
     * @param hex HEX color string
     * @return Array of [R, G, B] values (0-255)
     */
    public static int[] hexToRGB(String hex) {
        if (!isValidHex(hex)) {
            throw new IllegalArgumentException("Invalid HEX color format: " + hex);
        }

        int rgb = Integer.parseInt(hex.startsWith("#") ? hex.substring(1) : hex, 16);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        return new int[]{r, g, b};
    }

    /**
     * Convert RGB components to HEX color string (e.g. "#FF0000")
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     * @return HEX color string (e.g., "#FF0000")
     */
    public static String rgbToHex(int r, int g, int b) {
        return String.format("#%02X%02X%02X",
                Math.clamp(r, 0, 255),
                Math.clamp(g, 0, 255),
                Math.clamp(b, 0, 255));
    }

    /**
     * Convert java.awt.Color to HEX color string
     */
    public static String rgbToHex(java.awt.Color color) {
        if (isNull(color)) {
            return "#FFFFFF";
        }
        return rgbToHex(color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Convert javafx.scene.paint.Color to HEX color string
     */
    public static String rgbToHex(javafx.scene.paint.Color color) {
        if (isNull(color)) {
            return "#FFFFFF";
        }
        return rgbToHex((int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    public static boolean isValidHex(String hex) {
        return nonNull(hex) && hex.matches("#?[0-9A-Fa-f]{6}");
    }
}
