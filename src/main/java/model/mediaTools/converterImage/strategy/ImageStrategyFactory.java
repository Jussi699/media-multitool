package model.mediaTools.converterImage.strategy;

import java.io.File;

import static model.utility.io.FileUtils.getFileExtension;

public class ImageStrategyFactory {
    public static ImageConversionStrategy getStrategy(File source, String targetFormat) {
        String inputExt = getFileExtension(source).toLowerCase();
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
