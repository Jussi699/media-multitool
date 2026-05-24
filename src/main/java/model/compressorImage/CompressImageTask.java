package model.compressorImage;

import javafx.concurrent.Task;
import model.properties.ImageProperties;
import java.util.Optional;

public class CompressImageTask extends Task<Optional<CompressionResult>> {
    private final Compressor compressor;
    private final ImageProperties properties;
    private final boolean isSvg;

    public CompressImageTask(Compressor compressor, ImageProperties properties, boolean isSvg) {
        this.compressor = compressor;
        this.properties = properties;
        this.isSvg = isSvg;
    }

    @Override
    protected Optional<CompressionResult> call() throws Exception {
        updateProgress(0, 1);
        Optional<CompressionResult> result;
        if (isSvg) {
            result = compressor.removeSvgMetadata(properties);
        } else {
            result = compressor.compressorStandardImage(properties);
        }
        updateProgress(1, 1);
        return result;
    }
}
