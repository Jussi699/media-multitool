package converter;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.converterMP3.ConverterToMP3;
import model.logger.ErrorLogger;
import model.workWithFiles.SelectAudioVideoFile;

import java.io.File;
import java.nio.file.Paths;

import static model.workWithFiles.Util.*;

public class ConverterMP3ViewController {
    private static final String DEFAULT_FILE_TEXT_SELECT_FILE = "Selected file: none";
    private static final String DEFAULT_FILE_TEXT_SUCCESS_CONVERT = "Successfully converted!";
    private static final String DEFAULT_FILE_TEXT_SUCCESS_DOES_NOT_CONVERT = "So close, yet no success";
    private static final int SUCCESS_MESSAGE_DURATION_SECONDS = 5;

    private final File DEFAULT_PATH = Paths.get(System.getProperty("user.home"), "Desktop").toFile();
    private int bitRate;
    private int channel;
    private int samplingRate;
    private File outputPath;
    private File file;
    private final PauseTransition hideSuccessMessageTimer =
            new PauseTransition(Duration.seconds(SUCCESS_MESSAGE_DURATION_SECONDS));

    @FXML public VBox converterMP3Page;
    @FXML private Label labelConvertMP3;
    @FXML private Button btnSelectAudioVideoFile;
    @FXML private Button btnChoiceDirForSaveMP3;
    @FXML private Label labelSelectAudioName;
    @FXML private ToggleButton btnToMP3;
    @FXML private Button btnSubmitConvert;
    @FXML private Button btnReset;
    @FXML private Label labelSuccessConvert;
    @FXML private ComboBox<String> comboBoxChoiceBitRate;
    @FXML private ComboBox<String> comboBoxChoiceChannels;
    @FXML private ComboBox<String> comboBoxChoiceSamplingRate;


    @FXML
    public void initialize() {
        outputPath = DEFAULT_PATH;
        setupClearMessageTimer(labelSuccessConvert, hideSuccessMessageTimer);
        labelSelectAudioName.setText(DEFAULT_FILE_TEXT_SELECT_FILE);

        setupComboBox(comboBoxChoiceBitRate);
        setupComboBox(comboBoxChoiceChannels);
        setupComboBox(comboBoxChoiceSamplingRate);

        comboBoxChoiceBitRate.getItems().addAll("128 kbps", "192 kbps", "256 kbps", "320 kbps");
        comboBoxChoiceChannels.getItems().addAll("1 Channels", "2 Channels");
        comboBoxChoiceSamplingRate.getItems().addAll("8000 Hz", "11025 Hz",
                                                        "12000 Hz", "16000 Hz",
                                                        "22050 Hz", "24000 Hz",
                                                        "32000 Hz", "44100 Hz", "48000 Hz");

        comboBoxChoiceBitRate.setValue("128 kbps");
        comboBoxChoiceChannels.setValue("2 Channels");
        comboBoxChoiceSamplingRate.setValue("44100 Hz");

        bitRate = 128;
        channel = 2;
        samplingRate = 44100;
    }

    @FXML
    public void onSelectAudioVideoPressed() {
        SelectAudioVideoFile selectAudioVideoFile = new SelectAudioVideoFile();
        Stage stage = (Stage) btnSelectAudioVideoFile.getScene().getWindow();
        file = selectAudioVideoFile.choiceFile(stage);

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
    public void onFormatMp3Pressed() {

    }

    @FXML
    public void onStartConversionPressed() {
        ConverterToMP3.convert(file, outputPath, bitRate, channel, samplingRate);
    }

    @FXML
    public void onResetPressed() {
        comboBoxChoiceBitRate.setValue("128 kbps");
        comboBoxChoiceChannels.setValue("2 Channels");
        comboBoxChoiceSamplingRate.setValue("44100 Hz");
        bitRate = 128;
        channel = 2;
        samplingRate = 44100;
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
}
