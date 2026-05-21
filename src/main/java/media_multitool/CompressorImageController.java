package media_multitool;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.compressorImage.Compressor;
import model.compressorImage.CompressionResult;
import model.converterImage.UsefulMethods;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.select.SelectFile;
import model.utility.DetermineType;
import model.utility.DragDropped;
import model.utility.Item;
import viewHelp.Alerts;
import viewHelp.ComboBoxes;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;

import static model.utility.Util.*;
import static viewHelp.Message.*;

public class CompressorImageController {
    private final String DEFAULT_FILE_TEXT = "Selected image file: none";
    private final ImageProperties imageProperties = new ImageProperties();

    @FXML private StackPane dropZone;
    @FXML private ComboBox<Item> comboBoxOutputQuality;
    @FXML private ComboBox<Item> comboBoxScaleImage;
    @FXML private Button btnSelectPhotoFile;
    @FXML private Button btnChoiceDirForSaveImage;
    @FXML private Label labelSuccessConvert;
    @FXML private Label labelSelectImageName;
    @FXML private Label textDragZone;
    @FXML private ImageView imageViewPreview;
    @FXML private Label labelPreviewPlaceholder;
    @FXML private StackPane previewContainer;

    @FXML
    public void initialize() {
        btnChoiceDirForSaveImage.setTooltip(new Tooltip("Default directory: Desktop"));
        comboBoxOutputQuality.setValue(new Item(-1, "Quality"));
        comboBoxScaleImage.setValue(new Item(-1, "Scale"));

        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccessConvert, imageProperties.getHideSuccessMessageTimer());

        labelSuccessConvert.setVisible(false);
        labelSuccessConvert.setText("Compression status");
        labelSelectImageName.setText(DEFAULT_FILE_TEXT);

        ComboBoxes.setupComboBox(comboBoxOutputQuality, Item::title);
        ComboBoxes.setupComboBox(comboBoxScaleImage, Item::title);

        if (imageViewPreview != null && previewContainer != null) {
            imageViewPreview.fitWidthProperty().bind(previewContainer.widthProperty().subtract(10));
            imageViewPreview.fitHeightProperty().bind(previewContainer.heightProperty().subtract(10));
        }

        onResetPressed();

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
    
    @FXML
    public void ActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectPhotoFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp",
                        "*.tiff", "*.tif", "*.bmp", "*.ppm", "*.pgm", "*.pam", "*.jpe", "*.svg"),
                "Choice image"
        ).ifPresent(this::loadFile);
    }

    @FXML
    public void btnChoiceDirForSaveImage() {
        Stage stage = getStage(btnChoiceDirForSaveImage);
        directoryChooser(stage, imageProperties.getOutput(), "Select directory for save image")
                .ifPresent(imageProperties::setOutput);
    }

    public void submitCompressAndDownload() {
        if (imageProperties.getImage() == null || imageProperties.getOutput() == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "File missing!", "Select image first.");
            return;
        }

        UsefulMethods usefulMethods = new UsefulMethods();
        String targetFormat = usefulMethods.normalizeFormat(imageProperties.getTypeImage());
        boolean isSvg = "svg".equalsIgnoreCase(targetFormat);
        boolean qualityRequired = "jpeg".equalsIgnoreCase(targetFormat) || "jpg".equalsIgnoreCase(targetFormat) || "webp".equalsIgnoreCase(targetFormat);

        if (!isSvg && imageProperties.getScale() == -1) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Setting missing", "Missing settings", "First select Scale!");
            return;
        }

        if (qualityRequired && imageProperties.getQuality() == -1) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Setting missing", "Missing settings", "First select Quality!");
            return;
        }

        btnSelectPhotoFile.setDisable(true);
        btnChoiceDirForSaveImage.setDisable(true);
        hideSuccessMessage(labelSuccessConvert, imageProperties.getHideSuccessMessageTimer());

        CompletableFuture.supplyAsync(() -> {
            try {
                if ("svg".equals(targetFormat)) {
                    return Compressor.removeSvgMetadata(imageProperties);
                } else {
                    return Compressor.compressorStandardImage(imageProperties);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, IO_EXECUTOR).thenAccept(compressionResultOpt -> Platform.runLater(() -> {
            btnSelectPhotoFile.setDisable(false);
            btnChoiceDirForSaveImage.setDisable(false);

            if (compressionResultOpt.isEmpty()) {
                showErrorMessage(labelSuccessConvert, "So close, yet no success", imageProperties.getHideSuccessMessageTimer());
                ErrorLogger.warn("Compressed image has null! " + getClass().getName());
                return;
            }

            CompressionResult compressionResult = compressionResultOpt.get();
            imageProperties.setCompressedImage(compressionResult.outputFile());
            if (!compressionResult.sizeReduced()) {
                imageProperties.setCompressedImage(null);
                String warningMessage = String.format(Locale.US,
                        "Compression skipped: file would not shrink (%s -> %s)",
                        formatBytes(compressionResult.originalSizeBytes()),
                        formatBytes(compressionResult.compressedSizeBytes()));
                showErrorMessage(labelSuccessConvert, warningMessage, imageProperties.getHideSuccessMessageTimer());
                Alerts.alertDialog(Alert.AlertType.INFORMATION, "Information", "Compression skipped",
                        "The compressed file would be larger than the original, so it was not kept.");
                ErrorLogger.info("Compression skipped because output is larger or equal to source: " + imageProperties.getImage().getName());
                return;
            }

            if (imageProperties.getCompressedImage().exists() && imageProperties.getCompressedImage().isFile() && imageProperties.getCompressedImage().length() > 0) {
                showSuccessText(labelSuccessConvert, buildSuccessMessage(compressionResult), imageProperties.getHideSuccessMessageTimer());
                ErrorLogger.info("Compression completed: " + imageProperties.getCompressedImage().getName());
            } else {
                ErrorLogger.warn("Compression finished but file not found or empty: " + imageProperties.getCompressedImage().getName());
                showErrorMessage(labelSuccessConvert, "Compression Failed: File missing", imageProperties.getHideSuccessMessageTimer());
                Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "Missing File",
                        "Compression finished, but saved file was not found.");
            }
        })).exceptionally(e -> {
            Platform.runLater(() -> {
                btnSelectPhotoFile.setDisable(false);
                btnChoiceDirForSaveImage.setDisable(false);
                Throwable cause = e.getCause();
                if (cause instanceof IllegalArgumentException) {
                    ErrorLogger.log(118, ErrorLogger.Level.WARN, "Invalid parameters for compression", cause);
                    showErrorMessage(labelSuccessConvert, "Error: Invalid parameters", imageProperties.getHideSuccessMessageTimer());
                    Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "Invalid Parameters", cause.getMessage());
                } else if (cause instanceof IOException || (cause != null && cause.getCause() instanceof IOException)) {
                    ErrorLogger.log(105, ErrorLogger.Level.ERROR, "IO Error during compression", cause);
                    showErrorMessage(labelSuccessConvert, "Error: " + cause.getMessage(), imageProperties.getHideSuccessMessageTimer());
                    Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Compression Failed", cause.getMessage());
                } else {
                    ErrorLogger.log(1001, ErrorLogger.Level.ERROR, "Unexpected error during conversion", cause);
                    showErrorMessage(labelSuccessConvert, "System Error: " + (cause != null ? cause.getMessage() : e.getMessage()), imageProperties.getHideSuccessMessageTimer());
                    Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "System Error", "Something went wrong: " + (cause != null ? cause.getMessage() : e.getMessage()));
                }
            });
            return null;
        });
    }

    @FXML
    public void onResetPressed() {
        comboBoxOutputQuality.setValue(new Item(-1, "Quality"));
        comboBoxScaleImage.setValue(new Item(-1, "Scale"));

        imageProperties.setImage(null);
        imageProperties.setCompressedImage(null);
        imageProperties.setTypeImage(null);
        imageProperties.setQuality(-1);
        imageProperties.setScale(-1);

        labelSelectImageName.setText(DEFAULT_FILE_TEXT);
        labelSuccessConvert.setVisible(false);
        labelSuccessConvert.setText("");
        hideSuccessMessage(labelSuccessConvert, imageProperties.getHideSuccessMessageTimer());

        if (textDragZone != null) {
            textDragZone.setText("Drag files here");
        }
        if (dropZone != null && dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().remove("drop-zone-filled");
        }

        if (imageViewPreview != null) {
            imageViewPreview.setImage(null);
        }
        if (labelPreviewPlaceholder != null) {
            labelPreviewPlaceholder.setVisible(true);
        }
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Image Compressor",
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


    @FXML
    public void handleDragOver(DragEvent e) {
        DragDropped.handleDragOver(e, List.of(
                ".png", ".jpg", ".jpeg", ".webp", ".tiff", ".tif", ".bmp"), dropZone);
    }

    @FXML
    public void handleDragDropped(DragEvent e) {
        File droppedFile = DragDropped.handleDragDropped(e, dropZone, textDragZone);
        if (droppedFile != null) {
            loadFile(droppedFile);
        }
    }

    private void loadFile(File selectedFile) {
        imageProperties.setImage(selectedFile);
        imageProperties.setTypeImage(DetermineType.determineFormat(selectedFile).orElse(null));
        labelSelectImageName.setText("Select image: " + selectedFile.getName());

        if (imageViewPreview != null) {
            try {
                Image image = new Image(selectedFile.toURI().toString());
                imageViewPreview.setImage(image);
                if (labelPreviewPlaceholder != null) {
                    labelPreviewPlaceholder.setVisible(false);
                }
            } catch (Exception e) {
                ErrorLogger.error("Failed to load preview: " + e.getMessage());
            }
        }

        if (textDragZone != null) {
            textDragZone.setText("Selected: " + selectedFile.getName());
        }
        if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
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

        labelSuccessConvert.setStyle("-fx-text-fill: #32CD32;");
        labelSuccessConvert.setText(String.format(Locale.US, "Estimated size: ~%.2f MB", estimatedMB));
        labelSuccessConvert.setVisible(true);
    }

    private double calculateEstimatedSizeMB() {
        if (imageProperties.getImage() == null) return 0;

        float scale = imageProperties.getScale();
        float quality = imageProperties.getQuality();

        // If not selected, assume 1.0 for estimation purposes
        float effectiveScale = (scale > 0) ? scale : 1.0f;
        float effectiveQuality = (quality > 0) ? quality : 1.0f;

        long originalSizeBytes = imageProperties.getImage().length();

        // Rough heuristic for image compression
        double estimatedBytes = originalSizeBytes * Math.pow(effectiveScale, 2) * effectiveQuality;

        return estimatedBytes / (1024.0 * 1024.0);
    }
}
