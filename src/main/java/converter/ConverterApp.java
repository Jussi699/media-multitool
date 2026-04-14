package converter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.logger.ErrorLogger;

import java.io.IOException;

public class ConverterApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        ErrorLogger.info("Application starting...");
        FXMLLoader fxmlLoader = new FXMLLoader(ConverterApp.class.getResource("/controller-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        scene.getStylesheets().add(String.valueOf(getClass().getResource("/style.css")));
        stage.setResizable(false);
        stage.setTitle("Converter!");
        stage.setScene(scene);
        stage.show();
        ErrorLogger.info("Main window displayed.");
    }
}
