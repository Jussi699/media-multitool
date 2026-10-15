package model.converterVideo;

import model.enums.MediaType;
import model.properties.VideoAndAudioProperties;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GifConversionTest {
    @TempDir
    Path tempDir;

    @Test
    void convertsVideoToAnimatedGif() throws Exception {
        File source = tempDir.resolve("source.mp4").toFile();
        createTestVideo(source);

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

    private void createTestVideo(File file) throws Exception {
        try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(file, 32, 32)) {
            recorder.setFormat("mp4");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
            recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
            recorder.setFrameRate(4);
            recorder.start();

            try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
                for (int i = 0; i < 4; i++) {
                    BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_3BYTE_BGR);
                    Graphics2D graphics = image.createGraphics();
                    graphics.setColor(i % 2 == 0 ? Color.RED : Color.BLUE);
                    graphics.fillRect(0, 0, 32, 32);
                    graphics.dispose();
                    recorder.record(converter.convert(image));
                }
            } finally {
                recorder.stop();
            }
        }
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
