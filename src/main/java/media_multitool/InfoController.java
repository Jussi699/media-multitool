package media_multitool;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import model.utility.Util;
import viewHelp.Alerts;
import viewHelp.Tooltips;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class InfoController {
    @FXML private Tooltip activeTooltip;

    @FXML
    public void initialize() {
    }

    @FXML
    public void toLogsWindows() {
        String logPath = Util.getAppConfigDir() + File.separator + "logs";
        File dirLog = new File(logPath);

        try {
            if (!dirLog.exists()) {
                dirLog.mkdirs();
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
        
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(discordId);
        clipboard.setContent(content);

        showSuccessTooltip(mouseEvent);
    }

    private void showSuccessTooltip(MouseEvent mouseEvent) {
        Tooltips.setupTooltipInfo(mouseEvent, "Discord ID copied to clipboard!\nNow you can paste it.", 2);
    }
}
