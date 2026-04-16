package model.converterMP3;

import javafx.scene.control.Alert;
import model.logger.ErrorLogger;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class ConverterToMP3 {

    public static boolean convert(File file, File pathForSave, int bitRate, int channels, int samplingRate) {
        if (file == null || !file.exists()) {
            ErrorLogger.alertDialog(Alert.AlertType.WARNING, "WARN", "File missing!", "The selected file was not found or is empty.");
            return false;
        }

        File target;
        if (pathForSave.isDirectory()) {
            String fileName = file.getName();
            int dotIndex = fileName.lastIndexOf('.');
            String nameWithoutExtension = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
            target = new File(pathForSave, nameWithoutExtension + ".mp3");
        } else {
            target = pathForSave;
        }

        CompletableFuture.supplyAsync(() -> {
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
                Encoder encoder = new Encoder();
                encoder.encode(new MultimediaObject(file), target, attrs);
                ErrorLogger.info("Conversation has been success!");
                return true;
            } catch (Exception e) {
                ErrorLogger.info("The conversion was not successful!");
                ErrorLogger.alertDialog(Alert.AlertType.WARNING, "ERROR", "Exception", "Exception. Check log file for more information!");
                ErrorLogger.log(109, ErrorLogger.Level.ERROR, "Exception", e);
                e.printStackTrace();
                return false;
            }
        }).thenAccept(_ -> ErrorLogger.info("Asynchrony was successful!"));
        return true;
    }
}
