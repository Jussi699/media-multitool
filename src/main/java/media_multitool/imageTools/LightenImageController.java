package media_multitool.imageTools;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import model.checks.Checking;
import model.preprocessing.ImagePreprocessing;
import model.logger.ErrorLogger;
import model.properties.MediaProperties;
import model.properties.ImageProperties;
import model.select.SelectFile;
import model.utility.*;
import viewHelp.Alerts;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import static model.utility.PathWorker.createOutputFile;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class LightenImageController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML private Slider sliderLighten;
    @FXML private StackPane dropZone, previewContainer;
    @FXML private Button btnSelectFile, btnChoiceFolderForSave, btnSubmit;
    @FXML private Label labelSelectImageName, textDragZone, labelPreviewPlaceholder;
    @FXML private ImageView imageViewPreview;

    private List<Control> listControls;
    private BufferedImage originalBufferedImage, currentBufferedImage;

    @FXML
    public void initialize() {
        listControls = List.of(sliderLighten, btnSubmit);
        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);

        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);

        sliderLighten.setMin(0);
        sliderLighten.setMax(255);
        sliderLighten.setValue(0);
        sliderLighten.valueProperty().addListener((_, _, newValue) -> updatePreview(newValue.intValue()));

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    private void updatePreview(int offset) {
        if (originalBufferedImage == null) {
            return;
        }

        ImagePreprocessing.brightnessImage(originalBufferedImage, offset).ifPresent(lightened -> {
            currentBufferedImage = lightened;
            setPreview(currentBufferedImage);
        });
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Lighten Image",
                """
                        How to use:
                        1. Select an image file using 'Select image' or drag and drop.
                        2. (Optional) Choose a directory for saving the output.
                        3. Use the slider to select several photos you want to lighten.
                        4. Click 'Lighten and Download' to apply the effect.
                        
                        This tool lighten your image.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    @Override
    protected void lockUI() {
        btnSelectFile.setDisable(true);
        btnChoiceFolderForSave.setDisable(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnChoiceFolderForSave.setDisable(false);
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
    public void onActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Images", Global.getSupportedImageFormatsForFileChooser()),
                "Select image"
        ).ifPresent(this::loadFile);
    }

    @FXML
    public void btnChoiceFolderForSave() {
        selectOutputDirectory(btnChoiceFolderForSave, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save image");
    }

    @FXML
    private void handleSliderRelease() {
        updatePreview((int) sliderLighten.getValue());
    }

    @FXML
    public void submitAndDownload() {
        if (Checking.checkImageAndOutputOnNull(imageProperties) || currentBufferedImage == null) {
            return;
        }

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);

                File outputFile = createOutputFile(
                        imageProperties.getImage(),
                        imageProperties.getOutput(),
                        imageProperties.getTypeImage()
                );

                updateProgress(50, 100);

                ImagePreprocessing.downloadImage(currentBufferedImage, imageProperties.getTypeImage(), outputFile);
                updateProgress(100, 100);

                return outputFile;
            }
        };

        executeMediaTask(task);
        labelSuccess.setManaged(true);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info("Image lightening successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Lightened image saved!", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @Override
    protected void handleTaskFailure(Throwable exception) {
        super.handleTaskFailure(exception);
        Platform.runLater(() -> {
            showErrorMessage(labelSuccess, "Error: " + exception.getMessage(), imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectImageName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true
        );
        reset(imageProperties, ctx, "Selected image file: none");

        currentBufferedImage = null;
        originalBufferedImage = null;
        if (sliderLighten != null) {
            sliderLighten.setValue(0);
        }
        disableControls();
    }

    private void loadFile(File selectedFile) {
        enableControls();
        imageProperties.setImage(selectedFile);
        imageProperties.setTypeImage(DetermineType.determineFormat(selectedFile).orElse(null));
        labelSelectImageName.setText("Select image: " + selectedFile.getName());

        if (imageViewPreview != null) {
            try {
                originalBufferedImage = ImageIO.read(selectedFile);
                if (originalBufferedImage != null) {
                    updatePreview((int) sliderLighten.getValue());
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
    }

    private void setPreview(BufferedImage bi) {
        if (bi != null && imageViewPreview != null) {
            Image image = SwingFXUtils.toFXImage(bi, null);
            imageViewPreview.setImage(image);
        }
    }
}
