package model.properties;

import javafx.animation.PauseTransition;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import model.enums.TypeMedia;

import java.io.File;

@Getter
@Setter
public class VideoAndAudioProperties implements MediaProperties {
    private String resolution;
    private String targetFormat;
    private int fps;
    private int videoBitRate;
    private int audioBitRate;
    private int channel;
    private int samplingRate;
    private int seconds = 5;
    private File output;
    private File srcFile;
    private File pathToImage;
    private boolean useGPU;
    private boolean isReverb;
    private String videoCodec;
    private String audioCodec;
    private String ffmpegFormat;
    private TypeMedia typeConvert;
    private final PauseTransition hideSuccessMessageTimer = new PauseTransition(Duration.seconds(seconds));

    @Override
    public PauseTransition getHideSuccessMessageTimer() {
        return hideSuccessMessageTimer;
    }

    @Override
    public void reset() {
        this.srcFile = null;
        this.targetFormat = null;
        this.resolution = null;
        this.fps = 0;
        this.videoBitRate = 0;
        this.channel = 0;
        this.samplingRate = 0;
        this.isReverb = false;
        this.audioBitRate = 0;
        this.videoCodec = null;
        this.audioCodec = null;
        this.ffmpegFormat = null;
        this.typeConvert = null;
        this.useGPU = false;
    }
}
