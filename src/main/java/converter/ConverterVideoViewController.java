package converter;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.logger.ErrorLogger;
import static model.utility.Util.*;

import java.io.File;
import java.nio.file.Paths;

public class ConverterVideoViewController {
    private static final String DEFAULT_FILE_TEXT = "Selected file: none";
    private static final int SUCCESS_MESSAGE_DURATION_SECONDS = 5;

    private File videoFile;
    private File outputPath;
    private String targetFormat;
    private final PauseTransition hideSuccessMessageTimer =
            new PauseTransition(Duration.seconds(SUCCESS_MESSAGE_DURATION_SECONDS));

    @FXML private VBox converterVideoPage;
    @FXML private Label labelConvertVideo;
    @FXML private Button btnSelectVideoFile;
    @FXML private Button btnChoiceDirForSaveVideo;
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
    }

    @FXML
    public void onSelectVideoPressed() {
//        SelectFile selectFile = new SelectFile();
//        Stage stage = (Stage) btnSelectVideoFile.getScene().getWindow();
//        videoFile = selectFile.choiceFile(stage);

        if (videoFile != null) {
            labelSelectVideoName.setText("Selected file: " + videoFile.getName());
            ErrorLogger.info("User selected video: " + videoFile.getAbsolutePath());
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
        hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
        if (!isReadyForConversion()) {
            return;
        }

        // Placeholder for real conversion integration.
        showSuccessMessage(labelSuccessConvert, targetFormat, hideSuccessMessageTimer);
    }

    @FXML
    public void onResetPressed() {
        videoFile = null;
        targetFormat = null;
        labelSelectVideoName.setText(DEFAULT_FILE_TEXT);
        btnToMP4.setSelected(false);
        btnToAVI.setSelected(false);
        btnToMKV.setSelected(false);
        hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
    }

    private boolean isReadyForConversion() {
        if (videoFile == null) {
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
}
