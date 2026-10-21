package services;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

public class VideoTestService {
    public static File createTestVideo(File outputFile, int width, int height, int frameCount, int frameRate) throws Exception {
        try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, width, height)) {
            recorder.setFormat("mp4");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
            recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
            recorder.setFrameRate(frameRate);
            recorder.start();

            try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
                for (int i = 0; i < frameCount; i++) {
                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                    Graphics2D graphics = image.createGraphics();
                    graphics.setColor(i % 2 == 0 ? Color.RED : Color.BLUE);
                    graphics.fillRect(0, 0, width, height);
                    graphics.setColor(Color.WHITE);
                    graphics.drawString("Frame " + i, Math.max(2, width / 6), height / 2);
                    graphics.dispose();
                    recorder.record(converter.convert(image));
                }
            } finally {
                recorder.stop();
            }
        }
        return outputFile;
    }

    /**
     * Создает тестовое видео со стандартными параметрами (128x128, 12 кадров, 6 fps).
     */
    public static File createTestVideo(File outputFile) throws Exception {
        return createTestVideo(outputFile, 128, 128, 12, 6);
    }

    /**
     * Создает тестовое видео в указанной директории (например, JUnit @TempDir).
     */
    public static File createTestVideo(Path directory, String fileName) throws Exception {
        return createTestVideo(directory.resolve(fileName).toFile());
    }
}
