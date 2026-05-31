package model.compressorVideo;

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

public class Compressor {
    private String videoCodec;
    private String audioCodec;
    private String ffmpegFormat;
    private boolean useGPU;
    private volatile File currentTarget;
    private final Encoder encoder = new Encoder();

    public void compress(File videoFile, File output,
                                VideoAttributes video, AudioAttributes audio, Consumer<Double> progressConsumer) throws EncoderException {
        currentTarget = output;

        try {
            MultimediaObject multimediaObject = new MultimediaObject(videoFile);
            MultimediaInfo sourceInfo = multimediaObject.getInfo();

            getCodec(videoFile);

            video.setCodec(videoCodec);
            
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat(ffmpegFormat);
            attrs.setVideoAttributes(video);

            if (sourceInfo.getAudio() != null) {
                audio.setCodec(audioCodec);
                attrs.setAudioAttributes(audio);
            } else {
                ErrorLogger.info("Source video has no audio track. Skipping audio attributes in compressor.");
            }

            encoder.encode(multimediaObject, output, attrs, new EncoderProgressListener() {
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
            
            ErrorLogger.info("Video compression completed successfully: " + output.getAbsolutePath());
        } catch (EncoderException e) {
            String msg = e.getMessage();
            boolean isCancelled = msg != null && (msg.contains("Encoding interrupted") || msg.contains("Stream Closed"));
            if (isCancelled) {
                ErrorLogger.info("Compression was cancelled by user.");
            } else {
                throw e;
            }
        } finally {
            clearCurrentTarget(output);
        }
    }

    public void cancelCompress() {
        File target = currentTarget;
        if (target != null) {
            EncoderUtility.abortEncoding(encoder, target);
            currentTarget = null;
        } else {
            encoder.abortEncoding();
        }
    }

    private void clearCurrentTarget(File target) {
        if (target != null && target.equals(currentTarget)) {
            currentTarget = null;
        }
    }

    public void getCodec(File videoFile) {
        String formatVideo = DetermineType.determineFormat(videoFile).orElse("");
        switch (formatVideo) {
            case "mp4", "m4v" -> {
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
            case "wmv", "x-ms-wmv" -> {
                videoCodec = useGPU ? "h264_nvenc" : "libx264";
                audioCodec = "aac";
                ffmpegFormat = "mp4";
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

    public void setUseGPU(boolean enable) {
        useGPU = enable;
    }

}
