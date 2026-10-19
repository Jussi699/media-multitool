package model.mediaTools.compressorImage;

import java.io.File;

public record CompressionResult(
        File outputFile,
        String format,
        long originalSizeBytes,
        long compressedSizeBytes,
        boolean sizeReduced
) {
    public double savedPercent() {
        if (originalSizeBytes <= 0 || !sizeReduced) {
            return 0.0;
        }

        return ((double) (originalSizeBytes - compressedSizeBytes) / originalSizeBytes) * 100.0;
    }
}
