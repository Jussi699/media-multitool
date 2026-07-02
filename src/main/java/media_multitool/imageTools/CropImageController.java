package media_multitool.imageTools;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import model.checks.Checking;
import model.helper.images.CropHelper;
import model.logger.ErrorLogger;
import model.preprocessing.ImagePreprocessing;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.*;
import viewHelp.Alerts;
import viewHelp.ZoomControlHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import static model.utility.PathWorker.*;
import static viewHelp.Message.*;

public class CropImageController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();

    @FXML private Slider imageScaleSlider;
    @FXML private ScrollPane scrollPaneImage;
    @FXML private StackPane dropZone, previewContainer;
    @FXML private Pane cropOverlay;
    @FXML private Label labelSelectImageName, textDragZone, labelPreviewPlaceholder;
    @FXML private ImageView imageViewPreview;
    @FXML private Rectangle cropRect;

    @FXML private Button btnSelectFile, btnChoiceFolderForSaveFile, btnAspectRatioSquare, btnAspectRatio9x16, btnAspectRatio16x9, btnAspectRatio4x5,
            btnAspectRatio3x4, btnAspectRatio5x4, btnAspectRatio4x3,btnAspectRatio2x3, btnAspectRatio3x2,btnAspectRatio5x7,btnAspectRatio7x5,
            btnAspectRatio1x2,btnAspectRatio2x1, btnSubmit;

    private BufferedImage originalBufferedImage;
    private ZoomControlHelper zoomControlHelper;
    private CropHelper cropHelper;
    private List<Control> listControls;

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML
    public void initialize() {
        listControls = List.of(
                btnAspectRatioSquare, btnAspectRatio9x16, btnAspectRatio16x9, btnAspectRatio4x5,
                btnAspectRatio3x4, btnAspectRatio5x4, btnAspectRatio4x3, btnAspectRatio2x3,
                btnAspectRatio3x2, btnAspectRatio5x7, btnAspectRatio7x5, btnAspectRatio1x2,
                btnAspectRatio2x1, imageScaleSlider, btnSubmit
        );

        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, imageProperties.getHideSuccessMessageTimer(), true);
        zoomControlHelper = new ZoomControlHelper(scrollPaneImage, imageViewPreview, imageScaleSlider, previewContainer, 1.0, 3.0);
        cropHelper = new CropHelper(cropOverlay, imageViewPreview, cropRect, scrollPaneImage, previewContainer, imageScaleSlider);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Crop Image",
                """
                        How to use:
                        1. Select an image file or drag it into the drop zone.
                        \s
                        2. A crop zone appears automatically on load.
                        \s
                        3. Controls:
                        * Drag the crop zone to move it anywhere on the image.
                        * Drag the white handles (corners / edges) to resize the crop zone.
                        * Use the aspect-ratio buttons to snap to a fixed ratio.
                        \s
                        4. Use the slider or mouse wheel to zoom in for precision.
                        \s
                        5. Click "Crop and Download" to save the selected area.
                        \s
                        If you have any questions or problems, please go to Info and write to me on Discord.
                        """
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
                new FileChooser.ExtensionFilter("Images", Global.getSupportedImageFormatsForFileChooser()),
                "Select image"
        ).ifPresent(this::loadFile);
    }

    @FXML
    public void onChoiceFolderForSaveFile() {
        selectOutputDirectory(btnChoiceFolderForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save image");
    }

    @FXML
    public void submitAndDownload() {
        if (Checking.checkImageAndOutputOnNull(imageProperties) || originalBufferedImage == null) {
            return;
        }

        CropHelper.CropArea cropArea = cropHelper.getCropArea();
        if (cropArea == null) {
            showErrorMessage(labelSuccess, "Select crop area first.", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            return;
        }

        CropHelper.CropArea cropToSave = cropArea.copy();
        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);

                File outputFile = createOutputFile(
                        imageProperties.getImage(),
                        imageProperties.getOutput(),
                        imageProperties.getTypeImage()
                );

                int x = Math.clamp((int) Math.floor(cropToSave.x()), 0, originalBufferedImage.getWidth() - 1);
                int y = Math.clamp((int) Math.floor(cropToSave.y()), 0, originalBufferedImage.getHeight() - 1);
                int width = Math.clamp((int) Math.round(cropToSave.width()), 1, originalBufferedImage.getWidth() - x);
                int height = Math.clamp((int) Math.round(cropToSave.height()), 1, originalBufferedImage.getHeight() - y);

                updateProgress(50, 100);

                BufferedImage cropped = copyImage(originalBufferedImage.getSubimage(x, y, width, height));
                ImagePreprocessing.downloadImage(cropped, imageProperties.getTypeImage(), outputFile);
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
        ErrorLogger.info("Image cropped successfully to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Cropped image saved!", imageProperties.getHideSuccessMessageTimer());
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
                dropZone, imageViewPreview, null, true
        );
        reset(imageProperties, ctx, "Selected image file: none");

        originalBufferedImage = null;
        if (cropHelper != null) {
            cropHelper.reset();
        }

        if (zoomControlHelper != null) {
            zoomControlHelper.resetZoom();
        }
        disableControls();
    }

    private void loadFile(File selectedFile) {
        enableControls();
        imageProperties.setImage(selectedFile);
        imageProperties.setTypeImage(DetermineType.determineFormat(selectedFile).orElse(null));

        labelSelectImageName.setText("Select image: " + selectedFile.getName());
        textDragZone.setText("Select image: " + selectedFile.getName());

        try {
            originalBufferedImage = ImageIO.read(selectedFile);
            if (originalBufferedImage == null) {
                showErrorMessage(labelSuccess, "Unsupported image format.", imageProperties.getHideSuccessMessageTimer());
                return;
            }

            zoomControlHelper.resetZoom();
            setPreview(originalBufferedImage);

            if (labelPreviewPlaceholder != null) {
                labelPreviewPlaceholder.setVisible(false);
            }

            Platform.runLater(() -> {
                zoomControlHelper.updateImageSize();
                cropHelper.setOriginalBufferedImage(originalBufferedImage);
                cropHelper.createDefaultCrop();
                cropHelper.updateCropOverlay();
            });
        } catch (Exception e) {
            ErrorLogger.error("Failed to load preview: " + e.getMessage());
            showErrorMessage(labelSuccess, "Failed to load image.", imageProperties.getHideSuccessMessageTimer());
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

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    public void onActionSelectAspectRatio(ActionEvent event) {
        Object source = event.getSource();

        if      (source == btnAspectRatioSquare)  cropHelper.setupAspectRatio(1.0, 1.0);
        else if (source == btnAspectRatio9x16)    cropHelper.setupAspectRatio(9.0, 16.0);
        else if (source == btnAspectRatio16x9)    cropHelper.setupAspectRatio(16.0, 9.0);
        else if (source == btnAspectRatio4x5)     cropHelper.setupAspectRatio(4.0, 5.0);
        else if (source == btnAspectRatio5x4)     cropHelper.setupAspectRatio(5.0, 4.0);
        else if (source == btnAspectRatio3x4)     cropHelper.setupAspectRatio(3.0, 4.0);
        else if (source == btnAspectRatio4x3)     cropHelper.setupAspectRatio(4.0, 3.0);
        else if (source == btnAspectRatio2x3)     cropHelper.setupAspectRatio(2.0, 3.0);
        else if (source == btnAspectRatio3x2)     cropHelper.setupAspectRatio(3.0, 2.0);
        else if (source == btnAspectRatio5x7)     cropHelper.setupAspectRatio(5.0, 7.0);
        else if (source == btnAspectRatio7x5)     cropHelper.setupAspectRatio(7.0, 5.0);
        else if (source == btnAspectRatio1x2)     cropHelper.setupAspectRatio(1.0, 2.0);
        else if (source == btnAspectRatio2x1)     cropHelper.setupAspectRatio(2.0, 1.0);
    }
}
