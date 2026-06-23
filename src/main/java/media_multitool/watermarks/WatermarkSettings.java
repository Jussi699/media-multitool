package media_multitool.watermarks;

import lombok.Getter;
import lombok.Setter;

import java.awt.image.BufferedImage;

@Getter
@Setter
public class WatermarkSettings {
    private WatermarkType type;
    
    private double opacity = 100;
    private double rotation = 0;
    private double size = 100;
    private double spacing = 50;
    private boolean tileMode = false;
    private String tilePattern = "single";
    
    private String text = "Watermark";
    private String fontName = "Arial";
    private double fontSize = 2.5;
    private java.awt.Color textColor = java.awt.Color.WHITE;
    private String effect = "none";
    
    private BufferedImage watermarkImage;
    private double positionX = 0;
    private double positionY = 0;
    private boolean useCustomPosition = false;
    
    public enum WatermarkType {
        TEXT, IMAGE, NONE
    }
    
    public WatermarkSettings() {
        this.type = WatermarkType.NONE;
    }
    
    public WatermarkSettings copy() {
        WatermarkSettings copy = new WatermarkSettings();
        copy.type = this.type;
        copy.opacity = this.opacity;
        copy.rotation = this.rotation;
        copy.size = this.size;
        copy.spacing = this.spacing;
        copy.tileMode = this.tileMode;
        copy.tilePattern = this.tilePattern;
        copy.text = this.text;
        copy.fontName = this.fontName;
        copy.fontSize = this.fontSize;
        copy.textColor = this.textColor;
        copy.effect = this.effect;
        copy.watermarkImage = this.watermarkImage;
        copy.positionX = this.positionX;
        copy.positionY = this.positionY;
        copy.useCustomPosition = this.useCustomPosition;
        return copy;
    }
}
