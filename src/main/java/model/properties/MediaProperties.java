package model.properties;

import javafx.animation.PauseTransition;

public interface MediaProperties {
    void reset();
    PauseTransition getHideSuccessMessageTimer();
}
