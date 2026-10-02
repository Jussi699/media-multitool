package model.helper.watermarks;

import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Encapsulates all watermark interaction setup logic (mouse events, drag, resize, overlay
 * listeners) that is shared between WatermarkImageController and WatermarkPdfController.
 *
 * <p>The caller supplies:
 * <ul>
 *   <li>{@code imageSupplier} – a lambda that returns the current source image (may be null before a
 *       file is loaded).</li>
 *   <li>{@code settingsSupplier} – a lambda that returns the current {@link WatermarkSettings}.</li>
 *   <li>{@code onUpdate} – called whenever the preview should be refreshed.</li>
 *   <li>{@code onComplete} – called when a drag / resize gesture ends (e.g. to sync sub-windows).</li>
 * </ul>
 */
public class WatermarkInteractionSetup {

    private final ImageView imageView;
    private final StackPane previewContainer;
    private final Pane watermarkOverlayPane;

    private final WatermarkDragHandler dragHandler;
    private final WatermarkResizeHandler resizeHandler;
    private final WatermarkOverlayManager overlayManager;

    private final Supplier<BufferedImage> imageSupplier;
    private final Supplier<WatermarkSettings> settingsSupplier;
    private final Supplier<List<WatermarkSettings>> settingsListSupplier;
    private final Consumer<Integer> selectionCallback;

    private final Runnable onUpdate;
    private final Runnable onComplete;

    public WatermarkInteractionSetup(
            ImageView imageView,
            StackPane previewContainer,
            Pane watermarkOverlayPane,
            WatermarkDragHandler dragHandler,
            WatermarkResizeHandler resizeHandler,
            WatermarkOverlayManager overlayManager,
            Supplier<BufferedImage> imageSupplier,
            Supplier<WatermarkSettings> settingsSupplier,
            Supplier<List<WatermarkSettings>> settingsListSupplier,
            Consumer<Integer> selectionCallback,
            Runnable onUpdate,
            Runnable onComplete
    ) {
        this.imageView = imageView;
        this.previewContainer = previewContainer;
        this.watermarkOverlayPane = watermarkOverlayPane;
        this.dragHandler = dragHandler;
        this.resizeHandler = resizeHandler;
        this.overlayManager = overlayManager;
        this.imageSupplier = imageSupplier;
        this.settingsSupplier = settingsSupplier;
        this.settingsListSupplier = settingsListSupplier;
        this.selectionCallback = selectionCallback;
        this.onUpdate = onUpdate;
        this.onComplete = onComplete;
    }

    /**
     * Wire up all interaction: overlay elements, listeners, drag and resize handlers.
     * Safe to call even when the view is not yet fully initialised (guards are in place).
     */
    public void setup() {
        if (isNull(imageView) || isNull(previewContainer)) {
            return;
        }

        imageView.setPickOnBounds(true);

        if (nonNull(watermarkOverlayPane)) {
            overlayManager.buildOverlayElements();
        }

        initContainerListeners();
        setupHandleCallbacks();
        setupMouseEvents();
    }

    /**
     * Rebuild overlay elements and re-attach all resize handlers.
     * Call this after a new file has been loaded.
     */
    public void rebuildOverlay() {
        if (nonNull(watermarkOverlayPane)) {
            overlayManager.buildOverlayElements();
            attachAllResizeHandlers();
        }
    }

    /** Listen for container / imageView size changes and update the overlay accordingly. */
    private void initContainerListeners() {
        previewContainer.widthProperty().addListener((_, _, _) -> conditionalOverlayUpdate());
        previewContainer.heightProperty().addListener((_, _, _) -> conditionalOverlayUpdate());
        imageView.fitWidthProperty().addListener((_, _, _) -> conditionalOverlayUpdate());
        imageView.fitHeightProperty().addListener((_, _, _) -> conditionalOverlayUpdate());
    }

    private void conditionalOverlayUpdate() {
        BufferedImage img = imageSupplier.get();
        WatermarkSettings s = settingsSupplier.get();
        if (nonNull(img) && s.getType() != WatermarkSettings.WatermarkType.NONE) {
            onUpdate.run();
        }
    }

    /** Wire the update / complete callbacks into drag and resize handlers. */
    private void setupHandleCallbacks() {
        dragHandler.setOnUpdate(settings -> {
            dragHandler.setContext(imageSupplier.get(), settings);
            onUpdate.run();
        });
        dragHandler.setOnDragComplete(onComplete);

        resizeHandler.setOnUpdate(settings -> {
            resizeHandler.setContext(imageSupplier.get(), settings);
            onUpdate.run();
        });
        resizeHandler.setOnResizeComplete(onComplete);
    }

    /** Attach mouse-pressed / dragged / released / moved / clicked to the imageView. */
    private void setupMouseEvents() {
        imageView.setOnMousePressed(event -> {
            BufferedImage image = imageSupplier.get();
            if (nonNull(image) && imageView.getBoundsInLocal().getWidth() > 0) {
                double scaleX = image.getWidth() / imageView.getBoundsInLocal().getWidth();
                double scaleY = image.getHeight() / imageView.getBoundsInLocal().getHeight();
                double x = event.getX() * scaleX;
                double y = event.getY() * scaleY;
                List<WatermarkSettings> allSettings = settingsListSupplier.get();
                for (int i = allSettings.size() - 1; i >= 0; i--) {
                    WatermarkSettings candidate = allSettings.get(i);
                    if (!WatermarkDimensionsHelper.canDrag(candidate, image)) {
                        continue;
                    }
                    double[] bounds = WatermarkDimensionsHelper.getCurrentPosition(candidate, image);
                    if (x >= bounds[0] && x <= bounds[0] + bounds[2]
                            && y >= bounds[1] && y <= bounds[1] + bounds[3]) {
                        selectionCallback.accept(i);
                        break;
                    }
                }
            }
            dragHandler.setContext(image, settingsSupplier.get());
            dragHandler.handleMousePressed(event);
        });
        imageView.setOnMouseDragged(dragHandler::handleMouseDragged);
        imageView.setOnMouseReleased(dragHandler::handleMouseReleased);
        imageView.setOnMouseMoved(dragHandler::handleMouseMoved);
        attachAllResizeHandlers();
    }

    /** Attach resize mouse events to all edge and corner handles. */
    private void attachAllResizeHandlers() {
        for (WatermarkOverlayManager.HandlePosition pos : WatermarkOverlayManager.HandlePosition.values()) {
            javafx.scene.shape.Rectangle handle = overlayManager.getHandle(pos);
            if (nonNull(handle)) {
                attachResizeHandler(handle, pos.name());
            }
        }
    }

    private void attachResizeHandler(javafx.scene.shape.Rectangle handle, String handleId) {
        handle.setOnMousePressed(event -> {
            resizeHandler.setContext(imageSupplier.get(), settingsSupplier.get());
            resizeHandler.handleMousePressed(event, handleId);
        });
        handle.setOnMouseDragged(event -> {
            resizeHandler.setContext(imageSupplier.get(), settingsSupplier.get());
            resizeHandler.handleMouseDragged(event);
        });
        handle.setOnMouseReleased(resizeHandler::handleMouseReleased);
    }
}
