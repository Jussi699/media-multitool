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
            if (stageHolder[0] == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(record.fxmlPath()));
                Scene scene = new Scene(loader.load());

                controller = loader.getController();
                controllerSetup.accept(controller);

                Stage stage = new Stage();

                stage.initModality(Modality.NONE);
                stage.initOwner(record.ownerButton().getScene().getWindow());

                stage.setTitle(title);
                stage.setScene(scene);
                stage.setMinWidth(400);
                stage.setMinHeight(400);

                stage.setOnCloseRequest(_ -> stageHolder[0] = null);

                stageHolder[0] = stage;
            } else {
                controller = null;
            }

            if (controller != null && record.currentWatermarkSettings().getType() == record.expectedType()) {
                settingsLoader.accept(controller);
            }

            if (!stageHolder[0].isShowing()) {
                stageHolder[0].show();
            } else {
                stageHolder[0].toFront();
            }

            return controller;
        } catch (Exception e) {
            ErrorLogger.error("Failed to open " + title + ": " + e.getMessage());
            return null;
        }
    }
}
