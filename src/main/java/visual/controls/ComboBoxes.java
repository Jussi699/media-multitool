package visual.controls;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.isNull;

public class ComboBoxes {
    private ComboBoxes() {
        /* This utility class should not be instantiated */
    }

    public static <T> void setupComboBox(ComboBox<T> comboBox, Function<T, String> textProvider) {
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(T item) { return isNull(item) ? null : textProvider.apply(item); }
            @Override
            public T fromString(String string) { return null; }
        });
    }

    public static void setupStringComboBox(ComboBox<String> cb) {
        cb.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || isNull(item)) {
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
                if (empty || isNull(item)) {
                    setText(cb.getValue());
                } else {
                    setText(item);
                }
            }
        });
    }

    public static List<ComboBoxItem> createItems(String matchSourceTitle, String suffix, int... values) {
        List<ComboBoxItem> list = new ArrayList<>();
        list.add(new ComboBoxItem(-1, matchSourceTitle));
        for (int val : values) {
            list.add(new ComboBoxItem(val, val + suffix));
        }
        return list;
    }

    public static List<ComboBoxItem> createItemsRound(String matchSourceTitle, String suffix, float... values) {
        List<ComboBoxItem> list = new ArrayList<>();
        list.add(new ComboBoxItem(-1f, matchSourceTitle));
        for (float val : values) {
            list.add(new ComboBoxItem(val, Math.round(val * 100) + suffix));
        }
        return list;
    }

    public static List<ComboBoxItem> createItems(String matchSourceTitle, String prefix, String suffix, int... values) {
        List<ComboBoxItem> list = new ArrayList<>();
        list.add(new ComboBoxItem(-1, matchSourceTitle));
        for (int val : values) {
            list.add(new ComboBoxItem(val, prefix + val + " " + suffix));
        }
        return list;
    }
}
