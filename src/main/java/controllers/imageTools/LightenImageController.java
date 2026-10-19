package controllers.imageTools;

import javafx.application.Platform;
import javafx.fxml.FXML;
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
import java.io.File;

import static java.util.Objects.isNull;
import static model.utility.io.PathWorker.getSavedPath;
import static visual.controls.Message.*;

public class LightenImageController extends AbstractImageToolController {
    @FXML private Slider sliderLighten;
    @FXML private Label currentValueSlider;

    @FXML
    public void initialize() {
        imageProperties.setOutput(getSavedPath());

        setupTooltips();
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(() -> processedImage, "Lightened");
        sliderLighten.valueProperty().addListener((_, _, _) -> generatePreview());

        bindingImageViewToPreviewContainer(imagePreview, previewContainer);

        sliderLighten.setMin(0);
        sliderLighten.setMax(255);
        sliderLighten.setValue(0);

        SliderSetup.bindPercentageLabel(sliderLighten, currentValueSlider, 255);

        isPressedReset();
        setupDragAndDrop(dropZone, SupportedFormat.getAllSupportedImageFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfo(
                "Lighten Image",
                """
                        3. Use the slider to set how much you want to lighten the image;
                        
                        4. Click "Lighten and Download";
                        """
        );
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imagePreview, progressBar, true, "image"
        );
        reset(imageProperties, ctx, "Selected image file: none");

        processedImage = null;
        originalImage = null;
        sliderLighten.setValue(0);
        currentValueSlider.setText("100%");
        disableControls();
    }

    @Override
    protected void generatePreview() {
        if (isNull(originalImage)) {
            return;
        }

        int value = getSliderValue();

        if (value == 0) {
            processedImage = originalImage;
            setImagePreview(processedImage, imagePreview);
            return;
        }

        ImageTools.brightnessImage(originalImage, value).ifPresent(lightened -> {
            processedImage = lightened;
            setImagePreview(processedImage, imagePreview);
        });
    }


    @Override
    protected void disableControls() {
        setControlsDisabled(true, sliderLighten, btnSubmit, btnSubmitAndCopy, btnReset);
    }

    @Override
    protected void enableControls() {
        setControlsDisabled(false, sliderLighten, btnSubmit, btnSubmitAndCopy, btnReset);
    }

    @FXML
    private void handleSliderRelease() {
        generatePreview();
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "Image lightening successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Lightened image saved!", imageProperties.getHideSuccessMessageTimer());
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
        return (int) sliderLighten.getValue();
    }
}
