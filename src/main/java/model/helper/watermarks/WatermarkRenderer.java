package model.helper.watermarks;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class WatermarkRenderer {
    /**
     * Apply watermark to base image based on settings
     */
    public static BufferedImage applyWatermark(BufferedImage baseImage, WatermarkSettings settings) {
        if (settings == null || settings.getType() == WatermarkSettings.WatermarkType.NONE) {
            return baseImage;
        }
        if (baseImage == null) {
            return null;
        }
        
        BufferedImage result = copyImage(baseImage);
        
        try {
            if (settings.getType() == WatermarkSettings.WatermarkType.TEXT) {
                applyTextWatermark(result, settings);
            } else if (settings.getType() == WatermarkSettings.WatermarkType.IMAGE) {
                applyImageWatermark(result, settings);
            }
        } catch (Exception e) {
            System.err.println("Error applying watermark: " + e.getMessage());
        }
        
        return result;
    }
    
    private static void applyTextWatermark(BufferedImage image, WatermarkSettings settings) {
        Graphics2D g2d = image.createGraphics();
        
        try {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_SPEED);

            int base_font_size = 24;
            int fontSize = (int) (base_font_size * settings.getFontSize());
            Font font = new Font(settings.getFontName(), Font.BOLD, fontSize);
            g2d.setFont(font);
            
            Color color = settings.getTextColor();
            Color colorWithAlpha = new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                (int) Math.min(255, settings.getOpacity() * 2.55)
            );
            g2d.setColor(colorWithAlpha);
            
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(settings.getText());
            int textHeight = fm.getHeight();
            
            double rotation = Math.toRadians(settings.getRotation());
            
            TextEffect effect = resolveEffect(settings.getEffect());
            
            switch (settings.getTilePattern()) {
                case "single"  -> renderTextSingle(g2d, image, settings, textWidth, textHeight, colorWithAlpha, rotation, effect);
                case "grid"    -> renderTextTiled(g2d, image, settings, textWidth, textHeight, colorWithAlpha, rotation, effect, false);
                case "diamond" -> renderTextTiled(g2d, image, settings, textWidth, textHeight, colorWithAlpha, rotation, effect, true);
            }
        } finally {
            g2d.dispose();
        }
    }
    
    /**
     * Resolved effect type to avoid string comparisons per tile
     */
    private enum TextEffect { NONE, SHADOW, OUTLINE, GLOW }
    
    private static TextEffect resolveEffect(String effect) {
        if (effect == null) return TextEffect.NONE;
        return switch (effect.toLowerCase()) {
            case "shadow"  -> TextEffect.SHADOW;
            case "outline" -> TextEffect.OUTLINE;
            case "glow"    -> TextEffect.GLOW;
            default        -> TextEffect.NONE;
        };
    }
    
    private static void renderTextSingle(Graphics2D g2d, BufferedImage image, WatermarkSettings settings,
                                         int textWidth, int textHeight, Color color, double rotation, TextEffect effect) {
        int x, y;
        if (settings.isUseCustomPosition()) {
            x = (int) settings.getPositionX();
            y = (int) settings.getPositionY() + textHeight;
        } else {
            x = (image.getWidth() - textWidth) / 2;
            y = (image.getHeight() + textHeight) / 2;
        }
        
        double centerX = settings.isUseCustomPosition() ? x + textWidth / 2.0 : image.getWidth() / 2.0;
        double centerY = settings.isUseCustomPosition() ? y - textHeight / 2.0 : image.getHeight() / 2.0;
        
        AffineTransform transform = new AffineTransform();
        transform.translate(centerX, centerY);
        transform.rotate(rotation);
        transform.translate(-centerX, -centerY);
        
        g2d.setTransform(transform);
        drawTextWithEffect(g2d, settings.getText(), x, y, effect, color);
    }
    
    /**
     * Unified grid/diamond tiled text rendering. 
     * Reuses a single AffineTransform instance to avoid thousands of allocations on large images.
     */
    private static void renderTextTiled(Graphics2D g2d, BufferedImage image, WatermarkSettings settings,
                                        int textWidth, int textHeight, Color color, double rotation,
                                        TextEffect effect, boolean diamond) {
        int spacing = (int) (settings.getSpacing() * 50);
        int stepX = textWidth + spacing;
        int stepY = textHeight + spacing;
        
        String text = settings.getText();
        AffineTransform transform = new AffineTransform();
        
        boolean offset = false;
        for (int y = textHeight; y < image.getHeight(); y += stepY) {
            int startX = (diamond && offset) ? stepX / 2 : 0;
            for (int x = startX; x < image.getWidth(); x += stepX) {
                transform.setToIdentity();
                transform.translate(x + textWidth / 2.0, y);
                transform.rotate(rotation);
                transform.translate(-textWidth / 2.0, 0);
                
                g2d.setTransform(transform);
                drawTextWithEffect(g2d, text, 0, 0, effect, color);
            }
            if (diamond) offset = !offset;
        }
    }
    
    /**
     * Draw text with pre-resolved effect type (avoids string comparison per tile)
     */
    private static void drawTextWithEffect(Graphics2D g2d, String text, int x, int y, 
                                           TextEffect effect, Color color) {
        switch (effect) {
            case SHADOW  -> drawShadowEffect(g2d, text, x, y, color);
            case OUTLINE -> drawOutlineEffect(g2d, text, x, y, color);
            case GLOW    -> drawGlowEffect(g2d, text, x, y, color);
            default      -> {
                g2d.setColor(color);
                g2d.drawString(text, x, y);
            }
        }
    }
    
    private static void drawShadowEffect(Graphics2D g2d, String text, int x, int y, Color color) {
        g2d.setColor(new Color(0, 0, 0, color.getAlpha()));
        g2d.drawString(text, x + 2, y + 2);
        g2d.setColor(color);
        g2d.drawString(text, x, y);
    }
    
    private static void drawOutlineEffect(Graphics2D g2d, String text, int x, int y, Color color) {
        Color outlineColor = new Color(0, 0, 0, color.getAlpha());
        g2d.setColor(outlineColor);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    g2d.drawString(text, x + dx, y + dy);
                }
            }
        }
        g2d.setColor(color);
        g2d.drawString(text, x, y);
    }
    
    private static void drawGlowEffect(Graphics2D g2d, String text, int x, int y, Color color) {
        int alphaDiv = Math.max(1, color.getAlpha() / 3);
        Color glowColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alphaDiv);
        g2d.setColor(glowColor);
        for (int i = 3; i > 0; i--) {
            g2d.drawString(text, x - i, y - i);
            g2d.drawString(text, x + i, y - i);
            g2d.drawString(text, x - i, y + i);
            g2d.drawString(text, x + i, y + i);
        }
        g2d.setColor(color);
        g2d.drawString(text, x, y);
    }
    
    private static void applyImageWatermark(BufferedImage image, WatermarkSettings settings) {
        if (settings.getWatermarkImage() == null) {
            return;
        }
        
        Graphics2D g2d = image.createGraphics();
        
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_SPEED);
            
            int size = (int) settings.getSize();
            float opacity = (float) Math.min(1.0, settings.getOpacity() / 100.0);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            
            double rotation = Math.toRadians(settings.getRotation());
            
            if (settings.isTileMode()) {
                renderImageTiled(g2d, image, settings, size, rotation);
            } else {
                renderImageSingle(g2d, image, settings, size, rotation);
            }
        } finally {
            g2d.dispose();
        }
    }
    
    /**
     * Unified tiled image rendering (grid + diamond).
     * Reuses AffineTransform instance to avoid excessive allocation.
     */
    private static void renderImageTiled(Graphics2D g2d, BufferedImage image, WatermarkSettings settings,
                                         int size, double rotation) {
        int step = size + (int) settings.getSpacing();
        boolean diamond = "diamond".equals(settings.getTilePattern());
        BufferedImage wmImage = settings.getWatermarkImage();
        AffineTransform transform = new AffineTransform();
        
        boolean offset = false;
        for (int y = 0; y < image.getHeight(); y += step) {
            int startX = (diamond && offset) ? step / 2 : 0;
            for (int x = startX; x < image.getWidth(); x += step) {
                transform.setToIdentity();
                transform.translate(x + size / 2.0, y + size / 2.0);
                transform.rotate(rotation);
                transform.translate(-size / 2.0, -size / 2.0);
                
                g2d.setTransform(transform);
                g2d.drawImage(wmImage, 0, 0, size, size, null);
            }
            if (diamond) offset = !offset;
        }
    }
    
    private static void renderImageSingle(Graphics2D g2d, BufferedImage image, WatermarkSettings settings,
                                          int size, double rotation) {
        int x, y;
        if (settings.isUseCustomPosition()) {
            x = (int) settings.getPositionX();
            y = (int) settings.getPositionY();
        } else {
            x = (image.getWidth() - size) / 2;
            y = (image.getHeight() - size) / 2;
        }
        
        AffineTransform transform = new AffineTransform();
        transform.translate(x + size / 2.0, y + size / 2.0);
        transform.rotate(rotation);
        transform.translate(-size / 2.0, -size / 2.0);
        
        g2d.setTransform(transform);
        g2d.drawImage(settings.getWatermarkImage(), 0, 0, size, size, null);
    }
    
    /**
     * Copy image preserving the source type when possible.
     * TYPE_INT_ARGB is used only if the source has alpha or is a custom type.
     * OPTIMIZED: Use TYPE_INT_RGB for faster rendering when no alpha is needed.
     */
    private static BufferedImage copyImage(BufferedImage source) {
        int type = source.getType();
        if (type == BufferedImage.TYPE_INT_RGB || type == 0) {
            type = BufferedImage.TYPE_INT_RGB;
        } else if (type == BufferedImage.TYPE_INT_ARGB) {
            type = BufferedImage.TYPE_INT_RGB;
        }
        
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), type);
        Graphics2D g = copy.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return copy;
    }
    
    public static Image renderPreview(BufferedImage baseImage, WatermarkSettings settings) {
        if (baseImage == null) return null;
        BufferedImage result = applyWatermark(baseImage, settings);
        if (result == null) return null;
        return SwingFXUtils.toFXImage(result, null);
    }
}
