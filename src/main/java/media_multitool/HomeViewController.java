package media_multitool;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import viewHelp.Alerts;

public class HomeViewController {
    private ViewController mainController;

    public void setMainController(ViewController mainController) {
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

    public void onOpenEditorAudioPressed() {
        if (mainController != null) {
            mainController.showEditorAudioPage();
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

    public void onOpenNegativeImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(1);
        }
    }

    public void onOpenTurnImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(2);
        }
    }

    public void onOpenLightenImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(3);
        }
    }

    public void onOpenDarkenImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(4);
        }
    }

    public void onOpenColorizeImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(5);
        }
    }

    public void onOpenBlackAndWhiteImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(6);
        }
    }

    public void onOpenBlurImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(7);
        }
    }

    public void onOpenFindPixelPressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(8);
        }
    }
}
