package converter;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.converterImage.ConverterImage;
import model.converterImage.DetermineType;
import model.logger.ErrorLogger;
import model.workWithFiles.SelectImageFile;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.ifok.image.image4j.codec.ico.ICODecoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static model.workWithFiles.Util.*;

public class ConverterImageViewController {
    private static final int SUCCESS_MESSAGE_DURATION_SECONDS = 5;
    private static final String ICO_PLACEHOLDER = "to ICO";

    private File image;
    private File outputPath;
    private String typeImage;
    private int sizeIcoImage;
    private final PauseTransition hideSuccessMessageTimer =
            new PauseTransition(Duration.seconds(SUCCESS_MESSAGE_DURATION_SECONDS));

    @FXML private VBox converterImagePage;
    @FXML private VBox converterPage;
    @FXML private Label labelSelectImageName;
    @FXML private Slider imageScaleSlider;
    @FXML private ImageView imageViewPhoto;
    @FXML private Button btnReset;
    @FXML private Button btnSelectPhotoFile;
    @FXML private Label LabelConvertPhoto;
    @FXML private ToggleButton btnToPNG;
    @FXML private ToggleButton btnToJPEG;
    @FXML private Button btnSubmitConvert;
    @FXML private Button btnChoiceDirForSaveImage;
    @FXML private ComboBox<String> comboBoxIcoSize;
    @FXML private Label labelSuccessConvert;
    @FXML private ScrollPane scrollPanePhoto;
    @FXML private StackPane imageContainer;
    @FXML private ToggleButton btnToWEBM;
    @FXML private Label LabelPlus;
    @FXML private Label LabelMinus;

    @FXML
    public void initialize() {
        Tooltip tooltipChoiceDir = new Tooltip("Standard directory, Desktop");
        btnChoiceDirForSaveImage.setTooltip(tooltipChoiceDir);

        imageContainer.setManaged(true);
        imageContainer.setAlignment(Pos.CENTER);
        scrollPanePhoto.setPannable(true);
        scrollPanePhoto.setFitToHeight(true);
        scrollPanePhoto.setFitToWidth(true);
        imageScaleSlider.setMin(1.0);
        imageScaleSlider.setMax(5.0);
        imageScaleSlider.setValue(1.0);

        imageViewPhoto.scaleXProperty().bind(imageScaleSlider.valueProperty());
        imageViewPhoto.scaleYProperty().bind(imageScaleSlider.valueProperty());

        imageScaleSlider.valueProperty().addListener((_, _, newVal) -> {
            updateImageContainerSize(newVal.doubleValue());
            Platform.runLater(this::adjustScrollBarToCenter);
        });

        scrollPanePhoto.viewportBoundsProperty().addListener((_, _, _) -> updateImageSize());
        imageViewPhoto.imageProperty().addListener((_, _, _) -> updateImageSize());

        outputPath = Paths.get(System.getProperty("user.home"), "Desktop").toFile();
        setupClearMessageTimer(labelSuccessConvert, hideSuccessMessageTimer);

        labelSelectImageName.setText("Select image: none");

        comboBoxIcoSize.getItems().addAll("16", "32", "64", "128", "256");
        comboBoxIcoSize.setDisable(false);
        comboBoxIcoSize.setValue(ICO_PLACEHOLDER);

        comboBoxIcoSize.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || ICO_PLACEHOLDER.equals(item)) {
                    setText(ICO_PLACEHOLDER);
                    setStyle(
                            "-fx-background-color: LightGrey;" +
                                    "-fx-background-radius: 10;" +
                                    "-fx-border-radius: 10;" +
                                    "-fx-alignment: center;" +
                                    "-fx-text-fill: black;"
                    );
                } else {
                    setText(item);
                    setStyle(
                            "-fx-background-color: #32CD32;" +
                                    "-fx-background-radius: 10;" +
                                    "-fx-border-radius: 10;" +
                                    "-fx-alignment: center;" +
                                    "-fx-text-fill: black;"
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
                        ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Error", "Format", "Invalid size value!");
                    }
                }
            }
        });

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
    }

    @FXML
    public void ActionBtnSelectFile() {
        SelectImageFile selectImageFile = new SelectImageFile();
        Stage stage = (Stage) btnSelectPhotoFile.getScene().getWindow();
        image = selectImageFile.choiceFile(stage);

        if (image == null) return;

        ErrorLogger.info("User selected file: " + image.getAbsolutePath());
        labelSelectImageName.setText("Select image: " + image.getName());

        try {
            BufferedImage bi = readPreviewImage(image);
            if (bi == null) {
                ErrorLogger.warn("Failed to read preview for file: " + image.getName());
                ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Error", "Format", "Unsupported image format!");
                return;
            }

            Image fxImage = SwingFXUtils.toFXImage(bi, null);
            imageScaleSlider.setValue(1.0);
            imageViewPhoto.setImage(fxImage);

            updateImageSize();
            ErrorLogger.info("Preview loaded successfully for: " + image.getName());
        } catch (IOException e) {
            ErrorLogger.log(107, ErrorLogger.Level.ERROR, "IO | File error while loading preview", e);
            ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Error", "IO", "File error!");
        }
    }

    private BufferedImage readPreviewImage(File imageFile) throws IOException {
        if ("ico".equals(getFileExtension(imageFile))) {
            List<BufferedImage> images = ICODecoder.read(imageFile);
            if (images.isEmpty()) {
                return null;
            }

            return getLargestImage(images);
        }

        return ImageIO.read(imageFile);
    }

    private BufferedImage getLargestImage(List<BufferedImage> images) {
        BufferedImage largestImage = images.getFirst();

        for (BufferedImage imageCandidate : images) {
            if (imageCandidate.getWidth() * imageCandidate.getHeight()
                    > largestImage.getWidth() * largestImage.getHeight()) {
                largestImage = imageCandidate;
            }
        }

        return largestImage;
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
    public void btnChoiceDirForSaveImage() {
        Stage stage = getStage(btnChoiceDirForSaveImage);
        File selectedPath = setPathForSave(stage, outputPath);
        if (selectedPath != null) {
            outputPath = selectedPath;
        }
    }

    private String getFileExtension(File file) {
        if (file == null) {
            return "";
        }

        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    private String normalizeFormat(String format) {
        if (format == null) {
            return "";
        }

        String normalizedFormat = format.toLowerCase();
        if ("jpg".equals(normalizedFormat)) {
            return "jpeg";
        }

        return normalizedFormat;
    }

    private String getSourceImageFormat(File file) {
        try {
            return normalizeFormat(DetermineType.determineType(file));
        } catch (Exception e) {
            return normalizeFormat(getFileExtension(file));
        }
    }

    @FXML
    public void SubmitConvertAndDownload() {
        if (image == null || outputPath == null) {
            ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Warning", "Selection", "Select image first.");
            return;
        }

        if (typeImage == null) {
            ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Warning", "Selection", "Select photo format (PNG/JPEG/ICO/WEBP).");
            return;
        }

        try {
            hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
            String inputExtension = getSourceImageFormat(image);
            String targetFormat = normalizeFormat(typeImage);

            if (inputExtension.equals(targetFormat)) {
                ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Warning", "Format",
                        "Source and target formats are the same (" + targetFormat + ").");
                return;
            }

            File convertedFile;
            if ("ico".equals(targetFormat)) {
                if (sizeIcoImage <= 0) {
                    ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Warning", "ICO Size", "Select ICO size.");
                    return;
                }
                convertedFile = ConverterImage.convertToIco(image, outputPath, sizeIcoImage);
            } else if ("ico".equals(inputExtension)) {
                convertedFile = ConverterImage.convertFromIco(image, outputPath, targetFormat);
            } else {
                convertedFile = ConverterImage.convert(image, outputPath, targetFormat);
            }

            if (isValidConvertedFile(convertedFile)) {
                showSuccessMessage(labelSuccessConvert, targetFormat, hideSuccessMessageTimer);
                ErrorLogger.info("Conversion completed: " + convertedFile.getName());
            } else {
                ErrorLogger.warn("Conversion finished but file not found or empty: " + convertedFile.getName());
                ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Warning", "Missing File",
                        "Conversion finished, but saved file was not found.");
            }

        } catch (IllegalArgumentException e) {
            ErrorLogger.log(1002, ErrorLogger.Level.WARN, "Invalid parameters for conversion", e);
            ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Warning", "Invalid Parameters", e.getMessage());
        } catch (IOException e) {
            ErrorLogger.log(105, ErrorLogger.Level.ERROR, "IO Error during conversion", e);
            ErrorLogger.alertDialog(Alert.AlertType.ERROR, "Error", "Conversion Failed", e.getMessage());
        } catch (Exception e) {
            ErrorLogger.log(1001, ErrorLogger.Level.ERROR, "Unexpected error during conversion", e);
            ErrorLogger.alertDialog(Alert.AlertType.ERROR, "Error", "System Error", "Something went wrong: " + e.getMessage());
        }
    }

    private void selectRasterFormat(String format) {
        typeImage = format;
        btnToPNG.setSelected("png".equals(format));
        btnToJPEG.setSelected("jpeg".equals(format));
        btnToWEBM.setSelected("webp".equals(format));
        comboBoxIcoSize.setValue(ICO_PLACEHOLDER);
    }

    private Stage getStage(Control control) {
        return (Stage) control.getScene().getWindow();
    }

    private boolean isValidConvertedFile(File convertedFile) {
        return convertedFile != null
                && convertedFile.exists()
                && convertedFile.isFile()
                && convertedFile.length() > 0;
    }

    public void ActionBtnToWEBM() {
        selectRasterFormat("webp");
    }

    public void isPressedReset() {
        image = null;
        labelSelectImageName.setText("Select image: none");
        imageViewPhoto.setImage(null);

        btnToPNG.setSelected(false);
        btnToJPEG.setSelected(false);
        btnToWEBM.setSelected(false);
        comboBoxIcoSize.setValue(ICO_PLACEHOLDER);

        sizeIcoImage = 0;
        imageScaleSlider.setValue(1.0);
        hideSuccessMessage(labelSuccessConvert, hideSuccessMessageTimer);
    }
}
