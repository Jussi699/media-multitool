package media_multitool;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;

import javafx.scene.input.DragEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.converterVideo.ConverterVideoAudioFile;
import model.logger.ErrorLogger;
import model.select.SelectFile;
import model.utility.DragDropped;
import viewHelp.Alerts;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static viewHelp.Message.*;
import static model.utility.Parsers.*;
import static model.utility.Util.*;

public class ConverterAudioController {
    private String targetFormat;
    private int bitRate;
    private int channel;
    private int samplingRate;
    private File outputPath;
    private File file;
    private final PauseTransition hideSuccessMessageTimer =
            new PauseTransition(Duration.seconds(5));

    @FXML private StackPane dropZone;
    @FXML private ProgressBar progressBarConvert;
    @FXML private Button btnSelectAudioVideoFile;
    @FXML private Button btnChoiceDirForSaveMP3;
    @FXML private Button btnSubmitConvert;
    @FXML private Label textDragZone;
    @FXML private Label labelSuccessConvert;
    @FXML private Label labelSelectAudioFile;
    @FXML private ComboBox<String> comboBoxChoiceBitRate;
    @FXML private ComboBox<String> comboBoxChoiceChannels;
    @FXML private ComboBox<String> comboBoxChoiceSamplingRate;
    @FXML private ToggleButton btnToMP3;
    @FXML private ToggleButton btnToAAC;
    @FXML private ToggleButton btnToOggVorbis;
    @FXML private ToggleButton btnToOPUS;
    @FXML private ToggleButton btnToFLAC;
    @FXML private ToggleButton btnToALAC;
    @FXML private ToggleButton btnToWAV;
    @FXML private ToggleButton btnToAIFF;

    @FXML
    public void initialize() {
        btnChoiceDirForSaveMP3.setTooltip(new Tooltip("Default directory: Desktop"));
        btnSubmitConvert.setTooltip(new Tooltip("Converting a large file may take longer!"));

        outputPath = getSavedPath();
        setupClearMessageTimer(labelSuccessConvert, progressBarConvert, hideSuccessMessageTimer);

        labelSuccessConvert.setVisible(false);
        labelSuccessConvert.setManaged(true);
        labelSelectAudioFile.setVisible(true);
        labelSelectAudioFile.setText("Selected audio file: none");

        setupComboBox(comboBoxChoiceBitRate);
        setupComboBox(comboBoxChoiceChannels);
        setupComboBox(comboBoxChoiceSamplingRate);

        comboBoxChoiceBitRate.getStyleClass().add("comboBoxMP3");
        comboBoxChoiceChannels.getStyleClass().add("comboBoxMP3");
        comboBoxChoiceSamplingRate.getStyleClass().add("comboBoxMP3");

        comboBoxChoiceBitRate.getItems().addAll("128 kbps", "192 kbps", "256 kbps", "320 kbps");
        comboBoxChoiceChannels.getItems().addAll("1 Channels", "2 Channels");
        comboBoxChoiceSamplingRate.getItems().addAll("8000 Hz", "11025 Hz", "12000 Hz", "16000 Hz",
                                                        "22050 Hz", "24000 Hz", "32000 Hz",
                                                        "44100 Hz", "48000 Hz");

        resetToDefaults();
    }

    private void resetToDefaults() {
        comboBoxChoiceBitRate.setValue("320 kbps");
        comboBoxChoiceChannels.setValue("2 Channels");
        comboBoxChoiceSamplingRate.setValue("48000 Hz");
        bitRate = 320;
        channel = 2;
        samplingRate = 48000;
        progressBarConvert.setProgress(0);

        btnToAAC.setSelected(false);
        btnToAIFF.setSelected(false);
        btnToALAC.setSelected(false);
        btnToFLAC.setSelected(false);
        btnToMP3.setSelected(false);
        btnToOggVorbis.setSelected(false);
        btnToOPUS.setSelected(false);
        btnToWAV.setSelected(false);

        labelSelectAudioFile.setText("Selected audio file: none");

        file = null;
        if (dropZone != null) dropZone.getStyleClass().remove("drop-zone-filled");
        if (textDragZone != null) textDragZone.setText("Drag files here");
    }

    @FXML
    public void onSelectAudioVideoPressed() {
        SelectFile selectAudioVideoFile = new SelectFile();
        Stage stage = (Stage) btnSelectAudioVideoFile.getScene().getWindow();
        File selectedFile = selectAudioVideoFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("All Media Files",
                        "*.mp3", "*.wav", "*.ogg", "*.flac", "*.m4a", "*.aac", "*.wma",
                        "*.mp4", "*.avi", "*.mkv", "*.mov", "*.flv", "*.wmv"),
                    "Choice video/audio file"
                );

        if(selectedFile != null) {
            loadFile(selectedFile);
        }
    }

    private void loadFile(File selectedFile) {
        this.file = selectedFile;
        ErrorLogger.info("User select file (video/audio): " + file.getAbsolutePath());

        if (textDragZone != null) {
            textDragZone.setText("Selected: " + file.getName());
        }
        if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }
        labelSelectAudioFile.setText("Selected audio file: " + file.getName());
        hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
    }

    @FXML
    public void onSelectOutputDirectoryPressed() {
        Stage stage = getStage(btnChoiceDirForSaveMP3);
        File selectedPath = setPathForSave(stage, outputPath);
        if(selectedPath != null) {
            outputPath = selectedPath;
        }
    }

    @FXML
    public void onStartConversionPressed() {
        if(outputPath == null){
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Output path missing!", "Select output directory!");
            return;
        }

        if (file == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "File missing!", "Select audio or video file!");
            return;
        }

        if(targetFormat == null){
            Alerts.alertDialog(Alert.AlertType.WARNING, "WARN", "Format missing!", "Select audio format!");
            return;
        }

        btnSubmitConvert.setDisable(true);
        progressBarConvert.setProgress(0);

        CompletableFuture.supplyAsync(() -> getMetadata(file), IO_EXECUTOR)
            .thenCompose(sourceInfo -> {
                if (sourceInfo != null && sourceInfo.getAudio() == null) {
                    Platform.runLater(() -> {
                        Alerts.alertDialog(
                                Alert.AlertType.WARNING,
                                "No Audio Track Detected",
                                "The selected file does not have an audio track.",
                                "Audio conversion is not possible for this file. Please select a file with audio."
                        );
                        btnSubmitConvert.setDisable(false);
                    });
                    return CompletableFuture.completedFuture(null);
                }

                int originalChannels = parseChannels(sourceInfo);
                if (originalChannels == 1 && channel == 2) {
                    CompletableFuture<Boolean> proceedFuture = new CompletableFuture<>();
                    Platform.runLater(() -> {
                        boolean proceed = Alerts.confirmationDialog(
                                "Mono to Stereo Confirmation",
                                "The source file is mono (1 channel).",
                                "Do you want to convert it to stereo (2 channels) anyway?"
                        );
                        proceedFuture.complete(proceed);
                    });
                    return proceedFuture.thenCompose(proceed -> {
                        if (!proceed) return CompletableFuture.completedFuture(null);
                        return startAudioConversion();
                    });
                }
                return startAudioConversion();
            })
            .thenAccept(success -> {
                if (success == null) {
                     Platform.runLater(() -> btnSubmitConvert.setDisable(false));
                     return;
                }
                Platform.runLater(() -> {
                    btnSubmitConvert.setDisable(false);
                    if (success) {
                        showSuccessMessage(labelSuccessConvert, targetFormat, hideSuccessMessageTimer);
                        showProgressBar(progressBarConvert, hideSuccessMessageTimer);
                    } else {
                        if (progressBarConvert.getProgress() > 0 && progressBarConvert.getProgress() < 1.0) {
                            progressBarConvert.setProgress(0);
                        } else {
                            showErrorMessage(labelSuccessConvert, progressBarConvert, "So close, yet no success", hideSuccessMessageTimer);
                        }
                    }
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    btnSubmitConvert.setDisable(false);
                    progressBarConvert.setProgress(0);
                    ErrorLogger.error("Async audio conversion error: " + e.getMessage());
                });
                return null;
            });
    }

    private CompletableFuture<Boolean> startAudioConversion() {
        String audioCodec;
        String ffmpegFormat;

        switch (targetFormat.toLowerCase()) {
            case "aac" -> {audioCodec = "aac";ffmpegFormat = "adts";}
            case "ogg" -> {audioCodec = "libvorbis";ffmpegFormat = "ogg";}
            case "opus" -> {audioCodec = "libopus";ffmpegFormat = "opus";}
            case "flac" -> {audioCodec = "flac";ffmpegFormat = "flac";}
            case "alac", "m4a" -> {audioCodec = "alac";ffmpegFormat = "ipod";}
            case "wav" -> {audioCodec = "pcm_s16le";ffmpegFormat = "wav";}
            case "aiff" -> {audioCodec = "pcm_s16be";ffmpegFormat = "aiff";}
            default -> {audioCodec = "libmp3lame";ffmpegFormat = "mp3";}
        }

        return ConverterVideoAudioFile.convert(file, outputPath, bitRate, channel, samplingRate,
                audioCodec, ffmpegFormat, progress ->
                        Platform.runLater(() -> progressBarConvert.setProgress(progress)));
    }

    @FXML
    public void onResetPressed() {
        resetToDefaults();
        outputPath = getSavedPath();
        hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
    }

    public void onFormatMp3Pressed() {
        selectFormat("mp3", btnToMP3);
    }

    public void onFormatAACPressed() {
        selectFormat("aac", btnToAAC);
        System.out.println("Тут может быть два формата");
    }

    public void onFormatOggPressed() {
        selectFormat("ogg", btnToOggVorbis);
    }

    public void onFormatOpusPressed() {
        selectFormat("opus", btnToOPUS);
    }

    public void onFormatFlacPressed() {
        selectFormat("flac", btnToFLAC);
    }

    public void onFormatAlacPressed() {
        selectFormat("m4a", btnToALAC);
    }

    public void onFormatWAvPressed() {
        selectFormat("wav", btnToWAV);
    }

    public void onFormatAiffPressed() {
        selectFormat("aiff", btnToAIFF);
    }

    private void selectFormat(String format, ToggleButton selectedBtn) {
        targetFormat = format;
        btnToMP3.setSelected(selectedBtn == btnToMP3);
        btnToAAC.setSelected(selectedBtn == btnToAAC);
        btnToOggVorbis.setSelected(selectedBtn == btnToOggVorbis);
        btnToOPUS.setSelected(selectedBtn == btnToOPUS);
        btnToFLAC.setSelected(selectedBtn == btnToFLAC);
        btnToALAC.setSelected(selectedBtn == btnToALAC);
        btnToWAV.setSelected(selectedBtn == btnToWAV);
        btnToAIFF.setSelected(selectedBtn == btnToAIFF);
        hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
    }

    public void onChoiceBitRate() {
        bitRate = parseComboBoxStringToInt(comboBoxChoiceBitRate);
        ErrorLogger.info("User select bitRate: " + bitRate);

    }

    public void onChoiceChannels() {
        channel = parseComboBoxStringToInt(comboBoxChoiceChannels);
        ErrorLogger.info("User select channels: " + channel);

    }

    public void onChoiceSamplingRate() {
        samplingRate = parseComboBoxStringToInt(comboBoxChoiceSamplingRate);
        ErrorLogger.info("User select sampling rate: " + samplingRate);
    }

    private void setupComboBox(ComboBox<String> cb) {
        cb.getStyleClass().add("combo-box-mp3");

        cb.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                }
            }
        });

        cb.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(cb.getValue());
                } else {
                    setText(item);
                }
            }
        });
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
        DragDropped.handleDragOver(e, List.of(
                ".mp3", ".wav", ".ogg", ".flac", ".m4a", ".aac", ".wma",
                ".mp4", ".avi", ".mkv", ".mov", ".flv", ".wmv", ".webm", ".3gp"
        ), dropZone);
    }

    @FXML
    public void onCancelConversation() {
        ConverterVideoAudioFile.cancelConversion();
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
