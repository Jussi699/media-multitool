package media_multitool.watermarks;

import javafx.application.Platform;
import javafx.concurrent.Task;
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
import media_multitool.watermarks.viewController.WatermarkPhotoController;
import media_multitool.watermarks.viewController.WatermarkTextController;
import model.helper.MediaHelper;
import model.helper.images.CropHelper;
import model.helper.watermarks.*;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.properties.VideoAndAudioProperties;
import model.select.SelectFile;
import model.utility.DetermineType;
import model.utility.Global;
import model.utility.PathWorker;
import model.utility.ResetContext;
import viewHelp.Alerts;
import viewHelp.OpenWatermarkWindow;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static model.utility.PathWorker.createOutputFile;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class WatermarkVideoController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();
    private final VideoAndAudioProperties videoProperties = new VideoAndAudioProperties();
    private final AtomicBoolean cancelFlag = new AtomicBoolean(false);

    @FXML private Slider imageScaleSlider;
    @FXML private ScrollPane scrollPaneImage;
    @FXML private StackPane dropZone, previewContainer;
    @FXML private Pane watermarkOverlayPane, cropOverlay;
    @FXML private Label labelSelectImageName, textDragZone, labelPreviewPlaceholder;
    @FXML private ImageView imageViewPreview;
    @FXML private Button btnSelectFile, btnChoiceFolderForSaveFile, btnWatermarkText, btnWatermarkPhoto, btnSubmit, btnCancel;

    private BufferedImage originalBufferedImage;
    private CropHelper cropHelper;
    private List<Control> listControls;

    private WatermarkSettings currentWatermarkSettings;
    private Stage textWatermarkStage, photoWatermarkStage;
    private WatermarkTextController textWatermarkController;
    private WatermarkPhotoController photoWatermarkController;

    private WatermarkOverlayManager overlayManager;
    private WatermarkInteractionSetup interactionSetup;

    @Override
    protected MediaProperties getProperties() {
        return videoProperties;
    }

    @FXML
    public void initialize() {
        if(imageScaleSlider == null) {
            return;
        }

        currentWatermarkSettings = new WatermarkSettings();

        listControls = List.of(btnSubmit, btnWatermarkText, btnWatermarkPhoto, btnReset);

        videoProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);
        cropHelper = new CropHelper(cropOverlay, imageViewPreview, new Rectangle(), scrollPaneImage, previewContainer, imageScaleSlider);

        overlayManager = new WatermarkOverlayManager(watermarkOverlayPane, previewContainer);

        interactionSetup = new WatermarkInteractionSetup(
                imageViewPreview,
                previewContainer,
                watermarkOverlayPane,
                new WatermarkDragHandler(imageViewPreview),
                new WatermarkResizeHandler(imageViewPreview),
                overlayManager,
                () -> originalBufferedImage,
                () -> currentWatermarkSettings,
                this::refreshPreview,
                this::syncSettingsToSubWindows
        );

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedVideoFormats(), this::loadFile);
        interactionSetup.setup();

        if (progressBar != null) {
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.setProgress(0);
        }
    }

    private void refreshPreview() {
        updatePreviewWithWatermark();
        updateWatermarkOverlay();
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
                "Watermark Video",
                """
                        How to use:
                        1. Select an video file or drag it into the drop zone.
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
        if (btnCancel != null) btnCancel.setDisable(false);
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnChoiceFolderForSaveFile.setDisable(false);
        btnReset.setDisable(false);
        if (btnCancel != null) btnCancel.setDisable(true);
        cancelFlag.set(false);
    }

    @Override
    protected void disableControls() { listControls.forEach(c -> c.setDisable(true)); }

    @Override
    protected void enableControls()  { listControls.forEach(c -> c.setDisable(false)); }

    @FXML
    private void onActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Video", Global.getSupportedVideoFormatsForFileChooser()),
                "Select video"
        ).ifPresent(this::loadFile);
    }

    @FXML
    private void onChoiceFolderForSaveFile() {
        selectOutputDirectory(btnChoiceFolderForSaveFile, videoProperties.getOutput(), videoProperties::setOutput, "Select directory for save video");
    }

    private boolean checks() {
        if (originalBufferedImage == null) {
            ErrorLogger.error("Error extract image from video file!");
            return false;
        }

        if(videoProperties.getSrcFile() == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "File missing!", "Select video file!");
            return false;
        }

        if(videoProperties.getOutput() == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Output path missing!", "Select output directory!");
            return false;
        }

        if (currentWatermarkSettings.getType() == WatermarkSettings.WatermarkType.NONE) {
            showErrorMessage(labelSuccess, "Please configure a watermark first.", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            return false;
        }

        return true;
    }

    @FXML
    private void cancelProcessing() {
        cancelFlag.set(true);
        cancelCurrentTask();
    }

    @FXML
    private void submitAndDownload() {
        if(checks()) {
            cancelFlag.set(false);
            Task<File> task = new Task<>() {
                @Override
                protected File call() {
                    updateProgress(0, 100);

                    File outputFile = createOutputFile(
                            videoProperties.getSrcFile(),
                            videoProperties.getOutput(),
                            "watermarked",
                            videoProperties.getTargetFormat()
                    );

                        boolean result = WatermarkVideoHelper.applyWatermark(
                                videoProperties.getSrcFile(),
                                outputFile,
                                currentWatermarkSettings.copy(),
                                videoProperties.getTargetFormat(),
                                pct -> updateProgress(pct, 100),
                                cancelFlag
                        );

                        if (isCancelled() || cancelFlag.get()) {
                            WatermarkVideoHelper.deleteFile(outputFile);
                            return null;
                        }

                        if (!result) {
                            throw new RuntimeException("Failed to apply watermark to video!");
                        }

                        updateProgress(100, 100);
                        return outputFile;
                }

            };

            executeMediaTask(task);
            labelSuccess.setManaged(true);
        }
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (result == null || Boolean.FALSE.equals(result)) {
            return;
        }

        File outputFile = (File) result;
        ErrorLogger.info("Video with watermark saved successfully to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Watermarked video saved!", imageProperties.getHideSuccessMessageTimer());
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
    private void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectImageName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, null, true, "video"
        );
        reset(imageProperties, ctx, "Selected video file: none");

        originalBufferedImage = null;
        currentWatermarkSettings = new WatermarkSettings();

        resetSubWindowControllers();

        if (watermarkOverlayPane != null) { overlayManager.clearOverlay(); }
        if (cropHelper != null)           { cropHelper.reset(); }
        disableControls();
    }

    private void resetSubWindowControllers() {
        if (textWatermarkController != null)  { textWatermarkController.resetToDefaults();  }
        if (photoWatermarkController != null) { photoWatermarkController.resetToDefaults(); }
    }

    private void loadFile(File selectedFile) {
        enableControls();
        videoProperties.setSrcFile(selectedFile);
        imageProperties.setImage(selectedFile);

        videoProperties.setTargetFormat(MediaHelper.getFFmpegFormat(DetermineType.determineFormat(selectedFile).orElse(null)));

        labelSelectImageName.setText("Select video: " + selectedFile.getName());
        textDragZone.setText("Select video: " + selectedFile.getName());

        loadImage(selectedFile);

        if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);
    }

    private void loadImage(File selectedFile) {
        if (progressBar != null) {
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        }

        Task<BufferedImage> loadTask = new Task<>() {
            @Override
            protected BufferedImage call() {
                return WatermarkVideoHelper.getRandomImageFromVideo(selectedFile).orElse(null);
            }
        };

        loadTask.setOnSucceeded(_ -> {
            originalBufferedImage = loadTask.getValue();

            if (progressBar != null) {
                progressBar.setProgress(0);
            }

            if (originalBufferedImage == null) {
                showErrorMessage(labelSuccess, "Unsupported video format.", videoProperties.getHideSuccessMessageTimer());
                return;
            }

            interactionSetup.rebuildOverlay();
            updatePreviewWithWatermark();
            updateWatermarkOverlay();

            if (labelPreviewPlaceholder != null) {
                labelPreviewPlaceholder.setVisible(false);
            }
        });

        loadTask.setOnFailed(_ -> {
            if (progressBar != null) progressBar.setProgress(0);
            ErrorLogger.error("Failed to load preview: " + loadTask.getException().getMessage());
            showErrorMessage(labelSuccess, "Failed to load video.", imageProperties.getHideSuccessMessageTimer());
        });

        PathWorker.IO_EXECUTOR.execute(loadTask);
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
        WatermarkDimensionsHelper.applyRelativePosition(relX, relY, settings, originalBufferedImage);
        updateWatermarkPreview(settings);
    }

    public void handleOpenWindowWatermarkText() {
        Stage[] holder = {textWatermarkStage};
        RecordOpenWatermarkWindow record = new RecordOpenWatermarkWindow(
                holder,
                "/viewses/watermark-views/window-watermark-text.fxml",
                "Text Watermark Settings",
                currentWatermarkSettings,
                btnWatermarkText,
                WatermarkSettings.WatermarkType.TEXT
        );

        WatermarkTextController ctrl = new OpenWatermarkWindow().openWatermarkWindow(
                record,
                (WatermarkTextController c) -> {
                    c.setMainVideoController(this);
                    c.setWindowTitle("Text Watermark Settings");
                    textWatermarkController = c;
                },
                c -> c.loadSettings(currentWatermarkSettings)
        );
        textWatermarkStage = holder[0];
        if (ctrl != null) {
            textWatermarkController = ctrl;
            updateWatermarkPreview(ctrl.getSettings());
        }
    }

    public void handleOpenWindowWatermarkPhoto() {
        Stage[] holder = {photoWatermarkStage};
        RecordOpenWatermarkWindow record = new RecordOpenWatermarkWindow(
                holder,
                "/viewses/watermark-views/window-watermark-photo.fxml",
                "Photo Watermark Settings",
                currentWatermarkSettings,
                btnWatermarkPhoto,
                WatermarkSettings.WatermarkType.IMAGE
        );
        WatermarkPhotoController ctrl = new OpenWatermarkWindow().openWatermarkWindow(
                record,
                (WatermarkPhotoController c) -> {
                    c.setMainVideoController(this);
                    c.setWindowTitle("Photo Watermark Settings");
                    photoWatermarkController = c;
                },
                c -> c.loadSettings(currentWatermarkSettings)
        );
        photoWatermarkStage = holder[0];
        if (ctrl != null) {
            photoWatermarkController = ctrl;
            updateWatermarkPreview(ctrl.getSettings());
        }
    }
}
