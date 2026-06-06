package viewHelp.audioEditor;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.preprocessing.AudioPreprocessing;
import model.properties.VideoAndAudioProperties;

import java.util.List;
import java.util.Objects;

public class AudioEditor {
    public static void updatePreviewWithPath(VideoAndAudioProperties audioProperties, ImageView imageView) {
        if (audioProperties.getPathToImage() != null) {
            setPreview(new Image(audioProperties.getPathToImage().toURI().toString()), imageView);
        }
    }

    public static void setPreview(Image image, ImageView imageViewPreview) {
        if (imageViewPreview != null) {
            imageViewPreview.setImage(image);
        }
    }

    public static void loadDefaultPreview(ImageView imageViewPreview) {
        setPreview(new Image(Objects.requireNonNull(AudioEditor.class.getResourceAsStream("/img/defaultImageMp3.png"))), imageViewPreview);
    }

    public static void clearFields(List<TextField> textFields, ComboBox<String> comboBox) {
        textFields.forEach(TextInputControl::clear);
        comboBox.setValue(null);
        comboBox.getEditor().clear();
    }

    public static void updatePreview(VideoAndAudioProperties audioProperties, ImageView imageViewPreview) {
        if (audioProperties.getSrcFile() != null) {
            AudioPreprocessing.getIconMp3(audioProperties.getSrcFile()).ifPresent(file -> AudioEditor.setPreview(file, imageViewPreview));
        }
    }

    public static void setGenreValue(ComboBox<String> genreComboBox, String genre) {
        if (genre == null || genre.isEmpty()) {
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
