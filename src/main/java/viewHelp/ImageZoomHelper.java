package viewHelp;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class ImageZoomHelper {
    private static final double ZOOM_FACTOR = 8.0;
    private static final double MAGNIFIER_RADIUS = 70.0;

    public static void applyZoomEffect(ImageView imageView, StackPane container) {
        if (imageView == null || container == null) return;

        imageView.setCursor(Cursor.CROSSHAIR);

        ImageView magnifier = new ImageView();
        magnifier.setFitWidth(MAGNIFIER_RADIUS * 2);
        magnifier.setFitHeight(MAGNIFIER_RADIUS * 2);
        magnifier.setPreserveRatio(true);
        magnifier.setSmooth(false);
        magnifier.setCache(false);
        magnifier.setMouseTransparent(true);

        Circle clip = new Circle(MAGNIFIER_RADIUS, MAGNIFIER_RADIUS, MAGNIFIER_RADIUS);
        magnifier.setClip(clip);

        Circle border = new Circle(MAGNIFIER_RADIUS, MAGNIFIER_RADIUS, MAGNIFIER_RADIUS);
        border.setFill(null);
        border.setStroke(Color.color(1, 0, 0, 0.8));
        border.setStrokeWidth(2);
        border.setMouseTransparent(true);

        Circle centerDot = new Circle(MAGNIFIER_RADIUS, MAGNIFIER_RADIUS, 2);
        centerDot.setFill(Color.color(0, 0, 0, 0.5));
        centerDot.setMouseTransparent(true);

        StackPane magnifierWrapper = new StackPane(magnifier, border, centerDot);
        magnifierWrapper.setPickOnBounds(false);
        magnifierWrapper.setMouseTransparent(true);
        magnifierWrapper.setVisible(false);
        magnifierWrapper.setManaged(false);

        container.getChildren().add(magnifierWrapper);

        imageView.setOnMouseEntered(event -> {
            if (imageView.getImage() != null) {
                magnifier.setImage(imageView.getImage());
                magnifierWrapper.setVisible(true);
                updateMagnifier(event, imageView, magnifier, magnifierWrapper, container);
            }
        });

        imageView.setOnMouseExited(_ -> magnifierWrapper.setVisible(false));

        imageView.setOnMouseMoved(event -> {
            if (imageView.getImage() != null) {
                if (!magnifierWrapper.isVisible()) {
                    magnifier.setImage(imageView.getImage());
                    magnifierWrapper.setVisible(true);
                }
                updateMagnifier(event, imageView, magnifier, magnifierWrapper, container);
            } else {
                magnifierWrapper.setVisible(false);
            }
        });
    }

    private static void updateMagnifier(MouseEvent event, ImageView imageView, ImageView magnifier, StackPane magnifierWrapper, StackPane container) {
        double x = event.getX();
        double y = event.getY();

        double imageWidth = imageView.getImage().getWidth();
        double imageHeight = imageView.getImage().getHeight();
        
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

        double sourceX = Math.floor(relativeX * imageWidth);
        double sourceY = Math.floor(relativeY * imageHeight);

        double viewWidth = (MAGNIFIER_RADIUS * 2) / ZOOM_FACTOR;
        double viewHeight = (MAGNIFIER_RADIUS * 2) / ZOOM_FACTOR;

        magnifier.setViewport(new Rectangle2D(
                sourceX - Math.floor(viewWidth / 2),
                sourceY - Math.floor(viewHeight / 2),
                viewWidth,
                viewHeight
        ));

        Point2D scenePt = imageView.localToScene(x, y);
        Point2D containerPt = container.sceneToLocal(scenePt);

        magnifierWrapper.setLayoutX(containerPt.getX() - MAGNIFIER_RADIUS);
        magnifierWrapper.setLayoutY(containerPt.getY() - MAGNIFIER_RADIUS);
    }
}