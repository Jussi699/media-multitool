package media_multitool;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert;
import viewHelp.Alerts;

public class HomeViewController {
    @FXML private Button btnOpenInfo;
    @FXML private Button btnOpenImageCompressor;
    @FXML private Button btnOpenImageConverter;
    @FXML private Button btnOpenVideoConverter;
    @FXML private Button btnOpenMP3Converter;
    @FXML private Button btnOpenVideoCompressor;
    @FXML private VBox homePage;
    private ControllerView mainController;

    public void setMainController(ControllerView mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void onOpenImageConverterPressed() {
        if (mainController != null) {
            mainController.showConverterImagePage();
        }
    }

    @FXML
    private void onOpenVideoConverterPressed() {
        if (mainController != null) {
            mainController.showConverterVideoPage();
        }
    }

    @FXML
    private void onOpenMP3ConverterPressed() {
        if (mainController != null) {
            mainController.showConverterAudioPage();
        }
    }

    @FXML
    private void onOpenVideoCompressorPressed() {
        if (mainController != null) {
            mainController.showCompressorVideoPage();
        }
    }

    @FXML
    private void onOpenImageCompressorPressed() {
        if (mainController != null) {
            mainController.showCompressorImagePage();
        }
    }

    @FXML
    private void onOpenInfoPressed() {
        if (mainController != null) {
            mainController.showInfoPage();
        }
    }


    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Home Page",
                "Welcome to Media Multitool! Select a tool from the buttons below or use the navigation sidebar to start converting or compressing your media files."
        );
    }
}
