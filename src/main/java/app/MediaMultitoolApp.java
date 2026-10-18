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
    private static final String PATH_MAIN_ICON = "/img/tools.png";

    @Override
    public void start(Stage loadingStage) throws IOException {
        checkLoggingStatus();
        showLoadingStage(loadingStage);
        startBackgroundLoading(createLoadAppTask(loadingStage));
    }

    private void checkLoggingStatus() {
        if (!Launcher.isCanWriteInLog()) {
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Logback Error",
                    "Logback cannot write logs to the log file!\nIn the app, go to the \"Info\" tab to access the logs folder and view the 'logback.log' file.");
        }
    }

    private void showLoadingStage(Stage loadingStage) throws IOException {
        Scene loadingScene = createLoadingScene();

        loadingStage.initStyle(StageStyle.DECORATED);
        loadingStage.setResizable(false);
        loadingStage.setTitle("Loading App");
        loadingStage.setScene(loadingScene);
        applyAppIcon(loadingStage);

        loadingStage.setOnCloseRequest(_ -> Platform.exit());

        WindowsDwmUtils.enableDarkMode(loadingStage);
        loadingStage.show();
    }

    private Scene createLoadingScene() throws IOException {
        FXMLLoader loadingLoader = new FXMLLoader(MediaMultitoolApp.class.getResource("/viewses/loading-page-view.fxml"));
        Scene loadingScene = new Scene(loadingLoader.load(), 300, 200);
        loadingScene.getStylesheets().add(String.valueOf(getClass().getResource("/style.css")));
        loadingScene.getStylesheets().add(String.valueOf(getClass().getResource("/root.css")));
        return loadingScene;
    }

    private Task<Scene> createLoadAppTask(Stage loadingStage) {
        Task<Scene> loadAppTask = new Task<>() {
            @Override
            protected Scene call() throws Exception {
                return createMainScene();
            }
        };

        loadAppTask.setOnSucceeded(_ -> onAppLoadSucceeded(loadingStage, loadAppTask));
        loadAppTask.setOnFailed(_ -> onAppLoadFailed(loadingStage, loadAppTask.getException()));
        loadAppTask.setOnCancelled(_ -> onAppLoadCancelled(loadingStage));

        return loadAppTask;
    }

    private Scene createMainScene() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MediaMultitoolApp.class.getResource("/viewses/controller-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1040, 860);
        scene.getStylesheets().add(String.valueOf(getClass().getResource("/style.css")));
        scene.getStylesheets().add(String.valueOf(getClass().getResource("/root.css")));
        scene.getStylesheets().add(String.valueOf(getClass().getResource("/home_page&control_panel.css")));
        return scene;
    }

    private void onAppLoadSucceeded(Stage loadingStage, Task<Scene> loadAppTask) {
        if (loadAppTask.isCancelled() || !loadingStage.isShowing()) {
            return;
        }

        Scene mainScene = loadAppTask.getValue();
        showMainStage(mainScene);
        loadingStage.close();
    }

    private void showMainStage(Scene mainScene) {
        Stage mainStage = new Stage();
        applyAppIcon(mainStage);
        mainStage.setResizable(true);
        mainStage.setTitle("Media multitool!");
        mainStage.setScene(mainScene);
        WindowsDwmUtils.enableDarkMode(mainStage);
        mainStage.show();
    }

    private void onAppLoadFailed(Stage loadingStage, Throwable ex) {
        if (nonNull(ex)) {
            String rootCauseMessage = extractRootCauseMessage(ex);
            ErrorLogger.error("Failed to load application", ex);
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Failed load app",
                    "Application startup failed.\nRoot cause: " + rootCauseMessage
                            + "\n\nFull details are available in the application log.");
        }
        loadingStage.close();
        Platform.exit();
    }

    private String extractRootCauseMessage(Throwable ex) {
        Throwable rootCause = ex;
        while (nonNull(rootCause.getCause()) && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause.getClass().getSimpleName()
                + (nonNull(rootCause.getMessage()) ? ": " + rootCause.getMessage() : "");
    }

    private void onAppLoadCancelled(Stage loadingStage) {
        loadingStage.close();
        Platform.exit();
    }

    private void applyAppIcon(Stage stage) {
        try {
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream(PATH_MAIN_ICON))));
        } catch (NullPointerException e) {
            ErrorLogger.error("The icon for the application is missing or damaged: " + e + " (" + PATH_MAIN_ICON + ")");
        }
    }

    private void startBackgroundLoading(Task<Scene> loadAppTask) {
        Thread loaderThread = new Thread(loadAppTask);
        loaderThread.setDaemon(true);
        loaderThread.start();
    }
}
