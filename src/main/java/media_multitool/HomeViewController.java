package media_multitool;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import lombok.Setter;
import viewHelp.Alerts;

public class HomeViewController {
    @Setter
    private ViewController mainController;

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
    private void onOpenEditorAudioTagPressed() {
        if (mainController != null) {
            mainController.showEditorAudioTagPage();
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

    @FXML
    private void onOpenNegativeImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(1);
        }
    }

    @FXML
    private void onOpenTurnImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(2);
        }
    }

    @FXML
    private void onOpenLightenImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(3);
        }
    }

    @FXML
    private void onOpenDarkenImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(4);
        }
    }

    @FXML
    private void onOpenColorizeImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(5);
        }
    }

    @FXML
    private void onOpenBlackAndWhiteImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(6);
        }
    }

    @FXML
    private void onOpenBlurImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(7);
        }
    }

    @FXML
    private void onOpenFindPixelPressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(8);
        }
    }

    @FXML
    private void onOpenImageToPdfPressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionPdf(21);
        }
    }
}
