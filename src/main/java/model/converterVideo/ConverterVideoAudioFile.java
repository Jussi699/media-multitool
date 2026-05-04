package model.converterVideo;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import model.logger.ErrorLogger;
import model.utility.EncoderUtility;
import viewHelp.Alerts;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.VideoSize;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.progress.EncoderProgressListener;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static model.utility.Util.IO_EXECUTOR;

public class ConverterVideoAudioFile {
    private static final Encoder encoder = new Encoder();
    
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                encoder.abortEncoding();
            } catch (Exception ignored) {
            }
        }));
    }
    
    public static File nameFileAfter;

    private static volatile File currentTarget;

    public static CompletableFuture<Boolean> convert(File file,
                                                     File pathForSave,
                                                     int videoBitrate, int audioBitrate,
                                                     int channels, int samplingRate, int fps,
                                                     String videoCodec, String audioCodec, String output_format, String resolution, String typeConvert,
                                                     Consumer<Double> progressConsumer) {
        if (!checkingFileStatic(file)) {
            return CompletableFuture.completedFuture(false);
        }

        File target;
        if (pathForSave.isDirectory()) {
            String fileName = file.getName();
            int dotIndex = fileName.lastIndexOf('.');
            String nameWithoutExtension = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
            String extension = output_format;
            if ("matroska".equalsIgnoreCase(output_format)) {
                extension = "mkv";
            } else if ("ipod".equalsIgnoreCase(output_format)) {
                extension = "m4a";
            } else if ("adts".equalsIgnoreCase(output_format)) {
                extension = "aac";
            }
            target = new File(pathForSave, nameWithoutExtension + "_" + UUID.randomUUID().toString().replace("-", "") + "." + extension);
            nameFileAfter = target;
        } else {
            target = pathForSave;
        }

        currentTarget = target;

        return CompletableFuture.supplyAsync(() -> {
            ErrorLogger.info("Starting async conversion...");
            try {
                MultimediaObject multimediaObject = new MultimediaObject(file);
                MultimediaInfo sourceInfo = multimediaObject.getInfo();
                
                EncodingAttributes attrs = new EncodingAttributes();
                String normalizedFormat = output_format;
                if ("mkv".equalsIgnoreCase(output_format)) {
                    normalizedFormat = "matroska";
                } else if ("m4a".equalsIgnoreCase(output_format)) {
                    normalizedFormat = "ipod";
                }
                attrs.setOutputFormat(normalizedFormat);

                boolean isVideo = "video".equalsIgnoreCase(typeConvert);

                if (sourceInfo.getAudio() != null || !isVideo) {
                    AudioAttributes audio = new AudioAttributes();
                    audio.setCodec(audioCodec);

                    if (audioBitrate > 0 && isLossless(audioCodec)) {
                        audio.setBitRate(audioBitrate * 1000);
                    }
                    audio.setChannels(channels);
                    audio.setSamplingRate(samplingRate);
                    attrs.setAudioAttributes(audio);
                } else {
                    ErrorLogger.info("Source file has no audio track. Skipping audio attributes for video conversion.");
                }

                if (isVideo) {
                    VideoAttributes video = new VideoAttributes();
                    video.setCodec(videoCodec);
                    if (videoBitrate > 0) {
                        video.setBitRate(videoBitrate * 1000);
                    }
                    video.setPixelFormat("yuv420p");

                    if (fps > 0) {
                        video.setFrameRate(fps);
                    }
                    if (resolution != null && resolution.contains("x")) {
                        try {
                            String[] res = resolution.split("x");
                            int width = Integer.parseInt(res[0]);
                            int height = Integer.parseInt(res[1]);

                            if (width % 2 != 0) width--;
                            if (height % 2 != 0) height--;

                            video.setSize(new VideoSize(width, height));
                        } catch (Exception e) {
                            ErrorLogger.warn("Invalid resolution format: " + resolution);
                        }
                    }
                    attrs.setVideoAttributes(video);
                }

                ErrorLogger.info("Starting encoding: " + file.getName() + " [V-BR: " + videoBitrate + ", A-BR: " + audioBitrate + "]");

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
        }, IO_EXECUTOR);
    }

    public static CompletableFuture<Boolean> convert(File file,
                                                     File pathForSave,
                                                     int bitRate, int channels, int samplingRate,
                                                     String audioCodec, String output_format,
                                                     Consumer<Double> progressConsumer) {
        return convert(file, pathForSave, -1, bitRate, channels, samplingRate, -1, null, audioCodec, output_format, null, "audio", progressConsumer);
    }

    public static void cancelConversion() {
        File target = currentTarget;
        if (target != null) {
            EncoderUtility.abortEncoding(encoder, target);
            currentTarget = null;
        } else {
            encoder.abortEncoding();
        }
    }

    private static boolean checkingFileStatic(File file) {
        if (file == null || !file.exists()) {
            try {
                Platform.runLater(() -> Alerts.alertDialog(Alert.AlertType.WARNING, "WARN",
                        "File missing!", "The selected file was not found or is empty."));
            } catch (IllegalStateException ignored) {}
            return false;
        }
        return true;
    }

    public static boolean isLossless(String codec) {
        if (codec == null) return true;
        String c = codec.toLowerCase();
        return !c.contains("flac") && !c.contains("alac") && !c.contains("pcm");
    }

    private static void handleError(Exception e) {
        String msg = e.getMessage();
        boolean isCancelled = msg != null && (msg.contains("Encoding interrupted") || msg.contains("Stream Closed"));

        if (isCancelled) {
            ErrorLogger.info("Conversion was cancelled by user.");
        } else {
            ErrorLogger.info("Conversion failed: " + e.getMessage());
            try {
                Platform.runLater(() -> Alerts.alertDialog(Alert.AlertType.WARNING, "ERROR", "Conversion Error", "FFmpeg Error: " + e.getMessage()));
            } catch (IllegalStateException ignored) {}
            ErrorLogger.log(109, ErrorLogger.Level.ERROR, "Exception during conversion", e);
        }
    }

    private static void clearCurrentTarget(File target) {
        if (target != null && target.equals(currentTarget)) {
            currentTarget = null;
        }
    }
}
