package media_multitool.converters;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import model.converterVideo.ConverterVideoAudioFile;
import model.converterVideo.ConvertVideoAudioTask;
import model.enums.TypeMedia;
import model.helper.pdfWorker.MediaHelper;
import model.logger.ErrorLogger;
import model.properties.MediaProperties;
import model.properties.VideoAndAudioProperties;
import model.select.SelectFile;
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
    private static final ToggleGroup toggleGroup = new ToggleGroup();
    private ConvertVideoAudioTask currentTask;

    @FXML private StackPane dropZone;
    @FXML private Button btnSelectAudioVideoFile, btnChoiceDirForSave, btnSubmitAndDownload, btnCancelConversion;
    @FXML private Label textDragZone, labelSelectFile;
    @FXML private ComboBox<String> comboBoxChoiceBitRate, comboBoxChoiceChannels, comboBoxChoiceSamplingRate;
    @FXML private ToggleButton btnToMP3, btnToAAC, btnToOggVorbis, btnToOPUS, btnToFLAC, btnToALAC, btnToWAV, btnToAIFF;

    private List<Control> listControls;

    @Override
    protected MediaProperties getProperties() {
        return audioProperties;
    }

    @FXML
    public void initialize() {
        List<ToggleButton> listToggleBtn = List.of(btnToMP3, btnToAAC, btnToOggVorbis, btnToOPUS, btnToFLAC, btnToALAC, btnToWAV, btnToAIFF);
        listToggleBtn.forEach(tb -> tb.setToggleGroup(toggleGroup));

        listControls = new ArrayList<>(listToggleBtn);
        listControls.addAll(List.of(comboBoxChoiceBitRate, comboBoxChoiceChannels, comboBoxChoiceSamplingRate, btnSubmitAndDownload, btnCancelConversion));

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

        isPressedReset();

        List<String> allFormats = new ArrayList<>(Global.getAllSupportedAudioFormats());
        allFormats.addAll(Global.getAllSupportedVideoFormats());
        setupDragAndDrop(dropZone, textDragZone, allFormats, this::loadFile);
    }

    private void resetToDefaults() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, null,
                dropZone, null, progressBar, true
        );
        Util.reset(audioProperties, ctx, "Selected media file: none");

        comboBoxChoiceBitRate.setValue("320 kbps");
        comboBoxChoiceChannels.setValue("2 Channels");
        comboBoxChoiceSamplingRate.setValue("48000 Hz");
        audioProperties.setAudioBitRate(320);
        audioProperties.setChannel(2);
        audioProperties.setSamplingRate(48000);
        progressBar.setProgress(0);

        toggleGroup.selectToggle(null);
    }

    @Override
    protected void lockUI() {
        btnSubmitAndDownload.setDisable(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        btnSubmitAndDownload.setDisable(false);
        btnReset.setDisable(false);

    }

    @Override
    protected void disableControls() {
        if (listControls != null) listControls.forEach(c -> c.setDisable(true));
    }

    @Override
    protected void enableControls() {
        if (listControls != null) listControls.forEach(c -> c.setDisable(false));
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.TRUE.equals(result)) {
            showSuccessMessage(labelSuccess, audioProperties.getTargetFormat(), audioProperties.getHideSuccessMessageTimer());
            showProgressBar(progressBar, audioProperties.getHideSuccessMessageTimer());
        }
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
        enableControls();
        audioProperties.setSrcFile(selectedFile);
        ErrorLogger.info("User select file (video/audio): " + audioProperties.getSrcFile().getAbsolutePath());

        textDragZone.setText("Selected: " + audioProperties.getSrcFile().getName());

        if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        labelSelectFile.setText("Selected audio file: " + audioProperties.getSrcFile().getName());
        hideSuccessMessage(labelSuccess, audioProperties.getHideSuccessMessageTimer(), true);
    }

    @FXML
    public void onSelectOutputDirectoryPressed() {
        selectOutputDirectory(btnChoiceDirForSave, audioProperties.getOutput(), audioProperties::setOutput, "Select directory for save audio");
    }

    private boolean checkAudioTrack(MultimediaInfo sourceInfo) {
        if(sourceInfo != null && sourceInfo.getAudio() == null) {
            Platform.runLater(() -> Alerts.alertDialog(
                    Alert.AlertType.WARNING,
                    "No Audio Track Detected",
                    "The selected file does not have an audio track.",
                    "Audio conversion is not possible for this file. Please select a file with audio."
            ));
            return false;
        }
        return true;
    }

    private boolean checkForNull(VideoAndAudioProperties audioProperties) {
        if(audioProperties.getOutput() == null){
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Output path missing!", "Select output directory!");
            return false;
        }

        if (audioProperties.getSrcFile() == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "File missing!", "Select audio or video file!");
            return false;
        }

        if(audioProperties.getTargetFormat() == null){
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Format missing!", "Select audio format!");
            return false;
        }

        return true;
    }

    @FXML
    public void onStartConversionPressed() {
        if(!checkForNull(audioProperties)) {
            return;
        }

        CompletableFuture.supplyAsync(() -> getMetadata(audioProperties.getSrcFile()), IO_EXECUTOR)
            .thenAccept(sourceInfoOpt -> {
                MultimediaInfo sourceInfo = sourceInfoOpt.orElse(null);
                if(!checkAudioTrack(sourceInfo)) {
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
                        if (proceed) continueWithConversion(sourceInfo);
                    });
                } else {
                    Platform.runLater(() -> continueWithConversion(sourceInfo));
                }
            });
    }

    private void continueWithConversion(MultimediaInfo sourceInfo) {
        String targetFormat = audioProperties.getTargetFormat().toLowerCase();

        int finalAudioBitrate = audioProperties.getAudioBitRate();
        int finalChannels = audioProperties.getChannel();
        int finalSamplingRate = audioProperties.getSamplingRate();

        ErrorLogger.info("Before conversion: selectedBitrate=" + finalAudioBitrate 
                + ", sourceBitrate=" + parseAudioBitrate(sourceInfo));

        if (finalAudioBitrate <= 0) {
            finalAudioBitrate = parseAudioBitrate(sourceInfo);
            ErrorLogger.info("Bitrate <= 0, using source bitrate: " + finalAudioBitrate);
        }
        if (finalAudioBitrate <= 0) {
            finalAudioBitrate = 320;
            ErrorLogger.info("Source bitrate invalid, using default: 320");
        }

        if (finalChannels <= 0) finalChannels = (sourceInfo != null) ? parseChannels(sourceInfo) : 2;
        if (finalChannels <= 0) finalChannels = 2;

        if (finalSamplingRate <= 0) finalSamplingRate = (sourceInfo != null) ? parseSamplingRate(sourceInfo) : 48000;
        if (finalSamplingRate <= 0) finalSamplingRate = 48000;

        audioProperties.setAudioBitRate(finalAudioBitrate);
        audioProperties.setChannel(finalChannels);
        audioProperties.setSamplingRate(finalSamplingRate);

        ErrorLogger.info("Final audio properties set: BR=" + finalAudioBitrate + ", CH=" + finalChannels + ", SR=" + finalSamplingRate);

        audioProperties.setAudioCodec(MediaHelper.getAudioCodec(targetFormat, false));
        audioProperties.setFfmpegFormat(MediaHelper.getFFmpegFormat(targetFormat));
        audioProperties.setTypeConvert(TypeMedia.AUDIO);

        ConverterVideoAudioFile converter = new ConverterVideoAudioFile();
        currentTask = new ConvertVideoAudioTask(converter, audioProperties, TypeMedia.AUDIO);
        
        executeMediaTask(currentTask);
    }

    @FXML
    public void isPressedReset() {
        onCancelConversion();
        resetToDefaults();
        audioProperties.setOutput(getSavedPath());
        hideSuccessMessage(labelSuccess, audioProperties.getHideSuccessMessageTimer(),true);
        disableControls();
    }

    @FXML
    private void onActionClickToggleBtnFormat(ActionEvent e) {
        viewHelp.Message.hideSuccessMessage(labelSuccess, getProperties().getHideSuccessMessageTimer(), true);

        ToggleButton tb = (ToggleButton) e.getSource();

        if (!tb.isSelected()) {
            audioProperties.setTargetFormat(null);
            return;
        }

        switch (tb.getId()) {
            case "btnToMP3" -> selectFormat("mp3", audioProperties::setTargetFormat);
            case "btnToAAC" -> selectFormat("aac", audioProperties::setTargetFormat);
            case "btnToOggVorbis" -> selectFormat("ogg", audioProperties::setTargetFormat);
            case "btnToOPUS" -> selectFormat("opus", audioProperties::setTargetFormat);
            case "btnToFLAC" -> selectFormat("flac", audioProperties::setTargetFormat);
            case "btnToALAC" -> selectFormat("m4a", audioProperties::setTargetFormat);
            case "btnToWAV" -> selectFormat("wav", audioProperties::setTargetFormat);
            case "btnToAIFF" -> selectFormat("aiff", audioProperties::setTargetFormat);
        }
    }

    @FXML
    public void onChoiceComboBox(ActionEvent event) {
        if (!(event.getSource() instanceof ComboBox<?> source)) {
            return;
        }

        switch (source.getId()) {
            case "comboBoxChoiceBitRate" -> audioProperties.setAudioBitRate(parseComboBoxStringToInt(comboBoxChoiceBitRate));
            case "comboBoxChoiceChannels" -> audioProperties.setChannel(parseComboBoxStringToInt(comboBoxChoiceChannels));
            case "comboBoxChoiceSamplingRate" -> audioProperties.setSamplingRate(parseComboBoxStringToInt(comboBoxChoiceSamplingRate));
        }
    }

    @FXML
    public void onCancelConversion() {
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
