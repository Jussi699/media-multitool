package media_multitool;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import lombok.Setter;
import viewHelp.Alerts;

public class HomeViewController {

    @Setter private ViewController mainController;

    @FXML
    public void initialize() {
    }

    //--------------------------Converters--------------------------//
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
    //--------------------------/Converters--------------------------//


    //--------------------------Compressors--------------------------//
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
    //--------------------------/Compressors--------------------------//


    //--------------------------Image--------------------------//

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
    private void onOpenCropImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(9);
        }
    }

    @FXML
    private void onOpenColorReplaceInImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionImage(10);
        }
    }
    //--------------------------/Image Tools--------------------------//


    //--------------------------PDF--------------------------//

    @FXML
    private void onOpenImageToPdfPressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionPdf(21);
        }
    }

    @FXML
    private void onOpenPdfToImagePressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionPdf(22);
        }
    }

    @FXML
    private void onOpenImagesToPdfPressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionPdf(23);
        }
    }

    @FXML
    public void onOpenRemovePagesPdfPressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionPdf(24);
        }
    }

    @FXML
    private void onOpenProtectPdfPressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionPdf(25);
        }
    }

    @FXML
    public void onOpenUnlockPdfPressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionPdf(26);
        }
    }

    @FXML
    public void onOpenCompressPdfPressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionPdf(27);
        }
    }

    @FXML
    public void onOpenMergePdfPressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionPdf(28);
        }
    }

    @FXML
    public void onOpenSplitPdfPressed() {
        if (mainController != null) {
            mainController.onActionChoiceActionPdf(29);
        }
    }
    //--------------------------/PDF tools--------------------------//


    //--------------------------Audio Tag Editor--------------------------//
    @FXML
    private void onOpenEditorAudioTagPressed() {
        if (mainController != null) {
            mainController.showEditorAudioTagPage();
        }
    }
    //--------------------------/Audio Tag Editor--------------------------//


    //--------------------------Info--------------------------//

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
    //--------------------------/Info--------------------------//
}
