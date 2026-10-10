package media_multitool.compressors;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import media_multitool.AbstractMediaController;
import model.compressorVideo.Compressor;
import model.compressorVideo.VideoPresets;
import model.compressorVideo.CompressVideoTask;
import model.logger.ErrorLogger;
import model.properties.MediaProperties;
import model.properties.VideoAndAudioProperties;
import model.utility.Global;
import model.utility.ResetContext;
import viewHelp.Alerts;
import viewHelp.InfoAlert;
import viewHelp.Message;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static model.utility.Parsers.*;
import static model.utility.PathWorker.getSavedPath;
import static java.util.Objects.isNull;
import static viewHelp.Message.*;
import static viewHelp.Utility.getMetadata;

public class CompressorVideoController extends AbstractMediaController {
    private final VideoAndAudioProperties videoProperties = new VideoAndAudioProperties();
    private final ToggleGroup toggleGroup = new ToggleGroup();

    @FXML private Button btnChoiceDirForSaveFile, btnSelectFile;
    @FXML private Button btnCancelCompression, btnCompress;
    @FXML private ToggleButton btnBasicCompress, btnStrongCompress, btnSuperCompress;
    @FXML private StackPane dropZone;
    @FXML private Label labelSelectFile,textDragZone;
    @FXML private CheckBox chkUseGPU, chkCompressAudio;

    private VideoPresets.Preset[] adaptivePresets;
    private VideoPresets.Preset selectedPreset;
    private CompressVideoTask currentTask;
    private Control[] controlsUI;
    private Control[] controls;

    private long durationMillis = 0;
    private boolean hasAudio = false;

    @Override
    protected MediaProperties getProperties() {
        return videoProperties;
    }

    @FXML
    public void initialize() {
        controlsUI = new Control[] {
                btnSelectFile, btnChoiceDirForSaveFile, btnReset,
                btnBasicCompress, btnSuperCompress, btnStrongCompress,
                chkUseGPU, chkCompressAudio, btnCompress,
        };

        controls = new Control[] {
                btnBasicCompress, btnSuperCompress, btnStrongCompress,
                chkUseGPU, chkCompressAudio, btnCompress, btnReset
        };

        videoProperties.setOutput(getSavedPath());

        Stream.of(btnBasicCompress, btnStrongCompress, btnSuperCompress)
                        .forEach(btn -> btn.setToggleGroup(toggleGroup));

        setupClearMessageTimer(labelSuccess, progressBar, videoProperties.getHideSuccessMessageTimer(), true);
        setupDragAndDrop(dropZone, Global.getAllSupportedVideoFormats(), this::loadFile);

        isPressedReset();
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "Compressor Video",
                "Select a video file using \"Select video\" or drag and drop it into the dash-bordered zone;",
                """
                       3. Select a compression preset:
                           - Basic: Balanced size and quality.
                           - Strong: Maximum compression, lower quality.
                           - Super: Optimized high quality with smaller size;

                       4. (Optional) Enable "Use GPU" if your hardware supports it;

                       5. (Optional) Toggle "Compress Audio" if needed;

                       6. Click "Compress and Download".

                       You can cancel the conversion at any time using the "Cancel Conversion" button.
                       """
        );
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, null,
                dropZone, null, progressBar, true, "video"
        );
        reset(videoProperties, ctx, "Select video file: none");

        cancelCompress();

        adaptivePresets = null;
        selectedPreset = null;
        durationMillis = 0;
        toggleGroup.selectToggle(null);
        chkUseGPU.setSelected(false);
        chkCompressAudio.setSelected(true);

        setDefaultProgressBar();
        disableControls();
    }

    @FXML
    public void submitAndDownload() {
        if(!checks()) return;
        if(!validateCompressionSize()) return;

        executeCompression();
    }

    @FXML
    private void selectFile() {
        selectInputFile(btnSelectFile,
                new FileChooser.ExtensionFilter("Video", Global.getSupportedVideoFormatsForFileChooser()), this::loadFile);
    }

    @FXML
    public void onChoiceFolderForSaveFile() {
        selectOutputDirectory(btnChoiceDirForSaveFile, videoProperties.getOutput(), videoProperties::setOutput, "Select directory for save video");
    }

    @FXML
    private void onActionCancelOperation() {
        btnCancelCompression.setDisable(true);
        cancelCompress();
    }

    @FXML
    private void onAudioCompressionSelected() {
        if (nonNull(videoProperties.getSrcFile()) && nonNull(adaptivePresets)) {
            adaptivePresets = VideoPresets.createAdaptivePresets(videoProperties.getSrcFile(), chkCompressAudio.isSelected()).orElse(null);
            selectedPreset = getPresetByButton((ToggleButton) toggleGroup.getSelectedToggle());
            updateEstimatedSize();
        }
    }

    @FXML
    public void onActionSelectPreset(ActionEvent actionEvent) {
        ToggleButton tb = (ToggleButton) actionEvent.getSource();
        if (isNull(adaptivePresets) || adaptivePresets.length < 3) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "No presets available!",
                    "No presets available!",
                    "Please load a video file first to create presets.");

            tb.setSelected(false);
            return;
        }

        selectedPreset = tb.isSelected() ? getPresetByButton(tb) : null;
        if (nonNull(selectedPreset)) {
            ErrorLogger.info("Selected preset: " + selectedPreset.name());
        }
        updateEstimatedSize();
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

    @Override
    protected void lockUI() {
        setControlsDisabled(true, controlsUI);
        btnCancelCompression.setDisable(false);
    }

    @Override
    protected void unlockUI() {
        setControlsDisabled(false, controlsUI);
        btnCancelCompression.setDisable(true);
    }

    @Override
    protected void disableControls() {
        setControlsDisabled(true, controls);
        btnCancelCompression.setDisable(true);
    }

    @Override
    protected void enableControls() {
        setControlsDisabled(false, controls);
        btnCancelCompression.setDisable(true);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.TRUE.equals(result)) {
            showSuccessText(labelSuccess, "Compression successful!", videoProperties.getHideSuccessMessageTimer());
            showProgressBar(progressBar, videoProperties.getHideSuccessMessageTimer());
        }
    }

    private void processPresetResult(VideoPresets.Preset[] adaptivePresets, File file) {
        if (nonNull(adaptivePresets)) {
            ErrorLogger.info(getClass(), "Adaptive presets created successfully for: "
                    + file.getName());
        } else {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "Preset Creation Error",
                    "Could not create presets from video. Check log for details.");
        }
    }

    private VideoPresets.Preset getPresetByButton(ToggleButton button) {
        if (isNull(adaptivePresets) || adaptivePresets.length < 3 || isNull(button)) return null;
        if (button == btnBasicCompress) return adaptivePresets[0];
        if (button == btnStrongCompress) return adaptivePresets[1];
        if (button == btnSuperCompress) return adaptivePresets[2];
        return null;
    }

    private void executeCompression() {
        Compressor compressor = new Compressor();
        videoProperties.setUseGPU(nonNull(chkUseGPU) && chkUseGPU.isSelected());
        compressor.setUseGPU(videoProperties.isUseGPU());
        compressor.setCompressAudio(nonNull(chkCompressAudio) && chkCompressAudio.isSelected());

        currentTask = new CompressVideoTask(compressor, videoProperties.getSrcFile(), videoProperties.getOutput(), selectedPreset);

        executeMediaTask(currentTask);
    }

    private boolean validateCompressionSize() {
        double estimatedMB = calculateEstimatedSizeMB();
        long originalSizeBytes = videoProperties.getSrcFile().length();
        double originalMB = originalSizeBytes / (1024.0 * 1024.0);

        if (estimatedMB > originalMB && estimatedMB > 0) {
            return Alerts.confirmationDialog(
                    "Compression Warning",
                    String.format(Locale.US, "Estimated size (~%.2f MB) is larger than original (%.2f MB).", estimatedMB, originalMB),
                    "Do you want to proceed anyway?"
            );
        }
        return true;
    }

    private void updateEstimatedSize() {
        if (!canCalculateEstimatedSize()) {
            return;
        }

        try {
            if (nonNull(videoProperties.getHideSuccessMessageTimer())) {
                videoProperties.getHideSuccessMessageTimer().stop();
            }
        } catch (Exception _) {
            // Ignored
        }

        double estimatedMB = calculateEstimatedSizeMB();
        if (estimatedMB <= 0) return;

        Message.showEstimatedSize(labelSuccess, estimatedMB);
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

    private void loadFile(File selectedFile) {
        if (!validateSelectedFile(selectedFile)) {
            return;
        }

        enableControls();
        videoProperties.setSrcFile(selectedFile);
        selectedPreset = null;

        toggleGroup.selectToggle(null);
        durationMillis = 0;

        File srcFile = videoProperties.getSrcFile();
        adaptivePresets = VideoPresets.createAdaptivePresets(srcFile, chkCompressAudio.isSelected()).orElse(null);

        processPresetResult(adaptivePresets, srcFile);

        labelSelectFile.setText("Selected file: " + srcFile.getName() + " (Loading info...)");

        CompletableFuture.supplyAsync(() -> getMetadata(srcFile))
                .thenAccept(infoOpt -> Platform.runLater(() -> updateLabelFromMetadata(infoOpt.orElse(null))));

        hideSuccessMessage(labelSuccess, videoProperties.getHideSuccessMessageTimer(), true);
        setDefaultProgressBar();
        markDropZoneLoaded(dropZone, textDragZone, srcFile.getName());
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

    private double calculateEstimatedSizeMB() {
        return VideoPresets.calculateEstimatedSizeMB(selectedPreset, durationMillis, hasAudio);
    }

    private void setDefaultProgressBar() {
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.setProgress(0);
    }

    private boolean canCalculateEstimatedSize() {
        if (isNull(selectedPreset) || durationMillis <= 0 || isNull(videoProperties.getSrcFile())) {
            labelSuccess.setVisible(false);
            return false;
        }
        return true;
    }

    private void cancelCompress() {
        if (nonNull(currentTask)) {
            currentTask.cancelCompress();
        }
    }
}
