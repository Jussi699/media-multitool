package model.preprocessing;

import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhotoAndVideoTagPreprocessingTest {
    @TempDir
    Path tempDir;

    @Test
    void writesAndReadsJpegTags() throws Exception {
        File image = createImage("photo.jpg", "jpg");
        Map<String, String> tags = Map.of(
                "title", "Test title",
                "artist", "Test artist",
                "comment", "Test comment",
                "description", "Test description",
                "keywords", "test, image",
                "copyright", "Test copyright",
                "rating", "4"
        );

        PhotoTagPreprocessing.applyTags(image, tags);

        assertEquals(tags, PhotoTagPreprocessing.getTags(image));
    }

    @Test
    void writesAndReadsTiffTags() throws Exception {
        File image = createImage("photo.tiff", "tiff");
        Map<String, String> tags = Map.of(
                "title", "Test title",
                "artist", "Test artist",
                "description", "Test description"
        );

        PhotoTagPreprocessing.applyTags(image, tags);

        assertEquals(tags, PhotoTagPreprocessing.getTags(image));
    }

    @Test
    void writesVideoTagsWithoutReencoding() throws Exception {
        File video = createVideo();
        Map<String, String> tags = Map.of("title", "Test title", "artist", "Test artist");

        VideoTagPreprocessing.applyTags(video, tags);

        assertEquals(tags, VideoTagPreprocessing.getTags(video));
    }

    private File createImage(String fileName, String format) throws Exception {
        File image = tempDir.resolve(fileName).toFile();
        BufferedImage bufferedImage = new BufferedImage(24, 24, BufferedImage.TYPE_INT_RGB);
        assertTrue(javax.imageio.ImageIO.write(bufferedImage, format, image));
        return image;
    }

    private File createVideo() throws Exception {
        File video = tempDir.resolve("video.mp4").toFile();
        try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(video, 32, 32);
            recorder.setFormat("mp4");
            recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MPEG4);
            recorder.setFrameRate(1);
            recorder.start();
            recorder.record(converter.convert(new BufferedImage(32, 32, BufferedImage.TYPE_3BYTE_BGR)));
            recorder.stop();
            recorder.release();
        }
        return video;
    }
}
