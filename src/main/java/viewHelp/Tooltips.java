package viewHelp;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

public class Tooltips {
    private static final Map<Node, Tooltip> activeTooltips = new HashMap<>();
    public static void setupTooltipInfo(MouseEvent event, String text, int showSeconds) {
        Node source = (Node) event.getSource();

        if (activeTooltips.containsKey(source) && activeTooltips.get(source).isShowing()) {
            return;
        }

        Tooltip tooltip = getTooltip(text);

        tooltip.show(source, event.getScreenX(), event.getScreenY() + 15);
        activeTooltips.put(source, tooltip);

        PauseTransition delay = new PauseTransition(Duration.seconds(showSeconds));
        delay.setOnFinished(_ -> {
            tooltip.hide();
            activeTooltips.remove(source);
        });
        delay.play();
    }

    private static Tooltip getTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);

        tooltip.setWrapText(true);
        tooltip.setMaxWidth(220);
        tooltip.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        tooltip.setStyle("-fx-font-size: 13px; " +
                         "-fx-background-color: #32CD32; " +
                         "-fx-text-fill: white; " +
                         "-fx-padding: 10; " +
                         "-fx-font-weight: bold; " +
                         "-fx-background-radius: 8;");

        tooltip.setAutoHide(true);
        tooltip.setShowDelay(Duration.ZERO);
        return tooltip;
    }
}
