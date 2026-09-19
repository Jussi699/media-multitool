package viewHelp;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class ImageZoomHelper {
    private static final double ZOOM_FACTOR = 16.0;
    private static final double MAGNIFIER_RADIUS = 86.0;

    public static void applyZoomEffect(ImageView imageView, StackPane container) {
        if (imageView == null || container == null) return;

        imageView.setCursor(Cursor.CROSSHAIR);

        Canvas magnifierCanvas = new Canvas(MAGNIFIER_RADIUS * 2, MAGNIFIER_RADIUS * 2);
        magnifierCanvas.setMouseTransparent(true);

        Circle border = new Circle(MAGNIFIER_RADIUS, MAGNIFIER_RADIUS, MAGNIFIER_RADIUS);
        border.setFill(null);
        border.setStroke(Color.color(0, 0.8, 0, 0.8));
        border.setStrokeWidth(2);
        border.setMouseTransparent(true);

        Circle centerDot = new Circle(MAGNIFIER_RADIUS, MAGNIFIER_RADIUS, 2);
        centerDot.setFill(Color.color(0, 0, 0, 0.5));
        centerDot.setMouseTransparent(true);

        StackPane magnifierWrapper = new StackPane(magnifierCanvas, border, centerDot);
        magnifierWrapper.setPickOnBounds(false);
        magnifierWrapper.setMouseTransparent(true);
        magnifierWrapper.setVisible(false);
        magnifierWrapper.setManaged(false);

        container.getChildren().add(magnifierWrapper);

        imageView.setOnMouseEntered(event -> {
            if (imageView.getImage() != null) {
                magnifierWrapper.setVisible(true);
                updateMagnifier(event, imageView, magnifierCanvas, magnifierWrapper, container);
            }
        });

        imageView.setOnMouseExited(_ -> magnifierWrapper.setVisible(false));

        imageView.setOnMouseMoved(event -> {
            if (imageView.getImage() != null) {
                if (!magnifierWrapper.isVisible()) {
                    magnifierWrapper.setVisible(true);
                }
                updateMagnifier(event, imageView, magnifierCanvas, magnifierWrapper, container);
            } else {
                magnifierWrapper.setVisible(false);
            }
        });
    }

    private static void updateMagnifier(MouseEvent event, ImageView imageView, Canvas magnifierCanvas, StackPane magnifierWrapper, StackPane container) {
        double x = event.getX();
        double y = event.getY();

        Image originalImage = imageView.getImage();
        if (originalImage == null) return;

        double imageWidth = originalImage.getWidth();
        double imageHeight = originalImage.getHeight();

        double displayedWidth = imageView.getBoundsInLocal().getWidth();
        double displayedHeight = imageView.getBoundsInLocal().getHeight();

        if (imageView.isPreserveRatio()) {
            double ratio = Math.min(displayedWidth / imageWidth, displayedHeight / imageHeight);
            displayedWidth = imageWidth * ratio;
            displayedHeight = imageHeight * ratio;
        }

        double offsetX = (imageView.getBoundsInLocal().getWidth() - displayedWidth) / 2;
        double offsetY = (imageView.getBoundsInLocal().getHeight() - displayedHeight) / 2;

        double relativeX = (x - offsetX) / displayedWidth;
        double relativeY = (y - offsetY) / displayedHeight;

        if (relativeX < 0 || relativeX > 1 || relativeY < 0 || relativeY > 1) {
            magnifierWrapper.setVisible(false);
            return;
        } else {
            magnifierWrapper.setVisible(true);
        }

        double sourceX = Math.round(relativeX * imageWidth);
        double sourceY = Math.round(relativeY * imageHeight);

        double viewWidth = Math.round((MAGNIFIER_RADIUS * 2) / ZOOM_FACTOR);
        double viewHeight = Math.round((MAGNIFIER_RADIUS * 2) / ZOOM_FACTOR);

        int startX = (int) Math.round(sourceX - viewWidth / 2);
        int startY = (int) Math.round(sourceY - viewHeight / 2);
        int w = (int) viewWidth;
        int h = (int) viewHeight;

        int imgW = (int) imageWidth;
        int imgH = (int) imageHeight;

        PixelReader reader = originalImage.getPixelReader();
        if (reader != null) {
            GraphicsContext gc = magnifierCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, MAGNIFIER_RADIUS * 2, MAGNIFIER_RADIUS * 2);

            gc.save();
            gc.beginPath();
            gc.arc(MAGNIFIER_RADIUS, MAGNIFIER_RADIUS, MAGNIFIER_RADIUS, MAGNIFIER_RADIUS, 0, 360);
            gc.closePath();
            gc.clip();

            for (int dy = 0; dy < h; dy++) {
                int srcY = startY + dy;
                for (int dx = 0; dx < w; dx++) {
                    int srcX = startX + dx;
                    Color color = Color.TRANSPARENT;
                    if (srcX >= 0 && srcX < imgW && srcY >= 0 && srcY < imgH) {
                        color = reader.getColor(srcX, srcY);
                    }
                    gc.setFill(color);
                    gc.fillRect(dx * ZOOM_FACTOR, dy * ZOOM_FACTOR, ZOOM_FACTOR, ZOOM_FACTOR);
                }
            }
            gc.restore();
        }

        Point2D scenePt = imageView.localToScene(x, y);
        Point2D containerPt = container.sceneToLocal(scenePt);

        magnifierWrapper.setLayoutX(Math.round(containerPt.getX() - MAGNIFIER_RADIUS));
        magnifierWrapper.setLayoutY(Math.round(containerPt.getY() - MAGNIFIER_RADIUS));
    }
}