package viewHelp;

import javafx.scene.control.Alert;
import model.logger.ErrorLogger;

public class Alerts {
    public static void alertDialog(Alert.AlertType type, String title, String headerText, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(message);

        var pane = alert.getDialogPane();


        var res = ErrorLogger.class.getResource("/style.css");
        if (res != null) pane.getStylesheets().add(res.toExternalForm());
        pane.getStyleClass().add("dialog-pane");

        switch (type) {
            case WARNING -> pane.getStyleClass().add("warning");
            case ERROR -> pane.getStyleClass().add("danger");
            case INFORMATION -> pane.getStyleClass().add("info");
        }

        alert.showAndWait();
    }

    public static boolean confirmationDialog(String title, String headerText, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(message);
        
        alert.getButtonTypes().setAll(javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);

        var pane = alert.getDialogPane();
        var res = ErrorLogger.class.getResource("/style.css");
        if (res != null) pane.getStylesheets().add(res.toExternalForm());
        pane.getStyleClass().add("dialog-pane");

        var result = alert.showAndWait();
        return result.isPresent() && result.get() == javafx.scene.control.ButtonType.YES;
    }
}
