package model.compressorVideo;

import javafx.scene.control.Alert;
import model.logger.ErrorLogger;
import model.utility.DetermineType;
import model.utility.EncoderUtility;
import viewHelp.Alerts;
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
    private static String videoCodec;
    private static String audioCodec;
    private static String ffmpegFormat;
    private static boolean useGPU;
    private static volatile File currentTarget;
    private static final Encoder encoder = new Encoder();

    /**
     * Method for video compression.
     * <p>
     * Codecs are not required and are not recommended to be passed in attributes.
     * </p>
     */
    public static void compress(File videoFile, File output,
                                VideoAttributes video, AudioAttributes audio, Consumer<Double> progressConsumer) {
        currentTarget = output;

        try {
            MultimediaObject multimediaObject = new MultimediaObject(videoFile);
            MultimediaInfo sourceInfo = multimediaObject.getInfo();

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
        }
        catch (EncoderException e) {
            Alerts.alertDialog(Alert.AlertType.ERROR, "Encoder error!", "Encoder error!",
                    "Video file compression error, check log files for more information!");
            ErrorLogger.log(120, ErrorLogger.Level.ERROR, "Encoder error! ", e);
        }
        catch (Exception e) {
            ErrorLogger.error(e.getMessage());
        }
        finally {
            clearCurrentTarget(output);
        }
    }

    public static void cancelCompress() {
        File target = currentTarget;
        if (target != null) {
            EncoderUtility.abortEncoding(encoder, target);
            currentTarget = null;
        } else {
            encoder.abortEncoding();
        }
    }

    private static void clearCurrentTarget(File target) {
        if (target != null && target.equals(currentTarget)) {
            currentTarget = null;
        }
    }

    public static void getCodec(File videoFile) {
        String formatVideo = DetermineType.determineFormat(videoFile);
        switch (formatVideo) {
            case "mp4", "m4v" -> {
                videoCodec = useGPU ? "h264_nvenc" : "libx264";
                audioCodec = "aac";
                ffmpegFormat = "mp4";
            }
            case "mkv", "matroska" -> {
                videoCodec = useGPU ? "h264_nvenc" : "libx264";
                audioCodec = "aac";
                ffmpegFormat = "mkv";
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
            case "mov" -> {
                videoCodec = useGPU ? "h264_nvenc" : "libx264";
                audioCodec = "aac";
                ffmpegFormat = "mov";
            }
            default -> {
                videoCodec = useGPU ? "h264_nvenc" : "libx264";
                audioCodec = "aac";
            }
        }
    }
}
