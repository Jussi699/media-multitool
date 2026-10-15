package model.imageTools;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

public class ImageToolsHelper {
    private ImageToolsHelper() {
        /* This utility class should not be instantiated */
    }

    /**
     * Creates a deep copy of the specified image.
     *
     * @param image the {@link BufferedImage} to copy
     * @return a new {@link BufferedImage} containing a copy of the input image
     */
    protected static BufferedImage copyImage(BufferedImage image) {
        int imageType = image.getType() == BufferedImage.TYPE_CUSTOM
                ? BufferedImage.TYPE_INT_ARGB
                : image.getType();
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), imageType);
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return copy;
    }

    /**
     * Converts or copies the specified image into a standard {@link BufferedImage#TYPE_INT_ARGB} format.
     *
     * @param image the input {@link BufferedImage}
     * @return a new {@link BufferedImage} in ARGB format
     */
    protected static BufferedImage normalizeToArgb(BufferedImage image) {
        BufferedImage normalized = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = normalized.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return normalized;
    }

    /**
     * Retrieves the underlying ARGB pixel array from a {@link BufferedImage}'s raster data buffer.
     *
     * @param image the {@link BufferedImage} whose raster is backed by a {@link DataBufferInt}
     * @return an integer array containing the ARGB pixel values
     */
    protected static int[] getPixels(BufferedImage image) {
        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    /**
     * Executes a single horizontal and vertical box-blur pass across the pixel array using integral images.
     *
     * @param pixels the target pixel array to blur in-place
     * @param width  the width of the image
     * @param height the height of the image
     * @param radius the blur radius in pixels
     */
    protected static void applyBoxBlurPass(int[] pixels, int width, int height, int radius) {
        IntegralImages integralImages = buildIntegralImages(pixels, width, height);
        int stride = width + 1;
        int[] blurredPixels = Arrays.copyOf(pixels, pixels.length);

        for (int y = 0; y < height; y++) {
            int y1 = Math.max(0, y - radius);
            int y2 = Math.min(height, y + radius + 1);
            for (int x = 0; x < width; x++) {
                int x1 = Math.max(0, x - radius);
                int x2 = Math.min(width, x + radius + 1);
                long area = (long) (x2 - x1) * (y2 - y1);

                long sumA = getRegionSum(integralImages.alpha(), stride, x1, y1, x2, y2);
                long sumR = getRegionSum(integralImages.red(), stride, x1, y1, x2, y2);
                long sumG = getRegionSum(integralImages.green(), stride, x1, y1, x2, y2);
                long sumB = getRegionSum(integralImages.blue(), stride, x1, y1, x2, y2);

                blurredPixels[y * width + x] = ((int) (sumA / area) << 24)
                        | ((int) (sumR / area) << 16)
                        | ((int) (sumG / area) << 8)
                        | (int) (sumB / area);
            }
        }

        System.arraycopy(blurredPixels, 0, pixels, 0, pixels.length);
    }

    /**
     * Builds summed-area tables (integral images) for the alpha, red, green, and blue color channels.
     *
     * @param pixels the source pixel array in ARGB format
     * @param width  the width of the image
     * @param height the height of the image
     * @return an {@link IntegralImages} instance containing the integral image tables for all channels
     */
    protected static IntegralImages buildIntegralImages(int[] pixels, int width, int height) {
        int stride = width + 1;
        long[] alpha = new long[stride * (height + 1)];
        long[] red = new long[stride * (height + 1)];
        long[] green = new long[stride * (height + 1)];
        long[] blue = new long[stride * (height + 1)];

        for (int y = 0; y < height; y++) {
            long rowA = 0;
            long rowR = 0;
            long rowG = 0;
            long rowB = 0;
            for (int x = 0; x < width; x++) {
                int argb = pixels[y * width + x];
                rowA += (argb >> 24) & 0xFF;
                rowR += (argb >> 16) & 0xFF;
                rowG += (argb >> 8) & 0xFF;
                rowB += argb & 0xFF;

                int index = (y + 1) * stride + (x + 1);
                int above = y * stride + (x + 1);
                alpha[index] = rowA + alpha[above];
                red[index] = rowR + red[above];
                green[index] = rowG + green[above];
                blue[index] = rowB + blue[above];
            }
        }

        return new IntegralImages(alpha, red, green, blue);
    }

    /**
     * Computes the sum of values within a rectangular region in constant O(1) time using an integral image table.
     *
     * @param integralImage the summed-area table array
     * @param stride        the row stride (width + 1) of the integral image
     * @param x1            the left coordinate (inclusive)
     * @param y1            the top coordinate (inclusive)
     * @param x2            the right coordinate (exclusive)
     * @param y2            the bottom coordinate (exclusive)
     * @return the sum of values inside the rectangular region
     */
    protected static long getRegionSum(long[] integralImage, int stride,
                                     int x1, int y1, int x2, int y2) {
        return integralImage[y2 * stride + x2]
                - integralImage[y1 * stride + x2]
                - integralImage[y2 * stride + x1]
                + integralImage[y1 * stride + x1];
    }
}
