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
import model.helper.watermarks.*;
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

import static model.utility.PathWorker.createOutputFile;
import static model.utility.PathWorker.getSavedPath;
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
        
        listControls = List.of(btnSubmit, btnWatermarkText, btnWatermarkPhoto);

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
        
        // Listen for container size changes and update overlay position
        previewContainer.widthProperty().addListener((_, _, _) -> {
            if (originalBufferedImage != null && currentWatermarkSettings.getType() != WatermarkSettings.WatermarkType.NONE) {
                updateWatermarkOverlay();
            }
        });
        previewContainer.heightProperty().addListener((_, _, _) -> {
            if (originalBufferedImage != null && currentWatermarkSettings.getType() != WatermarkSettings.WatermarkType.NONE) {
                updateWatermarkOverlay();
            }
        });
        
        // Listen for imageView size changes
        imageViewPreview.fitWidthProperty().addListener((_, _, _) -> {
            if (originalBufferedImage != null && currentWatermarkSettings.getType() != WatermarkSettings.WatermarkType.NONE) {
                updateWatermarkOverlay();
            }
        });
        imageViewPreview.fitHeightProperty().addListener((_, _, _) -> {
            if (originalBufferedImage != null && currentWatermarkSettings.getType() != WatermarkSettings.WatermarkType.NONE) {
                updateWatermarkOverlay();
            }
        });
        
        // Configure drag handler callbacks
        dragHandler.setOnUpdate(settings -> {
            dragHandler.setContext(originalBufferedImage, settings);
            updatePreviewWithWatermark();
            updateWatermarkOverlay();
        });
        dragHandler.setOnDragComplete(this::syncSettingsToSubWindows);
        
        // Configure resize handler callbacks
        resizeHandler.setOnUpdate(settings -> {
            resizeHandler.setContext(originalBufferedImage, settings);
            updatePreviewWithWatermark();
            updateWatermarkOverlay();
        });
        resizeHandler.setOnResizeComplete(this::syncSettingsToSubWindows);
        
        // Wire mouse events to drag handler
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
        
        // Attach resize handlers to corner handles only
        WatermarkOverlayManager.HandlePosition[] cornerHandles = {
            WatermarkOverlayManager.HandlePosition.TL,
            WatermarkOverlayManager.HandlePosition.TR,
            WatermarkOverlayManager.HandlePosition.BL,
            WatermarkOverlayManager.HandlePosition.BR
        };
        for (WatermarkOverlayManager.HandlePosition pos : cornerHandles) {
            javafx.scene.shape.Rectangle handle = overlayManager.getHandle(pos);
            if (handle != null) {
                attachResizeHandler(handle, pos.name());
            }
        }
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
    
    /**
     * Attach resize handlers to all overlay handles (called after rebuild)
     */
    private void attachAllResizeHandlers() {
        WatermarkOverlayManager.HandlePosition[] cornerHandles = {
            WatermarkOverlayManager.HandlePosition.TL,
            WatermarkOverlayManager.HandlePosition.TR,
            WatermarkOverlayManager.HandlePosition.BL,
            WatermarkOverlayManager.HandlePosition.BR
        };
        for (WatermarkOverlayManager.HandlePosition pos : cornerHandles) {
            javafx.scene.shape.Rectangle handle = overlayManager.getHandle(pos);
            if (handle != null) {
                attachResizeHandler(handle, pos.name());
            }
        }
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

                File outputFile = createOutputFile(
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
        reset(imageProperties, ctx, "Selected image file: none");

        originalBufferedImage = null;
        currentWatermarkSettings = new WatermarkSettings();

        if (watermarkOverlayPane != null) {
            overlayManager.clearOverlay();
        }

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

            if (watermarkOverlayPane != null) {
                overlayManager.buildOverlayElements();
                attachAllResizeHandlers();
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

    /**
     * Open or bring to front a watermark sub-window.
     * Eliminates the copy-pasted window creation logic.
     */
    private <T> T openWatermarkWindow(
            Stage[] stageHolder, String fxmlPath, String title,
            Button ownerButton, java.util.function.Consumer<T> controllerSetup,
            WatermarkSettings.WatermarkType expectedType, java.util.function.Consumer<T> settingsLoader
    ) {
        try {
            T controller;
            if (stageHolder[0] == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Scene scene = new Scene(loader.load());
                
                controller = loader.getController();
                controllerSetup.accept(controller);
                
                Stage stage = new Stage();
                stage.initModality(Modality.NONE);
                stage.initOwner(ownerButton.getScene().getWindow());
                stage.setTitle(title);
                stage.setScene(scene);
                stage.setMinWidth(400);
                stage.setMinHeight(400);
                stage.setOnCloseRequest(_ -> stageHolder[0] = null);
                stageHolder[0] = stage;
            } else {
                controller = null;
            }
            
            if (controller != null && currentWatermarkSettings.getType() == expectedType) {
                settingsLoader.accept(controller);
            }
            
            if (!stageHolder[0].isShowing()) {
                stageHolder[0].show();
            } else {
                stageHolder[0].toFront();
            }
            
            return controller;
        } catch (Exception e) {
            ErrorLogger.error("Failed to open " + title + ": " + e.getMessage());
            return null;
        }
    }

    public void handleOpenWindowWatermarkText() {
        Stage[] holder = {textWatermarkStage};
        WatermarkTextController ctrl = openWatermarkWindow(
                holder,
                "/viewses/watermark-views/window-watermark-text.fxml",
                "Text Watermark Settings",
                btnWatermarkText,
                (WatermarkTextController c) -> {
                    c.setMainController(this);
                    textWatermarkController = c;
                },
                WatermarkSettings.WatermarkType.TEXT,
                c -> c.loadSettings(currentWatermarkSettings)
        );
        textWatermarkStage = holder[0];
        if (ctrl != null) textWatermarkController = ctrl;
    }

    public void handleOpenWindowWatermarkPhoto() {
        Stage[] holder = {photoWatermarkStage};
        WatermarkPhotoController ctrl = openWatermarkWindow(
                holder,
                "/viewses/watermark-views/window-watermark-photo.fxml",
                "Photo Watermark Settings",
                btnWatermarkPhoto,
                (WatermarkPhotoController c) -> {
                    c.setMainController(this);
                    photoWatermarkController = c;
                },
                WatermarkSettings.WatermarkType.IMAGE,
                c -> c.loadSettings(currentWatermarkSettings)
        );
        photoWatermarkStage = holder[0];
        if (ctrl != null) photoWatermarkController = ctrl;
    }
}
