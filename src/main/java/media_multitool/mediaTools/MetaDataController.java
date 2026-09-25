package media_multitool.mediaTools;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import model.logger.ErrorLogger;
import model.metadata.MetadataEntry;
import model.metadata.MetadataHelper;
import model.properties.MediaProperties;
import model.properties.MetaDataProperties;
import model.select.SelectFile;
import model.utility.Global;
import model.utility.PathWorker;
import model.utility.ResetContext;
import viewHelp.Alerts;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Cells.setCellFactoryTableColumn;
import static viewHelp.Message.setupClearMessageTimer;
import static viewHelp.Message.showErrorMessage;
import static viewHelp.Message.showSuccessText;

public class MetaDataController extends AbstractMediaController {
    private final MetaDataProperties metaDataProperties = new MetaDataProperties();

    @FXML private TableView<MetadataEntry> tableViewMetadata;
    @FXML private TableColumn<MetadataEntry, String> colCategory, colProperty, colValue;
    @FXML private TableColumn<MetadataEntry, Boolean> colCanDelete;
    @FXML private ScrollBar metadataVerticalScrollBar;

    @FXML private TextField txtSearch;
    @FXML private StackPane dropZone;
    @FXML private Label textDragZone, labelSelectFile;

    @FXML private Button btnSelectFile, btnChoiceFolderForSaveFile, btnCopyAll, btnDeleteTag, btnRemoveAllMetadata;

    private final ObservableList<MetadataEntry> masterMetadataList = FXCollections.observableArrayList();
    private FilteredList<MetadataEntry> filteredMetadataList;
    private List<Control> listControls;
    private ScrollBar internalMetadataScrollBar;

    @Override
    protected MediaProperties getProperties() {
        return metaDataProperties;
    }

    @FXML
    public void initialize() {
        listControls = List.of(btnCopyAll, btnDeleteTag, btnRemoveAllMetadata, btnReset);
        metaDataProperties.setOutput(getSavedPath());

        setupTableColumns();
        setupContextMenu();
        setupSearchFilter();

        setupClearMessageTimer(labelSuccess, progressBar, metaDataProperties.getHideSuccessMessageTimer(), true);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedMetadataFormats(), this::loadFile);

        Platform.runLater(() -> {
            setupExternalVerticalScrollBar();
            attachScrollBarListener();
        });
    }

    private void setupTableColumns() {
        tableViewMetadata.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableViewMetadata.setFixedCellSize(28.0);

        colCategory.setCellValueFactory(data -> data.getValue().categoryProperty());
        colProperty.setCellValueFactory(data -> data.getValue().keyProperty());
        colValue.setCellValueFactory(data -> data.getValue().valueProperty());

        colCanDelete.setCellValueFactory(data -> data.getValue().canDeleteProperty());
        setCellFactoryTableColumn(colCanDelete);

        filteredMetadataList = new FilteredList<>(masterMetadataList, _ -> true);
        tableViewMetadata.setItems(filteredMetadataList);

        tableViewMetadata.setRowFactory(_ -> {
            TableRow<MetadataEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    MetadataEntry rowData = row.getItem();
                    copyToClipboard(rowData.getValue());
                    showSuccessMessage("Copied to clipboard: " + rowData.getValue());
                }
            });
            return row;
        });

        tableViewMetadata.skinProperty().addListener((_, _, newSkin) -> {
            if (newSkin != null) {
                Platform.runLater(() -> {
                    setupExternalVerticalScrollBar();
                    attachScrollBarListener();
                });
            }
        });
    }

    private void setupExternalVerticalScrollBar() {
        if (metadataVerticalScrollBar == null || tableViewMetadata == null) {
            return;
        }

        ScrollBar currentInternalBar = null;
        for (Node node : tableViewMetadata.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar scrollBar
                    && scrollBar.getOrientation() == Orientation.VERTICAL) {
                currentInternalBar = scrollBar;
                break;
            }
        }
        if (currentInternalBar == null) {
            return;
        }

        currentInternalBar.setVisible(true);
        currentInternalBar.setManaged(true);
        currentInternalBar.setOpacity(0);
        currentInternalBar.setPrefWidth(0);
        currentInternalBar.setMinWidth(0);
        currentInternalBar.setMaxWidth(0);

        if (internalMetadataScrollBar != currentInternalBar) {
            if (internalMetadataScrollBar != null) {
                metadataVerticalScrollBar.minProperty().unbind();
                metadataVerticalScrollBar.maxProperty().unbind();
                metadataVerticalScrollBar.visibleAmountProperty().unbind();
                metadataVerticalScrollBar.valueProperty()
                        .unbindBidirectional(internalMetadataScrollBar.valueProperty());
            }

            internalMetadataScrollBar = currentInternalBar;
            metadataVerticalScrollBar.minProperty().bind(internalMetadataScrollBar.minProperty());
            metadataVerticalScrollBar.maxProperty().bind(internalMetadataScrollBar.maxProperty());
            metadataVerticalScrollBar.visibleAmountProperty()
                    .bind(internalMetadataScrollBar.visibleAmountProperty());
            metadataVerticalScrollBar.valueProperty()
                    .bindBidirectional(internalMetadataScrollBar.valueProperty());

            internalMetadataScrollBar.maxProperty()
                    .addListener((_, _, _) -> updateExternalVerticalScrollBarVisibility());
            internalMetadataScrollBar.visibleAmountProperty()
                    .addListener((_, _, _) -> updateExternalVerticalScrollBarVisibility());
        }

        if (!metadataVerticalScrollBar.minHeightProperty().isBound()) {
            metadataVerticalScrollBar.minHeightProperty().bind(tableViewMetadata.heightProperty());
            metadataVerticalScrollBar.prefHeightProperty().bind(tableViewMetadata.heightProperty());
            metadataVerticalScrollBar.maxHeightProperty().bind(tableViewMetadata.heightProperty());
        }
        updateExternalVerticalScrollBarVisibility();
    }

    private void updateExternalVerticalScrollBarVisibility() {
        if (metadataVerticalScrollBar == null || internalMetadataScrollBar == null) {
            return;
        }

        if (masterMetadataList.isEmpty()) {
            metadataVerticalScrollBar.setVisible(false);
            metadataVerticalScrollBar.setManaged(false);
            return;
        }

        boolean hasVerticalOverflow = internalMetadataScrollBar.getMax()
                > internalMetadataScrollBar.getMin();
        metadataVerticalScrollBar.setVisible(hasVerticalOverflow);
        metadataVerticalScrollBar.setManaged(hasVerticalOverflow);
    }

    private void attachScrollBarListener() {
        if (tableViewMetadata == null) return;
        for (Node node : tableViewMetadata.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar scrollBar && scrollBar.getOrientation() == Orientation.VERTICAL) {
                scrollBar.visibleProperty().addListener((_, _, _) -> alignTableColumns());
                break;
            }
        }
    }

    private void alignTableColumns() {
        Platform.runLater(() -> Platform.runLater(() -> {
            if (tableViewMetadata == null) return;
            setupExternalVerticalScrollBar();
            updateExternalVerticalScrollBarVisibility();
            var policy = tableViewMetadata.getColumnResizePolicy();
            if (policy != null) {
                policy.call(new TableView.ResizeFeatures<>(tableViewMetadata, null, 0.0));
            }
            tableViewMetadata.requestLayout();
            Node headerRow = tableViewMetadata.lookup("TableHeaderRow");
            if (headerRow != null) {
                requestLayoutRecursively(headerRow);
            }
        }));
    }

    private void requestLayoutRecursively(Node node) {
        if (node instanceof Parent parent) {
            parent.requestLayout();
            for (Node child : parent.getChildrenUnmodifiable()) {
                requestLayoutRecursively(child);
            }
        }
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem copyValueItem = new MenuItem("Copy Value");
        copyValueItem.setOnAction(_ -> {
            MetadataEntry selected = tableViewMetadata.getSelectionModel().getSelectedItem();
            if (selected != null) {
                copyToClipboard(selected.getValue());
                showSuccessMessage("Value copied to clipboard!");
            }
        });

        MenuItem copyKeyValueItem = new MenuItem("Copy Tag & Value");
        copyKeyValueItem.setOnAction(_ -> {
            MetadataEntry selected = tableViewMetadata.getSelectionModel().getSelectedItem();
            if (selected != null) {
                copyToClipboard(selected.getKey() + ": " + selected.getValue());
                showSuccessMessage("Tag & value copied to clipboard!");
            }
        });

        MenuItem deleteTagItem = new MenuItem("Delete This Tag");
        deleteTagItem.setOnAction(_ -> onActionDeleteSelectedTag());

        contextMenu.getItems().addAll(copyValueItem, copyKeyValueItem, new SeparatorMenuItem(), deleteTagItem);
        tableViewMetadata.setContextMenu(contextMenu);
    }

    private void setupSearchFilter() {
        txtSearch.textProperty().addListener((_, _, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                filteredMetadataList.setPredicate(_ -> true);
            } else {
                String lower = newVal.toLowerCase().trim();
                filteredMetadataList.setPredicate(entry ->
                        entry.getCategory().toLowerCase().contains(lower) ||
                        entry.getKey().toLowerCase().contains(lower) ||
                        entry.getValue().toLowerCase().contains(lower)
                );
            }
            alignTableColumns();
        });
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Media & Documents (*.jpg, *.mp4, *.mp3, *.pdf, ...)",
                        Global.getSupportedMetadataFormatsForFileChooser())).ifPresent(this::loadFile);
    }

    @FXML
    public void onChoiceFolderForSaveFile() {
        selectOutputDirectory(btnChoiceFolderForSaveFile, metaDataProperties.getOutput(),
                metaDataProperties::setOutput, "Select directory for saving cleaned files");
    }

    @FXML
    public void onActionCopyAll() {
        if (metaDataProperties.getCurrentFile() == null || masterMetadataList.isEmpty()) {
            return;
        }

        String report = MetadataHelper.exportMetadataToText(metaDataProperties.getCurrentFile(), masterMetadataList);
        copyToClipboard(report);
        showSuccessMessage("All metadata copied to clipboard!");
    }

    @FXML
    public void onActionDeleteSelectedTag() {
        MetadataEntry selected = tableViewMetadata.getSelectionModel().getSelectedItem();
        File currentFile = metaDataProperties.getCurrentFile();

        if (selected == null || currentFile == null) {
            Alerts.alertDialog(Alert.AlertType.INFORMATION, "Selection", "No tag selected",
                    "Please select a metadata tag from the table to delete.");
            return;
        }

        if (!selected.isCanDelete()) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Not Editable", "Cannot delete property",
                    "The selected property ('" + selected.getKey() + "') is a technical file property and cannot be deleted individually. " +
                            "Use 'Remove All Metadata' to strip file metadata.");
            return;
        }

        boolean success = MetadataHelper.deleteSingleTag(currentFile, selected, metaDataProperties.getOutput());
        if (success) {
            showSuccessMessage("Tag '" + selected.getKey() + "' deleted successfully!");
            reloadCurrentFileMetadata();
        } else {
            showErrorMessage(labelSuccess, "Failed to delete tag '" + selected.getKey() + "'.", metaDataProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        }
    }

    @FXML
    public void onActionRemoveAllMetadata() {
        File currentFile = metaDataProperties.getCurrentFile();
        if (currentFile == null) {
            return;
        }

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                updateProgress(10, 100);
                File cleanFile = MetadataHelper.removeAllMetadata(currentFile, metaDataProperties.getOutput());
                updateProgress(100, 100);
                return cleanFile;
            }
        };

        executeMediaTask(task);
        labelSuccess.setManaged(true);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.FALSE.equals(result) || result == null) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info("Metadata stripped successfully! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Clean file saved to: " + outputFile.getName(), metaDataProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @Override
    protected void handleTaskFailure(Throwable exception) {
        super.handleTaskFailure(exception);
        Platform.runLater(() -> {
            showErrorMessage(labelSuccess, "Error: " + exception.getMessage(), metaDataProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, null,
                dropZone, null, progressBar, true, "photo, PDF, video or audio file"
        );
        reset(metaDataProperties, ctx, "Selected file: none");

        masterMetadataList.clear();
        txtSearch.clear();
        if (metadataVerticalScrollBar != null) {
            metadataVerticalScrollBar.setVisible(false);
            metadataVerticalScrollBar.setManaged(false);
        }
        disableControls();
        alignTableColumns();
    }

    private void loadFile(File selectedFile) {
        if (selectedFile == null || !selectedFile.exists()) {
            return;
        }

        enableControls();
        metaDataProperties.setCurrentFile(selectedFile);
        PathWorker.saveInputPath(selectedFile);

        labelSelectFile.setText("Selected: " + selectedFile.getName());

        if (textDragZone != null) {
            textDragZone.setText("Selected: " + selectedFile.getName());
        }
        if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }

        reloadCurrentFileMetadata();
    }

    private void reloadCurrentFileMetadata() {
        File current = metaDataProperties.getCurrentFile();
        if (current == null) return;

        List<MetadataEntry> entries = MetadataHelper.extractMetadata(current);
        masterMetadataList.setAll(entries);
        txtSearch.clear();

        alignTableColumns();
    }

    private void copyToClipboard(String text) {
        if (text == null) return;
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    private void showSuccessMessage(String message) {
        Platform.runLater(() -> {
            showSuccessText(labelSuccess, message, metaDataProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Media Metadata Viewer & Cleaner",
                "How to use:",
                """
                        1. Select or Drag & Drop any file (Photo, PDF, Video, Audio).
                        2. View all technical and embedded metadata in the table.
                        3. The 'Del' column indicates if a property can be deleted manually (✓) or is read-only (✕).
                        4. Use the Search field to quickly filter properties or values.
                        5. Double-click or Right-click any row to copy values or delete specific tags.
                        6. Click 'Copy All Metadata' to export everything to clipboard.
                        7. Click 'Remove All Metadata' to strip all metadata and save a clean copy of the file.
                        """
        );
    }

    @Override
    protected void lockUI() {
        Stream.of(btnSelectFile, btnChoiceFolderForSaveFile, btnReset, btnCopyAll, btnDeleteTag, btnRemoveAllMetadata)
                .forEach(btn -> btn.setDisable(true));
    }

    @Override
    protected void unlockUI() {
        Stream.of(btnSelectFile, btnChoiceFolderForSaveFile, btnReset, btnCopyAll, btnDeleteTag, btnRemoveAllMetadata)
                .forEach(btn -> btn.setDisable(false));
    }

    @Override
    protected void disableControls() {
        listControls.forEach(c -> c.setDisable(true));
    }

    @Override
    protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));
    }
}
