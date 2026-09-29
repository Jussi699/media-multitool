package viewHelp;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.logger.ErrorLogger;

import java.net.URL;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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

    public static ImageView createQuestionGraphic() {
        URL resource = Alerts.class.getResource(QUESTION_ICON_PATH);
        if (isNull(resource)) {
            ErrorLogger.error("File " + QUESTION_ICON_PATH + " not found");
            return null;
        }

        return new ImageView(new Image(resource.toExternalForm(), 48, 48, true, true));
    }

    private static ImageView createGraphic(Alert.AlertType type) {
        String path = getIconPath(type);
        URL resource = Alerts.class.getResource(path);
        if (isNull(resource)) {
            ErrorLogger.error("File " + path + " not found");
            return null;
        }

        Image image = new Image(resource.toExternalForm(), 48, 48, true, true);
        return new ImageView(image);
    }

    private static Image createStageIcon(Alert.AlertType type) {
        String path = getIconPath(type);
        URL resource = Alerts.class.getResource(path);
        if (isNull(resource)) {
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

        DialogPane dialogPane = alert.getDialogPane();
        double dynamicWidth = calculatePreferredWidth(headerText, message);
        dialogPane.setPrefWidth(dynamicWidth);
        dialogPane.setMinHeight(Region.USE_PREF_SIZE);

        alert.setGraphic(null);
        ImageView graphic = createGraphic(type);
        if (nonNull(graphic)) {
            alert.setGraphic(graphic);
        }

        applyDialogStyles(alert, type);
        alert.showAndWait();
    }

    private static double calculatePreferredWidth(String header, String message) {
        double maxLineWidth = 0;
        Font contentFont = Font.font("Segoe UI", 14);

        String allText = (header != null ? header + "\n" : "") + (message != null ? message : "");
        for (String line : allText.split("\n")) {
            Text textNode = new Text(line);
            textNode.setFont(contentFont);
            maxLineWidth = Math.max(maxLineWidth, textNode.getLayoutBounds().getWidth());
        }

        double targetWidth = maxLineWidth + 120;

        return Math.clamp(targetWidth, 380, 750);
    }

    public static void showProgressDialog(Stage owner, Task<?> task, String title, String headerText) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        if (nonNull(owner)) {
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
            if (nonNull(message) && !message.isEmpty()) {
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
        if (nonNull(rootRes)) {
            pane.getStylesheets().add(rootRes.toExternalForm());
        }
        var res = ErrorLogger.class.getResource("/style.css");
        if (nonNull(res)) {
            pane.getStylesheets().add(res.toExternalForm());
        }
        pane.getStyleClass().add("dialog-pane");

        if (nonNull(pane.getScene()) && pane.getScene().getWindow() instanceof Stage stage) {
            Image stageIcon = createStageIcon(type);
            if (nonNull(stageIcon)) {
                stage.getIcons().setAll(stageIcon);
            }
        }

        applyTypeStyleClass(type, pane);
        setupWindowAppearance(alert, type);
    }

    private static void applyTypeStyleClass(Alert.AlertType type, DialogPane pane) {
        switch (type) {
            case WARNING           -> pane.getStyleClass().add("warning");
            case ERROR             -> pane.getStyleClass().add("danger");
            case INFORMATION       -> pane.getStyleClass().add("info");
            default                -> pane.getStyleClass().add("info");
        }
    }

    private static void setupWindowAppearance(Alert alert, Alert.AlertType type) {
        alert.showingProperty().addListener((_, _, isShowing) -> {
            if (Boolean.TRUE.equals(isShowing)) {
                Platform.runLater(() -> {
                    var scene = alert.getDialogPane().getScene();
                    if (nonNull(scene)) {
                        scene.setFill(Color.web("#232323"));

                        if (nonNull(scene.getWindow())) {
                            if (scene.getWindow() instanceof Stage stage) {
                                Image stageIcon = createStageIcon(type);
                                if (nonNull(stageIcon)) {
                                    stage.getIcons().setAll(stageIcon);
                                }
                            }
                            WindowsDwmUtils.applyDarkMode(scene.getWindow());
                        }
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

        alert.setGraphic(null);
        ImageView graphic = createGraphic(Alert.AlertType.CONFIRMATION);
        if (nonNull(graphic)) {
            alert.setGraphic(graphic);
        }

        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        applyDialogStyles(alert, Alert.AlertType.CONFIRMATION);

        var result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }
}
