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
import viewHelp.Alerts;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static model.utility.Parsers.*;
import static model.utility.PathWorker.getSavedPath;
import static model.utility.TemplateCheck.isValidFile;
import static model.utility.TemplateCheck.canRead;
import static java.util.Objects.isNull;
import static viewHelp.Message.*;
import static viewHelp.Utility.getMetadata;

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
        listControls = List.of(btnBasicCompress, btnStrongCompress, btnSuperCompress, chkUseGPU, chkCompressAudio, btnCompress, btnCancelConversion, btnReset);

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
        Stream.of(btnSelectFile, btnChoiceDirForSaveFile, btnReset, btnCompress, chkCompressAudio, chkUseGPU,
                        btnSuperCompress, btnBasicCompress, btnStrongCompress)
                .forEach(btn -> btn.setDisable(true));
    }

    @Override
    protected void unlockUI() {
        Stream.of(btnSelectFile, btnChoiceDirForSaveFile, btnReset, btnCompress, chkCompressAudio, chkUseGPU,
                        btnSuperCompress, btnBasicCompress, btnStrongCompress)
                .forEach(btn -> btn.setDisable(false));
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
                new FileChooser.ExtensionFilter("Video", Global.getSupportedVideoFormatsForFileChooser()))
                .ifPresent(this::loadFile);
    }

    @FXML
    public void onActionChoiceDirForSaveFile() {
        selectOutputDirectory(btnChoiceDirForSaveFile, videoProperties.getOutput(), videoProperties::setOutput, "Select directory for save video");
    }

    @FXML
    private void onGPUSelected() {
        videoProperties.setUseGPU(chkUseGPU.isSelected());

        // WebM (libvpx) does not support GPU acceleration — NVENC cannot encode VP8/VP9.
        // Notify the user so the checkbox is not misleading.
        if (chkUseGPU.isSelected() && nonNull(videoProperties.getSrcFile())) {
            String fmt = model.utility.DetermineType.determineFormat(videoProperties.getSrcFile()).orElse("");
            if ("webm".equals(fmt)) {
                Alerts.alertDialog(Alert.AlertType.INFORMATION,
                        "GPU not supported for WebM",
                        "GPU acceleration ignored",
                        "The WebM format uses libvpx which does not support NVENC GPU acceleration. " +
                        "Encoding will proceed using the CPU (libvpx).");
            }
        }
    }

    @FXML
    private void onAudioCompressionSelected() {
        // Recreate presets when audio compression setting changes
        if (nonNull(videoProperties.getSrcFile()) && nonNull(adaptivePresets)) {
            adaptivePresets = VideoPresets.createAdaptivePresets(videoProperties.getSrcFile(), chkCompressAudio.isSelected()).orElse(null);

            if (nonNull(adaptivePresets) && adaptivePresets.length >= 3 && nonNull(selectedPreset)) {
                ToggleButton selected = (ToggleButton) toggleGroup.getSelectedToggle();
                if      (selected == btnBasicCompress)   {selectedPreset = adaptivePresets[0];}
                else if (selected == btnStrongCompress)  {selectedPreset = adaptivePresets[1];}
                else if (selected == btnSuperCompress)   {selectedPreset = adaptivePresets[2];}

                updateEstimatedSize();
            } else if (isNull(adaptivePresets)) {
                selectedPreset = null;
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

        if (isNull(videoProperties.getSrcFile()) || isNull(adaptivePresets) || isNull(selectedPreset)) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Invalid selection!", "Invalid selection!",
                    "Please select a video file and a preset.");
            return false;
        }

        if (isNull(videoProperties.getOutput())) {
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
        videoProperties.setUseGPU(nonNull(chkUseGPU) && chkUseGPU.isSelected());
        compressor.setUseGPU(videoProperties.isUseGPU());
        compressor.setCompressAudio(nonNull(chkCompressAudio) && chkCompressAudio.isSelected());
        
        currentTask = new CompressVideoTask(compressor, videoProperties.getSrcFile(), videoProperties.getOutput(), selectedPreset);
        
        executeMediaTask(currentTask);
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, null,
                dropZone, null, progressBar, true, "video"
        );
        reset(videoProperties, ctx, "Select video file: none");

        if (nonNull(currentTask)) currentTask.cancelCompress();

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

        if (isNull(adaptivePresets) || adaptivePresets.length < 3) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "No presets available!", "No presets available!",
                    "Please load a video file first to create presets.");
            tb.setSelected(false);
            return;
        }

        String selectPreset = "Selected preset: ";

        if (!tb.isSelected()) {
            selectedPreset = null;
        } else if (source == btnBasicCompress) {
            selectedPreset = adaptivePresets[0];
            ErrorLogger.info(selectPreset + selectedPreset.name());
        } else if (source == btnStrongCompress) {
            selectedPreset = adaptivePresets[1];
            ErrorLogger.info(selectPreset + selectedPreset.name());
        } else if (source == btnSuperCompress) {
            selectedPreset = adaptivePresets[2];
            ErrorLogger.info(selectPreset + selectedPreset.name());
        }
        updateEstimatedSize();
    }

    private void updateEstimatedSize() {
        if (isNull(selectedPreset) || durationMillis <= 0 || isNull(videoProperties.getSrcFile())) {
            labelSuccess.setVisible(false);
            return;
        }

        try {
            if (nonNull(videoProperties.getHideSuccessMessageTimer())) {
                videoProperties.getHideSuccessMessageTimer().stop();
            }
        } catch (Exception _) {}

        double estimatedMB = calculateEstimatedSizeMB();
        if (estimatedMB <= 0) return;

        labelSuccess.setStyle("-fx-text-fill: #32CD32;");
        labelSuccess.setText(String.format("Estimated size: ~%.2f MB", estimatedMB));
        labelSuccess.setVisible(true);
    }

    private double calculateEstimatedSizeMB() {
        if (isNull(selectedPreset) || durationMillis <= 0) return 0;

        int vBitrate = selectedPreset.video().getBitRate().orElse(0);
        int aBitrate = (hasAudio && nonNull(selectedPreset.audio())) ? selectedPreset.audio().getBitRate().orElse(0) : 0;

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
                        1. Select a video file using "Select video" or drag and drop it into the dash-bordered zone;

                        2. (Optional) Select where you want to save the result by clicking on "Directory for save".
                            (Default directory: Desktop);

                        3. Select a compression preset:
                           - Basic: Balanced size and quality.
                           - Strong: Maximum compression, lower quality.
                           - Super: Optimized high quality with smaller size;

                        4. (Optional) Enable "Use GPU" if your hardware supports it;

                        5. (Optional) Toggle "Compress Audio" if needed;

                        6. Click "Compress and Download".

                        You can cancel the conversion at any time using the "Cancel Compress" button.

                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    private void loadFile(File selectedFile) {
        if (!validateSelectedFile(selectedFile)) {
            return;
        }

        enableControls();
        videoProperties.setSrcFile(selectedFile);
        selectedPreset = null;

        toggleGroup.selectToggle(null);

        durationMillis = 0;

        adaptivePresets = VideoPresets.createAdaptivePresets(videoProperties.getSrcFile(), chkCompressAudio.isSelected()).orElse(null);
        if (nonNull(adaptivePresets)) {
            ErrorLogger.info(getClass(), "Adaptive presets created successfully for: "
                    + videoProperties.getSrcFile().getName());
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
        if (isNull(info) || isNull(videoProperties.getSrcFile())) {
            durationMillis = 0;
            hasAudio = false;
            return;
        }

        durationMillis = info.getDuration();
        hasAudio = nonNull(info.getAudio());
        String res = parseResolution(info).orElse("N/A");
        int f = parseFps(info);
        int vbr = parseVideoBitrate(info);
        int abr = parseAudioBitrate(info);

        String infoText = String.format("Selected video file: %s [%s, %d fps, V:%d kbps, A:%d kbps]",
                videoProperties.getSrcFile().getName(),
                res,
                f, vbr, abr);

        // Warn the user if video bitrate could not be read from the file.
        // In this case presets fall back to a 5000 kbps baseline, which may produce
        // a larger output than the original for low-bitrate sources.
        if (vbr <= 0) {
            infoText += " [!] Video bitrate unknown — fallback 5000 kbps used for presets.";
            ErrorLogger.warn("Could not read video bitrate for: " + videoProperties.getSrcFile().getName()
                    + ". Presets will use 5000 kbps fallback — output may be larger than source.");
        }

        labelSelectFile.setText(infoText);
        updateEstimatedSize();
    }

    @FXML
    private void onActionCancelOperation() {
        if (nonNull(currentTask)) currentTask.cancelCompress();
    }
}
