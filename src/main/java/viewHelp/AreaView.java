package viewHelp;

import javafx.scene.control.TextArea;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AreaView {
    private TextArea textArea;
    private int fontSize = 10;
    private String textColor = "#cccccc";

    public AreaView(TextArea textArea) {
        this.textArea = textArea;
    }

    public void changeTextColor(String color) {
        this.textColor = color;
        applyStyle();
    }

    public void changeFontSize(int size) {
        this.fontSize = size;
        applyStyle();
    }

    private void applyStyle() {
        if (textArea != null) {
            textArea.setStyle(String.format("-fx-font-size: %dpx; -fx-text-fill: %s; -fx-text-inner-color: %s;",
                    fontSize, textColor, textColor));
        }
    }
}