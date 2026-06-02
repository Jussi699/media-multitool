package model.converterVideo;

import javafx.concurrent.Task;
import java.io.File;

public class ConvertVideoAudioTask extends Task<Boolean> {
    private final ConverterVideoAudioFile converter;
    private final File file;
    private final File outputDir;
    private final int videoBitrate;
    private final int audioBitrate;
    private final int channels;
    private final int samplingRate;
    private final int fps;
    private final String videoCodec;
    private final String audioCodec;
    private final String outputFormat;
    private final String resolution;
    private final String typeConvert;

    public ConvertVideoAudioTask(ConverterVideoAudioFile converter, File file, File outputDir, 
                                 int videoBitrate, int audioBitrate, int channels, int samplingRate, int fps, 
                                 String videoCodec, String audioCodec, String outputFormat, String resolution, String typeConvert) {
        this.converter = converter;
        this.file = file;
        this.outputDir = outputDir;
        this.videoBitrate = videoBitrate;
        this.audioBitrate = audioBitrate;
        this.channels = channels;
        this.samplingRate = samplingRate;
        this.fps = fps;
        this.videoCodec = videoCodec;
        this.audioCodec = audioCodec;
        this.outputFormat = outputFormat;
        this.resolution = resolution;
        this.typeConvert = typeConvert;
    }

    @Override
    protected Boolean call() throws Exception {
        return converter.convert(file, outputDir, videoBitrate, audioBitrate, channels, samplingRate, fps, 
                                 videoCodec, audioCodec, outputFormat, resolution, typeConvert, 
                                 p -> updateProgress(p, 1.0));
    }
    
    public void cancelConversion() {
        cancel();
        converter.cancelConversion();
    }
}
