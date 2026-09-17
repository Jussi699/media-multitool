package viewHelp;

import javafx.scene.control.Label;
import javafx.scene.control.Slider;

public class SliderSetup {
    public static void setupListenerInSliderForUpdateNewValueInLabelInTheFromPercentages(Slider slider, Label label, double maxValue) {
        slider.valueProperty().addListener((_, _, newValue) -> {
            int value = newValue.intValue();
            int percentage = (int) (value / maxValue * 100);
            label.setText(percentage + "%");
        });
    }

    public static void setupListenerInSliderForUpdateNewValueInLabelInTheFromNumbers(Slider slider, Label label ) {
        slider.valueProperty().addListener((_, _, newValue) ->
                label.setText(newValue.intValue() + "%"));
    }
}
