package app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import model.logger.ErrorLogger;

import java.io.IOException;
import java.util.Objects;

public class MediaMultitoolApp extends Application {
    @Override
    public void start(Stage loadingStage) throws IOException {
        FXMLLoader loadingLoader = new FXMLLoader(MediaMultitoolApp.class.getResource("/viewses/loading-page-view.fxml"));
        Scene loadingScene = new Scene(loadingLoader.load(), 300, 200);
        loadingScene.getStylesheets().add(String.valueOf(getClass().getResource("/style.css")));
        loadingScene.getStylesheets().add(String.valueOf(getClass().getResource("/root.css")));

        loadingStage.initStyle(StageStyle.DECORATED);
        loadingStage.setResizable(false);
        loadingStage.setTitle("Loading App");
        loadingStage.setScene(loadingScene);

        try {
            loadingStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/mainImage.png"))));
        } catch (NullPointerException _) {
            ErrorLogger.warn("The icon for the application is missing or damaged.");
        }

        loadingStage.setOnCloseRequest(_ -> {
            Platform.exit();
            System.exit(0);
        });

        loadingStage.show();


        Task<Scene> loadAppTask = new Task<>() {
            @Override
            protected Scene call() throws Exception {
                FXMLLoader fxmlLoader = new FXMLLoader(MediaMultitoolApp.class.getResource("/viewses/controller-view.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 1040, 900);
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
                mainStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/mainImage.png"))));
            } catch (NullPointerException _) {
                ErrorLogger.warn("The icon for the application is missing or damaged.");
            }

            mainStage.setResizable(true);
            mainStage.setMinHeight(600);
            mainStage.setMinWidth(700);

            mainStage.setTitle("Media multitool!");
            mainStage.setScene(mainScene);

            loadingStage.close();
            mainStage.show();
        });

        loadAppTask.setOnFailed(_ -> {
            Throwable ex = loadAppTask.getException();
            if (ex != null) {
                ErrorLogger.error("Failed to load application: " + ex.getMessage());
            }
            loadingStage.close();
            Platform.exit();
            System.exit(0);
        });

        loadAppTask.setOnCancelled(_ -> {
            loadingStage.close();
            Platform.exit();
            System.exit(0);
        });

        Thread loaderThread = new Thread(loadAppTask);
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        System.exit(0);
    }
}
