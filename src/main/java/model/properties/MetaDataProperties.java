package model.properties;

import javafx.animation.PauseTransition;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;

import java.io.File;

@Getter
@Setter
public class MetaDataProperties implements MediaProperties {
    private File currentFile;
    private File output;
    private int secondsForHideSuccessMessage = 5;
    private final PauseTransition hideSuccessMessageTimer = new PauseTransition(Duration.seconds(secondsForHideSuccessMessage));

    @Override
    public void reset() {
        this.currentFile = null;
    }
}
