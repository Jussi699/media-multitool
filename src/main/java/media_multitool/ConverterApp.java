package media_multitool;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import model.logger.ErrorLogger;

import java.io.IOException;
import java.util.Objects;

public class ConverterApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ConverterApp.class.getResource("/viewses/controller-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        scene.getStylesheets().add(String.valueOf(getClass().getResource("/style.css")));

        try {stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/mainImage.png"))));}
        catch (NullPointerException e){ErrorLogger.warn("The icon for the application is missing or damaged.");}

        stage.setResizable(false);
        stage.setTitle("Media multitool!");
        stage.setScene(scene);
        stage.show();
    }
}
