package media_multitool.compressors;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import model.compressorVideo.Compressor;
import model.compressorVideo.VideoPresets;
import model.compressorVideo.CompressVideoTask;
import model.logger.ErrorLogger;
import model.properties.MediaProperties;
import model.properties.VideoAndAudioProperties;
import model.select.SelectFile;
import model.utility.Global;
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
    private static final ToggleGroup toggleGroup = new ToggleGroup();
    private CompressVideoTask currentTask;

    @Override
    protected MediaProperties getProperties() {
        return videoProperties;
    }

    @FXML private Label labelSelectFile,textDragZone;
    @FXML private Button btnChoiceDirForSaveFile, btnSelectFile, btnCancelConversion, btnCompress;
    @FXML private ToggleButton btnBasicCompress, btnStrongCompress, btnSuperCompress;
    @FXML private StackPane dropZone;
    @FXML private CheckBox chkUseGPU, chkCompressAudio;

    private List<Control> listControls;

    private long durationMillis = 0;
    private boolean hasAudio = false;

    @FXML
    public void initialize() {
        listControls = List.of(btnBasicCompress, btnStrongCompress, btnSuperCompress, chkUseGPU, chkCompressAudio, btnCompress, btnCancelConversion);

        videoProperties.setOutput(getSavedPath());

        btnBasicCompress.setToggleGroup(toggleGroup);
        btnStrongCompress.setToggleGroup(toggleGroup);
        btnSuperCompress.setToggleGroup(toggleGroup);

        setupClearMessageTimer(labelSuccess, progressBar, videoProperties.getHideSuccessMessageTimer(), true);

        setupDragAndDrop(dropZone, Global.getAllSupportedVideoFormats(), this::loadFile);
        
        chkCompressAudio.setSelected(true);
        
        isPressedReset();
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

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.TRUE.equals(result)) {
            showSuccessText(labelSuccess, "Compression successful!", videoProperties.getHideSuccessMessageTimer());
            showProgressBar(progressBar, videoProperties.getHideSuccessMessageTimer());
        }
    }

    @FXML
    public void onActionBtnSelectVideoFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Video", Global.getSupportedVideoFormatsForFileChooser()), "Select video")
                .ifPresent(this::loadFile);
    }

    @FXML
    public void onActionChoiceDirForSaveFile() {
        selectOutputDirectory(btnChoiceDirForSaveFile, videoProperties.getOutput(), videoProperties::setOutput, "Select directory for save video");
    }

    @FXML
    private void onGPUSelected() {
        videoProperties.setUseGPU(chkUseGPU.isSelected());
    }

    @FXML
    private void onAudioCompressionSelected() {
        // Recreate presets when audio compression setting changes
        if (videoProperties.getSrcFile() != null && adaptivePresets != null) {
            adaptivePresets = VideoPresets.createAdaptivePresets(videoProperties.getSrcFile(), chkCompressAudio.isSelected()).orElse(null);
            
            if (selectedPreset != null) {
                ToggleButton selected = (ToggleButton) toggleGroup.getSelectedToggle();
                if      (selected == btnBasicCompress)   {selectedPreset = adaptivePresets[0];}
                else if (selected == btnStrongCompress)  {selectedPreset = adaptivePresets[1];}
                else if (selected == btnSuperCompress)   {selectedPreset = adaptivePresets[2];}

                updateEstimatedSize();
            }
        }
    }

    private boolean checks() {
        if (!(btnBasicCompress.isSelected() || btnStrongCompress.isSelected() || btnSuperCompress.isSelected())) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Unselected preset option!", "Unselected preset option!",
                    "First, select a pre-configured compression option!");
            return false;
        }

        if (videoProperties.getSrcFile() == null || adaptivePresets == null || selectedPreset == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Invalid selection!", "Invalid selection!",
                    "Please select a video file and a preset.");
            return false;
        }

        if (videoProperties.getOutput() == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Output directory not selected!", "Output directory not selected!",
                    "Please select an output directory for the compressed video.");
            return false;
        }

        return true;
    }

    @FXML
    public void submitAndDownload() {
        if(!checks()) {
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

        Compressor compressor = new Compressor();
        videoProperties.setUseGPU(chkUseGPU != null && chkUseGPU.isSelected());
        compressor.setUseGPU(videoProperties.isUseGPU());
        compressor.setCompressAudio(chkCompressAudio != null && chkCompressAudio.isSelected());
        
        currentTask = new CompressVideoTask(compressor, videoProperties.getSrcFile(), videoProperties.getOutput(), selectedPreset);
        
        executeMediaTask(currentTask);
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, null,
                dropZone, null, progressBar, true
        );
        Util.reset(videoProperties, ctx, "Select video file: none");

        if (currentTask != null) currentTask.cancelCompress();

        adaptivePresets = null;
        selectedPreset = null;
        durationMillis = 0;

        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.setProgress(0);

        chkUseGPU.setSelected(false);
        chkCompressAudio.setSelected(true);
        
        disableControls();
    }

    @FXML
    public void onActionSelectPreset(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();
        ToggleButton tb = (ToggleButton) source;

        if (adaptivePresets == null || adaptivePresets.length < 3) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "No presets available!", "No presets available!",
                    "Please load a video file first to create presets.");
            tb.setSelected(false);
            return;
        }

        if (!tb.isSelected()) {
            selectedPreset = null;
        } else if (source == btnBasicCompress) {
            selectedPreset = adaptivePresets[0];
            ErrorLogger.info("Selected preset: " + selectedPreset.name());
        } else if (source == btnStrongCompress) {
            selectedPreset = adaptivePresets[1];
            ErrorLogger.info("Selected preset: " + selectedPreset.name());
        } else if (source == btnSuperCompress) {
            selectedPreset = adaptivePresets[2];
            ErrorLogger.info("Selected preset: " + selectedPreset.name());
        }
        updateEstimatedSize();
    }

    private void updateEstimatedSize() {
        if (selectedPreset == null || durationMillis <= 0 || videoProperties.getSrcFile() == null) {
            labelSuccess.setVisible(false);
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
                "Compressor Video",
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

    private void loadFile(File selectedFile) {
        enableControls();
        videoProperties.setSrcFile(selectedFile);
        selectedPreset = null;

        toggleGroup.selectToggle(null);

        durationMillis = 0;

        adaptivePresets = VideoPresets.createAdaptivePresets(videoProperties.getSrcFile(), chkCompressAudio.isSelected()).orElse(null);
        if (adaptivePresets != null) {
            ErrorLogger.info("Adaptive presets created successfully for: " + videoProperties.getSrcFile().getName());
        } else {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "Preset Creation Error",
                    "Could not create presets from video. Check log for details.");
        }
        labelSelectFile.setText("Selected file: " + videoProperties.getSrcFile().getName() + " (Loading info...)");

        CompletableFuture.supplyAsync(() -> getMetadata(videoProperties.getSrcFile()))
                .thenAccept(infoOpt -> Platform.runLater(() -> updateLabelFromMetadata(infoOpt.orElse(null))));

        hideSuccessMessage(labelSuccess, videoProperties.getHideSuccessMessageTimer(), true);

        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.setProgress(0);

        textDragZone.setText("Selected: " + videoProperties.getSrcFile().getName());

        if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
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

        String infoText = String.format("Selected video file: %s [%s, %d fps, V:%d kbps, A:%d kbps]",
                videoProperties.getSrcFile().getName(),
                res,
                f, vbr, abr);

        labelSelectFile.setText(infoText);
        updateEstimatedSize();
    }

    @FXML
    private void onActionCancelOperation() {
        if (currentTask != null) currentTask.cancelCompress();
    }
}
