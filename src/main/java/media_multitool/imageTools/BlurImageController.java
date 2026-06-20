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
import model.preprocessing.ImagePreprocessing;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.*;
import viewHelp.Alerts;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;

import static model.utility.Util.bindingImageViewToPreviewContainer;
import static model.utility.Util.getSavedPath;
import static viewHelp.Message.*;

public class BlurImageController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();
    private BufferedImage originalBufferedImage;
    private BufferedImage currentBufferedImage;

    @FXML private Slider sliderBlurry;
    @FXML private StackPane dropZone;
    @FXML private Button btnSelectFile, btnChoiceFolderForSaveFile, btnSubmit, btnCancel;
    @FXML private Label labelSelectImageName, textDragZone, labelPreviewPlaceholder;
    @FXML private ImageView imageViewPreview;
    @FXML private StackPane previewContainer;

    private Task<?> currentTask;
    private java.util.List<Control> listControls;

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML
    public void initialize() {
        listControls = java.util.List.of(sliderBlurry, btnSubmit);
        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);

        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);

        sliderBlurry.setMin(0);
        sliderBlurry.setMax(20);
        sliderBlurry.setValue(0);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    private void updatePreview(int radius) {
        if (originalBufferedImage == null) {
            return;
        }

        currentTask = new Task<BufferedImage>() {
            @Override
            protected BufferedImage call() throws Exception {
                updateProgress(0, 1.0);
                updateMessage("Updating preview...");
                return ImagePreprocessing.blurryImage(
                        originalBufferedImage,
                        radius,
                        progress -> updateProgress(progress, 1.0)
                ).orElseThrow(() -> new Exception("Preview generation failed"));
            }
        };

        executeMediaTask(currentTask);
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Blur Image",
                """
                        How to use:
                        1. Select an image file using 'Select image' or drag and drop.
                        2. (Optional) Choose a directory for saving the output.
                        3. Use the slider to choose how much you want to blur.
                        4. Click 'Download' to apply the effect.
                        
                        The effect may take a long time to complete!
                        
                        This tool blurry your image.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    @Override
    protected void lockUI() {
        disableControls();
        btnSelectFile.setDisable(true);
        btnChoiceFolderForSaveFile.setDisable(true);
        btnReset.setDisable(true);
        if (btnCancel != null) {
            btnCancel.setVisible(true);
            btnCancel.setManaged(true);
        }
    }

    @Override
    protected void unlockUI() {
        enableControls();
        btnSelectFile.setDisable(false);
        btnChoiceFolderForSaveFile.setDisable(false);
        btnReset.setDisable(false);
        if (btnCancel != null) {
            btnCancel.setVisible(false);
            btnCancel.setManaged(false);
        }
    }

    @Override
    protected void disableControls() {
        if (listControls != null) listControls.forEach(c -> c.setDisable(true));
    }

    @Override
    protected void enableControls() {
        if (listControls != null) listControls.forEach(c -> c.setDisable(false));
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Images", Global.getSupportedImageFormatsForFileChooser()),
                "Choice image"
        ).ifPresent(this::loadFile);
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
        if (Checking.checkImageAndOutputOnNull(imageProperties) || originalBufferedImage == null) {
            return;
        }

        currentTask = new Task<File>() {
            @Override
            protected File call() throws Exception {
                updateProgress(0, 1.0);
                updateMessage("Blurring image...");
                int radius = (int) sliderBlurry.getValue();
                
                Optional<BufferedImage> blurred = ImagePreprocessing.blurryImage(
                        originalBufferedImage, 
                        radius, 
                        progress -> updateProgress(progress, 1.0)
                );

                if (isCancelled()) {
                    return null;
                }

                if (blurred.isEmpty()) {
                    throw new Exception("Blurring failed");
                }

                currentBufferedImage = blurred.get();

                updateMessage("Saving image...");
                File outputFile = Util.createOutputFile(
                        imageProperties.getImage(),
                        imageProperties.getOutput(),
                        imageProperties.getTypeImage()
                );

                ImagePreprocessing.downloadImage(currentBufferedImage, imageProperties.getTypeImage(), outputFile);
                updateProgress(1.0, 1.0);

                return outputFile;
            }
        };

        executeMediaTask(currentTask);
        if (labelSuccess != null) {
            labelSuccess.setManaged(true);
        }
    }

    @FXML
    private void cancelTask() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        if (result instanceof BufferedImage bi) {
            currentBufferedImage = bi;
            setPreview(currentBufferedImage);
            Platform.runLater(() -> {
                if (progressBar != null) {
                    progressBar.setVisible(true);
                    progressBar.setManaged(true);
                }
                if (labelSuccess != null) {
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
        ErrorLogger.info("Image blur successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Blurry image saved!", imageProperties.getHideSuccessMessageTimer());
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
        Util.reset(imageProperties, ctx, "Selected image file: none");

        currentBufferedImage = null;
        originalBufferedImage = null;
        if (sliderBlurry != null) {
            sliderBlurry.setValue(0);
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
                    updatePreview((int) sliderBlurry.getValue());
                    if (labelPreviewPlaceholder != null) {
                        labelPreviewPlaceholder.setVisible(false);
                    }
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
