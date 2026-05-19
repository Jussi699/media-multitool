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
import model.logger.ErrorLogger;
import model.properties.VideoAndAudioProperties;
import model.select.SelectFile;
import model.utility.DetermineType;
import model.utility.DragDropped;
import viewHelp.Alerts;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static model.utility.Parsers.*;
import static model.utility.Util.*;
import static viewHelp.Message.*;

public class CompressorVideoController {
    private VideoPresets.Preset[] adaptivePresets;
    private VideoPresets.Preset selectedPreset;
    private final VideoAndAudioProperties videoProperties = new VideoAndAudioProperties();
    private static final ToggleGroup group = new ToggleGroup();



    @FXML private Label labelSuccessCompress;
    @FXML private Label labelSelectVideoName;
    @FXML private Label textDragZone;
    @FXML private Button btnChoiceDirForSaveVideo;
    @FXML private Button btnSelectVideoFile;
    @FXML private ToggleButton btnBasicCompress;
    @FXML private ToggleButton btnStrongCompress;
    @FXML private ToggleButton btnSuperCompress;
    @FXML private StackPane dropZone;
    @FXML private ProgressBar progressBarCompress;
    @FXML private CheckBox chkUseGPU;

    @FXML
    public void initialize() {
        btnChoiceDirForSaveVideo.setTooltip(new Tooltip("Default directory: Desktop"));
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
        // ensure success label and progress bar are cleared after configured seconds
        setupClearMessageTimer(labelSuccessCompress, progressBarCompress, videoProperties.getHideSuccessMessageTimer());
    }

    @FXML
    public void onActionBtnSelectVideoFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectVideoFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Video", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.webm"), "Select video")
                .ifPresent(videoProperties::setSrcFile);

        if (videoProperties.getSrcFile() == null) return;

        ErrorLogger.info("User selected file (video): " + videoProperties.getSrcFile().getAbsolutePath());
        labelSelectVideoName.setText("Select video: " + videoProperties.getSrcFile().getName());
        
        adaptivePresets = VideoPresets.createAdaptivePresets(videoProperties.getSrcFile()).orElse(null);
        if (adaptivePresets != null) {
            ErrorLogger.info("Adaptive presets created successfully for: " + videoProperties.getSrcFile().getName());
        } else {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "Preset Creation Error",
                    "Could not create presets from video. Check log for details.");
        }
    }

    @FXML
    public void onActionSelectOutputDir() {
        Stage stage = getStage(btnChoiceDirForSaveVideo);
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

        Compressor.setUseGPU(chkUseGPU != null && chkUseGPU.isSelected());

        btnSelectVideoFile.setDisable(true);
        btnChoiceDirForSaveVideo.setDisable(true);
        if (chkUseGPU != null) chkUseGPU.setDisable(true);
        progressBarCompress.setProgress(0);

        CompletableFuture.supplyAsync(() -> {
            try {
                Compressor.getCodec(videoProperties.getSrcFile());
                return getMetadata(videoProperties.getSrcFile());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, IO_EXECUTOR)
                .thenCompose(infoOpt -> {
                    if (infoOpt.isPresent() && infoOpt.get().getAudio() == null) {
                        CompletableFuture<Boolean> proceedFuture = new CompletableFuture<>();
                        Platform.runLater(() -> {
                            boolean proceed = Alerts.confirmationDialog(
                                    "No Audio Track Detected",
                                    "The selected file does not appear to have an audio track.",
                                    "Do you want to proceed anyway? (The output will be silent)"
                            );
                            proceedFuture.complete(proceed);
                        });
                        return proceedFuture.thenCompose(proceed -> {
                            if (Boolean.FALSE.equals(proceed)) {
                                btnSelectVideoFile.setDisable(false);
                                btnChoiceDirForSaveVideo.setDisable(false);
                                if (chkUseGPU != null) chkUseGPU.setDisable(false);
                                return CompletableFuture.completedFuture(null);
                            }
                            return startCompression();
                        });
                    }
                    return startCompression();
                })
                .thenAccept(result -> Platform.runLater(() -> {
                    if (result == null) return;

                    btnSelectVideoFile.setDisable(false);
                    btnChoiceDirForSaveVideo.setDisable(false);
                    if (chkUseGPU != null) chkUseGPU.setDisable(false);

                    if (result.success) {
                        long original = result.originalSize;
                        long compressed = result.compressedSize;
                        double savedPercent = 0.0;
                        double savedMB = 0.0;
                        if (original > 0 && compressed >= 0) {
                            savedPercent = (original - compressed) * 100.0 / original;
                            savedMB = (original - compressed) / (1024.0 * 1024.0);
                        }

                        String message = String.format("Saved %.1f%% | %.2f MB", savedPercent, savedMB);
                        showSuccessText(labelSuccessCompress, message, videoProperties.getHideSuccessMessageTimer());
                        showProgressBar(progressBarCompress, videoProperties.getHideSuccessMessageTimer());
                    } else {
                        progressBarCompress.setProgress(0);
                        hideSuccessMessage(labelSuccessCompress, progressBarCompress, videoProperties.getHideSuccessMessageTimer());
                    }

                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        btnSelectVideoFile.setDisable(false);
                        btnChoiceDirForSaveVideo.setDisable(false);
                        if (chkUseGPU != null) chkUseGPU.setDisable(false);
                        // stop any running hide-success timer to avoid it later hiding the bar
                        try { if (videoProperties.getHideSuccessMessageTimer() != null) videoProperties.getHideSuccessMessageTimer().stop(); } catch (Exception ignored) {}
                        progressBarCompress.setProgress(0);
                        
                        String errorMsg = e.getMessage();
                        ErrorLogger.error("Compression error: " + errorMsg);
                    });
                    return null;
                });
    }

    private static class VideoCompressionResult {
        final boolean success;
        final long originalSize;
        final long compressedSize;

        VideoCompressionResult(boolean success, long originalSize, long compressedSize) {
            this.success = success;
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
        }
    }

    private CompletableFuture<VideoCompressionResult> startCompression() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Compressor.getCodec(videoProperties.getSrcFile());
                File finalFileOutput = new File(videoProperties.getOutput(), videoProperties.getSrcFile().getName() + UUID.randomUUID().toString().replace("-", "") +
                        "." + DetermineType.determineFormat(videoProperties.getSrcFile()).orElse("mp4"));

                long originalSize = videoProperties.getSrcFile().exists() ? videoProperties.getSrcFile().length() : 0L;

                Compressor.compress(videoProperties.getSrcFile(), finalFileOutput,
                        selectedPreset.video(), selectedPreset.audio(), progress -> Platform.runLater(() -> progressBarCompress.setProgress(progress)));

                long compressedSize = finalFileOutput.exists() ? finalFileOutput.length() : -1L;
                boolean success = compressedSize > 0;
                return new VideoCompressionResult(success, originalSize, compressedSize);
            } catch (Exception e) {
                ErrorLogger.error("Internal compression error: " + e.getMessage());
                return new VideoCompressionResult(false, videoProperties.getSrcFile() != null && videoProperties.getSrcFile().exists() ? videoProperties.getSrcFile().length() : 0L, -1L);
            }
        }, IO_EXECUTOR);
    }

    @FXML
    public void onActionPressedReset() {
        Compressor.cancelCompress();
        
        btnBasicCompress.setSelected(false);
        btnStrongCompress.setSelected(false);
        btnSuperCompress.setSelected(false);

        videoProperties.setSrcFile(null);
        adaptivePresets = null;
        selectedPreset = null;

        resetToDefault();

        labelSelectVideoName.setText("Select video: none");
        labelSuccessCompress.setVisible(false);

        if (progressBarCompress != null) {
            progressBarCompress.setVisible(true);
            progressBarCompress.setManaged(true);
            progressBarCompress.setProgress(0);
        }

        if (textDragZone != null) {
            textDragZone.setText("Drag files here");
        }
        if (dropZone != null && dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().remove("drop-zone-filled");
        }
        if (chkUseGPU != null) chkUseGPU.setSelected(false);
        
        btnSelectVideoFile.setDisable(false);
        btnChoiceDirForSaveVideo.setDisable(false);
        if (chkUseGPU != null) chkUseGPU.setDisable(false);
    }

    private void resetToDefault() {
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
            group.getSelectedToggle().setSelected(false);
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

        // hide only success label here (keep progress bar visible and reset to 0)
        hideSuccessMessage(labelSuccessCompress, videoProperties.getHideSuccessMessageTimer());
        if (progressBarCompress != null) {
            progressBarCompress.setVisible(true);
            progressBarCompress.setManaged(true);
            progressBarCompress.setProgress(0);
        }

        if (textDragZone != null) {
            textDragZone.setText("Selected: " + videoProperties.getSrcFile().getName());
        }
        if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }
    }

    private void updateLabelFromMetadata(ws.schild.jave.info.MultimediaInfo info) {
        if (info == null || videoProperties.getSrcFile() == null) return;

        String res = parseResolution(info).orElse("N/A");
        int f = parseFps(info);
        int vbr = parseVideoBitrate(info);
        int abr = parseAudioBitrate(info);

        String infoText = String.format("Selected file: %s [%s, %d fps, V:%d kbps, A:%d kbps]",
                videoProperties.getSrcFile().getName(),
                res,
                f, vbr, abr);

        labelSelectVideoName.setText(infoText);
    }

    @FXML
    private void onActionCancelCompress() {
        Compressor.cancelCompress();
        try {
            if (videoProperties.getHideSuccessMessageTimer() != null) {
                videoProperties.getHideSuccessMessageTimer().stop();
            }
        } catch (Exception ignored) {
        }

        if (progressBarCompress != null) {
            progressBarCompress.setVisible(true);
            progressBarCompress.setManaged(true);
            progressBarCompress.setProgress(0);
        }
        if (labelSuccessCompress != null) {
            labelSuccessCompress.setVisible(false);
            labelSuccessCompress.setText("");
        }
    }
}
