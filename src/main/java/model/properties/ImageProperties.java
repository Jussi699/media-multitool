package model.properties;

import javafx.animation.PauseTransition;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;

import java.io.File;

@Getter
@Setter
public class ImageProperties implements MediaProperties {
    private File image;
    private File output;
    private File compressedImage;
    private String typeImage;
    private int sizeIcoImage;
    private int secondsForHideSuccessMessage = 5;
    private float scale;
    private float quality;
    private final PauseTransition hideSuccessMessageTimer = new PauseTransition(Duration.seconds(secondsForHideSuccessMessage));
    private String pathToImage;

    @Override
    public void reset() {
        this.image = null;
        this.typeImage = null;
        this.compressedImage = null;
        this.scale = -1;
        this.quality = -1;
    }

    public void setImage(File image) {
        this.image = image;
        if(image != null) {
            pathToImage = image.getPath();
        }
    }
}
