package visual.util;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import visual.controls.Tooltips;

import static java.util.Objects.isNull;

public class Clipboards {
    private Clipboards() {
        /* This utility class should not be instantiated */
    }

    public static void clip(String copyText, String textSuccess, int showSecond, MouseEvent event) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(copyText);
        clipboard.setContent(content);

        showSuccessTooltip(event, textSuccess, showSecond);
    }

    private static void showSuccessTooltip(MouseEvent mouseEvent, String copyText, int showSecond) {
        Tooltips.setupTooltip(mouseEvent, copyText, showSecond);
    }

    public static void copyToClipboard(String text) {
        if (isNull(text)) return;
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }
}
