package model.converterVideo;

import model.enums.MediaType;
import model.logger.ErrorLogger;
import model.properties.VideoAndAudioProperties;
import model.utility.EncoderUtility;
import model.utility.PreparingAttributes;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.progress.EncoderProgressListener;

import java.io.File;
import java.util.UUID;
import java.util.function.DoubleConsumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class ConverterVideoAudioFile {
    private final Encoder encoder = new Encoder();
    private volatile File currentTarget;

    public File nameFileAfter;

    public boolean convert(VideoAndAudioProperties properties, MediaType typeConvert, DoubleConsumer progressConsumer) {
        File file = properties.getSrcFile();
        if (!checkingFile(file)) {
            return false;
        }

        File target = prepareTargetFile(file, properties.getOutput(), properties.getFfmpegFormat());
        currentTarget = target;
        nameFileAfter = target;

        ErrorLogger.info(ConverterVideoAudioFile.class, "Starting conversion...");
        try {
            MultimediaObject multimediaObject = new MultimediaObject(file);
            MultimediaInfo sourceInfo = multimediaObject.getInfo();

            EncodingAttributes attrs = createEncodingAttributes(properties, typeConvert, sourceInfo);

            ErrorLogger.info(ConverterVideoAudioFile.class, "Starting encoding: " + file.getName() + " [V-BR: " + properties.getVideoBitRate() + ", A-BR: " + properties.getAudioBitRate() + "]");

            encoder.encode(multimediaObject, target, attrs, new EncoderProgressListener() {
                @Override
                public void sourceInfo(MultimediaInfo info) {
                    ErrorLogger.info(ConverterVideoAudioFile.class, "Source info: " + info);
                }

                @Override
                public void progress(int permille) {
                    if (nonNull(progressConsumer)) {
                        progressConsumer.accept(permille / 1000.0);
                    }
                }

                @Override
                public void message(String message) {
                    ErrorLogger.info(ConverterVideoAudioFile.class, "FFmpeg: " + message);
                }
            });

            ErrorLogger.info(ConverterVideoAudioFile.class, "Conversion successful!");
            clearCurrentTarget(target);
            return true;
        } catch (Exception e) {
            handleError(e);
            clearCurrentTarget(target);
            return false;
        }
    }

    private File prepareTargetFile(File file, File pathForSave, String outputFormat) {
        if (pathForSave.isDirectory()) {
            String fileName = file.getName();
            int dotIndex = fileName.lastIndexOf('.');
            String nameWithoutExtension = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
            String extension = getExtensionFromFormat(outputFormat);
            return new File(pathForSave, nameWithoutExtension + "_" + UUID.randomUUID().toString().replace("-", "") + "." + extension);
        }
        return pathForSave;
    }

    private String getExtensionFromFormat(String format) {
        if ("matroska".equalsIgnoreCase(format)) return "mkv";
        if ("ipod".equalsIgnoreCase(format))     return "m4a";
        if ("adts".equalsIgnoreCase(format))     return "aac";
        if ("asf".equalsIgnoreCase(format))      return "wmv";
        return format;
    }

    private EncodingAttributes createEncodingAttributes(VideoAndAudioProperties properties, MediaType type, MultimediaInfo sourceInfo) {
        EncodingAttributes attrs = new EncodingAttributes();
        String outputFormat = properties.getFfmpegFormat();
        String normalizedFormat = outputFormat;
        if ("mkv".equalsIgnoreCase(outputFormat))      { normalizedFormat = "matroska"; }
        else if ("m4a".equalsIgnoreCase(outputFormat)) { normalizedFormat = "ipod";     }

        attrs.setOutputFormat(normalizedFormat);

        boolean isVideo = (type == MediaType.VIDEO);
        boolean hasAudio = (nonNull(sourceInfo) && nonNull(sourceInfo.getAudio()));
        
        ErrorLogger.info(ConverterVideoAudioFile.class, "Creating encoding attributes - Format: " + normalizedFormat + ", IsVideo: " + isVideo + ", HasAudio: " + hasAudio);

        if (hasAudio || !isVideo) {
            ErrorLogger.info(ConverterVideoAudioFile.class, "Setting up audio attributes...");
            attrs.setAudioAttributes(setupAudioAttributes(properties));
        } else {
            ErrorLogger.info(ConverterVideoAudioFile.class, "Source file has no audio track. Skipping audio attributes for video conversion.");
        }

        if (isVideo) {
            ErrorLogger.info(ConverterVideoAudioFile.class, "Setting up video attributes...");
            attrs.setVideoAttributes(setupVideoAttributes(properties));
        }
        return attrs;
    }

    private AudioAttributes setupAudioAttributes(VideoAndAudioProperties properties) {
        AudioAttributes audio = new AudioAttributes();
        String audioCodec = properties.getAudioCodec();
        audio.setCodec(audioCodec);
        ErrorLogger.info(ConverterVideoAudioFile.class, "Setting audio codec: " + audioCodec);

        int audioBitrate = properties.getAudioBitRate();
        if (audioBitrate > 0 && shouldSetAudioBitrate(audioCodec)) {
            audio.setBitRate(audioBitrate * 1000);
            ErrorLogger.info(ConverterVideoAudioFile.class, "Setting audio bitrate: " + audioBitrate + " kbps");
        } else {
            ErrorLogger.info(ConverterVideoAudioFile.class, "Skipping audio bitrate (value=" + audioBitrate + ", shouldSet=" + shouldSetAudioBitrate(audioCodec) + ")");
        }
        
        int channels = properties.getChannel();
        if (channels > 0) {
            audio.setChannels(channels);
            ErrorLogger.info(ConverterVideoAudioFile.class, "Setting audio channels: " + channels);
        } else {
            ErrorLogger.info(ConverterVideoAudioFile.class, "Skipping audio channels (value=" + channels + ")");
        }
        
        int samplingRate = properties.getSamplingRate();
        if (samplingRate > 0) {
            audio.setSamplingRate(samplingRate);
            ErrorLogger.info(ConverterVideoAudioFile.class, "Setting audio sampling rate: " + samplingRate + " Hz");
        } else {
            ErrorLogger.info(ConverterVideoAudioFile.class, "Skipping audio sampling rate (value=" + samplingRate + ")");
        }
        
        return audio;
    }

    private VideoAttributes setupVideoAttributes(VideoAndAudioProperties properties) {
        VideoAttributes video = new VideoAttributes();
        video.setCodec(properties.getVideoCodec());
        int videoBitrate = properties.getVideoBitRate();
        if (videoBitrate > 0) {
            video.setBitRate(videoBitrate * 1000);
        }
        video.setPixelFormat("yuv420p");

        int fps = properties.getFps();
        if (fps > 0) { video.setFrameRate(fps); }

        PreparingAttributes.parseSize(properties.getResolution()).ifPresent(video::setSize);
        return video;
    }

    public void cancelConversion() {
        File target = currentTarget;
        if (nonNull(target)) {
            EncoderUtility.abortEncoding(encoder, target);
            currentTarget = null;
        } else {
            encoder.abortEncoding();
        }
    }

    private boolean checkingFile(File file) {
        return file != null && file.exists();
    }

    public static boolean shouldSetAudioBitrate(String codec) {
        if (isNull(codec)) return true;
        String c = codec.toLowerCase();
        return !c.contains("flac") && !c.contains("alac") && !c.contains("pcm");
    }

    private void handleError(Exception e) {
        String msg = e.getMessage();
        Throwable cause = e.getCause();
        String causeMsg = (nonNull(cause)) ? cause.getMessage() : "";

        boolean isCancelled = (nonNull(msg) && (msg.contains("Encoding interrupted") || msg.contains("Stream Closed")))
                || (nonNull(causeMsg) && causeMsg.contains("Stream Closed"));

        if (isCancelled) {
            ErrorLogger.info(ConverterVideoAudioFile.class, "Conversion was cancelled by user.");
        } else {
            ErrorLogger.info(ConverterVideoAudioFile.class, "Conversion failed: " + e.getMessage());
            ErrorLogger.log(109, ErrorLogger.Level.ERROR, "Exception during conversion", e);
        }
    }

    private void clearCurrentTarget(File target) {
        if (nonNull(target) && target.equals(currentTarget)) {
            currentTarget = null;
        }
    }
}
