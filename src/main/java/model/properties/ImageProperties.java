package model.properties;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.io.File;

public class ImageProperties {
    private File image;
    private File output;
    private File compressedImage;
    private String typeImage;
    private int sizeIcoImage;
    private int secondsForHideSuccessMessage = 5;
    private float scale;
    private float quality;
    private final PauseTransition hideSuccessMessageTimer = new PauseTransition(Duration.seconds(secondsForHideSuccessMessage));

    public File getImage() {
        return image;
    }

    public File getCompressedImage() {
        return compressedImage;
    }

    public void setCompressedImage(File compressedImage) {
        this.compressedImage = compressedImage;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public float getQuality() {
        return quality;
    }

    public void setQuality(float quality) {
        this.quality = quality;
    }

    public void setImage(File image) {
        this.image = image;
    }

    public File getOutput() {
        return output;
    }

    public void setOutput(File output) {
        this.output = output;
    }

    public String getTypeImage() {
        return typeImage;
    }

    public void setTypeImage(String typeImage) {
        this.typeImage = typeImage;
    }

    public int getSizeIcoImage() {
        return sizeIcoImage;
    }

    public void setSizeIcoImage(int sizeIcoImage) {
        this.sizeIcoImage = sizeIcoImage;
    }

    public int getSecondsForHideSuccessMessage() {
        return secondsForHideSuccessMessage;
    }

    public void setSecondsForHideSuccessMessage(int secondsForHideSuccessMessage) {
        this.secondsForHideSuccessMessage = secondsForHideSuccessMessage;
    }

    public PauseTransition getHideSuccessMessageTimer() {
        return hideSuccessMessageTimer;
    }
}
