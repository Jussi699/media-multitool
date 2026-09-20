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

    private WatermarkDimensionsHelper() {}

    public static double getScale(BufferedImage image) {
        if (image == null) return 1.0;
        return Math.min(image.getWidth(), image.getHeight()) / 1000.0;
    }

    public static int computeFontSize(WatermarkSettings settings, BufferedImage image) {
        double scale = getScale(image);
        int baseFontSize = Math.max(10, (int) Math.round(24 * scale));
        return Math.max(8, (int) Math.round(baseFontSize * settings.getFontSize()));
    }

    /**
     * Get or create cached FontMetrics for the given settings and image scale.
     */
    private static FontMetrics getMetrics(WatermarkSettings settings, BufferedImage image) {
        int fontSize = computeFontSize(settings, image);
        String fontName = settings.getFontName();
        
        if (cachedFont == null || cachedFontSize != fontSize || !fontName.equals(cachedFontName)) {
            cachedFontName = fontName;
            cachedFontSize = fontSize;
            cachedFont = new Font(fontName, Font.BOLD, fontSize);

            Canvas sharedCanvas = new Canvas();
            cachedMetrics = sharedCanvas.getFontMetrics(cachedFont);
        }
        
        return cachedMetrics;
    }
    
    /**
     * Calculate watermark dimensions (width and height) in a single call.
     */
    public static double[] calculateDimensions(WatermarkSettings settings) {
        return calculateDimensions(settings, null);
    }

    /**
     * Calculate watermark dimensions capped to the given image bounds.
     */
    public static double[] calculateDimensions(WatermarkSettings settings, BufferedImage image) {
        if (settings.getType() == WatermarkSettings.WatermarkType.IMAGE) {
            double scale = getScale(image);
            double size = settings.getSize() * scale;
            if (image != null) {
                double maxDim = Math.min(image.getWidth(), image.getHeight());
                if (size > maxDim) {
                    return new double[]{maxDim, maxDim};
                }
            }
            return new double[]{size, size};
        }
        
        FontMetrics fm = getMetrics(settings, image);
        return new double[]{fm.stringWidth(settings.getText()), fm.getAscent() + fm.getDescent()};
    }
    
    /**
     * Calculate watermark width based on settings
     */
    public static double calculateWidth(WatermarkSettings settings) {
        return calculateWidth(settings, null);
    }

    public static double calculateWidth(WatermarkSettings settings, BufferedImage image) {
        if (settings.getType() == WatermarkSettings.WatermarkType.IMAGE) {
            return settings.getSize() * getScale(image);
        }
        return getMetrics(settings, image).stringWidth(settings.getText());
    }
    
    /**
     * Calculate watermark height based on settings
     */
    public static double calculateHeight(WatermarkSettings settings) {
        return calculateHeight(settings, null);
    }

    public static double calculateHeight(WatermarkSettings settings, BufferedImage image) {
        if (settings.getType() == WatermarkSettings.WatermarkType.IMAGE) {
            return settings.getSize() * getScale(image);
        }
        FontMetrics fm = getMetrics(settings, image);
        return fm.getAscent() + fm.getDescent();
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
        
        double[] dims = calculateDimensions(settings, image);
        double posX = (image.getWidth() - dims[0]) / 2;
        double posY = (image.getHeight() - dims[1]) / 2;
        
        settings.setPositionX(Math.max(0, posX));
        settings.setPositionY(Math.max(0, posY));
        settings.setUseCustomPosition(true);
    }
    
    /**
     * Get current watermark position and dimensions as [posX, posY, width, height].
     */
    public static double[] getCurrentPosition(WatermarkSettings settings, BufferedImage image) {
        double[] dims = calculateDimensions(settings, image);
        
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

    /**
     * Position the watermark at the given relative image coordinates (0–1 range).
     */
    public static void applyRelativePosition(double relX, double relY, WatermarkSettings settings, BufferedImage image) {
        if (image == null) {
            return;
        }

        double[] dims = calculateDimensions(settings, image);
        double halfW = dims[0] / 2;
        double halfH = dims[1] / 2;

        double x = Math.clamp(relX * image.getWidth()  - halfW, 0, Math.max(0, image.getWidth()  - dims[0]));
        double y = Math.clamp(relY * image.getHeight() - halfH, 0, Math.max(0, image.getHeight() - dims[1]));

        settings.setPositionX(x);
        settings.setPositionY(y);
        settings.setUseCustomPosition(true);
    }
}
