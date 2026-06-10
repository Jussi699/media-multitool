package model.utility;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import viewHelp.Tooltips;

public class Clipboards {
    public void clip(String copyText, String textSuccess, int showSecond, MouseEvent event) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(copyText);
        clipboard.setContent(content);

        showSuccessTooltip(event, textSuccess, showSecond);
    }

    private void showSuccessTooltip(MouseEvent mouseEvent, String copyText, int showSecond) {
        Tooltips.setupTooltip(mouseEvent, copyText, showSecond);
    }
}
