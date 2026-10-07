package media_multitool.imageTools;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import media_multitool.AbstractImageToolController;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.utility.Global;
import model.utility.ResetContext;
import org.jspecify.annotations.NonNull;
import viewHelp.InfoAlert;
import viewHelp.SliderSetup;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class BlurImageController extends AbstractImageToolController {
    private final ImageProperties imageProperties = new ImageProperties();

    @FXML private Slider sliderBlurry;
    @FXML private Button btnSubmit, btnCancelBlurring;
    @FXML private Label currentValueSlider;
    @FXML private ImageView preview;
    @FXML private StackPane previewContainer;

    private Task<?> currentTask;
    private List<Control> listControls;

    @Override
    protected ImageProperties getImageProperties() {
        return imageProperties;
    }

    @Override
    protected ImageView getImageView() {
        return preview;
    }

    @FXML
    public void initialize() {
        listControls = List.of(sliderBlurry, btnSubmit, btnSubmitAndCopy, btnReset);
        imageProperties.setOutput(getSavedPath());

        setupTooltips();
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(() -> processedImage, "Blurred");

        bindingImageViewToPreviewContainer(preview, previewContainer);

        sliderBlurry.setMin(0);
        sliderBlurry.setMax(100);
        sliderBlurry.setValue(0);

        SliderSetup.bindPercentageLabel(sliderBlurry, currentValueSlider, 20);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    @Override
    protected void generatePreview() {
        if (isNull(originalImage)) {
            return;
        }

        int radius = getSliderValue();

        if (radius == 0) {
            processedImage = originalImage;
            setImagePreview(processedImage, preview);
            return;
        }

        currentTask = new Task<BufferedImage>() {
            @Override
            protected BufferedImage call() throws Exception {
                updateProgress(0, 1.0);
                updateMessage("Updating preview...");
                return com.imagetools.ImageTools.blurryImage(
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
        disableControls();
        btnSelectFile.setDisable(true);
        btnChooseSaveDirectory.setDisable(true);
        btnReset.setDisable(true);
        btnCancelBlurring.setDisable(false);
    }

    @Override
    protected void unlockUI() {
        enableControls();
        btnSelectFile.setDisable(false);
        btnChooseSaveDirectory.setDisable(false);
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
    private void handleSliderRelease() {
        generatePreview();
    }

    @Override
    protected BufferedImage getFinalImageForDownload(DoubleConsumer progressUpdater) {
        Optional<BufferedImage> blurred = com.imagetools.ImageTools.blurryImage(
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
            setImagePreview(processedImage, preview);
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
                dropZone, preview, progressBar, true, "image"
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
