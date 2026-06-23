package media_multitool.watermarks;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import static model.utility.Util.bindingImageViewToPreviewContainer;
import static model.utility.Util.getSavedPath;
import static viewHelp.Message.*;

public class WatermarkImageController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();

    @FXML private Slider imageScaleSlider;
    @FXML private ScrollPane scrollPaneImage;
    @FXML private StackPane dropZone, previewContainer;
    @FXML private Pane watermarkOverlayPane, cropOverlay;
    @FXML private Label labelSelectImageName, textDragZone, labelPreviewPlaceholder;
    @FXML private ImageView imageViewPreview;
    @FXML private Button btnSelectFile, btnChoiceFolderForSaveFile, btnWatermarkText, btnWatermarkPhoto, btnSubmit;

    private BufferedImage originalBufferedImage;
    private CropHelper cropHelper;
    private List<Control> listControls;
    
    private WatermarkSettings currentWatermarkSettings;
    private Stage textWatermarkStage, photoWatermarkStage;
    private WatermarkTextController textWatermarkController;
    private WatermarkPhotoController photoWatermarkController;
    
    private WatermarkDragHandler dragHandler;
    private WatermarkResizeHandler resizeHandler;
    private WatermarkOverlayManager overlayManager;

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML
    public void initialize() {
        if (imageScaleSlider == null) {
            return;
        }
        
        currentWatermarkSettings = new WatermarkSettings();
        
        listControls = List.of(
                btnSubmit, btnWatermarkText, btnWatermarkPhoto
        );

        imageProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, imageProperties.getHideSuccessMessageTimer(), true);
        cropHelper = new CropHelper(cropOverlay, imageViewPreview, new Rectangle(), scrollPaneImage, previewContainer, imageScaleSlider);

        dragHandler = new WatermarkDragHandler(imageViewPreview);
        resizeHandler = new WatermarkResizeHandler(imageViewPreview);
        overlayManager = new WatermarkOverlayManager(watermarkOverlayPane, previewContainer);
        
        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
        setupWatermarkInteraction();
    }
    
    /**
     * Setup watermark interaction (drag, resize, overlay)
     */
    private void setupWatermarkInteraction() {
        if (imageViewPreview == null || previewContainer == null) {
            return;
        }
        
        imageViewPreview.setPickOnBounds(true);
        
        if (watermarkOverlayPane != null) {
            overlayManager.buildOverlayElements();
        }
        
        dragHandler.setOnUpdate(settings -> {
            dragHandler.setContext(originalBufferedImage, settings);
            updatePreviewWithWatermark();
            updateWatermarkOverlay();
        });
        dragHandler.setOnDragComplete(this::syncSettingsToSubWindows);
        
        resizeHandler.setOnUpdate(settings -> {
            resizeHandler.setContext(originalBufferedImage, settings);
            updatePreviewWithWatermark();
            updateWatermarkOverlay();
        });
        resizeHandler.setOnResizeComplete(this::syncSettingsToSubWindows);
        
        imageViewPreview.setOnMousePressed(event -> {
            dragHandler.setContext(originalBufferedImage, currentWatermarkSettings);
            dragHandler.handleMousePressed(event);
        });
        imageViewPreview.setOnMouseDragged(dragHandler::handleMouseDragged);
        imageViewPreview.setOnMouseReleased(dragHandler::handleMouseReleased);
        imageViewPreview.setOnMouseMoved(dragHandler::handleMouseMoved);
        imageViewPreview.setOnMouseClicked(event -> {
            dragHandler.setContext(originalBufferedImage, currentWatermarkSettings);
            dragHandler.handleMouseClicked(event);
        });
        
        attachResizeHandlers();
    }
    
    /**
     * Attach resize event handlers to overlay handles
     */
    private void attachResizeHandlers() {
        attachResizeHandler(overlayManager.getHandleTL(), "TL");
        attachResizeHandler(overlayManager.getHandleTC(), "TC");
        attachResizeHandler(overlayManager.getHandleTR(), "TR");
        attachResizeHandler(overlayManager.getHandleML(), "ML");
        attachResizeHandler(overlayManager.getHandleMR(), "MR");
        attachResizeHandler(overlayManager.getHandleBL(), "BL");
        attachResizeHandler(overlayManager.getHandleBC(), "BC");
        attachResizeHandler(overlayManager.getHandleBR(), "BR");
    }
    
    /**
     * Attach resize handler to a specific handle
     */
    private void attachResizeHandler(javafx.scene.shape.Rectangle handle, String handleId) {
        handle.setOnMousePressed(event -> {
            resizeHandler.setContext(originalBufferedImage, currentWatermarkSettings);
            resizeHandler.handleMousePressed(event, handleId);
        });
        handle.setOnMouseDragged(event -> {
            resizeHandler.setContext(originalBufferedImage, currentWatermarkSettings);
            resizeHandler.handleMouseDragged(event);
        });
        handle.setOnMouseReleased(resizeHandler::handleMouseReleased);
    }
    
    private void syncSettingsToSubWindows() {
        if (photoWatermarkController != null && photoWatermarkStage != null && photoWatermarkStage.isShowing()) {
            photoWatermarkController.loadSettings(currentWatermarkSettings);
        }
        if (textWatermarkController != null && textWatermarkStage != null && textWatermarkStage.isShowing()) {
            textWatermarkController.loadSettings(currentWatermarkSettings);
        }
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Watermark Image",
                """
                        How to use:
                        1. Select an image file or drag it into the drop zone.
                        \s
                        2. Click "Text" or "Photo" to open watermark settings.
                        \s
                        3. Configure watermark settings - changes appear in real-time.
                        \s
                        4. Click or drag on the preview to reposition the watermark.
                           Drag the handles on corners and edges to resize.
                        \s
                        5. Click "Submit and Download" to save the watermarked image.
                        \s
                        This tool helps you add watermarks to your images.
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

        if (currentWatermarkSettings.getType() == WatermarkSettings.WatermarkType.NONE) {
            showErrorMessage(labelSuccess, "Please configure a watermark first.", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            return;
        }

        WatermarkSettings settingsToSave = currentWatermarkSettings.copy();
        
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

                BufferedImage watermarked = WatermarkRenderer.applyWatermark(originalBufferedImage, settingsToSave);
                ImagePreprocessing.downloadImage(watermarked, imageProperties.getTypeImage(), outputFile);
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
        ErrorLogger.info("Image with watermark saved successfully to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Watermarked image saved!", imageProperties.getHideSuccessMessageTimer());
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
        Util.reset(imageProperties, ctx, "Selected image file: none");

        originalBufferedImage = null;
        currentWatermarkSettings = new WatermarkSettings();
        
        if (cropHelper != null) {
            cropHelper.reset();
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

            updatePreviewWithWatermark();
            updateWatermarkOverlay();

            if (labelPreviewPlaceholder != null) {
                labelPreviewPlaceholder.setVisible(false);
            }
        } catch (Exception e) {
            ErrorLogger.error("Failed to load preview: " + e.getMessage());
            showErrorMessage(labelSuccess, "Failed to load image.", imageProperties.getHideSuccessMessageTimer());
        }

        if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);
    }

    private void updatePreviewWithWatermark() {
        if (originalBufferedImage == null) {
            return;
        }

        Image previewImage = WatermarkRenderer.renderPreview(originalBufferedImage, currentWatermarkSettings);
        if (previewImage != null && imageViewPreview != null) {
            imageViewPreview.setImage(previewImage);
        }
    }

    public void updateWatermarkPreview(WatermarkSettings settings) {
        this.currentWatermarkSettings = settings.copy();
        updatePreviewWithWatermark();
        updateWatermarkOverlay();
    }
    
    private void updateWatermarkOverlay() {
        overlayManager.updateOverlay(currentWatermarkSettings, originalBufferedImage, imageViewPreview);
    }

    public void updateWatermarkPosition(double relX, double relY, WatermarkSettings settings) {
        if (originalBufferedImage == null) {
            return;
        }
        
        int x = (int) (relX * originalBufferedImage.getWidth() - settings.getSize() / 2);
        int y = (int) (relY * originalBufferedImage.getHeight() - settings.getSize() / 2);
        
        x = Math.clamp(x, 0, originalBufferedImage.getWidth() - (int) settings.getSize());
        y = Math.clamp(y, 0, originalBufferedImage.getHeight() - (int) settings.getSize());
        
        settings.setPositionX(x);
        settings.setPositionY(y);
        settings.setUseCustomPosition(true);
        
        updateWatermarkPreview(settings);
    }

    public void handleOpenWindowWatermarkText() {
        try {
            if (textWatermarkStage == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewses/watermark-views/window-watermark-text.fxml"));
                Scene scene = new Scene(loader.load());
                
                textWatermarkController = loader.getController();
                textWatermarkController.setMainController(this);
                
                textWatermarkStage = new Stage();
                textWatermarkStage.initModality(Modality.NONE);
                textWatermarkStage.initOwner(btnWatermarkText.getScene().getWindow());
                textWatermarkStage.setTitle("Text Watermark Settings");
                textWatermarkStage.setScene(scene);
                textWatermarkStage.setMinWidth(400);
                textWatermarkStage.setMinHeight(400);
                textWatermarkStage.setOnCloseRequest(_ -> textWatermarkStage = null);
            }
            
            if (currentWatermarkSettings.getType() == WatermarkSettings.WatermarkType.TEXT) {
                textWatermarkController.loadSettings(currentWatermarkSettings);
            }
            
            if (!textWatermarkStage.isShowing()) {
                textWatermarkStage.show();
            } else {
                textWatermarkStage.toFront();
            }
        } catch (Exception e) {
            ErrorLogger.error("Failed to open text watermark window: " + e.getMessage());
        }
    }

    public void handleOpenWindowWatermarkPhoto() {
        try {
            if (photoWatermarkStage == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewses/watermark-views/window-watermark-photo.fxml"));
                Scene scene = new Scene(loader.load());
                
                photoWatermarkController = loader.getController();
                photoWatermarkController.setMainController(this);
                
                photoWatermarkStage = new Stage();
                photoWatermarkStage.initModality(Modality.NONE);
                photoWatermarkStage.initOwner(btnWatermarkPhoto.getScene().getWindow());
                photoWatermarkStage.setTitle("Photo Watermark Settings");
                photoWatermarkStage.setScene(scene);
                photoWatermarkStage.setMinWidth(400);
                photoWatermarkStage.setMinHeight(400);
                photoWatermarkStage.setOnCloseRequest(_ -> photoWatermarkStage = null);
            }
            
            if (currentWatermarkSettings.getType() == WatermarkSettings.WatermarkType.IMAGE) {
                photoWatermarkController.loadSettings(currentWatermarkSettings);
            }
            
            if (!photoWatermarkStage.isShowing()) {
                photoWatermarkStage.show();
            } else {
                photoWatermarkStage.toFront();
            }
        } catch (Exception e) {
            ErrorLogger.error("Failed to open photo watermark window: " + e.getMessage());
        }
    }
}
