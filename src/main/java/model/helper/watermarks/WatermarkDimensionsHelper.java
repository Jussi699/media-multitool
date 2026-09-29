package model.helper.watermarks;

import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class WatermarkDimensionsHelper {
    private static String cachedFontName;
    private static int cachedFontSize;
    private static Font cachedFont;
    private static FontMetrics cachedMetrics;

    private WatermarkDimensionsHelper() {}

    public static double getScale(BufferedImage image) {
        if (isNull(image)) return 1.0;
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
        
        if (isNull(cachedFont) || cachedFontSize != fontSize || !fontName.equals(cachedFontName)) {
            cachedFontName = fontName;
            cachedFontSize = fontSize;
            cachedFont = new Font(fontName, Font.BOLD, fontSize);

            Canvas sharedCanvas = new Canvas();
            cachedMetrics = sharedCanvas.getFontMetrics(cachedFont);
        }
        
        return cachedMetrics;
    }

    private static Rectangle2D getTextVisualBounds(WatermarkSettings settings, BufferedImage image) {
        Font font = getMetrics(settings, image).getFont();
        FontRenderContext context = new FontRenderContext(null, true, true);
        GlyphVector glyphs = font.createGlyphVector(context, settings.getText());
        return glyphs.getVisualBounds();
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
            double width = settings.getSize() * scale * settings.getResizeScaleX();
            double height = settings.getSize() * scale * settings.getResizeScaleY();
            if (nonNull(image)) {
                width = Math.min(width, image.getWidth());
                height = Math.min(height, image.getHeight());
            }
            return new double[]{width, height};
        }
        
        Rectangle2D bounds = getTextVisualBounds(settings, image);
        return new double[]{
            bounds.getWidth() * settings.getResizeScaleX(),
            bounds.getHeight() * settings.getResizeScaleY()
        };
    }
    
    /**
     * Calculate watermark width based on settings
     */
    public static double calculateWidth(WatermarkSettings settings) {
        return calculateWidth(settings, null);
    }

    public static double calculateWidth(WatermarkSettings settings, BufferedImage image) {
        if (settings.getType() == WatermarkSettings.WatermarkType.IMAGE) {
            double width = settings.getSize() * getScale(image) * settings.getResizeScaleX();
            return nonNull(image) ? Math.min(width, image.getWidth()) : width;
        }
        return getTextVisualBounds(settings, image).getWidth() * settings.getResizeScaleX();
    }
    
    /**
     * Calculate watermark height based on settings
     */
    public static double calculateHeight(WatermarkSettings settings) {
        return calculateHeight(settings, null);
    }

    public static double calculateHeight(WatermarkSettings settings, BufferedImage image) {
        if (settings.getType() == WatermarkSettings.WatermarkType.IMAGE) {
            double height = settings.getSize() * getScale(image) * settings.getResizeScaleY();
            return nonNull(image) ? Math.min(height, image.getHeight()) : height;
        }
        return getTextVisualBounds(settings, image).getHeight() * settings.getResizeScaleY();
    }

    public static Rectangle2D calculateTextVisualBounds(WatermarkSettings settings, BufferedImage image) {
        return getTextVisualBounds(settings, image);
    }
    
    /**
     * Check if watermark can be dragged based on current settings
     */
    public static boolean canDrag(WatermarkSettings settings, BufferedImage image) {
        if (isNull(image) || settings.getType() == WatermarkSettings.WatermarkType.NONE) {
            return false;
        }
        return settings.isSingleMode() && 
               (settings.getType() != WatermarkSettings.WatermarkType.IMAGE || settings.getWatermarkImage() != null);
    }
    
    /**
     * Initialize the default center position for the watermark
     */
    public static void initDefaultPosition(WatermarkSettings settings, BufferedImage image) {
        if (isNull(image)) {
            return;
        }
        
        double[] dims = calculateDimensions(settings, image);
        double posX   = (image.getWidth() - dims[0]) / 2;
        double posY   = (image.getHeight() - dims[1]) / 2;
        
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
        if (isNull(image)) {
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
