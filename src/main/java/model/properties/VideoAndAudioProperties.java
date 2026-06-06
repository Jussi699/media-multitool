package model.properties;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.io.File;

public class VideoAndAudioProperties implements MediaProperties {
    private String resolution;
    private String targetFormat;
    private int fps;
    private int bitRate;
    private int channel;
    private int samplingRate;
    private int seconds = 5;
    private File output;
    private File srcFile;
    private File pathToImage;
    private final PauseTransition hideSuccessMessageTimer = new PauseTransition(Duration.seconds(seconds));


    @Override
    public void reset() {
        this.srcFile = null;
        this.targetFormat = null;
        this.resolution = null;
        this.fps = 0;
        this.bitRate = 0;
        this.channel = 0;
        this.samplingRate = 0;
    }

    public File getPathToImage() {
        return pathToImage;
    }

    public void setPathToImage(File pathToImage) {
        this.pathToImage = pathToImage;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getSamplingRate() {
        return samplingRate;
    }

    public void setSamplingRate(int samplingRate) {
        this.samplingRate = samplingRate;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public File getOutput() {
        return output;
    }

    public void setOutput(File output) {
        this.output = output;
    }

    public File getSrcFile() {
        return srcFile;
    }

    public void setSrcFile(File srcFile) {
        this.srcFile = srcFile;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(String targetFormat) {
        this.targetFormat = targetFormat;
    }

    public PauseTransition getHideSuccessMessageTimer() {
        return hideSuccessMessageTimer;
    }
}
