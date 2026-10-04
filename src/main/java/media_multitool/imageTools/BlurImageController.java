package media_multitool.imageTools;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import model.checks.Checking;
import model.converterImage.UsefulMethods;
import model.preprocessing.ImagePreprocessing;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.*;
import org.jspecify.annotations.NonNull;
import viewHelp.Alerts;
import viewHelp.SliderSetup;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.PathWorker.createOutputFile;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class BlurImageController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();
    private BufferedImage originalBufferedImage, currentBufferedImage;

    @FXML private Slider sliderBlurry;
    @FXML private StackPane dropZone;
    @FXML private Button btnSelectFile, btnChoiceFolderForSaveFile, btnSubmit, btnCancelBlurring;
    @FXML private Label labelSelectImageName, textDragZone, labelPreviewPlaceholder, currentValueSlider;
    @FXML private ImageView imageViewPreview;
    @FXML private StackPane previewContainer;

    private Task<?> currentTask;
    private List<Control> listControls;

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML
    public void initialize() {
        listControls = List.of(sliderBlurry, btnSubmit, btnSubmitAndCopy, btnReset);
        imageProperties.setOutput(getSavedPath());

        setupTooltips();
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(() -> currentBufferedImage, "Blurred");

        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);

        sliderBlurry.setMin(0);
        sliderBlurry.setMax(100);
        sliderBlurry.setValue(0);

        SliderSetup.bindPercentageLabel(sliderBlurry, currentValueSlider, 20);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    private void updatePreview(int radius) {
        if (isNull(originalBufferedImage)) {
            return;
        }

        currentTask = new Task<BufferedImage>() {
            @Override
            protected BufferedImage call() throws Exception {
                updateProgress(0, 1.0);
                updateMessage("Updating preview...");
                return com.imagetools.ImageTools.blurryImage(
                        originalBufferedImage,
                        radius,
                        progress -> updateProgress(progress, 1.0)
                ).orElseThrow(() -> new Exception("Preview generation failed"));
            }
        };

        executeMediaTask(currentTask, "Blur preview update");
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Blur Image",
                """
                        How to use:
                        1. Select an image file using "Select image" or drag and drop it into the dash-bordered zone;

                        2. (Optional) Select where you want to save the result by clicking on "Directory for save".
                            (Default directory: Desktop);

                        3. Use the slider to set the blur intensity;

                        4. Click "Download" to apply the effect;

                        5. (Optional) Click "To Clipboard" to copy the image to the clipboard.

                        Certain copied images may not show a preview in the Windows clipboard menu (Win + V).
                        However, the image is still in the clipboard and can be pasted as usual.

                        The effect may take a long time to complete.

                        This tool will blur your image.

                        You can cancel the conversion at any time using the "Cancel" button.

                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    @Override
    protected void lockUI() {
        disableControls();
        btnSelectFile.setDisable(true);
        btnChoiceFolderForSaveFile.setDisable(true);
        btnReset.setDisable(true);
        btnCancelBlurring.setDisable(false);
    }

    @Override
    protected void unlockUI() {
        enableControls();
        btnSelectFile.setDisable(false);
        btnChoiceFolderForSaveFile.setDisable(false);
        btnReset.setDisable(false);
        btnCancelBlurring.setDisable(true);
    }

    @Override
    protected void disableControls() {
        listControls.forEach(c -> c.setDisable(true));
        btnCancelBlurring.setDisable(true);
    }

    @Override
    protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));
        btnCancelBlurring.setDisable(true);
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Images", Global.getSupportedImageFormatsForFileChooser())).ifPresent(this::loadFile);
    }

    @FXML
    public void onChoiceFolderForSaveFile() {
        selectOutputDirectory(btnChoiceFolderForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save image");
    }

    @FXML
    private void handleSliderRelease() {
        updatePreview((int) sliderBlurry.getValue());
    }

    @FXML
    public void submitAndDownload() {
        if (Checking.checkImageAndOutputOnNull(imageProperties) || isNull(originalBufferedImage)) {
            return;
        }

        currentTask = new Task<File>() {
            @Override
            protected File call() throws Exception {
                updateProgress(0, 1.0);
                updateMessage("Blurring image...");
                int radius = (int) sliderBlurry.getValue();

                Optional<BufferedImage> blurred = com.imagetools.ImageTools.blurryImage(
                        originalBufferedImage,
                        radius,
                        progress -> updateProgress(progress, 1.0)
                );

                if (isCancelled()) {
                    return null;
                }

                if (blurred.isEmpty()) {
                    throw new IllegalStateException("Blurring failed: processed image is empty");
                }

                currentBufferedImage = blurred.get();

                updateMessage("Saving image...");
                File outputFile = createOutputFile(
                        imageProperties.getImage(),
                        imageProperties.getOutput(),
                        imageProperties.getTypeImage()
                );

                ImagePreprocessing.downloadImage(currentBufferedImage, imageProperties.getTypeImage(), outputFile);
                updateProgress(1.0, 1.0);

                return outputFile;
            }
        };

        executeMediaTask(currentTask, "Image blur");
        if (nonNull(labelSuccess)) {
            labelSuccess.setManaged(true);
        }
    }

    @FXML
    private void cancelTask() {
        if (nonNull(currentTask) && currentTask.isRunning()) {
            currentTask.cancel();
        }
        cancelCurrentTask();
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        if (result instanceof BufferedImage bi) {
            currentBufferedImage = bi;
            setImagePreview(currentBufferedImage, imageViewPreview);
            Platform.runLater(() -> {
                if (nonNull(progressBar)) {
                    progressBar.setVisible(true);
                    progressBar.setManaged(true);
                }
                if (nonNull(labelSuccess)) {
                    labelSuccess.setVisible(true);
                    labelSuccess.setManaged(true);
                }
            });
            return;
        }

        super.handleTaskSuccess(result);

        if (Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "Image blur successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Blurry image saved!", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @Override
    protected void handleTaskFailure(@NonNull Throwable exception) {
        super.handleTaskFailure(exception);
        Platform.runLater(() -> {
            showErrorMessage(labelSuccess, "Error: " + exception.getMessage(), imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @FXML
    public void isPressedReset() {
        cancelTask();

        ResetContext ctx = new ResetContext(
                labelSelectImageName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true, "image"
        );
        reset(imageProperties, ctx, "Selected image file: none");

        currentBufferedImage = null;
        originalBufferedImage = null;
        sliderBlurry.setValue(0);
        currentValueSlider.setText("100%");
        disableControls();
    }

    private void loadFile(File selectedFile) {
        if (!validateSelectedFile(selectedFile)) {
            return;
        }

        enableControls();
        imageProperties.setImage(selectedFile);
        imageProperties.setTypeImage(DetermineType.determineFormat(selectedFile).orElse(null));
        labelSelectImageName.setText("Select image: " + selectedFile.getName());

        if (nonNull(imageViewPreview)) {
            try {
                originalBufferedImage = UsefulMethods.readImage(selectedFile);
                if (nonNull(originalBufferedImage)) {
                    updatePreview((int) sliderBlurry.getValue());
                    if (nonNull(labelPreviewPlaceholder)) {
                        labelPreviewPlaceholder.setVisible(false);
                    }
                }
            } catch (Exception e) {
                ErrorLogger.error("Failed to load preview: " + e.getMessage());
            }
        }

        if (nonNull(textDragZone)) {
            textDragZone.setText("Selected: " + selectedFile.getName());
        }
        if (nonNull(dropZone) && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }
    }

}
