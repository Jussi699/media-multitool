package media_multitool.compressors;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import media_multitool.AbstractMediaController;
import model.compressorImage.Compressor;
import model.compressorImage.CompressionResult;
import model.compressorImage.CompressImageTask;
import model.converterImage.UsefulMethods;
import model.logger.ErrorLogger;
import model.properties.MediaProperties;
import model.properties.ImageProperties;
import model.utility.*;
import viewHelp.Alerts;
import viewHelp.ComboBoxes;
import viewHelp.InfoAlert;

import java.io.File;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.compressorImage.Compressor.calculateEstimatedSizeMB;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.ComboBoxes.*;
import static viewHelp.Message.*;

public class CompressorImageController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();

    @FXML private Button btnSelectFile, btnChoiceDirForSaveFile, btnSubmit;
    @FXML private ImageView imageViewPreview;
    @FXML private StackPane dropZone, previewContainer;
    @FXML private Label textDragZone, labelSelectFile, labelPreviewPlaceholder;
    @FXML private ComboBox<Item> qualityComboBox, scaleComboBox;

    private Control[] controls;

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML
    public void initialize() {
        controls = new Control[] {qualityComboBox, scaleComboBox, btnSubmit, btnReset };

        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);

        initComboBoxes();
        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);
        isPressedReset();

        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "Compressor Image",
                "Select an image file using \"Select image\" or drag and drop it into the dash-bordered zone",
                """
                       3. Configure Scale and Quality settings;

                       4. Click "Compress and Download".

                       For SVG files, the compressor removes unnecessary metadata to reduce file size.

                       You can cancel the conversion at any time using the "Cancel Conversion" button.
                       """
        );
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, null, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true, "image"
        );
        reset(imageProperties, ctx, "Selected image file: none");

        labelSuccess.setText("Estimated size: Waiting load image");
        scaleComboBox.getSelectionModel().selectFirst();
        qualityComboBox.getSelectionModel().selectFirst();
        disableControls();
    }

    @FXML
    public void selectFile() {
        selectInputFile(btnSelectFile,
                new FileChooser.ExtensionFilter("Images", Global.getSupportedImageFormatsForFileChooser()), this::loadFile);
    }

    @FXML
    public void onChoiceFolderForSaveFile() {
        selectOutputDirectory(btnChoiceDirForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save image");
    }

    @FXML
    public void submitAndDownload() {
        String targetFormat = UsefulMethods.normalizeFormat(imageProperties.getTypeImage());
        boolean isSvg = "svg".equalsIgnoreCase(targetFormat);

        if(!validateImageFormat(isSvg, targetFormat)) {
            return;
        }

        Compressor compressor = new Compressor();
        CompressImageTask task = new CompressImageTask(compressor, imageProperties, isSvg);
        executeMediaTask(task);
    }

    private boolean validateImageFormat(boolean isSvg, String targetFormat) {
        boolean qualityRequired = "jpeg".equalsIgnoreCase(targetFormat)
                || "jpg".equalsIgnoreCase(targetFormat)
                || "webp".equalsIgnoreCase(targetFormat);

        return checks(isSvg, qualityRequired);
    }

    @FXML
    public void onChoiceSettingCompressImage(ActionEvent event) {
        ComboBox<Item> source = (ComboBox<Item>) event.getSource();

        String id = source.getId();

        if(isNull(id)) {
            ErrorLogger.error("Source id is null");
            return;
        }

        Item selectedItem;

        switch (id) {
            case "qualityComboBox" -> {
                selectedItem = qualityComboBox.getValue();
                imageProperties.setQuality(nonNull(selectedItem) ? selectedItem.id() : -1);
                ErrorLogger.info(getClass(), "User selected quality: " + imageProperties.getQuality());
                updateEstimatedSize();
            }
            case "scaleComboBox" -> {
                selectedItem = scaleComboBox.getValue();
                imageProperties.setScale((nonNull(selectedItem)) ? selectedItem.id() : -1);
                ErrorLogger.info(getClass(), "User selected scale: " + imageProperties.getScale());
            }
            default -> throw new IllegalStateException("Unexpected id: " + id);
        }

        updateEstimatedSize();
    }

    @Override
    protected void lockUI() {
        toggleUI(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        toggleUI(false);
        btnReset.setDisable(false);
    }

    @Override
    protected void disableControls() {
        toggleControls(true);
    }

    @Override
    protected void enableControls() {
        toggleControls(false);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);

        if (Boolean.FALSE.equals(result)) {
            return;
        }

        if (result instanceof Optional<?> opt && opt.orElse(null) instanceof CompressionResult compressionResult) {
            processCompressionResult(compressionResult);
        } else {
            handleInvalidResult();
        }
    }

    private void loadFile(File selectedFile) {
        if (!validateSelectedFile(selectedFile)) {
            return;
        }

        enableControls();
        imageProperties.setImage(selectedFile);
        imageProperties.setTypeImage(DetermineType.determineFormat(selectedFile).orElse(null));
        labelSelectFile.setText("Select image: " + selectedFile.getName());

        if (nonNull(imageViewPreview)) {
            try {
                imageViewPreview.setImage(new Image(selectedFile.toURI().toString()));
                labelPreviewPlaceholder.setVisible(false);
            } catch (Exception e) {
                ErrorLogger.error("Failed to load preview: " + e.getMessage());
            }
        }

        markDropZoneLoaded(dropZone, textDragZone, selectedFile.getName());
        updateEstimatedSize();
    }

    private void initComboBoxes() {
        qualityComboBox.setValue(new Item(-1, "Quality"));
        scaleComboBox.setValue(new Item(-1, "Scale"));

        ComboBoxes.setupComboBox(qualityComboBox, Item::title);
        ComboBoxes.setupComboBox(scaleComboBox, Item::title);

        qualityComboBox.getItems().addAll(createItemsRound("Quality", "%",
                1.0f, 0.9f, 0.85f,
                0.75f, 0.6f, 0.5f, 0.25f,
                0.15f, 0.10f, 0.05f));

        scaleComboBox.getItems().addAll(createItemsRound("Scale", "%",
                1.0f, 0.9f, 0.85f,
                0.75f, 0.6f, 0.5f, 0.25f,
                0.15f, 0.10f, 0.05f));
    }

    private void processCompressionResult(CompressionResult compressionResult) {
        imageProperties.setCompressedImage(compressionResult.outputFile());

        if (!compressionResult.sizeReduced()) {
            handleSkippedCompression(compressionResult);
            return;
        }

        showSuccessText(labelSuccess, buildSuccessMessage(compressionResult), imageProperties.getHideSuccessMessageTimer());
        showProgressBar(progressBar, imageProperties.getHideSuccessMessageTimer());
    }

    private void handleSkippedCompression(CompressionResult compressionResult) {
        imageProperties.setCompressedImage(null);

        String warningMessage = String.format(Locale.US,
                "Compression skipped: file would not shrink (%s -> %s)",
                formatBytes(compressionResult.originalSizeBytes()),
                formatBytes(compressionResult.compressedSizeBytes()));

        showErrorMessage(labelSuccess, warningMessage, imageProperties.getHideSuccessMessageTimer());

        Alerts.alertDialog(Alert.AlertType.INFORMATION, "Information", "Compression skipped",
                "The compressed file would be larger than the original, so it was not kept.");
    }

    private void handleInvalidResult() {
        showErrorMessage(labelSuccess, "So close, yet no success", imageProperties.getHideSuccessMessageTimer());
        ErrorLogger.warn("Compressed image result is empty or invalid! " + getClass().getName());
    }

    private boolean checks(boolean isSvg, boolean qualityRequired) {
        if (isNull(imageProperties.getImage())) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "File missing!", "Select image first.");
            return false;
        }

        if (isNull(imageProperties.getOutput())) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "Output missing!", "Select output path first.");
            return false;
        }

        if (!isSvg && imageProperties.getScale() == -1) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Setting missing", "Missing settings", "First select Scale!");
            return false;
        }

        if (qualityRequired && imageProperties.getQuality() == -1) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Setting missing", "Missing settings", "First select Quality!");
            return false;
        }
        return true;
    }

    private String buildSuccessMessage(CompressionResult result) {
        return String.format(Locale.US,
                "Compressed to %s | saved %.1f%% (%s -> %s)",
                result.format().toUpperCase(Locale.ROOT),
                result.savedPercent(),
                formatBytes(result.originalSizeBytes()),
                formatBytes(result.compressedSizeBytes()));
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
    }


    private void updateEstimatedSize() {
        if (isNull(imageProperties.getImage())) {
            return;
        }

        try {
            if (nonNull(imageProperties.getHideSuccessMessageTimer())) {
                imageProperties.getHideSuccessMessageTimer().stop();
            }
        } catch (Exception _) {
            // Ignored
        }

        double estimatedMB = calculateEstimatedSizeMB(imageProperties);
        if (estimatedMB <= 0) return;

        showEstimatedSize(labelSuccess, estimatedMB);
    }

    private void toggleControls(boolean disabled) {
        Stream.of(controls).forEach(c -> c.setDisable(disabled));
    }

    private void toggleUI(boolean disabled) {
        btnSelectFile.setDisable(disabled);
        btnChoiceDirForSaveFile.setDisable(disabled);
    }
}
