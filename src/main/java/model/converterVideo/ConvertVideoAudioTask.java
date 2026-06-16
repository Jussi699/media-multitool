package model.converterVideo;

import javafx.concurrent.Task;
import model.enums.TypeMedia;
import model.properties.VideoAndAudioProperties;

public class ConvertVideoAudioTask extends Task<Boolean> {
    private final ConverterVideoAudioFile converter;
    private final VideoAndAudioProperties videoAndAudioProperties;
    private final TypeMedia typeConvert;

    public ConvertVideoAudioTask(ConverterVideoAudioFile converter, VideoAndAudioProperties videoAndAudioProperties, TypeMedia typeConvert) {
        this.converter = converter;
        this.typeConvert = typeConvert;
        this.videoAndAudioProperties = videoAndAudioProperties;
    }

    @Override
    protected Boolean call() throws Exception {
        return converter.convert(videoAndAudioProperties, typeConvert , p -> updateProgress(p, 1.0));
    }
    
    public void cancelConversion() {
        cancel();
        converter.cancelConversion();
    }
}
