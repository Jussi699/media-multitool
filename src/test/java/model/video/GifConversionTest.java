package model.video;

import model.enums.MediaType;
import model.mediaTools.converterVideo.ConverterVideoAudioFile;
import model.properties.VideoAndAudioProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static services.VideoTestService.createTestVideo;

class GifConversionTest {
    @TempDir
    Path tempDir;

    @Test
    void convertsVideoToAnimatedGif() throws Exception {
        File source = tempDir.resolve("source.mp4").toFile();
        createTestVideo(source, 32, 32, 4, 4);

        VideoAndAudioProperties properties = new VideoAndAudioProperties();
        properties.setSrcFile(source);
        properties.setOutput(tempDir.toFile());
        properties.setFfmpegFormat("gif");
        properties.setVideoCodec("gif");
        properties.setVideoBitRate(0);
        properties.setFps(4);
        properties.setResolution("32x32");

        ConverterVideoAudioFile converter = new ConverterVideoAudioFile();
        assertTrue(converter.convert(properties, MediaType.VIDEO, _ -> {}));
        assertTrue(converter.nameFileAfter.isFile());
        assertTrue(hasMultipleFrames(converter.nameFileAfter));

        VideoAndAudioProperties reverseProperties = new VideoAndAudioProperties();
        reverseProperties.setSrcFile(converter.nameFileAfter);
        reverseProperties.setOutput(tempDir.toFile());
        reverseProperties.setFfmpegFormat("mp4");
        reverseProperties.setVideoCodec("mpeg4");
        reverseProperties.setAudioCodec("aac");
        reverseProperties.setVideoBitRate(1000);
        reverseProperties.setFps(4);
        reverseProperties.setResolution("32x32");

        ConverterVideoAudioFile reverseConverter = new ConverterVideoAudioFile();
        assertTrue(reverseConverter.convert(reverseProperties, MediaType.VIDEO, _ -> {}));
        assertTrue(reverseConverter.nameFileAfter.isFile());
        assertTrue(reverseConverter.nameFileAfter.length() > 0);
    }

    private boolean hasMultipleFrames(File file) throws Exception {
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return false;
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                return reader.getNumImages(true) > 1;
            } finally {
                reader.dispose();
            }
        }
    }
}
