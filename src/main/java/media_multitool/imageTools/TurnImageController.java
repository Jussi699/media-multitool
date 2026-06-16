package media_multitool.imageTools;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.embed.swing.SwingFXUtils;
import media_multitool.AbstractMediaController;
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

import static model.utility.Util.*;
import static viewHelp.Message.*;

public class TurnImageController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    private BufferedImage currentBufferedImage;

    @FXML private StackPane dropZone, previewContainer;
    @FXML private Button btnSelectFile, btnChoiceFolderForSave, btnFlipHorizontally, btnFlipVertically, btnTurnImageRight, btnTurnImageLeft, btnSubmit;
    @FXML private Label labelSelectFile, textDragZone, labelPreviewPlaceholder;
    @FXML private ImageView imageViewPreview;

    private List<Button> listControls;

    @FXML
    public void initialize() {
        listControls = List.of(btnFlipHorizontally, btnFlipVertically, btnTurnImageRight, btnTurnImageLeft, btnSubmit);
        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);

        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Turn",
                """
                        How to use:
                        1. Select an image file using 'Select image' or drag and drop.
                        2. (Optional) Choose a directory for saving the output.
                        3. Select which direction you want to rotate or flip the image by pressing the corresponding button.
                        4. Click 'Turn and Download' to apply the effect.

                        This tool turns or flips your image.

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
    public void btnChoiceFolderForSave() {
        selectOutputDirectory(btnChoiceFolderForSave, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save image");
    }

    @FXML
    private void onActionTurnImage(ActionEvent event) {
        if (currentBufferedImage == null) {
            return;
        }

        Button source = (Button) event.getSource();

        String side = switch (source.getId()) {
            case "btnFlipHorizontally" -> "flip_horizontally";
            case "btnFlipVertically" -> "flip_vertically";
            case "btnTurnImageRight" -> "turn_right";
            case "btnTurnImageLeft" -> "turn_left";

            default -> throw new IllegalStateException("Unexpected value: " + source.getId());
        };


        ImagePreprocessing.turnImage(currentBufferedImage, side).ifPresent(rotated -> {
            currentBufferedImage = rotated;
            setPreview(currentBufferedImage);
        });
    }

    @FXML
    public void submitTurnAndDownload() {
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
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info("Image rotation successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Rotated image saved!", imageProperties.getHideSuccessMessageTimer());
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
                labelSelectFile, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true
        );
        reset(imageProperties, ctx, "Selected image file: none");

        currentBufferedImage = null;
        disableControls();
    }

    private void loadFile(File selectedFile) {
        enableControls();
        imageProperties.setImage(selectedFile);
        imageProperties.setTypeImage(DetermineType.determineFormat(selectedFile).orElse(null));
        labelSelectFile.setText("Select image: " + selectedFile.getName());

        if (imageViewPreview != null) {
            try {
                currentBufferedImage = ImageIO.read(selectedFile);
                if (currentBufferedImage != null) {
                    setPreview(currentBufferedImage);
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
            Image image = SwingFXUtils.toFXImage(bi, null);
            imageViewPreview.setImage(image);
        }
    }
}
