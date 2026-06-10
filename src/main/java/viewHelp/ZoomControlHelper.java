package viewHelp;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class ZoomControlHelper {
    private final ScrollPane scrollPane;
    private final ImageView imageView;
    private final Slider slider;
    private final StackPane container;

    private boolean suppressZoomListener;
    private Point2D sliderZoomSceneFocal, sliderZoomImageAnchor;

    public ZoomControlHelper(ScrollPane scrollPane, ImageView imageView, Slider slider, StackPane container, double minZoom, double maxZoom) {
        this.scrollPane = scrollPane;
        this.imageView = imageView;
        this.slider = slider;
        this.container = container;
        
        slider.setMin(minZoom);
        slider.setMax(maxZoom);
        slider.setValue(1.0);
        
        init();
    }

    private void init() {
        imageView.scaleXProperty().bind(slider.valueProperty());
        imageView.scaleYProperty().bind(slider.valueProperty());
        imageView.setPickOnBounds(true);

        slider.valueProperty().addListener((_, _, newVal) -> {
            if (suppressZoomListener) {
                return;
            }
            captureViewportCenterAnchor();
            applyZoomWithAnchor(newVal.doubleValue(), sliderZoomSceneFocal, sliderZoomImageAnchor);
        });

        scrollPane.viewportBoundsProperty().addListener((_, _, _) -> updateImageSize());
        imageView.imageProperty().addListener((_, _, _) -> updateImageSize());
        setupZoomWithMouseWheel();
    }

    public void updateImageSize() {
        if (imageView.getImage() != null) {
            if (slider.getValue() == 1.0) {
                double viewPortWidth = scrollPane.getViewportBounds().getWidth();
                double viewPortHeight = scrollPane.getViewportBounds().getHeight();

                imageView.setFitWidth(viewPortWidth - 20);
                imageView.setFitHeight(viewPortHeight - 20);
                imageView.setPreserveRatio(true);
                updateImageContainerSize(slider.getValue());
            }
        }
    }

    private void setupZoomWithMouseWheel() {
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (imageView.getImage() == null || event.getDeltaY() == 0) {
                return;
            }
            event.consume();

            double step = 0.1;
            double delta = event.getDeltaY() > 0 ? step : -step;
            double oldZoom = slider.getValue();
            double newZoom = Math.clamp(
                    oldZoom + delta,
                    slider.getMin(),
                    slider.getMax()
            );
            if (Math.abs(newZoom - oldZoom) < 1e-9) {
                return;
            }

            Point2D sceneFocal = new Point2D(event.getSceneX(), event.getSceneY());
            Point2D imageAnchor = imageView.sceneToLocal(sceneFocal);

            applyZoomWithAnchor(newZoom, sceneFocal, imageAnchor);
        });
    }

    private void captureViewportCenterAnchor() {
        Bounds viewportBounds = scrollPane.getViewportBounds();
        double centerX = viewportBounds.getMinX() + viewportBounds.getWidth() / 2;
        double centerY = viewportBounds.getMinY() + viewportBounds.getHeight() / 2;
        sliderZoomSceneFocal = scrollPane.localToScene(centerX, centerY);
        sliderZoomImageAnchor = imageView.sceneToLocal(sliderZoomSceneFocal);
    }

    private void clearSliderZoomAnchor() {
        sliderZoomSceneFocal = null;
        sliderZoomImageAnchor = null;
    }

    private void applyZoomWithAnchor(double newZoom, Point2D sceneFocal, Point2D imageAnchor) {
        if (sceneFocal == null || imageAnchor == null) {
            return;
        }

        suppressZoomListener = true;
        slider.setValue(newZoom);
        suppressZoomListener = false;
        
        updateImageContainerSize(newZoom);

        Platform.runLater(() -> {
            Point2D newScenePos = imageView.localToScene(imageAnchor);
            double deltaX = newScenePos.getX() - sceneFocal.getX();
            double deltaY = newScenePos.getY() - sceneFocal.getY();

            Bounds viewport = scrollPane.getViewportBounds();
            double newContentW = imageView.getFitWidth() * newZoom;
            double newContentH = imageView.getFitHeight() * newZoom;
            
            double scrollableWidth = Math.max(newContentW - viewport.getWidth(), 0);
            double scrollableHeight = Math.max(newContentH - viewport.getHeight(), 0);

            if (scrollableWidth > 0) {
                double hVal = scrollPane.getHvalue() + deltaX / scrollableWidth;
                scrollPane.setHvalue(Math.clamp(hVal, 0, 1));
            } else {
                scrollPane.setHvalue(0.5);
            }
            
            if (scrollableHeight > 0) {
                double vVal = scrollPane.getVvalue() + deltaY / scrollableHeight;
                scrollPane.setVvalue(Math.clamp(vVal, 0, 1));
            } else {
                scrollPane.setVvalue(0.5);
            }
        });
    }

    public void updateImageContainerSize(double zoom) {
        double newWidth = imageView.getFitWidth() * zoom;
        double newHeight = imageView.getFitHeight() * zoom;

        container.setMinWidth(newWidth);
        container.setMinHeight(newHeight);
        container.setPrefWidth(newWidth);
        container.setPrefHeight(newHeight);
    }

    public void resetZoom() {
        suppressZoomListener = true;
        slider.setValue(1.0);
        suppressZoomListener = false;
        clearSliderZoomAnchor();
        scrollPane.setHvalue(0);
        scrollPane.setVvalue(0);
        resetPreviewContainerSize();
    }

    private void resetPreviewContainerSize() {
        container.setMinWidth(Region.USE_PREF_SIZE);
        container.setMinHeight(200.0);
        container.setPrefWidth(Region.USE_COMPUTED_SIZE);
        container.setPrefHeight(Region.USE_COMPUTED_SIZE);
    }
}
