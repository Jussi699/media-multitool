package model.converterMP3;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import model.logger.ErrorLogger;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.progress.EncoderProgressListener;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ConverterToMP3 {
    private static final Encoder encoder = new Encoder();

    public static CompletableFuture<Boolean> convert(File file, File pathForSave, int bitRate, int channels, int samplingRate, Consumer<Double> progressConsumer) {
        if (file == null || !file.exists()) {
            ErrorLogger.alertDialog(Alert.AlertType.WARNING, "WARN", "File missing!", "The selected file was not found or is empty.");
            return CompletableFuture.completedFuture(false);
        }

        File target;
        if (pathForSave.isDirectory()) {
            String fileName = file.getName();
            int dotIndex = fileName.lastIndexOf('.');
            String nameWithoutExtension = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
            target = new File(pathForSave, nameWithoutExtension + UUID.randomUUID().toString().replace("-", "") + ".mp3");
        } else {
            target = pathForSave;
        }

        return CompletableFuture.supplyAsync(() -> {
            ErrorLogger.info("Starting async...");
            try {
                AudioAttributes audio = new AudioAttributes();
                audio.setCodec("libmp3lame");
                audio.setBitRate(bitRate * 1000);
                audio.setChannels(channels);
                audio.setSamplingRate(samplingRate);

                EncodingAttributes attrs = new EncodingAttributes();
                attrs.setAudioAttributes(audio);
                attrs.setOutputFormat("mp3");

                ErrorLogger.info("Starting conversation...");
                encoder.encode(new MultimediaObject(file), target, attrs, new EncoderProgressListener() {
                    @Override
                    public void sourceInfo(MultimediaInfo info) {}

                    @Override
                    public void progress(int permille) {
                        if (progressConsumer != null) {
                            progressConsumer.accept(permille / 1000.0);
                        }
                    }

                    @Override
                    public void message(String message) {}
                });
                ErrorLogger.info("Conversation has been success!");
                return true;
            } catch (Exception e) {
                String msg = e.getMessage();
                boolean isCancelled = msg != null && (msg.contains("Encoding interrupted") || msg.contains("Stream Closed"));
                
                if (isCancelled) {
                    ErrorLogger.info("Conversion was cancelled by user (or stream closed due to abort).");
                } else {
                    ErrorLogger.info("The conversion was not successful!");
                    Platform.runLater(() -> ErrorLogger.alertDialog(Alert.AlertType.WARNING, "ERROR", "Exception", "Exception. Check log file for more information!"));
                    ErrorLogger.log(109, ErrorLogger.Level.ERROR, "Exception", e);
                    e.printStackTrace();
                }
                return false;
            }
        });
    }

    public static void cancelConversion() {
        encoder.abortEncoding();
    }
}
