package media_multitool;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Alert;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.converterVideo.ConverterVideoAudioFile;
import model.converterVideo.ConvertVideoAudioTask;
import model.logger.ErrorLogger;
import model.properties.VideoAndAudioProperties;
import model.select.SelectFile;
import model.utility.DragDropped;
import model.utility.Global;
import model.utility.ResetContext;
import model.utility.Util;
import viewHelp.ComboBoxes;
import ws.schild.jave.info.MultimediaInfo;
import viewHelp.Alerts;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static viewHelp.Message.*;
import static model.utility.Parsers.*;
import static model.utility.Util.*;

public class ConverterAudioController extends AbstractMediaController {
    private final VideoAndAudioProperties audioProperties = new VideoAndAudioProperties();
    private ConvertVideoAudioTask currentTask;

    @FXML private StackPane dropZone;
    @FXML private Button btnSelectAudioVideoFile, btnChoiceDirForSave, btnSubmitAndDownload;
    @FXML private Label textDragZone, labelSelectAudioFile;
    @FXML private ComboBox<String> comboBoxChoiceBitRate, comboBoxChoiceChannels, comboBoxChoiceSamplingRate;
    @FXML private ToggleButton btnToMP3, btnToAAC, btnToOggVorbis, btnToOPUS, btnToFLAC, btnToALAC, btnToWAV, btnToAIFF;

    private List<ToggleButton> listBtn;

    @FXML
    public void initialize() {
        listBtn = List.of(btnToMP3, btnToAAC, btnToOggVorbis, btnToOPUS, btnToFLAC, btnToALAC, btnToWAV, btnToAIFF);

        audioProperties.setOutput(getSavedPath());
        setupClearMessageTimer(labelSuccess, progressBar, audioProperties.getHideSuccessMessageTimer(), true);

        ComboBoxes.setupStringComboBox(comboBoxChoiceBitRate);
        ComboBoxes.setupStringComboBox(comboBoxChoiceChannels);
        ComboBoxes.setupStringComboBox(comboBoxChoiceSamplingRate);

        comboBoxChoiceBitRate.getItems().addAll("128 kbps", "192 kbps", "256 kbps", "320 kbps");
        comboBoxChoiceChannels.getItems().addAll("1 Channels", "2 Channels");
        comboBoxChoiceSamplingRate.getItems().addAll("8000 Hz", "11025 Hz", "12000 Hz", "16000 Hz",
                                                        "22050 Hz", "24000 Hz", "32000 Hz",
                                                        "44100 Hz", "48000 Hz");

        resetToDefaults();
    }

    private void resetToDefaults() {
        ResetContext ctx = new ResetContext(
                labelSelectAudioFile, labelSuccess, textDragZone, null,
                dropZone, null, progressBar, true
        );
        Util.reset(audioProperties, ctx, "Selected file: none");

        comboBoxChoiceBitRate.setValue("320 kbps");
        comboBoxChoiceChannels.setValue("2 Channels");
        comboBoxChoiceSamplingRate.setValue("48000 Hz");
        audioProperties.setBitRate(320);
        audioProperties.setChannel(2);
        audioProperties.setSamplingRate(48000);
        if (progressBar != null) progressBar.setProgress(0);

        for (ToggleButton tb : listBtn) {
            tb.setSelected(false);
        }
    }

    @Override
    protected void lockUI() {
        btnSubmitAndDownload.setDisable(true);
        if (btnReset != null) btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSubmitAndDownload.setDisable(false);
        if (btnReset != null) btnReset.setDisable(false);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.TRUE.equals(result)) {
            showSuccessMessage(labelSuccess, audioProperties.getTargetFormat(), audioProperties.getHideSuccessMessageTimer());
            showProgressBar(progressBar, audioProperties.getHideSuccessMessageTimer());
        } else {
            audioProperties.getHideSuccessMessageTimer().playFromStart();
        }
    }

    @Override
    protected void handleTaskCancelled() {
        super.handleTaskCancelled();
        audioProperties.getHideSuccessMessageTimer().playFromStart();
    }

    @Override
    protected void handleTaskFailure(Throwable exception) {
        super.handleTaskFailure(exception);
        audioProperties.getHideSuccessMessageTimer().playFromStart();
    }

    @FXML
    public void onSelectAudioVideoPressed() {
        SelectFile selectAudioVideoFile = new SelectFile();
        Stage stage = (Stage) btnSelectAudioVideoFile.getScene().getWindow();

        List<String> allFilters = new ArrayList<>(Global.getSupportedAudioFormatsForFileChooser());
        allFilters.addAll(Global.getSupportedVideoFormatsForFileChooser());

        selectAudioVideoFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("All Media Files", allFilters),
                    "Choice video/audio file"
                ).ifPresent(this::loadFile);
    }

    private void loadFile(File selectedFile) {
        audioProperties.setSrcFile(selectedFile);
        ErrorLogger.info("User select file (video/audio): " + audioProperties.getSrcFile().getAbsolutePath());

        if (textDragZone != null) {
            textDragZone.setText("Selected: " + audioProperties.getSrcFile().getName());
        }
        if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }
        labelSelectAudioFile.setText("Selected audio file: " + audioProperties.getSrcFile().getName());
        hideSuccessMessage(labelSuccess, audioProperties.getHideSuccessMessageTimer(), true);
    }

    @FXML
    public void onSelectOutputDirectoryPressed() {
        Stage stage = (Stage) btnChoiceDirForSave.getScene().getWindow();
        directoryChooser(stage, audioProperties.getOutput(), "Select directory for save audio")
                .ifPresent(audioProperties::setOutput);
    }

    @FXML
    public void onStartConversionPressed() {
        if(audioProperties.getOutput() == null){
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Output path missing!", "Select output directory!");
            return;
        }

        if (audioProperties.getSrcFile() == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "File missing!", "Select audio or video file!");
            return;
        }

        if(audioProperties.getTargetFormat() == null){
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Format missing!", "Select audio format!");
            return;
        }

        CompletableFuture.supplyAsync(() -> getMetadata(audioProperties.getSrcFile()), IO_EXECUTOR)
            .thenAccept(sourceInfoOpt -> {
                MultimediaInfo sourceInfo = sourceInfoOpt.orElse(null);
                if (sourceInfo != null && sourceInfo.getAudio() == null) {
                    Platform.runLater(() -> Alerts.alertDialog(
                            Alert.AlertType.WARNING,
                            "No Audio Track Detected",
                            "The selected file does not have an audio track.",
                            "Audio conversion is not possible for this file. Please select a file with audio."
                    ));
                    return;
                }

                int originalChannels = parseChannels(sourceInfo);
                if (originalChannels == 1 && audioProperties.getChannel() == 2) {
                    Platform.runLater(() -> {
                        boolean proceed = Alerts.confirmationDialog(
                                "Mono to Stereo Confirmation",
                                "The source file is mono (1 channel).",
                                "Do you want to convert it to stereo (2 channels) anyway?"
                        );
                        if (proceed) startAudioConversion();
                    });
                } else {
                    Platform.runLater(this::startAudioConversion);
                }
            });
    }

    private void startAudioConversion() {
        String audioCodec;
        String ffmpegFormat;

        switch (audioProperties.getTargetFormat().toLowerCase()) {
            case "aac" -> {audioCodec = "aac";ffmpegFormat = "adts";}
            case "ogg" -> {audioCodec = "libvorbis";ffmpegFormat = "ogg";}
            case "opus" -> {audioCodec = "libopus";ffmpegFormat = "opus";}
            case "flac" -> {audioCodec = "flac";ffmpegFormat = "flac";}
            case "alac", "m4a", "m4b", "m4v", "mp4" -> {audioCodec = "alac";ffmpegFormat = "ipod";}
            case "wav" -> {audioCodec = "pcm_s16le";ffmpegFormat = "wav";}
            case "aiff" -> {audioCodec = "pcm_s16be";ffmpegFormat = "aiff";}
            default -> {audioCodec = "libmp3lame";ffmpegFormat = "mp3";}
        }

        ConverterVideoAudioFile converter = new ConverterVideoAudioFile();
        currentTask = new ConvertVideoAudioTask(converter, audioProperties.getSrcFile(), audioProperties.getOutput(), -1, audioProperties.getBitRate(),
                audioProperties.getChannel(), audioProperties.getSamplingRate(), -1,
                null, audioCodec, ffmpegFormat, null, "audio");
        
        executeMediaTask(currentTask);
    }

    @FXML
    public void onResetPressed() {
        resetToDefaults();
        audioProperties.setOutput(getSavedPath());
        hideSuccessMessage(labelSuccess, audioProperties.getHideSuccessMessageTimer(),true);
    }

    @FXML
    public void onFormatMp3Pressed() {
        selectFormat("mp3", btnToMP3);
    }

    @FXML
    public void onFormatAACPressed() {
        selectFormat("aac", btnToAAC);
    }

    @FXML
    public void onFormatOggPressed() {
        selectFormat("ogg", btnToOggVorbis);
    }

    @FXML
    public void onFormatOpusPressed() {
        selectFormat("opus", btnToOPUS);
    }

    @FXML
    public void onFormatFlacPressed() {
        selectFormat("flac", btnToFLAC);
    }

    @FXML
    public void onFormatAlacPressed() {
        selectFormat("m4a", btnToALAC);
    }

    @FXML
    public void onFormatWAvPressed() {
        selectFormat("wav", btnToWAV);
    }

    @FXML
    public void onFormatAiffPressed() {
        selectFormat("aiff", btnToAIFF);
    }

    private void selectFormat(String format, ToggleButton selectedBtn) {
        audioProperties.setTargetFormat(format);
        btnToMP3.setSelected(selectedBtn == btnToMP3);
        btnToAAC.setSelected(selectedBtn == btnToAAC);
        btnToOggVorbis.setSelected(selectedBtn == btnToOggVorbis);
        btnToOPUS.setSelected(selectedBtn == btnToOPUS);
        btnToFLAC.setSelected(selectedBtn == btnToFLAC);
        btnToALAC.setSelected(selectedBtn == btnToALAC);
        btnToWAV.setSelected(selectedBtn == btnToWAV);
        btnToAIFF.setSelected(selectedBtn == btnToAIFF);
        hideSuccessMessage(labelSuccess, audioProperties.getHideSuccessMessageTimer(), true);
    }

    @FXML
    public void onChoiceBitRate() {
        audioProperties.setBitRate(parseComboBoxStringToInt(comboBoxChoiceBitRate));
        ErrorLogger.info("User select bitRate: " + audioProperties.getBitRate());

    }

    @FXML
    public void onChoiceChannels() {
        audioProperties.setChannel(parseComboBoxStringToInt(comboBoxChoiceChannels));
        ErrorLogger.info("User select channels: " + audioProperties.getChannel());

    }

    @FXML
    public void onChoiceSamplingRate() {
        audioProperties.setSamplingRate(parseComboBoxStringToInt(comboBoxChoiceSamplingRate));
        ErrorLogger.info("User select sampling rate: " + audioProperties.getSamplingRate());
    }

    @FXML
    private void handleDragDropped(DragEvent e) {
        File droppedFile = DragDropped.handleDragDropped(e, dropZone, textDragZone);
        if (droppedFile != null) {
            loadFile(droppedFile);
        }
    }

    @FXML
    public void handleDragOver(DragEvent e) {
        List<String> allFormats = new ArrayList<>(Global.getAllSupportedAudioFormats());
        allFormats.addAll(Global.getAllSupportedVideoFormats());

        DragDropped.handleDragOver(e, allFormats, dropZone);
    }

    @FXML
    public void onCancelConversation() {
        if (currentTask != null) currentTask.cancelConversion();
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Audio Converter",
                """
                        How to use:
                        1. Select an audio or video file using 'Select audio/video' or drag and drop it into the dash-bordered zone.
                        2. (Optional) Choose a directory for saving the output.
                        3. Select the target audio format and configure quality settings (Bitrate, Channels, Sampling Rate).
                        4. Click 'Convert and Download'.
                        
                        You can cancel the conversion at any time using the 'Cancel' button.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }
}
