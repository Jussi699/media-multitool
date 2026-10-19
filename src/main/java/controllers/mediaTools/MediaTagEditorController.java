package controllers.mediaTools;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import controllers.abstractControllers.AbstractMediaController;
import model.enums.BatchTargetType;
import model.enums.MediaType;
import model.metadata.DetailsAudioFile;
import model.preprocessing.AudioPreprocessing;
import model.preprocessing.PhotoTagPreprocessing;
import model.preprocessing.VideoTagPreprocessing;
import model.logger.ErrorLogger;
import model.properties.MediaProperties;
import model.properties.VideoAndAudioProperties;
import model.utility.context.ResetContext;
import model.utility.format.SupportedFormat;
import org.jaudiotagger.tag.FieldKey;
import model.helper.TableViewHelper;
import org.jspecify.annotations.NonNull;
import visual.dialogs.Alerts;
import visual.dialogs.InfoAlert;
import model.helper.media.AudioTagEditorUiHelper;
import visual.controls.SetupScrollPane;

import java.io.File;
import java.util.*;
import java.util.function.UnaryOperator;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static model.enums.MediaType.*;
import static model.utility.io.PathWorker.*;
import static model.utility.checks.TemplateCheck.*;
import static visual.controls.Message.*;

public class MediaTagEditorController extends AbstractMediaController {
    private final ObservableList<DetailsAudioFile> masterFile = FXCollections.observableArrayList();
    private final VideoAndAudioProperties audioProperties = new VideoAndAudioProperties();
    private final FilteredList<DetailsAudioFile> filteredFile = new FilteredList<>(masterFile, _ -> true);
    private final ScrollBar[] tableInternalVBarRef = new ScrollBar[1];

    private static final Map<String, FieldKey> AUDIO_TAG_KEYS = Map.of(
            "title", FieldKey.TITLE,
            "artist", FieldKey.ARTIST,
            "album", FieldKey.ALBUM,
            "albumArtist", FieldKey.ALBUM_ARTIST,
            "composer", FieldKey.COMPOSER,
            "track", FieldKey.TRACK,
            "discNumber", FieldKey.DISC_NO,
            "comment", FieldKey.COMMENT,
            "year", FieldKey.YEAR
    );

    private static final Set<String> AUDIO_EDITABLE_TAGS = Set.of(
            "title", "artist", "album", "albumArtist", "composer", "track",
            "discNumber", "comment", "year", "genre"
    );
    private static final Set<String> PHOTO_EDITABLE_TAGS = Set.of(
            "title", "artist", "comment", "description", "keywords", "copyright", "rating"
    );
    private static final Set<String> VIDEO_EDITABLE_TAGS = Set.of(
            "title", "artist", "album", "albumArtist", "composer", "track", "discNumber",
            "comment", "year", "description", "keywords", "copyright", "rating", "genre"
    );

    @Override
    protected MediaProperties getProperties() {
        return audioProperties;
    }

    @FXML private Button btnChangeIcon, btnSelectMediaFile, btnChoiceDirForSave;
    @FXML private Button btnSelectMultipleFile, btnSaveChanges;
    @FXML private ImageView imageViewPreview;
    @FXML private StackPane dropZone;
    @FXML private ScrollPane tableScrollPane;
    @FXML private ScrollBar tableVerticalScrollBar;
    @FXML private Label labelSelectImageName, textDragZone;
    @FXML private Label titleLabel, artistLabel, albumLabel;
    @FXML private Label albumArtistLabel, composerLabel, trackLabel;
    @FXML private Label discNumberLabel, commentLabel, yearLabel;
    @FXML private Label descriptionLabel, keywordsLabel, copyrightLabel;
    @FXML private Label ratingLabel, genreLabel;
    @FXML private ComboBox<String> genreComboBox;
    @FXML private TableView<DetailsAudioFile> tableViewAudio;
    @FXML private TextField titleField, artistField, albumField,
            albumArtistField, composerField, trackField,
            discNumberField, commentField, yearField,
            descriptionField, keywordsField, copyrightField,
            ratingField, textFieldFindFile;

    private List<TextField> textFields;
    private Map<String, TextField> tagTextFields;
    private Map<String, Label> tagLabels;
    private Map<String, String> originalTagLabelTexts;
    private Set<String> editableTagKeys = Set.of();
    private MediaType selectedMediaType;
    private File chosenDir;

    @FXML
    private void initialize() {
        tableViewAudio.setItems(filteredFile);

        initLists();
        initTextFieldFind();
        initComboBoxes();

        audioProperties.setOutput(getSavedPath());

        setupClearMessageTimer(labelSuccess, progressBar, audioProperties.getHideSuccessMessageTimer(), true);

        onResetPressed();
        setupDragAndDrop(dropZone, getSupportedTagEditorFormats(), this::loadFile);
    }

    @FXML
    private void showInfo() {
        InfoAlert.showToolInfoWithoutClipboard(
                "Media Tag Editor",
                """
                        1. Select an audio, photo or video file using "Select media" or drag and drop it into the dash-bordered zone;
                            (Optional) To view and edit multiple files from a folder,
                             click the "Select multiple file" button and select the folder;""",
                """
                3. Edit the available tag fields for the selected media file;

                4. (Optional) Change the cover image for audio files using "Change Icon";
                
                5. Click "Save Changes" to apply all changes.

                Supported audio formats: %s
                Supported video formats: %s
                Supported photo formats: JPEG (.jpg, .jpeg) and TIFF (.tif, .tiff).
                """.formatted(
                        String.join(", ", SupportedFormat.getAllSupportedAudioFormats()) + ";",
                        String.join(", ", SupportedFormat.getAllSupportedVideoFormats()) + ";"
                )
        );
    }

    @FXML
    private void onResetPressed() {
        ResetContext ctx = new ResetContext(
                labelSelectImageName, labelSuccess, textDragZone, null,
                dropZone, imageViewPreview, progressBar, true, "photo, video or audio"
        );
        reset(audioProperties, ctx, "Selected audio file: none");
        AudioTagEditorUiHelper.loadDefaultPreview(imageViewPreview);
        AudioTagEditorUiHelper.clearFields(textFields, genreComboBox);
        configureEditableFields(MediaType.NONE, Set.of());
        masterFile.clear();
        if (!SetupScrollPane.hasRealTableData(masterFile)) {
            SetupScrollPane.fillPlaceholderRows(tableViewAudio, masterFile, tableScrollPane.getViewportBounds().getHeight());
        }
        disableControls();

        if (nonNull(audioProperties.getSrcFile())) {
            ErrorLogger.error("For some reason, the file is not null when resetting!");
            AudioTagEditorUiHelper.loadDefaultPreview(imageViewPreview);
            AudioTagEditorUiHelper.clearFields(textFields, genreComboBox);
        }
    }

    @FXML
    private void selectFile() {
        selectInputFile(btnSelectMediaFile,
                new FileChooser.ExtensionFilter("Media file", getSupportedTagEditorFormatsForFileChooser()), this::loadFile);
    }

    @FXML
    private void chooseSaveDirectory() {
        selectOutputDirectory(btnChoiceDirForSave, audioProperties.getOutput(), audioProperties::setOutput, "Select directory for save media file");
    }

    @FXML
    private void submitAndDownload() {
        if (isNull(audioProperties.getSrcFile())) {
            ErrorLogger.error("Media file not selected!");
            return;
        }

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                updateProgress(10, 100);
                switch (selectedMediaType) {
                    case AUDIO -> {
                        Map<FieldKey, String> tags = collectTags();
                        String imagePath = nonNull(audioProperties.getPathToImage())
                                ? audioProperties.getPathToImage().getPath() : null;
                        AudioPreprocessing.applyTags(audioProperties.getSrcFile(), tags, imagePath);
                    }
                    case IMAGE -> PhotoTagPreprocessing.applyTags(audioProperties.getSrcFile(), collectMediaTags());
                    default    -> VideoTagPreprocessing.applyTags(audioProperties.getSrcFile(), collectMediaTags());
                }

                updateProgress(100, 100);
                return true;
            }
        };

        executeMediaTask(task);
    }

    @FXML
    private void selectMultipleFile() {
        Stage stage = (Stage) btnChoiceDirForSave.getScene().getWindow();

        initChoiceDialog(stage).showAndWait().ifPresent(type ->
                directoryChooser(stage, audioProperties.getOutput(), "Select directory")
                        .ifPresent(dir -> {
                            disableControls();
                            chosenDir = dir;
                            audioProperties.setOutput(dir);
                            ErrorLogger.info(getClass(), "Batch processing directory selected: " + dir.getAbsolutePath());
                            List<String> formats = getFormatsForMediaType(type);

                            Task<List<DetailsAudioFile>> task = createTask(formats);

                            Alerts.showProgressDialog(stage, task, "Loading files", "Reading " + type.getDisplayName().toLowerCase(Locale.ROOT) + " from directory");
                            executeMediaTask(task);
                        }));
    }

    @FXML
    private void changeIcon() {
        if (selectedMediaType != AUDIO) {
            Alerts.alertDialog(Alert.AlertType.INFORMATION,
                    "Audio file not selected!",
                    "Audio file not selected!",
                    "First select audio file!");
            return;
        }

        selectInputFile(btnChangeIcon,
                new FileChooser.ExtensionFilter("Image file", SupportedFormat.getSupportedImageFormatsForFileChooser()), this::loadIcon);
    }

    @Override
    protected void lockUI() {
        toggleUI(true);
    }

    @Override
    protected void unlockUI() {
        toggleUI(false);
        updateTagFieldAvailability();
    }

    @Override
    protected void disableControls() {
        toggleControls(true);
    }

    @Override
    protected void enableControls() {
        toggleControls(false);
        updateTagFieldAvailability();
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        if (result instanceof List<?> list) {
            if (!list.isEmpty() && list.getFirst() instanceof DetailsAudioFile) {
                List<DetailsAudioFile> audioFiles = list.stream()
                        .filter(DetailsAudioFile.class::isInstance)
                        .map(DetailsAudioFile.class::cast)
                        .toList();
                masterFile.setAll(audioFiles);
                ErrorLogger.info(getClass(), "Loaded " + audioFiles.size() + " files to table.");
                enableControls();
            }
            return;
        }

        super.handleTaskSuccess(result);
        if (result instanceof Boolean && Boolean.FALSE.equals(result)) {
            return;
        }
        ErrorLogger.info(getClass(), "Media tags changed successfully!");

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Tags saved successfully!", audioProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
            if (selectedMediaType == AUDIO) {
                AudioTagEditorUiHelper.updatePreview(audioProperties, imageViewPreview);

                DetailsAudioFile selected = tableViewAudio.getSelectionModel().getSelectedItem();
                if (nonNull(selected)) {
                    Map<FieldKey, String> newTags = collectTags();
                    selected.setTitle(newTags.get(FieldKey.TITLE));
                    selected.setArtist(newTags.get(FieldKey.ARTIST));
                    selected.setAlbum(newTags.get(FieldKey.ALBUM));
                    selected.setAlbumArtist(newTags.get(FieldKey.ALBUM_ARTIST));
                    selected.setTrack(newTags.get(FieldKey.TRACK));
                    selected.setDiscNumber(newTags.get(FieldKey.DISC_NO));
                    selected.setYear(newTags.get(FieldKey.YEAR));
                    selected.setGenre(genreComboBox.getEditor().getText());
                    selected.setComment(newTags.get(FieldKey.COMMENT));
                    tableViewAudio.refresh();
                }
            }
        });
    }

    @Override
    protected void handleTaskFailure(@NonNull Throwable exception) {
        super.handleTaskFailure(exception);
        Platform.runLater(() -> {
            showErrorMessage(labelSuccess, "Error: " + exception.getMessage(), audioProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    private void loadIcon(File icon) {
        if (!validateSelectedFile(icon)) {
            return;
        }
        audioProperties.setPathToImage(icon);
        AudioTagEditorUiHelper.updatePreviewWithPath(audioProperties, imageViewPreview);
    }

    private void loadFile(File selectedFile) {
        if (!validateSelectedFile(selectedFile, true)) {
            return;
        }

        MediaType mediaType;
        try {
            mediaType = selectedMediaType(selectedFile);
        } catch (IllegalArgumentException exception) {
            ErrorLogger.warn(getClass(), exception.getMessage());
            showErrorMessage(labelSuccess, exception.getMessage(), audioProperties.getHideSuccessMessageTimer());
            return;
        }

        configureEditableFields(mediaType, getEditableTags(mediaType));
        enableControls();
        audioProperties.setSrcFile(selectedFile);
        labelSelectImageName.setText("Selected: " + selectedFile.getName());

        try {
            if (mediaType == AUDIO) {
                AudioPreprocessing.getIconMp3(audioProperties.getSrcFile())
                        .ifPresentOrElse(file -> AudioTagEditorUiHelper.setPreview(file, imageViewPreview),
                                () -> AudioTagEditorUiHelper.loadDefaultPreview(imageViewPreview));
                populateFields(AudioPreprocessing.getTags(selectedFile));
            } else {
                if (PhotoTagPreprocessing.supports(selectedFile)) {
                    populateMediaFields(PhotoTagPreprocessing.getTags(selectedFile));
                } else {
                    populateMediaFields(VideoTagPreprocessing.getTags(selectedFile));
                }
                AudioTagEditorUiHelper.loadDefaultPreview(imageViewPreview);
            }

        } catch (Exception e) {
            ErrorLogger.error("Failed to load tags: " + e.getMessage());
            showErrorMessage(labelSuccess, "Failed to load tags: " + e.getMessage(),
                    audioProperties.getHideSuccessMessageTimer());
        }

        markDropZoneLoaded(dropZone, textDragZone, selectedFile.getName());
    }

    private void initTextFieldFind() {
        yearField.setTextFormatter(new TextFormatter<>(getFilterYear()));
        ratingField.setTextFormatter(new TextFormatter<>(getFilterRating()));

        Timeline searchDebounce = createSearchDebounce();

        textFieldFindFile.textProperty().addListener((_, _, _) -> {
            searchDebounce.stop();
            searchDebounce.playFromStart();
        });
    }

    private void initTableViewAndScrollPane() {
        tableViewAudio.setTableMenuButtonVisible(false);
        tableViewAudio.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        var columns = tableViewAudio.getColumns();

        columns.forEach(column -> {
            if (nonNull(column.getId())) {
                column.setCellValueFactory(new PropertyValueFactory<>(column.getId()));
            }
            if (column.getPrefWidth() > 0) {
                column.setMinWidth(column.getPrefWidth());
            }
        });

        SetupScrollPane.configureTableHorizontalScroll(
                tableScrollPane, tableViewAudio, masterFile, columns, tableVerticalScrollBar, tableInternalVBarRef);

        tableViewAudio.getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> {
            if (nonNull(newValue)) {
                loadFile(new File(newValue.getPath()));
            }
        });
    }

    private void initLists() {
        tagTextFields = Map.ofEntries(
                e("title", titleField),
                e("artist", artistField),
                e("album", albumField),
                e("albumArtist", albumArtistField),
                e("composer", composerField),
                e("track", trackField),
                e("discNumber", discNumberField),
                e("comment", commentField),
                e("year", yearField),
                e("description", descriptionField),
                e("keywords", keywordsField),
                e("copyright", copyrightField),
                e("rating", ratingField)
        );
        tagLabels = Map.ofEntries(
                e("title", titleLabel),
                e("artist", artistLabel),
                e("album", albumLabel),
                e("albumArtist", albumArtistLabel),
                e("composer", composerLabel),
                e("track", trackLabel),
                e("discNumber", discNumberLabel),
                e("comment", commentLabel),
                e("year", yearLabel),
                e("description", descriptionLabel),
                e("keywords", keywordsLabel),
                e("copyright", copyrightLabel),
                e("rating", ratingLabel),
                e("genre", genreLabel)
        );
        originalTagLabelTexts = new HashMap<>();
        tagLabels.forEach((key, label) -> originalTagLabelTexts.put(key, label.getText()));

        textFields = new ArrayList<>(tagTextFields.values());
        textFields.add(textFieldFindFile);

        initTableViewAndScrollPane();

    }

    private @NonNull Timeline createSearchDebounce() {
        Timeline searchDebounce = new Timeline(new KeyFrame(Duration.millis(300), _ -> {
            String query = textFieldFindFile.getText().toLowerCase();
            filteredFile.setPredicate(file -> {
                if (isNull(file)) return true;
                if (query.isEmpty()) return true;
                return file.getFileName().toLowerCase().contains(query);
            });
            SetupScrollPane.fillPlaceholderRows(tableViewAudio, masterFile, tableScrollPane.getViewportBounds().getHeight());
        }));
        searchDebounce.setCycleCount(1);
        return searchDebounce;
    }

    private Map<String, String> collectMediaTags() {
        Map<String, String> tags = new LinkedHashMap<>();

        tagTextFields.forEach((key, field) -> {
            if (editableTagKeys.contains(key)) {
                tags.put(key, field.getText());
            }
        });

        if (editableTagKeys.contains("genre")) {
            tags.put("genre", genreComboBox.getEditor().getText());
        }

        return tags;
    }

    private @NonNull Map<FieldKey, String> collectTags() {
        Map<FieldKey, String> tags = new EnumMap<>(FieldKey.class);
        AUDIO_TAG_KEYS.forEach((key, fieldKey) -> tags.put(fieldKey, tagTextFields.get(key).getText()));

        String genre = genreComboBox.getEditor().getText();
        if (nonNull(genre) && !genre.isBlank()) {
            tags.put(FieldKey.GENRE, genre);
        }
        return tags;
    }

    private void populateFields(@NonNull Map<FieldKey, String> tags) {
        AUDIO_TAG_KEYS.forEach((key, fieldKey) -> tagTextFields.get(key).setText(tags.getOrDefault(fieldKey, "")));

        String rawYear = tags.getOrDefault(FieldKey.YEAR, "").replaceAll("\\D.*", "");
        yearField.setText(rawYear.length() > 4 ? rawYear.substring(0, 4) : rawYear);

        List.of(descriptionField, keywordsField, copyrightField, ratingField).forEach(TextInputControl::clear);
        AudioTagEditorUiHelper.setGenreValue(genreComboBox, tags.get(FieldKey.GENRE));
    }

    private void populateMediaFields(@NonNull Map<String, String> tags) {
        tagTextFields.forEach((key, field) -> field.setText(tags.getOrDefault(key, "")));
        AudioTagEditorUiHelper.setGenreValue(genreComboBox, tags.get("genre"));
    }

    private MediaType selectedMediaType(@NonNull File file) {
        if (isAudioFile(file)) {
            return AUDIO;
        }
        String fileName = file.getName().toLowerCase(Locale.ROOT);
        if (PhotoTagPreprocessing.supports(file)) {
            return IMAGE;
        }
        if (SupportedFormat.getAllSupportedVideoFormats().stream().anyMatch(fileName::endsWith)) {
            return VIDEO;
        }
        if (SupportedFormat.getAllSupportedImageFormats().stream().anyMatch(fileName::endsWith)) {
            throw new IllegalArgumentException("Photo tag editing supports JPEG and TIFF files only.");
        }
        throw new IllegalArgumentException("Unsupported media file: " + file.getName());
    }

    private Set<String> getEditableTags(MediaType mediaType) {
        return switch (mediaType) {
            case AUDIO -> AUDIO_EDITABLE_TAGS;
            case IMAGE -> PHOTO_EDITABLE_TAGS;
            case VIDEO -> VIDEO_EDITABLE_TAGS;
            default -> throw new IllegalArgumentException("Unsupported media type: " + mediaType);
        };
    }

    private static List<String> getSupportedTagEditorFormats() {
        List<String> formats = new ArrayList<>(SupportedFormat.getAllSupportedAudioFormats());
        formats.addAll(SupportedFormat.getAllSupportedVideoFormats());
        formats.addAll(List.of(".jpg", ".jpeg", ".tif", ".tiff"));
        return formats;
    }

    private static List<String> getSupportedTagEditorFormatsForFileChooser() {
        return getSupportedTagEditorFormats().stream()
                .map(format -> "*" + format)
                .toList();
    }

    private void configureEditableFields(MediaType mediaType, Set<String> editableTags) {
        selectedMediaType = mediaType;
        editableTagKeys = editableTags;
        updateTagFieldAvailability();
    }

    private void updateTagFieldAvailability() {
        if (isNull(tagTextFields) || isNull(tagLabels)) {
            return;
        }
        tagTextFields.forEach((key, field) -> {
            boolean editable = editableTagKeys.contains(key);
            field.setDisable(!editable);
            updateLabelStatus(key, editable);
        });

        boolean genreEditable = editableTagKeys.contains("genre");
        genreComboBox.setDisable(!genreEditable);
        updateLabelStatus("genre", genreEditable);

        btnChangeIcon.setDisable(selectedMediaType != AUDIO);
    }

    private void updateLabelStatus(String key, boolean editable) {
        Label label = tagLabels.get(key);
        if (nonNull(label)) {
            String original = originalTagLabelTexts.get(key);
            label.setText(editable || selectedMediaType == MediaType.NONE
                    ? original
                    : original + " (not editable for " + selectedMediaType.getTypeCategory().toLowerCase() + ")");
        }
    }

    private ChoiceDialog<BatchTargetType> initChoiceDialog(Stage stage) {
        ChoiceDialog<BatchTargetType> dialog = new ChoiceDialog<>(
                BatchTargetType.ALL,
                BatchTargetType.values()
        );

        dialog.initOwner(stage);
        dialog.setTitle("Batch File");
        dialog.setHeaderText("Choose files to search for");
        dialog.setContentText("Media type:");
        dialog.setGraphic(Alerts.createQuestionGraphic());
        dialog.setOnShown(_ -> dialog.getDialogPane().setGraphic(Alerts.createQuestionGraphic()));

        return dialog;
    }

    private Task<List<DetailsAudioFile>> createTask(List<String> formats) {
        return new Task<>() {
            @Override
            protected List<DetailsAudioFile> call() {
                return TableViewHelper.loadFilesFromDir(
                        chosenDir.toPath(),
                        formats,
                        (processed, total) -> {
                            updateProgress(processed, Math.max(total, 1));
                            updateMessage("Read files: " + processed + " / " + total);
                        },
                        this::isCancelled
                );
            }
        };
    }

    private UnaryOperator<TextFormatter.Change> getFilterYear() {
        return change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*") && newText.length() <= 4) {
                return change;
            }
            return null;
        };
    }

    private UnaryOperator<TextFormatter.Change> getFilterRating() {
        return change -> {
            String newText = change.getControlNewText();
            if (newText.matches("[0-5]") && newText.length() <= 1) {
                return change;
            }
            return null;
        };
    }

    private List<String> getFormatsForMediaType(BatchTargetType type) {
        return switch (type) {
            case PHOTOS -> List.of(".jpg", ".jpeg", ".tif", ".tiff");
            case VIDEOS -> SupportedFormat.getAllSupportedVideoFormats();
            case AUDIO -> SupportedFormat.getAllSupportedAudioFormats();
            case ALL -> getSupportedTagEditorFormats();
        };
    }

    private static <K, V> Map.Entry<K, V> e(K key, V value) {
        return Map.entry(key, value);
    }

    private void initComboBoxes() {
        genreComboBox.getItems().addAll(
                "Rock", "Pop", "Jazz", "Classical", "Hip Hop", "Electronic", "Metal", "Blues", "Country", "Reggae", "Other"
        );
        updateGenrePromptStyle(isNull(genreComboBox.getValue()));
        genreComboBox.valueProperty().addListener((_, _, newValue) ->
                updateGenrePromptStyle(isNull(newValue)));
    }

    private void updateGenrePromptStyle(boolean promptVisible) {
        if (promptVisible) {
            if (!genreComboBox.getStyleClass().contains("prompt-visible")) {
                genreComboBox.getStyleClass().add("prompt-visible");
            }
        } else {
            genreComboBox.getStyleClass().remove("prompt-visible");
        }
    }

    private void toggleUI(boolean disabled) {
        toggleControls(disabled);
        setControlsDisabled(disabled, btnSelectMediaFile, btnChoiceDirForSave, btnSelectMultipleFile);
        dropZone.setDisable(disabled);
    }

    private void toggleControls(boolean disabled) {
        setControlsDisabled(disabled, textFieldFindFile, genreComboBox, btnSaveChanges, btnChangeIcon, btnReset);
        tagTextFields.values().forEach(field -> field.setDisable(disabled));
    }
}
