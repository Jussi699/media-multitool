package media_multitool.watermarks;

import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;

public class WatermarkDimensionsHelper {
    private static final int BASE_FONT_SIZE = 24;
    
    /**
     * Calculate watermark width based on settings
     */
    public static double calculateWidth(WatermarkSettings settings) {
        if (settings.getType() == WatermarkSettings.WatermarkType.IMAGE) {
            return settings.getSize();
        }
        
        double sizeMultiplier = settings.getFontSize();
        int fontSize = (int) (BASE_FONT_SIZE * sizeMultiplier);
        Font font = new Font(settings.getFontName(), Font.BOLD, fontSize);
        FontMetrics fm = new Canvas().getFontMetrics(font);
        return fm.stringWidth(settings.getText());
    }
    
    /**
     * Calculate watermark height based on settings
     */
    public static double calculateHeight(WatermarkSettings settings) {
        if (settings.getType() == WatermarkSettings.WatermarkType.IMAGE) {
            return settings.getSize();
        }
        
        double sizeMultiplier = settings.getFontSize();
        int fontSize = (int) (BASE_FONT_SIZE * sizeMultiplier);
        Font font = new Font(settings.getFontName(), Font.BOLD, fontSize);
        FontMetrics fm = new Canvas().getFontMetrics(font);
        return fm.getHeight();
    }
    
    /**
     * Check if watermark can be dragged based on current settings
     */
    public static boolean canDrag(WatermarkSettings settings, BufferedImage image) {
        if (image == null || settings.getType() == WatermarkSettings.WatermarkType.NONE) {
            return false;
        }
        
        if (settings.getType() == WatermarkSettings.WatermarkType.TEXT) {
            return "single".equals(settings.getTilePattern());
        }
        
        if (settings.getType() == WatermarkSettings.WatermarkType.IMAGE) {
            return !settings.isTileMode() && settings.getWatermarkImage() != null;
        }
        
        return false;
    }
    
    /**
     * Initialize default center position for watermark
     */
    public static void initDefaultPosition(WatermarkSettings settings, BufferedImage image) {
        if (image == null) {
            return;
        }
        
        double wmWidth = calculateWidth(settings);
        double wmHeight = calculateHeight(settings);
        double posX = (image.getWidth() - wmWidth) / 2;
        double posY = (image.getHeight() - wmHeight) / 2;
        
        settings.setPositionX(Math.max(0, posX));
        settings.setPositionY(Math.max(0, posY));
        settings.setUseCustomPosition(true);
    }
    
    /**
     * Get current watermark position, calculating default if needed
     */
    public static double[] getCurrentPosition(WatermarkSettings settings, BufferedImage image) {
        double wmWidth = calculateWidth(settings);
        double wmHeight = calculateHeight(settings);
        
        double posX, posY;
        if (settings.isUseCustomPosition()) {
            posX = settings.getPositionX();
            posY = settings.getPositionY();
        } else {
            posX = (image.getWidth() - wmWidth) / 2;
            posY = (image.getHeight() - wmHeight) / 2;
        }
        
        return new double[]{posX, posY, wmWidth, wmHeight};
    }
}
