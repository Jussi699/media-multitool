package model.preprocessing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import services.ImageTestService;
import services.VideoTestService;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhotoAndVideoTagPreprocessingTest {
    @TempDir
    Path tempDir;

    @Test
    void writesAndReadsJpegTags() throws Exception {
        File image = ImageTestService.createJpg(tempDir, "photo.jpg");
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
        File image = ImageTestService.createTiff(tempDir, "photo.tiff");
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
        File video = VideoTestService.createTestVideo(tempDir, "video.mp4");
        Map<String, String> tags = Map.of("title", "Test title", "artist", "Test artist", "year", "2024");

        VideoTagPreprocessing.applyTags(video, tags);

        assertEquals(tags, VideoTagPreprocessing.getTags(video));
    }
}
