package model.helper.images;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.Optional;

public class PixelHelper {
    public static Optional<Color> pixelSelection(MouseEvent e, ImageView imageViewPreview) {
        Image image = imageViewPreview.getImage();
        if (image == null) return Optional.empty();

        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();

        double displayedWidth = imageViewPreview.getBoundsInLocal().getWidth();
        double displayedHeight = imageViewPreview.getBoundsInLocal().getHeight();

        double actualWidth = displayedWidth;
        double actualHeight = displayedHeight;
        double offsetX = 0;
        double offsetY = 0;

        if (imageViewPreview.isPreserveRatio()) {
            double ratio = Math.min(displayedWidth / imageWidth, displayedHeight / imageHeight);
            actualWidth = imageWidth * ratio;
            actualHeight = imageHeight * ratio;
            offsetX = (displayedWidth - actualWidth) / 2;
            offsetY = (displayedHeight - actualHeight) / 2;
        }

        double mouseX = e.getX() - offsetX;
        double mouseY = e.getY() - offsetY;

        if (mouseX < 0 || mouseX >= actualWidth || mouseY < 0 || mouseY >= actualHeight) {
            return Optional.empty();
        }

        int pixelX = (int) (mouseX * imageWidth / actualWidth);
        int pixelY = (int) (mouseY * imageHeight / actualHeight);

        pixelX = Math.clamp(pixelX, 0, (int) imageWidth - 1);
        pixelY = Math.clamp(pixelY, 0, (int) imageHeight - 1);

        PixelReader pixelReader = image.getPixelReader();
        if (pixelReader != null) {
            return Optional.of(pixelReader.getColor(pixelX, pixelY));
        }

        return Optional.empty();
    }

    public static String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
