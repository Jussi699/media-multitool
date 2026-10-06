package media_multitool.imageTools;

import javafx.application.Platform;
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
import model.properties.MediaProperties;
import model.utility.Global;
import model.utility.ResetContext;
import org.jspecify.annotations.NonNull;
import viewHelp.InfoAlert;
import viewHelp.SliderSetup;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.function.DoubleConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class DarkenImageController extends AbstractImageToolController {
    private final ImageProperties imageProperties = new ImageProperties();

    @FXML private Button btnSubmit;
    @FXML private ImageView imageViewPreview;
    @FXML private StackPane previewContainer;
    @FXML private Slider sliderDarken;
    @FXML private Label currentValueSlider;

    private List<Control> listControls;

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @Override
    protected ImageProperties getImageProperties() {
        return imageProperties;
    }

    @Override
    protected ImageView getImageView() {
        return imageViewPreview;
    }

    @FXML
    public void initialize() {
        listControls = List.of(sliderDarken, btnSubmit, btnSubmitAndCopy, btnReset);
        imageProperties.setOutput(getSavedPath());

        setupTooltips();
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(() -> processedImage, "Darkened");
        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);
        sliderDarken.valueProperty().addListener((_, _, _) -> generatePreview());

        sliderDarken.setMin(0);
        sliderDarken.setMax(255);
        sliderDarken.setValue(0);

        SliderSetup.bindPercentageLabel(sliderDarken, currentValueSlider, 255);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfo(
                "Darken Image",
                """
                        3. Use the slider to set how much you want to darken the image;
                        
                        4. Click "Darken and Download;
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true, "image"
        );
        reset(imageProperties, ctx, "Selected image file: none");

        originalImage = null;
        processedImage = null;
        sliderDarken.setValue(0);
        currentValueSlider.setText("100%");
        disableControls();
    }

    @FXML
    private void handleSliderRelease() {
        generatePreview();
    }

    @Override
    protected void generatePreview() {
        if (isNull(originalImage)) {
            return;
        }

        int value = getSliderValue();

        if (value == 0) {
            processedImage = originalImage;
            setImagePreview(processedImage, imageViewPreview);
            return;
        }

        if (isNull(processedImage)) {
            processedImage = originalImage;
            setImagePreview(processedImage, imageViewPreview);
        }

        com.imagetools.ImageTools.brightnessImage(originalImage, -value).ifPresent(darkened -> {
            processedImage = darkened;
            setImagePreview(processedImage, imageViewPreview);
        });
    }

    @Override
    protected BufferedImage getFinalImageForDownload(DoubleConsumer progressUpdater) {
        var darkened = com.imagetools.ImageTools.brightnessImage(
                originalImage,
                getSliderValue()
        );

        if (darkened.isEmpty()) {
            throw new IllegalStateException("Darkening failed: processed image is empty");
        }

        processedImage = darkened.get();
        return processedImage;
    }

    @Override
    protected void lockUI() {
        btnSelectFile.setDisable(true);
        btnChooseSaveDirectory.setDisable(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnChooseSaveDirectory.setDisable(false);
        btnReset.setDisable(false);
    }

    @Override
    protected void disableControls() {
        listControls.forEach(c -> c.setDisable(true));
    }

    @Override
    protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        if (result instanceof BufferedImage bi) {
            processedImage = bi;
            setImagePreview(processedImage, imageViewPreview);
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
        ErrorLogger.info(getClass(), "Image darkening successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Darkened image saved!", imageProperties.getHideSuccessMessageTimer());
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

    private int getSliderValue() {
        return (int) sliderDarken.getValue();
    }
}
