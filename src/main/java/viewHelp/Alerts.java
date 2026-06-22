package viewHelp;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.logger.ErrorLogger;

public class Alerts {
    public static void alertDialog(Alert.AlertType type, String title, String headerText, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(message);
        alert.setResizable(true);
        alert.setWidth(600);
        applyDialogStyles(alert, type);
        alert.showAndWait();
    }

    public static void showProgressDialog(Stage owner, Task<?> task, String title, String headerText) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(headerText);

        Label messageLabel = new Label("Files is loading: ");
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(320);
        progressBar.progressProperty().bind(task.progressProperty());

        Timeline timeline = getTimeline(messageLabel);

        task.messageProperty().addListener((_, _, message) -> {
            if (message != null && !message.isEmpty()) {
                timeline.stop();
                Platform.runLater(() -> messageLabel.setText(message));
            }
        });

        VBox content = new VBox(10, messageLabel, progressBar);
        content.setPadding(new Insets(10));
        alert.getDialogPane().setContent(content);
        alert.getButtonTypes().setAll(ButtonType.CANCEL);
        applyDialogStyles(alert, Alert.AlertType.INFORMATION);

        alert.show();

        Button cancelButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setOnAction(_ -> task.cancel());

        task.stateProperty().addListener((_, _, newState) -> {
            if (newState == Worker.State.SUCCEEDED
                    || newState == Worker.State.FAILED
                    || newState == Worker.State.CANCELLED) {
                timeline.stop();
                Platform.runLater(alert::close);
            }
        });
    }

    private static Timeline getTimeline(Label messageLabel) {
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), _ -> {
            String currentText = messageLabel.getText();
            String text = "Files is loading: ";
            if (currentText.equals(text + "."))       { messageLabel.setText(text + "..");  }
            else if (currentText.equals(text + "..")) { messageLabel.setText(text + "..."); }
            else                                      { messageLabel.setText(text + ".");   }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        return timeline;
    }

    private static void applyDialogStyles(Alert alert, Alert.AlertType type) {
        var pane = alert.getDialogPane();
        var res = ErrorLogger.class.getResource("/style.css");
        if (res != null) {
            pane.getStylesheets().add(res.toExternalForm());
        }
        pane.getStyleClass().add("dialog-pane");

        switch (type) {
            case WARNING           -> pane.getStyleClass().add("warning");
            case ERROR             -> pane.getStyleClass().add("danger");
            case INFORMATION, NONE -> pane.getStyleClass().add("info");
        }
    }

    public static boolean confirmationDialog(String title, String headerText, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(message);
        
        alert.getButtonTypes().setAll(javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);

        applyDialogStyles(alert, Alert.AlertType.CONFIRMATION);

        var result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }
}
