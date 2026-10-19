package visual.dialogs;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.helper.watermarks.RecordOpenWatermarkWindow;
import model.logger.ErrorLogger;
import visual.platform.WindowsDwmUtils;

import java.util.function.Consumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class OpenWatermarkWindow {
    public <T> T openWatermarkWindow(RecordOpenWatermarkWindow watermarkWindow, Consumer<T> controllerSetup, Consumer<T> settingsLoader) {
        Stage[] stageHolder = watermarkWindow.stageHolder();
        String title = watermarkWindow.title();

        try {
            T controller;
            Stage stage = stageHolder[0];

            if (isNull(stage)) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(watermarkWindow.fxmlPath()));
                Scene scene = new Scene(loader.load());
                scene.getStylesheets().add(String.valueOf(getClass().getResource("/styles/style.css")));
                scene.getStylesheets().add(String.valueOf(getClass().getResource("/styles/root.css")));
                scene.getStylesheets().add(String.valueOf(getClass().getResource("/styles/home_page&control_panel.css")));
                controller = loader.getController();
                controllerSetup.accept(controller);

                stage = new Stage();
                stage.initModality(Modality.NONE);
                stage.initOwner(watermarkWindow.ownerButton().getScene().getWindow());

                stage.setTitle(title);
                stage.setScene(scene);
                stage.setMinWidth(380);
                stage.setMinHeight(400);

                stage.setMaxWidth(380);
                stage.setMaxHeight(750);

                stage.setUserData(controller);
                stageHolder[0] = stage;

                WindowsDwmUtils.enableDarkMode(stage);
            } else {
                @SuppressWarnings("unchecked")
                T existing = (T) stage.getUserData();
                controller = existing;
            }

            if (nonNull(controller) && watermarkWindow.currentWatermarkSettings().getType() == watermarkWindow.expectedType()) {
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
