package converter;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import model.converter.ConverterImage;
import model.converter.DetermineType;
import model.logger.ErrorLogger;
import model.workWithFiles.ClassSelect;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.ifok.image.image4j.codec.ico.ICODecoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class Controller {
    private static final int SUCCESS_MESSAGE_DURATION_SECONDS = 5;
    private static final String ICO_PLACEHOLDER = "to ICO";

    private File image;
    private File outputPath;
    private String typeImage;
    private int sizeIcoImage;
    private final PauseTransition hideSuccessMessageTimer =
            new PauseTransition(Duration.seconds(SUCCESS_MESSAGE_DURATION_SECONDS));

    @FXML
    private Label LabelSelectImageName;

    @FXML
    private Slider imageScaleSlider;

    @FXML
    private Pane leftPane;

    @FXML
    private Pane rightPane;

    @FXML
    private ImageView imageViewPhoto;

    @FXML
    private AnchorPane AnchorPane;

    @FXML
    private Pane mainPane;

    @FXML
    private Button btnSelectPhotoFile;

    @FXML
    private Label LabelConvertPhoto;

    @FXML
    private ToggleButton btnToPNG;

    @FXML
    private ToggleButton btnToJPEG;

    @FXML
    private Button btnSubmitConvert;

    @FXML
    private Button btnChoiceDirForSaveImage;

    @FXML
    private ComboBox<String> ComboBoxIcoSize;

    @FXML
    private Label LabelSuccessConvert;

    @FXML
    private ScrollPane scrollPanePhoto;

    @FXML
    private StackPane imageContainer;

    @FXML
    private ToggleButton btnToWEBM;


    @FXML
    public void initialize() {
        assert AnchorPane != null : "fx:id=\"AnchorPane\" was not injected!";
        assert mainPane != null : "fx:id=\"mainPane\" was not injected!";
        assert btnSelectPhotoFile != null : "fx:id=\"btnSelectPhotoFile\" was not injected!";
        assert LabelConvertPhoto != null : "fx:id=\"LabelConvertPhoto\" was not injected!";
        assert btnToPNG != null : "fx:id=\"btnToPNG\" was not injected!";
        assert btnToJPEG != null : "fx:id=\"btnToJPEG\" was not injected!";
        assert btnSubmitConvert != null : "fx:id=\"btnSubmitConvert\" was not injected!";
        assert btnChoiceDirForSaveImage != null : "fx:id=\"btnChoiceDirForSaveImage\" was not injected!";
        assert LabelSelectImageName != null : "fx:id=\"LabelSelectImageName\" was not injected!";
        assert ComboBoxIcoSize != null : "fx:id=\"ComboBoxIcoSize\" was not injected!";
        assert LabelSuccessConvert != null : "fx:id=\"LabelSuccessConvert\" was not injected!";
        assert imageViewPhoto != null : "fx:id=\"imageViewPhoto\" was not injected!";
        assert leftPane != null : "fx:id=\"leftPane\" was not injected!";
        assert rightPane != null : "fx:id=\"rightPane\" was not injected!";
        assert imageScaleSlider != null : "fx:id=\"imageScaleSlider\" was not injected!";
        assert scrollPanePhoto != null : "fx:id=\"scrollPanePhoto\" was not injected!";
        assert btnToWEBM != null : "fx:id=\"btnToWEBM\" was not injected!";

        Tooltip tooltipChoiceDir = new Tooltip("Standard directory, Desktop");
        btnChoiceDirForSaveImage.setTooltip(tooltipChoiceDir);

        imageScaleSlider.setMin(1.0);
        imageScaleSlider.setMax(5.0); // Увеличил до 5 для наглядности
        imageScaleSlider.setValue(1.0);

        imageViewPhoto.scaleXProperty().bind(imageScaleSlider.valueProperty());
        imageViewPhoto.scaleYProperty().bind(imageScaleSlider.valueProperty());

        imageScaleSlider.valueProperty().addListener((_, _, newVal) -> {
            double zoom = newVal.doubleValue();

            double newWidth = imageViewPhoto.getFitWidth() * zoom;
            double newHeight = imageViewPhoto.getFitHeight() * zoom;

            imageContainer.setMinWidth(newWidth);
            imageContainer.setMinHeight(newHeight);
        });

        outputPath = Paths.get(System.getProperty("user.home"), "Desktop").toFile();
        LabelSuccessConvert.setVisible(false);
        LabelSuccessConvert.setManaged(false);
        LabelSuccessConvert.setText("");

        hideSuccessMessageTimer.setOnFinished(_ -> {
            LabelSuccessConvert.setVisible(false);
            LabelSuccessConvert.setManaged(false);
            LabelSuccessConvert.setText("");
        });

        ComboBoxIcoSize.getItems().addAll("16", "32", "64", "128");
        ComboBoxIcoSize.setDisable(false);
        ComboBoxIcoSize.setValue(ICO_PLACEHOLDER);

        ComboBoxIcoSize.setButtonCell(new ListCell<>() {
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

        ComboBoxIcoSize.setCellFactory(_ -> new ListCell<>() {
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

        ComboBoxIcoSize.getSelectionModel().selectedItemProperty().addListener((_, _, newVal) -> {
            if (newVal != null && !newVal.equals(ICO_PLACEHOLDER) && imageViewPhoto.getImage() != null) {
                if (image != null && image.getName().toLowerCase().endsWith(".ico")) {
                    try {
                        double size = Double.parseDouble(newVal);
                        imageViewPhoto.setFitHeight(size);
                        imageViewPhoto.setFitWidth(size);
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
        String selected = ComboBoxIcoSize.getValue();

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
        ClassSelect selectImageFile = new ClassSelect();
        Stage stage = (Stage) btnSelectPhotoFile.getScene().getWindow();
        image = selectImageFile.choiceFile(stage);

        if (image == null) return;

        LabelSelectImageName.setText(image.getName());

        try {
            BufferedImage bi = readPreviewImage(image);
            if (bi == null) {
                ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Error", "Format", "Unsupported image format!");
                return;
            }

            Image fxImage = SwingFXUtils.toFXImage(bi, null);
            imageViewPhoto.setImage(fxImage);

            updateImageSize();
        } catch (IOException e) {
            ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Error", "IO", "File error!");
        }
    }

    private BufferedImage readPreviewImage(File imageFile) throws IOException {
        if ("ico".equals(getFileExtension(imageFile))) {
            List<BufferedImage> images = ICODecoder.read(imageFile);
            if (images == null || images.isEmpty()) {
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
        if (imageViewPhoto.getImage() != null && image != null) {
            if (image.getName().toLowerCase().endsWith(".ico")) {
                String val = ComboBoxIcoSize.getValue();
                double size = (val == null || val.equals(ICO_PLACEHOLDER)) ? 16 : Double.parseDouble(val);

                imageViewPhoto.setPreserveRatio(false);
                imageViewPhoto.setFitHeight(size);
                imageViewPhoto.setFitWidth(size);
            } else {
                imageViewPhoto.setPreserveRatio(true);
                imageViewPhoto.setFitHeight(imageViewPhoto.getFitHeight() - 20);
                imageViewPhoto.setFitWidth(imageViewPhoto.getFitHeight());
            }
        }
    }

    @FXML
    public void btnChoiceDirForSaveImage() {
        Stage stage = getStage(btnChoiceDirForSaveImage);
        File selectedPath = ClassSelect.setPathForSave(stage, outputPath);
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
        try {
            if (image == null || outputPath == null) {
                ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Warning", "Warning", "Select image first.");
                return;
            }

            if (typeImage == null) {
                ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Warning", "Warning", "Select photo format (PNG/JPEG/ICO).");
                return;
            }

            hideSuccessMessage();

            String inputExtension = getSourceImageFormat(image);
            String targetFormat = normalizeFormat(typeImage);
            File convertedFile;

            if (inputExtension.equals(targetFormat)) {
                ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Warning", "Warning",
                        "You cannot convert an image to the same format.");
                return;
            }

            if ("ico".equals(targetFormat)) {
                if (ComboBoxIcoSize.getValue() == null || ICO_PLACEHOLDER.equals(ComboBoxIcoSize.getValue()) || sizeIcoImage <= 0) {
                    ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Warning", "Warning", "Select ICO size.");
                    return;
                }

                convertedFile = ConverterImage.convertToIco(image, outputPath, sizeIcoImage);
            } else {
                if ("ico".equals(inputExtension)) {
                    convertedFile = ConverterImage.convertFromIco(image, outputPath, targetFormat);
                } else {
                    convertedFile = ConverterImage.convert(image, outputPath, targetFormat);
                }
            }

            if (isValidConvertedFile(convertedFile)) {
                showSuccessMessage("Image converted successfully!");
            } else {
                ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Warning", "Warning",
                        "Conversion finished, but saved file was not found.");
            }

        } catch (IllegalArgumentException e) {
            ErrorLogger.alertDialog(Alert.AlertType.WARNING, "Warning", "Warning", "Invalid image or format selected.");
            ErrorLogger.logError(104, "IllegalArgumentException during conversion", e);

        } catch (IOException e) {
            ErrorLogger.alertDialog(Alert.AlertType.ERROR, "Error", "Conversion Error", "Failed to convert image: " + e.getMessage());
            ErrorLogger.logError(105, "I/O error during conversion", e);

        } catch (Exception e) {
            ErrorLogger.alertDialog(Alert.AlertType.ERROR, "Error", "Unexpected Error", "Something went wrong: " + e.getMessage());
            ErrorLogger.logError(999, "Unexpected error in SubmitConvertAndDownload", e);
        }
    }

    private void selectRasterFormat(String format) {
        typeImage = format;
        btnToPNG.setSelected("png".equals(format));
        btnToJPEG.setSelected("jpeg".equals(format));
        btnToWEBM.setSelected("webp".equals(format));
        ComboBoxIcoSize.setValue(ICO_PLACEHOLDER);
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

    private void showSuccessMessage(String message) {
        LabelSuccessConvert.setText(message);
        LabelSuccessConvert.setManaged(true);
        LabelSuccessConvert.setVisible(true);
        hideSuccessMessageTimer.stop();
        hideSuccessMessageTimer.playFromStart();
    }

    private void hideSuccessMessage() {
        hideSuccessMessageTimer.stop();
        LabelSuccessConvert.setVisible(false);
        LabelSuccessConvert.setManaged(false);
        LabelSuccessConvert.setText("");
    }

    public void ActionBtnToWEBM() {
        selectRasterFormat("webp");
    }
}
