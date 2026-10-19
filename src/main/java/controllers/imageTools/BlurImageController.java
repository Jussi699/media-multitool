package controllers.imageTools;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import controllers.abstractControllers.AbstractImageToolController;
import model.imageTools.ImageTools;
import model.logger.ErrorLogger;
import model.utility.format.SupportedFormat;
import model.utility.context.ResetContext;
import org.jspecify.annotations.NonNull;
import visual.dialogs.InfoAlert;
import visual.controls.SliderSetup;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;
import java.util.function.DoubleConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.io.PathWorker.getSavedPath;
import static visual.controls.Message.*;

public class BlurImageController extends AbstractImageToolController {
    @FXML private Button btnCancelBlurring;
    @FXML private Label currentValueSlider;
    @FXML private Slider sliderBlurry;

    private Task<?> currentTask;

    @FXML
    public void initialize() {
        imageProperties.setOutput(getSavedPath());

        setupTooltips();
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(() -> processedImage, "Blurred");

        bindingImageViewToPreviewContainer(imagePreview, previewContainer);

        sliderBlurry.setMin(0);
        sliderBlurry.setMax(100);
        sliderBlurry.setValue(0);

        SliderSetup.bindPercentageLabel(sliderBlurry, currentValueSlider, 20);

        isPressedReset();
        setupDragAndDrop(dropZone, SupportedFormat.getAllSupportedImageFormats(), this::loadFile);
    }

    @Override
    protected void generatePreview() {
        if (isNull(originalImage)) {
            return;
        }

        int radius = getSliderValue();

        if (radius == 0) {
            processedImage = originalImage;
            setImagePreview(processedImage, imagePreview);
            return;
        }

        currentTask = new Task<BufferedImage>() {
            @Override
            protected BufferedImage call() throws Exception {
                updateProgress(0, 1.0);
                updateMessage("Updating preview...");
                return ImageTools.blurryImage(
                        originalImage,
                        radius,
                        progress -> updateProgress(progress, 1.0)
                ).orElseThrow(() -> new Exception("Preview generation failed"));
            }
        };

        executeMediaTask(currentTask, "Blur preview update");
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfo(
                "Blur Image",

                "3. Use the slider to set the blur intensity;" +
                "4. Click \"Download\" to apply the effect;",

                "The effect may take a long time to complete.\n" +
                "You can cancel the conversion at any time using the \"Cancel\" button."
        );
    }

    @Override
    protected void lockUI() {
        toggleUI(true);
        btnCancelBlurring.setDisable(false);
    }

    @Override
    protected void unlockUI() {
        toggleUI(false);
        btnCancelBlurring.setDisable(true);
    }

    @Override
    protected void disableControls() {
        setControlsDisabled(true, sliderBlurry, btnSubmit, btnSubmitAndCopy, btnReset);
        btnCancelBlurring.setDisable(true);
    }

    @Override
    protected void enableControls() {
        setControlsDisabled(false, sliderBlurry, btnSubmit, btnSubmitAndCopy, btnReset);
        btnCancelBlurring.setDisable(true);
    }

    @FXML
    private void handleSliderRelease() {
        generatePreview();
    }

    @Override
    protected BufferedImage getFinalImageForDownload(DoubleConsumer progressUpdater) {
        Optional<BufferedImage> blurred = ImageTools.blurryImage(
                originalImage,
                getSliderValue(),
                progressUpdater
        );

        if (blurred.isEmpty()) {
            throw new IllegalStateException("Blurring failed: processed image is empty");
        }

        processedImage = blurred.get();
        return processedImage;
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
            processedImage = bi;
            setImagePreview(processedImage, imagePreview);
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
                labelSelectFile, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imagePreview, progressBar, true, "image"
        );
        reset(imageProperties, ctx, "Selected image file: none");

        processedImage = null;
        originalImage = null;
        sliderBlurry.setValue(0);
        currentValueSlider.setText("100%");
        disableControls();
    }

    private int getSliderValue() {
        return (int) sliderBlurry.getValue();
    }
}
