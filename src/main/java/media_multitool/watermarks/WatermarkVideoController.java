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
import model.exceptions.ImageProcessingException;
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
import org.jspecify.annotations.NonNull;
import viewHelp.Alerts;
import viewHelp.OpenWatermarkWindow;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
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
    @FXML private Button btnDeleteWatermark;
    @FXML private Button btnEditWatermark;
    @FXML private ListView<WatermarkSettings> listWatermarks;

    private BufferedImage originalBufferedImage;
    private CropHelper cropHelper;
    private List<Control> listControls;

    private WatermarkSettings currentWatermarkSettings;
    private final List<WatermarkSettings> watermarkSettingsList = new ArrayList<>();
    private int selectedWatermarkIndex = -1;
    private WatermarkTextController textWatermarkController;
    private WatermarkPhotoController photoWatermarkController;
    private WatermarkOverlayManager overlayManager;
    private WatermarkInteractionSetup interactionSetup;
    private Stage textWatermarkStage, photoWatermarkStage;


    @Override
    protected MediaProperties getProperties() {
        return videoProperties;
    }

    @FXML
    public void initialize() {
        if(isNull(imageScaleSlider)) {
            return;
        }

        currentWatermarkSettings = new WatermarkSettings();

        listControls = List.of(btnSubmit, btnWatermarkText, btnWatermarkPhoto, btnReset,
                listWatermarks, btnDeleteWatermark, btnEditWatermark);

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
                () -> watermarkSettingsList,
                this::selectWatermark,
                this::refreshPreview,
                this::syncSettingsToSubWindows
        );

        isPressedReset();
        setupWatermarkList();
        setupDragAndDrop(dropZone, Global.getAllSupportedVideoFormats(), this::loadFile);
        interactionSetup.setup();

        if (nonNull(progressBar)) {
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
        if (nonNull(photoWatermarkController) && nonNull(photoWatermarkStage) && photoWatermarkStage.isShowing()) {
            photoWatermarkController.loadSettings(currentWatermarkSettings);
        }
        if (nonNull(textWatermarkController) && nonNull(textWatermarkStage) && textWatermarkStage.isShowing()) {
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
                        1. Select a video file using "Select video" or drag and drop it into the dash-bordered zone;

                        2. (Optional) Select where you want to save the result by clicking on "Directory for save".
                            (Default directory: Desktop);

                        3. Click "Add Text" or "Add Photo" to create watermarks; select one to edit or delete it;

                        4. Drag a watermark to move it, or drag its handles to resize it;

                        5. Click "Submit and Download".

                        This tool helps you add watermarks to your videos.

                        You can cancel the conversion at any time using the "Cancel" button.

                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    @Override
    protected void lockUI() {
        btnSelectFile.setDisable(true);
        btnChoiceFolderForSaveFile.setDisable(true);
        btnReset.setDisable(true);
        btnCancel.setDisable(false);
    }

    @Override
    protected void unlockUI() {
        btnSelectFile.setDisable(false);
        btnChoiceFolderForSaveFile.setDisable(false);
        btnReset.setDisable(false);
        btnCancel.setDisable(true);
        cancelFlag.set(false);
    }

    @Override
    protected void disableControls() { listControls.forEach(c -> c.setDisable(true)); }

    @Override
    protected void enableControls()  {
        listControls.forEach(c -> c.setDisable(false));
        updateDeleteButton();
        updateEditButton();
    }

    @FXML
    private void onActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Video", Global.getSupportedVideoFormatsForFileChooser())).ifPresent(this::loadFile);
    }

    @FXML
    private void onChoiceFolderForSaveFile() {
        selectOutputDirectory(btnChoiceFolderForSaveFile, videoProperties.getOutput(), videoProperties::setOutput, "Select directory for save video");
    }

    private boolean checks() {
        if (isNull(originalBufferedImage)) {
            ErrorLogger.error("Error extract image from video file!");
            return false;
        }

        if(isNull(videoProperties.getSrcFile())) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "File missing!", "Select video file!");
            return false;
        }

        if(isNull(videoProperties.getOutput())) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Output path missing!", "Select output directory!");
            return false;
        }

        if (watermarkSettingsList.stream().noneMatch(this::isConfigured)) {
            showErrorMessage(labelSuccess, "Please configure a watermark first.", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            return false;
        }

        return true;
    }

    @FXML
    private void cancelProcessing() {
        cancelFlag.set(true);
    }

    @FXML
    private void submitAndDownload() {
        if(checks()) {
            List<WatermarkSettings> settingsToApply = copyWatermarkSettings();
            cancelFlag.set(false);
            Task<File> task = new Task<>() {
                @Override
                protected File call() {
                    updateProgress(0, 100);
                    if (cancelFlag.get()) {
                        return null;
                    }

                    File outputFile = createOutputFile(
                            videoProperties.getSrcFile(),
                            videoProperties.getOutput(),
                            "watermarked",
                            videoProperties.getTargetFormat()
                    );

                        boolean result = WatermarkVideoHelper.applyWatermarks(
                                videoProperties.getSrcFile(),
                                outputFile,
                                settingsToApply,
                                videoProperties.getTargetFormat(),
                                pct -> updateProgress(pct, 100),
                                cancelFlag
                        );

                        if (isCancelled() || cancelFlag.get()) {
                            WatermarkVideoHelper.deleteFile(outputFile);
                            return null;
                        }

                        if (!result) {
                            WatermarkVideoHelper.deleteFile(outputFile);
                            ErrorLogger.error("Failed to apply watermark to image!");
                            throw new ImageProcessingException("Failed to apply watermark to video!");
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
        if (isNull(result) || Boolean.FALSE.equals(result)) {
            if (isNull(result)) {
                handleTaskCancelled();
            } else {
                super.handleTaskSuccess(result);
            }
            return;
        }
        super.handleTaskSuccess(result);

        File outputFile = (File) result;
        ErrorLogger.info(getClass(), "Video with watermark saved successfully to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Watermarked video saved!", imageProperties.getHideSuccessMessageTimer());
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
    private void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectImageName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, null, true, "video"
        );
        reset(imageProperties, ctx, "Selected video file: none");

        originalBufferedImage = null;
        currentWatermarkSettings = new WatermarkSettings();
        watermarkSettingsList.clear();
        listWatermarks.getItems().clear();
        selectedWatermarkIndex = -1;
        updateDeleteButton();
        updateEditButton();
        updateEditButton();

        resetSubWindowControllers();

        if (nonNull(watermarkOverlayPane)) { overlayManager.clearOverlay(); }
        if (nonNull(cropHelper))           { cropHelper.reset(); }
        disableControls();
    }

    private void resetSubWindowControllers() {
        if (nonNull(textWatermarkController))  { textWatermarkController.resetToDefaults();  }
        if (nonNull(photoWatermarkController)) { photoWatermarkController.resetToDefaults(); }
    }

    private void loadFile(File selectedFile) {
        if (!validateSelectedFile(selectedFile)) {
            return;
        }

        enableControls();
        videoProperties.setSrcFile(selectedFile);
        imageProperties.setImage(selectedFile);

        videoProperties.setTargetFormat(MediaHelper.getFFmpegFormat(DetermineType.determineFormat(selectedFile).orElse(null)));

        labelSelectImageName.setText("Select video: " + selectedFile.getName());
        textDragZone.setText("Select video: " + selectedFile.getName());

        loadImage(selectedFile);

        if (nonNull(dropZone) && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);
    }

    private void loadImage(File selectedFile) {
        if (nonNull(progressBar)) {
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        }

        Task<BufferedImage> loadTask = new Task<>() {
            @Override
            protected BufferedImage call() {
                return WatermarkVideoHelper.getRandomImageFromVideo(selectedFile).orElse(null);
            }
        };

        loadTask.setOnSucceeded(_ -> {
            originalBufferedImage = loadTask.getValue();

            if (nonNull(progressBar)) {
                progressBar.setProgress(0);
            }

            if (isNull(originalBufferedImage)) {
                showErrorMessage(labelSuccess, "Unsupported video format.", videoProperties.getHideSuccessMessageTimer());
                return;
            }

            interactionSetup.rebuildOverlay();
            updatePreviewWithWatermark();
            updateWatermarkOverlay();

            if (nonNull(labelPreviewPlaceholder)) {
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
        if (isNull(originalBufferedImage)) {
            return;
        }

        Image previewImage = WatermarkRenderer.renderPreview(originalBufferedImage, watermarkSettingsList);
        if (nonNull(previewImage) && nonNull(imageViewPreview)) {
            imageViewPreview.setImage(previewImage);
        }
    }

    public void updateWatermarkPreview(WatermarkSettings settings) {
        if (selectedWatermarkIndex < 0 || selectedWatermarkIndex >= watermarkSettingsList.size()) {
            return;
        }
        this.currentWatermarkSettings = settings.copy();
        watermarkSettingsList.set(selectedWatermarkIndex, currentWatermarkSettings);
        listWatermarks.refresh();
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
        addWatermark(WatermarkSettings.WatermarkType.TEXT);
        openTextWatermarkWindow();
    }

    public void handleOpenWindowWatermarkPhoto() {
        addWatermark(WatermarkSettings.WatermarkType.IMAGE);
        openPhotoWatermarkWindow();
    }

    @FXML
    private void editSelectedWatermark() {
        if (currentWatermarkSettings == null) return;
        if (currentWatermarkSettings.getType() == WatermarkSettings.WatermarkType.TEXT) {
            openTextWatermarkWindow();
        } else if (currentWatermarkSettings.getType() == WatermarkSettings.WatermarkType.IMAGE) {
            openPhotoWatermarkWindow();
        }
    }

    @FXML
    private void deleteSelectedWatermark() {
        if (selectedWatermarkIndex < 0 || selectedWatermarkIndex >= watermarkSettingsList.size()) return;
        int removedIndex = selectedWatermarkIndex;
        watermarkSettingsList.remove(removedIndex);
        listWatermarks.getItems().remove(removedIndex);
        selectedWatermarkIndex = -1;
        currentWatermarkSettings = new WatermarkSettings();
        if (!watermarkSettingsList.isEmpty()) {
            selectWatermark(Math.min(removedIndex, watermarkSettingsList.size() - 1));
        } else {
            listWatermarks.getSelectionModel().clearSelection();
            refreshPreview();
        }
        updateDeleteButton();
        updateEditButton();
    }

    private void setupWatermarkList() {
        listWatermarks.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(WatermarkSettings item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else if (item.getType() == WatermarkSettings.WatermarkType.TEXT) setText("Text: " + item.getText());
                else setText("Photo watermark");
            }
        });
        listWatermarks.getSelectionModel().selectedIndexProperty().addListener((_, _, index) -> {
            if (index.intValue() >= 0 && index.intValue() < watermarkSettingsList.size()) {
                selectWatermark(index.intValue());
            }
        });
        updateDeleteButton();
    }

    private void selectWatermark(int index) {
        if (index < 0 || index >= watermarkSettingsList.size()) return;
        selectedWatermarkIndex = index;
        currentWatermarkSettings = watermarkSettingsList.get(index);
        listWatermarks.getSelectionModel().select(index);
        updateDeleteButton();
        updateEditButton();
        refreshPreview();
        syncSettingsToSubWindows();
    }

    private void addWatermark(WatermarkSettings.WatermarkType type) {
        WatermarkSettings settings = new WatermarkSettings();
        settings.setType(type);
        watermarkSettingsList.add(settings);
        listWatermarks.getItems().add(settings);
        selectWatermark(watermarkSettingsList.size() - 1);
    }

    private boolean isConfigured(WatermarkSettings settings) {
        return settings.getType() == WatermarkSettings.WatermarkType.TEXT
                || (settings.getType() == WatermarkSettings.WatermarkType.IMAGE && settings.getWatermarkImage() != null);
    }

    private List<WatermarkSettings> copyWatermarkSettings() {
        return watermarkSettingsList.stream().map(WatermarkSettings::copy).toList();
    }

    private void updateDeleteButton() {
        if (btnDeleteWatermark != null) btnDeleteWatermark.setDisable(watermarkSettingsList.isEmpty());
    }

    private void updateEditButton() {
        if (btnEditWatermark != null) btnEditWatermark.setDisable(selectedWatermarkIndex < 0);
    }

    private void openTextWatermarkWindow() {
        Stage[] holder = {textWatermarkStage};
        RecordOpenWatermarkWindow watermarkSettings = new RecordOpenWatermarkWindow(
                holder,
                "/viewses/watermark-views/window-watermark-text.fxml",
                "Text Watermark Settings",
                currentWatermarkSettings,
                btnWatermarkText,
                WatermarkSettings.WatermarkType.TEXT
        );

        WatermarkTextController ctrl = new OpenWatermarkWindow().openWatermarkWindow(
                watermarkSettings,
                (WatermarkTextController c) -> {
                    c.setMainVideoController(this);
                    c.setWindowTitle("Text Watermark Settings");
                    textWatermarkController = c;
                },
                c -> c.loadSettings(currentWatermarkSettings)
        );
        textWatermarkStage = holder[0];
        if (nonNull(ctrl)) {
            textWatermarkController = ctrl;
        }
    }

    private void openPhotoWatermarkWindow() {
        Stage[] holder = {photoWatermarkStage};
        RecordOpenWatermarkWindow watermarkSettings = new RecordOpenWatermarkWindow(
                holder,
                "/viewses/watermark-views/window-watermark-photo.fxml",
                "Photo Watermark Settings",
                currentWatermarkSettings,
                btnWatermarkPhoto,
                WatermarkSettings.WatermarkType.IMAGE
        );
        WatermarkPhotoController ctrl = new OpenWatermarkWindow().openWatermarkWindow(
                watermarkSettings,
                (WatermarkPhotoController c) -> {
                    c.setMainVideoController(this);
                    c.setWindowTitle("Photo Watermark Settings");
                    photoWatermarkController = c;
                },
                c -> c.loadSettings(currentWatermarkSettings)
        );
        photoWatermarkStage = holder[0];
        if (nonNull(ctrl)) {
            photoWatermarkController = ctrl;
        }
    }
}
