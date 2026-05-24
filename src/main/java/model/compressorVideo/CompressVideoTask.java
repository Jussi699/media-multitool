package model.compressorVideo;

import javafx.concurrent.Task;
import java.io.File;
import java.util.UUID;
import model.utility.DetermineType;

public class CompressVideoTask extends Task<Boolean> {
    private final Compressor compressor;
    private final File srcFile;
    private final File outputDir;
    private final VideoPresets.Preset selectedPreset;

    public CompressVideoTask(Compressor compressor, File srcFile, File outputDir, VideoPresets.Preset selectedPreset) {
        this.compressor = compressor;
        this.srcFile = srcFile;
        this.outputDir = outputDir;
        this.selectedPreset = selectedPreset;
    }

    @Override
    protected Boolean call() throws Exception {
        File finalFileOutput = new File(outputDir, srcFile.getName() + UUID.randomUUID().toString().replace("-", "") +
                "." + DetermineType.determineFormat(srcFile).orElse("mp4"));

        compressor.compress(srcFile, finalFileOutput,
                selectedPreset.video(), selectedPreset.audio(), p -> updateProgress(p, 1.0));

        return finalFileOutput.exists() && finalFileOutput.length() > 0;
    }
    
    public void cancelCompress() {
        compressor.cancelCompress();
        cancel();
    }
}
