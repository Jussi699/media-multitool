package media_multitool.watermarks;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class WatermarkRenderer {
    private static final int BASE_FONT_SIZE = 24;
    
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
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int fontSize = (int) (BASE_FONT_SIZE * settings.getFontSize());
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
            
            String text = settings.getText();
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();
            
            double rotation = Math.toRadians(settings.getRotation());
            
            switch (settings.getTilePattern()) {
                case "single"  -> applyTextSingleTile(g2d, image, text, textWidth, textHeight, settings, colorWithAlpha, rotation);
                case "grid"    -> applyTextGridTile(g2d, image, text, textWidth, textHeight, settings, colorWithAlpha, rotation);
                case "diamond" -> applyTextDiamondTile(g2d, image, text, textWidth, textHeight, settings, colorWithAlpha, rotation);
            }
        } finally {
            g2d.dispose();
        }
    }
    
    private static void applyTextSingleTile(Graphics2D g2d, BufferedImage image, String text, 
                                           int textWidth, int textHeight, WatermarkSettings settings,
                                           Color color, double rotation) {
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
        drawTextWithEffect(g2d, text, x, y, settings, color);
    }
    
    private static void applyTextGridTile(Graphics2D g2d, BufferedImage image, String text,
                                         int textWidth, int textHeight, WatermarkSettings settings,
                                         Color color, double rotation) {
        int spacing = (int) (settings.getSpacing() * 50);
        int stepX = textWidth + spacing;
        int stepY = textHeight + spacing;
        
        for (int y = textHeight; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                AffineTransform transform = new AffineTransform();
                transform.translate(x + textWidth / 2.0, y);
                transform.rotate(rotation);
                transform.translate(-textWidth / 2.0, 0);
                
                g2d.setTransform(transform);
                drawTextWithEffect(g2d, text, 0, 0, settings, color);
            }
        }
    }
    
    private static void applyTextDiamondTile(Graphics2D g2d, BufferedImage image, String text,
                                            int textWidth, int textHeight, WatermarkSettings settings,
                                            Color color, double rotation) {
        int spacing = (int) (settings.getSpacing() * 50);
        int stepX = textWidth + spacing;
        int stepY = textHeight + spacing;
        
        boolean offset = false;
        for (int y = textHeight; y < image.getHeight(); y += stepY) {
            int startX = offset ? stepX / 2 : 0;
            for (int x = startX; x < image.getWidth(); x += stepX) {
                AffineTransform transform = new AffineTransform();
                transform.translate(x + textWidth / 2.0, y);
                transform.rotate(rotation);
                transform.translate(-textWidth / 2.0, 0);
                
                g2d.setTransform(transform);
                drawTextWithEffect(g2d, text, 0, 0, settings, color);
            }
            offset = !offset;
        }
    }
    
    private static void drawTextWithEffect(Graphics2D g2d, String text, int x, int y, 
                                          WatermarkSettings settings, Color color) {
        String effect = settings.getEffect();
        if (effect == null) effect = "none";

        switch (effect.toLowerCase()) {
            case "shadow"  -> drawShadowEffect(g2d, text, x, y, color);
            case "outline" -> drawOutlineEffect(g2d, text, x, y, color);
            case "glow"    -> drawGlowEffect(g2d, text, x, y, color);
            default        -> {
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
        g2d.setColor(new Color(0, 0, 0, color.getAlpha()));
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
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alphaDiv));
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
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            
            int size = (int) settings.getSize();
            float opacity = (float) Math.min(1.0, settings.getOpacity() / 100.0);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            
            double rotation = Math.toRadians(settings.getRotation());
            
            if (settings.isTileMode()) { applyImageTile(g2d, image, settings, size, rotation);   }
            else                       { applyImageSingle(g2d, image, settings, size, rotation); }
        } finally {
            g2d.dispose();
        }
    }
    
    private static void applyImageTile(Graphics2D g2d, BufferedImage image, WatermarkSettings settings, 
                                       int size, double rotation) {
        int spacing = (int) settings.getSpacing();
        int step = size + spacing;
        String pattern = settings.getTilePattern();
        
        if ("diamond".equals(pattern)) { applyImageDiamondTile(g2d, image, settings, size, rotation, step); }
        else                           { applyImageGridTile(g2d, image, settings, size, rotation, step);    }
    }
    
    private static void applyImageGridTile(Graphics2D g2d, BufferedImage image, WatermarkSettings settings,
                                          int size, double rotation, int step) {
        for (int y = 0; y < image.getHeight(); y += step) {
            for (int x = 0; x < image.getWidth(); x += step) {
                drawTransformedImage(g2d, settings.getWatermarkImage(), x, y, size, rotation);
            }
        }
    }
    
    private static void applyImageDiamondTile(Graphics2D g2d, BufferedImage image, WatermarkSettings settings,
                                             int size, double rotation, int step) {
        boolean offset = false;
        for (int y = 0; y < image.getHeight(); y += step) {
            int startX = offset ? step / 2 : 0;
            for (int x = startX; x < image.getWidth(); x += step) {
                drawTransformedImage(g2d, settings.getWatermarkImage(), x, y, size, rotation);
            }
            offset = !offset;
        }
    }
    
    private static void applyImageSingle(Graphics2D g2d, BufferedImage image, WatermarkSettings settings, int size, double rotation) {
        int x, y;
        if (settings.isUseCustomPosition()) {
            x = (int) settings.getPositionX();
            y = (int) settings.getPositionY();
        } else {
            x = (image.getWidth() - size) / 2;
            y = (image.getHeight() - size) / 2;
        }
        
        drawTransformedImage(g2d, settings.getWatermarkImage(), x, y, size, rotation);
    }
    
    private static void drawTransformedImage(Graphics2D g2d, BufferedImage wmImage, int x, int y, int size, double rotation) {
        AffineTransform transform = new AffineTransform();
        transform.translate(x + size / 2.0, y + size / 2.0);
        transform.rotate(rotation);
        transform.translate(-size / 2.0, -size / 2.0);
        
        g2d.setTransform(transform);
        g2d.drawImage(wmImage, 0, 0, size, size, null);
    }
    
    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
            source.getWidth(),
            source.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g = copy.createGraphics();
        try {
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return copy;
    }
    
    public static Image renderPreview(BufferedImage baseImage, WatermarkSettings settings) {
        if (baseImage == null) { return null; }
        BufferedImage result = applyWatermark(baseImage, settings);
        if (result == null) { return null; }
        return SwingFXUtils.toFXImage(result, null);
    }
}
