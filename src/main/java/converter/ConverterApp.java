package converter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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

        try {stage.getIcons().add(new Image(getClass().getResourceAsStream("/image/convert.png")));}
        catch (NullPointerException e){ErrorLogger.warn("The icon for the application is missing or damaged.");}

        stage.setResizable(false);
        stage.setTitle("Converter!");
        stage.setScene(scene);
        stage.show();
        ErrorLogger.info("Main window displayed.");
    }
}
