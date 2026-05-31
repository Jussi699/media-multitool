package model.converterVideo;

import model.compressorVideo.Compressor;
import model.compressorVideo.VideoPresets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class VideoOperationsTest {

    private final String PATH_TO_VIDEO = "E:\\test\\srcVideo\\MP4.MP4";

    @TempDir
    Path tempDir;

    @Test
    public void testVideoConversion() throws Exception {
        File source = new File(PATH_TO_VIDEO);
        ConverterVideoAudioFile converter = new ConverterVideoAudioFile();
        
        boolean success = converter.convert(
                source,
                tempDir.toFile(),
                2000, 128, 2, 44100, 30,
                "libx264", "aac", "mp4", "1280x720", "video",
                _ -> {}
        );
        
        assertTrue(success);
        File result = converter.nameFileAfter;
        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    public void testVideoCompression() throws Exception {
        File source = new File(PATH_TO_VIDEO);
        Compressor compressor = new Compressor();
        
        File output = new File(tempDir.toFile(), "compressed_" + UUID.randomUUID() + ".mp4");
        
        VideoPresets.Preset[] presets = VideoPresets.createAdaptivePresets(source).orElseThrow();
        VideoPresets.Preset selected = presets[0];
        
        compressor.compress(source, output, selected.video(), selected.audio(), _ -> {});
        
        assertTrue(output.exists());
        assertTrue(output.length() > 0);
    }
}
