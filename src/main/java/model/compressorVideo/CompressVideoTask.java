package model.compressorVideo;

import javafx.concurrent.Task;
import java.io.File;
import model.utility.DetermineType;
import model.utility.PathWorker;

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
        updateProgress(0, 1);

        String format = DetermineType.determineFormat(srcFile).orElse("mp4");
        File finalFileOutput = PathWorker.createOutputFile(srcFile, outputDir, format);

        if (isCancelled()) {
            return false;
        }

        compressor.compress(srcFile, finalFileOutput,
                selectedPreset.video(), selectedPreset.audio(), selectedPreset.crf(), p -> updateProgress(p, 1.0));

        return finalFileOutput.exists() && finalFileOutput.length() > 0;
    }
    
    public void cancelCompress() {
        compressor.cancelCompress();
        cancel();
    }
}
