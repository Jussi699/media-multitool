package media_multitool;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import model.converterImage.ConvertImageTask;
import model.properties.ImageProperties;
import model.logger.ErrorLogger;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.select.SelectFile;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.StackPane;
import model.utility.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

import viewHelp.Alerts;
import viewHelp.Cells;

import static viewHelp.Message.*;
import static model.utility.Util.*;
import static model.converterImage.UsefulMethods.*;

public class ConverterImageController extends AbstractMediaController {
    private final String ICO_PLACEHOLDER = "to ICO";
    private final ImageProperties imageProperties = new ImageProperties();
    private File path_folderBatchProcessing;
    private List<File> filesToProcess = new ArrayList<>();

    @FXML private Button btnSelectPhoto, btnChoiceFolderForSave, btnSelectBatchFileProcessing;
    @FXML private Label labelSelectImage, textDragZone, labelPreviewPlaceholder;
    @FXML private ToggleButton btnToSVG, btnToWEBM, btnToJPEG, btnToPNG, btnToTIFF, btnToBMP, btnToPPM, btnToPGM, btnToPAM;
    @FXML private Slider imageScaleSlider;
    @FXML private ComboBox<String> comboBoxIcoSize;
    @FXML private ScrollPane scrollPanePhoto;
    @FXML private ImageView imageViewPhoto;
    @FXML private StackPane dropZone, imageContainer;

    private List<ToggleButton> listBtn;

    @FXML
    public void initialize() {
        listBtn = List.of(
                btnToSVG, btnToWEBM, btnToJPEG, btnToPNG, btnToTIFF, btnToBMP, btnToPPM, btnToPGM, btnToPAM
        );

        imageProperties.setOutput(getSavedPath());

        imageScaleSlider.setMin(1.0);
        imageScaleSlider.setMax(5.0);
        imageScaleSlider.setValue(1.0);

        comboBoxIcoSize.getItems().addAll("16", "32", "64", "128", "256", "512", "768");
        comboBoxIcoSize.setValue(ICO_PLACEHOLDER);

        imageViewPhoto.scaleXProperty().bind(imageScaleSlider.valueProperty());
        imageViewPhoto.scaleYProperty().bind(imageScaleSlider.valueProperty());

        imageScaleSlider.valueProperty().addListener((_, _, newVal) -> {
            updateImageContainerSize(newVal.doubleValue());
            Platform.runLater(this::adjustScrollBarToCenter);
        });

        scrollPanePhoto.viewportBoundsProperty().addListener((_, _, _) -> updateImageSize());
        imageViewPhoto.imageProperty().addListener((_, _, _) -> updateImageSize());

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);

        Cells.comboBoxIcoSizeButtonCell(comboBoxIcoSize, ICO_PLACEHOLDER);
        Cells.comboBoxIcoSizeSetCellFactory(comboBoxIcoSize, ICO_PLACEHOLDER);

        comboBoxIcoSize.getSelectionModel().selectedItemProperty().addListener((_, _, newVal) -> {
            if (newVal != null && !newVal.equals(ICO_PLACEHOLDER) && imageViewPhoto.getImage() != null) {
                if (imageProperties.getImage() != null && imageProperties.getImage().getName().toLowerCase().endsWith(".ico")) {
                    try {
                        double size = Double.parseDouble(newVal);
                        imageViewPhoto.setFitHeight(size);
                        imageViewPhoto.setFitWidth(size);
                        updateImageContainerSize(imageScaleSlider.getValue());
                        Platform.runLater(this::adjustScrollBarToCenter);
                    } catch (NumberFormatException e) {
                        Alerts.alertDialog(Alert.AlertType.WARNING, "Error", "Format", "Invalid size value!");
                    }
                }
            }
        });
    }

    @Override
    protected void lockUI() {
        lockButtonFormat();
        btnSelectPhoto.setDisable(true);
        btnChoiceFolderForSave.setDisable(true);
        btnSelectBatchFileProcessing.setDisable(true);
        if (btnReset != null) btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        unlockButtonFormat();
        btnSelectPhoto.setDisable(false);
        btnChoiceFolderForSave.setDisable(false);
        btnSelectBatchFileProcessing.setDisable(false);
        if (btnReset != null) btnReset.setDisable(false);
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        super.handleTaskSuccess(result);
        if (Boolean.TRUE.equals(result)) {
            Platform.runLater(() -> showSuccessText(labelSuccess, "Conversion successful!", imageProperties.getHideSuccessMessageTimer()));
        } else {
            imageProperties.getHideSuccessMessageTimer().playFromStart();
        }
    }

    @Override
    protected void handleTaskCancelled() {
        super.handleTaskCancelled();
        imageProperties.getHideSuccessMessageTimer().playFromStart();
    }

    @Override
    protected void handleTaskFailure(Throwable exception) {
        super.handleTaskFailure(exception);
        imageProperties.getHideSuccessMessageTimer().playFromStart();
    }

    @FXML
    public void btnChoiceFolderForSave() {
        Stage stage = getStage(btnChoiceFolderForSave);
        directoryChooser(stage, imageProperties.getOutput(), "Select directory for save image")
                .ifPresent(imageProperties::setOutput);
    }

    @FXML
    private void onActionBtnBatchFileProcessing() {
        Stage stage = getStage(btnSelectBatchFileProcessing);
        directoryChooser(stage, path_folderBatchProcessing, "Select directory with image")
                .ifPresent(selectedPath -> {
                    path_folderBatchProcessing = selectedPath;

                    List<File> result = Preparation.getFilesFromFolder(path_folderBatchProcessing, Global.getAllSupportedImageFormats());

                    filesToProcess = new ArrayList<>(result);

                    if (filesToProcess.isEmpty()) {
                        Alerts.alertDialog(Alert.AlertType.WARNING, "No matching files found", "No matching files found",
                                "No matching files were found in the selected directory.\nPerhaps it only contains unsupported images!");
                        return;
                    }

                    for (File s : filesToProcess) ErrorLogger.info("User selected file (image): " + s.getName());

                    imageProperties.setImage(filesToProcess.getFirst());
                    labelSelectImage.setText("Current file in list: " + imageProperties.getImage().getName());

                    try {
                        Optional<BufferedImage> biOpt = readPreviewImage(imageProperties.getImage());
                        if (biOpt.isEmpty()) {
                            ErrorLogger.warn("Failed to read preview for file: " + imageProperties.getImage().getName());
                            Alerts.alertDialog(Alert.AlertType.WARNING, "Error", "Format", "Unsupported image format!");
                            return;
                        }

                        Image fxImage = SwingFXUtils.toFXImage(biOpt.get(), null);
                        imageScaleSlider.setValue(1.0);
                        imageViewPhoto.setImage(fxImage);
                        updateImageSize();

                        if (labelPreviewPlaceholder != null) {
                            labelPreviewPlaceholder.setVisible(false);
                        }
                        
                        if (textDragZone != null) {
                            textDragZone.setText("Batch: " + filesToProcess.size() + " files");
                        }
                        if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
                            dropZone.getStyleClass().add("drop-zone-filled");
                        }

                        ErrorLogger.info("Preview loaded successfully for: " + imageProperties.getImage().getName());
                    } catch (IOException e) {
                        ErrorLogger.log(122, ErrorLogger.Level.ERROR, "IO | File error while loading preview", e);
                        Alerts.alertDialog(Alert.AlertType.WARNING, "Error", "IO", "File error!");
                    }
                });
    }

    @FXML
    private void onActionChoiceIcoSize() {
        String selected = comboBoxIcoSize.getValue();

        if (selected == null || selected.equals(ICO_PLACEHOLDER)) return;

        imageProperties.setSizeIcoImage(Integer.parseInt(selected));
        for(ToggleButton tb : listBtn) {
            tb.setSelected(false);
        }

        imageProperties.setTypeImage("ico");
    }

    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectImage, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPhoto, progressBar, true
        );

        Util.reset(imageProperties, ctx, "Selected image file: none");

        path_folderBatchProcessing = null;
        filesToProcess.clear();

        for(ToggleButton tb : listBtn) {
            tb.setSelected(false);
        }

        comboBoxIcoSize.setValue(ICO_PLACEHOLDER);
        imageScaleSlider.setValue(1.0);
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectPhoto.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Images", Global.getAllSupportedImageFormatsForFileChooser()),
                "Choice image"
        ).ifPresent(this::loadImage);
    }

    private void loadImage(File file) {
        imageProperties.setImage(file);
        filesToProcess.clear();

        ErrorLogger.info("User selected file (image): " + imageProperties.getImage().getAbsolutePath());
        labelSelectImage.setText("Select image: " + imageProperties.getImage().getName());

        try {
            Optional<BufferedImage> biOpt = readPreviewImage(imageProperties.getImage());
            if (biOpt.isEmpty()) {
                ErrorLogger.warn("Failed to read preview for file: " + imageProperties.getImage().getName());
                Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Format", "Unsupported image format!");
                return;
            }

            Image fxImage = SwingFXUtils.toFXImage(biOpt.get(), null);
            imageScaleSlider.setValue(1.0);
            imageViewPhoto.setImage(fxImage);

            updateImageSize();

            labelPreviewPlaceholder.setVisible(false);

            textDragZone.setText("Selected: " + file.getName());

            if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
                dropZone.getStyleClass().add("drop-zone-filled");
            }
            
            ErrorLogger.info("Preview loaded successfully for: " + imageProperties.getImage().getName());
        } catch (IOException e) {
            ErrorLogger.log(107, ErrorLogger.Level.ERROR, "IO | File error while loading preview", e);
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "IO", "File error!");
        }
    }

    @FXML
    public void handleDragOver(DragEvent e) {
        DragDropped.handleDragOver(e, Global.getAllSupportedImageFormats(), dropZone);
    }

    @FXML
    public void handleDragDropped(DragEvent e) {
        File droppedFile = DragDropped.handleDragDropped(e, dropZone, textDragZone);
        if (droppedFile != null) {
            loadImage(droppedFile);
        }
    }

    private void updateImageSize() {
        if (imageViewPhoto.getImage() != null) {
            if (imageScaleSlider.getValue() == 1.0) {
                double viewPortWidth = scrollPanePhoto.getViewportBounds().getWidth();
                double viewPortHeight = scrollPanePhoto.getViewportBounds().getHeight();

                imageViewPhoto.setFitWidth(viewPortWidth - 20);
                imageViewPhoto.setFitHeight(viewPortHeight - 20);
                imageViewPhoto.setPreserveRatio(true);
            }
        }
    }

    private void adjustScrollBarToCenter() {
            scrollPanePhoto.setHvalue(0.5);
            scrollPanePhoto.setVvalue(0.5);
    }

    private void updateImageContainerSize(double zoom) {
        double newWidth = imageViewPhoto.getFitWidth() * zoom;
        double newHeight = imageViewPhoto.getFitHeight() * zoom;

        imageContainer.setMinWidth(newWidth);
        imageContainer.setMinHeight(newHeight);
        imageContainer.setPrefWidth(newWidth);
        imageContainer.setPrefHeight(newHeight);
    }

    @FXML
    public void submitAndDownload() {
        if (imageProperties.getImage() == null || imageProperties.getOutput() == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "File missing!",
                    "Select image first.");
            return;
        }

        if (imageProperties.getTypeImage() == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "File missing!",
                    "Select photo format!");
            return;
        }

        List<File> snapshot = filesToProcess.isEmpty() ? List.of(imageProperties.getImage()) : new ArrayList<>(filesToProcess);
        
        ConvertImageTask task = new ConvertImageTask(
                snapshot, 
                imageProperties.getOutput(), 
                imageProperties.getTypeImage(), 
                imageProperties.getSizeIcoImage()
        );

        task.messageProperty().addListener((_, _, newVal) -> Platform.runLater(() -> labelSuccess.setText(newVal)));
        
        executeMediaTask(task);
    }

    private void selectRasterFormat(String format) {
        imageProperties.setTypeImage(format);
        btnToPNG.setSelected("png".equals(format));
        btnToJPEG.setSelected("jpeg".equals(format));
        btnToWEBM.setSelected("webp".equals(format));
        btnToTIFF.setSelected("tif".equals(format));
        btnToBMP.setSelected("bmp".equals(format));
        btnToPPM.setSelected("ppm".equals(format));
        btnToPGM.setSelected("pgm".equals(format));
        btnToPAM.setSelected("pam".equals(format));
        btnToSVG.setSelected("svg".equals(format));

        comboBoxIcoSize.setValue(ICO_PLACEHOLDER);
    }

    @FXML
    private void onActionBtnToPNG() {
        selectRasterFormat("png");
    }

    @FXML
    private void onActionBtnToJPEG() {
        selectRasterFormat("jpeg");
    }

    @FXML
    public void onActionBtnToWEBM() {
        selectRasterFormat("webp");
    }

    @FXML
    public void onActionBtnToTIFF() {
        selectRasterFormat("tif");
    }

    @FXML
    public void onActionBtnToBMP() {
        selectRasterFormat("bmp");
    }

    @FXML
    public void onActionBtnToPPM() {
        selectRasterFormat("ppm");
    }

    @FXML
    public void onActionBtnToPAM() {
        selectRasterFormat("pam");
    }

    @FXML
    public void onActionBtnToPGM() {
        selectRasterFormat("pgm");
    }

    @FXML
    private void onActionBtnToSVG() {
        selectRasterFormat("svg");
    }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Image Converter",
                """
                        How to use:
                        1. Select an image file using 'Select image'.
                        3. (Optional) Select the directory from which you want to convert all photos
                        3. (Optional) Choose a directory for saving the output.
                        4. Select the target format (PNG, JPEG, etc.).
                        5. Click 'Convert and Download'.
                        
                        You can also zoom the preview using the slider or mouse wheel.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    private void lockButtonFormat() {
        for(ToggleButton tb : listBtn) {
            tb.setDisable(true);
        }
        comboBoxIcoSize.setDisable(true);
    }

    private void unlockButtonFormat() {
        for(ToggleButton tb : listBtn) {
            tb.setDisable(false);
        }
        comboBoxIcoSize.setDisable(false);
    }
}
