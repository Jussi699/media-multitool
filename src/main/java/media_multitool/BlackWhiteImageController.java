package media_multitool;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.preprocessing.ImagePreprocessing;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.select.SelectFile;
import model.utility.*;
import viewHelp.Alerts;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import static model.utility.Util.directoryChooser;
import static model.utility.Util.getSavedPath;
import static viewHelp.Message.*;

public class BlackWhiteImageController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();
    private BufferedImage originalBufferedImage;
    private BufferedImage currentBufferedImage;

    @FXML private StackPane dropZone;
    @FXML private Button btnSelectPhotoFile;
    @FXML private Button btnChoiceDirForSaveImage;
    @FXML private Label labelSelectImageName;
    @FXML private Label textDragZone;
    @FXML private ImageView imageViewPreview;
    @FXML private Label labelPreviewPlaceholder;
    @FXML private StackPane previewContainer;

    @FXML
    public void initialize() {
        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);

        if (imageViewPreview != null && previewContainer != null) {
            imageViewPreview.fitWidthProperty().bind(previewContainer.widthProperty().subtract(10));
            imageViewPreview.fitHeightProperty().bind(previewContainer.heightProperty().subtract(10));
        }

        onResetPressed();
    }

    private void updatePreview() {
        if (originalBufferedImage == null) {
            return;
        }

        ImagePreprocessing.blackAndWhiteImage(originalBufferedImage).ifPresent(bw -> {
            currentBufferedImage = bw;
            setPreview(currentBufferedImage);
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
                        1. Select an image file using 'Select image' or drag and drop.
                        2. (Optional) Choose a directory for saving the output.
                        3. Click 'Download' to apply the effect.
                        
                        This tool will take a black and white your image.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    @Override
    protected void lockUI() {
        btnSelectPhotoFile.setDisable(true);
        btnChoiceDirForSaveImage.setDisable(true);
        if (btnReset != null) btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectPhotoFile.setDisable(false);
        btnChoiceDirForSaveImage.setDisable(false);
        if (btnReset != null) btnReset.setDisable(false);
    }

    @FXML
    public void handleDragOver(DragEvent e) {
        DragDropped.handleDragOver(e, Global.getAllSupportedImageFormats(), dropZone);
    }

    @FXML
    public void handleDragDropped(DragEvent e) {
        File droppedFile = DragDropped.handleDragDropped(e, dropZone, textDragZone);
        if (droppedFile != null) {
            loadFile(droppedFile);
        }
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectPhotoFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Images", Global.getSupportedImageFormatsForFileChooser()),
                "Choice image"
        ).ifPresent(this::loadFile);
    }

    @FXML
    public void btnChoiceDirForSaveImage() {
        Stage stage = (Stage) btnChoiceDirForSaveImage.getScene().getWindow();
        directoryChooser(stage, imageProperties.getOutput(), "Select directory for save image")
                .ifPresent(imageProperties::setOutput);
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

                File outputFile = Util.createOutputFile(
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
            imageProperties.getHideSuccessMessageTimer().playFromStart();
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info("Image black-white successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Black-White image saved!", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @Override
    protected void handleTaskCancelled() {
        super.handleTaskCancelled();
        imageProperties.getHideSuccessMessageTimer().playFromStart();
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
    public void onResetPressed() {
        ResetContext ctx = new ResetContext(
                labelSelectImageName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true
        );
        Util.reset(imageProperties, ctx, "Selected image file: none");

        currentBufferedImage = null;
        originalBufferedImage = null;
    }

    private void loadFile(File selectedFile) {
        imageProperties.setImage(selectedFile);
        imageProperties.setTypeImage(DetermineType.determineFormat(selectedFile).orElse(null));
        labelSelectImageName.setText("Select image: " + selectedFile.getName());

        if (imageViewPreview != null) {
            try {
                originalBufferedImage = ImageIO.read(selectedFile);
                updatePreview();
                if (currentBufferedImage != null) {
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
