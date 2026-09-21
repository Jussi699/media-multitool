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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.logger.ErrorLogger;

import java.net.URL;

public class Alerts {
    private static final String INFO_ICON_PATH = "/img/info.png";
    private static final String QUESTION_ICON_PATH = "/img/question.png";

    private Alerts() {}

    private static String getIconPath(Alert.AlertType type) {
        if (type == Alert.AlertType.CONFIRMATION) {
            return QUESTION_ICON_PATH;
        }
        return INFO_ICON_PATH;
    }

    private static ImageView createGraphic(Alert.AlertType type) {
        String path = getIconPath(type);
        URL resource = Alerts.class.getResource(path);
        if (resource == null) {
            ErrorLogger.error("File " + path + " not found");
            return null;
        }

        Image image = new Image(resource.toExternalForm(), 48, 48, true, true);
        return new ImageView(image);
    }

    private static Image createStageIcon(Alert.AlertType type) {
        String path = getIconPath(type);
        URL resource = Alerts.class.getResource(path);
        if (resource == null) {
            return null;
        }
        return new Image(resource.toExternalForm());
    }

    public static void alertDialog(Alert.AlertType type, String title, String headerText, String message) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> alertDialog(type, title, headerText, message));
            return;
        }

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(message);
        alert.initModality(Modality.NONE);
        alert.setResizable(true);
        alert.setWidth(600);

        ImageView graphic = createGraphic(type);
        if (graphic != null) {
            alert.setGraphic(graphic);
        }

        applyDialogStyles(alert, type);
        alert.showAndWait();
    }

    public static void showProgressDialog(Stage owner, Task<?> task, String title, String headerText) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.initModality(Modality.NONE);
        alert.setTitle(title);
        alert.setHeaderText(headerText);

        Label messageLabel = new Label("Files is loading: ");
        messageLabel.setTextFill(Color.web("#cccccc"));
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
        var rootRes = ErrorLogger.class.getResource("/root.css");
        if (rootRes != null) {
            pane.getStylesheets().add(rootRes.toExternalForm());
        }
        var res = ErrorLogger.class.getResource("/style.css");
        if (res != null) {
            pane.getStylesheets().add(res.toExternalForm());
        }
        pane.getStyleClass().add("dialog-pane");

        if (pane.getScene() != null && pane.getScene().getWindow() instanceof Stage stage) {
            Image stageIcon = createStageIcon(type);
            if (stageIcon != null) {
                stage.getIcons().setAll(stageIcon);
            }
        }

        switch (type) {
            case WARNING           -> pane.getStyleClass().add("warning");
            case ERROR             -> pane.getStyleClass().add("danger");
            case INFORMATION       -> pane.getStyleClass().add("info");
            default                -> pane.getStyleClass().add("info");
        }

        alert.showingProperty().addListener((_, _, isShowing) -> {
            if (Boolean.TRUE.equals(isShowing)) {
                Platform.runLater(() -> {
                    var scene = alert.getDialogPane().getScene();
                    if (scene != null && scene.getWindow() != null) {
                        if (scene.getWindow() instanceof Stage stage) {
                            Image stageIcon = createStageIcon(type);
                            if (stageIcon != null) {
                                stage.getIcons().setAll(stageIcon);
                            }
                        }
                        WindowsDwmUtils.applyDarkMode(scene.getWindow());
                    }
                });
            }
        });
    }

    public static boolean confirmationDialog(String title, String headerText, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setHeaderText(headerText);
        alert.setContentText(message);

        ImageView graphic = createGraphic(Alert.AlertType.CONFIRMATION);
        if (graphic != null) {
            alert.setGraphic(graphic);
        }

        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        applyDialogStyles(alert, Alert.AlertType.CONFIRMATION);

        var result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }
}
