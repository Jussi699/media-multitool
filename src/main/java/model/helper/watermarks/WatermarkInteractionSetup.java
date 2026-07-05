package model.helper.watermarks;

import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

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
        this.onUpdate = onUpdate;
        this.onComplete = onComplete;
    }

    /**
     * Wire up all interaction: overlay elements, listeners, drag and resize handlers.
     * Safe to call even when the view is not yet fully initialised (guards are in place).
     */
    public void setup() {
        if (imageView == null || previewContainer == null) {
            return;
        }

        imageView.setPickOnBounds(true);

        if (watermarkOverlayPane != null) {
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
        if (watermarkOverlayPane != null) {
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
        if (img != null && s.getType() != WatermarkSettings.WatermarkType.NONE) {
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
            dragHandler.setContext(imageSupplier.get(), settingsSupplier.get());
            dragHandler.handleMousePressed(event);
        });
        imageView.setOnMouseDragged(dragHandler::handleMouseDragged);
        imageView.setOnMouseReleased(dragHandler::handleMouseReleased);
        imageView.setOnMouseMoved(dragHandler::handleMouseMoved);
        imageView.setOnMouseClicked(event -> {
            dragHandler.setContext(imageSupplier.get(), settingsSupplier.get());
            dragHandler.handleMouseClicked(event);
        });

        attachAllResizeHandlers();
    }

    /** Attach resize mouse events to all four corner handles. */
    private void attachAllResizeHandlers() {
        for (WatermarkOverlayManager.HandlePosition pos : WatermarkOverlayManager.HandlePosition.values()) {
            javafx.scene.shape.Rectangle handle = overlayManager.getHandle(pos);
            if (handle != null) {
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
