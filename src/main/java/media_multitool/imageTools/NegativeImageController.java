package media_multitool.imageTools;

import javafx.application.Platform;
import javafx.concurrent.Task;
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
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.*;
import viewHelp.Alerts;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import static model.utility.PathWorker.createOutputFile;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class NegativeImageController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML private StackPane dropZone, previewContainer;
    @FXML private Button btnSelectFile, btnChoiceDirForSaveFile, btnSubmit;
    @FXML private Label labelSelectFileName, textDragZone, labelPreviewPlaceholder;
    @FXML private ImageView imageViewPreview;

    private BufferedImage originalBufferedImage, currentBufferedImage;
    private List<Control> listControls;

    @FXML
    public void initialize() {
        listControls = List.of(btnSubmit);
        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    @Override
    protected void lockUI() {
        btnSelectFile.setDisable(true);
        btnChoiceDirForSaveFile.setDisable(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnChoiceDirForSaveFile.setDisable(false);
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
    public void onActionChoiceDirForSaveFile() {
        selectOutputDirectory(btnChoiceDirForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save image");
    }

    @FXML
    public void submitNegativeAndDownload() {
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
        ErrorLogger.info("Image negative successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Negative image saved!", imageProperties.getHideSuccessMessageTimer());
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
                labelSelectFileName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true
        );
        reset(imageProperties, ctx, "Selected image file: none");
        originalBufferedImage = null;
        currentBufferedImage = null;
        disableControls();
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Negative Image",
                """
                        How to use:
                        1. Select an image file using 'Select image' or drag and drop.
                        2. (Optional) Choose a directory for saving the output.
                        3. Click 'Negative and Download' to apply the effect.
                        
                        This tool creates a negative version of your image.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    private void updatePreview() {
        if (originalBufferedImage == null) {
            return;
        }

        ImagePreprocessing.toNegative(imageProperties.getImage()).ifPresent(negative -> {
            currentBufferedImage = negative;
            setPreview(currentBufferedImage);
        });
    }

    private void loadFile(File selectedFile) {
        enableControls();
        imageProperties.setImage(selectedFile);
        imageProperties.setTypeImage(DetermineType.determineFormat(selectedFile).orElse(null));
        labelSelectFileName.setText("Select image: " + selectedFile.getName());

        if (imageViewPreview != null) {
            try {
                originalBufferedImage = javax.imageio.ImageIO.read(selectedFile);
                updatePreview();
                if (currentBufferedImage != null) {
                    labelPreviewPlaceholder.setVisible(false);
                }
            } catch (Exception e) {
                ErrorLogger.error("Failed to load preview: " + e.getMessage());
            }
        }

        textDragZone.setText("Selected: " + selectedFile.getName());

        if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }
    }

    private void setPreview(BufferedImage bi) {
        if (bi != null && imageViewPreview != null) {
            Image image = javafx.embed.swing.SwingFXUtils.toFXImage(bi, null);
            imageViewPreview.setImage(image);
        }
    }
}
