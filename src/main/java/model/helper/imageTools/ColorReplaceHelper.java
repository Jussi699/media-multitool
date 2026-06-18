package model.helper.imageTools;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class ColorReplaceHelper {
    
    private static final Map<String, String> COLOR_HEX_MAP = new HashMap<>();
    
    static {
        COLOR_HEX_MAP.put("Red", "#FF0000");
        COLOR_HEX_MAP.put("Green", "#00FF00");
        COLOR_HEX_MAP.put("Blue", "#0000FF");
        COLOR_HEX_MAP.put("Yellow", "#FFFF00");
        COLOR_HEX_MAP.put("Cyan", "#00FFFF");
        COLOR_HEX_MAP.put("Magenta", "#FF00FF");
        COLOR_HEX_MAP.put("White", "#FFFFFF");
        COLOR_HEX_MAP.put("Black", "#000000");
        COLOR_HEX_MAP.put("Orange", "#FFA500");
        COLOR_HEX_MAP.put("Purple", "#800080");
    }

    public static String getHexFromColorName(String colorName) {
        return COLOR_HEX_MAP.getOrDefault(colorName, "#FF0000");
    }

    public static String[] getAvailableColorNames() {
        return COLOR_HEX_MAP.keySet().toArray(new String[0]);
    }

    public static boolean isValidHex(String hex) {
        return hex != null && hex.matches("#[0-9A-Fa-f]{6}");
    }
    
    /**
     * Replace specific color in image with target color
     * @param source Source image
     * @param sourceHex Source color in HEX format
     * @param targetHex Target color in HEX format
     * @param intensity Intensity of replacement (0-100)
     * @param smoothing Smoothing of color transitions (0-50)
     * @param enhancement Enhancement of target color (0-100)
     * @return Processed image
     */
    public static BufferedImage replaceColor(BufferedImage source, String sourceHex, String targetHex, 
                                              double intensity, int smoothing, int enhancement) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        int sourceRGB = Integer.parseInt(sourceHex.substring(1), 16);
        int targetRGB = Integer.parseInt(targetHex.substring(1), 16);
        
        int sourceR = (sourceRGB >> 16) & 0xFF;
        int sourceG = (sourceRGB >> 8) & 0xFF;
        int sourceB = sourceRGB & 0xFF;
        
        int targetR = (targetRGB >> 16) & 0xFF;
        int targetG = (targetRGB >> 8) & 0xFF;
        int targetB = targetRGB & 0xFF;
        
        double threshold = smoothing * 2.55;
        double intensityFactor = intensity / 100.0;
        double enhancementFactor = enhancement / 100.0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = source.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                
                double distance = Math.sqrt(
                    Math.pow(r - sourceR, 2) + 
                    Math.pow(g - sourceG, 2) + 
                    Math.pow(b - sourceB, 2)
                );
                
                if (distance <= threshold) {
                    double factor = (1.0 - distance / threshold) * intensityFactor;
                    
                    int newR = (int) (r + (targetR - r) * factor * enhancementFactor);
                    int newG = (int) (g + (targetG - g) * factor * enhancementFactor);
                    int newB = (int) (b + (targetB - b) * factor * enhancementFactor);
                    
                    newR = Math.clamp(newR, 0, 255);
                    newG = Math.clamp(newG, 0, 255);
                    newB = Math.clamp(newB, 0, 255);
                    
                    int newPixel = (alpha << 24) | (newR << 16) | (newG << 8) | newB;
                    result.setRGB(x, y, newPixel);
                } else {
                    result.setRGB(x, y, pixel);
                }
            }
        }
        
        return result;
    }

    /**
     * Replace all colors in image - shift entire image towards target color
     * @param source Source image
     * @param targetHex Target color in HEX format
     * @param intensity Intensity of replacement (0-100)
     * @param enhancement Enhancement of target color (0-100)
     * @return Processed image
     */
    public static BufferedImage replaceAllColors(BufferedImage source, String targetHex, 
                                                   double intensity, int enhancement) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        int targetRGB = Integer.parseInt(targetHex.substring(1), 16);
        
        int targetR = (targetRGB >> 16) & 0xFF;
        int targetG = (targetRGB >> 8) & 0xFF;
        int targetB = targetRGB & 0xFF;
        
        double intensityFactor = intensity / 100.0;
        double enhancementFactor = enhancement / 100.0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = source.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                
                int newR = (int) (r + (targetR - r) * intensityFactor * enhancementFactor);
                int newG = (int) (g + (targetG - g) * intensityFactor * enhancementFactor);
                int newB = (int) (b + (targetB - b) * intensityFactor * enhancementFactor);
                
                newR = Math.clamp(newR, 0, 255);
                newG = Math.clamp(newG, 0, 255);
                newB = Math.clamp(newB, 0, 255);
                
                int newPixel = (alpha << 24) | (newR << 16) | (newG << 8) | newB;
                result.setRGB(x, y, newPixel);
            }
        }
        
        return result;
    }
    
    /**
     * Parse RGB components from HEX color
     * @param hex HEX color string
     * @return Array of [R, G, B] values (0-255)
     */
    public static int[] hexToRGB(String hex) {
        if (!isValidHex(hex)) {
            throw new IllegalArgumentException("Invalid HEX color format: " + hex);
        }
        
        int rgb = Integer.parseInt(hex.substring(1), 16);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        
        return new int[]{r, g, b};
    }
    
    /**
     * Convert RGB components to HEX color string
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
}
