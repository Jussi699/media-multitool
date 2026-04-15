package model.converterMP3;

import javafx.scene.control.Alert;
import model.logger.ErrorLogger;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ConverterToMP3 {
    public static String getFormatFile(File file) {
        if(!file.exists()) {
            return null;
        }

        try {
            return Files.probeContentType(file.toPath());
        } catch (IOException e){
            ErrorLogger.alertDialog(Alert.AlertType.WARNING, "ERROR", "Read Error!",
                    "File format could not be determined!\nCheck that the file is not damaged and that it is accessible");
            return null;
        }
    }

    public static boolean convert(File file, File pathForSave, int bitRate, int channels, int samplingRate) {
        String format = getFormatFile(file);

        if(format == null) {
            ErrorLogger.alertDialog(Alert.AlertType.WARNING, "WARN", "File missing!", "The selected file was not found or is empty.");
            ErrorLogger.warn("File is null | Name: " + file.getName() + " | Path: " + file.getAbsolutePath());
            return false;
        }

        try {
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libmp3lame");
            audio.setBitRate(bitRate);
            audio.setChannels(channels);
            audio.setSamplingRate(samplingRate);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setAudioAttributes(audio);
            attrs.setInputFormat(format);
            attrs.setOutputFormat("mp3");

            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(file), pathForSave, attrs);
            return true;
        } catch (Exception e){
            ErrorLogger.alertDialog(Alert.AlertType.WARNING, "ERROR", "Exception", "Exception. Check log file for more information!");
            ErrorLogger.log(109, ErrorLogger.Level.ERROR, "Exception", e);
            e.printStackTrace();
            return false;
        }
    }

}
