package controllers.converters;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import controllers.abstractControllers.AbstractImageConverterController;
import model.helper.media.MediaHelper;
import model.logger.ErrorLogger;
import visual.util.DragDropped;
import model.utility.format.SupportedFormat;
import model.utility.context.ResetContext;
import visual.dialogs.Alerts;
import visual.controls.Cells;
import visual.dialogs.InfoAlert;

import java.io.File;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.io.PathWorker.getSavedPath;
import static visual.controls.Message.hideSuccessMessage;
import static visual.controls.Message.setupClearMessageTimer;

public class ConverterImageController extends AbstractImageConverterController {
    private static final String TO_ICO = "to ICO";

    @FXML private ToggleButton btnToSVG, btnToWEBP, btnToJPEG;
    @FXML private ToggleButton btnToPNG, btnToTIFF, btnToBMP;
    @FXML private ToggleButton btnToPPM, btnToPGM, btnToPAM;
    @FXML private ComboBox<String> icoSizeComboBox;

    @FXML
    public void initialize() {
        imageProperties.setOutput(getSavedPath());
        initToggleGroup();
        initComboBoxes();

        bindingImageViewToPreviewContainer(imagePreview, previewContainer);
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);

        isPressedReset();

        List<String> supportedFormats = SupportedFormat.getAllSupportedImageFormats();
        dropZone.setOnDragOver(e -> DragDropped.handleDragOver(e, supportedFormats, dropZone));
        dropZone.setOnDragDropped(e -> {
            List<File> droppedFiles = DragDropped.handleDragDropped(e, dropZone, supportedFormats);
            if (!droppedFiles.isEmpty()) {
                loadImages(droppedFiles, false);
            }
        });
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "Converter Image",
                "1. Select an image file using \"Select image\" or drag and drop it into the dash-bordered zone",
                """
                        3. (Optional) To convert an entire folder, click the "Batch file processing" button and select the folder;
                        
                        4. Select the target image format (PNG, JPEG, WEBP, etc.) or ICO size;

                        5. Click "Convert and Download".
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imagePreview, progressBar, true, "image(s)"
        );

        reset(imageProperties, ctx, "Selected image file: none");
        bindingImageViewToPreviewContainer(imagePreview, previewContainer);

        pathFolderBatchProcessing = null;
        filesToProcess.clear();

        icoSizeComboBox.setValue(TO_ICO);

        clearToggleGroup(toggleGroup);
        disableControls();
    }

    @FXML
    private void onActionClickToggleBtnFormat(ActionEvent e) {
        if (nonNull(icoSizeComboBox)) {
            icoSizeComboBox.setValue(TO_ICO);
        }
        hideSuccessMessage(labelSuccess, getProperties().getHideSuccessMessageTimer(), true);

        MediaHelper.selectFormat(e).ifPresentOrElse(
                format -> selectFormat(format.getExtension(), imageProperties::setTypeImage),
                () -> imageProperties.setTypeImage(null)
        );
    }

    @FXML
    private void onActionChoiceIcoSize() {
        hideSuccessMessage(labelSuccess, getProperties().getHideSuccessMessageTimer(), true);
        String selected = icoSizeComboBox.getValue();

        if (isNull(selected) || selected.equals(TO_ICO)) {
            if ("ico".equals(imageProperties.getTypeImage())) {
                imageProperties.setTypeImage(null);
            }
            return;
        }

        imageProperties.setSizeIcoImage(Integer.parseInt(selected));
        clearToggleGroup(toggleGroup);
        imageProperties.setTypeImage("ico");
    }

    @Override
    protected void lockUI() {
        toggleUI(true);
    }

    @Override
    protected void unlockUI() {
        toggleUI(false);
    }

    @Override
    protected void disableControls() {
        toggleControls(true);
    }

    @Override
    protected void enableControls() {
        toggleControls(false);
    }

    private void initToggleGroup() {
        configureToggleGroup(toggleGroup, btnToSVG, btnToWEBP, btnToJPEG, btnToPNG,
                btnToTIFF, btnToBMP, btnToPPM, btnToPGM, btnToPAM);
    }

    private void toggleControls(boolean flag) {
        setControlsDisabled(flag, parametersContainer, btnSubmit, btnReset);
    }

    private void toggleUI(boolean flag) {
        toggleControls(flag);
        setControlsDisabled(flag, btnSelectFile, btnChoiceFolderForSaveFile, btnSelectBatchFileProcessing);
        dropZone.setDisable(flag);
    }

    private void initComboBoxes() {
        final String[] sizeIco = new String[] {"16", "32", "64", "128", "256", "512", "768"};

        icoSizeComboBox.getItems().addAll(sizeIco);
        icoSizeComboBox.setValue(TO_ICO);

        Cells.comboBoxIcoSizeButtonCell(icoSizeComboBox, TO_ICO);
        Cells.comboBoxIcoSizeSetCellFactory(icoSizeComboBox, TO_ICO);

        icoSizeComboBox.getSelectionModel().selectedItemProperty().addListener((_, _, newVal) -> {
            final boolean firstCheck = nonNull(newVal) && !newVal.equals(TO_ICO) && nonNull(imagePreview.getImage());
            final boolean secondCheck = nonNull(imageProperties.getImage()) && imageProperties.getImage().getName().toLowerCase().endsWith(".ico");

            if (firstCheck && secondCheck) {
                    try {
                        double size = Double.parseDouble(newVal);
                        imagePreview.fitHeightProperty().unbind();
                        imagePreview.fitWidthProperty().unbind();
                        imagePreview.setFitHeight(size);
                        imagePreview.setFitWidth(size);
                    } catch (NumberFormatException e) {
                        Alerts.alertDialog(Alert.AlertType.WARNING, "Error", "Format", "Invalid size value!");
                        ErrorLogger.warn("Invalid size value: " + e.getMessage());
                    }
                }
        });
    }
}
