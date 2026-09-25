package viewHelp;

import javafx.scene.control.*;
import model.metadata.MetadataEntry;

public class Cells {
    private Cells() {}

    public static void comboBoxIcoSizeButtonCell(ComboBox<String> comboBoxIcoSize, String text) {
        comboBoxIcoSize.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || text.equals(item)) {
                    setText(text);
                    setStyle("-fx-background-color: transparent; -fx-alignment: CENTER; -fx-text-fill: WHITE;");
                } else {
                    setText(item);
                    setStyle("-fx-background-color: -color-for-active-button-comboBox; -fx-alignment: center; -fx-text-fill: WHITE; -fx-background-radius: 10 0 0 10");
                }
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
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: WHITE;");
                }
            }
        });
    }

    public static void setCellFactoryTableColumn(TableColumn<MetadataEntry, Boolean> table) {
        table.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean canDelete, boolean empty) {
                super.updateItem(canDelete, empty);
                if (empty || canDelete == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                } else if (canDelete) {
                    setText("✓");
                    setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 15px; -fx-alignment: CENTER;");
                    setTooltip(new Tooltip("Can be deleted manually"));
                } else {
                    setText("✕");
                    setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 13px; -fx-alignment: CENTER;");
                    setTooltip(new Tooltip("Read-only file property"));
                }
            }
        });
    }
}
