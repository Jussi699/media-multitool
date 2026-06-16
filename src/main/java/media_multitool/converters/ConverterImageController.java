package media_multitool.converters;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import media_multitool.AbstractMediaController;
import model.converterImage.ConvertImageTask;
import model.properties.ImageProperties;
import model.logger.ErrorLogger;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.properties.MediaProperties;
import model.select.SelectFile;
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
    private final ImageProperties imageProperties = new ImageProperties();
    private static final ToggleGroup toggleGroup = new ToggleGroup();

    private File path_folderBatchProcessing;
    private List<File> filesToProcess = new ArrayList<>();

    @FXML private Button btnSelectFile, btnChoiceFolderForSaveFile, btnSelectBatchFileProcessing, btnSubmit;
    @FXML private Label labelSelectFile, textDragZone, labelPreviewPlaceholder;
    @FXML private ToggleButton btnToSVG, btnToWEBP, btnToJPEG, btnToPNG, btnToTIFF, btnToBMP, btnToPPM, btnToPGM, btnToPAM;
    @FXML private ComboBox<String> comboBoxIcoSize;
    @FXML private ImageView imageViewPreview;
    @FXML private StackPane dropZone, previewContainer;

    private List<Control> listControls;

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML
    public void initialize() {
        List<ToggleButton> listToggleBtn = List.of(
                btnToSVG, btnToWEBP, btnToJPEG, btnToPNG, btnToTIFF, btnToBMP, btnToPPM, btnToPGM, btnToPAM
        );

        listControls = new ArrayList<>(listToggleBtn);
        listControls.addAll(List.of(comboBoxIcoSize, btnSubmit));

        listToggleBtn.forEach(btn -> btn.setToggleGroup(toggleGroup));

        imageProperties.setOutput(getSavedPath());

        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);

        initComboBoxes();

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadImage);
    }

    private void initComboBoxes() {
        comboBoxIcoSize.getItems().addAll("16", "32", "64", "128", "256", "512", "768");
        comboBoxIcoSize.setValue("to ICO");

        Cells.comboBoxIcoSizeButtonCell(comboBoxIcoSize, "to ICO");
        Cells.comboBoxIcoSizeSetCellFactory(comboBoxIcoSize, "to ICO");

        comboBoxIcoSize.getSelectionModel().selectedItemProperty().addListener((_, _, newVal) -> {
            if (newVal != null && !newVal.equals("to ICO") && imageViewPreview.getImage() != null) {
                if (imageProperties.getImage() != null && imageProperties.getImage().getName().toLowerCase().endsWith(".ico")) {
                    try {
                        double size = Double.parseDouble(newVal);
                        imageViewPreview.fitHeightProperty().unbind();
                        imageViewPreview.fitWidthProperty().unbind();
                        imageViewPreview.setFitHeight(size);
                        imageViewPreview.setFitWidth(size);
                    } catch (NumberFormatException e) {
                        Alerts.alertDialog(Alert.AlertType.WARNING, "Error", "Format", "Invalid size value!");
                    }
                }
            }
        });
    }

    @Override
    protected void lockUI() {
        disableControls();
        btnSelectFile.setDisable(true);
        btnChoiceFolderForSaveFile.setDisable(true);
        btnSelectBatchFileProcessing.setDisable(true);
        btnReset.setDisable(true);
    }

    @Override
    protected void unlockUI() {
        enableControls();
        btnSelectFile.setDisable(false);
        btnChoiceFolderForSaveFile.setDisable(false);
        btnSelectBatchFileProcessing.setDisable(false);
        btnReset.setDisable(false);
    }

    @Override
    protected void disableControls() {
        listControls.forEach(c -> c.setDisable(true));
    }

    @Override
    protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));
    }

    @FXML
    public void onChoiceFolderForSaveFile() {
        selectOutputDirectory(btnChoiceFolderForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save image");
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
                    labelSelectFile.setText("Current file in list: " + imageProperties.getImage().getName());

                    try {
                        Optional<BufferedImage> biOpt = readPreviewImage(imageProperties.getImage());
                        if (biOpt.isEmpty()) {
                            ErrorLogger.warn("Failed to read preview for file: " + imageProperties.getImage().getName());
                            Alerts.alertDialog(Alert.AlertType.WARNING, "Error", "Format", "Unsupported image format!");
                            return;
                        }

                        Image fxImage = SwingFXUtils.toFXImage(biOpt.get(), null);

                        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);

                        imageViewPreview.setImage(fxImage);

                        labelPreviewPlaceholder.setVisible(false);

                        textDragZone.setText("Batch: " + filesToProcess.size() + " files");

                        if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
                            dropZone.getStyleClass().add("drop-zone-filled");
                        }

                        ErrorLogger.info("Preview loaded successfully for: " + imageProperties.getImage().getName());
                    } catch (IOException e) {
                        ErrorLogger.log(122, ErrorLogger.Level.ERROR, "IO | File error while loading preview", e);
                        Alerts.alertDialog(Alert.AlertType.WARNING, "Error", "IO", "File error!");
                    }
                });
    }

    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectFile, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true
        );

        Util.reset(imageProperties, ctx, "Selected image file: none");

        bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);

        path_folderBatchProcessing = null;
        filesToProcess.clear();

        toggleGroup.selectToggle(null);

        comboBoxIcoSize.setValue("to ICO");
        disableControls();
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Images", Global.getSupportedImageFormatsForFileChooser()),
                "Choice image"
        ).ifPresent(this::loadImage);
    }

    private void loadImage(File file) {
        enableControls();
        imageProperties.setImage(file);
        filesToProcess.clear();

        ErrorLogger.info("User selected file (image): " + imageProperties.getImage().getAbsolutePath());
        labelSelectFile.setText("Select image file: " + imageProperties.getImage().getName());

        try {
            Optional<BufferedImage> biOpt = readPreviewImage(imageProperties.getImage());
            if (biOpt.isEmpty()) {
                ErrorLogger.warn("Failed to read preview for file: " + imageProperties.getImage().getName());
                Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Format", "Unsupported image format!");
                return;
            }

            Image fxImage = SwingFXUtils.toFXImage(biOpt.get(), null);

            Util.bindingImageViewToPreviewContainer(imageViewPreview, previewContainer);

            imageViewPreview.setImage(fxImage);


            labelPreviewPlaceholder.setVisible(false);

            textDragZone.setText("Selected: " + file.getName());

            if (!dropZone.getStyleClass().contains("drop-zone-filled")) {
                dropZone.getStyleClass().add("drop-zone-filled");
            }

            
            ErrorLogger.info("Preview loaded successfully for: " + imageProperties.getImage().getName());
        } catch (IOException e) {
            ErrorLogger.log(107, ErrorLogger.Level.ERROR, "IO | File error while loading preview", e);
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "IO", "File error!");
        }
    }

    private boolean checkForNull() {
        if (imageProperties.getImage() == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "File missing!",
                    "Select image first.");
            return false;
        }

        if(imageProperties.getOutput() == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "File missing!",
                    "Select output directory!.");
            return false;
        }

        if (imageProperties.getTypeImage() == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "File missing!",
                    "Select photo format!");
            return false;
        }

        return true;
    }

    @FXML
    public void submitAndDownload() {
        if(!checkForNull()) {
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

    @FXML
    private void onActionClickToggleBtnFormat(ActionEvent e) {
        comboBoxIcoSize.setValue("to ICO");
        viewHelp.Message.hideSuccessMessage(labelSuccess, getProperties().getHideSuccessMessageTimer(), true);

        ToggleButton tb = (ToggleButton) e.getSource();

        if (!tb.isSelected()) {
            imageProperties.setTypeImage(null);
            return;
        }

        switch (tb.getId()) {
            case "btnToPNG" -> selectFormat("png", imageProperties::setTypeImage);
            case "btnToJPEG" -> selectFormat("jpeg", imageProperties::setTypeImage);
            case "btnToWEBP" -> selectFormat("webp", imageProperties::setTypeImage);
            case "btnToTIFF" -> selectFormat("tif", imageProperties::setTypeImage);
            case "btnToBMP" -> selectFormat("bmp", imageProperties::setTypeImage);
            case "btnToPPM" -> selectFormat("ppm", imageProperties::setTypeImage);
            case "btnToPAM" -> selectFormat("pam", imageProperties::setTypeImage);
            case "btnToPGM" -> selectFormat("pgm", imageProperties::setTypeImage);
            case "btnToSVG" -> selectFormat("svg", imageProperties::setTypeImage);
        }
    }

    @FXML
    private void onActionChoiceIcoSize() {
        viewHelp.Message.hideSuccessMessage(labelSuccess, getProperties().getHideSuccessMessageTimer(), true);
        String selected = comboBoxIcoSize.getValue();

        if (selected == null || selected.equals("to ICO")) {
            if ("ico".equals(imageProperties.getTypeImage())) {
                imageProperties.setTypeImage(null);
            }
            return;
        }

        imageProperties.setSizeIcoImage(Integer.parseInt(selected));

        toggleGroup.selectToggle(null);

        imageProperties.setTypeImage("ico");
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
}
