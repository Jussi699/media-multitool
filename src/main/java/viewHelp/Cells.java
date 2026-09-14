package viewHelp;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

public class Cells {
    public static void comboBoxIcoSizeButtonCell(ComboBox<String> comboBoxIcoSize, String text) {
        comboBoxIcoSize.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || text.equals(item)) {
                    setText(text);
                } else {
                    setText(item);
                }
                setStyle("-fx-background-color: transparent; -fx-alignment: center; -fx-text-fill: white;");
            }
        });
    }

    public static void comboBoxIcoSizeSetCellFactory(ComboBox<String> comboBoxIcoSize, String text) {
        comboBoxIcoSize.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || text.equals(item)) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setGraphic(null);
                    setStyle("-fx-alignment: center; -fx-text-fill: white;");
                }
            }
        });
    }
}
