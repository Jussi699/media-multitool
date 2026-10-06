package media_multitool.imageTools;

import com.imagetools.ImageTools;
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
import model.utility.Global;
import model.utility.ResetContext;
import org.jspecify.annotations.NonNull;
import viewHelp.InfoAlert;
import viewHelp.SliderSetup;

import java.io.File;
import java.util.List;

import static java.util.Objects.isNull;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class LightenImageController extends AbstractImageToolController {
    private final ImageProperties imageProperties = new ImageProperties();

    @FXML private Button btnSubmit;
    @FXML private ImageView imageViewPreview;
    @FXML private StackPane previewContainer;
    @FXML private Slider sliderLighten;
    @FXML private Label currentValueSlider;

    private List<Control> listControls;

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
        listControls = List.of(sliderLighten, btnSubmit, btnSubmitAndCopy, btnReset);
        imageProperties.setOutput(getSavedPath());

        setupTooltips();
        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(() -> processedImage, "Lightened");
        sliderLighten.valueProperty().addListener((_, _, _) -> generatePreview());

        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);

        sliderLighten.setMin(0);
        sliderLighten.setMax(255);
        sliderLighten.setValue(0);

        SliderSetup.bindPercentageLabel(sliderLighten, currentValueSlider, 255);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
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
                dropZone, imageViewPreview, progressBar, true, "image"
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
            setImagePreview(processedImage, imageViewPreview);
            return;
        }

        ImageTools.brightnessImage(originalImage, value).ifPresent(lightened -> {
            processedImage = lightened;
            setImagePreview(processedImage, imageViewPreview);
        });
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
