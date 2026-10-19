package model.helper.watermarks;

import lombok.NonNull;
import model.utility.media.ImageFormatDetector;
import net.ifok.image.image4j.codec.ico.ICODecoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.List;

import static model.utility.io.FileHelper.getLargestImage;

public class WatermarkLoadAndSaveHelper {
    private WatermarkLoadAndSaveHelper() {
        /* This utility class should not be instantiated */
    }

    public static BufferedImage loadSvgAsBufferedImage(@NonNull File svgFile) throws TranscoderException, IOException {
        PNGTranscoder transcoder = new PNGTranscoder();
        try (InputStream in = Files.newInputStream(svgFile.toPath())) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            transcoder.transcode(new TranscoderInput(in), new TranscoderOutput(baos));
            byte[] pngBytes = baos.toByteArray();
            try (ByteArrayInputStream bais = new ByteArrayInputStream(pngBytes)) {
                return ImageIO.read(bais);
            }
        }
    }

    public static BufferedImage determinedAndLoadTypeAsBufferedImage(@NonNull File file) throws TranscoderException, IOException {
        String ext = ImageFormatDetector.getExtensionByString(file.getName());
        BufferedImage bufferedImage;

        if ("svg".equalsIgnoreCase(ext)) {
            bufferedImage = WatermarkLoadAndSaveHelper.loadSvgAsBufferedImage(file);
        } else if ("ico".equalsIgnoreCase(ext)) {
            List<BufferedImage> images = ICODecoder.read(file);
            bufferedImage = images.isEmpty() ? null : getLargestImage(images);
        } else {
            bufferedImage = ImageIO.read(file);
        }

        return bufferedImage;
    }
}
