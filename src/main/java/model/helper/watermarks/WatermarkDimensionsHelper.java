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

    /**
     * Get or create cached FontMetrics for the given settings.
     * This is the single most impactful optimization: Font/FontMetrics creation
     * was happening on every mouse move event during drag operations.
     */
    private static FontMetrics getMetrics(WatermarkSettings settings) {
        int baseFontSize = 24;
        int fontSize = (int) (baseFontSize * settings.getFontSize());
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
     * Avoids the overhead of computing Font/FontMetrics twice when both are needed.
     */
    public static double[] calculateDimensions(WatermarkSettings settings) {
        if (settings.getType() == WatermarkSettings.WatermarkType.IMAGE) {
            double size = settings.getSize();
            return new double[]{size, size};
        }
        
        FontMetrics fm = getMetrics(settings);
        return new double[]{fm.stringWidth(settings.getText()), fm.getAscent() + fm.getDescent()};
    }

    /**
     * Calculate watermark dimensions capped to the given image bounds.
     * For an IMAGE type, the size is capped to the smaller of the image width/height
     * so the watermark never exceeds the base image dimensions.
     */
    public static double[] calculateDimensions(WatermarkSettings settings, BufferedImage image) {
        double[] dims = calculateDimensions(settings);
        if (image != null && settings.getType() == WatermarkSettings.WatermarkType.IMAGE) {
            double maxDim = Math.min(image.getWidth(), image.getHeight());
            if (dims[0] > maxDim) {
                return new double[]{maxDim, maxDim};
            }
        }
        return dims;
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
        FontMetrics fm = getMetrics(settings);
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
     * Single call replaces separate width/height calculations.
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
     *
     * <p>The watermark is centred on the click point and clamped to stay within the image
     * boundaries. The updated position is written back into {@code settings} and the custom
     * position flag is set to {@code true}.
     *
     * @param relX     Horizontal click position as a fraction of the image width  (0..1).
     * @param relY     Vertical   click position as a fraction of the image height (0..1).
     * @param settings Watermark settings to update in-place.
     * @param image    Source image used for boundary clamping.
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
