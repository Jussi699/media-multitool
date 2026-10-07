package media_multitool;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import model.helper.FileHelper;
import model.logger.ErrorLogger;
import model.utility.Clipboards;
import model.utility.OS;
import viewHelp.Alerts;
import viewHelp.AreaView;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.utility.TemplateCheck.canRead;
import static model.utility.TemplateCheck.canReadAndWrite;

public class InfoController {
    @FXML private Spinner<Integer> spinnerFontSize;
    @FXML private TextArea logArea;
    @FXML private Button btnGithub, reloadLogArea, clearLogArea;
    @FXML private RadioButton colorDefault, colorGreen;

    private AreaView areaViewHelper;
    private FileHelper fileHelper;

    private final File currentFile = getTodayLogFile();

    @FXML
    public void initialize() {
        areaViewHelper = new AreaView(logArea);

        setupSpinner();
        setupRadioButton();

        updateStringsInLogArea();

        fileHelper = new FileHelper(currentFile);
    }

    private void setupSpinner() {
        spinnerFontSize.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(6, 20, 10, 1));

        spinnerFontSize.getValueFactory().valueProperty()
                .addListener((_, _, newValue) -> areaViewHelper.changeFontSize(newValue));
    }

    private void setupRadioButton() {
        ToggleGroup group = new ToggleGroup();
        colorDefault.setToggleGroup(group);
        colorGreen.setToggleGroup(group);

        group.selectedToggleProperty().addListener((_, _, newToggle) -> {
            if (nonNull(newToggle)) {
                RadioButton selected = (RadioButton) newToggle;

                if (selected == colorGreen) {
                    areaViewHelper.changeTextColor("#2ba82b");
                } else if (selected == colorDefault) {
                    areaViewHelper.changeTextColor("#cccccc");
                }
            }
        });

        colorDefault.setSelected(true);
    }

    private File getTodayLogFile() {
        String todayDate = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        File logDirFile = new File(OS.getAppConfigDir() + File.separator + "logs");
        return new File(logDirFile, "app." + todayDate + ".log");
    }

    @FXML
    private void toLogsDir() {
        String logPath = OS.getAppConfigDir() + File.separator + "logs";
        File dirLog = new File(logPath);

        try {
            if (!dirLog.exists() && !dirLog.mkdirs()) {
                ErrorLogger.error("Error create directory!");
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dirLog);
            }
        } catch (IOException e) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Error opening directory", "IO Error", "Could not open logs directory!");
            ErrorLogger.error("Could not open logs directory: " + e.getMessage());
        }
    }

    public void callReloadLogArea() {
        onReloadLogArea();
    }

    @FXML
    private void onReloadLogArea() {
        if(canRead(currentFile)) {
            updateStringsInLogArea();
        }
    }

    @FXML
    private void onClearLogArea() {
        if(canReadAndWrite(currentFile)) {
            clearLogArea();
        }
    }

    private void updateStringsInLogArea() {
        CompletableFuture.runAsync(() -> {
            fileHelper = new FileHelper(currentFile);
            try {
                ArrayList<String> listStrings = fileHelper.getStringsFromFile();

                if(listStrings.isEmpty()) {
                    Platform.runLater(() -> logArea.setText("Logs for today are missing or empty."));
                }
                else {
                    String fullLog = String.join("\n", listStrings);
                    Platform.runLater(() -> {
                        logArea.setText(fullLog);
                        logArea.positionCaret(fullLog.length());
                    });
                }
            } catch (IOException e) {
                ErrorLogger.error("Error reading log file: " + e.getMessage());
                Platform.runLater(() ->
                        Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Error reading",
                                "Error reading the log file.\nCheck log file for more details!")
                );
            }
        });
    }

    private void clearLogArea() {
        CompletableFuture.runAsync(() -> {
            try {
                fileHelper.clearAllStringsFromFile();
                Platform.runLater(() -> {
                    logArea.setText("Logs for today are missing or empty.");
                    logArea.positionCaret(0);
                });
            } catch (IOException e) {
                ErrorLogger.error("Error clearing the log file: " + currentFile.getAbsolutePath() + "\n" + e.getMessage());
                Platform.runLater(() ->
                        Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Error clearing",
                                "Error clearing the log file.\nCheck log file for more details!")
                );
            }
        });
    }

    @FXML
    private void handleContactClick(MouseEvent mouseEvent) {
        String discordId = "jussi6";

        Clipboards.clip(discordId,"Discord ID copied to clipboard!\nNow you can paste it.", 2, mouseEvent);
    }

    @FXML
    private void redirectToGithub() {
        if (isNull(btnGithub)) {
            ErrorLogger.error("The GitHub redirect button has a null value!");
            return;
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI("https://github.com/Jussi699/media-multitool"));
            }
        } catch (Exception e) {
            ErrorLogger.error("An error occurred while opening the page: " + e);
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error opening page", "Error opening page",
                    "An error occurred while opening the page.\nCheck log file for more details!");
        }
    }
}
