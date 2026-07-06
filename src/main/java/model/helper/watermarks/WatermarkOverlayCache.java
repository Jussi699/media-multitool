package model.helper.watermarks;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Pre-renders the watermark onto a transparent ARGB canvas the size of the video frame.
 * This overlay is then composited onto each BGR video frame directly, avoiding any
 * pixel-format conversion that would swap the red and blue channels.
 */
public record WatermarkOverlayCache(BufferedImage overlay) {

    /**
     * Build the overlay for the given video dimensions.
     * Returns an instance with a null overlay if there is nothing to render.
     */
    public static WatermarkOverlayCache build(WatermarkSettings settings, int width, int height) {
        if (settings == null || settings.getType() == WatermarkSettings.WatermarkType.NONE
                || width <= 0 || height <= 0) {
            return new WatermarkOverlayCache(null);
        }

        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        WatermarkRenderer.applyWatermarkInPlace(canvas, settings);
        return new WatermarkOverlayCache(canvas);
    }
}
