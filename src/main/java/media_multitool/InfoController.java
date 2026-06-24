package media_multitool;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import model.logger.ErrorLogger;
import model.utility.Clipboards;
import model.utility.OS;
import viewHelp.Alerts;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class InfoController {
    @FXML private Tooltip activeTooltip;

    @FXML
    public void toLogsWindows() {
        String logPath = OS.getAppConfigDir() + File.separator + "logs";
        File dirLog = new File(logPath);

        try {
            if (!dirLog.exists()) {
                if(!dirLog.mkdirs()) {
                    ErrorLogger.error("Error create directory!");
                }
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dirLog);
            }
        } catch (IOException e) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Error opening directory", "IO Error", "Could not open logs directory!");
        }
    }

    @FXML
    private void handleContactClick(MouseEvent mouseEvent) {
        if (activeTooltip != null && activeTooltip.isShowing()) {
            return;
        }

        String discordId = "jussi6";

        Clipboards clipboards = new Clipboards();
        clipboards.clip(discordId,"Discord ID copied to clipboard!\nNow you can paste it.", 2, mouseEvent);
    }
}
