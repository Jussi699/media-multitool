package media_multitool;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.converterVideo.ConverterVideoAudioFile;
import model.logger.ErrorLogger;
import model.select.SelectFile;
import model.utility.DragDropped;
import model.utility.Item;
import viewHelp.Alerts;
import viewHelp.ComboBoxes;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static viewHelp.Message.*;
import static model.utility.Parsers.*;
import static model.utility.Util.*;

public class ConverterVideoController {
    private Item selectedItem;
    private String resolution;
    private int fps;
    private int bitRate;
    private int channel;
    private int samplingRate;
    private File outputPath;
    private File file;
    private String targetFormat;
    private final PauseTransition hideSuccessMessageTimer =
            new PauseTransition(Duration.seconds(5));

    @FXML private Label labelSelectVideoName;
    @FXML private Label labelSuccessConvert;
    @FXML private Label textDragZone;
    @FXML private Button btnSubmitConvert;
    @FXML private Button btnSelectVideoFile;
    @FXML private Button btnChoiceDirForSaveVideo;
    @FXML private ToggleButton btnToMP4;
    @FXML private ToggleButton btnToAVI;
    @FXML private ToggleButton btnToMKV;
    @FXML private ToggleButton btnToWEBM;
    @FXML private ToggleButton btnToMOV;
    @FXML private ComboBox<Item> comboBoxChoiceBitRate;
    @FXML private ComboBox<Item> comboBoxChoiceChannels;
    @FXML private ComboBox<Item> comboBoxChoiceSamplingRate;
    @FXML private ComboBox<Item> comboBoxChoiceFPS;
    @FXML private ComboBox<String> comboBoxChoiceResolution;
    @FXML private CheckBox checkBoxGPU;
    @FXML private ProgressBar progressBarConvert;
    @FXML private StackPane dropZone;

    @FXML
    public void initialize() {
        outputPath = getSavedPath();
        setupClearMessageTimer(labelSuccessConvert, hideSuccessMessageTimer);
        labelSelectVideoName.setText("Selected video file: none");
        labelSuccessConvert.setVisible(false);

        ComboBoxes.setupComboBox(comboBoxChoiceBitRate, Item::title);
        ComboBoxes.setupComboBox(comboBoxChoiceChannels, Item::title);
        ComboBoxes.setupComboBox(comboBoxChoiceSamplingRate, Item::title);
        ComboBoxes.setupComboBox(comboBoxChoiceFPS, Item::title);

        resetToDefaults();

        comboBoxChoiceBitRate.getItems().addAll(
                new Item(-1, "Match source"),
                new Item(1000, "1000 kbps (SD)"),
                new Item(2500, "2500 kbps (720p)"),
                new Item(5000, "5000 kbps (1080p)"),
                new Item(8000, "8000 kbps (High)")
        );

        comboBoxChoiceChannels.getItems().addAll(
                new Item(-1, "Match source"),
                new Item(1, "1 Channels"),
                new Item(2, "2 Channels")
        );

        comboBoxChoiceSamplingRate.getItems().addAll(
                new Item(-1, "Match source"),
                new Item(8000, "8000 Hz"),
                new Item(11025, "11025 Hz"),
                new Item(12000, "12000 Hz"),
                new Item(16000, "16000 Hz"),
                new Item(22050, "22050 Hz"),
                new Item(24000, "24000 Hz"),
                new Item(32000, "32000 Hz"),
                new Item(44100, "44100 Hz"),
                new Item(48000, "48000 Hz")
        );

        comboBoxChoiceFPS.getItems().addAll(
                new Item(-1, "Match source"),
                new Item(24, "24 fps"),
                new Item(30, "30 fps"),
                new Item(60, "60 fps")
        );

        comboBoxChoiceResolution.getItems().addAll(
                "Match source",
                "1280x720",
                "1920x1080",
                "3840x2160"
        );
    }

    private void resetToDefaults() {
        bitRate = 5000;
        channel = 2;
        samplingRate = 48000;
        fps = 30;
        resolution = "1920x1080";

        comboBoxChoiceBitRate.setValue(new Item(5000, "5000 kbps (1080p)"));
        comboBoxChoiceChannels.setValue(new Item(2, "2 Channels"));
        comboBoxChoiceSamplingRate.setValue(new Item(48000, "48000 Hz"));
        comboBoxChoiceFPS.setValue(new Item(30, "30 fps"));
        comboBoxChoiceResolution.setValue("1920x1080");

        if (dropZone != null) dropZone.getStyleClass().remove("drop-zone-filled");
        if (textDragZone != null) textDragZone.setText("Drag files here");
    }

    @FXML
    public void onSelectVideoPressed() {
       SelectFile selectFile = new SelectFile();
       Stage stage = (Stage) btnSelectVideoFile.getScene().getWindow();
        File selectedFile = selectFile.choiceFile(stage,
               new FileChooser.ExtensionFilter("Video", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.webm") ,"Select video");

        if (selectedFile != null) {
            loadFile(selectedFile);
        }
    }

    private void loadFile(File selectedFile) {
        this.file = selectedFile;
        labelSelectVideoName.setText("Selected file: " + file.getName() + " (Loading info...)");
        
        CompletableFuture.supplyAsync(() -> getMetadata(file))
            .thenAccept(info -> Platform.runLater(() -> updateLabelFromMetadata(info)));
        
        hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
        
        if (textDragZone != null) {
            textDragZone.setText("Selected: " + file.getName());
        }
        if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }
    }

    private void updateLabelFromMetadata(ws.schild.jave.info.MultimediaInfo info) {
        if (info == null || file == null) return;

        String res = parseResolution(info);
        int f = parseFps(info);
        int vbr = parseVideoBitrate(info);
        int abr = parseAudioBitrate(info);

        String infoText = String.format("Selected file: %s [%s, %d fps, V:%d kbps, A:%d kbps]", 
                file.getName(), 
                (res != null ? res : "N/A"), 
                f, vbr, abr);
        
        labelSelectVideoName.setText(infoText);
    }

    @FXML
    public void onSelectOutputDirectoryPressed() {
        Stage stage = (Stage) btnChoiceDirForSaveVideo.getScene().getWindow();
        File selectedPath = setPathForSave(stage, outputPath);
        if (selectedPath != null) {
            outputPath = selectedPath;
            hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
        }
    }

    public void onChoiceBitRate() {
        selectedItem = comboBoxChoiceBitRate.getValue();
        bitRate = (selectedItem != null) ? (int) selectedItem.id() : -1;
    }

    public void onChoiceChannels() {
        selectedItem = comboBoxChoiceChannels.getValue();
        channel = (selectedItem != null) ? (int) selectedItem.id() : -1;
    }

    public void onChoiceSamplingRate() {
        selectedItem = comboBoxChoiceSamplingRate.getValue();
        samplingRate = (selectedItem != null) ? (int) selectedItem.id() : -1;
    }

    public void onChoiceFPS() {
        selectedItem = comboBoxChoiceFPS.getValue();
        fps = (selectedItem != null) ? (int) selectedItem.id() : -1;
    }

    public void onChoiceResolution() {
        resolution = comboBoxChoiceResolution.getValue();
    }

    @FXML
    public void onFormatWEBMPressed() {
        selectFormat("webm", btnToWEBM);
    }

    @FXML
    public void onFormatMp4Pressed() {
        selectFormat("mp4", btnToMP4);
    }

    @FXML
    public void onFormatAviPressed() {
        selectFormat("avi", btnToAVI);
    }

    @FXML
    public void onFormatMkvPressed() {
        selectFormat("mkv", btnToMKV);
    }

    @FXML
    public void onFormatMOVPressed() {
        selectFormat("mov", btnToMOV);
    }

    private void selectFormat(String format, ToggleButton selectedBtn) {
        targetFormat = format;
        btnToMP4.setSelected(selectedBtn == btnToMP4);
        btnToAVI.setSelected(selectedBtn == btnToAVI);
        btnToMKV.setSelected(selectedBtn == btnToMKV);
        btnToWEBM.setSelected(selectedBtn == btnToWEBM);
        btnToMOV.setSelected(selectedBtn == btnToMOV);
        hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
    }

    @FXML
    public void onStartConversionPressed() {
        if(outputPath == null || file == null || targetFormat == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Missing selection", "Select file, output directory and target format.");
            return;
        }

        if(targetFormat.equals("webm")) {
            if(samplingRate == 11025 || samplingRate == 22050 || samplingRate == 32000 || samplingRate == 44100){
                Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Sampling rate not support!",
                        "This sampling rate: (" + samplingRate + ") not supported for WEBM video format!");
                return;
            }
        }

        btnSubmitConvert.setDisable(true);
        progressBarConvert.setProgress(0);


        CompletableFuture.supplyAsync(() -> getMetadata(file), IO_EXECUTOR)
            .thenCompose(sourceInfo -> {
                if (sourceInfo != null && sourceInfo.getAudio() == null) {
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
                            return CompletableFuture.completedFuture(null);
                        }
                        return continueConversion(sourceInfo);
                    });
                }
                return continueConversion(sourceInfo);
            })
            .thenAccept(success -> Platform.runLater(() -> {
                btnSubmitConvert.setDisable(false);
                if (success == null) return;
                if (success) {
                    showSuccessMessage(labelSuccessConvert, targetFormat, hideSuccessMessageTimer);
                    showProgressBar(progressBarConvert, hideSuccessMessageTimer);
                } else {
                    progressBarConvert.setProgress(0);
                }
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    btnSubmitConvert.setDisable(false);
                    progressBarConvert.setProgress(0);
                    ErrorLogger.error("Async conversion error: " + e.getMessage());
                });
                return null;
            });
    }

    private CompletableFuture<Boolean> continueConversion(MultimediaInfo sourceInfo) {
        int finalVideoBitrate = (bitRate == -1) ? parseVideoBitrate(sourceInfo) : bitRate;
        if (finalVideoBitrate <= 0) finalVideoBitrate = parseBitrate(sourceInfo);
        if (finalVideoBitrate <= 0) finalVideoBitrate = 5000;

        int finalAudioBitrate = parseAudioBitrate(sourceInfo);
        if (finalAudioBitrate <= 0) finalAudioBitrate = 192;

        int finalChannels = (channel == -1) ? parseChannels(sourceInfo) : channel;
        int finalSamplingRate = (samplingRate == -1) ? parseSamplingRate(sourceInfo) : samplingRate;
        int finalFps = (fps == -1) ? parseFps(sourceInfo) : fps;
        String finalResolution = ("Match source".equalsIgnoreCase(resolution)) ? parseResolution(sourceInfo) : resolution;

        if (finalChannels <= 0) finalChannels = 2;
        if (finalSamplingRate <= 0) finalSamplingRate = 48000;
        if (finalFps <= 0) finalFps = 30;

        String videoCodec;
        String audioCodec;
        String ffmpegFormat = targetFormat;

        boolean useGPU = checkBoxGPU != null && checkBoxGPU.isSelected();

        switch (targetFormat) {
            case "mp4", "m4v" -> { videoCodec = useGPU ? "h264_nvenc" : "libx264"; audioCodec = "aac"; ffmpegFormat = "mp4"; }
            case "mkv", "matroska" -> { videoCodec = useGPU ? "h264_nvenc" : "libx264"; audioCodec = "aac"; ffmpegFormat = "mkv"; }
            case "avi" -> { videoCodec = useGPU ? "h264_nvenc" : "mpeg4"; audioCodec = "libmp3lame"; ffmpegFormat = "avi"; }
            case "webm" -> { videoCodec = "libvpx"; audioCodec = "libvorbis"; ffmpegFormat = "webm"; }
            case "mov" -> { videoCodec = useGPU ? "h264_nvenc" : "libx264"; audioCodec = "aac"; ffmpegFormat = "mov"; }
            default -> { videoCodec = useGPU ? "h264_nvenc" : "libx264"; audioCodec = "aac"; }
        }

        return ConverterVideoAudioFile.convert(file, outputPath, finalVideoBitrate, finalAudioBitrate, finalChannels, finalSamplingRate, finalFps,
                videoCodec, audioCodec, ffmpegFormat, finalResolution, "video", progress ->
                        Platform.runLater(() -> progressBarConvert.setProgress(progress)));
    }

    @FXML
    public void onResetPressed() {
        file = null;
        targetFormat = null;
        labelSelectVideoName.setText("Selected video file: none");
        btnToMP4.setSelected(false);
        btnToAVI.setSelected(false);
        btnToMKV.setSelected(false);
        btnToWEBM.setSelected(false);
        btnToMOV.setSelected(false);
        if (checkBoxGPU != null) checkBoxGPU.setSelected(false);
        resetToDefaults();
        hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
        progressBarConvert.setProgress(0);
    }

    @FXML
    public void onCancelConversation() {
        ConverterVideoAudioFile.cancelConversion();
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

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Video Converter",
                """
                        How to use:
                        1. Select a video or audio file using 'Select audio/video' or drag and drop it into the dash-bordered zone.
                        2. (Optional) Choose a directory for saving the output.
                        3. Select the target video format (MP4, AVI, MKV, etc.).
                        4. Configure video and audio settings (Bitrate, FPS, Resolution, etc.).
                        5. (Optional) Enable GPU Acceleration if your hardware supports it.
                        6. Click 'Convert and Download'.
                        
                        You can cancel the conversion at any time using the 'Cancel' button.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }
}

