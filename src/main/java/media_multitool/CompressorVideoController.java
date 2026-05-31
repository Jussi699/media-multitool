package media_multitool;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.compressorVideo.Compressor;
import model.compressorVideo.VideoPresets;
import model.compressorVideo.CompressVideoTask;
import model.logger.ErrorLogger;
import model.properties.VideoAndAudioProperties;
import model.select.SelectFile;
import model.utility.DragDropped;
import model.utility.ResetContext;
import model.utility.Util;
import viewHelp.Alerts;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static model.utility.Parsers.*;
import static model.utility.Util.*;
import static viewHelp.Message.*;

public class CompressorVideoController extends AbstractMediaController {
    private VideoPresets.Preset[] adaptivePresets;
    private VideoPresets.Preset selectedPreset;
    private final VideoAndAudioProperties videoProperties = new VideoAndAudioProperties();
    private static final ToggleGroup group = new ToggleGroup();
    private final Compressor compressor = new Compressor();
    private CompressVideoTask currentTask;

    @FXML private Label labelSelectVideoName;
    @FXML private Label textDragZone;
    @FXML private Button btnChoiceDirForSaveVideo;
    @FXML private Button btnSelectVideoFile;
    @FXML private ToggleButton btnBasicCompress;
    @FXML private ToggleButton btnStrongCompress;
    @FXML private ToggleButton btnSuperCompress;
    @FXML private StackPane dropZone;
    @FXML private CheckBox chkUseGPU;
    @FXML private Button btnCompress;
    @FXML private Button btnCancel;

    private long durationMillis = 0;
    private boolean hasAudio = false;

    @FXML
    public void initialize() {
        btnChoiceDirForSaveVideo.setTooltip(new Tooltip("Default directory: Desktop"));
        btnCompress.setTooltip(new Tooltip("Compression may take a long time, especially for large files."));

        Tooltip tooltipBasicCompress = new Tooltip("Medium size, high quality");
        Tooltip tooltipStrongCompress = new Tooltip("Smallest size, lowest quality");
        Tooltip tooltipSuperCompress = new Tooltip("Small size, high quality");

        btnBasicCompress.setTooltip(tooltipBasicCompress);
        btnStrongCompress.setTooltip(tooltipStrongCompress);
        btnSuperCompress.setTooltip(tooltipSuperCompress);

        videoProperties.setOutput(getSavedPath());

        btnBasicCompress.setToggleGroup(group);
        btnStrongCompress.setToggleGroup(group);
        btnSuperCompress.setToggleGroup(group);

        if (chkUseGPU != null) {
            chkUseGPU.setTooltip(new Tooltip("Use NVENC (NVIDIA GPU) if available (may significantly speed up encoding)."));
            chkUseGPU.setSelected(false);
        }
        setupClearMessageTimer(labelSuccess, progressBar, videoProperties.getHideSuccessMessageTimer(), true);
    }

    @Override
    protected void lockUI() {
        btnSelectVideoFile.setDisable(true);
        btnChoiceDirForSaveVideo.setDisable(true);
        btnCompress.setDisable(true);
        if (chkUseGPU != null) chkUseGPU.setDisable(true);
        if (btnReset != null) btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSelectVideoFile.setDisable(false);
        btnChoiceDirForSaveVideo.setDisable(false);
        btnCompress.setDisable(false);
        if (chkUseGPU != null) chkUseGPU.setDisable(false);
        if (btnReset != null) btnReset.setDisable(false);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.TRUE.equals(result)) {
            showSuccessText(labelSuccess, "Compression successful!", videoProperties.getHideSuccessMessageTimer());
            showProgressBar(progressBar, videoProperties.getHideSuccessMessageTimer());
        } else {
            videoProperties.getHideSuccessMessageTimer().playFromStart();
        }
    }

    @Override
    protected void handleTaskCancelled() {
        super.handleTaskCancelled();
        videoProperties.getHideSuccessMessageTimer().playFromStart();
    }

    @Override
    protected void handleTaskFailure(Throwable exception) {
        super.handleTaskFailure(exception);
        videoProperties.getHideSuccessMessageTimer().playFromStart();
    }

    @FXML
    public void onActionBtnSelectVideoFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectVideoFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Video", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.webm"), "Select video")
                .ifPresent(this::loadFile);
    }

    @FXML
    public void onActionSelectOutputDir() {
        Stage stage = (Stage) btnChoiceDirForSaveVideo.getScene().getWindow();
        directoryChooser(stage, videoProperties.getOutput(), "Select directory for save image")
                .ifPresent(videoProperties::setOutput);
    }

    @FXML
    public void onActionCompressAndDownload() {
        if (!checkChoicePreset()) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Unselected preset option!", "Unselected preset option!",
                    "First, select a pre-configured compression option!");
            return;
        }
        
        if (videoProperties.getSrcFile() == null || adaptivePresets == null || selectedPreset == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Invalid selection!", "Invalid selection!",
                    "Please select a video file and a preset.");
            return;
        }
        
        if (videoProperties.getOutput() == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Output directory not selected!", "Output directory not selected!",
                    "Please select an output directory for the compressed video.");
            return;
        }

        double estimatedMB = calculateEstimatedSizeMB();
        long originalSizeBytes = videoProperties.getSrcFile().length();
        double originalMB = originalSizeBytes / (1024.0 * 1024.0);

        if (estimatedMB > originalMB && estimatedMB > 0) {
            boolean proceed = Alerts.confirmationDialog(
                    "Compression Warning",
                    "Estimated size (~" + String.format("%.2f", estimatedMB) + " MB) is larger than original (" + String.format("%.2f", originalMB) + " MB).",
                    "Do you want to proceed anyway?"
            );
            if (!proceed) return;
        }

        compressor.setUseGPU(chkUseGPU != null && chkUseGPU.isSelected());
        
        currentTask = new CompressVideoTask(compressor, videoProperties.getSrcFile(), videoProperties.getOutput(), selectedPreset);
        
        executeMediaTask(currentTask);
    }

    @FXML
    public void onActionPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectVideoName, labelSuccess, textDragZone, null,
                dropZone, null, progressBar, true
        );
        Util.reset(videoProperties, ctx, "Select video: none");

        if (currentTask != null) currentTask.cancelCompress();
        
        btnBasicCompress.setSelected(false);
        btnStrongCompress.setSelected(false);
        btnSuperCompress.setSelected(false);

        adaptivePresets = null;
        selectedPreset = null;
        durationMillis = 0;

        if (progressBar != null) {
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.setProgress(0);
        }

        if (chkUseGPU != null) chkUseGPU.setSelected(false);
        
        btnSelectVideoFile.setDisable(false);
        btnChoiceDirForSaveVideo.setDisable(false);
        if (chkUseGPU != null) chkUseGPU.setDisable(false);
    }

    private boolean checkChoicePreset() {
        return btnBasicCompress.isSelected() || btnStrongCompress.isSelected() || btnSuperCompress.isSelected();
    }

    @FXML
    public void onActionSelectPreset(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();

        if (adaptivePresets == null || adaptivePresets.length < 3) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "No presets available!", "No presets available!",
                    "Please load a video file first to create presets.");
            if (group.getSelectedToggle() != null) group.getSelectedToggle().setSelected(false);
            return;
        }

        if (source == btnBasicCompress && btnBasicCompress.isSelected()) {
            selectedPreset = adaptivePresets[0];
            ErrorLogger.info("Selected preset: " + selectedPreset.name());
        } else if (source == btnStrongCompress && btnStrongCompress.isSelected()) {
            selectedPreset = adaptivePresets[1];
            ErrorLogger.info("Selected preset: " + selectedPreset.name());
        } else if (source == btnSuperCompress && btnSuperCompress.isSelected()) {
            selectedPreset = adaptivePresets[2];
            ErrorLogger.info("Selected preset: " + selectedPreset.name());
        }
        updateEstimatedSize();
    }

    private void updateEstimatedSize() {
        if (selectedPreset == null || durationMillis <= 0 || videoProperties.getSrcFile() == null) {
            return;
        }

        try {
            if (videoProperties.getHideSuccessMessageTimer() != null) {
                videoProperties.getHideSuccessMessageTimer().stop();
            }
        } catch (Exception ignored) {}

        double estimatedMB = calculateEstimatedSizeMB();
        if (estimatedMB <= 0) return;

        labelSuccess.setStyle("-fx-text-fill: #32CD32;");
        labelSuccess.setText(String.format("Estimated size: ~%.2f MB", estimatedMB));
        labelSuccess.setVisible(true);
    }

    private double calculateEstimatedSizeMB() {
        if (selectedPreset == null || durationMillis <= 0) return 0;

        int vBitrate = selectedPreset.video().getBitRate().orElse(0);
        int aBitrate = (hasAudio && selectedPreset.audio() != null) ? selectedPreset.audio().getBitRate().orElse(0) : 0;

        double totalBitrateBps = vBitrate + aBitrate;
        double durationSeconds = durationMillis / 1000.0;

        double sizeBytes = (totalBitrateBps * durationSeconds) / 8.0;
        return sizeBytes / (1024.0 * 1024.0);
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Video Compressor",
                """
                        How to use:
                        1. Select a video file using 'Select video'.
                        2. (Optional) Choose a directory for saving the output.
                        3. Select a compression preset:
                           - Basic: Balanced size and quality.
                           - Strong: Maximum compression, lower quality.
                           - Super: Optimized high quality with smaller size.
                        4. Click 'Convert and Download'.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    @FXML
    public void handleDragOver(DragEvent e) {
        DragDropped.handleDragOver(e, List.of(
                ".mp4", ".avi", ".mkv", ".mov", ".webm", ".flv", ".wmv", ".3gp"), dropZone);
    }

    @FXML
    public void handleDragDropped(DragEvent e) {
        File droppedFile = DragDropped.handleDragDropped(e, dropZone, textDragZone);
        if (droppedFile != null) {
            loadFile(droppedFile);
        }
    }

    private void loadFile(File selectedFile) {
        videoProperties.setSrcFile(selectedFile);
        selectedPreset = null;
        if (group.getSelectedToggle() != null) {
            group.getSelectedToggle().setSelected(false);
        }
        durationMillis = 0;

        adaptivePresets = VideoPresets.createAdaptivePresets(videoProperties.getSrcFile()).orElse(null);
        if (adaptivePresets != null) {
            ErrorLogger.info("Adaptive presets created successfully for: " + videoProperties.getSrcFile().getName());
        } else {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "Preset Creation Error",
                    "Could not create presets from video. Check log for details.");
        }
        labelSelectVideoName.setText("Selected file: " + videoProperties.getSrcFile().getName() + " (Loading info...)");

        CompletableFuture.supplyAsync(() -> getMetadata(videoProperties.getSrcFile()))
                .thenAccept(infoOpt -> Platform.runLater(() -> updateLabelFromMetadata(infoOpt.orElse(null))));

        hideSuccessMessage(labelSuccess, videoProperties.getHideSuccessMessageTimer(), true);
        if (progressBar != null) {
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.setProgress(0);
        }

        if (textDragZone != null) {
            textDragZone.setText("Selected: " + videoProperties.getSrcFile().getName());
        }
        if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }
    }

    private void updateLabelFromMetadata(ws.schild.jave.info.MultimediaInfo info) {
        if (info == null || videoProperties.getSrcFile() == null) {
            durationMillis = 0;
            hasAudio = false;
            return;
        }

        durationMillis = info.getDuration();
        hasAudio = info.getAudio() != null;
        String res = parseResolution(info).orElse("N/A");
        int f = parseFps(info);
        int vbr = parseVideoBitrate(info);
        int abr = parseAudioBitrate(info);

        String infoText = String.format("Selected file: %s [%s, %d fps, V:%d kbps, A:%d kbps]",
                videoProperties.getSrcFile().getName(),
                res,
                f, vbr, abr);

        labelSelectVideoName.setText(infoText);
        updateEstimatedSize();
    }

    @FXML
    private void onActionCancelCompress() {
        if (currentTask != null) currentTask.cancelCompress();
    }
}
