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

import static model.converterImage.UsefulMethods.*;

import viewHelp.Alerts;
import static viewHelp.Message.*;
import static model.utility.Util.*;

public class ConverterImageController {
    private final String ICO_PLACEHOLDER = "to ICO";

    private File image;
    private File outputPath;
    private String typeImage;
    private int sizeIcoImage;
    private final PauseTransition hideSuccessMessageTimer =
            new PauseTransition(Duration.seconds(5));

    @FXML private Button btnSelectPhotoFile;
    @FXML private Button btnChoiceDirForSaveImage;
    @FXML private Label labelSuccessConvert;
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
        Tooltip tooltipChoiceDir = new Tooltip("Standard directory, Desktop");
        outputPath = getSavedPath();
        btnChoiceDirForSaveImage.setTooltip(tooltipChoiceDir);
        imageContainer.setManaged(true);
        imageContainer.setAlignment(Pos.CENTER);
        scrollPanePhoto.setPannable(true);
        scrollPanePhoto.setFitToHeight(true);
        scrollPanePhoto.setFitToWidth(true);
        imageScaleSlider.setMin(1.0);
        imageScaleSlider.setMax(5.0);
        imageScaleSlider.setValue(1.0);

        labelSelectImage.setText("Selected image file: none");

        comboBoxIcoSize.getItems().addAll("16", "32", "64", "128", "256");
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

        setupClearMessageTimer(labelSuccessConvert, hideSuccessMessageTimer);
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
    public void btnChoiceDirForSaveImage() {
        Stage stage = getStage(btnChoiceDirForSaveImage);
        File selectedPath = setPathForSave(stage, outputPath);
        if (selectedPath != null) {
            outputPath = selectedPath;
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
        hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
    }

    @FXML
    public void ActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectPhotoFile.getScene().getWindow();
        image = selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.ico", "*.webp",
                        "*.tiff", "*.tif", "*.bmp", "*.ppm", "*.pgm", "*.pam", "*.jpe", "*.svg"),
                "Choice image"
        );

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
    public void SubmitConvertAndDownload() {
        if (image == null || outputPath == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "File missing!", "Select image first.");
            return;
        }

        if (typeImage == null) {
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "File missing!", "Select photo format (PNG/JPEG/ICO/WEBP).");
            return;
        }

        try {
            hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
            String inputExtension;
            UsefulMethods usefulMethods = new UsefulMethods();
            try {
                inputExtension = usefulMethods.normalizeFormat(DetermineType.determineFormat(image));
            } catch (Exception e) {
                inputExtension = usefulMethods.normalizeFormat(getFileExtension(image));
            }
            String targetFormat = usefulMethods.normalizeFormat(typeImage);

            if (inputExtension.equals(targetFormat)) {
                Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "Format",
                        "Source and target formats are the same (" + targetFormat + ").");
                return;
            }

            File convertedFile;
            if ("ico".equals(targetFormat)) {
                if (sizeIcoImage <= 0) {
                    Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "ICO Size", "Select ICO size.");
                    return;
                }
                convertedFile = ConverterImage.convertToIco(image, outputPath, sizeIcoImage);
            } else if ("ico".equals(inputExtension)) {
                convertedFile = ConverterImage.convertFromIco(image, outputPath, targetFormat);
            }
            else{
                convertedFile = ConverterImage.convert(image, outputPath, targetFormat);
            }
            if (convertedFile.exists() && convertedFile.isFile() && convertedFile.length() > 0) {
                showSuccessMessage(labelSuccessConvert, targetFormat, hideSuccessMessageTimer);
                ErrorLogger.info("Conversion completed: " + convertedFile.getName());
            } else {
                ErrorLogger.warn("Conversion finished but file not found or empty: " + convertedFile.getName());
                showErrorMessage(labelSuccessConvert, "Conversion Failed: File missing", hideSuccessMessageTimer);
                Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "Missing File",
                        "Conversion finished, but saved file was not found.");
            }

        } catch (IllegalArgumentException e) {
            ErrorLogger.log(119, ErrorLogger.Level.WARN, "Invalid parameters for conversion", e);
            showErrorMessage(labelSuccessConvert, "Error: Invalid parameters", hideSuccessMessageTimer);
            Alerts.alertDialog(Alert.AlertType.WARNING, "Warning", "Invalid Parameters", e.getMessage());
        } catch (IOException e) {
            ErrorLogger.log(105, ErrorLogger.Level.ERROR, "IO Error during conversion", e);
            showErrorMessage(labelSuccessConvert, "Error: " + e.getMessage(), hideSuccessMessageTimer);
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "Conversion Failed", e.getMessage());
        } catch (Exception e) {
            ErrorLogger.log(1001, ErrorLogger.Level.ERROR, "Unexpected error during conversion", e);
            showErrorMessage(labelSuccessConvert, "System Error: " + e.getMessage(), hideSuccessMessageTimer);
            Alerts.alertDialog(Alert.AlertType.ERROR, "Error", "System Error", "Something went wrong: " + e.getMessage());
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
}
