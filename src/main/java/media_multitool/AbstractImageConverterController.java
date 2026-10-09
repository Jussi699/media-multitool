package media_multitool;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.converterImage.ConvertImageTask;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.Global;
import model.utility.Preparation;
import viewHelp.Alerts;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.converterImage.UsefulMethods.readPreviewImage;
import static model.utility.PathWorker.directoryChooser;
import static model.utility.PathWorker.getSavedPath;

public abstract class AbstractImageConverterController extends AbstractMediaController {
    protected final ImageProperties imageProperties = new ImageProperties();
    protected final ToggleGroup toggleGroup = new ToggleGroup();

    protected File pathFolderBatchProcessing;
    protected List<File> filesToProcess = new ArrayList<>();

    @FXML protected Button btnSelectFile, btnChoiceFolderForSaveFile, btnSelectBatchFileProcessing, btnSubmit;
    @FXML protected ImageView imageViewPreview;
    @FXML protected StackPane dropZone, previewContainer;
    @FXML protected Label labelSelectFile, textDragZone, labelPreviewPlaceholder;
    @FXML protected VBox parametersContainer;

    @FXML
    public void initialize() {
        imageProperties.setOutput(getSavedPath());
    }

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML
    public void onChoiceFolderForSaveFile() {
        selectOutputDirectory(btnChoiceFolderForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save image");
    }

    @FXML
    public void selectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectImageFile.showOpenMultipleDialog(stage,
                new FileChooser.ExtensionFilter("Images", Global.getSupportedImageFormatsForFileChooser()))
                .ifPresent(files -> {
                    if (!files.isEmpty()) {
                        loadImages(files, false);
                    }
                });
    }

    @FXML
    public void onActionBtnBatchFileProcessing() {
        Stage stage = (Stage) btnSelectBatchFileProcessing.getScene().getWindow();
        directoryChooser(stage, pathFolderBatchProcessing, "Select directory with image")
                .ifPresent(selectedPath -> {
                    pathFolderBatchProcessing = selectedPath;

                    List<File> result = Preparation.getFilesFromFolder(pathFolderBatchProcessing, Global.getAllSupportedImageFormats());
                    filesToProcess = new ArrayList<>(result);

                    if (filesToProcess.isEmpty()) {
                        Alerts.alertDialog(Alert.AlertType.WARNING, "No matching files found", "No matching files found",
                                "No matching files were found in the selected directory.\nPerhaps it only contains unsupported images!");
                        return;
                    }

                    loadImages(filesToProcess, true);
                });
    }

    private String buildSelectFileLabelText(boolean batchSelection) {
        String imageName = imageProperties.getImage().getName();
        int count = filesToProcess.size();

        if (batchSelection) {
            return "Batch: " + count + " images; preview: " + imageName;
        }
        if (count == 1) {
            return "Select image file: " + imageName;
        }
        return "Selected " + count + " images; preview: " + imageName;
    }

    protected void loadImages(List<File> files, boolean batchSelection) {
        if (files.isEmpty()) {
            return;
        }

        filesToProcess = new ArrayList<>(files.stream().filter(this::validateSelectedFile).toList());
        if (filesToProcess.isEmpty()) {
            return;
        }

        enableControls();
        imageProperties.setImage(filesToProcess.getFirst());

        ErrorLogger.info(getClass(), "Selected " + filesToProcess.size() + " image file(s) for conversion.");
        if (nonNull(labelSelectFile)) {
            labelSelectFile.setText(buildSelectFileLabelText(batchSelection));
        }

        if (nonNull(textDragZone)) {
            textDragZone.setText((batchSelection ? "Batch: " : "Selected: ") + filesToProcess.size() + " files");
        }

        imageViewPreview.setImage(null);
        labelPreviewPlaceholder.setVisible(true);

        try {
            Optional<BufferedImage> biOpt = readPreviewImage(imageProperties.getImage());
            if (biOpt.isEmpty()) {
                ErrorLogger.warn("Failed to read preview for file: " + imageProperties.getImage().getName());
                Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Format", "Unsupported image format!");
                return;
            }

            Image fxImage = SwingFXUtils.toFXImage(biOpt.get(), null);

            bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);

            imageViewPreview.setImage(fxImage);
            labelPreviewPlaceholder.setVisible(false);

            if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
                dropZone.getStyleClass().add("drop-zone-filled");
            }

            ErrorLogger.info(getClass(), "Preview loaded successfully for: " + imageProperties.getImage().getName());
        } catch (IOException e) {
            ErrorLogger.log(107, ErrorLogger.Level.ERROR, "Failed load preview", e);
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Failed load preview",
                    "Failed load preview.\nCheck log file for more details!");
        }
    }

    protected boolean checkCommonParameters() {
        if (isNull(imageProperties.getImage())) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "File missing!", "Select image first.");
            return false;
        }

        if (isNull(imageProperties.getOutput())) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "Output missing!", "Select output directory!.");
            return false;
        }

        if (isNull(imageProperties.getTypeImage())) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "Format missing!", "Select photo format!");
            return false;
        }

        return true;
    }

    @FXML
    protected void submitAndDownload() {
        if (!checkCommonParameters()) {
            return;
        }

        List<File> snapshot = filesToProcess.isEmpty() ? List.of(imageProperties.getImage()) : new ArrayList<>(filesToProcess);

        ConvertImageTask task = new ConvertImageTask(
                snapshot,
                imageProperties.getOutput(),
                imageProperties.getTypeImage(),
                imageProperties.getSizeIcoImage()
        );

        task.messageProperty().addListener((_, _, newVal) -> Platform.runLater(() -> labelSuccess.setText(newVal)));

        executeMediaTask(task);
    }
}
