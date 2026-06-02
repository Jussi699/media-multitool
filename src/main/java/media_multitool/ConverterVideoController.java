package media_multitool;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.converterVideo.ConverterVideoAudioFile;
import model.converterVideo.ConvertVideoAudioTask;
import model.properties.VideoAndAudioProperties;
import model.select.SelectFile;
import model.utility.DragDropped;
import model.utility.Global;
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

public class ConverterVideoController extends AbstractMediaController {
    private Item selectedItem;
    private final VideoAndAudioProperties videoProperties = new VideoAndAudioProperties();
    private ConvertVideoAudioTask currentTask;

    @FXML private Label labelSelectVideoName, textDragZone;
    @FXML private Button btnSubmitConvert, btnSelectVideoFile, btnChoiceDirForSaveVideo;
    @FXML private ToggleButton btnToMP4, btnToAVI, btnToMKV, btnToWEBM, btnToMOV, btnToFLV, btnToWMV, btnTo3GP;
    @FXML private ComboBox<Item> comboBoxChoiceBitRate, comboBoxChoiceChannels, comboBoxChoiceSamplingRate, comboBoxChoiceFPS;
    @FXML private ComboBox<String> comboBoxChoiceResolution;
    @FXML private CheckBox checkBoxGPU;
    @FXML private StackPane dropZone;

    private List<ToggleButton> listBtn;

    @FXML
    public void initialize() {
        listBtn = List.of(
                btnToMP4, btnToAVI, btnToMKV, btnToWEBM, btnToMOV, btnToFLV, btnToWMV, btnTo3GP
        );

        videoProperties.setOutput(getSavedPath());
        setupClearMessageTimer(labelSuccess, progressBar, videoProperties.getHideSuccessMessageTimer(), true);

        ComboBoxes.setupComboBox(comboBoxChoiceBitRate, Item::title);
        ComboBoxes.setupComboBox(comboBoxChoiceChannels, Item::title);
        ComboBoxes.setupComboBox(comboBoxChoiceSamplingRate, Item::title);
        ComboBoxes.setupComboBox(comboBoxChoiceFPS, Item::title);

        resetToDefaults();

        comboBoxChoiceBitRate.getItems().addAll(
                new Item(-1, "Match source"),
                new Item(1000, "1000 kbps (SD)"), new Item(2500, "2500 kbps (720p)"),
                new Item(5000, "5000 kbps (1080p)"), new Item(8000, "8000 kbps (High)")
        );

        comboBoxChoiceChannels.getItems().addAll(
                new Item(-1, "Match source"),
                new Item(1, "1 Channels"), new Item(2, "2 Channels")
        );

        comboBoxChoiceSamplingRate.getItems().addAll(
                new Item(-1, "Match source"),
                new Item(8000, "8000 Hz"),
                new Item(11025, "11025 Hz"), new Item(12000, "12000 Hz"),
                new Item(16000, "16000 Hz"), new Item(22050, "22050 Hz"),
                new Item(24000, "24000 Hz"), new Item(32000, "32000 Hz"),
                new Item(44100, "44100 Hz"), new Item(48000, "48000 Hz")
        );

        comboBoxChoiceFPS.getItems().addAll(
                new Item(-1, "Match source"),
                new Item(24, "24 fps"), new Item(30, "30 fps"), new Item(60, "60 fps")
        );

        comboBoxChoiceResolution.getItems().addAll(
                "Match source",
                "1280x720", "1920x1080", "3840x2160"
        );
    }

    private void resetToDefaults() {
        videoProperties.setBitRate(5000);
        videoProperties.setChannel(2);
        videoProperties.setSamplingRate(48000);
        videoProperties.setFps(30);
        videoProperties.setResolution("1920x1080");

        comboBoxChoiceBitRate.setValue(new Item(5000, "5000 kbps (1080p)"));
        comboBoxChoiceChannels.setValue(new Item(2, "2 Channels"));
        comboBoxChoiceSamplingRate.setValue(new Item(48000, "48000 Hz"));
        comboBoxChoiceFPS.setValue(new Item(30, "30 fps"));
        comboBoxChoiceResolution.setValue("1920x1080");

        if (dropZone != null) dropZone.getStyleClass().remove("drop-zone-filled");
        if (textDragZone != null) textDragZone.setText("Drag files here");
    }

    @Override
    protected void lockUI() {
        btnSubmitConvert.setDisable(true);
        if (btnReset != null) btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSubmitConvert.setDisable(false);
        if (btnReset != null) btnReset.setDisable(false);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        if (Boolean.FALSE.equals(result)) {
            if (currentTask != null && currentTask.isCancelled()) {
                handleTaskCancelled();
                return;
            }
        }

        super.handleTaskSuccess(result);

        if (Boolean.TRUE.equals(result)) {
            showSuccessMessage(labelSuccess, videoProperties.getTargetFormat(), videoProperties.getHideSuccessMessageTimer());
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
        String msg = exception.getMessage();
        Throwable cause = exception.getCause();
        String causeMsg = (cause != null) ? cause.getMessage() : "";

        boolean isCancelled = (msg != null && (msg.contains("Encoding interrupted") || msg.contains("Stream Closed")))
                || (causeMsg != null && causeMsg.contains("Stream Closed"))
                || (currentTask != null && currentTask.isCancelled());

        if (isCancelled) {
            handleTaskCancelled();
            return;
        }
        super.handleTaskFailure(exception);
        videoProperties.getHideSuccessMessageTimer().playFromStart();
    }

    @FXML
    public void onSelectVideoPressed() {
        SelectFile selectFile = new SelectFile();
        Stage stage = (Stage) btnSelectVideoFile.getScene().getWindow();
        selectFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Video", Global.getAllSupportedVideoFormatsForFileChooser()), "Select video")
                .ifPresent(this::loadFile);
    }

    private void loadFile(File selectedFile) {
        videoProperties.setSrcFile(selectedFile);
        labelSelectVideoName.setText("Selected file: " + videoProperties.getSrcFile().getName() + " (Loading info...)");
        
        CompletableFuture.supplyAsync(() -> getMetadata(videoProperties.getSrcFile()))
            .thenAccept(infoOpt -> Platform.runLater(() -> updateLabelFromMetadata(infoOpt.orElse(null))));
        
        hideSuccessMessage(labelSuccess, videoProperties.getHideSuccessMessageTimer(), true);
        
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
    public void onSelectOutputDirectoryPressed() {
        Stage stage = (Stage) btnChoiceDirForSaveVideo.getScene().getWindow();
        directoryChooser(stage, videoProperties.getOutput(), "Select directory for save video")
                .ifPresent(selectedPath -> {
                    videoProperties.setOutput(selectedPath);
                    hideSuccessMessage(labelSuccess, videoProperties.getHideSuccessMessageTimer(), true);
                });
    }

    @FXML
    public void onChoiceBitRate() {
        selectedItem = comboBoxChoiceBitRate.getValue();
        videoProperties.setBitRate((selectedItem != null) ? (int) selectedItem.id() : -1);
    }

    @FXML
    public void onChoiceChannels() {
        selectedItem = comboBoxChoiceChannels.getValue();
        videoProperties.setChannel((selectedItem != null) ? (int) selectedItem.id() : -1);
    }

    @FXML
    public void onChoiceSamplingRate() {
        selectedItem = comboBoxChoiceSamplingRate.getValue();
        videoProperties.setSamplingRate((selectedItem != null) ? (int) selectedItem.id() : -1);
    }

    @FXML
    public void onChoiceFPS() {
        selectedItem = comboBoxChoiceFPS.getValue();
        videoProperties.setFps((selectedItem != null) ? (int) selectedItem.id() : -1);
    }

    @FXML
    public void onChoiceResolution() {
        videoProperties.setResolution(comboBoxChoiceResolution.getValue());
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

    @FXML
    public void onFormatFlvPressed() {
        selectFormat("flv", btnToFLV);
    }

    @FXML
    public void onFormatWmvPressed() {
        selectFormat("wmv", btnToWMV);
    }

    @FXML
    public void onFormat3gpPressed() {
        selectFormat("3gp", btnTo3GP);
    }

    private void selectFormat(String format, ToggleButton selectedBtn) {
        videoProperties.setTargetFormat(format);
        btnToMP4.setSelected(selectedBtn == btnToMP4);
        btnToAVI.setSelected(selectedBtn == btnToAVI);
        btnToMKV.setSelected(selectedBtn == btnToMKV);
        btnToWEBM.setSelected(selectedBtn == btnToWEBM);
        btnToMOV.setSelected(selectedBtn == btnToMOV);
        btnToFLV.setSelected(selectedBtn == btnToFLV);
        btnToWMV.setSelected(selectedBtn == btnToWMV);
        btnTo3GP.setSelected(selectedBtn == btnTo3GP);
        hideSuccessMessage(labelSuccess, videoProperties.getHideSuccessMessageTimer(), true);
    }

    @FXML
    public void onStartConversionPressed() {
        if(videoProperties.getOutput() == null || videoProperties.getSrcFile() == null || videoProperties.getTargetFormat() == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Missing selection", "Select file, output directory and target format.");
            return;
        }

        if(videoProperties.getTargetFormat().equals("webm")) {
            if(videoProperties.getSamplingRate() == 11025 || videoProperties.getSamplingRate() == 22050 || videoProperties.getSamplingRate() == 32000 || videoProperties.getSamplingRate() == 44100){
                Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Sampling rate not support!",
                        "This sampling rate: (" + videoProperties.getSamplingRate() + ") not supported for WEBM video format!");
                return;
            }
        }

        CompletableFuture.supplyAsync(() -> getMetadata(videoProperties.getSrcFile()), IO_EXECUTOR)
            .thenAccept(sourceInfoOpt -> {
                MultimediaInfo sourceInfo = sourceInfoOpt.orElse(null);
                if (sourceInfo != null && sourceInfo.getAudio() == null) {
                    Platform.runLater(() -> {
                        boolean proceed = Alerts.confirmationDialog(
                                "No Audio Track Detected",
                                "The selected file does not appear to have an audio track.",
                                "Do you want to proceed anyway? (The output will be silent)"
                        );
                        if (proceed) continueWithConversion(sourceInfo);
                    });
                } else {
                    Platform.runLater(() -> continueWithConversion(sourceInfo));
                }
            });
    }

    private void continueWithConversion(MultimediaInfo sourceInfo) {
        int finalVideoBitrate = (videoProperties.getBitRate() == -1) ? parseVideoBitrate(sourceInfo) : videoProperties.getBitRate();
        if (finalVideoBitrate <= 0) finalVideoBitrate = parseBitrate(sourceInfo);
        if (finalVideoBitrate <= 0) finalVideoBitrate = 5000;

        int finalAudioBitrate = parseAudioBitrate(sourceInfo);
        if (finalAudioBitrate <= 0) finalAudioBitrate = 192;

        int finalChannels = (videoProperties.getChannel() == -1) ? parseChannels(sourceInfo) : videoProperties.getChannel();
        int finalSamplingRate = (videoProperties.getSamplingRate() == -1) ? parseSamplingRate(sourceInfo) : videoProperties.getSamplingRate();
        int finalFps = (videoProperties.getFps() == -1) ? parseFps(sourceInfo) : videoProperties.getFps();
        String finalResolution = ("Match source".equalsIgnoreCase(videoProperties.getResolution())) ? parseResolution(sourceInfo).orElse(null) : videoProperties.getResolution();

        if (finalChannels <= 0) finalChannels = 2;
        if (finalSamplingRate <= 0) finalSamplingRate = 48000;
        if (finalFps <= 0) finalFps = 30;

        String videoCodec;
        String audioCodec;
        String ffmpegFormat = videoProperties.getTargetFormat();

        boolean useGPU = checkBoxGPU != null && checkBoxGPU.isSelected();

        switch (videoProperties.getTargetFormat()) {
            case "mp4", "m4v" -> { videoCodec = useGPU ? "h264_nvenc" : "libx264"; audioCodec = "aac"; ffmpegFormat = "mp4"; }
            case "mkv", "matroska" -> { videoCodec = useGPU ? "h264_nvenc" : "libx264"; audioCodec = "aac"; ffmpegFormat = "matroska"; }
            case "avi" -> { videoCodec = useGPU ? "h264_nvenc" : "mpeg4"; audioCodec = "libmp3lame"; ffmpegFormat = "avi"; }
            case "webm" -> { videoCodec = "libvpx"; audioCodec = "libvorbis"; ffmpegFormat = "webm"; }
            case "mov" -> { videoCodec = useGPU ? "h264_nvenc" : "libx264"; audioCodec = "aac"; ffmpegFormat = "mov"; }
            default -> { videoCodec = useGPU ? "h264_nvenc" : "libx264"; audioCodec = "aac"; }
        }

        ConverterVideoAudioFile converter = new ConverterVideoAudioFile();
        currentTask = new ConvertVideoAudioTask(converter, videoProperties.getSrcFile(), videoProperties.getOutput(), finalVideoBitrate, finalAudioBitrate, finalChannels, finalSamplingRate, finalFps,
                videoCodec, audioCodec, ffmpegFormat, finalResolution, "video");
        
        executeMediaTask(currentTask);
    }

    @FXML
    public void onResetPressed() {
        videoProperties.setSrcFile(null);
        videoProperties.setTargetFormat(null);
        labelSelectVideoName.setText("Selected file: none");

        for (ToggleButton tb : listBtn) {
            tb.setSelected(false);
        }

        if (checkBoxGPU != null) checkBoxGPU.setSelected(false);
        onCancelConversation();
        resetToDefaults();
        hideSuccessMessage(labelSuccess, videoProperties.getHideSuccessMessageTimer(), true);
    }

    @FXML
    public void onCancelConversation() {
        if (currentTask != null) currentTask.cancelConversion();
    }

    @FXML
    public void handleDragOver(DragEvent e) {
        DragDropped.handleDragOver(e, Global.getAllSupportedVideoFormats(), dropZone);
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
