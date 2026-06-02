package viewHelp;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
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

    public static void setupStringComboBox(ComboBox<String> cb) {
        cb.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                }
            }
        });

        cb.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(cb.getValue());
                } else {
                    setText(item);
                }
            }
        });
    }
}
