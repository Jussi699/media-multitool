package model.converterImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class DetermineType {
    public static String determineType(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File is null");
        }

        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            if (iis == null) {
                throw new IOException("Unable to create image input stream");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(iis, true, true);
                    return reader.getFormatName();
                } finally {
                    reader.dispose();
                }
            }

            throw new IOException("Unknown image format");
        }
    }
}
