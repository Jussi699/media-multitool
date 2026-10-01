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
import model.preprocessing.ImagePreprocessing;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.*;
import org.jspecify.annotations.NonNull;
import viewHelp.Alerts;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.PathWorker.createOutputFile;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class BlackWhiteImageController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();
    private BufferedImage originalBufferedImage;
    private BufferedImage currentBufferedImage;

    @FXML private StackPane dropZone, previewContainer;
    @FXML private Button btnSelectFile, btnChoiceFolderForSaveFile, btnSubmit;
    @FXML private Label labelSelectFile, textDragZone, labelPreviewPlaceholder;
    @FXML private ImageView imageViewPreview;

    private List<Control> listControls;

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML
    public void initialize() {
        listControls = List.of(btnSubmit, btnReset, btnSubmitAndCopy);
        imageProperties.setOutput(getSavedPath());

        setupTooltips();

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        setupImageClipboardButton(() -> currentBufferedImage, "Black-white");
        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    private void updatePreview() {
        if (isNull(originalBufferedImage)) {
            return;
        }

        com.imagetools.ImageTools.blackAndWhiteImage(originalBufferedImage).ifPresent(bw -> {
            currentBufferedImage = bw;
            setImagePreview(currentBufferedImage, imageViewPreview);
        });
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Black-White Image",
                """
                        How to use:
                        1. Select an image file using "Select image" or drag and drop it into the dash-bordered zone;

                        2. (Optional) Select where you want to save the result by clicking on "Directory for save".
                            (Default directory: Desktop);

                        3. Click "Download" to apply the effect;

                        4. (Optional) Click "To Clipboard" to copy the image to the clipboard.

                        Certain copied images may not show a preview in the Windows clipboard menu (Win + V).
                        However, the image is still in the clipboard and can be pasted as usual.

                        This tool will make your image black and white.

                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    @Override
    protected void lockUI() {
        btnSelectFile.setDisable(true);
        btnChoiceFolderForSaveFile.setDisable(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnChoiceFolderForSaveFile.setDisable(false);
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
                new FileChooser.ExtensionFilter("Images", Global.getSupportedImageFormatsForFileChooser())).ifPresent(this::loadFile);
    }

    @FXML
    public void onChoiceFolderForSaveFile() {
        selectOutputDirectory(btnChoiceFolderForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save image");
    }

    @FXML
    private void submitAndDownload() {
        if (Checking.checkImageAndOutputOnNull(imageProperties, currentBufferedImage)) {
            return;
        }

        BufferedImage imageToSubmit = currentBufferedImage;
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

                ImagePreprocessing.downloadImage(imageToSubmit, imageProperties.getTypeImage(), outputFile);
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
        ErrorLogger.info(getClass(), "Image black-white successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Black-White image saved!", imageProperties.getHideSuccessMessageTimer());
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
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true, "image"
        );
        reset(imageProperties, ctx, "Selected image file: none");

        currentBufferedImage = null;
        originalBufferedImage = null;
        disableControls();
    }

    private void loadFile(File selectedFile) {
        if (!validateSelectedFile(selectedFile)) {
            return;
        }

        originalBufferedImage = null;
        currentBufferedImage = null;
        if (nonNull(imageViewPreview)) {
            imageViewPreview.setImage(null);
        }
        if (nonNull(labelPreviewPlaceholder)) {
            labelPreviewPlaceholder.setVisible(true);
        }
        disableControls();

        imageProperties.setImage(selectedFile);
        imageProperties.setTypeImage(DetermineType.determineFormat(selectedFile).orElse(null));
        labelSelectFile.setText("Select image: " + selectedFile.getName());

        if (nonNull(imageViewPreview)) {
            try {
                originalBufferedImage = ImageIO.read(selectedFile);
                if (isNull(originalBufferedImage)) {
                    throw new IllegalArgumentException("Unsupported image format.");
                }
                updatePreview();
                if (nonNull(currentBufferedImage) && nonNull(labelPreviewPlaceholder)) {
                    labelPreviewPlaceholder.setVisible(false);
                }
                if (nonNull(currentBufferedImage)) {
                    enableControls();
                } else {
                    throw new IllegalStateException("Failed to create image preview.");
                }
            } catch (Exception e) {
                ErrorLogger.error("Failed to load preview: " + e.getMessage());
                showErrorMessage(labelSuccess, "Failed to load image: " + e.getMessage(), imageProperties.getHideSuccessMessageTimer());
                labelSuccess.setManaged(true);
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
