package model.compressorVideo;

import lombok.Setter;
import model.logger.ErrorLogger;
import model.utility.DetermineType;
import model.utility.EncoderUtility;
import ws.schild.jave.EncoderException;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.progress.EncoderProgressListener;

import java.io.File;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

public class Compressor {
    private final Encoder encoder = new Encoder();
    private volatile File currentTarget;

    private String videoCodec;
    private String audioCodec;
    private String ffmpegFormat;

    @Setter private boolean useGPU;
    @Setter private boolean compressAudio = true;

    public void compress(File videoFile, File output,
                                VideoAttributes video, AudioAttributes audio,
                                int crf, Consumer<Double> progressConsumer) throws EncoderException {
        currentTarget = output;

        try {
            MultimediaObject multimediaObject = new MultimediaObject(videoFile);
            MultimediaInfo sourceInfo = multimediaObject.getInfo();

            getCodec(videoFile);

            video.setCodec(videoCodec);

            if ("libx264".equals(videoCodec) && crf >= 0) {
                video.setBitRate(null);
                video.setCrf(crf);
                video.setPreset("medium");
                ErrorLogger.info(Compressor.class, "Using CRF mode: crf=" + crf + ", preset=medium, codec=" + videoCodec);
            } else {
                ErrorLogger.info(Compressor.class, "Using ABR mode: bitRate=" + video.getBitRate().orElse(-1) + " bps, codec=" + videoCodec);
            }

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat(ffmpegFormat);
            attrs.setVideoAttributes(video);

            if (nonNull(sourceInfo.getAudio())) {
                if (compressAudio) {
                    audio.setCodec(audioCodec);
                    attrs.setAudioAttributes(audio);
                } else {
                    AudioAttributes copyAudio = new AudioAttributes();
                    copyAudio.setCodec("copy");
                    attrs.setAudioAttributes(copyAudio);
                    ErrorLogger.info(Compressor.class, "Using copy codec for audio - preserving original audio stream.");
                }
            } else {
                ErrorLogger.info(Compressor.class, "Source video has no audio track. Skipping audio attributes in compressor.");
            }

            encoder.encode(multimediaObject, output, attrs, new EncoderProgressListener() {
                @Override
                public void sourceInfo(MultimediaInfo info) {
                    ErrorLogger.info(Compressor.class, "Source info: " + info);
                }


                @Override
                public void progress(int permille) {
                    if (nonNull(progressConsumer)) {
                        progressConsumer.accept(permille / 1000.0);
                    }
                }

                @Override
                public void message(String message) {
                    ErrorLogger.info(Compressor.class, "FFmpeg: " + message);
                }
            });
            
            ErrorLogger.info(Compressor.class, "Video compression completed successfully: " + output.getAbsolutePath());
        } catch (EncoderException e) {
            String msg = e.getMessage();
            boolean isCancelled = nonNull(msg) && (msg.contains("Encoding interrupted") || msg.contains("Stream Closed"));
            if (isCancelled) {
                ErrorLogger.info(Compressor.class, "Compression was cancelled by user.");
            } else {
                throw e;
            }
        } finally {
            clearCurrentTarget(output);
        }
    }

    public void cancelCompress() {
        File target = currentTarget;
        if (nonNull(target)) {
            EncoderUtility.abortEncoding(encoder, target);
            currentTarget = null;
        } else {
            encoder.abortEncoding();
        }
    }

    private void clearCurrentTarget(File target) {
        if (nonNull(target) && target.equals(currentTarget)) {
            currentTarget = null;
        }
    }

    public void getCodec(File videoFile) {
        String formatVideo = DetermineType.determineFormat(videoFile).orElse("");
        switch (formatVideo) {
            case "mp4", "m4v", "wmv", "x-ms-wmv" -> {
                videoCodec = useGPU ? "h264_nvenc" : "libx264";
                audioCodec = "aac";
                ffmpegFormat = "mp4";
            }
            case "mov" -> {
                videoCodec = useGPU ? "h264_nvenc" : "libx264";
                audioCodec = "aac";
                ffmpegFormat = "mov";
            }
            case "mkv", "matroska" -> {
                videoCodec = useGPU ? "h264_nvenc" : "libx264";
                audioCodec = "aac";
                ffmpegFormat = "matroska";
            }
            case "avi" -> {
                videoCodec = useGPU ? "h264_nvenc" : "mpeg4";
                audioCodec = "libmp3lame";
                ffmpegFormat = "avi";
            }
            case "webm" -> {
                videoCodec = "libvpx";
                audioCodec = "libvorbis";
                ffmpegFormat = "webm";
            }
            case "flv" -> {
                videoCodec = useGPU ? "h264_nvenc" : "libx264";
                audioCodec = "aac";
                ffmpegFormat = "flv";
            }
            case "3gp", "3gpp" -> {
                videoCodec = useGPU ? "h264_nvenc" : "libx264";
                audioCodec = "aac";
                ffmpegFormat = "3gp";
            }
            default -> {
                videoCodec = useGPU ? "h264_nvenc" : "libx264";
                audioCodec = "aac";
            }
        }
    }
}
