package media_multitool;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import model.converterImage.ConverterImage;
import model.converterImage.UsefulMethods;
import model.utility.DetermineType;
import model.logger.ErrorLogger;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.select.SelectFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static model.converterImage.UsefulMethods.*;

import model.utility.Preparation;
import viewHelp.Alerts;
import static viewHelp.Message.*;
import static model.utility.Util.*;

public class ConverterImageController {
    private final String ICO_PLACEHOLDER = "to ICO";
    private File image;
    private File outputPath;
    private File path_folderBatchProcessing;
    private String typeImage;
    private int sizeIcoImage;
    private List<File> filesToProcess = new ArrayList<>();
    private final PauseTransition hideSuccessMessageTimer =
            new PauseTransition(Duration.seconds(5));

    @FXML private Button btnSelectPhotoFile;
    @FXML private Button btnChoiceFolderForSaveImage;
    @FXML private Button btnSelectBatchFileProcessing;
    @FXML private Label labelSuccessConvert;
    @FXML private ProgressBar progressBarConvert;
    @FXML private Label labelSelectImage;
    @FXML private ToggleButton btnToSVG;
    @FXML private ToggleButton btnToWEBM;
    @FXML private ToggleButton btnToJPEG;
    @FXML private ToggleButton btnToPNG;
    @FXML private ToggleButton btnToTIFF;
    @FXML private ToggleButton btnToBMP;
    @FXML private ToggleButton btnToPPM;
    @FXML private ToggleButton btnToPGM;
    @FXML private ToggleButton btnToPAM;
    @FXML private Slider imageScaleSlider;
    @FXML private ComboBox<String> comboBoxIcoSize;
    @FXML private ScrollPane scrollPanePhoto;
    @FXML private StackPane imageContainer;
    @FXML private ImageView imageViewPhoto;


    @FXML
    public void initialize() {
        outputPath = getSavedPath();
        btnChoiceFolderForSaveImage.setTooltip(new Tooltip("Default directory: Desktop"));
        imageContainer.setManaged(true);
        imageContainer.setAlignment(Pos.CENTER);
        scrollPanePhoto.setPannable(true);
        scrollPanePhoto.setFitToHeight(true);
        scrollPanePhoto.setFitToWidth(true);
        imageScaleSlider.setMin(1.0);
        imageScaleSlider.setMax(5.0);
        imageScaleSlider.setValue(1.0);

        labelSelectImage.setText("Selected image file: none");

        comboBoxIcoSize.getItems().addAll("16", "32", "64", "128", "256", "512", "768");
        comboBoxIcoSize.setDisable(false);
        comboBoxIcoSize.setValue(ICO_PLACEHOLDER);

        scrollPanePhoto.getStyleClass().add("scroll-pane-image");

        imageViewPhoto.scaleXProperty().bind(imageScaleSlider.valueProperty());
        imageViewPhoto.scaleYProperty().bind(imageScaleSlider.valueProperty());

        imageScaleSlider.valueProperty().addListener((_, _, newVal) -> {
            updateImageContainerSize(newVal.doubleValue());
            Platform.runLater(this::adjustScrollBarToCenter);
        });

        scrollPanePhoto.viewportBoundsProperty().addListener((_, _, _) -> updateImageSize());
        imageViewPhoto.imageProperty().addListener((_, _, _) -> updateImageSize());

        setupClearMessageTimer(labelSuccessConvert, progressBarConvert, hideSuccessMessageTimer);
        labelSuccessConvert.setText("Conversion status");

        comboBoxIcoSize.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || ICO_PLACEHOLDER.equals(item)) {
                    setText(ICO_PLACEHOLDER);
                    setStyle("-fx-background-color: LightGrey;-fx-background-radius: 10;" +
                                    "-fx-border-radius: 10;-fx-alignment: center;-fx-text-fill: black;"
                    );
                } else {
                    setText(item);
                    setStyle("-fx-background-color: #32CD32;-fx-background-radius: 10;" +
                                    "-fx-border-radius: 10;-fx-alignment: center;-fx-text-fill: black;"
                    );
                }
            }
        });

        comboBoxIcoSize.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || ICO_PLACEHOLDER.equals(item)) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setGraphic(null);
                    setStyle("-fx-alignment: center; -fx-text-fill: black;");
                }
            }
        });

        comboBoxIcoSize.getSelectionModel().selectedItemProperty().addListener((_, _, newVal) -> {
            if (newVal != null && !newVal.equals(ICO_PLACEHOLDER) && imageViewPhoto.getImage() != null) {
                if (image != null && image.getName().toLowerCase().endsWith(".ico")) {
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

    @FXML
    private void btnChoiceFolderForSaveImage() {
        Stage stage = getStage(btnChoiceFolderForSaveImage);
        File selectedPath = directoryChooser(stage, outputPath, "Select directory for save image");
        if (selectedPath != null) {
            outputPath = selectedPath;
        }
    }

    @FXML
    private void onActionBtnBatchFileProcessing() {
        Stage stage = getStage(btnSelectBatchFileProcessing);
        File selectedPath = directoryChooser(stage, path_folderBatchProcessing, "Select directory with image");
        if (selectedPath == null) {
            return;
        }
        path_folderBatchProcessing = selectedPath;

        List<File> result = Preparation.getFilesFromFolder(path_folderBatchProcessing, "png", "jpg", "jpeg", "ico", "webp",
                "tiff", "tif", "bmp", "ppm", "pgm", "pam", "jpe", "svg");

        if (result == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "The list is empty", "The list is empty",
                    "Unfortunately, the directory could not be read or is empty!");
            return;
        }

        filesToProcess = new ArrayList<>(result);

        if (filesToProcess.isEmpty()) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "No matching files found", "No matching files found",
                    "No matching files were found in the selected directory.\nPerhaps it only contains unsupported images!");
            return;
        }

        for (File s : filesToProcess) ErrorLogger.info("User selected file (image): " + s.getName());

        image = filesToProcess.getFirst();
        labelSelectImage.setText("Current file in list: " + image.getName());

        try {
            BufferedImage bi = readPreviewImage(image);
            if (bi == null) {
                ErrorLogger.warn("Failed to read preview for file: " + image.getName());
                Alerts.alertDialog(Alert.AlertType.WARNING, "Error", "Format", "Unsupported image format!");
                return;
            }

            Image fxImage = SwingFXUtils.toFXImage(bi, null);
            imageScaleSlider.setValue(1.0);
            imageViewPhoto.setImage(fxImage);

            updateImageSize();
            ErrorLogger.info("Preview loaded successfully for: " + image.getName());
        } catch (IOException e) {
            ErrorLogger.log(122, ErrorLogger.Level.ERROR, "IO | File error while loading preview", e);
            Alerts.alertDialog(Alert.AlertType.WARNING, "Error", "IO", "File error!");
        }
    }

    @FXML
    private void onChoiceIcoSize() {
        String selected = comboBoxIcoSize.getValue();

        if (selected == null || selected.equals(ICO_PLACEHOLDER)) {
            return;
        }

        sizeIcoImage = Integer.parseInt(selected);
        typeImage = "ico";
        btnToPNG.setSelected(false);
        btnToJPEG.setSelected(false);
        btnToWEBM.setSelected(false);
        btnToTIFF.setSelected(false);
        btnToBMP.setSelected(false);
        btnToPPM.setSelected(false);
        btnToPGM.setSelected(false);
        btnToPAM.setSelected(false);
        btnToSVG.setSelected(false);
    }

    public void isPressedReset() {
        image = null;
        labelSelectImage.setText("Selected image file: none");
        path_folderBatchProcessing = null;
        filesToProcess.clear();
        imageViewPhoto.setImage(null);

        btnToPNG.setSelected(false);
        btnToJPEG.setSelected(false);
        btnToWEBM.setSelected(false);
        btnToTIFF.setSelected(false);
        btnToBMP.setSelected(false);
        btnToPPM.setSelected(false);
        btnToPGM.setSelected(false);
        btnToPAM.setSelected(false);
        btnToSVG.setSelected(false);

        comboBoxIcoSize.setValue(ICO_PLACEHOLDER);

        sizeIcoImage = 0;
        imageScaleSlider.setValue(1.0);
        hideSuccessMessage(labelSuccessConvert, progressBarConvert, hideSuccessMessageTimer);
        unlockButtonFormat();
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectPhotoFile.getScene().getWindow();
        image = selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.ico", "*.webp",
                        "*.tiff", "*.tif", "*.bmp", "*.ppm", "*.pgm", "*.pam", "*.jpe", "*.svg"),
                "Choice image"
        );

        filesToProcess.clear();

        if (image == null) return;

        ErrorLogger.info("User selected file (image): " + image.getAbsolutePath());
        labelSelectImage.setText("Select image: " + image.getName());

        try {
            BufferedImage bi = readPreviewImage(image);
            if (bi == null) {
                ErrorLogger.warn("Failed to read preview for file: " + image.getName());
                Alerts.alertDialog(Alert.AlertType.WARNING, "Error", "Format", "Unsupported image format!");
                return;
            }

            Image fxImage = SwingFXUtils.toFXImage(bi, null);
            imageScaleSlider.setValue(1.0);
            imageViewPhoto.setImage(fxImage);

            updateImageSize();
            ErrorLogger.info("Preview loaded successfully for: " + image.getName());
        } catch (IOException e) {
            ErrorLogger.log(107, ErrorLogger.Level.ERROR, "IO | File error while loading preview", e);
            Alerts.alertDialog(Alert.AlertType.WARNING, "Error", "IO", "File error!");
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
    public void onActionSubmitConvertAndDownload() {
        if (image == null || outputPath == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "File missing!", "Select image first.");
            return;
        }

        if (typeImage == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "File missing!", "Select photo format (PNG/JPEG/ICO/WEBP).");
            return;
        }

        lockButtonFormat();
        hideSuccessMessage(labelSuccessConvert, progressBarConvert, hideSuccessMessageTimer);

        List<File> snapshot = new ArrayList<>();
        if (filesToProcess.isEmpty()) {
            snapshot.add(image);
        } else {
            snapshot.addAll(filesToProcess);
        }

        UsefulMethods usefulMethods = new UsefulMethods();
        progressBarConvert.setVisible(true);
        progressBarConvert.setManaged(true);
        progressBarConvert.setProgress(0);

        CompletableFuture.runAsync(() -> {
            int total = snapshot.size();
            int failedCount = 0;
            boolean isBatch = total > 1;

            for (int i = 0; i < total; i++) {
                File img = snapshot.get(i);
                final int currentFileIndex = i + 1;
                
                if (isBatch) {
                    Platform.runLater(() -> labelSuccessConvert.setText("Processing: " + currentFileIndex + " / " + total));
                }

                boolean success = executeConversion(img, usefulMethods, isBatch);
                if (!success) failedCount++;

                final double progress = (double) currentFileIndex / total;
                Platform.runLater(() -> progressBarConvert.setProgress(progress));
            }

            if (isBatch) {
                if (failedCount > 0) {
                    final int finalFailedCount = failedCount;
                    Platform.runLater(() -> {
                        showErrorMessage(labelSuccessConvert, "Completed with " + finalFailedCount + " errors", hideSuccessMessageTimer);
                        Alerts.alertDialog(Alert.AlertType.WARNING,
                            "Batch Processing Complete",
                            "Some files failed",
                            "Processed " + total + " files. " + finalFailedCount + " files failed to convert. Check logs for details.");
                    });
                } else {
                    Platform.runLater(() -> showSuccessText(labelSuccessConvert, "Batch conversion successful! (" + total + " files)", hideSuccessMessageTimer));
                }
            }
        }, IO_EXECUTOR).thenRun(() -> Platform.runLater(() -> {
            unlockButtonFormat();
            showProgressBar(progressBarConvert, hideSuccessMessageTimer);
        })).exceptionally(e -> {
            Platform.runLater(() -> {
                unlockButtonFormat();
                ErrorLogger.error("Async image conversion error: " + e.getMessage());
            });
            return null;
        });
    }

    private boolean executeConversion(File image, UsefulMethods usefulMethods, boolean isBatch) {
        try {
            String inputExtension;
            try {
                inputExtension = usefulMethods.normalizeFormat(DetermineType.determineFormat(image));
            } catch (Exception e) {
                inputExtension = usefulMethods.normalizeFormat(getFileExtension(image));
            }
            String targetFormat = usefulMethods.normalizeFormat(typeImage);

            File convertedFile;
            if ("ico".equals(targetFormat)) {
                if (sizeIcoImage <= 0) {
                    if (!isBatch) Platform.runLater(() -> Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "ICO Size", "Select ICO size."));
                    return false;
                }
                convertedFile = ConverterImage.convertToIco(image, outputPath, sizeIcoImage);
            } else if ("ico".equals(inputExtension)) {
                convertedFile = ConverterImage.convertFromIco(image, outputPath, targetFormat);
            }
            else{
                convertedFile = ConverterImage.convert(image, outputPath, targetFormat);
            }
            if (convertedFile.exists() && convertedFile.isFile() && convertedFile.length() > 0) {
                if (!isBatch) Platform.runLater(() -> showSuccessMessage(labelSuccessConvert, targetFormat, hideSuccessMessageTimer));
                ErrorLogger.info("Conversion completed: " + convertedFile.getName());
                return true;
            } else {
                ErrorLogger.warn("Conversion finished but file not found or empty: " + convertedFile.getName());
                Platform.runLater(() -> {
                    showErrorMessage(labelSuccessConvert, progressBarConvert, "Conversion Failed: File missing", hideSuccessMessageTimer);
                    if (!isBatch) Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "Missing File",
                            "Conversion finished, but saved file was not found.");
                });
                return false;
            }

        } catch (IllegalArgumentException e) {
            ErrorLogger.log(119, ErrorLogger.Level.WARN, "Invalid parameters for conversion", e);
            Platform.runLater(() -> {
                showErrorMessage(labelSuccessConvert, progressBarConvert, "Error: Invalid parameters", hideSuccessMessageTimer);
                if (!isBatch) Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "Invalid Parameters", e.getMessage());
            });
            return false;
        } catch (IOException e) {
            ErrorLogger.log(105, ErrorLogger.Level.ERROR, "IO Error during conversion", e);
            Platform.runLater(() -> {
                showErrorMessage(labelSuccessConvert, progressBarConvert, "Error: " + e.getMessage(), hideSuccessMessageTimer);
                if (!isBatch) Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Conversion Failed", e.getMessage());
            });
            return false;
        } catch (Exception e) {
            ErrorLogger.log(1001, ErrorLogger.Level.ERROR, "Unexpected error during conversion", e);
            Platform.runLater(() -> {
                showErrorMessage(labelSuccessConvert, progressBarConvert, "System Error: " + e.getMessage(), hideSuccessMessageTimer);
                if (!isBatch) Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "System Error", "Something went wrong: " + e.getMessage());
            });
            return false;
        }
    }

    private void selectRasterFormat(String format) {
        typeImage = format;
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
    private void ActionBtnToPNG() {
        selectRasterFormat("png");
    }

    @FXML
    private void ActionBtnToJPEG() {
        selectRasterFormat("jpeg");
    }

    @FXML
    public void ActionBtnToWEBM() {
        selectRasterFormat("webp");
    }

    public void ActionBtnToTIFF() {
        selectRasterFormat("tif");
    }

    public void ActionBtnToBMP() {
        selectRasterFormat("bmp");
    }

    public void ActionBtnToPPM() {
        selectRasterFormat("ppm");
    }

    public void ActionBtnToPAM() {
        selectRasterFormat("pam");
    }

    public void ActionBtnToPGM() {
        selectRasterFormat("pgm");

    }
    @FXML
    private void ActionBtnToSVG() {
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
                        2. (Optional) Choose a directory for saving the output.
                        3. Select the target format (PNG, JPEG, etc.).
                        4. Click 'Convert and Download'.
                        
                        You can also zoom the preview using the slider or mouse wheel.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    private void lockButtonFormat() {
        btnToPNG.setDisable(true);
        btnToJPEG.setDisable(true);
        btnToWEBM.setDisable(true);
        btnToTIFF.setDisable(true);
        btnToBMP.setDisable(true);
        btnToPPM.setDisable(true);
        btnToPGM.setDisable(true);
        btnToPAM.setDisable(true);
        btnToSVG.setDisable(true);
        comboBoxIcoSize.setDisable(true);
    }

    private void unlockButtonFormat() {
        btnToPNG.setDisable(false);
        btnToJPEG.setDisable(false);
        btnToWEBM.setDisable(false);
        btnToTIFF.setDisable(false);
        btnToBMP.setDisable(false);
        btnToPPM.setDisable(false);
        btnToPGM.setDisable(false);
        btnToPAM.setDisable(false);
        btnToSVG.setDisable(false);
        comboBoxIcoSize.setDisable(false);
    }
}
