package model.converterImage.strategy;

import java.io.File;
import java.io.IOException;

public interface ImageConversionStrategy {
    File convert(File source, File destinationDir, String format) throws IOException;
}
