package converter;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.converterVideo.ConverterVideoAudioFile;
import model.logger.ErrorLogger;
import model.select.SelectFile;

import java.io.File;
import java.nio.file.Paths;
import static model.utility.Util.*;

public class ConverterMP3ViewController {
    private static final int SUCCESS_MESSAGE_DURATION_SECONDS = 5;

    private final File DEFAULT_PATH = Paths.get(System.getProperty("user.home"), "Desktop").toFile();
    private int bitRate;
    private int channel;
    private int samplingRate;
    private File outputPath;
    private File file;
    private final PauseTransition hideSuccessMessageTimer =
            new PauseTransition(Duration.seconds(SUCCESS_MESSAGE_DURATION_SECONDS));

    @FXML private Label textDragZone;
    @FXML private StackPane dropZone;
    @FXML private ProgressBar progressBarConvert;
    @FXML private Button btnSelectAudioVideoFile;
    @FXML private Button btnChoiceDirForSaveMP3;
    @FXML private Label labelSelectAudioName;
    @FXML private Button btnSubmitConvert;
    @FXML private Label labelSuccessConvert;
    @FXML private ComboBox<String> comboBoxChoiceBitRate;
    @FXML private ComboBox<String> comboBoxChoiceChannels;
    @FXML private ComboBox<String> comboBoxChoiceSamplingRate;

    @FXML
    public void initialize() {
        btnChoiceDirForSaveMP3.setTooltip(new Tooltip("Default path \"Desktop\""));
        btnSubmitConvert.setTooltip(new Tooltip("Converting a large file may take longer!"));

        outputPath = DEFAULT_PATH;
        setupClearMessageTimer(labelSuccessConvert, progressBarConvert, hideSuccessMessageTimer);
        labelSelectAudioName.setText("Selected file: none");

        labelSuccessConvert.setVisible(false);
        labelSuccessConvert.setManaged(true);

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

        comboBoxChoiceBitRate.setValue("320 kbps");
        comboBoxChoiceChannels.setValue("2 Channels");
        comboBoxChoiceSamplingRate.setValue("48000 Hz");

        bitRate = 320;
        channel = 2;
        samplingRate = 48000;



    }

    @FXML
    public void onSelectAudioVideoPressed() {
        SelectFile selectAudioVideoFile = new SelectFile();
        Stage stage = (Stage) btnSelectAudioVideoFile.getScene().getWindow();
        file = selectAudioVideoFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("All Media Files",
                        "*.mp3", "*.wav", "*.ogg", "*.flac", "*.m4a", "*.aac", "*.wma",
                        "*.mp4", "*.avi", "*.mkv", "*.mov", "*.flv", "*.wmv"),
                    "Choice video/audio file"
                );

        if(file == null) return;

        ErrorLogger.info("User select file (video/audio): " + file.getAbsolutePath());
        labelSelectAudioName.setText("Select video/audio: " + file.getName());
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
            ErrorLogger.alertDialog(Alert.AlertType.WARNING, "WARN", "Output path missing!", "Select output directory!");
            return;
        }

        if (file == null) {
            ErrorLogger.alertDialog(Alert.AlertType.WARNING, "WARN", "File missing!", "Select audio or video file!");
            return;
        }

        int originalChannels = getChannels(file);
        if (originalChannels == 1 && channel == 2) {
            boolean proceed = ErrorLogger.confirmationDialog(
                    "Mono to Stereo Confirmation",
                    "The source file is mono (1 channel).",
                    "Do you want to convert it to stereo (2 channels) anyway?"
            );
            if (!proceed) return;
        }

        progressBarConvert.setProgress(0);
        btnSubmitConvert.setDisable(true);
        ConverterVideoAudioFile.convert(file, outputPath, bitRate, channel, samplingRate,
                "libmp3lame", "mp3", progress -> {
            Platform.runLater(() -> progressBarConvert.setProgress(progress));
        }).thenAccept(success -> Platform.runLater(() -> {
            btnSubmitConvert.setDisable(false);
            if (success) {
                showSuccessMessage(labelSuccessConvert, "mp3", hideSuccessMessageTimer);
                showProgressBar(progressBarConvert, hideSuccessMessageTimer);
            } else {
                if (progressBarConvert.getProgress() > 0 && progressBarConvert.getProgress() < 1.0) {
                    progressBarConvert.setProgress(0);
                } else {
                    showErrorMessage(labelSuccessConvert, progressBarConvert,"So close, yet no success", hideSuccessMessageTimer);
                }
            }
        }));
    }

    @FXML
    public void onResetPressed() {
        comboBoxChoiceBitRate.setValue("320 kbps");
        comboBoxChoiceChannels.setValue("2 Channels");
        comboBoxChoiceSamplingRate.setValue("48000 Hz");
        bitRate = 128;
        channel = 2;
        samplingRate = 44100;
        progressBarConvert.setProgress(0);
        labelSelectAudioName.setText("Selected file: none");
        file = null;
        outputPath = DEFAULT_PATH;
        dropZone.getStyleClass().remove("drop-zone-filled");
        textDragZone.setText("Drag files here");
    }

    public void onChoiceBitRate() {
        bitRate = parseComboBoxStringToInt(comboBoxChoiceBitRate);
        ErrorLogger.info("Select bitRate: " + bitRate);

    }

    public void onChoiceChannels() {
        channel = parseComboBoxStringToInt(comboBoxChoiceChannels);
        ErrorLogger.info("Select channels: " + channel);

    }

    public void onChoiceSamplingRate() {
        samplingRate = parseComboBoxStringToInt(comboBoxChoiceSamplingRate);
        ErrorLogger.info("Select sampling rate: " + samplingRate);
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
        Dragboard db = e.getDragboard();
        boolean success = false;

        if(db.hasFiles()){
            file = db.getFiles().getFirst();

            success = true;
            dropZone.getStyleClass().remove("drop-zone-filled");
            dropZone.getStyleClass().add("drop-zone-filled");
            textDragZone.setText("Select file: " + file.getName());
        }

        e.setDropCompleted(success);
        labelSelectAudioName.setText(file.getName());
        e.consume();
    }

    public void handleDragOver(DragEvent e) {
        Dragboard db = e.getDragboard();

        if (e.getGestureSource() != dropZone && db.hasFiles()) {
            File file = db.getFiles().getFirst();

            if (isSupportedMediaFile(file)) {
                e.acceptTransferModes(TransferMode.COPY);
            }
        }

        e.consume();
    }

    private boolean isSupportedMediaFile(File file) {
        String name = file.getName().toLowerCase();

        return name.endsWith(".mp3")
                || name.endsWith(".wav")
                || name.endsWith(".aac")
                || name.endsWith(".mp4")
                || name.endsWith(".mkv")
                || name.endsWith(".avi")
                || name.endsWith(".mov")
                || name.endsWith(".m4a")
                || name.endsWith(".flac")
                || name.endsWith(".ogg")
                || name.endsWith(".wma")
                || name.endsWith(".flv")
                || name.endsWith(".wmv")
                || name.endsWith(".webm")
                || name.endsWith(".3gp");
    }


    public void onCancelConversation() {
        ConverterVideoAudioFile.cancelConversion();
    }
}
