package model.converterVideo;

import model.enums.TypeMedia;
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
import java.util.function.Consumer;

public class ConverterVideoAudioFile {
    private final Encoder encoder = new Encoder();
    private volatile File currentTarget;

    public File nameFileAfter;

    public boolean convert(VideoAndAudioProperties properties, TypeMedia typeConvert, Consumer<Double> progressConsumer) {
        File file = properties.getSrcFile();
        if (!checkingFile(file)) {
            return false;
        }

        File target = prepareTargetFile(file, properties.getOutput(), properties.getFfmpegFormat());
        currentTarget = target;
        nameFileAfter = target;

        ErrorLogger.info("Starting conversion...");
        try {
            MultimediaObject multimediaObject = new MultimediaObject(file);
            MultimediaInfo sourceInfo = multimediaObject.getInfo();

            EncodingAttributes attrs = createEncodingAttributes(properties, typeConvert, sourceInfo);

            ErrorLogger.info("Starting encoding: " + file.getName() + " [V-BR: " + properties.getVideoBitRate() + ", A-BR: " + properties.getAudioBitRate() + "]");

            encoder.encode(multimediaObject, target, attrs, new EncoderProgressListener() {
                @Override
                public void sourceInfo(MultimediaInfo info) {
                    ErrorLogger.info("Source info: " + info.toString());
                }

                @Override
                public void progress(int permille) {
                    if (progressConsumer != null) {
                        progressConsumer.accept(permille / 1000.0);
                    }
                }

                @Override
                public void message(String message) {
                    ErrorLogger.info("FFmpeg: " + message);
                }
            });

            ErrorLogger.info("Conversion successful!");
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
        if ("ipod".equalsIgnoreCase(format)) return "m4a";
        if ("adts".equalsIgnoreCase(format)) return "aac";
        if ("asf".equalsIgnoreCase(format)) return "wmv";
        return format;
    }

    private EncodingAttributes createEncodingAttributes(VideoAndAudioProperties properties, TypeMedia type, MultimediaInfo sourceInfo) {
        EncodingAttributes attrs = new EncodingAttributes();
        String outputFormat = properties.getFfmpegFormat();
        String normalizedFormat = outputFormat;
        if ("mkv".equalsIgnoreCase(outputFormat)) {
            normalizedFormat = "matroska";
        } else if ("m4a".equalsIgnoreCase(outputFormat)) {
            normalizedFormat = "ipod";
        }
        attrs.setOutputFormat(normalizedFormat);

        boolean isVideo = (type == TypeMedia.VIDEO);

        if (sourceInfo.getAudio() != null || !isVideo) {
            attrs.setAudioAttributes(setupAudioAttributes(properties));
        } else {
            ErrorLogger.info("Source file has no audio track. Skipping audio attributes for video conversion.");
        }

        if (isVideo) {
            attrs.setVideoAttributes(setupVideoAttributes(properties));
        }
        return attrs;
    }

    private AudioAttributes setupAudioAttributes(VideoAndAudioProperties properties) {
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec(properties.getAudioCodec());

        int audioBitrate = properties.getAudioBitRate();
        if (audioBitrate > 0 && shouldSetAudioBitrate(properties.getAudioCodec())) {
            audio.setBitRate(audioBitrate * 1000);
        }
        audio.setChannels(properties.getChannel());
        audio.setSamplingRate(properties.getSamplingRate());
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
        if (fps > 0) {
            video.setFrameRate(fps);
        }

        PreparingAttributes.parseSize(properties.getResolution()).ifPresent(video::setSize);
        return video;
    }

    public void cancelConversion() {
        File target = currentTarget;
        if (target != null) {
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
        if (codec == null) return true;
        String c = codec.toLowerCase();
        return !c.contains("flac") && !c.contains("alac") && !c.contains("pcm");
    }

    private void handleError(Exception e) {
        String msg = e.getMessage();
        Throwable cause = e.getCause();
        String causeMsg = (cause != null) ? cause.getMessage() : "";

        boolean isCancelled = (msg != null && (msg.contains("Encoding interrupted") || msg.contains("Stream Closed")))
                || (causeMsg != null && causeMsg.contains("Stream Closed"));

        if (isCancelled) {
            ErrorLogger.info("Conversion was cancelled by user.");
        } else {
            ErrorLogger.info("Conversion failed: " + e.getMessage());
            ErrorLogger.log(109, ErrorLogger.Level.ERROR, "Exception during conversion", e);
        }
    }

    private void clearCurrentTarget(File target) {
        if (target != null && target.equals(currentTarget)) {
            currentTarget = null;
        }
    }
}
