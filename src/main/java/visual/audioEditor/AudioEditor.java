package visual.audioEditor;

import app.Launcher;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.logger.ErrorLogger;
import model.preprocessing.AudioPreprocessing;
import model.properties.VideoAndAudioProperties;

import java.net.URL;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class AudioEditor {
    private AudioEditor() {
        /* This utility class should not be instantiated */
    }

    public static void updatePreviewWithPath(VideoAndAudioProperties audioProperties, ImageView imageView) {
        if (nonNull(audioProperties.getPathToImage())) {
            Image image = (new Image(audioProperties.getPathToImage().toURI().toString()));
            setPreview(image, imageView);
        }
    }

    public static void setPreview(Image image, ImageView imageViewPreview) {
        if (nonNull(imageViewPreview)) {
            imageViewPreview.setImage(image);
        }
    }

    public static void loadDefaultPreview(ImageView imageViewPreview) {
        URL imageResource = Launcher.class.getResource("/img/defaultImageMp3.png");
        if (isNull(imageResource)) {
            ErrorLogger.error("Default audio preview image not found: /img/defaultImageMp3.png");
            return;
        }
        setPreview(new Image(imageResource.toExternalForm()), imageViewPreview);
    }

    public static void clearFields(List<TextField> textFields, ComboBox<String> comboBox) {
        textFields.forEach(TextInputControl::clear);
        comboBox.setValue(null);
        comboBox.getEditor().clear();
    }

    public static void updatePreview(VideoAndAudioProperties audioProperties, ImageView imageViewPreview) {
        if (nonNull(audioProperties.getSrcFile())) {
            AudioPreprocessing.getIconMp3(audioProperties.getSrcFile()).ifPresent(
                    file -> AudioEditor.setPreview(file, imageViewPreview));
        }
    }

    public static void setGenreValue(ComboBox<String> genreComboBox, String genre) {
        if (isNull(genre) || genre.isEmpty()) {
            genreComboBox.setValue(null);
            genreComboBox.getEditor().clear();
            return;
        }
        if (genreComboBox.getItems().contains(genre)) {
            genreComboBox.setValue(genre);
        } else {
            genreComboBox.setValue(null);
            genreComboBox.getEditor().setText(genre);
        }
    }
}
