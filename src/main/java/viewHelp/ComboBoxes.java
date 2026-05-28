package viewHelp;

import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import java.util.function.Function;

public class ComboBoxes {
    public static <T> void setupComboBox(ComboBox<T> comboBox, Function<T, String> textProvider) {
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(T item) { return item == null ? null : textProvider.apply(item); }
            @Override
            public T fromString(String string) { return null; }
        });
    }
}
