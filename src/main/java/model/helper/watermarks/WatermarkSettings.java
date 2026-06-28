package model.helper.watermarks;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

@Getter
@Setter
public class WatermarkSettings {
    private WatermarkType type;
    
    private double opacity     = 100;
    private double rotation    = 0;
    private double size        = 100;
    private double spacing     = 50;
    private double fontSize    = 2.5;
    private double positionX   = 0;
    private double positionY   = 0;

    private String tilePattern = "single";
    private String text        = "Watermark";
    private String fontName    = "Arial";
    private String effect      = "none";

    private Color textColor = Color.WHITE;
    private BufferedImage watermarkImage;

    private boolean useCustomPosition = false;
    private boolean tileMode          = false;

    public enum WatermarkType {
        TEXT, IMAGE, NONE
    }
    
    public WatermarkSettings() {
        this.type = WatermarkType.NONE;
    }
    
    /**
     * Update a property while preserving the custom position flag.
     * Eliminates the repetitive wasCustom save/restore pattern across controllers.
     */
    public void updatePreservingPosition(Consumer<WatermarkSettings> updater) {
        boolean wasCustom = this.useCustomPosition;
        updater.accept(this);
        this.useCustomPosition = wasCustom;
    }
    
    /**
     * Check if this watermark is in single (non-tiled) mode
     */
    public boolean isSingleMode() {
        if (type == WatermarkType.IMAGE) {
            return !tileMode;
        }
        return "single".equals(tilePattern);
    }
    
    public WatermarkSettings copy() {
        WatermarkSettings copy = new WatermarkSettings();
        copy.type              = this.type;
        copy.opacity           = this.opacity;
        copy.rotation          = this.rotation;
        copy.size              = this.size;
        copy.spacing           = this.spacing;
        copy.tileMode          = this.tileMode;
        copy.tilePattern       = this.tilePattern;
        copy.text              = this.text;
        copy.fontName          = this.fontName;
        copy.fontSize          = this.fontSize;
        copy.textColor         = this.textColor;
        copy.effect            = this.effect;
        copy.watermarkImage    = this.watermarkImage;
        copy.positionX         = this.positionX;
        copy.positionY         = this.positionY;
        copy.useCustomPosition = this.useCustomPosition;
        return copy;
    }
}
