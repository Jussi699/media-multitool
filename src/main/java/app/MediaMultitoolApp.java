package app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import model.logger.ErrorLogger;
import viewHelp.Alerts;
import viewHelp.WindowsDwmUtils;

import java.io.IOException;
import java.util.Objects;

import static java.util.Objects.nonNull;

public class MediaMultitoolApp extends Application {
    private static final String PATH_MAIN_ICON = "/img/mainIcon.png";

    @Override
    public void start(Stage loadingStage) throws IOException {
        if(!Launcher.isCanWriteInLog()) {
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Logback Error",
                    "Logback cannot write logs to the log file!\nIn the app, go to the \"Info\" tab to access the logs folder and view the 'logback.log' file.");
        }

        FXMLLoader loadingLoader = new FXMLLoader(MediaMultitoolApp.class.getResource("/viewses/loading-page-view.fxml"));
        Scene loadingScene = new Scene(loadingLoader.load(), 300, 200);
        loadingScene.getStylesheets().add(String.valueOf(getClass().getResource("/style.css")));
        loadingScene.getStylesheets().add(String.valueOf(getClass().getResource("/root.css")));

        loadingStage.initStyle(StageStyle.DECORATED);
        loadingStage.setResizable(false);
        loadingStage.setTitle("Loading App");
        loadingStage.setScene(loadingScene);

        try {
            loadingStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream(PATH_MAIN_ICON))));
        } catch (NullPointerException e) {
            ErrorLogger.error("The icon for the application is missing or damaged: " + e + " (" + PATH_MAIN_ICON + ")");
        }

        loadingStage.setOnCloseRequest(_ -> Platform.exit());

        WindowsDwmUtils.enableDarkMode(loadingStage);
        loadingStage.show();

        Task<Scene> loadAppTask = new Task<>() {
            @Override
            protected Scene call() throws Exception {
                FXMLLoader fxmlLoader = new FXMLLoader(MediaMultitoolApp.class.getResource("/viewses/controller-view.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 1040, 860);
                scene.getStylesheets().add(String.valueOf(getClass().getResource("/style.css")));
                scene.getStylesheets().add(String.valueOf(getClass().getResource("/root.css")));
                scene.getStylesheets().add(String.valueOf(getClass().getResource("/home_page&control_panel.css")));
                return scene;
            }
        };

        loadAppTask.setOnSucceeded(_ -> {
            if (loadAppTask.isCancelled() || !loadingStage.isShowing()) {
                return;
            }

            Scene mainScene = loadAppTask.getValue();
            Stage mainStage = new Stage();

            try {
                mainStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/mainIcon.png"))));
            } catch (NullPointerException e) {
                ErrorLogger.error("The icon for the application is missing or damaged: " + e + " (" + PATH_MAIN_ICON + ")");
            }

            mainStage.setResizable(true);

            mainStage.setTitle("Media multitool!");
            mainStage.setScene(mainScene);

            WindowsDwmUtils.enableDarkMode(mainStage);

            loadingStage.close();
            mainStage.show();
        });

        loadAppTask.setOnFailed(_ -> {
            Throwable ex = loadAppTask.getException();
            if (nonNull(ex)) {
                ErrorLogger.error("Failed to load application: " + ex.getMessage() + ex.getCause());
                Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Failed load app",
                        "Failed to load application: " + ex.getMessage() + ex.getCause());
            }
            loadingStage.close();
            Platform.exit();
        });

        loadAppTask.setOnCancelled(_ -> {
            loadingStage.close();
            Platform.exit();
        });

        Thread loaderThread = new Thread(loadAppTask);
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

}
