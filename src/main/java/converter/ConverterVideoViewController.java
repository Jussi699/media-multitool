package converter;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.converterVideo.ConverterVideoAudioFile;
import model.logger.ErrorLogger;

import static model.utility.DetermineType.determineFormat;
import static model.utility.Util.*;
import model.select.SelectFile;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.nio.file.Paths;

public class ConverterVideoViewController {
    private static final String DEFAULT_FILE_TEXT = "Selected file: none";
    private static final int SUCCESS_MESSAGE_DURATION_SECONDS = 5;
    private static String videoCodec;

    private int bitRate;
    private int channel;
    private int samplingRate;
    private File outputPath;
    private File file;
    private String targetFormat;
    private final PauseTransition hideSuccessMessageTimer =
            new PauseTransition(Duration.seconds(SUCCESS_MESSAGE_DURATION_SECONDS));

    @FXML private ToggleButton btnToWEBM;
    @FXML private VBox converterVideoPage;
    @FXML private Label labelConvertVideo;
    @FXML private Button btnSelectVideoFile;
    @FXML private Button btnChoiceDirForSaveVideo;
    @FXML private ProgressBar progressBarConvert;
    @FXML private Button btnSubmitConvert;
    @FXML private Button btnReset;
    @FXML private ToggleButton btnToMP4;
    @FXML private ToggleButton btnToAVI;
    @FXML private ToggleButton btnToMKV;
    @FXML private Label labelSelectVideoName;
    @FXML private Label labelSuccessConvert;

    @FXML
    public void initialize() {
        outputPath = Paths.get(System.getProperty("user.home"), "Desktop").toFile();
        setupClearMessageTimer(labelSuccessConvert, hideSuccessMessageTimer);
        labelSelectVideoName.setText(DEFAULT_FILE_TEXT);

        try {
            MultimediaObject instance = new MultimediaObject(new File("C:\\Users\\dinfa\\Desktop\\sample-5s.webm"));
            MultimediaInfo info = instance.getInfo();
            ErrorLogger.info(info.getFormat());
        }
        catch (EncoderException e) {
            ErrorLogger.warn("Unable to determine file type!");
            ErrorLogger.log(113, ErrorLogger.Level.ERROR, "EncoderException", e);
        }
    }

    @FXML
    public void onSelectVideoPressed() {
       SelectFile selectFile = new SelectFile();
       Stage stage = (Stage) btnSelectVideoFile.getScene().getWindow();
        file = selectFile.choiceFile(stage,
               new FileChooser.ExtensionFilter("Video", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.webm") ,"Select vide");

        if (file != null) {
            labelSelectVideoName.setText("Selected file: " + file.getName());
            ErrorLogger.info("User selected video: " + file.getAbsolutePath());
            hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
        }
    }

    @FXML
    public void onSelectOutputDirectoryPressed() {
        Stage stage = (Stage) btnChoiceDirForSaveVideo.getScene().getWindow();
        File selectedPath = setPathForSave(stage, outputPath);
        if (selectedPath != null) {
            outputPath = selectedPath;
            hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
            ErrorLogger.info("Output directory selected: " + outputPath.getAbsolutePath());
        }
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

    private void selectFormat(String format, ToggleButton selectedBtn) {
        targetFormat = format;
        btnToMP4.setSelected(selectedBtn == btnToMP4);
        btnToAVI.setSelected(selectedBtn == btnToAVI);
        btnToMKV.setSelected(selectedBtn == btnToMKV);
        hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
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

        String formatFile = determineFormat(file);

        if(!(formatFile == null)) {
            switch (formatFile){
                case "mp4" -> videoCodec = "libx264";
                case "mkv", "matroska" -> videoCodec = "libx264";
                case "avi" -> videoCodec = "mpeg4";
                case "webm" -> videoCodec = "libvpx-vp9";
                default -> {
                    videoCodec = "libx264";
                    ErrorLogger.warn("Unknown format: " + formatFile + ". Using default codec.");
                }
            }
        }
        else {
            ErrorLogger.warn("DetermineFormatVideo return NULL");
            ErrorLogger.alertDialog(Alert.AlertType.ERROR, "ERROR", "NULL",
                    "The method returned NULL.\nCheck that the file has not been damaged!");
            return;
        }

        targetFormat = formatFile;
        ConverterVideoAudioFile.convert(file, outputPath, bitRate, channel, samplingRate,
                videoCodec, formatFile, progress -> {
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
                    showErrorMessage(labelSuccessConvert, progressBarConvert,
                            "So close, yet no success",
                            hideSuccessMessageTimer);
                }
            }
        }));
    }

    @FXML
    public void onResetPressed() {
        file = null;
        targetFormat = null;
        labelSelectVideoName.setText(DEFAULT_FILE_TEXT);
        btnToMP4.setSelected(false);
        btnToAVI.setSelected(false);
        btnToMKV.setSelected(false);
        hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
    }

    private boolean isReadyForConversion() {
        if (file == null) {
            ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Warning", "Selection", "Select video first.");
            return false;
        }

        if (targetFormat == null) {
            ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Warning", "Selection", "Select target video format.");
            return false;
        }

        if (outputPath == null) {
            ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Warning", "Selection", "Select output directory.");
            return false;
        }

        return true;
    }

    public void onFormatWEBMPressed() {

    }
}
