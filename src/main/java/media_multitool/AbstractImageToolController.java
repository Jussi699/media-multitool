package media_multitool;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.checks.Checking;
import model.converterImage.UsefulMethods;
import model.interfaces.ImageFilterStrategy;
import model.logger.ErrorLogger;
import model.preprocessing.ImagePreprocessing;
import model.properties.ImageProperties;
import model.select.SelectFile;
import model.utility.DetermineType;
import model.utility.Global;
import model.utility.PathWorker;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.DoubleConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static viewHelp.Message.showErrorMessage;

public abstract class AbstractImageToolController extends AbstractMediaController {
    protected BufferedImage originalImage;
    protected BufferedImage processedImage;

    @FXML protected Label labelPreviewPlaceholder, textDragZone, labelSelectFile;
    @FXML protected StackPane dropZone;
    @FXML protected Button btnChooseSaveDirectory;
    @FXML protected Button btnSelectFile;

    protected abstract void generatePreview();

    protected abstract ImageProperties getImageProperties();

    protected abstract ImageView getImageView();

    protected void applyTransformation(ImageFilterStrategy strategy) {
        if (isNull(originalImage)) return;
        try {
            processedImage = strategy.process(originalImage);
            setImagePreview(processedImage, getImageView());
        } catch (Exception e) {
            ErrorLogger.error("Filter failed: " + e.getMessage());
        }
    }

    protected BufferedImage getFinalImageForDownload(DoubleConsumer progressUpdater) {
        progressUpdater.accept(1.0);
        return processedImage;
    }

    @FXML
    protected void submitAndDownload() {
        if (Checking.checkImageAndOutputOnNull(getImageProperties()) || isNull(processedImage)) {
            return;
        }

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                BufferedImage finalImage = getFinalImageForDownload(
                        progress -> updateProgress(progress, 1.0)
                );

                if (isCancelled()) return null;
                if (isNull(finalImage)) throw new IllegalStateException("Image processing failed: result is null");

                updateMessage("Saving image...");

                File outputFile = PathWorker.createOutputFile(
                        getImageProperties().getImage(),
                        getImageProperties().getOutput(),
                        getImageProperties().getTypeImage());

                ImagePreprocessing.downloadImage(finalImage, getImageProperties().getTypeImage(), outputFile);

                updateProgress(1.0, 1.0);

                return outputFile;
            }
        };

        executeMediaTask(task);
    }

    @FXML
    protected void chooseSaveDirectory() {
        selectOutputDirectory(btnChooseSaveDirectory, getImageProperties().getOutput(), getImageProperties()::setOutput, "Select directory for save image");
    }

    @FXML
    protected void selectImages() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Images", Global.getSupportedImageFormatsForFileChooser())).ifPresent(this::loadFile);
    }

    protected void loadFile(File file) {
        if (!validateSelectedFile(file)) {
            return;
        }

        if (isNull(getImageView())) {
            ErrorLogger.warn("Cannot generate preview: imageView is not initialized");
            return;
        }

        originalImage = null;
        processedImage = null;

        getImageView().setImage(null);

        if (nonNull(labelPreviewPlaceholder)) {
            labelPreviewPlaceholder.setVisible(true);
        }

        disableControls();

        getImageProperties().setImage(file);
        getImageProperties().setTypeImage(DetermineType.determineFormat(file).orElse(null));

        if (nonNull(labelSelectFile)) labelSelectFile.setText("Select image: " + file.getName());

        try {
            originalImage = UsefulMethods.readImage(file);
            if (isNull(originalImage)) throw new IllegalArgumentException("Unsupported image format.");

            generatePreview();

            if (nonNull(processedImage) && nonNull(labelPreviewPlaceholder)) {
                labelPreviewPlaceholder.setVisible(false);
            }

            if (nonNull(processedImage)) {
                enableControls();
            } else {
                throw new IllegalStateException("Failed to create image preview.");
            }
        } catch (Exception e) {
            ErrorLogger.error("Failed to load preview: " + e.getMessage());
            showErrorMessage(labelSuccess, "Failed to load image: " + e.getMessage(), getImageProperties().getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        }


        if (nonNull(textDragZone)) {
            textDragZone.setText("Selected: " + file.getName());
        }
        if (nonNull(dropZone) && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }
    }
}
