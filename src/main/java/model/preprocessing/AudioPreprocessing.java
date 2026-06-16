package model.preprocessing;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import model.logger.ErrorLogger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import viewHelp.Alerts;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

public class AudioPreprocessing {
    public static void applyTags(File file, Map<FieldKey, String> tags, String pathToPhoto) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTagOrCreateAndSetDefault();

            for (Map.Entry<FieldKey, String> entry : tags.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    tag.setField(entry.getKey(), entry.getValue());
                } else {
                    tag.deleteField(entry.getKey());
                }
            }

            if (pathToPhoto != null && !pathToPhoto.isEmpty()) {
                Artwork artwork = ArtworkFactory.createArtworkFromFile(new File(pathToPhoto));
                tag.deleteArtworkField();
                tag.setField(artwork);
            }

            audioFile.commit();
        }
        catch (Exception e) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Something wrong!", "Something wrong!", "Check log file for more details for error!");
            ErrorLogger.error("193 | " + e.getMessage());
        }
    }

    public static Map<FieldKey, String> getTags(File file) {
        Map<FieldKey, String> tagMap = new HashMap<>();
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            if (tag != null) {
                for (FieldKey key : FieldKey.values()) {
                    try {
                        String value = tag.getFirst(key);
                        if (value != null && !value.isEmpty()) {
                            tagMap.put(key, value);
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Something wrong!", "Something wrong!", "Check log file for more details for error!");
            ErrorLogger.error("190 | " + e.getMessage());
        }
        return tagMap;
    }

    public static Optional<Image> getIconMp3(File file) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();

            if (tag != null) {
                Artwork artwork = tag.getFirstArtwork();
                if (artwork != null) {
                    byte[] imageData = artwork.getBinaryData();
                    return Optional.of(new Image(new ByteArrayInputStream(imageData)));
                }
            }
        } catch (Exception e) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Something wrong!", "Something wrong!", "Check log file for more details for error!");
            ErrorLogger.error("191 | " + e.getMessage());
        }
        return Optional.empty();
    }
}
