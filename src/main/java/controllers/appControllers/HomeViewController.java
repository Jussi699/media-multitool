package controllers.appControllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import lombok.Setter;
import visual.dialogs.Alerts;

import static java.util.Objects.nonNull;

public class HomeViewController {
    @Setter private ViewController mainController;

    @FXML
    public void initialize() {}

    @FXML
    public void onOpenPagePressed(ActionEvent actionEvent) {
        if (nonNull(mainController)) {
           Button button = (Button) actionEvent.getSource();

            switch (button.getId()) {
                // Converters
                case "btnImageConverter" -> mainController.showConverterImagePage();
                case "btnVideoConverter" -> mainController.showConverterVideoPage();
                case "btnAudioConverter" -> mainController.showConverterAudioPage();

                // Compressors
                case "btnCompressImage"  -> mainController.showCompressorImagePage();
                case "btnCompressVideo"  -> mainController.showCompressorVideoPage();
                case "btnCompressPdf"    -> mainController.showCompressorPdfPage();

                // Image Tools
                case "btnNegative"       -> mainController.onActionChoiceActionImage(1);
                case "btnRotate"         -> mainController.onActionChoiceActionImage(2);
                case "btnLighten"        -> mainController.onActionChoiceActionImage(3);
                case "btnDarken"         -> mainController.onActionChoiceActionImage(4);
                case "btnColorize"       -> mainController.onActionChoiceActionImage(5);
                case "btnBlackAndWhite"  -> mainController.onActionChoiceActionImage(6);
                case "btnBlur"           -> mainController.onActionChoiceActionImage(7);
                case "btnSpotBlur"       -> mainController.onActionChoiceActionImage(8);
                case "btnFindPixel"      -> mainController.onActionChoiceActionImage(9);
                case "btnColorReplace"   -> mainController.onActionChoiceActionImage(10);
                case "btnCrop"           -> mainController.onActionChoiceActionImage(11);

                // PDF Tools
                case "btnImageToPdf"     -> mainController.onActionChoiceActionPdf(21);
                case "btnImagesToPdf"    -> mainController.onActionChoiceActionPdf(22);
                case "btnPdfToImage"     -> mainController.onActionChoiceActionPdf(23);
                case "btnRemovePagesPdf" -> mainController.onActionChoiceActionPdf(24);
                case "btnProtectPdf"     -> mainController.onActionChoiceActionPdf(25);
                case "btnUnlockPdf"      -> mainController.onActionChoiceActionPdf(26);
                case "btnPdfCompress"    -> mainController.onActionChoiceActionPdf(27);
                case "btnMergePdf"       -> mainController.onActionChoiceActionPdf(28);
                case "btnSplitPdf"       -> mainController.onActionChoiceActionPdf(29);
                case "btnPdfToWord"      -> mainController.onActionChoiceActionPdf(30);
                case "btnWordToPdf"      -> mainController.onActionChoiceActionPdf(31);

                // Watermark
                case "btnWatermarkImage" -> mainController.onActionChoiceActionWatermark(41);
                case "btnWatermarkPdf"   -> mainController.onActionChoiceActionWatermark(42);
                case "btnWatermarkVideo" -> mainController.onActionChoiceActionWatermark(43);

                // Media Tools
                case "btnMediaTagEditor" -> mainController.showMediaEditorTagPage();
                case "btnMetadata"       -> mainController.showMetaDataPage();

                // Info
                case "btnApplicationInfo" -> mainController.showInfoPage();
                default -> throw new IllegalArgumentException("Unexpected id: " + button.getId());
            }
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
