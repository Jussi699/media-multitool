package viewHelp;

import javafx.scene.control.Button;
import javafx.scene.paint.Color;

public class WorkColors {

    /**
     * Обновление графического интерфейса (View) кнопки выбора цвета.
     */
    public static void updateColorView(java.awt.Color awtColor, Button btnColorPicker) {
        String hexColor = String.format("#%02x%02x%02x", awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
        boolean darkBackground = isDark(awtColor);

        btnColorPicker.setStyle(String.format(
                "-fx-background-color: %s; " +
                        "-fx-text-fill: %s; " +
                        "-fx-min-width: 100; -fx-min-height: 30; " +
                        "-fx-border-color: black; -fx-border-radius: 5; -fx-background-radius: 5;",
                hexColor, darkBackground ? "white" : "black"
        ));
    }

    /**
     * Проверка, является ли цвет темным (для выбора цвета текста).
     */
    public static boolean isDark(java.awt.Color color) {
        double luma = 0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue();
        return luma < 128;
    }

    /**
     * Конвертация JavaFX Color в java.awt.Color.
     */
    public static java.awt.Color toAwtColor(Color fxColor) {
        return new java.awt.Color(
                (float) fxColor.getRed(),
                (float) fxColor.getGreen(),
                (float) fxColor.getBlue(),
                (float) fxColor.getOpacity()
        );
    }

    /**
     * Конвертация java.awt.Color в JavaFX Color.
     */
    public static Color toFxColor(java.awt.Color awtColor) {
        return Color.rgb(
                awtColor.getRed(),
                awtColor.getGreen(),
                awtColor.getBlue(),
                awtColor.getAlpha() / 255.0
        );
    }
}
