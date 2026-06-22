package media_multitool.compressors;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import model.compressorImage.Compressor;
import model.compressorImage.CompressionResult;
import model.compressorImage.CompressImageTask;
import model.converterImage.UsefulMethods;
import model.logger.ErrorLogger;
import model.properties.MediaProperties;
import model.properties.ImageProperties;
import model.select.SelectFile;
import model.utility.*;
import viewHelp.Alerts;
import viewHelp.ComboBoxes;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static model.utility.Util.*;
import static viewHelp.Message.*;

public class CompressorImageController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML private StackPane dropZone, previewContainer;
    @FXML private ComboBox<Item> comboBoxOutputQuality, comboBoxScaleImage;
    @FXML private Button btnSelectImageFile, btnChoiceDirForSaveImage, btnSubmit;
    @FXML private Label textDragZone, labelSelectFile, labelPreviewPlaceholder;
    @FXML private ImageView imageViewPreview;

    private List<Control> listControls;

    @FXML
    public void initialize() {
        listControls = List.of(comboBoxOutputQuality, comboBoxScaleImage, btnSubmit);

        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);

        initComboBoxes();
        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);
        isPressedReset();

        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    private void initComboBoxes() {
        comboBoxOutputQuality.setValue(new Item(-1, "Quality"));
        comboBoxScaleImage.setValue(new Item(-1, "Scale"));

        ComboBoxes.setupComboBox(comboBoxOutputQuality, Item::title);
        ComboBoxes.setupComboBox(comboBoxScaleImage, Item::title);

        comboBoxOutputQuality.getItems().addAll(
                new Item(-1f, "Quality"),
                new Item(1.0f, "100%"), new Item(0.9f, "90%"),
                new Item(0.85f, "85%"), new Item(0.75f, "75%"),
                new Item(0.6f, "60%"), new Item(0.5f, "50%"),
                new Item(0.25f, "25%"), new Item(0.15f, "15%"),
                new Item(0.10f, "10%"), new Item(0.05f, "5%")
        );

        comboBoxScaleImage.getItems().addAll(
                new Item(-1f, "Scale"),
                new Item(1.0f, "100%"), new Item(0.9f, "90%"),
                new Item(0.85f, "85%"), new Item(0.75f, "75%"),
                new Item(0.6f, "60%"), new Item(0.5f, "50%"),
                new Item(0.25f, "25%"), new Item(0.15f, "15%"),
                new Item(0.10f, "10%"), new Item(0.05f, "5%")
        );
    }

    @Override
    protected void lockUI() {
        btnSelectImageFile.setDisable(true);
        btnChoiceDirForSaveImage.setDisable(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectImageFile.setDisable(false);
        btnChoiceDirForSaveImage.setDisable(false);
        btnReset.setDisable(false);
    }

    @Override
    protected void disableControls() {
        if (listControls != null) listControls.forEach(c -> c.setDisable(true));
    }

    @Override
    protected void enableControls() {
        if (listControls != null) listControls.forEach(c -> c.setDisable(false));
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);

        if (Boolean.FALSE.equals(result)) {
            return;
        }

        if (result instanceof Optional<?> opt) {
            if (opt.isPresent() && opt.get() instanceof CompressionResult compressionResult) {

                imageProperties.setCompressedImage(compressionResult.outputFile());

                if (!compressionResult.sizeReduced()) {
                    imageProperties.setCompressedImage(null);
                    String warningMessage = String.format(Locale.US,
                            "Compression skipped: file would not shrink (%s -> %s)",
                            formatBytes(compressionResult.originalSizeBytes()),
                            formatBytes(compressionResult.compressedSizeBytes()));

                    showErrorMessage(labelSuccess, warningMessage, imageProperties.getHideSuccessMessageTimer());

                    Alerts.alertDialog(Alert.AlertType.INFORMATION, "Information", "Compression skipped",
                            "The compressed file would be larger than the original, so it was not kept.");
                    return;
                }

                showSuccessText(labelSuccess, buildSuccessMessage(compressionResult), imageProperties.getHideSuccessMessageTimer());
                showProgressBar(progressBar, imageProperties.getHideSuccessMessageTimer());

            } else {
                showErrorMessage(labelSuccess, "So close, yet no success", imageProperties.getHideSuccessMessageTimer());
                ErrorLogger.warn("Compressed image result is empty or invalid! " + getClass().getName());
            }
        }
    }
    
    @FXML
    public void ActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectImageFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Images", Global.getSupportedImageFormatsForFileChooser()),
                "Select image"
        ).ifPresent(this::loadFile);
    }

    @FXML
    public void btnChoiceDirForSaveImage() {
        selectOutputDirectory(btnChoiceDirForSaveImage, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save image");
    }

    private boolean checks(boolean isSvg, boolean qualityRequired) {
        if (imageProperties.getImage() == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "File missing!", "Select image first.");
            return false;
        }

        if (imageProperties.getOutput() == null) {
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

    @FXML
    public void submitCompressAndDownload() {
        String targetFormat = UsefulMethods.normalizeFormat(imageProperties.getTypeImage());
        boolean isSvg = "svg".equalsIgnoreCase(targetFormat);
        boolean qualityRequired = "jpeg".equalsIgnoreCase(targetFormat) || "jpg".equalsIgnoreCase(targetFormat) || "webp".equalsIgnoreCase(targetFormat);

        if(!checks(isSvg, qualityRequired)) {
            return;
        }

        Compressor compressor = new Compressor();
        CompressImageTask task = new CompressImageTask(compressor, imageProperties, isSvg);
        executeMediaTask(task);
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true
        );
        Util.reset(imageProperties, ctx, "Selected image file: none");

        comboBoxOutputQuality.setValue(new Item(-1, "Quality"));
        comboBoxScaleImage.setValue(new Item(-1, "Scale"));
        disableControls();
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Compressor Image",
                """
                        How to use:
                        1. Select an image file using 'Select image'.
                        2. (Optional) Choose a directory for saving the output.
                        3. Select Scale and Quality settings.
                        4. Click 'Convert and Download'.
                        
                        For SVG files, the compressor removes unnecessary metadata to reduce file size.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    @FXML
    public void onChoiceScaleImage() {
        Item selectedItem = comboBoxScaleImage.getValue();
        imageProperties.setScale((selectedItem != null) ? selectedItem.id() : -1);
        ErrorLogger.info("User select scale: " + imageProperties.getScale());
        updateEstimatedSize();
    }

    @FXML
    public void onChoiceOutputQuality() {
        Item selectedItem = comboBoxOutputQuality.getValue();
        imageProperties.setQuality((selectedItem != null) ? selectedItem.id() : -1);
        ErrorLogger.info("User select quality: " + imageProperties.getQuality());
        updateEstimatedSize();
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


    private void loadFile(File selectedFile) {
        enableControls();
        imageProperties.setImage(selectedFile);
        imageProperties.setTypeImage(DetermineType.determineFormat(selectedFile).orElse(null));
        labelSelectFile.setText("Select image: " + selectedFile.getName());

        if (imageViewPreview != null) {
            try {
                Image image = new Image(selectedFile.toURI().toString());
                imageViewPreview.setImage(image);

                labelPreviewPlaceholder.setVisible(false);
            } catch (Exception e) {
                ErrorLogger.error("Failed to load preview: " + e.getMessage());
            }
        }

        textDragZone.setText("Selected: " + selectedFile.getName());

        if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        updateEstimatedSize();
    }

    private void updateEstimatedSize() {
        if (imageProperties.getImage() == null) {
            return;
        }

        try {
            if (imageProperties.getHideSuccessMessageTimer() != null) {
                imageProperties.getHideSuccessMessageTimer().stop();
            }
        } catch (Exception ignored) {}

        double estimatedMB = calculateEstimatedSizeMB();
        if (estimatedMB <= 0) return;

        labelSuccess.setStyle("-fx-text-fill: #32CD32;");
        labelSuccess.setText(String.format(Locale.US, "Estimated size: ~%.2f MB", estimatedMB));
        labelSuccess.setVisible(true);
    }

    private double calculateEstimatedSizeMB() {
        if (imageProperties.getImage() == null) return 0;

        float scale = imageProperties.getScale();
        float quality = imageProperties.getQuality();

        float effectiveScale = (scale > 0) ? scale : 1.0f;
        float effectiveQuality = (quality > 0) ? quality : 1.0f;

        long originalSizeBytes = imageProperties.getImage().length();

        double estimatedBytes = originalSizeBytes * Math.pow(effectiveScale, 2) * effectiveQuality;

        return estimatedBytes / (1024.0 * 1024.0);
    }
}
