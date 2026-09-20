package viewHelp;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.helper.watermarks.RecordOpenWatermarkWindow;
import model.logger.ErrorLogger;

import java.util.function.Consumer;

public class OpenWatermarkWindow {
    public <T> T openWatermarkWindow(RecordOpenWatermarkWindow record, Consumer<T> controllerSetup, Consumer<T> settingsLoader) {
        Stage[] stageHolder = record.stageHolder();
        String title = record.title();

        try {
            T controller;
            Stage stage = stageHolder[0];

            if (stage == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(record.fxmlPath()));
                Scene scene = new Scene(loader.load());
                scene.getStylesheets().add(String.valueOf(getClass().getResource("/style.css")));
                scene.getStylesheets().add(String.valueOf(getClass().getResource("/root.css")));
                scene.getStylesheets().add(String.valueOf(getClass().getResource("/home_page&control_panel.css")));
                controller = loader.getController();
                controllerSetup.accept(controller);

                stage = new Stage();
                stage.initModality(Modality.NONE);
                stage.initOwner(record.ownerButton().getScene().getWindow());

                stage.setTitle(title);
                stage.setScene(scene);
                stage.setMinWidth(380);
                stage.setMinHeight(400);

                stage.setMaxWidth(380);
                stage.setMaxHeight(750);

                stage.setUserData(controller);
                stageHolder[0] = stage;
            } else {
                @SuppressWarnings("unchecked")
                T existing = (T) stage.getUserData();
                controller = existing;
            }

            if (controller != null && record.currentWatermarkSettings().getType() == record.expectedType()) {
                settingsLoader.accept(controller);
            }

            if (!stage.isShowing()) {
                stage.show();
            } else {
                stage.toFront();
            }

            return controller;
        } catch (Exception e) {
            ErrorLogger.error("Failed to open " + title + ": " + e.getMessage());
            return null;
        }
    }
}
