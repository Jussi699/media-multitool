package model.helper.images;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.HashMap;
import java.util.Map;
import static viewHelp.WorkColors.hexToRGB;

public class ColorReplaceHelper {
    private ColorReplaceHelper() {
        /* This utility class should not be instantiated */
    }

    private static final Map<String, String> COLOR_HEX_MAP = new HashMap<>();
    
    static {
        putColorToMap("Red", "#FF0000");
        putColorToMap("Green", "#00FF00");
        putColorToMap("Blue", "#0000FF");
        putColorToMap("Yellow", "#FFFF00");
        putColorToMap("Cyan", "#00FFFF");
        putColorToMap("Magenta", "#FF00FF");
        putColorToMap("White", "#FFFFFF");
        putColorToMap("Black", "#000000");
        putColorToMap("Orange", "#FFA500");
        putColorToMap("Purple", "#800080");
    }

    private static void putColorToMap(String key, String value) {
        COLOR_HEX_MAP.put(key, value);
    }

    public static String getHexFromColorName(String colorName) {
        return COLOR_HEX_MAP.getOrDefault(colorName, "#FF0000");
    }

    public static String[] getAvailableColorNames() {
        return COLOR_HEX_MAP.keySet().toArray(new String[0]);
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
    

}
