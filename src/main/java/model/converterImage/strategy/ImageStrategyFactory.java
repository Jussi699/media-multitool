package model.converterImage.strategy;

import java.io.File;
import model.converterImage.UsefulMethods;

public class ImageStrategyFactory {
    public static ImageConversionStrategy getStrategy(File source, String targetFormat) {
        String inputExt = UsefulMethods.getFileExtension(source).toLowerCase();
        String outputExt = targetFormat.toLowerCase();

        if ("svg".equals(outputExt)) {
            return new SvgImageStrategy();
        }
        if ("ico".equals(outputExt) || "ico".equals(inputExt)) {
            return new IcoImageStrategy();
        }
        return new RasterImageStrategy();
    }
}
