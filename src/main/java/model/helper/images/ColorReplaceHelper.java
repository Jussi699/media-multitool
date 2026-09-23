package model.helper.images;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.HashMap;
import java.util.Map;

public class ColorReplaceHelper {
    private ColorReplaceHelper() {}

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
     * Replace a specific color in an image with a target color
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
        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        
        int[] pixels = ((DataBufferInt) result.getRaster().getDataBuffer()).getData();
        
        int[] srcRGB = hexToRGB(sourceHex);
        int sourceR = srcRGB[0];
        int sourceG = srcRGB[1];
        int sourceB = srcRGB[2];

        int[] tgtRGB = hexToRGB(targetHex);
        int targetR = tgtRGB[0];
        int targetG = tgtRGB[1];
        int targetB = tgtRGB[2];
        
        double threshold = smoothing * 2.55;
        double thresholdSq = threshold * threshold;
        double intensityFactor = intensity / 100.0;
        double enhancementFactor = enhancement / 100.0;
        
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int alpha = pixel & 0xFF000000;
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            
            int dr = r - sourceR;
            int dg = g - sourceG;
            int db = b - sourceB;
            double distSq = dr * dr + dg * dg + db * db;
            
            if (distSq <= thresholdSq) {
                double distance = Math.sqrt(distSq);
                double factor = (1.0 - distance / threshold) * intensityFactor;
                
                int newR = (int) (r + (targetR - r) * factor * enhancementFactor);
                int newG = (int) (g + (targetG - g) * factor * enhancementFactor);
                int newB = (int) (b + (targetB - b) * factor * enhancementFactor);
                
                newR = Math.clamp(newR, 0, 255);
                newG = Math.clamp(newG, 0, 255);
                newB = Math.clamp(newB, 0, 255);
                
                pixels[i] = alpha | (newR << 16) | (newG << 8) | newB;
            }
        }
        
        return result;
    }

    /**
     * Replace all colors in the image-shift the entire image towards the target color
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
        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();
        
        int[] pixels = ((DataBufferInt) result.getRaster().getDataBuffer()).getData();
        
        int[] tgtRGB = hexToRGB(targetHex);
        int targetR = tgtRGB[0];
        int targetG = tgtRGB[1];
        int targetB = tgtRGB[2];
        
        double factor = (intensity / 100.0) * (enhancement / 100.0);
        
        int[] lutR = new int[256];
        int[] lutG = new int[256];
        int[] lutB = new int[256];
        for (int v = 0; v < 256; v++) {
            lutR[v] = Math.clamp((int) (v + (targetR - v) * factor), 0, 255);
            lutG[v] = Math.clamp((int) (v + (targetG - v) * factor), 0, 255);
            lutB[v] = Math.clamp((int) (v + (targetB - v) * factor), 0, 255);
        }
        
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int alpha = pixel & 0xFF000000;
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            
            pixels[i] = alpha | (lutR[r] << 16) | (lutG[g] << 8) | lutB[b];
        }
        
        return result;
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
        if (color == null) {
            return "#FFFFFF";
        }
        return rgbToHex(color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Convert javafx.scene.paint.Color to HEX color string
     */
    public static String rgbToHex(javafx.scene.paint.Color color) {
        if (color == null) {
            return "#FFFFFF";
        }
        return rgbToHex((int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }
}
