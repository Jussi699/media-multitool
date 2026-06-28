package model.helper.watermarks;

import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;

public class WatermarkDimensionsHelper {
    private static String cachedFontName;
    private static int cachedFontSize;
    private static Font cachedFont;
    private static FontMetrics cachedMetrics;
    
    /**
     * Get or create cached FontMetrics for the given settings.
     * This is the single most impactful optimization: Font/FontMetrics creation
     * was happening on every mouse move event during drag operations.
     */
    private static FontMetrics getMetrics(WatermarkSettings settings) {
        int base_font_size = 24;
        int fontSize = (int) (base_font_size * settings.getFontSize());
        String fontName = settings.getFontName();
        
        if (cachedFont == null || cachedFontSize != fontSize || !fontName.equals(cachedFontName)) {
            cachedFontName = fontName;
            cachedFontSize = fontSize;
            cachedFont = new Font(fontName, Font.BOLD, fontSize);

            Canvas SHARED_CANVAS = new Canvas();
            cachedMetrics = SHARED_CANVAS.getFontMetrics(cachedFont);
        }
        
        return cachedMetrics;
    }
    
    /**
     * Calculate watermark dimensions (width and height) in a single call.
     * Avoids the overhead of computing Font/FontMetrics twice when both are needed.
     */
    public static double[] calculateDimensions(WatermarkSettings settings) {
        if (settings.getType() == WatermarkSettings.WatermarkType.IMAGE) {
            double size = settings.getSize();
            return new double[]{size, size};
        }
        
        FontMetrics fm = getMetrics(settings);
        return new double[]{fm.stringWidth(settings.getText()), fm.getHeight()};
    }
    
    /**
     * Calculate watermark width based on settings
     */
    public static double calculateWidth(WatermarkSettings settings) {
        if (settings.getType() == WatermarkSettings.WatermarkType.IMAGE) {
            return settings.getSize();
        }
        return getMetrics(settings).stringWidth(settings.getText());
    }
    
    /**
     * Calculate watermark height based on settings
     */
    public static double calculateHeight(WatermarkSettings settings) {
        if (settings.getType() == WatermarkSettings.WatermarkType.IMAGE) {
            return settings.getSize();
        }
        return getMetrics(settings).getHeight();
    }
    
    /**
     * Check if watermark can be dragged based on current settings
     */
    public static boolean canDrag(WatermarkSettings settings, BufferedImage image) {
        if (image == null || settings.getType() == WatermarkSettings.WatermarkType.NONE) {
            return false;
        }
        return settings.isSingleMode() && 
               (settings.getType() != WatermarkSettings.WatermarkType.IMAGE || settings.getWatermarkImage() != null);
    }
    
    /**
     * Initialize default center position for watermark
     */
    public static void initDefaultPosition(WatermarkSettings settings, BufferedImage image) {
        if (image == null) {
            return;
        }
        
        double[] dims = calculateDimensions(settings);
        double posX = (image.getWidth() - dims[0]) / 2;
        double posY = (image.getHeight() - dims[1]) / 2;
        
        settings.setPositionX(Math.max(0, posX));
        settings.setPositionY(Math.max(0, posY));
        settings.setUseCustomPosition(true);
    }
    
    /**
     * Get current watermark position and dimensions as [posX, posY, width, height].
     * Single call replaces separate width/height calculations.
     */
    public static double[] getCurrentPosition(WatermarkSettings settings, BufferedImage image) {
        double[] dims = calculateDimensions(settings);
        
        double posX, posY;
        if (settings.isUseCustomPosition()) {
            posX = settings.getPositionX();
            posY = settings.getPositionY();
        } else {
            posX = (image.getWidth() - dims[0]) / 2;
            posY = (image.getHeight() - dims[1]) / 2;
        }
        
        return new double[]{posX, posY, dims[0], dims[1]};
    }
}
